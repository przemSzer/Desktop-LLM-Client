package dev.local.ai.core.documents;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.stream.Stream;

import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class DocumentAnalyserTest {
    
    static class FilesAndExpectedMediaTypeArgumentSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(
                    "simple.pdf", 
                    MediaType.application("pdf"),
                    "Simple PDF Document"
                ),
                Arguments.of(
                    "simple.docx", 
                    MediaType.application("vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    "Simple DOCX Document"
                )
            );
        }
    }
    
    @ParameterizedTest( name = "[{arguments}]: ''{2}''")
    @ArgumentsSource(value = FilesAndExpectedMediaTypeArgumentSource.class)

    void testAnalyseDocument(String fileName, MediaType expectedMediaType, String caseDescription) {
        var fis = getTestResource(fileName);
        DocumentAnalyser analyser = new DocumentAnalyser();

        DocumentDescription description = analyser.analyseDocument(fis);
        assertThat(description)
            .isNotNull()
            .extracting(DocumentDescription::type)
            .isEqualTo(expectedMediaType);
        ;
        
        
    }


    private File getTestResource(String string) {
        return new File(getClass().getResource(string).getFile());
    }

    
}
