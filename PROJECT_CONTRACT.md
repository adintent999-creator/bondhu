# Bondhu — PROJECT_CONTRACT (pinned 27 Nov 2026)

EXACT_USER_GOAL
- Build Bondhu: a voice-first Android phone agent. Bengali/Hindi/English code-mixed
  speech → the phone actually performs multi-step tasks in other apps → spoken
  confirmation in <3s → an `action_log` row proves what happened.

TARGET_DELIVERABLES (ordered)
1. P0: scaffold, onboarding, orb, VoiceSession, tools #1–7, action log + audit UI,
   kill switch, Diagnostics. Demo APK installed on phone.
2. P1: memory CRUD, WhatsApp send, notification auto-reply, calls, SMS default-role check.
3. P2: screen read, screenshot Q&A, bounded a11y executor (3 named flows).
4. P3: personas, research, docs, routines.
5. P4: Termux typed-argv runner, PC relay, screen share.
6. P5: release APK + store copy + demo video script.

HARD_CONSTRAINTS
- Stack: Kotlin 2.x, Compose M3, Hilt, Room, DataStore, Keystore, coroutines/Flow.
- minSdk 26, targetSdk 35. Package `ai.bondhu.assistant`.
- Gemini Live over OkHttp WebSocket is the primary realtime adapter; user-supplied
  key only, never bundled. Local SpeechRecognizer(bn-BD)+TTS mode is the P0 fallback
  so the app is demo-able with zero key.
- No fake success. Every spoken claim maps to `action_log`.
- R2/R3 tools require explicit spoken confirmation or visible confirm card.
- Deny-list: payments/UPI/bKash/bank, OTP/password/PIN, delete account, factory reset.
- Kill switch <200ms: voice "থামো/stop/cancel" + double Power press.
- a11y runs are user-initiated, ≤8 steps, logged, abortable.
- Build on GitHub Actions; never run Gradle on the phone.

FORBIDDEN_DRIFT
- No social agents, video editing, smart-home, app-store distribution in P0–P3.
- No hardcoded API keys/secrets in repo.
- No claims of "done" without action_log evidence.

DONE_CRITERIA (P0)
- "টর্চ জ্বালাও, তারপর মেসেজ অ্যাপ খোলো" executes both with two action_log rows,
  spoken confirmation, and the debug APK runs on the real phone.
- Kill switch, confirmation gate, onboarding, Diagnostics all verified live.

STATUS
- [x] Contract pinned
- [ ] P0 scaffold + tool registry
- [ ] P0 GitHub build
- [ ] P0 install + live demo
- [ ] P1 comms
- [ ] P2 screen/hands
- [ ] P3 personality/research
- [ ] P4 power features
- [ ] P5 distribution

NOW: Starting P0 scaffold.
