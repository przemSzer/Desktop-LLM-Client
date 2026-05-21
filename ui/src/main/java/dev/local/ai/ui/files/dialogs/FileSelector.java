package dev.local.ai.ui.files.dialogs;

import java.io.File;
import java.util.List;

public interface FileSelector {

    File get();
    List<File> getMultiple();
}
