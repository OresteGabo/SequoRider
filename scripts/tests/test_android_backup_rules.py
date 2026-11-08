"""Prevent encrypted auth data from restoring without its device-bound Keystore keys."""
from pathlib import Path
import re
import unittest
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"


class AndroidBackupRulesTest(unittest.TestCase):
    def encrypted_stores(self):
        stores = set()
        for source in (ROOT / "shared/src/androidMain").rglob("*.kt"):
            stores.update(re.findall(
                r'EncryptedSharedPreferences\.create\(\s*context,\s*"([^"]+)"',
                source.read_text(),
            ))
        self.assertTrue(stores, "Expected the Android encrypted authentication stores")
        return {name + ".xml" for name in stores}

    def rules(self, attribute):
        application = ElementTree.parse(ROOT / "androidApp/src/main/AndroidManifest.xml").getroot().find("application")
        reference = application.attrib[ANDROID + attribute]
        self.assertTrue(reference.startswith("@xml/"))
        return ElementTree.parse(ROOT / "androidApp/src/main/res/xml" / (reference[5:] + ".xml")).getroot()

    def assert_encrypted_stores_excluded(self, rule):
        self.assertIsNotNone(rule)
        excluded = {item.attrib.get("path") for item in rule.findall("exclude")
                    if item.attrib.get("domain") == "sharedpref"}
        self.assertTrue(self.encrypted_stores().issubset(excluded),
                        "Every encrypted preference store must be excluded")
        self.assertEqual([], rule.findall("include"), "Retain ordinary settings backup")

    def test_legacy_cloud_backup_excludes_encrypted_preferences(self):
        self.assert_encrypted_stores_excluded(self.rules("fullBackupContent"))

    def test_android_12_cloud_backup_excludes_encrypted_preferences(self):
        self.assert_encrypted_stores_excluded(self.rules("dataExtractionRules").find("cloud-backup"))

    def test_android_12_device_transfer_excludes_encrypted_preferences(self):
        self.assert_encrypted_stores_excluded(self.rules("dataExtractionRules").find("device-transfer"))


if __name__ == "__main__":
    unittest.main()
