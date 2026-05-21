package dev.local.ai.ui.files.dialogs;

import java.io.File;
import java.util.List;

import javafx.stage.FileChooser;

public class OpenFilesDialog implements FileSelector {

    private FileChooser fileChooser;

    public OpenFilesDialog() {
        this.fileChooser = new FileChooser();
        this.fileChooser.setTitle("Select file");        
        this.fileChooser.setInitialDirectory(chooseInitialSelectedDir());
    }

    private File chooseInitialSelectedDir() {
        return new File(System.getProperty("user.home"));
    }

    @Override
    public List<File> getMultiple() {
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            fileChooser.setInitialDirectory(selectedFiles.get(0).getParentFile());
        }
        return selectedFiles;
    }

    @Override
    public File get() {    
        fileChooser.setTitle("Select File to Attach");
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
            return selectedFile;
        }
        
        return null;
    }

}
