package dev.local.ai.ui.connection.openai;

import java.util.function.Function;

import dev.local.ai.core.connections.OpenAIConnection;
import dev.local.ai.ui.connection.INewConnectionDialog;

public class OpenAIConnectionDialog implements INewConnectionDialog<OpenAIConnection> {

    @Override
    public void show() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'show'");
    }

    @Override
    public void onCancel(Runnable onCancel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onCancel'");
    }

    @Override
    public void onSave(Function<OpenAIConnection, Boolean> onSave) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onSave'");
    }

}
