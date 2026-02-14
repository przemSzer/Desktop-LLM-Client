# Personal AI Desktop Application

## Project Overview
**Working Title**: Personal AI / Personal Agent / Local AI (to be decided)

A lightweight desktop application providing access to LLMs, RAG, and Agents through a JavaFX GUI.

## Project Summary (Architecture & Implementation)

The project is a **multi-module Java 21** desktop application that provides a **chat interface to LLMs** (local and remote) via a **JavaFX** GUI. It is structured as a lightweight, configurable client with clear separation between core logic and UI, suitable for learning and extension.

### Architecture

- **Two Maven modules:** **`core`** (business logic, LLM integration, storage, events, tools) and **`ui`** (JavaFX UI, MVVM ViewModels/Controllers, FXML views).
- **UI pattern:** MVVM (View = FXML + Controller, ViewModel = state/commands, Model = core).
- **Decoupling:** `CoreEventBus` for cross-component events (e.g. LLM change); command pattern for undo/redo.

### Core Module (`core/`)

**Chat:** `ILLMChat` interface; `Chat` (non-streaming) and `StreamingChat` (streaming, tools, partial updates). `DefaultChats` factory; default is streaming Ollama `gemma3n:latest`.

**Connections:** `ModelProviderConnection` (sealed: `OllamaConnection`, `OpenAIConnection`); `ConnectionsStore` with `DataStorage` (e.g. `JsonFileStorage` under `~/.local-ai/connections.json`).

**Models:** `StreamingChatModelsProvider` builds LangChain4J models; `AvailableModelsService` + `OllamaModelService` / `OpenAIModelService`; `LLMInfoAndConnection` pairs model with connection.

**Events:** `CoreEventBus` (async pub/sub); `LLMChangedEvent` for model switch; `StreamingChat` subscribes and switches model.

**Storage:** `DataStorage` / `JsonFileStorage` for connections.

**Documents:** `DocumentAnalyser` (Apache Tika) and `DocumentDescription` for attachments and system message context.

**Tools:** `IToolExecutor`; `WebPageDownloaderTools` (singleton) – `downloadWebPage(url)` via LangChain4J; used by `StreamingChat` for tool calls.

### UI Module (`ui/`)

**Entry:** `MainApplication` loads `ChatWindow.fxml`; `ChatController` creates `ChatViewModel` with `DefaultChats.defaultChat()` and `CommandManager`.

**Chat MVVM:** `ChatViewModel` implements `IChatListener` and `IPartialMessagesListener`; observables for messages, input, status, attachments, undo/redo; `SendUserMessageToLLMCommand` (async), `ClearChatCommand`; `MessageCell` + `UserMessageControl`, `AIMessageControl`, `PartialMessageControl`, `ToolMessageControl`.

**Commands:** `ICommand`, `CommandManager` (undo/redo stacks, async execution).

**Connections UI:** `ConnectionsViewController` / `ConnectionsViewModel`; add-connection dialogs for Ollama and OpenAI.

**Model selector:** `LLMSelectorView` + `LLMSelectorViewModel`; loads connections and models; publishes `LLMChangedEvent` on model selection.

**File attachments:** `FileAttachmentControl`, `AttachedFileViewModel`; `PrepareFileToBeUsedByLLM` for document descriptions.

### Technology Stack

| Area        | Technology |
|------------|------------|
| Language   | Java 21    |
| Build      | Maven (multi-module) |
| UI         | JavaFX 21 (FXML, controls, graphics) |
| LLM        | LangChain4J 1.4 (OpenAI, Ollama; streaming + tools) |
| Documents  | Apache Tika, LangChain4J document parser/transformer (Jsoup) |
| Config     | Typesafe Config |
| Logging    | SLF4J + Logback |
| Storage    | JSON (Jackson) in `~/.local-ai` |
| Testing    | JUnit 5, Mockito, AssertJ |

### Data and Event Flow

1. **Sending a message:** User input + optional attachments → ChatController → ChatViewModel.sendMessage() → SendUserMessageToLLMCommand (async) → StreamingChat → LangChain4J streaming + optional tool calls (e.g. downloadWebPage) → IChatListener / IPartialMessagesListener → ChatViewModel updates chatMessages → list and cells refresh.
2. **Changing LLM:** User selects connection/model in LLMSelectorView → LLMSelectorViewModel publishes LLMChangedEvent → StreamingChat.changeModel() replaces StreamingChatModel.
3. **System message:** Typed in accordion + optional file attachments; ChatViewModel builds Message with document descriptions and calls chat.setSystemMessage().

### Testing

Core: e.g. ConnectionSerializationTest, DocumentAnalyserTest, JsonFileStorageTest. UI: ChatViewModelTest, ConnectionsViewModelTest, AgentUITest (JUnit 5; BDD-style Mockito where used).

### Notable Details

- Default runtime: streaming chat with local Ollama `gemma3n:latest` at `http://localhost:11434`; OpenAI variants require `OPENAI_API_KEY`.
- Undo/Redo: wired in UI and CommandManager; send and clear are command-based.
- Only StreamingChat implements model switch on LLMChangedEvent; non-streaming Chat’s changeModel is not implemented.
- Persistence: only connections (JSON); chat history is in-memory (message window).

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


