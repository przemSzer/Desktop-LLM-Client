package dev.local.ai.core.tools.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentTransformer;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public class WebPageDownloaderTools implements IToolExecutor {

    Logger logger = LoggerFactory.getLogger(WebPageDownloaderTools.class);
    private final DocumentTransformer transformer;
    private final DocumentParser parser;
    private final ObjectMapper objectMapper;
    private static final String DOWNLOAD_WEB_PAGE_TOOL_NAME = "downloadWebPage";
    

    public WebPageDownloaderTools() {
        transformer = new HtmlToTextDocumentTransformer();
        parser = new ApacheTikaDocumentParser(true);
        objectMapper = new ObjectMapper();
    }


    @Tool("Downloads a web page. The page is converted to text. Returns the text of the web page or error message in case of failure.")
    public String downloadWebPage(@P("The URL of the web page to download") String url, @ToolMemoryId String toolMemoryId){
        logger.info("Downloading web page: {}, tool memory id: {}", url, toolMemoryId);
        try {
            var document = UrlDocumentLoader.load(url, parser);            
            var text = transformer.transform(document);
            return text.text();
        } catch (Exception e) {
            logger.error("Failed to download web page: {}, tool memory id: {}", url, toolMemoryId, e);
            return "Failed to download web page, the following error occurred: " + e.getMessage();
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

    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest toolExecutionRequest){
        if (!toolExecutionRequest.name().equals(DOWNLOAD_WEB_PAGE_TOOL_NAME)){
            return Optional.empty();
        }
        
        try {
            // Parse JSON arguments to Map<String,String>
            Map<String, String> args = objectMapper.readValue(
                toolExecutionRequest.arguments(), 
                new TypeReference<Map<String, String>>() {}
            );
            
            // Extract URL from arguments
            String url = args.get("arg0");
            if (url == null || url.trim().isEmpty()) {
                return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, 
                    "Error: Missing required 'url' parameter"));
            }
            
            // Execute the download
            String result = downloadWebPage(url, "no id");
            return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, result));
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse tool arguments JSON: {}", toolExecutionRequest.arguments(), e);
            return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, 
                "Error: Invalid JSON arguments format - " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error executing tool: {}", toolExecutionRequest.name(), e);
            return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, 
                "Error: " + e.getMessage()));
        }
    }

}
