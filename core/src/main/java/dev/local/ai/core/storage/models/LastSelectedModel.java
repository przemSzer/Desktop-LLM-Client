package dev.local.ai.core.storage.models;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.core.storage.SettingsStorage;

/**
 * Remembers which {@link LLMInfoAndConnection} the user last selected and
 * persists that choice across application restarts.
 *
 * <p>Persists only {@code (modelInfo, connectionId)} so that the connection
 * itself (with API keys) is not duplicated outside of {@link ConnectionsStore}.
 * On read, the connection is resolved via {@link ConnectionsStore#findById};
 * if it no longer exists the selection becomes {@link Optional#empty()}.
 */
public class LastSelectedModel {

    private static final Logger logger = LoggerFactory.getLogger(LastSelectedModel.class);
    private static final String STORAGE_KEY = "lastSelectedModel";

    private final SettingsStorage settingsStorage;
    private final ConnectionsStore connectionsStore;
    private volatile LLMInfoAndConnection current;

    public LastSelectedModel(CoreEventBus eventBus,
                             SettingsStorage settingsStorage,
                             ConnectionsStore connectionsStore) {
        this.settingsStorage = settingsStorage;
        this.connectionsStore = connectionsStore;
        this.current = loadFromStorage().orElse(null);
        eventBus.subscribe(LLMChangedEvent.EVENT_TYPE, this::onLLMChanged);
    }

    /**
     * @return the last selected model and its connection, or empty if nothing
     *         has ever been selected or the connection has since been removed.
     */
    public Optional<LLMInfoAndConnection> get() {
        return Optional.ofNullable(current);
    }

    private void onLLMChanged(LLMChangedEvent event) {
        LLMInfoAndConnection updated = event.getModelInfo();
        if (updated == null || updated.connection() == null || updated.modelInfo() == null) {
            logger.warn("Ignoring LLMChangedEvent with incomplete payload: {}", updated);
            return;
        }
        this.current = updated;
        settingsStorage.save(STORAGE_KEY, new PersistedSelection(updated.modelInfo(), updated.connection().id()));
        logger.debug("Persisted last selected model: {} on connection {}", updated.modelInfo().id(), updated.connection().id());
    }

    private Optional<LLMInfoAndConnection> loadFromStorage() {
        logger.info("Loading last selected model from storage");
        return settingsStorage.read(STORAGE_KEY, PersistedSelection.class)
            .flatMap(this::resolve);
    }

    private Optional<LLMInfoAndConnection> resolve(PersistedSelection persisted) {
        if (persisted.modelInfo() == null || persisted.connectionId() == null) {
            return Optional.empty();
        }
        Optional<ModelProviderConnection> connection = connectionsStore.findById(persisted.connectionId());
        if (connection.isEmpty()) {
            logger.info("Persisted connection id '{}' no longer exists; clearing last selected model", persisted.connectionId());
            return Optional.empty();
        }
        logger.debug("Resolved last selected model: {} on connection {}", persisted.modelInfo().id(), persisted.connectionId());
        return Optional.ofNullable(new LLMInfoAndConnection(persisted.modelInfo(), connection.get()));
    }

    /**
     * Disk format. Kept intentionally narrow: the connection id is the only
     * link to {@link ConnectionsStore} so that API keys live in exactly one
     * place.
     */
    public record PersistedSelection(ModelInfo modelInfo, String connectionId) {}
}
