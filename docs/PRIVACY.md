# Privacy (P0)

- No analytics, no telemetry, no cloud sync by default.
- Gemini key, if supplied, is encrypted with Android Keystore AES-GCM and stored in DataStore as ciphertext.
- `action_log`, `messages`, `memory`, `reply_log` live only in local Room DB (`bondhu.db`).
- No screenshots/contact uploads in P0.
- Floating orb requires the user to grant overlay permission explicitly.
- Deny-list blocks money/OTP/password/factory-reset intents before execution.
