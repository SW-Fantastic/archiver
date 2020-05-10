package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveFile;

import java.io.File;

public interface ArchiveProcessor {

    void addFile(ArchiveFile target, File file);

    void removeFile(ArchiveFile target);

    void createArchive(File target);

    String getName();

    String getExtension();

    FileChooser.ExtensionFilter getFilter();

    ArchiveFile loadFile(File file);

}
