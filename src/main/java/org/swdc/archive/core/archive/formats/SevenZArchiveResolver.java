package org.swdc.archive.core.archive.formats;

import javafx.application.Platform;
import lombok.Builder;
import lombok.Data;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.apache.commons.compress.archivers.sevenz.*;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.ProgressView;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import java.util.stream.Collectors;

public class SevenZArchiveResolver extends ArchiveResolver {

    private ProgressView progressView = null;

    private static final String sevenZipPlatforms = "sevenzipjbinding-platforms.properties";
    private static final String sevenZipModuleName = "sevenzipjbinding.all.platforms";
    private static final String sevenZipLibDesc = "sevenzipjbinding-lib.properties";

    /**
     * 根据index，返回一个7z的文件描述对象
     */
    @FunctionalInterface
    private interface ItemInformation {
        IOutItemBase getInfo(int i, OutItemFactory outItemFactory) throws SevenZipException;
    }

    /**
     * 根据index，返回一个7z的输入流对象
     */
    @FunctionalInterface
    private interface IndexedItemStream {
        ISequentialInStream getStream(int i) throws SevenZipException, FileNotFoundException;
    }

    /**
     * 创建7z的文件写入接口。
     *
     * @param information 获取当前index的文件的描述信息的接口对象
     * @param steam 获取当前index的流的接口对象
     * @return 创建的写入接口对象
     */
    private static IOutCreateCallback createOutCallback(ItemInformation information, IndexedItemStream steam) {
        return new IOutCreateCallback() {
            @Override
            public void setOperationResult(boolean b) throws SevenZipException {

            }

            @Override
            public IOutItemBase getItemInformation(int i, OutItemFactory outItemFactory) throws SevenZipException {
                return information.getInfo(i,outItemFactory);
            }

            @Override
            public ISequentialInStream getStream(int i) throws SevenZipException {
                try {
                    return steam.getStream(i);
                } catch (Exception e) {
                    throw new SevenZipException(e);
                }
            }

            @Override
            public void setTotal(long l) throws SevenZipException {

            }

            @Override
            public void setCompleted(long l) throws SevenZipException {

            }
        };
    }

