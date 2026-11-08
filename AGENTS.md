# Working on Sketch TV

## Project map

- `project.json`: shared source of truth for layout, actions, theme and widget settings.
- `tools/configure.py`: validation + generated Java in `out/gen/`; do not edit generated files.
- `src/dev/sketchtv/home/`: native Android app (Java 8 syntax, JDK 17, SDK 34, min API 21).
- `HomeView`: retained Canvas renderer + native clickable/focusable targets.
- `WidgetProvider` / `BuiltinWidgets` / `BuiltinParsers`: contracts and providers.
- `WidgetEngine`: JVM-testable scheduling/cache; `WidgetRepository`: Android background I/O.
- `tools/preview.py` + `preview/`: local studio; no TV or external-provider requests.
- `tools/device.py`: explicit, reversible device tuning; use fake ADB tests while developing.
- `docs/ARCHITECTURE.md`, `docs/DEVICE.md`, `docs/AUDIT.md`: decisions, limits and findings.

## Development workflow

1. Read the relevant source/tests; preserve unrelated changes.
2. Prefer JSON edits for layout work. Preserve the shared validator/geometry/focus contract.
3. Use `python3 tools/check.py` for core regression checks. JDK 17 must be on PATH.
4. If parsers change: `JSON_JAR=/path/json-20240303.jar sh tests/test_parsers.sh`.
5. If studio changes: `CHROMIUM=/path/chromium python3 tests/test_preview.py`.
6. If Android changes: `sh build.sh`, then verify `out/tvhome.apk` with SDK apksigner.
7. Report actual tests and limitations. JVM/browser checks are not physical TV verification.

No dependency downloads, package installation or network access are hidden inside the core checker. CI handles its own dependencies. Don't claim more RAM or faster navigation without comparable measurements.

## Device operations and private data

Code work does not authorize connecting to a real device. Require an explicitly provided target and permission for inspection; never discover or connect to arbitrary LAN devices. A request to inspect does not authorize modifications.

For any real setting change, present the exact plan, obtain separate confirmation for each change, persist original values BEFORE writes and verify afterward. Use the interactive `tools/device.py` flow. Do not pipe confirmation, add `--yes`, bypass permissions or modify the allowlist to circumvent a refusal. Existing user approval applies to the exact operation already approved.

Never disable/remove system apps, Google Play services, Play Store, Settings, System UI, setup, keyboards, Bluetooth, launchers, HDMI/input/tuner/boot frameworks or unknown packages. The supported tuning tool changes only animation scales and sleep timeout; it never controls screensaver, package state, accessibility, default Home or reboot. Device-specific debloating is deliberately outside its scope.

Keep model/serial/IP, snapshots, plans, journals, logs, screenshots and benchmarks in `.local/` or outside the checkout. Never add them to Git, CI artifacts or issue bodies. Never read or publish keystores, tokens or `.env` files. Use synthetic test data. Public code must remain neutral.

After real-device verification, remind the owner to turn off USB/network/wireless debugging. Preserve screensaver unless a separate informed decision has been made; never infer OLED technology from a generic model string.

## Runtime and quality boundaries

- No package-manager, filesystem cache parsing or network work in draw/onResume.
- Keep recurring work paused offscreen and schedule at actual provider deadlines.
- Bound response size, bitmap dimensions, text caches, retry rate and I/O timeouts.
- Every widget needs source, URL, license/conditions, timestamp, offline/error behavior and parser tests.
- Respect API 21 guards; compiling against SDK 34 does not prove older runtime compatibility.
- Preserve MIT attribution and Poppins OFL; do not bundle third-party marks without appropriate rights.
- Don't force-push. Publish only within the user's requested scope; prefer a reviewable branch/PR for substantial changes.
