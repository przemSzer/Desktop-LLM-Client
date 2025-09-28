package dev.local.ai.core.documents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DocumentAnalyser {

    Logger logger = LoggerFactory.getLogger(DocumentAnalyser.class);

    public DocumentDescription analyseDocument(InputStream inputStream, File file) {
        try (InputStream fis = inputStream) {
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
        } catch (FileNotFoundException e) {
            logger.error("Failed to analyse file: {}", file.getName(), e);
        } catch (IOException e) {
            logger.error("Failed to analyse file: {}", file.getName(), e);
        }    
        return null;              
    }

    public String extractText(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Tika parser = new Tika();
            return parser.parseToString(fis);
        } catch (FileNotFoundException | TikaException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
