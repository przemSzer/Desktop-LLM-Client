# Personal AI Desktop Application

## Project Overview
**Working Title**: Personal AI / Personal Agent / Local AI (to be decided)

A lightweight desktop application providing access to LLMs, RAG, and Agents through a JavaFX GUI.

## Project Summary (Architecture & Implementation)

The project is a **multi-module Java 21** desktop application that provides a **chat interface to LLMs** (local and remote) via a **JavaFX** GUI. It is structured as a lightweight, configurable client with clear separation between core logic and UI, suitable for learning and extension.

### Architecture

- **Two Maven modules:** **`core`** (business logic, LLM integration, storage, events, tools) and **`ui`** (JavaFX UI, MVVM ViewModels/Controllers, FXML views).
- **UI pattern:** MVVM (View = FXML + Controller, ViewModel = state/commands, Model = core).
- **Composition root:** `AppContext` (in `ui/dev.local.ai.context`) constructs and owns all long-lived services (event bus, storage, connections store, tool provider, command manager, etc.). It implements `AutoCloseable`; `MainApplication.stop()` closes it. A `ControllerFactory` (set on every `FXMLLoader`) constructor-injects controllers with dependencies from `AppContext` — no static singletons in application code.
- **Decoupling:** `CoreEventBus` for cross-component events (e.g. LLM change, tool selection, stop request); command pattern for undo/redo.

### Core Module (`core/`)

**Chat:** `ILLMChat` interface; `Chat` (non-streaming) and `StreamingChat` (streaming, tools, partial updates). `StreamingChat` is constructor-injected with `StreamingChatModel`, `IToolProvider`, `CoreEventBus`, and `StreamingChatModelsProvider`; it subscribes to `LLMChangedEvent` (model switch) and `StopRequestEvent`.

**Connections:** `ModelProviderConnection` (sealed: `OllamaConnection`, `OpenAIConnection`, `GoogleConnection`); `ConnectionsStore` (with `findById`) backed by `DataStorage` / `JsonFileStorage` at `~/.local-ai/connections.json`.

**Models:** `StreamingChatModelsProvider` builds LangChain4J streaming models; `AvailableModelsService` is implemented per provider (`OllamaModelService`, `OpenAIModelService`, `GoogleGeminiModelService`) and dispatched by `AvailableModelsServiceFactory`; `LLMInfoAndConnection` pairs `ModelInfo` with a concrete connection.

**Events:** `CoreEventBus` (async pub/sub, cached thread pool); listeners receive `LLMChangedEvent`, `StopRequestEvent`, `ToolsSelectionChangedEvent`.

**Storage:** `DataStorage` / `JsonFileStorage` for connections (`~/.local-ai/connections.json`); `SettingsStorage` / `JsonSettingsStorage` for user settings (`~/.local-ai/settings.json`). `LastSelectedModel` listens for `LLMChangedEvent` and persists `(ModelInfo, connectionId)` to settings; on startup it resolves the full `ModelProviderConnection` via `ConnectionsStore.findById`.

**Documents:** `DocumentAnalyser` (Apache Tika) and `DocumentDescription` for attachments and system message context.

**Tools:** `IToolProvider` (descriptors + specifications + executors) with three implementations:
- `DefaultToolsProvider` — bundles built-in tools (`WebPageDownloaderTools`, `CommandLineTools`).
- `ToolsProviderWithMCP` — built-in tools plus a single MCP server reached over Streamable HTTP (`AutoCloseable`, closed by `AppContext`).
- `FilterableToolProvider` — decorator wired to the event bus; toggles tools on/off from the UI via `ToolsSelectionChangedEvent`.

### UI Module (`ui/`)

**Entry:** `MainApplication.start()` builds `AppContext`, creates a `ControllerFactory(appContext)` and sets it on the `FXMLLoader` for `ChatWindow.fxml`. `stop()` calls `appContext.close()` (shuts down the event bus, closes MCP, etc.). The factory injects controllers explicitly — no `DefaultChats`, no static singletons. When no model has been selected yet, the factory builds a fallback chat against local Ollama `gemma3n:latest`.

**Chat MVVM:** `ChatController(ChatViewModel, IToolProvider, CoreEventBus, ConnectionsStore)` wires its child views (`ToolsSelectorView.init(...)`, `LLMSelectorView.init(...)`) with their dependencies. `ChatViewModel(ILLMChat, CommandManager, CoreEventBus)` implements `IChatListener` and `IPartialMessagesListener`; exposes observables for messages, input, status, attachments, undo/redo; commands: `SendUserMessageToLLMCommand` (async), `ClearChatCommand`; cells: `MessageCell` + `UserMessageControl`, `AIMessageControl`, `PartialMessageControl`, `ToolMessageControl`.

**Commands:** `ICommand`, `CommandManager` (undo/redo stacks, async execution).

**Connections UI:** `ConnectionsViewController` / `ConnectionsViewModel`; add-connection dialogs for Ollama, OpenAI, and Google.

