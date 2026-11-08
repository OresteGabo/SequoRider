# Rider acceptance builds

The signed MVP covers a testable rider app (13, 105, 121–125) and acceptance delivery (§9). A build and cold launch establish packaging only; an authenticated staged delivery still needs approved staging access and role accounts.

Normal builds retain `https://api.sequoservice.com`. Every client, including authentication, uses one generated build target. Account settings display the release ID, environment and server in English/French. Any API override requires all three Gradle properties: `sequoApiBaseUrl`, `sequoReleaseId`, `sequoEnvironment`. Nonlocal targets require HTTPS; `LOCAL_FIXTURE` accepts only loopback/emulator origins. No credentials, paths, queries or fragments are accepted in the server origin.

Build an isolated Android emulator artifact:

```sh
python3 scripts/build-acceptance.py --platform android --api-base-url http://10.0.2.2:55828 --release-id sequo-mvp-rc-20261108-01 --environment LOCAL_FIXTURE
```

Build an unsigned iOS simulator artifact:

```sh
python3 scripts/build-acceptance.py --platform ios --api-base-url http://127.0.0.1:55828 --release-id sequo-mvp-rc-20261108-01 --environment LOCAL_FIXTURE
```

Use the same helper with `--environment STAGING` and an explicitly approved HTTPS server to build a staging debug/simulator artifact. The helper never installs, launches, publishes or signs a store build. `build/acceptance/<release>/<platform>/build-target.json` records the target, app ID, source HEAD and artifact checksum. Those outputs are ignored by Git.

Android fixture/staging builds use `.fixture`/`.staging` application ID suffixes; ordinary release ID and version remain unchanged. Local cleartext is enabled only in the fixture debug manifest. Release cleartext stays disabled. Internet permission is explicit in the main manifest.

The iOS helper copies the shipping Info.plist into the ignored artifact directory; only the local simulator copy allows local HTTP. The existing shipping plist and Xcode signing configuration stay unchanged. Fixture/staging simulator bundle IDs end in `.fixture`/`.staging` to keep their sessions separate from normal installations. Physical iOS/TestFlight delivery requires an authorized Apple team, signing and provisioning. Google authentication for distinct acceptance app IDs requires approved OAuth registrations; do not use local demo data as proof of a real rider journey.

Verification:

```sh
./gradlew --offline -PsequoApiBaseUrl=http://10.0.2.2:55828 -PsequoReleaseId=sequo-mvp-rc-20261108-01 -PsequoEnvironment=LOCAL_FIXTURE :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug
```

Run against a fresh task-owned emulator/simulator. Confirm the launcher label identifies `LOCAL_FIXTURE`, the account page shows the expected release/server, an unavailable local server shows a readable error, and refresh never substitutes demo missions into a real session. The full acceptance journey must additionally sign in as an approved rider, accept a dispatched paid staging order, reject wrong proof, repeat safely, complete delivery and reload server history. Provider transfers remain disabled without approved integration and sandbox evidence.
