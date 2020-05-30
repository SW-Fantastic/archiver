package org.swdc.archive.core.archive.formats;

import javafx.application.Platform;
import lombok.Builder;
import lombok.Data;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.apache.commons.compress.archivers.sevenz.*;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.core.archive.formats.creators.CreatorView;
import org.swdc.archive.core.archive.formats.creators.SevenZipCreatorView;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.ProgressView;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.swdc.archive.core.archive.formats.SevenZipSupport.createOutCallback;

public class SevenZArchiveResolver extends ArchiveResolver implements SevenZipSupport{

    private ProgressView progressView = null;

    private boolean writeable = false;

    @Override
    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    /**
     * 本地文件描述符
     * 遍历文件夹，读取对应的路径
     * 和字节长度，保存备用。
     */
    @Data
    @Builder
    public static class SevenEntry {
        private String fullPath;
        private boolean folder;
        private String relative;
        private long size;
        private int index;
    }

    /**
     * Seven-zip Jbinding类库不支持直接从jpms下初始化。
     * 我们需要手动读取module并且载入类库。
     */
    @Override
    public void initialize() {
        try {
            this.initializePlatforms(this);
            Platform.runLater(() -> this.progressView = findView(ProgressView.class));
        } catch (Exception e) {
            UIUtil.notification("无法载入native动态类库: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to initialize : ",e);
        }
    }

    @Override
    public boolean creatable() {
        return true;
    }

    @Override
    public void saveComment(ArchiveFile file, String data) {
        UIUtil.notification("此格式不允许修改压缩文件注释。",this);
       return;
    }

