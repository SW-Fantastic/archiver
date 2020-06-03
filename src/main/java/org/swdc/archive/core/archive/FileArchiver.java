package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.formats.creators.CreatorView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public interface FileArchiver {

    void addFile(ArchiveFile target,ArchiveEntry entry, File file);

    void addFolder(ArchiveFile target, ArchiveEntry position, File folder);

    void removeFile(ArchiveFile target, ArchiveEntry entry);

    default <T extends CreatorView> void createArchive(T view){
        create(view.getSaveTarget(),view.getFiles(),view.getCreateParameters());
    }

    void create(File target,List<File> files, Object param);

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

    ByteBuffer getContent(ArchiveFile file, ArchiveEntry entry);

    default String getMime(ArchiveFile file, ArchiveEntry entry) {
        ByteBuffer byteBuffer = getContent(file,entry);
        if (byteBuffer == null) {
            return null;
        }
        BufferedInputStream bin = new BufferedInputStream(new ByteArrayInputStream(byteBuffer.array()));
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, entry.getFileName());
        try {
            MediaType type = detector.detect(bin,metadata);
            return type.toString();
        } catch (Exception e) {
            return null;
        }
    }

}
