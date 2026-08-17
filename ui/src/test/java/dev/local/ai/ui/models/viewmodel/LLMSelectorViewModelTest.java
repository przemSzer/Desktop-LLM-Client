package dev.local.ai.ui.models.viewmodel;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.*;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.ui.models.ModelsInfoDownloadTask;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.viewmodel.LLMSelectorViewModel.States;
import dev.local.ai.ui.utils.IUIRunner;
import io.reactivex.rxjava4.core.Single;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LLMSelectorViewModelTest {

    @Mock
    ConnectionsStore connectionsStore;

    @Mock
    CoreEventBus coreEventBus;

    @Mock
    ModelsInfoDownloadTask modelsInfoDownloadTask;

    @Captor
    ArgumentCaptor<LLMChangedEvent> llmChangedEventCaptor;

    IUIRunner uiRunner = Runnable::run;

    @Test
    void connections_should_be_loaded_after_creation() throws Exception {
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new OpenAIConnection("open AI", "open AI connection"),
                new GoogleConnection("google", "google connection"),
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask)) {

            connectionsShouldBeMappedToViewEquivalents(connections, viewModel);
            assertThat(viewModel.getState())
                    .isEqualTo(States.READY);
            assertThat(viewModel.getAvailableModels())
                    .isEmpty();
        }
    }

    @Test
    void refresh_connections_should_reload_from_store() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        connectionStoreContent(List.of(openAIConnection));

        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask)) {
            assertThat(viewModel.getConnections())
                    .hasSize(1);

            var ollamaConnection = new OllamaConnection("ollama", "ollama connection");
            given(connectionsStore.readAll())
                    .willReturn(List.of(openAIConnection, ollamaConnection));
            viewModel.refreshConnections();

            connectionsShouldBeMappedToViewEquivalents(
                    List.of(openAIConnection, ollamaConnection),
                    viewModel
            );
        }
    }

    @Test
    void changing_connection_should_load_models() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description",1000,100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description",1005,105),
                new ModelInfo("1", "gpt-5.6", "gpt-5.6-description",1006,106)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));


        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {
            var openAiConnection = viewModel.getConnections().get(2);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);

            assertThat(viewModel.getConnections())
                    .hasSize(connections.size());
            connectionsShouldBeMappedToViewEquivalents(connections, viewModel);
            assertThat(viewModel.getState())
                    .isEqualTo(States.READY);
            availableModelsShouldMatch(viewModel.getAvailableModels(), openAIModels);
            noModelShouldBeSelected(viewModel);
        }
    }

    private void noModelShouldBeSelected(LLMSelectorViewModel viewModel) {
        assertThat(viewModel.getSelectedModel())
                .isNull();
    }

    @Test
    void changing_connection_to_same_should_not_load_models() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description",1000,100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description",1005,105),
                new ModelInfo("1", "gpt-5.6", "gpt-5.6-description",1006,106)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));

        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {

            var openAiConnection = viewModel.getConnections().get(2);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);

            assertThat(viewModel.getConnections())
                    .hasSize(connections.size());
            connectionsShouldBeMappedToViewEquivalents(connections, viewModel);
            assertThat(viewModel.getState())
                    .isEqualTo(States.READY);
            availableModelsShouldMatch(viewModel.getAvailableModels(), openAIModels);
            then(modelsInfoDownloadTask)
                    .should(times(1))
                    .start(openAIConnection.id());
        }
    }

    @Test
    void changing_connection_to_empty_should_clear_models() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description",1000,100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description",1005,105),
                new ModelInfo("1", "gpt-5.6", "gpt-5.6-description",1006,106)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));

        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {
            var openAiConnection = viewModel.getConnections().get(2);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);
            viewModel
                    .selectedConnectionProperty()
                    .set(null);

            assertThat(viewModel.getConnections())
                    .hasSize(connections.size());
            connectionsShouldBeMappedToViewEquivalents(connections, viewModel);
            assertThat(viewModel.getState())
                    .isEqualTo(States.READY);
            then(modelsInfoDownloadTask)
                    .should(times(1))
                    .start(openAIConnection.id());
            assertThat(viewModel.getAvailableModels())
                    .isEmpty();
            assertThat(viewModel.getSelectedModel())
                    .isNull();
        }
    }

    @Test
    void while_models_are_loading_state_should_be_loading() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.never());


        try (var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {

            var openAiConnection = viewModel.getConnections().get(2);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);

            assertThat(viewModel.getState())
                    .isEqualTo(States.LOADING);
            assertThat(viewModel.isLoadingModels())
                    .isTrue();
        }

    }

    @Test
    void while_loading_would_fail_state_should_be_error() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.error(new Exception("error")));

        try(var viewModel = new LLMSelectorViewModel(connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {

            var openAiConnection = viewModel.getConnections().get(2);
            viewModel
                    .selectedConnectionProperty()
                    .set(openAiConnection);

            assertThat(viewModel.getState())
                    .isEqualTo(States.ERROR);
            assertThat(viewModel.isLoadingModels())
                    .isFalse();
        }
    }

    @Test
    void when_connection_is_changed_loading_tasks_should_be_disposed() throws Exception {
        var ollamaConnection = new OllamaConnection("ollama", "ollama connection");
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                ollamaConnection,
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);

        var firstDisposed = new AtomicBoolean(false);
        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description", 1000, 100)
        );

        given(modelsInfoDownloadTask.start(ollamaConnection.id()))
                .willReturn(Single.create(emitter ->
                        emitter.setCancellable(() -> firstDisposed.set(true))
                ));
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(
                        openAIModels.stream().map(LLMInfoViewModel::new).toList()
                ));

        try (var viewModel = new LLMSelectorViewModel(
                connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {

                var ollamaVm = viewModel.getConnections().get(0);
                var openAiVm = viewModel.getConnections().get(2);

                viewModel.selectedConnectionProperty().set(ollamaVm);
                assertThat(viewModel.getState())
                        .isEqualTo(States.LOADING);
                assertThat(firstDisposed)
                        .isFalse();

                viewModel.selectedConnectionProperty().set(openAiVm);

                assertThat(firstDisposed).isTrue();
                assertThat(viewModel.getState()).isEqualTo(States.READY);
                availableModelsShouldMatch(viewModel.getAvailableModels(), openAIModels);
        }
    }

    @Test
    void when_model_is_selected_event_should_be_published() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);
        given(connectionsStore.findById(openAIConnection.id()))
                .willReturn(Optional.of(openAIConnection));

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description", 1000, 100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description", 1005, 105),
                new ModelInfo("1", "gpt-5.6", "gpt-5.6-description", 1006, 106)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));

        try (var viewModel = new LLMSelectorViewModel(
                connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {
            viewModel.selectedConnectionProperty().set(viewModel.getConnections().get(2));
            var selectedModel = viewModel.getAvailableModels().get(1);
            viewModel.setSelectedModel(selectedModel);

            then(coreEventBus)
                    .should()
                    .publish(llmChangedEventCaptor.capture());
            var event = llmChangedEventCaptor.getValue();
            assertThat(event.getSource())
                    .isEqualTo(LLMSelectorViewModel.class.getSimpleName());
            assertThat(event.getEventType())
                    .isEqualTo(LLMChangedEvent.EVENT_TYPE);
            assertThat(event.getModelInfo().modelInfo())
                    .isEqualTo(openAIModels.get(1));
            assertThat(event.getModelInfo().connection())
                    .isEqualTo(openAIConnection);
        }
    }

    @Test
    void reselecting_same_model_after_clear_should_not_publish_again() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);
        given(connectionsStore.findById(openAIConnection.id()))
                .willReturn(Optional.of(openAIConnection));

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description", 1000, 100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description", 1005, 105)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));

        try (var viewModel = new LLMSelectorViewModel(
                connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {
            viewModel.selectedConnectionProperty().set(viewModel.getConnections().get(2));
            var selectedModel = viewModel.getAvailableModels().get(1);
            viewModel.setSelectedModel(selectedModel);
            viewModel.setSelectedModel(null);
            viewModel.setSelectedModel(selectedModel);

            then(coreEventBus)
                    .should(times(1))
                    .publish(llmChangedEventCaptor.capture());
            assertThat(llmChangedEventCaptor.getValue().getModelInfo().modelInfo())
                    .isEqualTo(openAIModels.get(1));
        }
    }

    @Test
    void selecting_different_model_should_publish_again() throws Exception {
        var openAIConnection = new OpenAIConnection("open AI", "open AI connection");
        List<ModelProviderConnection> connections = List.of(
                new OllamaConnection("ollama", "ollama connection"),
                new GoogleConnection("google", "google connection"),
                openAIConnection,
                new AnthropicConnection("anthropic", "anthropic connection")
        );
        connectionStoreContent(connections);
        given(connectionsStore.findById(openAIConnection.id()))
                .willReturn(Optional.of(openAIConnection));

        var openAIModels = List.of(
                new ModelInfo("1", "gpt-5.4", "gpt-5.4-description", 1000, 100),
                new ModelInfo("1", "gpt-5.5", "gpt-5.5-description", 1005, 105)
        );
        given(modelsInfoDownloadTask.start(openAIConnection.id()))
                .willReturn(Single.just(openAIModels.stream().map(LLMInfoViewModel::new).toList()));

        try (var viewModel = new LLMSelectorViewModel(
                connectionsStore, coreEventBus, modelsInfoDownloadTask, uiRunner)) {
            viewModel.selectedConnectionProperty().set(viewModel.getConnections().get(2));
            viewModel.setSelectedModel(viewModel.getAvailableModels().get(0));
            viewModel.setSelectedModel(viewModel.getAvailableModels().get(1));

            then(coreEventBus)
                    .should(times(2))
                    .publish(llmChangedEventCaptor.capture());
            assertThat(llmChangedEventCaptor.getAllValues())
                    .extracting(event -> event.getModelInfo().modelInfo())
                    .containsExactly(openAIModels.get(0), openAIModels.get(1));
        }
    }


    private void availableModelsShouldMatch(ObservableList<LLMInfoViewModel> viewLLMInfo, List<ModelInfo> openAIModels) {
        assertThat(viewLLMInfo)
                .hasSize(openAIModels.size());
        for (int i = 0; i < viewLLMInfo.size(); i++) {
          assertThat(viewLLMInfo.get(i).getCoreModelInfo())
                  .isEqualTo(openAIModels.get(i));
          assertThat(viewLLMInfo.get(i).getName())
                  .isEqualTo(openAIModels.get(i).name());
          assertThat(viewLLMInfo.get(i).getDescription())
                  .isEqualTo(openAIModels.get(i).description());
        }
    }

    private void connectionStoreContent(List<ModelProviderConnection> connections) {
        given(connectionsStore.readAll())
                .willReturn(connections);
    }

    private static void connectionsShouldBeMappedToViewEquivalents(List<ModelProviderConnection> connections, LLMSelectorViewModel viewModel) {
        assertThat(viewModel.getConnections())
                .hasSize(connections.size());
        for (int index = 0; index < connections.size(); index++) {
            assertThat(viewModel.getConnections().get(index).getName())
                    .isEqualTo(connections.get(index).name());
            assertThat(viewModel.getConnections().get(index).getDescription())
                    .isEqualTo(connections.get(index).description());
            assertThat(viewModel.getConnections().get(index).getId())
                    .isEqualTo(connections.get(index).id());
        }
    }
}