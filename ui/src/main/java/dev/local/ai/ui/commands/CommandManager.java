package dev.local.ai.ui.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Manages command execution, history, and undo/redo functionality.
 * Provides a centralized way to execute commands and maintain 
 * command history.
 */
public class CommandManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    
    private final Stack<ICommand> undoStack = new Stack<>();
    private final Stack<ICommand> redoStack = new Stack<>();
    private final ExecutorService executor;
    
    public CommandManager() {        
        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                var thread = new Thread(r, "command-execution");
                thread.setUncaughtExceptionHandler((t,ex)->{
                    logger.error("Uncaught exception in command execution thread", ex);
                });
                return thread;
            }
        });
    }
    
    /**
     * Executes a command synchronously
     * @param command the command to execute
     * @return true if execution was successful, false otherwise
     */
    public boolean executeCommand(ICommand command) {
        if (command == null) {
            logger.warn("Attempted to execute null command");
            return false;
        }
        
        if (!command.canExecute()) {
            logger.warn("Cannot execute command: {}", command.getDescription());
            return false;
        }
        
        try {
            logger.debug("Executing command: {}", command.getDescription());
            boolean success = command.execute();
            
            if (success && command.supportsUndo()) {
                undoStack.push(command);
                redoStack.clear(); // Clear redo stack when new command is executed
                logger.debug("Command added to undo stack: {}", command.getDescription());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error executing command: {}", command.getDescription(), e);
            return false;
        }
    }
    
    /**
     * Executes a command asynchronously
     * @param command the command to execute
     * @return CompletableFuture that completes with the execution result
     */
    public CompletableFuture<Boolean> executeCommandAsync(ICommand command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command), executor);
    }
    
    /**
     * Undoes the last executed command
     * @return true if undo was successful, false otherwise
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            logger.debug("No commands to undo");
            return false;
        }
        
        ICommand command = undoStack.pop();
        try {
            logger.debug("Undoing command: {}", command.getDescription());
            boolean success = command.undo();
            
            if (success) {
                redoStack.push(command);
                logger.debug("Command moved to redo stack: {}", command.getDescription());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error undoing command: {}", command.getDescription(), e);
            return false;
        }
    }
    
    /**
     * Redoes the last undone command
     * @return true if redo was successful, false otherwise
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            logger.debug("No commands to redo");
            return false;
        }
        
        ICommand command = redoStack.pop();
        try {
            logger.debug("Redoing command: {}", command.getDescription());
            boolean success = command.execute();
            
            if (success && command.supportsUndo()) {
                undoStack.push(command);
                logger.debug("Command moved back to undo stack: {}", command.getDescription());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error redoing command: {}", command.getDescription(), e);
            return false;
        }
    }
    
    /**
     * Checks if undo is available
     * @return true if there are commands to undo, false otherwise
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Checks if redo is available
     * @return true if there are commands to redo, false otherwise
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Gets the number of commands in the undo stack
     * @return number of commands that can be undone
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * Gets the number of commands in the redo stack
     * @return number of commands that can be redone
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    /**
     * Clears all command history
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        logger.info("Command history cleared");
    }
    
    /**
     * Shuts down the command manager and executor service
     */
    public void shutdown() {
        executor.shutdown();
        logger.info("CommandManager shutdown");
    }
}
