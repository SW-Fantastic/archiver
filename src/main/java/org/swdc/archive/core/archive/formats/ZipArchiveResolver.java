package org.swdc.archive.core.archive.formats;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.controlsfx.control.Notifications;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.DataUtil;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.MessageView;
import org.swdc.archive.ui.view.ProgressView;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
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
            ArchiveFile zipArchiveFile = new ArchiveFile(file);
            zipArchiveFile.setCharset(charset);
            zipArchiveFile.setWriteable(true);

            ZipFile zipFile = new ZipFile(file);
            zipFile.setCharset(charset);
            progressView.update("验证压缩文件：" + file.getName(), 20);
            if (!zipFile.isValidZipFile()) {
                progressView.finish();
                UIUtil.notification("无法打开文件：" + file.getName() + "，他不是一个有效的zip压缩文件。",this);
                return null;
            }
            ArchiveEntry rootEntry = new ArchiveEntry();
            rootEntry.setFileName("/");
            rootEntry.setFile(zipArchiveFile);
            rootEntry.setDictionary(true);
            progressView.update("读取内容",60);
            List<FileHeader> headers = zipFile.getFileHeaders();
            for (FileHeader header: headers) {
                resolveEntry(zipArchiveFile, rootEntry, header);
            }
            zipArchiveFile.setRoot(rootEntry);
            zipArchiveFile.setResolver(this.getClass());
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
            String path = position.getPath();
            path = path.substring(1, path.length() - 1);
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
                    UIUtil.notification("部分文件添加失败: \n" + UIUtil.exceptionToString(e), this);
                    logger.error("fail to reload file tree",e);
                }
            });
            progressView.show();
            progressor.start();
            zipFile.setRunInThread(true);
            zipFile.addFolder(folder,parameters);
        } catch (Exception e) {
            UIUtil.notification("添加文件夹失败: \n" + UIUtil.exceptionToString(e),this);
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
        String name = parent.substring(1);
        ZipParameters parameters = new ZipParameters();
        parameters.setRootFolderNameInZip(name);
        parameters.setOverrideExistingFilesInZip(true);
        try {
            zipFile.addFile(file,parameters);
            ArchiveEntry created = new ArchiveEntry();
            created.setFileName(file.getName());
            created.setParent(entry);
            created.setLastModifiedDate(new Date());
            created.setSize(file.length());
            created.setDictionary(false);
            created.setFile(target);
            entry.getChildren().add(created);
            this.emit(new ViewRefreshEvent(created,this));
        } catch (Exception e) {
            UIUtil.notification("文件添加失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to add file",e);
        }
    }

    @Override
    public void removeFile(ArchiveFile target, ArchiveEntry entry) {
        try {
            ZipFile zipFile = new ZipFile(target.getFile());
            zipFile.setCharset(target.getCharset());
            FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1));
            if (header == null) {
                return;
            }
            zipFile.removeFile(header);
            removeEntry(target.getRootEntry(),entry);
            this.emit(new ViewRefreshEvent(entry,this));
        } catch (Exception e) {
            UIUtil.notification("文件删除失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to remove file", e);
            return;
        }
    }

    @Override
    public void createArchive(File target) {

    }

    @Override
    public void rename(ArchiveFile file, ArchiveEntry target, String newName) {
        try {
            ZipFile zipFile = new ZipFile(file.getFile());
            zipFile.setCharset(file.getCharset());
            String headerName = target.getPath().substring(1);
            FileHeader header = zipFile.getFileHeader(headerName);
            if (header == null) {
                return;
            }
            String renamedPath = headerName.replace(target.getFileName(), newName);
            zipFile.renameFile(header,renamedPath);
            target.setFileName(newName);
            this.emit(new ViewRefreshEvent(target,this));
        } catch (Exception e) {
            UIUtil.notification("文件重命名失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to rename file or folder", e);
        }
    }

    @Override
    public void extractFile(ArchiveFile file, ArchiveEntry entry, File target) {
        extractFileImpl(file,entry,target,false);
    }

    public void extractFileImpl(ArchiveFile file, ArchiveEntry entry, File target, boolean cascade) {
        try {
            ZipFile zipFile = new ZipFile(file.getFile());
            zipFile.setCharset(file.getCharset());
            ProgressMonitor monitor = zipFile.getProgressMonitor();
            if (!cascade) {
                progressView.show();
            }
            Thread proc = getProgressThread("正在解压文件",monitor,null);
            if(!entry.isDictionary()) {
                FileHeader header = zipFile.getFileHeader(entry.getPath().substring(1));
                if (header == null && !cascade){
                    progressView.finish();
                    return;
                }
                proc.start();
                zipFile.setRunInThread(true);
                zipFile.extractFile(header,target.getAbsolutePath());
                return;
            }
            double prog;
            int resolved = 0;
            List<ArchiveEntry> children = entry.getChildren();
            for (ArchiveEntry item: children) {
                resolved ++;
                prog = resolved + 0.0 / children.size();
                progressView.update("正在解压：" + item.getFileName(), prog);
                this.extractFileImpl(file,item,target,true);
            }
        } catch (Exception e){
            UIUtil.notification("文件解压失败: \n" + UIUtil.exceptionToString(e), this);
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
            UIUtil.notification("文件解压失败: \n" + UIUtil.exceptionToString(e), this);
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
