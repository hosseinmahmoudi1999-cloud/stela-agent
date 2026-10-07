# Stela Agent

Stela Agent is a native Android AI assistant focused on Persian-language tasking, local execution planning, and deterministic Android actions. It is designed as a production-ready foundation for AI-assisted local automation with a clean Compose UI, state-driven execution flow, and secure API key handling.

## Project overview

- Native Android app for Kotlin + Jetpack Compose
- Persian RTL-first UI shell
- Structured execution state and plan model
- Local tool registry and task execution flow
- Room-based task history persistence
- DataStore-based settings
- Secure Gemini API key storage using AndroidX Security
- AccessibilityService setup for future automation support
- Voice input support using Android SpeechRecognizer

## Architecture

The app is organized into a small layered structure:

- app/src/main/java/com/stela/agent/MainActivity.kt: Compose entry point
- app/src/main/java/com/stela/agent/AppViewModel.kt: UI state and command orchestration
- app/src/main/java/com/stela/agent/agent/: agent state machine, execution events, tool registry
- app/src/main/java/com/stela/agent/domain/: agent domain models
- app/src/main/java/com/stela/agent/data/: Room database and DataStore settings
- app/src/main/java/com/stela/agent/network/: AI provider abstraction and Gemini implementation
- app/src/main/java/com/stela/agent/accessibility/: service and controller support
- app/src/main/java/com/stela/agent/security/: encrypted API key storage
- app/src/main/java/com/stela/agent/voice/: voice input integration

## Requirements

- Android Studio or Gradle command line
- JDK 21
- Android SDK with platform 35 and build-tools 35.0.0
- Internet access for Gemini API calls when enabled
- Android device for runtime permission and accessibility scenarios

## Setup

1. Install the Android SDK and set `sdk.dir` in `local.properties`.
2. Use Java 21:
   - export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.12+1-ms
   - export PATH="$JAVA_HOME/bin:$PATH"
3. Sync the project:
   - ./gradlew help

## Build

Debug build:

./gradlew assembleDebug

## Test

Unit tests:

./gradlew testDebugUnitTest

Lint:

./gradlew lintDebug

## Gemini configuration

The app does not hard-code any Gemini key. Configure the key at runtime in Settings, and it is stored securely in EncryptedSharedPreferences.

Recommended flow:

- Open Settings in the app
- Enter the Gemini API key
- Save the key
- Ensure the key is valid and has quota access

## Permissions

The app requests runtime permissions only when needed:

- RECORD_AUDIO for voice input
- POST_NOTIFICATIONS for notification-related workflows
- Accessibility capability through Android Settings when automation is enabled

## AccessibilityService setup

The app declares a basic accessibility service in the manifest and exposes its configuration under:

- app/src/main/res/xml/accessibility_service_config.xml

To use it on-device:

1. Open Android Settings
2. Go to Accessibility
3. Enable Stela Agent
4. Grant the service permission as required by the OS

## Troubleshooting

- If Gradle fails due to Java mismatch, ensure Java 21 is active.
- If Android SDK is missing, install platform 35 and build-tools 35.0.0.
- If Compose resources fail to link, run a clean build after SDK setup.
- If Gemini requests fail, check API key validity, network connectivity, and quota status.

## Known limitations

- This repository is a solid foundation rather than a full end-to-end device automation system.
- Full accessibility automation and agent execution require a real Android device and user permission.
- The Gemini integration is structured for future provider expansion but still depends on a valid API key and live network access.
- Some advanced local automation behaviors require Android accessibility support and app-specific accessibility semantics from the target apps.

## Security notes

- API keys are kept out of source control.
- Sensitive API data uses encrypted preferences.
- Avoid logging raw keys or user secrets.
- Do not commit local.properties with secrets or machine-specific paths beyond the SDK path.
