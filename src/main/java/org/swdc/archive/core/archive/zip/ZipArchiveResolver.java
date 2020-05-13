package org.swdc.archive.core.archive.zip;

import javafx.application.Platform;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.ProgressView;

import java.io.File;
import java.util.Date;
import java.util.List;

public class ZipArchiveResolver extends ArchiveResolver {

    private ProgressView progressView = null;

    @Override
    public void initialize() {
        Platform.runLater(() -> {
            this.progressView = findView(ProgressView.class);
        });
    }

    @Override
    public ArchiveFile loadFile(File file) {
        try {
            ZipArchiveFile zipArchiveFile = new ZipArchiveFile(file);
            ZipFile zipFile = new ZipFile(file);
            ArchiveEntry rootEntry = new ArchiveEntry();
            rootEntry.setFileName("/");
            rootEntry.setDictionary(true);

            List<FileHeader> headers = zipFile.getFileHeaders();
            for (FileHeader header: headers) {
                resolveEntry(rootEntry, header);
            }
            zipArchiveFile.setRoot(rootEntry);
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
    public void addFile(ArchiveFile target,ArchiveEntry entry, File file) {
        ZipFile zipFile = new ZipFile(target.getFile());
        if (!entry.isDictionary()) {
            entry = entry.getParent();
        }
        String parent = entry.getPath();
        String name = parent + "/" + file.getName();
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip(name);
        parameters.setOverrideExistingFilesInZip(true);
        try {
            zipFile.addFile(file,parameters);
            ArchiveEntry created = new ArchiveEntry();
            created.setFileName(file.getName());
            created.setParent(entry);
            created.setLastModifiedDate(new Date());
            created.setSize(file.length());
            created.setDictionary(false);
            entry.getChildren().add(created);
            this.emit(new ViewRefreshEvent(created,this));
        } catch (Exception e) {
            logger.error("fail to add file");
        }
    }

    @Override
    public boolean removeFile(ArchiveFile target, ArchiveEntry entry) {
        try {
            ZipFile zipFile = new ZipFile(target.getFile());
            if (!entry.isDictionary()) {
                FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1));
                zipFile.removeFile(header);
                removeEntry(target.getRootEntry(),entry);
                this.emit(new ViewRefreshEvent(entry,this));
                return true;
            }
            FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1) + "/");
            zipFile.removeFile(header);
            removeEntry(target.getRootEntry(),entry);
            this.emit(new ViewRefreshEvent(entry,this));
            return true;
        } catch (Exception e) {
            logger.error("fail to remove file", e);
            return false;
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
