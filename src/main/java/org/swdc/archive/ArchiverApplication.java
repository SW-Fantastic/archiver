package org.swdc.archive;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.swdc.archive.config.AppConfig;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.core.archive.ArchiveResolverManager;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXApplication;
import org.swdc.fx.FXSplash;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.SFXApplication;
import org.swdc.fx.container.ApplicationContainer;
import org.swdc.fx.properties.ConfigManager;
import org.swdc.fx.resource.source.ModulePathResource;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SFXApplication(splash = FXSplash.class, mainView = MainView.class,singleton = true)
public class ArchiverApplication extends FXApplication {

    private static ObservableList<Image> icons = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected void onLaunch(ConfigManager configManager) {
        configManager.register(AppConfig.class);
    }

    @Override
    protected void onStart(ApplicationContainer container) {
        container.register(ArchiveResolverManager.class);
    }

    @Override
    public void onFileOpenRequest(File file) {
        MainView view = findExistedComponent(MainView.class, v->v.getArchiveFile() == null);
        if (view == null) {
            view = this.findComponent(MainView.class);
        }
        List<ArchiveResolver> resolvers = getScoped(ArchiveResolver.class);
        ArchiveResolver resolver = null;
        for (ArchiveResolver resolverItem: resolvers) {
            if (file.getName().endsWith(resolverItem.getExtension())) {
                resolver = resolverItem;
            }
        }
        if (resolver == null) {
            return;
        }
        MainView mainView = view;
        ArchiveResolver archiveResolver = resolver;
        CompletableFuture.supplyAsync(() -> archiveResolver.loadFile(file))
                .whenComplete((archiveFile,e) -> {
                    if(archiveFile != null) {
                        mainView.setArchiveFile(archiveFile);
                        mainView.getStage().setTitle(archiveFile.getFile().getName() + " - 压缩管理");
                        mainView.show();
                    }
                });
    }

    @Override
    protected List<Image> loadIcons() {
        if (icons == null) {
            Module module = this.getClass().getModule();
            icons = FXCollections.observableArrayList();
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon16.png").getInputStream()));
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon24.png").getInputStream()));
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon32.png").getInputStream()));
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon48.png").getInputStream()));
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon64.png").getInputStream()));
            icons.add(new Image(new ModulePathResource(module,"viewIcons/icon72.png").getInputStream()));
        }
        return icons;
    }

    @Override
    protected void appHasStarted(FXView mainView) {
        mainView.show();
    }
}
