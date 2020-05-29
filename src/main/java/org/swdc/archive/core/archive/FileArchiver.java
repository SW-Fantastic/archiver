package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.formats.creators.CreatorView;

import java.io.File;
import java.util.List;

public interface FileArchiver {

    void addFile(ArchiveFile target,ArchiveEntry entry, File file);

    void addFolder(ArchiveFile target, ArchiveEntry position, File folder);

    void removeFile(ArchiveFile target, ArchiveEntry entry);

    default <T extends CreatorView> void createArchive(T view){
        create(view.getSaveTarget(),view.getFiles());
    }

    void create(File target,List<File> files);

    Class getCreator();

    void saveComment(ArchiveFile file,String data);

    void rename(ArchiveFile file, ArchiveEntry target, String newName);

    void extractFile(ArchiveFile file, ArchiveEntry entry, File target);

    void extractFiles(ArchiveFile file, File target);

    String getName();

    String getExtension();

    FileChooser.ExtensionFilter getFilter();

    ArchiveFile loadFile(File file);

    boolean creatable();

}
