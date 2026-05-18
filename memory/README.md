# Memory

This module contains the first skill-loading path for the Android app.

Phase 3 packages starter Markdown skills as Android assets under
`app/src/main/assets/skills`. The app loads each `SKILL.md`, parses the title
and `Allowed initial tools` section, and passes the selected skill into the
agent prompt.

The selected skill also acts as a tool allowlist. If a model requests a tool
outside the active skill, the agent denies the call before approval or
execution.
