# NIHONGO QUEST

Premium offline-first Japanese learning app for Android.

The native Android project is built with Kotlin, Jetpack Compose and Room. See the build instructions below.

## Build

Open this repository in Android Studio (Ladybug or newer), allow Gradle sync, then run the `app` configuration on an Android device or emulator.

A GitHub Actions workflow builds a debug APK on every push and publishes it as an artifact.

## Current scope

The first implementation focuses on a polished dashboard, vocabulary management, kanji notes, flashcard review, quiz sessions, local persistence, and backup/export. More advanced reading, listening and analytics modules can be expanded without changing the core data layer.


## Automated build
Every push to `main` runs the Android build workflow. Download the `nihongo-quest-debug-apk` artifact from the Actions run when it completes.
