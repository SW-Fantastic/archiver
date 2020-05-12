package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;

import java.io.File;

public interface ArchiveProcessor {

    void addFile(ArchiveFile target, File file);

    void removeFile(ArchiveFile target, ArchiveEntry entry);

    void createArchive(File target);

    void moveFile(ArchiveFile file, ArchiveEntry form, ArchiveEntry target);

    String getName();

    String getExtension();

    FileChooser.ExtensionFilter getFilter();

    ArchiveFile loadFile(File file);

}
