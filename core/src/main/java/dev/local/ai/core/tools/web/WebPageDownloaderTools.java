package dev.local.ai.core.tools.web;

import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentTransformer;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.ITool;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.ToolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class WebPageDownloaderTools implements ITool {

    private static final Logger logger = LoggerFactory.getLogger(WebPageDownloaderTools.class);
    private static final String DOWNLOAD_WEB_PAGE_TOOL_NAME = "downloadWebPage";
    private final DocumentTransformer transformer;
    private final DocumentParser parser;
    private static final int READ_TIMEOUT = 30000;
    private static final int CONNECT_TIMEOUT = 10000;

    public WebPageDownloaderTools() {
        transformer = new HtmlToTextDocumentTransformer(null, null, true);
        parser = new ApacheTikaDocumentParser(true);
    }


    @Tool(value = "Downloads a web page. The page is converted to text. Returns the text of the web page or error message in case of failure.",
        name = DOWNLOAD_WEB_PAGE_TOOL_NAME
    )
    public String downloadWebPage(@P("The URL of the web page to download") String url, @ToolMemoryId String toolMemoryId){
        logger.info("Downloading web page: {}, tool memory id: {}", url, toolMemoryId);
        try {
            var asUrl = new URI(url);            
            logger.debug("Downloading web page with timeout: {}, connect timeout: {}", READ_TIMEOUT, CONNECT_TIMEOUT);
            var urlDocumentSource = new URLSourceWithTimeout(asUrl.toURL(), READ_TIMEOUT, CONNECT_TIMEOUT);
            var document = DocumentLoader.load(urlDocumentSource, parser);            
            var text = transformer.transform(document);
            return text.text();
        } catch (Exception e) {
            logger.error("Failed to download web page: {}, tool memory id: {}", url, toolMemoryId, e);
            var message = e.getMessage();
            if (e.getCause() instanceof Exception cause) {
                message += ", " + cause.getMessage();
            }
            return "Failed to download web page, the following error occurred: " + message;
        }
    }

    private static class InternalInstanceHolder {
        private static final WebPageDownloaderTools INSTANCE = new WebPageDownloaderTools();
    }

    public static WebPageDownloaderTools getInstance() {
        return InternalInstanceHolder.INSTANCE;
    }

    public List<ToolSpecification> toolSpecifications() {
        return ToolSpecifications.toolSpecificationsFrom(getInstance());
    }

    public ToolDescriptor toDescriptor() {
        var specs = toolSpecifications();
        if (specs.isEmpty()) {
            return null;
        }
        var spec = specs.getFirst();
        return new ToolDescriptor(spec.name(), "Download web page", spec , this);
    }

    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest toolExecutionRequest){
        if (!canExecute(toolExecutionRequest)){
            return Optional.empty();
        }
        
        try {
            var args = ToolHelper.getArguments(toolExecutionRequest, "url");
            
            String url = args.get("arg0");
            if (url == null || url.trim().isEmpty()) {
                return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, 
                    "Error: Missing required 'url' parameter"));
            }
            
            String result = downloadWebPage(url, "no id");
            return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, result));
        } catch (Exception e) {
            logger.error("Unexpected error executing tool: {}", toolExecutionRequest.name(), e);
            var failedResult = ToolExecutionResultMessage.builder()
                .isError(true)
                .id(toolExecutionRequest.id())
                .toolName(toolExecutionRequest.name())
                .text("Error: " + e.getMessage())
                .build();
            return Optional.of(failedResult);
        }
    }


    public boolean canExecute(ToolExecutionRequest toolExecutionRequest) {
        return toolExecutionRequest.name().equals(DOWNLOAD_WEB_PAGE_TOOL_NAME);
    }

}
