package org.swdc.archive.core.archive.zip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipHeader;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class ZipArchiveResolver extends ArchiveResolver {

    @Override
    public ArchiveFile loadFile(File file) {
        try {
            ZipArchiveFile zipArchiveFile = new ZipArchiveFile(file);
            // org.apache.commons.compress.archivers.zip.ZipFile zipFile = new org.apache.commons.compress.archivers.zip.ZipFile(file);
            ZipFile zipFile = new ZipFile(file);
            ArchiveEntry rootEntry = new ArchiveEntry();
            rootEntry.setFileName("/");
            rootEntry.setDictionary(true);
            //Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            /* while (entries.hasMoreElements()) {
                ZipArchiveEntry archiveEntry = entries.nextElement();
                resolveEntry(rootEntry, archiveEntry);
            } */
            List<FileHeader> headers = zipFile.getFileHeaders();
            for (FileHeader header: headers) {
                resolveEntry(rootEntry, header);
            }
            zipArchiveFile.setRoot(rootEntry);
            //zipFile.close();
            return zipArchiveFile;
        } catch (Exception e){
            logger.error("fail to load ZipFile: ",e);
            return null;
        }
    }

    private ArchiveEntry resolveEntry(ArchiveEntry root, FileHeader archiveEntry){
        String fullPath = archiveEntry.getFileName();
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
        if (archiveEntry.getCompressedSize() > 0) {
            parent.setSize(archiveEntry.getCompressedSize());
        }
        parent.setLastModifiedDate(new Date(archiveEntry.getLastModifiedTime()));
        return parent;
    }

    @Override
    public void addFile(ArchiveFile target, File file) {

    }

    @Override
    public void removeFile(ArchiveFile target, ArchiveEntry entry) {
        try {
            ZipFile zipFile = new ZipFile(target.getFile());
            if (!entry.isDictionary()) {
                FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1));
                zipFile.removeFile(header);
                return;
            }
            List<ArchiveEntry> entries = entry.getChildren();
            for (ArchiveEntry item: entries) {
                removeFile(target,item);
            }
            FileHeader header = zipFile.getFileHeader(entry.getPath());
            zipFile.removeFile(header);
        } catch (Exception e) {
            logger.error("fail to remove file", e);
        }
    }

    @Override
    public void createArchive(File target) {

    }

    @Override
    public void moveFile(ArchiveFile file, ArchiveEntry form, ArchiveEntry target) {
        if(!target.isDictionary()) {
            return;
        }
        ZipFile zipFile = new ZipFile(file.getFile());
        try {
            if (!form.isDictionary()) {
                FileHeader header = zipFile.getFileHeader(form.getPath());
                String parent = form.getParent().getPath();
                String newName = target.getPath().replace(parent,target.getPath()) + "/" + form.getFileName();
                zipFile.renameFile(header,newName);
            } else {
                for (ArchiveEntry entry: form.getChildren()) {
                    moveFile(file,entry,target);
                }
            }
        } catch (Exception e) {
            logger.error("fail to move archive entry ",e);
        }

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
