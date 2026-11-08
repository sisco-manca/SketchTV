---
name: device-audit
description: Prepare measured, reversible Android TV tuning with explicit target selection and per-change consent.
disable-model-invocation: true
---

Read AGENTS.md and docs/DEVICE.md first.

Goal: $ARGUMENTS

1. Without explicit permission and a user-provided serial, work on docs or fake-device tests only. Never scan the LAN or infer a target from old notes.
2. For authorized inspection, use tools/device.py inspect with explicit --serial. Keep outputs local/private. Do not paste raw reports into issues or Git.
3. Separate observed measurements, firmware limits and hypotheses. Inspect MemAvailable and app PSS/frame stats; do not equate MemFree with speed.
4. Generate a plan, explain each exact change and its undo, and let the owner use the interactive apply command. Do not supply SI on the owner's behalf. A plan is not permission to apply it.
5. Preserve original null values as absent settings on undo; respect wrong-device and drift checks. Never broaden the allowlist or disable packages to gain RAM.
6. Compare under equivalent conditions, verify actual standby/return Home manually, and remind the owner to turn debugging off. Reboots, package changes and screensaver changes are not performed by this skill/tool.
