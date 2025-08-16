package dev.local.ai.ui.commands;

/**
 * Base interface for all commands in the MVVM pattern.
 * Commands encapsulate actions that can be executed, undone, and validated.
 */
public interface ICommand {
    /**
     * Executes the command
     * @return true if execution was successful, false otherwise
     */
    boolean execute();
    
    /**
     * Undoes the command (if supported)
     * @return true if undo was successful, false otherwise
     */
    boolean undo();
    
    /**
     * Checks if the command can be executed
     * @return true if the command can be executed, false otherwise
     */
    boolean canExecute();
    
    /**
     * Gets a description of what the command does
     * @return command description
     */
    String getDescription();
    
    /**
     * Checks if the command supports undo
     * @return true if undo is supported, false otherwise
     */
    default boolean supportsUndo() {
        return false;
    }
}
