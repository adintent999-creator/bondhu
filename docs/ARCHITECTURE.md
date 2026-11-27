# Bondhu Architecture (P0)

## Runtime
- `ai.bondhu.assistant` — Kotlin 2.x, Compose M3, Hilt, Room, DataStore, Keystore.
- minSdk 26, targetSdk 35. Builds on GitHub Actions only.

## Flow
`SpeechRecognizer(bn-BD)` → `LocalParser` → `PolicyEngine` → `TaskRunner` → `ToolRegistry`/`SystemToolExecutor` → `BondhuRepository.action()` → `TextToSpeech(bn-BD)`.

Every tool writes `action_log` with `tool, args_json, risk, decision, approved_by, status, proof_ref, latency_ms, error_code`. If no log row exists, the spoken claim is not allowed.

## Packages
- `core` — models, `SessionController` abort flag.
- `data` — Room entities/DAOs, DataStore, Keystore-encrypted key store, repositories.
- `domain` — parser, policy/deny-list, tool registry, system executor, `TaskRunner`.
- `voice` — `VoiceGateway`, local STT/TTS, Gemini Live adapter (activates only with user key).
- `platform` — foreground service, floating orb, double-power kill receiver.
- `ui` — Compose onboarding, home orb, diagnostics/audit, theme.

## P0 honesty note
No API key is bundled. P0 runs fully in local Bengali mode so it can be demonstrated and audited offline. `GeminiLiveGateway` is wired behind the same interface; adding a key in Diagnostics unlocks the realtime adapter path (live audio streaming is a P0-adapter, P1-hardening item).

## Evidence
Diagnostics screen shows `action_log` count and each proof row. `eval/golden_turns.yaml` is the source of truth for demo prompts; `eval/REPORT.md` records actual results only.
