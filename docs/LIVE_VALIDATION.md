# Live Validation

Phase 6 validates the current Android app on a real emulator or device. The
goal is to prove that the app can be installed, launched, connected to the
AccessibilityService, and used through the core tool and agent-debug controls.

## Environment

Validated on:

- Android Emulator: `TouchPilot_API_35`
- Android version: 15
- App package: `dev.touchpilot.app`
- Build variant: debug

## Setup

Build and install the debug APK:

```bash
ANDROID_HOME=/path/to/Android/Sdk ./gradlew installDebug
```

Launch the app:

```bash
adb shell monkey -p dev.touchpilot.app 1
```

Enable the accessibility service manually from Android settings, or on a test
emulator:

```bash
adb shell settings put secure enabled_accessibility_services \
  dev.touchpilot.app/dev.touchpilot.app.androidcontrol.TouchPilotAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell input keyevent 3
adb shell monkey -p dev.touchpilot.app 1
```

The app should show:

```text
Accessibility service: connected
```

## Checklist

- [x] App installs with `installDebug`.
- [x] App launches to `dev.touchpilot.app/.MainActivity`.
- [x] AccessibilityService connects and the app status reflects it.
- [x] `Observe Current Screen` returns an accessibility tree snapshot.
- [x] `Open App` opens another app by launcher label, verified with `Settings`.
- [x] `Tap Text` executes through the accessibility bridge.
- [x] `Type Into Focused Field` executes through the accessibility bridge.
- [x] `Back` executes the Android global back action.
- [x] `Home` executes the Android global home action.
- [x] `Scroll Down` and `Scroll Up` move the debug screen.
- [x] `Wait For Text` waits for visible text and renders the result.
- [x] Medium-risk local-router tools request approval before execution.
- [x] Skill allowlists deny tools that are not allowed by the selected skill.
- [x] Local router mode can be selected from the agent-provider spinner.
- [x] Local router can run a conservative step loop without a cloud provider.
- [x] MCP client UI is reachable and exposes endpoint, list, call, tool, and
      argument controls.
- [x] Export debug trace writes a trace file under app external files.

## Live-Test Notes

During Phase 6 validation, the app successfully installed, launched, connected
to the accessibility service, opened Android Settings through `Open App`,
rendered the lower Agent/MCP/debug sections after scrolling, switched the agent
provider to `Local router`, and exported a debug trace.

The live test found two usability issues that were fixed as part of Phase 6:

- Text-entry actions were brittle in ADB-driven live tests because the soft
  keyboard could stay open and intercept taps. Action buttons now hide the
  keyboard before running.
- The debug screen had no stable resource IDs for automation. Core controls now
  have explicit IDs under `res/values/ids.xml`.

Android may stop an accessibility service after `am force-stop` during
automation. Prefer normal launch flows for manual testing, or re-enable the
service after force-stopping the app in emulator scripts.
