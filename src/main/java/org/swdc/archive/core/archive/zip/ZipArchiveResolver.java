package org.swdc.archive.core.archive.zip;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.DataUtil;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.ProgressView;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

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
            progressView.show();
            progressView.update("正在读取文件：" + file.getName(), 0);
            Charset charset =  DataUtil.getCharset(file);
            progressView.update("读取文件编码格式：" + charset.name(),10);
            ZipArchiveFile zipArchiveFile = new ZipArchiveFile(file);
            zipArchiveFile.setCharset(charset);

            ZipFile zipFile = new ZipFile(file);
            zipFile.setCharset(charset);
            progressView.update("验证压缩文件：" + file.getName(), 20);
            if (!zipFile.isValidZipFile()) {
                return null;
            }
            ArchiveEntry rootEntry = new ArchiveEntry();
            rootEntry.setFileName("/");
            rootEntry.setDictionary(true);
            progressView.update("读取内容",60);
            List<FileHeader> headers = zipFile.getFileHeaders();
            for (FileHeader header: headers) {
                resolveEntry(zipArchiveFile, rootEntry, header);
            }
            zipArchiveFile.setRoot(rootEntry);
            progressView.finish();
            return zipArchiveFile;
        } catch (Exception e){
            logger.error("fail to load ZipFile: ",e);
            return null;
        }
    }


    private ArchiveEntry resolveEntry(ArchiveFile file,ArchiveEntry root, FileHeader archiveEntry){
        String fullPath = archiveEntry.getFileName();
        String[] paths = fullPath.split("/");
        ArchiveEntry parent = root;
        for (int idx = 0; idx <= paths.length - 1; idx ++) {
            String current = paths[idx];
            ArchiveEntry next = parent.getChildren().stream()
                    .filter(item -> item.getFileName().equalsIgnoreCase(current))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                next = new ArchiveEntry();
                next.setFileName(current);
                if (idx + 1 < paths.length) {
                    next.setDictionary(true);
                } else {
                    next.setDictionary(archiveEntry.isDirectory());
                }
                next.setParent(parent);
                next.setFile(file);
                parent.getChildren().add(next);
            }
            parent = next;
        }
        if (archiveEntry.getCompressedSize() > 0) {
            parent.setSize(archiveEntry.getCompressedSize());
        }
        parent.setLastModifiedDate(new Date(archiveEntry.getLastModifiedTime()));
        parent.setFile(file);
        return parent;
    }

    @Override
    public void addFolder(ArchiveFile target, ArchiveEntry position, File folder) {
        ZipFile zipFile = new ZipFile(target.getFile());
        zipFile.setCharset(target.getCharset());
        if (folder == null) {
            return;
        }
        if (position == null) {
            position = target.getRootEntry();
        }
        if (!position.isDictionary()) {
            position = position.getParent();
        }
        try {
            ZipParameters parameters = new ZipParameters();
            String path = position.getPath().substring(1);
            parameters.setRootFolderNameInZip(path);
            ProgressMonitor monitor = zipFile.getProgressMonitor();
            Thread progressor = getProgressThread("正在添加", monitor, (rst) -> {
                try {
                    List<FileHeader> headers = zipFile.getFileHeaders();
                    for (FileHeader header: headers) {
                        ArchiveEntry entry = resolveEntry(target, target.getRootEntry(), header);
                        mountArchiveEntry(entry);
                    }
                    this.emit(new ViewRefreshEvent(null,this));
                } catch (Exception e) {
                    logger.error("fail to reload file tree");
                }
            });
            progressView.show();
            progressor.start();
            zipFile.setRunInThread(true);
            zipFile.addFolder(folder,parameters);
        } catch (Exception e) {
            logger.error("fail to add folder", e);
        }
    }

    @Override
    public void addFile(ArchiveFile target,ArchiveEntry entry, File file) {
        ZipFile zipFile = new ZipFile(target.getFile());
        zipFile.setCharset(target.getCharset());
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
            zipFile.setCharset(target.getCharset());
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
        zipFile.setCharset(file.getCharset());
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
    public void extractFile(ArchiveFile file, ArchiveEntry entry, File target) {
        try {
            ZipFile zipFile = new ZipFile(file.getFile());
            zipFile.setCharset(file.getCharset());
            ProgressMonitor monitor = zipFile.getProgressMonitor();
            progressView.show();
            Thread proc = getProgressThread("正在解压文件",monitor,null);
            if(!entry.isDictionary()) {
                FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1));
                if (header == null){
                    progressView.finish();
                    return;
                }
                proc.start();
                zipFile.setRunInThread(true);
                zipFile.extractFile(header,target.getAbsolutePath());
                return;
            }
            List<ArchiveEntry> children = entry.getChildren();
            for (ArchiveEntry item: children) {
                this.extractFile(file,item,target);
            }
        } catch (Exception e){
            logger.error("fail to extract file: ",e);
        }
    }

    @Override
    public void extractFiles(ArchiveFile file, File target) {
        ZipFile zipFile = new ZipFile(file.getFile());
        zipFile.setCharset(file.getCharset());
        try {
            progressView.show();
            ProgressMonitor monitor = zipFile.getProgressMonitor();
            Thread prog = getProgressThread("正在解压", monitor,null);
            prog.start();
            zipFile.setRunInThread(true);
            zipFile.extractAll(target.getAbsolutePath());
        } catch (Exception e) {
            logger.error("fail to extract files",e);
        }
    }

    private Thread getProgressThread(String message, ProgressMonitor monitor, Consumer<Void> next) {
        return new Thread(() -> {
            try {
                while (monitor.getPercentDone() < 100) {
                    Thread.sleep(500);
                    String name = monitor.getFileName();
                    progressView.update(message + " : " + name, monitor.getPercentDone() / 100.0);
                }
                progressView.finish();
                if (next != null) {
                    next.accept(null);
                }
            } catch (Exception ignore) {
            }
        });
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
