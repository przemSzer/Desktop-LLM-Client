package dev.local.ai.ui.models;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.models.AvailableModelsServiceFactory;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.ui.models.errors.ModelsInfoDownloadFailed;
import dev.local.ai.ui.models.errors.ModelsInfoDownloadTaskInvalidInput;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
            .map(this::toViewModelInfo);
    }

    private List<LLMInfoViewModel> toViewModelInfo(List<ModelInfo> modelInfos) {
        return modelInfos
                .stream()
                .map(LLMInfoViewModel::new)
                .toList();
    }

    private ModelProviderConnection findConnectionById(String connectionId) {
        return connectionsStore.findById(connectionId).orElse(null);
    }

    private List<ModelInfo> doLoadModels(String connectionId)
    {
        try {
            var modelProviderConnection = findConnectionById(connectionId);
            if (modelProviderConnection == null) {
                logger.error("Cannot find connection by id {}", connectionId);
                throw new ModelsInfoDownloadTaskInvalidInput("Cannot find connection by id " + connectionId);
            }
            var availableModelsService = availableModelsServiceFactory.forConnection(modelProviderConnection);
            if (availableModelsService == null) {
                logger.error("Cannot get available models service for connection {}", modelProviderConnection);
                throw new ModelsInfoDownloadTaskInvalidInput("Cannot get available models service for connection " + modelProviderConnection);
            }
            return availableModelsService.loadModels();
        } catch (ModelsInfoDownloadTaskInvalidInput ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ModelsInfoDownloadFailed("Downloading models info failed", ex);
        }
    }

}
