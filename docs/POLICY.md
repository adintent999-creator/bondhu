# Policy & Confirmation

- Voice/text intents pass through `PolicyEngine` before any tool runs.
- `Decision.DENY` is final; no confirmation can override it.
- `Risk.R2`/`R3` tools require explicit spoken "হ্যাঁ/yes/ঠিক আছে".
- "না/no/বাতিল" clears pending confirmation.
- "থামো/stop/cancel" aborts immediately via `SessionController.abort()`.
- Double screen-off within 450 ms also aborts and stops the service.
- Accessibility automation (P2) is user-initiated, ≤8 steps, and never touches password/OTP/payment fields.
- No silent messages, calls, payments, or account deletions in P0.
