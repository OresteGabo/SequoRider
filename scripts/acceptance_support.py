"""Optional, validated local simulator signing and artifact payload fingerprints.

No private key export, custom entitlements, identity fabrication, install or launch.
"""
from dataclasses import dataclass
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess


class SigningConfigurationError(ValueError):
    pass


@dataclass(frozen=True, repr=False)
class DevelopmentSigning:
    identity: str
    team: str

    def xcode_flags(self):
        return ["CODE_SIGNING_ALLOWED=YES", "CODE_SIGNING_REQUIRED=YES",
                "CODE_SIGN_IDENTITY=" + self.identity, "DEVELOPMENT_TEAM=" + self.team,
                "CODE_SIGN_STYLE=Automatic"]


def _run_public_metadata(command, *, input_text=None):
    try:
        result = subprocess.run(command, input=input_text, capture_output=True, text=True, check=False)
    except OSError as error:
        raise SigningConfigurationError("Installed development signing metadata could not be checked.") from error
    if result.returncode:
        raise SigningConfigurationError("Installed development signing metadata is unavailable or expired.")
    return result.stdout


def validate_development_signing(reference, platform, environment):
    if reference is None:
        return None
    if platform != "ios" or environment != "LOCAL_FIXTURE":
        raise SigningConfigurationError("Development signing is restricted to iOS LOCAL_FIXTURE simulator builds.")
    reference = Path(reference)
    try:
        info = reference.lstat()
        if (not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) & 0o077):
            raise SigningConfigurationError("The signing reference must be a private regular file owned by this user.")
        with reference.open() as source:
            value = json.load(source)
    except (OSError, json.JSONDecodeError) as error:
        raise SigningConfigurationError("A private development signing reference is required.") from error
    if (not isinstance(value, dict) or set(value) != {"identity", "team"} or
            not isinstance(value["identity"], str) or not re.fullmatch(r"[0-9A-Fa-f]{40}", value["identity"]) or
            not isinstance(value["team"], str) or not re.fullmatch(r"[A-Z0-9]{10}", value["team"])):
        raise SigningConfigurationError("The reference must contain an installed certificate fingerprint and its team.")
    identity, team = value["identity"].upper(), value["team"]
    identities = _run_public_metadata(["security", "find-identity", "-v", "-p", "codesigning"])
    installed = re.findall(r'^\s*\d+\)\s+([0-9A-Fa-f]{40})\s+"Apple Development:[^"]+"\s*$', identities, re.M)
    if identity not in {item.upper() for item in installed}:
        raise SigningConfigurationError("The selected identity is not an installed valid Apple Development identity.")
    certificates = _run_public_metadata(["security", "find-certificate", "-a", "-c", "Apple Development", "-p"])
    matching = None
    for pem in re.findall(r"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----", certificates, re.S):
        try:
            der = base64.b64decode("".join(pem.splitlines()[1:-1]), validate=True)
        except ValueError:
            continue
        if hashlib.sha1(der).hexdigest().upper() == identity:
            matching = pem
            break
    if matching is None:
        raise SigningConfigurationError("The installed development certificate could not be matched to its fingerprint.")
    _run_public_metadata(["openssl", "x509", "-noout", "-checkend", "0"], input_text=matching)
    subject = _run_public_metadata(["openssl", "x509", "-noout", "-subject", "-nameopt", "RFC2253"], input_text=matching)
    teams = re.findall(r"(?:^|,)OU=([A-Z0-9]{10})(?=,|$)", subject.replace("subject=", "").splitlines()[0].strip())
    if teams != [team]:
        raise SigningConfigurationError("The reference team does not match the installed certificate organizational unit.")
    return DevelopmentSigning(identity, team)


def app_payload_metadata(artifact):
    """Hash every contained file and relative symlink; do not follow external links."""
    artifact = Path(artifact).resolve()
    records = []
    debug_dylibs = {}
    for path in sorted(artifact.rglob("*"), key=lambda item: item.relative_to(artifact).as_posix()):
        relative = path.relative_to(artifact).as_posix()
        if path.is_symlink():
            target = os.readlink(path)
            try:
                path.resolve().relative_to(artifact)
            except (ValueError, RuntimeError) as error:
                raise ValueError("The app bundle contains an external or cyclic symlink.") from error
            records.append([relative, "symlink", target])
        elif path.is_file():
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            records.append([relative, "file", digest])
            if path.name.endswith(".debug.dylib"):
                debug_dylibs[relative] = digest
    digest = hashlib.sha256(json.dumps(records, ensure_ascii=True, separators=(",", ":")).encode()).hexdigest()
    return {"payloadSha256": digest, "payloadFileCount": len(records), "debugDylibSha256": debug_dylibs}
