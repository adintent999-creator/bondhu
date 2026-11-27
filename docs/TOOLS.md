# Tool Registry (P0)

| # | id | risk | readonly | action |
|---|----|------|----------|--------|
| 1 | device_status | R0 | yes | model, OS, network |
| 2 | battery_detail | R0 | yes | battery percent, charging |
| 3 | get_time | R0 | yes | current time in Bengali digits |
| 4 | list_apps | R0 | yes | launchable apps |
| 5 | open_app | R1 | no | messages/dialer/camera/label match |
| 6 | set_torch | R1 | no | CameraManager torch |
| 7 | set_volume | R1 | no | media volume percent |
| 8 | set_brightness | R1 | no | system brightness |
| 9 | set_dnd | R2 | no | requires spoken "হ্যাঁ" + policy access |
| 10 | set_alarm | R2 | no | AlarmManager exact |
| 11 | set_timer | R2 | no | AlarmManager exact |
| 12 | remember | R1 | no | stores a memory row |

R2/R3 always produce `awaiting_approval` first, then `executed` only after confirmation.
Deny words: UPI, bKash, Nagad, bank, OTP, password, PIN, delete account, factory reset.
