package org.swdc.archive.ui.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveService;
import org.swdc.archive.core.archive.FileArchiver;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.core.archive.formats.creators.CreatorView;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXController;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StartViewController extends FXController {

    @FXML
    private MenuButton createButton;

    private ObservableList<MenuItem> creatable = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Bindings.bindContentBidirectional(createButton.getItems(),creatable);
    }

    @Override
    public void initialize() {
        List<ArchiveResolver> resolvers = getScoped(ArchiveResolver.class);
        List<MenuItem> menuItems = resolvers.stream()
                .filter(FileArchiver::creatable)
                .map(item ->
                    UIUtil.createMenuItem("创建" + item.getName(), e->{
                        if (item.getCreator() == null) {
                            return;
                        }
                        FXView creator = findView(item.getCreator());
                        creator.show();
                        item.createArchive((CreatorView) creator);
                    }))
                .collect(Collectors.toList());
        creatable.clear();
        creatable.addAll(menuItems);
    }

    @FXML
    public void openArchiveFile() {
        List<ArchiveResolver> processors = getScoped(ArchiveResolver.class);
        List<FileChooser.ExtensionFilter> filters = processors.stream()
                .map(ArchiveResolver::getFilter)
                .collect(Collectors.toList());
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开");
        chooser.getExtensionFilters().addAll(filters);

        List<String> extensions = filters.stream()
                .flatMap(item -> item.getExtensions().stream())
                .collect(Collectors.toList());

        FileChooser.ExtensionFilter allSupported = new FileChooser.ExtensionFilter("所有支持的格式", extensions.toArray(String[]::new));
        chooser.getExtensionFilters().add(allSupported);
        chooser.setSelectedExtensionFilter(allSupported);

        File archiveFile = chooser.showOpenDialog(null);
        if (archiveFile == null) {
            return;
        }
        FileArchiver processor = processors.stream()
                .filter(item -> archiveFile.getName().endsWith(item.getExtension()))
                .findFirst().orElse(null);
        if (processor == null) {
            return;
        }
        CompletableFuture.supplyAsync(() -> processor.loadFile(archiveFile))
                .whenComplete((archive, e) -> {
                    Platform.runLater(() -> {
                        if (archive == null || e != null) {
                            return;
                        }
                        MainView view = findExistedComponent(MainView.class, v->v.getArchiveFile() == null);
                        if (view == null) {
                            view = this.findView(MainView.class);
                        }
                        view.setArchiveFile(archive);
                        view.getStage().setTitle(archive.getFile().getName() + " - 压缩管理");
                        view.show();
                    });
                });
    }
}
