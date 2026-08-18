# Desktop LLM Client

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A desktop chat client for multimodal LLMs. Solo project: Java 26, JavaFX, LangChain4j, Maven multi-module, MVVM.

Connect to **Ollama**, **OpenAI**, **Anthropic**, or **Google Gemini**, stream replies, and optionally let the model use tools (web fetch, local commands, MCP). Tool calling is supported; there is no standalone agent loop yet.

## Status

This is an early preview (`0.1.0`), not production software. I use it daily and it works well for me, but you should not rely on it for critical tasks. Fine for learning and personal experiments. See [Security](#security).

## Features

- Streaming chat with conversation history
- Multi-provider connections (Ollama, OpenAI, Anthropic, Gemini)
- Tool use: local shell commands, web page download, MCP (experimental)
- JavaFX desktop UI with MVVM (`core` domain, `ui` presentation)

## Prerequisites

- JDK **26** (`JAVA_HOME` pointing at it)
- Maven 3.9+ (or use the included wrapper)

## Run

Windows:

```bat
run.bat
```

Linux / macOS:

```bash
chmod +x run.sh
./run.sh
```

Maven wrapper:

```bash
./mvnw clean install
./mvnw javafx:run -pl ui
```

## Architecture

```
core/   domain: chat, connections, models, storage, tools
ui/     JavaFX views, ViewModels, FXML
```

`AppContext` wires long-lived services. The UI talks to `core` through ViewModels; persistence lives under the user home directory.

## Data on disk

| Path | Contents |
|------|----------|
| `~/.local-ai/` | Connections, settings, chats |
| `~/.ai-agent/` | Application logs |

## Security

Provided **as-is** under Apache 2.0.

- API keys are stored locally in `~/.local-ai/connections.json`. Treat that file as secret; do not commit or share it.
- When the command-line tool is enabled, the model can run local commands on your machine. Use at your own risk.
- When the web-page tool is enabled, fetched page content is sent to the model. There is no prompt-injection defense; malicious instructions on a page can steer the model (including into other enabled tools).
- How to report issues: [SECURITY.md](SECURITY.md).

## Screenshots

Add a PNG of the chat window here, for example `docs/screenshot-chat.png`:

```markdown
![Chat window](docs/screenshot-chat.png)
```

## Build

```bash
./mvnw clean test
./mvnw package
```

## License

[Apache License 2.0](LICENSE)
