package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.utils.HostServicesProvider;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
        engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                javaBridge = new JavaBridge();
                window.setMember("javaBridge", javaBridge);
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
            double next = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, current + delta));
            webView.setZoom(next);
        });
    }

    public int addMessage(ChatMessageViewModel message) {
        int id = idCounter.incrementAndGet();
        messageIndex.put(id, message);

        MessageType type = message.getType();
        String cssClass = cssClassFor(type);
        String typeLabel = type.getDisplayName();
        String bodyHtml = bodyHtmlFor(message);

        if (type == MessageType.TOOL_CALL || type == MessageType.TOOL_RESULT) {
            String summary = toolSummary(message.getContent());
            String fullHtml = escapeForJs(bodyHtml);
            String summaryEscaped = escapeForJs(summary);
            runScript("addToolMessage(%d,'%s','%s','%s')", id, typeLabel, summaryEscaped, fullHtml);
        } else {
            String htmlEscaped = escapeForJs(bodyHtml);
            runScript("addMessage(%d,'%s','%s','%s')", id, cssClass, typeLabel, htmlEscaped);
        }
        return id;
    }

    public void setPartialMessage(String cumulativeContent) {
        String html = escapeForJs(markdownConverter.convertToHtml(cumulativeContent));
        runScript("setPartialMessage('%s')", html);
    }

    public void removePartialMessage() {
        runScript("removePartialMessage()");
    }

    public void clearMessages() {
        messageIndex.clear();
        idCounter.set(0);
        runScript("clearMessages()");
    }

    // ── helpers ──────────────────────────────────────────────

    private String bodyHtmlFor(ChatMessageViewModel message) {
        return switch (message.getType()) {
            case AI -> markdownConverter.convertToHtml(message.getContent());
            default -> "<p>" + escapeHtml(message.getContent()) + "</p>";
        };
    }

    private static String cssClassFor(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case AI -> "ai";
            case TOOL_CALL, TOOL_RESULT -> "tool";
            case PARTIAL -> "partial";
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

    private static String escapeForJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private void runScript(String format, Object... args) {
        String script = String.format(format, args);
        if (pageReady) {
            engine.executeScript(script);
        } else {
            pendingCalls.add(() -> engine.executeScript(script));
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
    }
}
