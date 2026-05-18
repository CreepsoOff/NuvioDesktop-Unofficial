Generate commit messages using Conventional Commits.

Format:
type(scope): short imperative summary

Allowed types:
- fix
- feat
- chore
- refactor
- test
- docs
- build

Rules:
- Keep the subject under 72 characters.
- Be specific, not generic.
- Do not mention AI, Copilot, Codex, Claude, or generated code.
- Do not include secrets, tokens, local paths, or logs.
- For Nuvio Desktop changes, prefer scopes like desktop, player, mpv, trakt, packaging, settings.
- For mediamp submodule changes, prefer scope mpv or mediamp.
- For submodule pointer updates, mention the submodule and why it changed.