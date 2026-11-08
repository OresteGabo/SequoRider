#!/usr/bin/env python3
"""Build an explicitly targeted acceptance artifact; never installs or launches it."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
from urllib.parse import urlsplit
from acceptance_support import SigningConfigurationError, validate_development_signing, app_payload_metadata


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--platform", choices=("android", "ios"), required=True)
    parser.add_argument("--api-base-url", required=True)
    parser.add_argument("--release-id", required=True)
    parser.add_argument("--environment", choices=("LOCAL_FIXTURE", "STAGING"), required=True)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--development-signing-reference", type=Path,
                        help="Private identity/team reference for an installed Apple Development certificate; iOS LOCAL_FIXTURE only.")
    args = parser.parse_args()
    try:
        development_signing = validate_development_signing(args.development_signing_reference, args.platform, args.environment)
    except SigningConfigurationError as error:
        parser.error(str(error))
    target = urlsplit(args.api_base_url)
    local = target.hostname in ("127.0.0.1", "localhost", "10.0.2.2", "::1")
    if (target.username is not None or target.password is not None or target.query or target.fragment or
            target.path not in ("", "/") or not target.hostname or
            (args.environment == "LOCAL_FIXTURE" and (not local or target.scheme not in ("http", "https"))) or
            (args.environment == "STAGING" and (local or target.scheme != "https"))):
        parser.error("Use a root local origin for LOCAL_FIXTURE or a nonlocal HTTPS origin for STAGING, without credentials.")
    try:
        port = target.port
        if port is not None and not 1 <= port <= 65535:
            raise ValueError()
    except ValueError:
        parser.error("Invalid API port.")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,79}", args.release_id):
        parser.error("Use a release ID containing letters, digits, dots, underscores or hyphens.")
    root = Path(__file__).resolve().parents[1]
    output = (args.output_dir or root / "build" / "acceptance" / args.release_id / args.platform).resolve()
    output.mkdir(parents=True, exist_ok=True)
    values = {"sequoApiBaseUrl": args.api_base_url.rstrip("/"), "sequoReleaseId": args.release_id,
              "sequoEnvironment": args.environment}
    gradle = [str(root / "gradlew"), "--offline"] + [f"-P{key}={value}" for key, value in values.items()]
    suffix = "fixture" if args.environment == "LOCAL_FIXTURE" else "staging"
    env = os.environ.copy()
    env.update({"ORG_GRADLE_PROJECT_" + key: value for key, value in values.items()})
    if args.platform == "android":
        if args.environment == "LOCAL_FIXTURE" and target.hostname != "10.0.2.2":
            parser.error("Android emulator fixture builds require the host alias 10.0.2.2.")
        subprocess.run(gradle + [":androidApp:assembleDebug"], cwd=root, env=env, check=True)
        artifact = output / f"SequoRider-{args.release_id}-{args.environment}.apk"
        shutil.copy2(root / "androidApp/build/outputs/apk/debug/androidApp-debug.apk", artifact)
        app_id = "dev.orestegabo.sequorider." + suffix
    else:
        if args.environment == "LOCAL_FIXTURE" and target.hostname not in ("127.0.0.1", "localhost", "::1"):
            parser.error("iOS simulator fixture builds require a loopback API origin.")
        info = plistlib.loads((root / "iosApp/iosApp/Info.plist").read_bytes())
        if args.environment == "LOCAL_FIXTURE":
            info["NSAppTransportSecurity"] = {"NSAllowsArbitraryLoads": True, "NSAllowsLocalNetworking": True}
        info["CFBundleDisplayName"] = f"SequoRider [{args.environment}]"
        fixture_info = output / "Acceptance-Info.plist"
        fixture_info.write_bytes(plistlib.dumps(info))
        app_id = "dev.orestegabo.sequorider.SequoRider." + suffix
        # Prebuild the shared framework offline. Xcode's existing integration script then reuses it.
        subprocess.run(gradle + [":shared:linkDebugFrameworkIosSimulatorArm64"], cwd=root, env=env, check=True)
        simulator_signing_flags = development_signing.xcode_flags() if development_signing else ["CODE_SIGNING_ALLOWED=NO"]
        subprocess.run(["xcodebuild", "-project", str(root / "iosApp/iosApp.xcodeproj"),
            "-scheme", "iosApp", "-configuration", "Debug", "-sdk", "iphonesimulator",
            "-destination", "generic/platform=iOS Simulator", "-derivedDataPath", str(output / "DerivedData"),
            "-disableAutomaticPackageResolution", "-skipPackageUpdates", *simulator_signing_flags,
            "ARCHS=arm64", "ONLY_ACTIVE_ARCH=YES",
            "PRODUCT_BUNDLE_IDENTIFIER=" + app_id, "INFOPLIST_FILE=" + str(fixture_info),
            "build"], cwd=root, env=env, check=True)
        apps = list((output / "DerivedData/Build/Products/Debug-iphonesimulator").glob("*.app"))
        if len(apps) != 1:
            raise RuntimeError("Expected one simulator app artifact.")
        artifact = output / f"SequoRider-{args.release_id}-{args.environment}.app"
        if artifact.exists():
            shutil.rmtree(artifact)
        shutil.copytree(apps[0], artifact, symlinks=True)
    dirty_paths = subprocess.check_output(["git", "diff", "--name-only", "-z", "HEAD", "--"], cwd=root).decode().split("\0")
    dirty_paths = [path for path in dirty_paths if path]
    metadata = {"app": "SequoRider", "platform": args.platform, "environment": args.environment,
        "releaseId": args.release_id, "apiBaseUrl": values["sequoApiBaseUrl"], "applicationId": app_id,
        "sourceHead": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(),
        "artifact": str(artifact), "installedOrLaunched": False,
        "trackedDirtyPaths": dirty_paths,
        "signing": "debug key" if args.platform == "android" else
            ("validated local Apple Development simulator" if development_signing else "unsigned simulator only")}
    if artifact.is_file():
        metadata["sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    else:
        executable = artifact / plistlib.loads((artifact / "Info.plist").read_bytes())["CFBundleExecutable"]
        metadata["executableSha256"] = hashlib.sha256(executable.read_bytes()).hexdigest()
        metadata.update(app_payload_metadata(artifact))
    (output / "build-target.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
