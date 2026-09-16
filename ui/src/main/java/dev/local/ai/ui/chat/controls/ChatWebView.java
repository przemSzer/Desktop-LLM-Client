package dev.local.ai.ui.chat.controls;

import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.MessageTypeView;
import dev.local.ai.ui.chat.viewmodel.ToolCallChatMessageViewModel;
import dev.local.ai.ui.utils.HostServicesProvider;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a single {@link WebView} that renders the entire chat conversation.
 * Messages are appended via JavaScript calls into an HTML template.
 */
public class ChatWebView extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebView.class);

    private final WebView webView;
    private final WebEngine engine;
    private final MarkdownHtmlConverter markdownConverter = new MarkdownHtmlConverter();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    private boolean pageReady = false;
    private JSObject jsWindow;
    private final java.util.List<Runnable> pendingCalls = new java.util.ArrayList<>();

    private final Map<Integer, ChatMessageViewModel> messageIndex = new java.util.LinkedHashMap<>();

    @SuppressWarnings("FieldCanBeLocal")
    private JavaBridge javaBridge;

    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;

    public ChatWebView() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        // Paint the WebView's outer container dark so a white flash
        // doesn't appear while the HTML is still loading.
        webView.setStyle("-fx-background-color: #2E3440;");
        engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                jsWindow = (JSObject) engine.executeScript("window");
                javaBridge = new JavaBridge();
                jsWindow.setMember("javaBridge", javaBridge);
                pageReady = true;
                pendingCalls.forEach(Runnable::run);
                pendingCalls.clear();
            }
        });

        String url = getClass().getResource("/chat/chat.html").toExternalForm();
        engine.load(url);

        setupZoomHandler();

        getChildren().add(webView);
    }

    private void setupZoomHandler() {
        webView.setOnScroll(event -> {
            if (!event.isControlDown()) return;
            event.consume();
            double current = webView.getZoom();
            double delta = event.getDeltaY() > 0 ? ZOOM_STEP : -ZOOM_STEP;
            double next = Math.clamp(current + delta, ZOOM_MIN, ZOOM_MAX);
            webView.setZoom(next);
        });
    }

    public int addMessage(ChatMessageViewModel message) {
        int id = idCounter.incrementAndGet();
        messageIndex.put(id, message);

        MessageTypeView type = message.getType();
        String cssClass = cssClassFor(type);
        String typeLabel = type.getDisplayName();
        String bodyHtml = bodyHtmlFor(message);

        if (type == MessageTypeView.TOOL_CALL || type == MessageTypeView.TOOL_RESULT) {
            String summary = toolSummary(message.getContent());
            callJs(
                    "addToolMessage",
                    id,
                    typeLabel,
                    summary,
                    bodyHtml,
                    type == MessageTypeView.TOOL_CALL);
        } else if (type == MessageTypeView.AI) {
            Statistics stats = message.getStatistics();
            if (stats != null) {
                callJs(
                        "addAiMessage",
                        id,
                        typeLabel,
                        bodyHtml,
                        stats.inputTokens(),
                        stats.outputTokens(),
                        stats.totalTokens());
            } else {
                callJs("addMessage", id, cssClass, typeLabel, bodyHtml);
            }
        } else {
            callJs("addMessage", id, cssClass, typeLabel, bodyHtml);
        }
        return id;
    }

    public void setPartialMessage(String cumulativeContent) {
        String html = markdownConverter.convertToHtml(cumulativeContent);
        callJs("setPartialMessage", html);
    }

    private int partialThinkingMessagesId = 0;

    public int setPartialThinkingMessage(String cumulativeContent) {
        partialThinkingMessagesId++;
        String html = markdownConverter.convertToHtml(cumulativeContent);
        callJs("setPartialThinkingMessage", html, partialThinkingMessagesId);
        return partialThinkingMessagesId;
    }

    public void setPartialThinkingMessage(String cumulativeContent, int id) {
        String html = markdownConverter.convertToHtml(cumulativeContent);
        callJs("setPartialThinkingMessage", html, id);
    }

    public void thinkingFinished(int id) {
        callJs("thinkingFinished", id);
    }

    public void removePartialMessage() {
        callJs("removePartialMessage");
    }

    public void clearMessages() {
        messageIndex.clear();
        idCounter.set(0);
        callJs("clearMessages");
    }

    public void setDarkMode(boolean enabled) {
        callJs("setDarkMode", enabled);
    }

    public void requestApproval(int webViewId) {
        callJs("showToolApproval", webViewId);
    }

    public void hideToolApproval(int webViewId) {
        callJs("hideToolApproval", webViewId);
    }

    public void requestApproval(String messageId) {
        findWebViewId(messageId).ifPresent(this::requestApproval);
    }

    public void hideToolApproval(String messageId) {
        findWebViewId(messageId).ifPresent(this::hideToolApproval);
    }

    private Optional<Integer> findWebViewId(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        return messageIndex.entrySet().stream()
                .filter(entry -> messageId.equals(entry.getValue().getId()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // ── helpers ──────────────────────────────────────────────

    private String bodyHtmlFor(ChatMessageViewModel message) {
        return switch (message.getType()) {
            case AI -> markdownConverter.convertToHtml(message.getContent());
            default -> "<p>" + escapeHtml(message.getContent()) + "</p>";
        };
    }

    private static String cssClassFor(MessageTypeView type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case USER -> "user";
            case AI -> "ai";
            case TOOL_CALL, TOOL_RESULT -> "tool";
            case PARTIAL_AI -> "partial";
            case ERROR -> "error";
            default -> "ai";
        };
    }

    private static String toolSummary(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "…" : content;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
    }

    /**
     * Invokes a global function in chat.html via {@link JSObject#call}, so Unicode
     * (e.g. emoji) is passed as Java strings instead of being embedded in script source.
     */
    private void callJs(String functionName, Object... args) {
        Runnable action = () -> jsWindow.call(functionName, args);
        if (pageReady) {
            action.run();
        } else {
            pendingCalls.add(action);
        }
    }

    // ── JS → Java bridge ────────────────────────────────────

    public class JavaBridge {

        public void copyMessage(int id) {
            ChatMessageViewModel msg = messageIndex.get(id);
            if (msg != null) {
                javafx.application.Platform.runLater(() -> {
                    var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                    var content = new javafx.scene.input.ClipboardContent();
                    content.putString(msg.getContent());
                    clipboard.setContent(content);
                });
            }
        }

        public void openLink(String url) {
            javafx.application.Platform.runLater(() -> {
                try {
                    HostServicesProvider.getInstance().getHostServices().showDocument(url);
                } catch (Exception e) {
                    logger.error("Failed to open link: {}", url, e);
                }
            });
        }

        public void approveTool(int id) {
            javafx.application.Platform.runLater(() -> {
                if (messageIndex.get(id) instanceof ToolCallChatMessageViewModel toolCall) {
                    toolCall.approve();
                }
            });
        }

        public void rejectTool(int id) {
            javafx.application.Platform.runLater(() -> {
                if (messageIndex.get(id) instanceof ToolCallChatMessageViewModel toolCall) {
                    toolCall.reject();
                }
            });
        }
    }
}
