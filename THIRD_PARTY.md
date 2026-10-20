# Public sources referenced (patterns only — not full vendored trees)

| Project | URL | Used ideas |
|---------|-----|------------|
| AhMyth Android RAT | https://github.com/AhMyth/AhMyth-Android-RAT | Client feature set (mic, camera, contacts) |
| AhMyth revival | https://github.com/Morsmalleo/AhMyth | Maintained fork notes |
| AndroRAT | https://github.com/karma9874/AndroRAT | Socket RAT client features (mic, SMS, loc, files) |
| AndroRAT classic | https://github.com/The404Hacking/AndroRAT | Contacts/call/SMS monitoring patterns |

This APK uses **Telegram + HTTP gate** as C2 (not AhMyth socket.io / AndroRAT python server).
Feature modules were re-implemented against current Android APIs inside `com.system.update`.
