# Logging Configuration

This project uses SLF4J with Logback for logging. The logging configuration is set up to output to both console and a `local.log` file.

## Configuration Files

### Core Module (`core/src/main/resources/logback.xml`)
- Configures logging for the core business logic
- Sets `dev.local.ai` package to DEBUG level
- Sets `dev.langchain4j` package to INFO level

### UI Module (`ui/src/main/resources/logback.xml`)
- Configures logging for the user interface
- Sets `com.example.ui` package to DEBUG level
- Sets `javafx` package to WARN level

## Log Output

### Console Output
- Format: `HH:mm:ss.SSS [thread] LEVEL logger - message`
- Example: `14:30:25.123 [JavaFX Application Thread] INFO ChatController - Initializing ChatController`

### File Output (`local.log`)
- Format: `yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message`
- Example: `2024-01-15 14:30:25.123 [JavaFX Application Thread] INFO ChatController - Initializing ChatController`

## Log Levels

- **ERROR**: Critical errors that prevent normal operation
- **WARN**: Warning conditions that might indicate problems
- **INFO**: General information about application progress
- **DEBUG**: Detailed information for debugging purposes

## Log File Management

- **File**: `local.log` (created in the application's working directory)
- **Rolling**: Daily rotation with pattern `local.yyyy-MM-dd.log`
- **Retention**: 30 days of log files
- **Size Cap**: Maximum 100MB total for all log files

## Usage Examples

### In Java Classes

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void myMethod() {
        logger.debug("Debug information");
        logger.info("General information");
        logger.warn("Warning message");
        logger.error("Error message", exception);
    }
}
```

### Logging Best Practices

1. **Use appropriate log levels**:
   - DEBUG for detailed debugging information
   - INFO for general application flow
   - WARN for potential issues
   - ERROR for actual errors

2. **Include context in log messages**:
   - Use parameterized logging: `logger.info("Processing user: {}", userId)`
   - Avoid string concatenation in log statements

3. **Log exceptions properly**:
   - Pass the exception as the last parameter: `logger.error("Failed to process", e)`

4. **Use meaningful logger names**:
   - Use the class name: `LoggerFactory.getLogger(MyClass.class)`

## Environment Variables

The application requires the following environment variable for proper operation:
- `OPENAI_API_KEY`: Your OpenAI API key for the chat functionality

## Troubleshooting

### Logs not appearing in file
- Check that the application has write permissions in the working directory
- Verify that the `local.log` file is being created
- Check that the logback configuration files are in the correct locations

### Too much logging
- Adjust log levels in the `logback.xml` files
- Set specific package loggers to higher levels (e.g., WARN instead of DEBUG)

### Log file too large
- The configuration automatically rotates logs daily
- Old log files are automatically cleaned up after 30 days
- Total log size is capped at 100MB 