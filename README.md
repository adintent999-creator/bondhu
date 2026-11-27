# Bondhu

Voice-first Android phone agent. Bengali-first. Every executed action writes an `action_log` row.

P0 runs fully offline with Android `SpeechRecognizer(bn-BD)` + TTS, no bundled API key.
A Gemini Live adapter is included behind the same `VoiceGateway` interface and activates
only when the user supplies a key in Diagnostics (stored encrypted in Android Keystore).

Contract: `PROJECT_CONTRACT.md`. Tasks: `TASK.md`. Architecture: `docs/ARCHITECTURE.md`.
Build: GitHub Actions `.github/workflows/build.yml`; output artifact `bondhu-debug-apk`.
