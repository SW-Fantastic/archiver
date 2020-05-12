package org.swdc.archive.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXController;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController extends FXController {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    public void removeFile() {
        MainView view = getView();
        ArchiveEntry selected = view.getSelectedEntry();
        ArchiveFile file = view.getArchiveFile();
        if (file == null || selected == null) {
            return;
        }
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        if (resolver.removeFile(file,selected)){
            if (selected.isDictionary()) {
                TreeItem<ArchiveEntry> parentItem = selected.getParent().toTreeItem(this);
                parentItem.getChildren().remove(selected.toTreeItem(this));
                parentItem.getValue().getChildren().remove(selected);
            }

        }
    }

}
