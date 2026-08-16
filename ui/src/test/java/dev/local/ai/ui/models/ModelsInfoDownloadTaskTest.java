package dev.local.ai.ui.models;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.connections.OpenAIConnection;
import dev.local.ai.core.models.AvailableModelsService;
import dev.local.ai.core.models.AvailableModelsServiceFactory;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.ui.models.errors.ModelsInfoDownloadFailed;
import dev.local.ai.ui.models.errors.ModelsInfoDownloadTaskInvalidInput;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.schedulers.Schedulers;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ModelsInfoDownloadTaskTest {

    Scheduler trampoline = Schedulers.trampoline();

    @Mock
    AvailableModelsServiceFactory availableModelsServiceFactory;
    @Mock
    AvailableModelsService availableModelsService;
    @Mock
    ConnectionsStore connectionsStore;

    @Test
    void start_should_load_models() {
        var task = createTask();
        var connection = new OpenAIConnection("open AI", "open AI connection");
        var models = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description", 1000, 100),
                new ModelInfo("2", "gpt-5.5", "gpt-5.5-description", 1005, 105)
        );
        given(connectionsStore.findById(connection.id()))
                .willReturn(Optional.of(connection));
        availableModelsFactoryWillWorkFor(connection);

        given(availableModelsService.loadModels())
                .willReturn(models);

        var result = task.start(connection.id()).blockingGet();

        assertThat(result)
                .extracting(LLMInfoViewModel::getCoreModelInfo)
                .containsExactlyElementsOf(models);
        then(connectionsStore)
                .should()
                .findById(connection.id());
        then(availableModelsServiceFactory)
                .should()
                .forConnection(connection);
        then(availableModelsService)
                .should()
                .loadModels();
    }

    private void availableModelsFactoryWillWorkFor(ModelProviderConnection connection) {
        given(availableModelsServiceFactory.forConnection(connection))
                .willReturn(availableModelsService);
    }

    private @NonNull ModelsInfoDownloadTask createTask() {
       return new ModelsInfoDownloadTask(
            availableModelsServiceFactory, trampoline, connectionsStore
        );
    }

    @Test
    void when_loading_fails_exception_should_be_thrown(){
        var task = createTask();
        var connection = new OpenAIConnection("open AI", "open AI connection");
        given(connectionsStore.findById(connection.id()))
                .willReturn(Optional.of(connection));
        availableModelsFactoryWillWorkFor(connection);
        var exp = new RuntimeException("Fatal error");
        willThrow(exp)
                .given(availableModelsService)
                .loadModels();

        assertThatThrownBy(() ->
            task.start(connection.id()).blockingGet()
        ).isInstanceOf(ModelsInfoDownloadFailed.class)
                .hasCause(exp);
    }

    @Test
    void when_connection_is_missing_invalid_input_should_be_thrown() {
        var task = createTask();
        var connectionId = "missing-connection";
        given(connectionsStore.findById(connectionId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> task.start(connectionId).blockingGet())
                .isInstanceOf(ModelsInfoDownloadTaskInvalidInput.class)
                .hasMessage("Cannot find connection by id " + connectionId);
        then(availableModelsServiceFactory)
                .shouldHaveNoInteractions();
        then(availableModelsService)
                .shouldHaveNoInteractions();
    }

    @Test
    void when_models_service_is_missing_invalid_input_should_be_thrown() {
        var task = createTask();
        var connection = new OpenAIConnection("open AI", "open AI connection");
        given(connectionsStore.findById(connection.id()))
                .willReturn(Optional.of(connection));
        given(availableModelsServiceFactory.forConnection(connection))
                .willReturn(null);

        assertThatThrownBy(() -> task.start(connection.id()).blockingGet())
                .isInstanceOf(ModelsInfoDownloadTaskInvalidInput.class)
                .hasMessage("Cannot get available models service for connection " + connection);
        then(availableModelsService)
                .shouldHaveNoInteractions();
    }
}
