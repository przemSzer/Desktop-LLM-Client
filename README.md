# Agent Project

A multi-module Java project with core and UI components built using JavaFX and following the MVVM pattern.

## Project Structure

```
agent/
├── core/           # Core business logic and models
├── ui/            # User interface components (JavaFX)
├── .vscode/       # Cursor IDE configuration
├── run.bat        # Windows run script
├── run.sh         # Unix/Linux run script
└── README.md      # This file
```

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Cursor IDE with Java extension

## Running the Application

### Option 1: Using Cursor IDE (Recommended)

1. **Open the project in Cursor IDE**
2. **Build the project first:**
   - Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
   - Type "Tasks: Run Task"
   - Select "maven-compile" or "build-project"

3. **Run the application:**
   - Press `F5` or go to Run and Debug panel
   - Select "Launch ChatApplication" or "Launch ChatApplication (Maven)"
   - Click the play button

### Option 2: Using Command Line

#### Windows:
```bash
run.bat
```

#### Unix/Linux:
```bash
chmod +x run.sh
./run.sh
```

### Option 3: Using Maven Wrapper

```bash
# Build the project
./mvnw clean install

# Run the JavaFX application
./mvnw javafx:run -pl ui
```

## Development

### Project Configuration

The project is configured with:

- **Java 21** as the target version
- **JavaFX 21.0.2** for the UI framework
- **Maven** for build management
- **SLF4J** for logging
- **JUnit 5** for testing

### IDE Configuration

The `.vscode` folder contains:

- `launch.json` - Debug configurations for running the application
- `tasks.json` - Build tasks for Maven operations
- `settings.json` - Java and Maven settings

### Key Features

- **MVVM Architecture**: The UI follows the Model-View-ViewModel pattern
- **Modular Design**: Core business logic is separated from UI components
- **JavaFX UI**: Modern desktop application interface
- **Maven Multi-module**: Clean separation of concerns

## Troubleshooting

### Common Issues

1. **JavaFX modules not found:**
   - Ensure you've run the build task first
   - Check that dependencies are copied to `ui/target/dependency/`

2. **Maven build fails:**
   - Verify Java 21 is installed and set as JAVA_HOME
   - Run `mvn clean` and try again

3. **Application won't start:**
   - Check the console for error messages
   - Ensure all dependencies are properly downloaded

### Debug Configuration

The launch configurations include:
- Proper JavaFX module path setup
- VM arguments for JavaFX modules
- Pre-launch tasks to ensure the project is built

## Building

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package the application
./mvnw package

# Install to local repository
./mvnw install
``` 