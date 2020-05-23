package org.swdc.archive.core.archive.formats;

import javafx.application.Platform;
import lombok.Builder;
import lombok.Data;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.ProgressView;

import static org.swdc.archive.core.archive.formats.SevenZipSupport.createExtractCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RarArchiveResolver extends ArchiveResolver implements SevenZipSupport {

    private boolean writeable = false;

    private ProgressView progressView;

    @Data
    @Builder
    private static class RarEntry{
        private String path;
        private String name;
        private boolean directory;
        private long size;
        private Date lastModifiedDate;
    }

    @Override
    public void initialize() {
        try {
            this.initializePlatforms(this);
            Platform.runLater(() -> this.progressView = findView(ProgressView.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addFile(ArchiveFile target, ArchiveEntry entry, File file) {

    }

    @Override
    public void addFolder(ArchiveFile target, ArchiveEntry position, File folder) {

    }

    @Override
    public void removeFile(ArchiveFile target, ArchiveEntry entry) {

    }

    @Override
    public void createArchive(File target) {

    }

    @Override
    public void rename(ArchiveFile file, ArchiveEntry target, String newName) {

    }

    @Override
    public void extractFile(ArchiveFile file, ArchiveEntry entry, File target) {

    }

    @Override
    public void extractFiles(ArchiveFile file, File target) {
        CompletableFuture.runAsync(() -> {
            try {
                progressView.show();
                RandomAccessFile originalFile = new RandomAccessFile(file.getFile().getAbsolutePath(), "rw");
                RandomAccessFileInStream inStream = new RandomAccessFileInStream(originalFile);
                IInArchive archive = null;
                progressView.update("检测文件类型",0);
                try {
                    archive = SevenZip.openInArchive(ArchiveFormat.RAR5, inStream);
                } catch (Exception e) {
                    archive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream);
                }
                int counts = archive.getNumberOfItems();
                List<Integer> allIndexes = new ArrayList<>();
                for (int idx = 0; idx < counts; idx ++) {
                    progressView.update("正在索引", (idx + 0.0) / counts);
                    boolean isFolder = archive.getStringProperty(idx,PropID.IS_FOLDER).equals("+");
                    if (!isFolder) {
                        allIndexes.add(idx);
                    }
                }
                IInArchive inArchive = archive;
                progressView.update("正在开始：",0);
                archive.extract(allIndexes.stream().mapToInt(i -> i).toArray(),false,createExtractCallback((i, mode) -> {
                    try {
                        String path = inArchive.getStringProperty(i,PropID.PATH);
                        File targetFile = new File(target.getAbsolutePath() + File.separator + path.replace("\\","/"));
                        if (targetFile.exists()) {
                            targetFile.delete();
                        }
                        return bytes-> {
                            try {
                                progressView.update("正在解压：" + path,(i + 0.0) / counts);
                                if (!targetFile.getParentFile().exists()) {
                                    targetFile.getParentFile().mkdirs();
                                }
                                FileOutputStream outputStream = new FileOutputStream(targetFile,true);
                                outputStream.write(bytes);
                                outputStream.close();
                                return bytes.length;
                            } catch (Exception e) {
                                logger.error("fail to extract file: ",e);
                                return 0;
                            }
                        };
                    } catch (Exception e) {
                        logger.error("fail to extract file: ",e);
                        return null;
                    }
                },result -> {
                    return;
                }));
                this.closeAllResources(archive,inStream,originalFile);
                progressView.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String getName() {
        return "WinRar文件";
    }

    @Override
    public String getExtension() {
        return "rar";
    }

    private ArchiveEntry resolveEntry(ArchiveFile file,ArchiveEntry root, RarEntry archiveEntry){
        String fullPath = archiveEntry.getPath();
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
        if (archiveEntry.getSize() > 0) {
            parent.setSize(archiveEntry.getSize());
        }
        parent.setLastModifiedDate(archiveEntry.getLastModifiedDate());
        parent.setFile(file);
        return parent;
    }

    @Override
    public ArchiveFile loadFile(File file) {
        try {
            progressView.show();
            ArchiveFile archiveFile = new ArchiveFile(file);
            RandomAccessFile originalFile = new RandomAccessFile(file.getAbsolutePath(),"rw");
            RandomAccessFileInStream inStream = new RandomAccessFileInStream(originalFile);
            IInArchive archive = null;
            progressView.update("检测格式：普通RAR", 0);
            try {
                archive = SevenZip.openInArchive(ArchiveFormat.RAR5,inStream);
            } catch (Exception e) {
                progressView.update("检测格式：RAR5", 0);
                archive = SevenZip.openInArchive(ArchiveFormat.RAR,inStream);
            }

            ArchiveEntry root = new ArchiveEntry();
            root.setFileName("/");
            root.setParent(null);
            root.setDictionary(true);
            root.setFile(archiveFile);
            archiveFile.setRoot(root);
            archiveFile.setResolver(this.getClass());
            archiveFile.setWriteable(false);

            int counts = archive.getNumberOfItems();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            for (int idx = 0; idx < counts; idx++) {
                progressView.update("索引内容：", (idx + 0.0) / counts);
                Date lastModify = sdf.parse(archive.getStringProperty(idx,PropID.LAST_MODIFICATION_TIME));
                RarEntry entry = RarEntry.builder()
                        .name(archive.getStringProperty(idx,PropID.NAME))
                        .directory(archive.getStringProperty(idx,PropID.IS_FOLDER).equals("+"))
                        .path(archive.getStringProperty(idx, PropID.PATH).replace("\\","/"))
                        .lastModifiedDate(lastModify)
                        .size(Long.parseLong(archive.getStringProperty(idx, PropID.SIZE)))
                        .build();
                resolveEntry(archiveFile, archiveFile.getRootEntry(),entry);
            }
            progressView.finish();
            return archiveFile;
        } catch (Exception e) {
            logger.error("fail to read rar file", e);
            return null;
        }
    }

    @Override
    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }
}
