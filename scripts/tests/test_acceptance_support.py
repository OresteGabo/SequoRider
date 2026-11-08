import base64
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("acceptance_support", ROOT / "acceptance_support.py")
support = importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name] = support
spec.loader.exec_module(support)


class AcceptanceSupportTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.der = b"synthetic certificate metadata fixture only"
        self.identity = hashlib.sha1(self.der).hexdigest().upper()
        self.team = "TESTTEAM01"
        self.reference = self.root / "private.json"
        self.reference.write_text(json.dumps({"identity": self.identity, "team": self.team}))
        self.reference.chmod(0o600)

    def metadata(self, command, **kwargs):
        if command[:2] == ["security", "find-identity"]:
            return types.SimpleNamespace(returncode=0, stdout=f'  1) {self.identity} "Apple Development: Synthetic fixture"\n', stderr="")
        if command[:2] == ["security", "find-certificate"]:
            pem = "-----BEGIN CERTIFICATE-----\n" + base64.b64encode(self.der).decode() + "\n-----END CERTIFICATE-----\n"
            return types.SimpleNamespace(returncode=0, stdout=pem, stderr="")
        return types.SimpleNamespace(returncode=0, stdout=f"subject=CN=Apple Development: Synthetic fixture,OU={self.team},O=Fixture\nCertificate will not expire\n", stderr="")

    def test_unsigned_default_and_nonfixture_signing_make_no_keychain_calls(self):
        with patch.object(support.subprocess, "run") as run:
            self.assertIsNone(support.validate_development_signing(None, "ios", "STAGING"))
            for platform, environment in [("android", "LOCAL_FIXTURE"), ("ios", "STAGING"), ("ios", "PRODUCTION")]:
                with self.assertRaises(support.SigningConfigurationError):
                    support.validate_development_signing(self.reference, platform, environment)
            run.assert_not_called()

    def test_private_installed_development_certificate_and_real_team_are_required(self):
        with patch.object(support.subprocess, "run", side_effect=self.metadata) as run:
            signing = support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")
        self.assertEqual(4, run.call_count)
        flags = signing.xcode_flags()
        self.assertIn("CODE_SIGN_STYLE=Automatic", flags)
        self.assertIn("DEVELOPMENT_TEAM=" + self.team, flags)
        self.assertFalse(any("ENTITLEMENTS" in flag for flag in flags))
        self.assertNotIn(self.identity, repr(signing))

    def test_ad_hoc_malformed_and_public_or_symlink_references_fail_before_metadata(self):
        with patch.object(support.subprocess, "run") as run:
            self.reference.chmod(0o644)
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")
            self.reference.chmod(0o600)
            self.reference.write_text(json.dumps({"identity": "-", "team": self.team}))
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")
            link = self.root / "link.json"; link.symlink_to(self.reference)
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(link, "ios", "LOCAL_FIXTURE")
            run.assert_not_called()

    def test_distribution_identity_or_other_team_cannot_sign(self):
        original = self.metadata
        def distribution(command, **kwargs):
            result = original(command, **kwargs)
            if command[:2] == ["security", "find-identity"]:
                result.stdout = result.stdout.replace("Apple Development:", "Apple Distribution:")
            return result
        with patch.object(support.subprocess, "run", side_effect=distribution):
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")
        self.reference.write_text(json.dumps({"identity": self.identity, "team": "OTHERTEAM1"}))
        with patch.object(support.subprocess, "run", side_effect=original):
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")

    def test_unmatched_or_expired_certificate_fails_closed(self):
        original = self.metadata
        def expired(command, **kwargs):
            result = original(command, **kwargs)
            if command[0] == "openssl": result.returncode = 1
            return result
        with patch.object(support.subprocess, "run", side_effect=expired):
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")
        def unmatched(command, **kwargs):
            result = original(command, **kwargs)
            if command[:2] == ["security", "find-certificate"]: result.stdout = ""
            return result
        with patch.object(support.subprocess, "run", side_effect=unmatched):
            with self.assertRaises(support.SigningConfigurationError):
                support.validate_development_signing(self.reference, "ios", "LOCAL_FIXTURE")

    def test_payload_changes_when_debug_code_changes_even_with_unchanged_launcher(self):
        app = self.root / "Fixture.app"; app.mkdir()
        (app / "Fixture").write_bytes(b"unchanged Xcode launcher")
        (app / "Fixture.debug.dylib").write_bytes(b"code version one")
        first = support.app_payload_metadata(app)
        (app / "Fixture.debug.dylib").write_bytes(b"code version two")
        second = support.app_payload_metadata(app)
        self.assertNotEqual(first["payloadSha256"], second["payloadSha256"])
        self.assertNotEqual(first["debugDylibSha256"], second["debugDylibSha256"])
        self.assertEqual(2, second["payloadFileCount"])

    def test_payload_does_not_read_external_symlinks(self):
        app = self.root / "Fixture.app"; app.mkdir()
        outside = self.root / "outside"; outside.write_bytes(b"not part of app")
        (app / "external").symlink_to(outside)
        with self.assertRaises(ValueError): support.app_payload_metadata(app)


if __name__ == "__main__":
    unittest.main()
