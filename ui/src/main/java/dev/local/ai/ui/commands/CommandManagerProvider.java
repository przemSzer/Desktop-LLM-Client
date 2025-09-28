package dev.local.ai.ui.commands;

public class CommandManagerProvider {

    private static class InternalInstanceHolder {
        private static final CommandManager INSTANCE = new CommandManager();
    }

    public static CommandManager get(){
        return InternalInstanceHolder.INSTANCE;
    }
}
