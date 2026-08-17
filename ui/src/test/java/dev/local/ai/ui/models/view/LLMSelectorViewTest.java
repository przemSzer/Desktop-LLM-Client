package dev.local.ai.ui.models.view;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.ui.models.ModelsInfoDownloadTask;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.viewmodel.LLMSelectorViewModel;
import io.reactivex.rxjava4.core.Single;
import io.reactivex.rxjava4.core.SingleEmitter;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LLMSelectorViewTest {

    private static final AtomicBoolean PLATFORM_STARTED = new AtomicBoolean(false);

    @Mock
    ConnectionsStore connectionsStore;

    @Mock
    CoreEventBus coreEventBus;

    @Mock
    ModelsInfoDownloadTask modelsInfoDownloadTask;

    private LLMSelectorViewModel viewModel;
    private LLMSelectorView view;

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        if (PLATFORM_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException _) {
                latch.countDown();
            }
            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("JavaFX toolkit failed to start within 5 seconds")
                    .isTrue();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (viewModel != null) {
            viewModel.close();
        }
    }

    @Test
    void fxml_should_wire_controls() throws InterruptedException {
        showView();

        runOnFxThreadAndWait(() -> {
            assertThat(view.getConnectionComboBox()).isNotNull();
            assertThat(view.getModelComboBox()).isNotNull();
            assertThat(view.getLoadingIndicator()).isNotNull();
            assertThat(view.getManageConnectionsButton()).isNotNull();
            assertThat(view.getConnectionComboBox().getPromptText())
                    .isEqualTo("Select a connection...");
            assertThat(view.getModelComboBox().getPromptText())
                    .isEqualTo("Select a model...");
            assertThat(view.getManageConnectionsButton().getText())
                    .isEqualTo("Connections...");
            assertThat(view.getModelComboBox().isEditable()).isFalse();
            assertThat(view.getConnectionComboBox().getMinWidth()).isEqualTo(140);
            assertThat(view.getModelComboBox().getMinWidth()).isEqualTo(140);
            assertThat(view.getConnectionComboBox().getMaxWidth()).isEqualTo(Double.POSITIVE_INFINITY);
            assertThat(view.getModelComboBox().getMaxWidth()).isEqualTo(Double.POSITIVE_INFINITY);
            assertThat(view.getConnectionComboBox().getCellFactory()).isNotNull();
            assertThat(view.getConnectionComboBox().getButtonCell()).isNotNull();
            assertThat(view.getModelComboBox().getCellFactory()).isNotNull();
            assertThat(view.getModelComboBox().getButtonCell()).isNotNull();
            assertThat(view.getManageConnectionsButton().getOnAction()).isNotNull();
            assertThat(view.getViewModel()).isSameAs(viewModel);
        });
    }

    @Test
    void connections_combo_should_be_bound_to_view_model() throws InterruptedException {
        var openAiConnection = new OpenAIConnection("open AI", "open AI connection");
        var ollamaConnection = new OllamaConnection("ollama", "ollama connection");
        given(modelsInfoDownloadTask.start(anyString()))
                .willReturn(Single.just(List.of()));
        showView(openAiConnection, ollamaConnection);

        runOnFxThreadAndWait(() -> {
            var openAi = viewModel.getConnections().getFirst();
            var ollama = viewModel.getConnections().get(1);

            view.getConnectionComboBox().setValue(openAi);

            assertThat(view.getConnectionComboBox().getItems())
                    .containsExactly(openAi, ollama);
            assertThat(viewModel.getSelectedConnection())
                    .isSameAs(openAi);

            viewModel.setSelectedConnection(ollama);

            assertThat(view.getConnectionComboBox().getValue())
                    .isSameAs(ollama);
        });
    }

    @Test
    void models_combo_should_be_bound_to_view_model() throws InterruptedException {
        var openAiConnection = new OpenAIConnection("open AI", "open AI connection");
        var gpt = model("gpt-5.4");
        var gemini = model("gemini-3");
        given(modelsInfoDownloadTask.start(openAiConnection.id())).willReturn(Single.just(List.of(gpt, gemini)));
        given(connectionsStore.findById(openAiConnection.id())).willReturn(Optional.of(openAiConnection));
        showView(openAiConnection);

        runOnFxThreadAndWait(() -> {
            viewModel.setSelectedConnection(viewModel.getConnections().getFirst());

            assertThat(view.getModelComboBox().getItems())
                    .containsExactly(gpt, gemini);

            view.getModelComboBox().setValue(gpt);

            assertThat(viewModel.getSelectedModel())
                    .isSameAs(gpt);
            then(coreEventBus)
                    .should()
                    .publish(any());

            viewModel.setSelectedModel(gemini);

            assertThat(view.getModelComboBox().getValue()).isSameAs(gemini);
        });
    }

    @Test
    void loading_indicator_should_follow_view_model() throws InterruptedException {
        var openAiConnection = new OpenAIConnection("open AI", "open AI connection");
        var pendingModels = new AtomicReference<SingleEmitter<List<LLMInfoViewModel>>>();
        given(modelsInfoDownloadTask.start(openAiConnection.id()))
                .willReturn(Single.create(pendingModels::set));
        showView(openAiConnection);

        runOnFxThreadAndWait(() -> {
            assertThat(view.getLoadingIndicator().isVisible()).isFalse();
            assertThat(view.getLoadingIndicator().isManaged()).isFalse();

            viewModel.setSelectedConnection(viewModel.getConnections().getFirst());

            assertThat(view.getLoadingIndicator().isVisible()).isTrue();
            assertThat(view.getLoadingIndicator().isManaged()).isTrue();
        });

        runOnFxThreadAndWait(() -> {
            pendingModels.get().onSuccess(List.of());

            assertThat(view.getLoadingIndicator().isVisible()).isFalse();
            assertThat(view.getLoadingIndicator().isManaged()).isFalse();
        });
    }

    @Test
    void connection_combo_should_stay_readable_when_model_name_is_long() throws InterruptedException {
        var openAiConnection = new OpenAIConnection("OpenAI", "OpenAI connection");
        var longModel = model("accounts/fireworks/models/qwen3-235b-a22b-instruct-2507");
        given(modelsInfoDownloadTask.start(openAiConnection.id())).willReturn(Single.just(List.of(longModel)));
        given(connectionsStore.findById(openAiConnection.id())).willReturn(Optional.of(openAiConnection));
        showView(openAiConnection);

        runOnFxThreadAndWait(() -> {
            viewModel.setSelectedConnection(viewModel.getConnections().getFirst());
            viewModel.setSelectedModel(longModel);

            new Scene(view, 420, 90);
            view.applyCss();
            view.resize(420, view.prefHeight(420));
            view.layout();

            assertThat(view.getConnectionComboBox().getWidth())
                    .as("connection combo must not collapse when a long model name is selected")
                    .isGreaterThanOrEqualTo(140);
            assertThat(view.getModelComboBox().getWidth())
                    .as("model combo should still have room for a long name on its own row")
                    .isGreaterThanOrEqualTo(140);
            assertThat(view.getConnectionComboBox().getWidth())
                    .isGreaterThan(view.getManageConnectionsButton().getWidth());
        });
    }

    @Test
    void public_api_should_delegate_to_view_model() throws InterruptedException {
        var openAiConnection = new OpenAIConnection("open AI", "open AI connection");
        var gpt = model("gpt-5.4");
        given(modelsInfoDownloadTask.start(openAiConnection.id())).willReturn(Single.just(List.of(gpt)));
        given(connectionsStore.findById(openAiConnection.id())).willReturn(Optional.of(openAiConnection));
        showView(openAiConnection);

        runOnFxThreadAndWait(() -> {
            var connection = viewModel.getConnections().getFirst();
            view.setSelectedConnection(connection);

            assertThat(view.getSelectedConnection()).isSameAs(connection);
            assertThat(view.getConnectionComboBox().getValue()).isSameAs(connection);

            view.setSelectedModel(gpt);

            assertThat(view.getSelectedModel()).isSameAs(gpt);
            assertThat(view.getModelComboBox().getValue()).isSameAs(gpt);
        });
    }

    private void showView(ModelProviderConnection... connections) throws InterruptedException {
        given(connectionsStore.readAll()).willReturn(List.of(connections));
        viewModel = new LLMSelectorViewModel(
                connectionsStore, coreEventBus, modelsInfoDownloadTask, Runnable::run);
        runOnFxThreadAndWait(() -> {
            view = new LLMSelectorView();
            view.init(null, null, viewModel);
        });
    }

    private static LLMInfoViewModel model(String name) {
        return new LLMInfoViewModel(new ModelInfo(name, name, name + "-description", 1000, 100));
    }

    private static void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(5, TimeUnit.SECONDS))
                .as("FX action did not complete in 5s")
                .isTrue();
        if (error.get() != null) {
            throw new AssertionError("FX action failed", error.get());
        }
    }
}
