package dev.local.ai.core.tools.web;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentTransformer;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.ToolHelper;

public class WebPageDownloaderTools implements IToolExecutor {

    Logger logger = LoggerFactory.getLogger(WebPageDownloaderTools.class);
    private final DocumentTransformer transformer;
    private final DocumentParser parser;
    private static final String DOWNLOAD_WEB_PAGE_TOOL_NAME = "downloadWebPage";
    

    public WebPageDownloaderTools() {
        transformer = new HtmlToTextDocumentTransformer(null, null, true);
        parser = new ApacheTikaDocumentParser(true);
    }


    @Tool("Downloads a web page. The page is converted to text. Returns the text of the web page or error message in case of failure.")
    public String downloadWebPage(@P("The URL of the web page to download") String url, @ToolMemoryId String toolMemoryId){
        logger.info("Downloading web page: {}, tool memory id: {}", url, toolMemoryId);
        try {
            var asUrl = new URI(url);
            var urlDocumentSource = new URLSourceWithTimeout(asUrl.toURL(), 10000, 10000);
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
        return new ToolDescriptor(DOWNLOAD_WEB_PAGE_TOOL_NAME, "Download web page", toolSpecifications(), this);
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
