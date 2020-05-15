package org.swdc.archive.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXController;

import java.io.File;
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

    @FXML
    public void addFile() {
        MainView view = getView();
        if (view.getArchiveFile() == null) {
            return;
        }
        ArchiveFile archiveFile = view.getArchiveFile();
        Class resolverClass = archiveFile.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加");
        File file = chooser.showOpenDialog(view.getStage());
        if (file == null) {
            return;
        }
        resolver.addFile(archiveFile,view.getSelectedEntry(),file);
    }

    @FXML
    public void extractAll() {
        MainView view = getView();
        if (view.getArchiveFile() == null){
            return;
        }
        ArchiveFile file = view.getArchiveFile();
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(view.getStage());
        resolver.extractFiles(file,target);
    }

    @FXML
    public void extractSelected() {
        MainView view = getView();
        if (view.getArchiveFile() == null){
            return;
        }
        ArchiveEntry selected = view.getSelectedEntry();
        ArchiveFile file = view.getArchiveFile();
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(view.getStage());
        if (target == null) {
            return;
        }
        resolver.extractFile(file,selected,target);
    }

}
