package dev.local.ai.ui.connection;

import java.util.function.Function;

import dev.local.ai.core.connections.ModelProviderConnection;

public interface INewConnectionDialog<T extends ModelProviderConnection> {

    void show();
    void onSave(Function<T, Boolean> onSave);
    void onCancel(Runnable onCancel);
}
