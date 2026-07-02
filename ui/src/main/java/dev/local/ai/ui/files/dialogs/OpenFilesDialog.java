package dev.local.ai.ui.files.dialogs;



import java.io.File;

import java.util.List;



import dev.local.ai.ui.utils.MainStageProvider;

import javafx.stage.FileChooser;

public class OpenFilesDialog implements FileSelector {

    private final MainStageProvider mainStageProvider;
    private final FileChooser fileChooser;

    public OpenFilesDialog(MainStageProvider mainStageProvider) {

        if (mainStageProvider == null) {

            throw new IllegalArgumentException("Main stage provider cannot be null");

        }
        this.mainStageProvider = mainStageProvider;
        this.fileChooser = new FileChooser();
        this.fileChooser.setTitle("Select file");
        this.fileChooser.setInitialDirectory(chooseInitialSelectedDir());
    }

    private File chooseInitialSelectedDir() {
        return new File(System.getProperty("user.home"));
    }

    @Override
    public List<File> getMultiple() {
        assertLastDirIsValidOrSetDefault();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainStageProvider.getMainWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            fileChooser.setInitialDirectory(selectedFiles.get(0).getParentFile());
        }

        return selectedFiles;
    }



    @Override
    public File get() {
        assertLastDirIsValidOrSetDefault();
        fileChooser.setTitle("Select File to Attach");
        File selectedFile = fileChooser.showOpenDialog(mainStageProvider.getMainWindow());

        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
            return selectedFile;
        }
        return null;
    }

    private void assertLastDirIsValidOrSetDefault() {
        if (!fileChooser.getInitialDirectory().isDirectory()){
            fileChooser.setInitialDirectory(chooseInitialSelectedDir());
        }
    }


}


