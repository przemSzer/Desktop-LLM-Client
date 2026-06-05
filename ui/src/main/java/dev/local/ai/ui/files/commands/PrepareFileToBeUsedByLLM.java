package dev.local.ai.ui.files.commands;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.documents.DocumentAnalyser;
import dev.local.ai.core.documents.DocumentDescription;
import dev.local.ai.ui.commands.ICommand;
import dev.local.ai.ui.files.viewmodel.FileStatus;

public class PrepareFileToBeUsedByLLM implements ICommand{

    private final File file;
    private final IStatusChanged statusChanged;
    private final Logger logger = LoggerFactory.getLogger(PrepareFileToBeUsedByLLM.class);

    public interface IStatusChanged{
        void statusChanged(FileStatus status, DocumentDescription description);
    }

    public PrepareFileToBeUsedByLLM(File file, IStatusChanged statusChanged) {
        this.file = file;
        this.statusChanged = statusChanged;
    }

    @Override
    public boolean execute() {
        try{
            var analyser = new DocumentAnalyser();
            var description = analyser.analyseDocument(file);
            if (description == null) {
                statusChanged.statusChanged(FileStatus.ERROR, null);
                logger.error("Failed to analyse file: {}", file.getName());
                return false;
            }

            statusChanged.statusChanged(FileStatus.TYPE_DETECTED, description);
            
            statusChanged.statusChanged(FileStatus.PREPARING, description);

            var text = analyser.extractContent(file);
            description = new DocumentDescription(description.title(), description.type(), text, file);
            
            statusChanged.statusChanged(FileStatus.VALID, description);
            return true;
        } catch (Exception e) {
            logger.error("Failed to prepare file: {}", file.getName(), e);
            statusChanged.statusChanged(FileStatus.ERROR, null);
            return false;
        }
    }

    @Override
    public boolean undo() {
        return true;
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Prepares file to be used by LLM";
    }

}
