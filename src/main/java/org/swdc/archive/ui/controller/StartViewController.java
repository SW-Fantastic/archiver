package org.swdc.archive.ui.controller;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveProcessor;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXController;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StartViewController extends FXController {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

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
        File archiveFile = chooser.showOpenDialog(null);
        if (archiveFile == null) {
            return;
        }
        ArchiveProcessor processor = processors.stream()
                .filter(item -> item.getFilter().equals(chooser.getSelectedExtensionFilter()))
                .findFirst().orElse(null);
        if (processor == null) {
            return;
        }
        ArchiveFile file = processor.loadFile(archiveFile);
        MainView view = findExistedComponent(MainView.class, v->v.getArchiveFile() == null);
        if (view == null) {
            view = this.findView(MainView.class);
        }
        view.setArchiveFile(file);
        view.show();
    }
}
