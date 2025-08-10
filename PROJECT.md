# Personal AI Desktop Application

## Project Overview
**Working Title**: Personal AI / Personal Agent / Local AI (to be decided)

A lightweight desktop application providing access to LLMs, RAG, and Agents through a JavaFX GUI.

## Technology Stack
- **GUI**: JavaFX
- **Build System**: Maven
- **Architecture**: Modular design with core and UI components
- **Models**: Support for both remote APIs and local lightweight models

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