package org.swdc.archive.core.archive.formats;

import javafx.application.Platform;
import lombok.SneakyThrows;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.apache.commons.compress.archivers.sevenz.*;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.ProgressView;

import java.io.*;
import java.util.List;
import java.util.Properties;

import java.util.stream.Collectors;

public class SevenZArchiveResolver extends ArchiveResolver {

    private ProgressView progressView = null;

    private static final String sevenZipPlatforms = "sevenzipjbinding-platforms.properties";
    private static final String sevenZipModuleName = "sevenzipjbinding.all.platforms";
    private static final String sevenZipLibDesc = "sevenzipjbinding-lib.properties";

    @Override
    public void initialize() {
        try {
            Module module = ModuleLayer.boot().findModule(sevenZipModuleName).orElse(null);
            File assets = new File(getAssetsPath() + "/sevenzip");
            File desc = new File(assets.getAbsolutePath() + "/" + sevenZipPlatforms);
            if (!assets.exists()) {
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
            List<String> platformsList = platforms.values()
                    .stream().map(Object::toString)
                    .collect(Collectors.toList());
            String platformTarget = getBestMatch(platformsList);
            if (platformTarget == null) {
                logger.error("can not initialize SevenZip. no native platform for system");
                logger.error("write method will not be available");
            } else {
                Properties libFileDesc = new Properties();
                InputStream libDesc = new FileInputStream(assets.getAbsolutePath() + "/" + platformTarget + "/" + sevenZipLibDesc);
                libFileDesc.load(libDesc);
                libDesc.close();
                String fileName = libFileDesc.getProperty("lib.1.name");
                System.load(new File(assets.getAbsolutePath() + "/" + platformTarget + "/" + fileName).getAbsolutePath());
                SevenZip.initLoadedLibraries();
            }
            Platform.runLater(() -> this.progressView = findView(ProgressView.class));
        } catch (Exception e) {
            logger.error("fail to initialize : ",e);
        }
    }

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
            String outPath = entry.getPath().substring(1) + file.getName();

            int counts = archive.getNumberOfItems();
            IOutUpdateArchive out = archive.getConnectedOutArchive7z();
            out.updateItems(fileOutStream, counts, new IOutCreateCallback() {
                @Override
                public void setOperationResult(boolean b) throws SevenZipException {

                }

                @Override
                public IOutItemBase getItemInformation(int i, OutItemFactory outItemFactory) throws SevenZipException {
                    if (i == counts - 1) {
                        IOutItem7z item = (IOutItem7z) outItemFactory.createOutItem();
                        item.setPropertyPath(outPath);
                        item.setDataSize(file.length());
                        return item;
                    }
                    return outItemFactory.createOutItem(i);
                }

                @Override
                public ISequentialInStream getStream(int i) throws SevenZipException {
                    if (i == counts - 1) {
                        try {
                            return new RandomAccessFileInStream(new RandomAccessFile(file.getAbsolutePath(),"r"));
                        } catch (Exception e) {
                            logger.error("fail to open input file");
                        }
                    }
                    return null;
                }

                @Override
                public void setTotal(long l) throws SevenZipException {

                }

                @Override
                public void setCompleted(long l) throws SevenZipException {

                }
            });
            fileOutStream.close();
            archive.close();
            fileInStream.close();
            origin.close();
            target.getFile().delete();
            File temp = new File(target.getFile().getAbsolutePath() + ".tmp");
            temp.renameTo(target.getFile());
        } catch (Exception e) {
            logger.error("fail to add file", e);
        }
    }

    @Override
    public void addFolder(ArchiveFile target, ArchiveEntry position, File folder) {

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
    public void moveFile(ArchiveFile file, ArchiveEntry form, ArchiveEntry target) {

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
