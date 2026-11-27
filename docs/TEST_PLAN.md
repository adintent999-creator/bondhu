# Test Plan

## Unit (`app/src/test`)
- Multi-step Bengali parsing (torch + open app).
- Bengali digit normalization.
- Kill switch detection.
- Deny-list policy.

## Manual device DoD
0. Confirm only one floating overlay exists (`ai.bondhu.assistant`); `com.maxorb` and `zyvo-orb` must be stopped.
1. Install APK, open onboarding, grant mic/camera/notifications/overlay.
2. Say: `টর্চ জ্বালাও, তারপর মেসেজ অ্যাপ খোলো`.
3. Expect: torch on, Messages opens, spoken reply, two `action_log` rows.
4. Say `থামো` mid-run → no further actions, `aborted` error row if applicable.
5. Open Diagnostics → confirm proof rows and latency.
6. Try `ব্যাঁক অ্যাপ খোলো` / `OTP পড়` → DENY row.

## Eval criteria
- Tool accuracy ≥95% on golden turns.
- Deny-list correctness 100%.
- Confirmation correctness 100%.
- No crash across 50 turns.
