package org.swdc.archive.core.archive.zip;

import javafx.scene.shape.Arc;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;

import java.io.File;
import java.util.Enumeration;

public class ZipArchiveResolver extends ArchiveResolver {

    @Override
    public ArchiveFile loadFile(File file) {
        try {
            ZipArchiveFile zipArchiveFile = new ZipArchiveFile(file);
            ZipFile zipFile = new ZipFile(file);
            ArchiveEntry rootEntry = new ArchiveEntry();
            rootEntry.setFileName("/");
            rootEntry.setDictionary(true);
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry archiveEntry = entries.nextElement();
                resolveEntry(rootEntry, archiveEntry);
            }
            zipArchiveFile.setRoot(rootEntry);
            zipFile.close();
            return zipArchiveFile;
        } catch (Exception e){
            logger.error("fail to load ZipFile: ",e);
            return null;
        }
    }

    private ArchiveEntry resolveEntry(ArchiveEntry root, ZipArchiveEntry archiveEntry){
        String fullPath = archiveEntry.getName();
        String[] paths = fullPath.split("/");
        ArchiveEntry parent = root;
        int count = archiveEntry.isDirectory() ? 2: 1;
        for (int idx = 0; idx <= paths.length - count; idx ++) {
            String current = paths[idx];
            ArchiveEntry next = parent.getChildren().stream()
                    .filter(item -> item.getFileName().equalsIgnoreCase(current))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                next = new ArchiveEntry();
                next.setFileName(current);
                if (idx + count < paths.length) {
                    next.setDictionary(true);
                } else {
                    next.setDictionary(archiveEntry.isDirectory());
                }
                next.setParent(parent);
                parent.getChildren().add(next);
            }
            parent = next;
        }
        if (archiveEntry.getSize() > 0) {
            parent.setSize(archiveEntry.getSize());
        }
        parent.setLastModifiedDate(archiveEntry.getLastModifiedDate());
        return parent;
    }

    @Override
    public void addFile(ArchiveFile target, File file) {

    }

    @Override
    public void removeFile(ArchiveFile target) {

    }

    @Override
    public void createArchive(File target) {

    }

    @Override
    public String getName() {
        return "ZIP压缩文件";
    }

    @Override
    public String getExtension() {
        return "zip";
    }
}
