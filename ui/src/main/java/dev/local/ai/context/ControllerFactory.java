package dev.local.ai.context;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.ui.chat.controller.ChatController;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.util.Callback;

/**
 * JavaFX controller factory that wires FXML controllers with their dependencies
 * from the {@link AppContext}.
 *
 * <p>Set on every {@code FXMLLoader} via
 * {@code loader.setControllerFactory(controllerFactory)}.
 *
 * <p>For controllers with an explicit mapping below, dependencies are
 * constructor-injected. Anything not mapped falls back to the JavaFX default
 * (no-arg constructor via reflection), so legacy controllers keep working
 * during migration.
 */
public final class ControllerFactory implements Callback<Class<?>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ControllerFactory.class);

    private final AppContext app;

    public ControllerFactory(AppContext app) {
        this.app = app;
    }

    @Override
    public Object call(Class<?> type) {
        try {
            if (type == ChatController.class) {
                ChatViewModel viewModel = new ChatViewModel(buildChat(), app.commandManager, app.eventBus);
                viewModel.selectedModelProperty().set(app.lastSelectedModel.get().orElse(null));
                return new ChatController(viewModel, app.toolProvider, app.eventBus, app.connectionsStore);
            }

            logger.debug("No explicit mapping for controller {}, using default constructor", type.getName());
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create controller: " + type.getName(), e);
        }
    }

    private ILLMChat buildChat() {
        return app.lastSelectedModel.get()
            .map(llm -> {
                logger.info("Building chat from last selected model: {} on connection {}",
                    llm.modelInfo().id(), llm.connection().id());
                var streamingModel = app.modelsProvider.createStreamingChatModel(llm);
                return (ILLMChat) new StreamingChat(streamingModel, app.toolProvider,
                    app.eventBus, app.modelsProvider);
            })
            .orElseGet(this::buildFallbackChat);
    }

    /**
     * First-launch fallback when the user has not yet picked a model.
     * Connects to a local Ollama instance with a sensible default model.
     * TODO: replace with an empty-state UI ("no model selected").
     */
    private ILLMChat buildFallbackChat() {
        logger.warn("No last selected model found; falling back to local Ollama gemma3n:latest");
        var streamingModel = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("gemma3n:latest")
            .timeout(Duration.ofMinutes(5))
            .logRequests(true)
            .logResponses(true)
            .build();
        return new StreamingChat(streamingModel, app.toolProvider,
            app.eventBus, app.modelsProvider);
    }
}