**Model selector:** `LLMSelectorView` + `LLMSelectorViewModel(ConnectionsStore, CoreEventBus)`; loads connections and models, publishes `LLMChangedEvent` on selection.

**Tools selector:** `ToolsSelectorView` + `ToolsSelectorViewModel(IToolProvider, CoreEventBus)`; publishes `ToolsSelectionChangedEvent`, which `FilterableToolProvider` consumes to filter what the LLM sees.

**File attachments:** `FileAttachmentControl`, `AttachedFileViewModel`; `PrepareFileToBeUsedByLLM` for document descriptions.

### Technology Stack

| Area        | Technology |
|------------|------------|
| Language   | Java 21    |
| Build      | Maven (multi-module) |
| UI         | JavaFX 21 (FXML, controls, graphics) |
| LLM        | LangChain4J 1.13 (OpenAI, Ollama, Google Gemini, Anthropic; streaming + tools) |
| Tools      | Built-in (web download, command line) + optional MCP server (Streamable HTTP) |
| Documents  | Apache Tika, LangChain4J document parser/transformer (Jsoup) |
| Config     | Typesafe Config |
| Logging    | SLF4J + Logback |
| Storage    | JSON (Jackson) in `~/.local-ai` |
| Testing    | JUnit 5, Mockito, AssertJ |

### Data and Event Flow

1. **Sending a message:** User input + optional attachments → `ChatController` → `ChatViewModel.sendMessage()` → `SendUserMessageToLLMCommand` (async) → `StreamingChat` → LangChain4J streaming + optional tool calls (e.g. `downloadWebPage`, `executeLocalCommand`, MCP tools) → `IChatListener` / `IPartialMessagesListener` → `ChatViewModel` updates `chatMessages` → list and cells refresh.
2. **Changing LLM:** User selects connection/model in `LLMSelectorView` → `LLMSelectorViewModel` publishes `LLMChangedEvent` → both `StreamingChat.changeModel()` (replaces `StreamingChatModel`) and `LastSelectedModel` (persists `(ModelInfo, connectionId)`) react.
3. **Toggling tools:** User clicks in `ToolsSelectorView` → `ToolsSelectorViewModel` publishes `ToolsSelectionChangedEvent` → `FilterableToolProvider` updates the enabled set → next LLM call only sees enabled tools.
4. **System message:** Typed in accordion + optional file attachments; `ChatViewModel` builds `Message` with document descriptions and calls `chat.setSystemMessage()`.

### Testing

Core: e.g. ConnectionSerializationTest, DocumentAnalyserTest, JsonFileStorageTest. UI: ChatViewModelTest, ConnectionsViewModelTest, AgentUITest (JUnit 5; BDD-style Mockito where used).

### Notable Details

- **First-launch fallback:** if no model has been selected yet, `ControllerFactory` builds a chat against local Ollama `gemma3n:latest` at `http://localhost:11434` (temporary; the long-term plan is an empty-state UI prompting to add a connection).
- **Undo/Redo:** wired in UI and `CommandManager`; send and clear are command-based.
- Only `StreamingChat` implements model switch on `LLMChangedEvent`; non-streaming `Chat`'s `changeModel` is not implemented.
- **Persistence:** connections (`connections.json`) and last selected model in settings (`settings.json`); chat history is in-memory (message window).
- **MCP:** initialized synchronously in `ToolsProviderWithMCP` constructor (default URL `http://localhost:8088/mcp`, 30 s timeout); if the server is unreachable the provider degrades to local tools only. Async/event-driven init is a planned improvement.

---

## Key Design Principles
- Lightweight desktop application with easy installation and use
- Configurable model parameters (URL, API Key, local model downloads)
- Well-organized codebase suitable for educational purposes
- Potential foundation for a learning course

## Planned Functionality (Phased Approach)

### Phase 1: Chat
- Basic chat functionality with defined LLMs
- Model configuration and management
- Simple conversation interface

# version 0.1.0
- LangChain4J setup
- Simple connection to OpenAI model (parameters stored in file)
- Visualisation of a conversation in UI (in simplest possible way)

### Phase 2: Tools
- Integration of tools in conversations
- Tool execution and response handling
- Enhanced conversation capabilities

### Phase 3: Simple Agent
- Agent implementation with basic reasoning
- Task execution and planning
- Agent configuration and management

### Phase 4: RAG (Retrieval-Augmented Generation)
- Document ingestion and processing
- Vector storage and retrieval
- Document-based conversations
- Knowledge base management

## Current Project Structure
```
agent/
├── core/           # Core business logic and model interfaces
├── ui/            # JavaFX UI components
├── pom.xml        # Parent POM
└── README.md
```

## Development Guidelines
- Follow MVVM pattern for UI logic
- Maintain clean separation between core and UI modules
- Design for educational value and learning
- Focus on ease of use and installation 