    private boolean writeable = false;

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
    }

    /**
     * Seven-zip Jbinding类库不支持直接从jpms下初始化。
     * 我们需要手动读取module并且载入类库。
     */
    @Override
    public void initialize() {
        try {
            // 读取7z-jbinding的模块，验证动态库信息。
            Module module = ModuleLayer.boot().findModule(sevenZipModuleName).orElse(null);
            File assets = new File(getAssetsPath() + "/sevenzip");
            File desc = new File(assets.getAbsolutePath() + "/" + sevenZipPlatforms);
            if (!assets.exists()) {
                // 准备释放动态库
                assets.mkdirs();
                FileOutputStream descOut = new FileOutputStream(desc);
                module.getResourceAsStream(sevenZipPlatforms).transferTo(descOut);
                descOut.close();
            }

            FileInputStream descInputStream = new FileInputStream(desc);
            Properties platforms = new Properties();
            platforms.load(descInputStream);
            descInputStream.close();

            for (String item: platforms.stringPropertyNames()) {
                // 释放动态类库
                File assetPlatformFolder = new File(assets.getAbsolutePath() + "/" + platforms.getProperty(item));
                if (!assetPlatformFolder.exists()) {
                    assetPlatformFolder.mkdirs();
                    String platformName = platforms.getProperty(item);
                    InputStream fileDesc = module.getResourceAsStream(platformName + "/" + sevenZipLibDesc);

                    FileOutputStream descOut = new FileOutputStream(assetPlatformFolder.getAbsolutePath() + "/" + sevenZipLibDesc);
                    module.getResourceAsStream(platformName + "/" + sevenZipLibDesc).transferTo(descOut);
                    descOut.close();
                    fileDesc.close();

                    fileDesc = module.getResourceAsStream(platformName + "/" + sevenZipLibDesc);
                    Properties libFileDesc = new Properties();
                    libFileDesc.load(fileDesc);
                    String fileName = libFileDesc.getProperty("lib.1.name");
                    FileOutputStream libFile = new FileOutputStream(assetPlatformFolder.getAbsolutePath() + "/" + fileName);
                    module.getResourceAsStream(platformName + "/" + fileName).transferTo(libFile);
                    fileDesc.close();
                    libFile.close();
                }
            }
            // 匹配操作系统
            List<String> platformsList = platforms.values()
                    .stream().map(Object::toString)
                    .collect(Collectors.toList());
            String platformTarget = getBestMatch(platformsList);
            if (platformTarget == null) {
                logger.error("can not initialize SevenZip. no native platform for system");
                logger.error("write method will not be available");
            } else {
                // 加载系统信息，初始化7z的native动态库。
                Properties libFileDesc = new Properties();
                InputStream libDesc = new FileInputStream(assets.getAbsolutePath() + "/" + platformTarget + "/" + sevenZipLibDesc);
                libFileDesc.load(libDesc);
                libDesc.close();
                String fileName = libFileDesc.getProperty("lib.1.name");
                System.load(new File(assets.getAbsolutePath() + "/" + platformTarget + "/" + fileName).getAbsolutePath());
                SevenZip.initLoadedLibraries();
                writeable = true;
            }
            Platform.runLater(() -> this.progressView = findView(ProgressView.class));
        } catch (Exception e) {
            logger.error("fail to initialize : ",e);
        }
    }

    /**
     * 返回最匹配的系统，参考7z-jbinding本身的实现
     * @param platforms
     * @return
     */
    private String getBestMatch(List<String> platforms) {
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name").split(" ")[0];
        if (platforms.contains(name + "-" + arch)) {
            return name + "-" + arch;
        }
        return null;
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

            fileOutStream.close();
            archive.close();
            fileInStream.close();
            origin.close();
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
            return Collections.emptyList();
        }
    }

    @Override
    public void addFolder(ArchiveFile target, ArchiveEntry position, File folder) {
        try {
            RandomAccessFile origin = new RandomAccessFile(target.getFile().getAbsolutePath(), "rw");
            RandomAccessFile originTemp = new RandomAccessFile(target.getFile().getAbsolutePath() + ".tmp", "rw");
            RandomAccessFileInStream fileInStream = new RandomAccessFileInStream(origin);
            RandomAccessFileOutStream fileOutStream = new RandomAccessFileOutStream(originTemp);
            IInArchive archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, fileInStream);
            if (archive == null) {
                return;
            }
            if (position == null) {
                position = target.getRootEntry();
            }
            if (!position.isDictionary()) {
                position = position.getParent();
            }
            if (!target.isWriteable()) {
                return;
            }
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
                    SevenEntry entry = folderContents.get(index);
                    IOutItem7z item = (IOutItem7z) outItemFactory.createOutItem();
                    item.setPropertyPath(outFolderPath + entry.getRelative());
                    item.setDataSize(entry.getSize());
                    item.setPropertyIsDir(entry.isFolder());
                    return item;
                }
                return outItemFactory.createOutItem(i);
            },idx -> {
                if (idx > countsOriginal - 1)  {
                    try {
                        int index = idx - countsOriginal;
                        SevenEntry entry = folderContents.get(index);
                        if (entry.isFolder()) {
                            return null;
                        }
                        return new RandomAccessFileInStream(new RandomAccessFile(entry.getFullPath(),"r"));
                    } catch (Exception e) {
                        logger.error("fail to open input file");
                    }
                }
                return null;
            }));

            fileOutStream.close();
            archive.close();
            fileInStream.close();
            origin.close();
            target.getFile().delete();
            File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
            temp.renameTo(target.getFile());

            SevenZFile sevenZFile = new SevenZFile(target.getFile());
            Iterable<SevenZArchiveEntry> entryIterable = sevenZFile.getEntries();
            for (SevenZArchiveEntry sevenZArchiveEntry : entryIterable) {
                ArchiveEntry entry = resolveEntry(target,target.getRootEntry(),sevenZArchiveEntry);
                this.mountArchiveEntry(entry);
            }
            this.emit(new ViewRefreshEvent(position,this));
        } catch (Exception e) {
            logger.error("fail to add folder: ",e);
        }
    }

    @Override
    public boolean removeFile(ArchiveFile target, ArchiveEntry entry) {
        return false;
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
            SevenZFile sevenZFile = new SevenZFile(file);
            ArchiveFile archiveFile = new ArchiveFile(file);
            archiveFile.setResolver(this.getClass());
            archiveFile.setWriteable(this.writeable);

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
            logger.error("fail to load sevenZ file", e);
            return null;
        }
    }

}
