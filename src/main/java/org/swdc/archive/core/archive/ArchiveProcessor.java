package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;

import java.io.File;

public interface ArchiveProcessor {

    void addFile(ArchiveFile target,ArchiveEntry entry, File file);

    void addFolder(ArchiveFile target, ArchiveEntry position, File folder);

    boolean removeFile(ArchiveFile target, ArchiveEntry entry);

    void createArchive(File target);

    void moveFile(ArchiveFile file, ArchiveEntry form, ArchiveEntry target);

    void extractFile(ArchiveFile file, ArchiveEntry entry, File target);

    void extractFiles(ArchiveFile file, File target);

    String getName();

    String getExtension();

    FileChooser.ExtensionFilter getFilter();

    ArchiveFile loadFile(File file);

}
