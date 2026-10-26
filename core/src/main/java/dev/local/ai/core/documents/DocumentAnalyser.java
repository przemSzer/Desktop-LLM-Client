package dev.local.ai.core.documents;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;


public class DocumentAnalyser {

    private static final Logger logger = LoggerFactory.getLogger(DocumentAnalyser.class);

    public DocumentDescription analyseDocument(InputStream inputStream, File file) {
        try (var _ = inputStream) {
            Tika parser = new Tika();
            var mediaTypeStr = parser.detect(inputStream);
            
            MediaType mediaType = MediaType.parse(mediaTypeStr);
            return new DocumentDescription(file.getName(), mediaType, "", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentDescription analyseDocument(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            logger.debug("Analysing file: {}", file.getName());
            return analyseDocument(fis, file);
        } catch (IOException e) {
            logger.error("Failed to analyse file: {}", file.getName(), e);
        }
        return null;              
    }

    public String extractContent(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Tika parser = new Tika();
            parser.setMaxStringLength(-1);
            var doc = analyseDocument(file);
            if ("image".equals(doc.type().getType())) {
                return Base64.getEncoder().encodeToString(fis.readAllBytes());
            }
            return parser.parseToString(fis);
        } catch (TikaException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
