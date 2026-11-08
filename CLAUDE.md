# Sketch TV — Claude Code

@AGENTS.md

Use the shared instructions above; do not duplicate or bypass their device/privacy rules.

Quick start: `python3 tools/check.py` (core checks, no device connection).
Preview: `python3 tools/preview.py` → http://127.0.0.1:8765.

User-invoked project skills:
- `/sketch-tv <goal>`: implement and verify a sketch/layout/widget change.
- `/device-audit <goal>`: prepare an inspection/optimization workflow; actual device access requires explicit target and permission.

Keep personal context in ignored `CLAUDE.local.md` and `.claude/settings.local.json`, never here. This project does not auto-approve tools, install hooks, configure credentials or require a particular Claude model. See `docs/CLAUDE_CODE.md` for setup and examples.
