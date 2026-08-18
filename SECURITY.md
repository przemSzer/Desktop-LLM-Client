# Security policy

## Reporting a vulnerability

Please report security issues through **GitHub Issues** if they do not expose secrets, or through **GitHub private vulnerability reporting** (Security tab) when available.

Do not attach API keys, `connections.json`, logs that may contain secrets, or `~/.local-ai` dumps.

## Known limitations

- **API keys at rest.** Provider keys are stored in `~/.local-ai/connections.json` on the local machine. Anyone with access to that file can use the keys.
- **Local command tool.** When enabled, the model can execute shell commands on the host. That is powerful and dangerous. Review tool use before enabling it.
- **No warranty.** The software is provided under the Apache 2.0 license **as-is**, without warranty of any kind.

You are responsible for provider accounts, API spend, and what the agent does on your computer.
