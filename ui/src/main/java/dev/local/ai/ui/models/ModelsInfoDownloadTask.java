package dev.local.ai.ui.models;

import java.util.List;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.models.AvailableModelsServiceFactory;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelsInfoDownloadTask {

    private static final Logger logger = LoggerFactory.getLogger(ModelsInfoDownloadTask.class);
    private final AvailableModelsServiceFactory availableModelsServiceFactory;
    private final Scheduler ioScheduler;
    private final ConnectionsStore connectionsStore;

    public ModelsInfoDownloadTask(
            AvailableModelsServiceFactory availableModelsServiceFactory, 
            Scheduler ioScheduler, 
            ConnectionsStore connectionsStore) {
        this.availableModelsServiceFactory = availableModelsServiceFactory;
        this.ioScheduler = ioScheduler;
        this.connectionsStore = connectionsStore;
    }

    public Single<List<LLMInfoViewModel>> start(String connectionId)
    {
        return Single.fromCallable(() -> doLoadModels(connectionId))
            .subscribeOn(ioScheduler)
            .map(models -> models.stream()
                .map(LLMInfoViewModel::new)
                .toList());
    }

    private ModelProviderConnection findConnectionById(String connectionId) {
        return connectionsStore.findById(connectionId).orElse(null);
    }

    private List<ModelInfo> doLoadModels(String connectionId)
    {
        var modelProviderConnection = findConnectionById(connectionId);
        if (modelProviderConnection == null) {
            logger.error("Cannot find connection by id {}", connectionId);
            throw new IllegalArgumentException("Cannot find connection by id " + connectionId);
        }
        var availableModelsService = availableModelsServiceFactory.forConnection(modelProviderConnection);
        if (availableModelsService == null) {
            logger.error("Cannot get available models service for connection {}", modelProviderConnection);
            throw new IllegalArgumentException("Cannot get available models service for connection " + modelProviderConnection);
        }
        return availableModelsService.loadModels();
    }

}
