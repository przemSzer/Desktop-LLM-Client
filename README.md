# Local AI Agent

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A desktop AI chat client for local and cloud models. Solo project: Java 26, JavaFX, LangChain4j, Maven multi-module, MVVM.

Connect to **Ollama**, **OpenAI**, **Anthropic**, or **Google Gemini**, stream replies, and optionally let the model use tools (web fetch, local commands, MCP).

## Features

- Streaming chat with conversation history
- Multi-provider connections (Ollama, OpenAI, Anthropic, Gemini)
- Tool use: MCP, local shell commands, web page download
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

This is an early preview (`0.1.0`), provided **as-is**.

- API keys are stored **locally** in `~/.local-ai/connections.json`. Treat that file as secret; do not commit or share it.
- When tools are enabled, the model can run **local commands** on your machine. Use at your own risk.
- See [SECURITY.md](SECURITY.md) for how to report issues.

## Screenshots

Add a PNG of the chat window here, for example `docs/screenshot-chat.png`:

```markdown
![Chat window](docs/screenshot-chat.png)
```

## Build

```bash
./mvnw clean compile
./mvnw test
./mvnw package
```

## License

[Apache License 2.0](LICENSE)
