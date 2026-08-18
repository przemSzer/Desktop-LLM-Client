# Security policy

## Reporting a vulnerability

Please report security issues through **GitHub Issues** if they do not expose secrets, or through **GitHub private vulnerability reporting** (Security tab) when available.

Do not attach API keys, `connections.json`, logs that may contain secrets, or `~/.local-ai` dumps.

## Known limitations

- **Early preview.** This is not production software. Do not use it for critical tasks.
- **API keys at rest.** Provider keys are stored in `~/.local-ai/connections.json` on the local machine. Anyone with access to that file can use the keys.
- **Local command tool.** When enabled, the model can execute shell commands on the host. That is powerful and dangerous. Review tool use before enabling it.
- **Web page tool / prompt injection.** When enabled, downloaded page content is sent to the model. There is no sanitization or prompt-injection defense. A page can contain instructions that steer the model, including into other enabled tools.
- **No warranty.** The software is provided under the Apache 2.0 license **as-is**, without warranty of any kind.

You are responsible for provider accounts, API spend, and what the application does on your computer.