    @Override
    public void addFile(ArchiveFile target, ArchiveEntry entry, File file) {
        try {
            RandomAccessFile origin = new RandomAccessFile(target.getFile().getAbsolutePath(), "rw");
            RandomAccessFile originTemp = new RandomAccessFile(target.getFile().getAbsolutePath() + ".tmp", "rw");
            RandomAccessFileInStream fileInStream = new RandomAccessFileInStream(origin);
            RandomAccessFileOutStream fileOutStream = new RandomAccessFileOutStream(originTemp);
            IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, fileInStream);
            if (archive == null) {
                return;
            }
            if (!entry.isDictionary()) {
                entry = entry.getParent();
            }
            String outFolder = entry.getPath().substring(1);
            while (outFolder.startsWith("/")) {
                outFolder = outFolder.substring(1);
            }
            String outPath = outFolder + file.getName();

            int counts = archive.getNumberOfItems();
            IOutUpdateArchive out = archive.getConnectedOutArchive7z();
            out.updateItems(fileOutStream, counts, createOutCallback((i,factory)-> {
                if (i == counts - 1) {
                    IOutItem7z item = (IOutItem7z) factory.createOutItem();
                    item.setPropertyPath(outPath);
                    item.setDataSize(file.length());
                    return item;
                }
                return factory.createOutItem(i);
            }, index-> {
                if (index == counts - 1) {
                    return new RandomAccessFileInStream(new RandomAccessFile(file.getAbsolutePath(),"r"));
                }
                return null;
            }));

            this.closeAllResources(fileOutStream,archive,fileInStream,origin);
            target.getFile().delete();
            File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
            temp.renameTo(target.getFile());
            ArchiveEntry created = new ArchiveEntry();
            created.setFile(target);
            created.setDictionary(false);
            created.setParent(entry);
            created.setFileName(file.getName());
            created.setLastModifiedDate(new Date());
            created.setSize(file.length());
            entry.getChildren().add(created);
            this.emit(new ViewRefreshEvent(created,this));
        } catch (Exception e) {
            UIUtil.notification("文件添加失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to add file", e);
        }
    }

    private List<SevenEntry> listFolderContent(File folder) {
        try {
            List<SevenEntry> paths = new ArrayList<>();
            Path folderPath = folder.getAbsoluteFile().toPath();
            Files.walkFileTree(folderPath, new SimpleFileVisitor<>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file == null || attrs == null) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    SevenEntry entry = SevenEntry.builder()
                            .folder(false)
                            .relative(folderPath.relativize(file).toString())
                            .size(Files.size(file))
                            .fullPath(file.toString())
                            .build();
                    paths.add(entry);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir == null || attrs == null) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (dir.equals(folderPath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    SevenEntry entry = SevenEntry.builder().fullPath(dir.toString())
                            .size(0)
                            .folder(true)
                            .relative(folderPath.relativize(dir).toString())
                            .build();
                    paths.add(entry);
                    return super.preVisitDirectory(dir, attrs);
                }
            });
            return paths;
        } catch (Exception e) {
            logger.error("fail to read folder", e);
            UIUtil.notification("文件索引失败: \n" + UIUtil.exceptionToString(e), this);
            return Collections.emptyList();
        }
    }

    @Override
    public void addFolder(ArchiveFile target, ArchiveEntry entry, File folder) {
        CompletableFuture.runAsync(() -> {
            try {
                progressView.show();
                RandomAccessFile origin = new RandomAccessFile(target.getFile().getAbsolutePath(), "rw");
                RandomAccessFile originTemp = new RandomAccessFile(target.getFile().getAbsolutePath() + ".tmp", "rw");
                RandomAccessFileInStream fileInStream = new RandomAccessFileInStream(origin);
                RandomAccessFileOutStream fileOutStream = new RandomAccessFileOutStream(originTemp);
                IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, fileInStream);
                if (archive == null) {
                    return;
                }
                ArchiveEntry position = entry;
                if (position == null) {
                    position = target.getRootEntry();
                }
                if (!position.isDictionary()) {
                    position = position.getParent();
                }
                if (!target.isWriteable()) {
                    return;
                }
                progressView.update("正在索引文件：", 0.1);
                String outPath = position.getPath().substring(1);
                outPath = outPath + "/" + folder.getName() + "/";
                while (outPath.startsWith("/")) {
                    outPath = outPath.substring(1);
                }
                String outFolderPath = outPath;
                List<SevenEntry> folderContents = this.listFolderContent(folder);

                int countsOriginal = archive.getNumberOfItems();
                int counts = countsOriginal + folderContents.size();
                IOutUpdateArchive out = archive.getConnectedOutArchive7z();
                out.updateItems(fileOutStream,counts,createOutCallback((i, outItemFactory) -> {
                    if (i > countsOriginal - 1) {

                        int index = i - countsOriginal;
                        SevenEntry entryItem = folderContents.get(index);
                        progressView.update("初始化：" + entryItem.getRelative(), (index + 0.0) / folderContents.size());
                        IOutItem7z item = (IOutItem7z) outItemFactory.createOutItem();
                        item.setPropertyPath(outFolderPath + entryItem.getRelative());
                        item.setDataSize(entryItem.getSize());
                        item.setPropertyIsDir(entryItem.isFolder());
                        return item;
                    }
                    return outItemFactory.createOutItem(i);
                },idx -> {
                    if (idx > countsOriginal - 1)  {
                        try {
                            int index = idx - countsOriginal;
                            SevenEntry item = folderContents.get(index);
                            progressView.update("添加文件：" + item.getRelative(), (index + 0.0) /folderContents.size());
                            if (item.isFolder()) {
                                return null;
                            }
                            return new RandomAccessFileInStream(new RandomAccessFile(item.getFullPath(),"r"));
                        } catch (Exception e) {
                            UIUtil.notification("部分文件添加失败: \n" + UIUtil.exceptionToString(e), this);
                            logger.error("fail to open input file",e);
                        }
                    }
                    return null;
                }));
                this.closeAllResources(fileOutStream,archive,fileInStream,origin);
                target.getFile().delete();
                File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
                temp.renameTo(target.getFile());
                progressView.update("清理临时文件", 0.9);
                SevenZFile sevenZFile = new SevenZFile(target.getFile());
                Iterable<SevenZArchiveEntry> entryIterable = sevenZFile.getEntries();
                progressView.update("更新数据", 0.9);
                for (SevenZArchiveEntry sevenZArchiveEntry : entryIterable) {
                    ArchiveEntry item = resolveEntry(target,target.getRootEntry(),sevenZArchiveEntry);
                    this.mountArchiveEntry(item);
                }
                this.emit(new ViewRefreshEvent(position,this));
                progressView.finish();
            } catch (Exception e) {
                logger.error("fail to add folder: ",e);
                UIUtil.notification("文件夹添加失败: \n" + UIUtil.exceptionToString(e), this);
                progressView.finish();
            }
        });
    }

    @Override
    public void removeFile(ArchiveFile target, ArchiveEntry entry) {
        try {
            this.removeFileImpl(target,entry,entry.isDictionary(), (Void) -> {
                removeEntry(target.getRootEntry(), entry);
                this.emit(new ViewRefreshEvent(entry,this));
            });
        } catch (Exception e) {
            logger.error("fail to remove file", e);
        }
    }

    private void removeFileImpl(ArchiveFile target, ArchiveEntry entry, boolean reIndex, Consumer consumer) throws Exception {
        CompletableFuture.runAsync(() -> {
            progressView.show();
            try {
                progressView.update("正在准备删除",0);
                RandomAccessFile originalFile = new RandomAccessFile(target.getFile().getAbsolutePath(), "r");
                RandomAccessFileInStream archiveIn = new RandomAccessFileInStream(originalFile);
                RandomAccessFile originTemp = new RandomAccessFile(target.getFile().getAbsolutePath() + ".tmp", "rw");
                RandomAccessFileOutStream fileOutStream = new RandomAccessFileOutStream(originTemp);

                IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, archiveIn);

                List<Integer> removedIndexes = new ArrayList<>();
                List<Integer> removedFolders = new ArrayList<>();

                String path = entry.getPath().substring(1);
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }

                int count = archive.getNumberOfItems();
                progressView.update("正在索引",0.2);
                if (!entry.isDictionary()) {
                    int removeIdx = -1;
                    for (int idx = 0; idx < archive.getNumberOfItems(); idx++) {
                        String entryPath = archive.getStringProperty(idx, PropID.PATH).replace("\\", "/");
                        if (entryPath.equals(path)) {
                            removedIndexes.add(idx);
                            removeIdx = idx;
                            break;
                        }
                    }
                    if (removeIdx == -1) {
                        this.closeAllResources(archive,archiveIn,originalFile,fileOutStream,originTemp);
                        return;
                    }
                } else {
                    for (int idx = 0; idx < archive.getNumberOfItems(); idx++) {
                        String entryPath = archive.getStringProperty(idx, PropID.PATH).replace("\\", "/");
                        String isFolder = archive.getStringProperty(idx, PropID.IS_FOLDER);
                        boolean folder = isFolder.equals("+");
                        if (entryPath.startsWith(path) && !folder) {
                            removedIndexes.add(idx);
                        } else if ((entryPath + "/").startsWith(path)) {
                            removedFolders.add(idx);
                        }
                    }
                }
                progressView.update("正在删除数据",0.6);
                IOutUpdateArchive out = archive.getConnectedOutArchive7z();
                try {
                    out.updateItems(fileOutStream, count, createOutCallback((i, factory) -> {
                        if (removedIndexes.contains(i) || removedFolders.contains(i)) {
                            int pos = i;
                            while (removedIndexes.contains(pos) || removedFolders.contains(pos)) {
                                pos++;
                            }
                            return factory.createOutItem(pos);
                        }
                        return factory.createOutItem(i);
                    }, i -> null));
                } catch (Exception e) {
                    this.closeAllResources(archive,archiveIn,originalFile,fileOutStream,originTemp);
                    File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
                    temp.delete();
                    progressView.finish();
                    UIUtil.notification("部分文件删除失败: \n" + UIUtil.exceptionToString(e), this);
                    logger.error("can not remove some file",e);
                    return;
                }
                this.closeAllResources(archive,archiveIn,originalFile,fileOutStream,originTemp);
                target.getFile().delete();
                File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
                temp.renameTo(target.getFile());
                if (reIndex) {
                    removeFileImpl(target,entry,false, consumer);
                }
                if (consumer != null) {
                    consumer.accept(null);
                }
                progressView.finish();
            } catch (Exception e) {
                logger.error("fail to remove file",e);
                UIUtil.notification("文件删除失败: \n" + UIUtil.exceptionToString(e), this);
            }
        });
    }

    @Override
    public void create(File target, List<File> files, Object param) {
        if (target == null || param == null || files == null || files.isEmpty()){
            return;
        }
        CompletableFuture.runAsync(() -> {
            progressView.show();
            progressView.update("正在索引文件",0);
            SevenZipCompressLevel compressLevel = (SevenZipCompressLevel)param;
            try {
                RandomAccessFile file = new RandomAccessFile(target.getAbsolutePath(), "rw");
                RandomAccessFileOutStream outStream = new RandomAccessFileOutStream(file);
                IOutCreateArchive7z archive7z = SevenZip.openOutArchive7z();
                archive7z.setLevel(compressLevel.getLevel());
                Map<Boolean,List<File>> fileList = files.stream().collect(Collectors.groupingBy(File::isDirectory,Collectors.toList()));

                List<SevenEntry> archiveContent = new ArrayList<>();
                if (fileList.containsKey(true)) {
                    List<SevenEntry> folderContents = fileList.get(true)
                            .stream()
                            .map(folder->{
                                List<SevenEntry> entries = this.listFolderContent(folder);
                                for(SevenEntry entry: entries) {
                                    entry.setRelative(folder.getName() + "/" + entry.getRelative());
                                }
                                return entries;
                            })
                            .flatMap(i->i.stream())
                            .collect(Collectors.toList());
                    archiveContent.addAll(folderContents);
                }
                if (fileList.containsKey(false)) {
                    List<SevenEntry> filesItem = fileList.get(false)
                            .stream().map(i -> SevenEntry.builder()
                                    .fullPath(i.getAbsolutePath())
                                    .relative(i.getName())
                                    .size(i.length())
                                    .build())
                            .collect(Collectors.toList());
                    archiveContent.addAll(filesItem);
                }

                try {
                    progressView.update("正在开始",0);
                    int count = archiveContent.size();
                    archive7z.createArchive(outStream,archiveContent.size(),createOutCallback((idx,factory)-> {
                        SevenEntry current = archiveContent.get(idx);
                        IOutItem7z item = (IOutItem7z) factory.createOutItem();
                        item.setDataSize(current.getSize());
                        item.setUpdateIsNewData(true);
                        item.setPropertyIsDir(current.isFolder());
                        item.setPropertyPath(current.getRelative());
                        return item;
                    },i -> {
                        SevenEntry entry = archiveContent.get(i);
                        progressView.update("正在添加：" + entry.getRelative(), (i + 0.0)/count);
                        if (!entry.isFolder()) {
                            RandomAccessFile targetFile = new RandomAccessFile(new File(entry.getFullPath()),"r");
                            return new RandomAccessFileInStream(targetFile);
                        }
                        return null;
                    }));
                    progressView.finish();
                    UIUtil.notification("压缩文件已经创建：" + target.getName(),this);
                } catch (Exception e) {
                    progressView.finish();
                    logger.error("fail to add some file", e);
                    UIUtil.notification("压缩文件创建失败：" + target.getName(),this);
                }
                closeAllResources(archive7z,outStream,file);
            } catch (Exception e) {
                progressView.finish();
                UIUtil.notification("压缩文件创建失败：" + target.getName(),this);
                logger.error("fail to create file",e);
            }
        });
    }

    @Override
    public Class<? extends CreatorView> getCreator() {
        return SevenZipCreatorView.class;
    }

    @Override
    public void rename(ArchiveFile file, ArchiveEntry target, String newName) {
        try {
            RandomAccessFile origin = new RandomAccessFile(file.getFile().getAbsolutePath(), "rw");
            RandomAccessFile originTemp = new RandomAccessFile(file.getFile().getAbsolutePath() + ".tmp", "rw");
            RandomAccessFileInStream fileInStream = new RandomAccessFileInStream(origin);
            RandomAccessFileOutStream fileOutStream = new RandomAccessFileOutStream(originTemp);
            IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, fileInStream);
            if (archive == null) {
                return;
            }
            String pathParent = target.getParent().getPath().substring(1);

            while (pathParent.startsWith("/")) {
                pathParent = pathParent.substring(1);
            }

            String pathOld = target.getPath().substring(1);
            while (pathOld.startsWith("/")) {
                pathOld = pathOld.substring(1);
            }

            Map<Integer,SevenEntry> updateIndexes = new HashMap<>();
            if (!target.isDictionary()) {
                int replacedIndex = -1;
                for (int idx = 0; idx < archive.getNumberOfItems(); idx++) {
                    String entryPath = archive.getStringProperty(idx, PropID.PATH).replace("\\", "/");
                    boolean isFolder = archive.getStringProperty(idx, PropID.IS_FOLDER).equals("+");
                    if (entryPath.equals(pathOld)) {
                        replacedIndex = idx;
                        updateIndexes.put(idx,SevenEntry.builder()
                                .index(idx)
                                .folder(isFolder)
                                .relative(entryPath.replace("\\","/"))
                                .build());
                        break;
                    }
                }
                if (replacedIndex == -1) {
                    this.closeAllResources(archive,fileInStream,origin,fileOutStream,originTemp);
                    return;
                }
            } else {
                for (int idx = 0; idx < archive.getNumberOfItems(); idx++) {
                    String entryPath = archive.getStringProperty(idx, PropID.PATH).replace("\\", "/");
                    boolean isFolder = archive.getStringProperty(idx, PropID.IS_FOLDER).equals("+");
                    if (entryPath.startsWith(pathOld)) {
                        updateIndexes.put(idx,SevenEntry.builder()
                                .index(idx)
                                .folder(isFolder)
                                .relative(entryPath.replace("\\","/"))
                                .build());
                    }
                }
            }

            IOutUpdateArchive out = archive.getConnectedOutArchive();
            String pathUpdate = pathParent;
            out.updateItems(fileOutStream,archive.getNumberOfItems(),createOutCallback((i, outItemFactory) -> {
                if (updateIndexes.containsKey(i)) {
                    IOutItem7z item = (IOutItem7z) outItemFactory.createOutItem(i);
                    SevenEntry entry = updateIndexes.get(i);
                    String newPath = entry.getRelative().replace(pathUpdate, "");
                    newPath = pathUpdate + newPath.replaceFirst(target.getFileName(), newName);
                    item.setPropertyPath(newPath.replace("/","\\"));
                    item.setPropertyIsDir(entry.isFolder());
                    item.setUpdateIsNewProperties(true);
                    return item;
                }
                return outItemFactory.createOutItem(i);
            }, i -> null));
            this.closeAllResources(archive,fileInStream,origin,fileOutStream,originTemp);
            file.getFile().delete();
            File temp = new File(file.getFile().getAbsolutePath() + ".tmp");
            temp.renameTo(file.getFile());
            if (target.isDictionary()) {
                this.removeFileImpl(file,target,false, (Void) -> {
                    target.setFileName(newName);
                    this.emit(new ViewRefreshEvent(target,this));
                });
            } else {
                target.setFileName(newName);
                this.emit(new ViewRefreshEvent(target,this));
            }
        } catch (Exception e) {
            UIUtil.notification("文件重命名失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to extract file", e);
        }
    }

    @Override
    public void extractFile(ArchiveFile file, ArchiveEntry entry, File target) {
        CompletableFuture.runAsync(() -> {
            try {
                progressView.show();
                String filePath = target.getAbsoluteFile().getPath();
                String path = entry.getPath().substring(1);
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }
                path = path.substring(0,path.length() - 1);
                SevenZFile sevenZFile = new SevenZFile(file.getFile());
                Iterable<SevenZArchiveEntry> iterable = sevenZFile.getEntries();
                Iterator<SevenZArchiveEntry> iterator = iterable.iterator();
                while (iterator.hasNext()) {
                    SevenZArchiveEntry sevenEntry = iterator.next();
                    progressView.update("正在索引文件",0);
                    if (!sevenEntry.getName().startsWith(path)) {
                        continue;
                    }
                    progressView.update("解压文件：" + sevenEntry.getName(), 0);
                    if (sevenEntry.isDirectory()) {
                        File dir = new File(filePath + File.separator + sevenEntry.getName());
                        dir.mkdirs();
                        continue;
                    }
                    File targetFile = new File(filePath + File.separator + sevenEntry.getName());
                    if (!targetFile.getParentFile().exists()){
                        targetFile.getParentFile().mkdirs();
                    }
                    InputStream in = sevenZFile.getInputStream(sevenEntry);
                    FileOutputStream outputStream = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[4096];
                    double progress = 0;
                    long total = sevenEntry.getSize();
                    long current = 0;
                    while (in.read(buffer) > 0) {
                        outputStream.write(buffer);
                        current = current + buffer.length;
                        progress = (current + 0.0) / total;
                        progressView.update("正在解压：" + sevenEntry.getName(), progress);
                    }
                    outputStream.flush();
                    this.closeAllResources(in,outputStream);
                    progressView.finish();
                }
            } catch (Exception e) {
                UIUtil.notification("文件解压失败: \n" + UIUtil.exceptionToString(e), this);
                logger.error("fail to extract file",e);
            }
        });
    }

    @Override
    public void extractFiles(ArchiveFile file, File target) {
        CompletableFuture.runAsync(() -> {
            try {
                progressView.show();
                String filePath = target.getAbsoluteFile().getPath();
                SevenZFile sevenZFile = new SevenZFile(file.getFile());
                Iterable<SevenZArchiveEntry> iterable = sevenZFile.getEntries();
                Iterator<SevenZArchiveEntry> iterator = iterable.iterator();
                progressView.update("正在索引文件",0);
                while (iterator.hasNext()) {
                    SevenZArchiveEntry entry = iterator.next();
                    progressView.update("解压文件：" + entry.getName(), 0);
                    if (entry.isDirectory()) {
                        File dir = new File(filePath + File.separator + entry.getName());
                        dir.mkdirs();
                        continue;
                    }
                    File targetFile = new File(filePath + File.separator + entry.getName());
                    if (!targetFile.getParentFile().exists()){
                        targetFile.getParentFile().mkdirs();
                    }
                    InputStream in = sevenZFile.getInputStream(entry);
                    FileOutputStream outputStream = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[4096];
                    double progress = 0;
                    long total = entry.getSize();
                    long current = 0;
                    while (in.read(buffer) > 0) {
                        outputStream.write(buffer);
                        current = current + buffer.length;
                        progress = (current + 0.0) / total;
                        progressView.update("正在解压：" + entry.getName(), progress);
                    }
                    outputStream.flush();
                    this.closeAllResources(in,outputStream);
                }
                progressView.finish();
            } catch (Exception e) {
                UIUtil.notification("文件解压失败: \n" + UIUtil.exceptionToString(e), this);
                logger.error("文件解压失败",e);
            }
        });
    }

    @Override
    public String getName() {
        return "7z压缩文件";
    }

    @Override
    public String getExtension() {
        return "7z";
    }

    private ArchiveEntry resolveEntry(ArchiveFile file,ArchiveEntry root, SevenZArchiveEntry archiveEntry){
        String fullPath = archiveEntry.getName();
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
            progressView.update("正在读取文件：" + file.getName(), 0);

            RandomAccessFile originalFile = new RandomAccessFile(file.getAbsolutePath(),"rw");
            RandomAccessFileInStream inStream = new RandomAccessFileInStream(originalFile);
            IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP,inStream);
            String comment = archive.getStringArchiveProperty(PropID.COMMENT);
            closeAllResources(archive,inStream,originalFile);

            SevenZFile sevenZFile = new SevenZFile(file);
            ArchiveFile archiveFile = new ArchiveFile(file);
            archiveFile.setResolver(this.getClass());
            archiveFile.setWriteable(this.writeable);
            archiveFile.setComment(comment);

            Iterable<SevenZArchiveEntry> entryIterable = sevenZFile.getEntries();
            ArchiveEntry root = new ArchiveEntry();
            root.setFileName("/");
            root.setParent(null);
            root.setDictionary(true);
            root.setFile(archiveFile);
            archiveFile.setRoot(root);

            progressView.update("正在读取内容 ：" + file.getName() , 0.5);
            for (SevenZArchiveEntry sevenZArchiveEntry : entryIterable) {
                resolveEntry(archiveFile,root,sevenZArchiveEntry);
            }
            progressView.finish();
            return archiveFile;
        } catch (Exception e) {
            UIUtil.notification("文件载入失败: \n" + UIUtil.exceptionToString(e), this);
            logger.error("fail to load sevenZ file", e);
            return null;
        }
    }

}
