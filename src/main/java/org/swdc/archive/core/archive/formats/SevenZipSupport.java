package org.swdc.archive.core.archive.formats;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.fx.AppComponent;
import org.swdc.fx.LifeCircle;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public interface SevenZipSupport extends LifeCircle {

    String sevenZipPlatforms = "sevenzipjbinding-platforms.properties";
    String sevenZipModuleName = "sevenzipjbinding.all.platforms";
    String sevenZipLibDesc = "sevenzipjbinding-lib.properties";

    /**
     * 根据index，返回一个7z的文件描述对象
     */
    @FunctionalInterface
    interface ItemInformation {
        IOutItemBase getInfo(int i, OutItemFactory outItemFactory) throws SevenZipException;
    }

    /**
     * 根据index，返回一个7z的输入流对象
     */
    @FunctionalInterface
    interface IndexedItemStream {
        ISequentialInStream getStream(int i) throws SevenZipException, FileNotFoundException;
    }


    /**
     * 创建7z的文件写入接口。
     *
     * @param information 获取当前index的文件的描述信息的接口对象
     * @param steam 获取当前index的流的接口对象
     * @return 创建的写入接口对象
     */
    static IOutCreateCallback createOutCallback(ItemInformation information, IndexedItemStream steam) {
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

    default void initializePlatforms(AppComponent component) {
        try {
            Logger logger = LoggerFactory.getLogger(this.getClass());
            if (!SevenZip.isInitializedSuccessfully()) {
                // 读取7z-jbinding的模块，验证动态库信息。
                Module module = ModuleLayer.boot().findModule(sevenZipModuleName).orElse(null);
                File assets = new File(component.getAssetsPath() + "/sevenzip");
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
                    setWriteable(true);
                }
            }
        } catch (Exception e) {

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

    void setWriteable(boolean writeable);

}
