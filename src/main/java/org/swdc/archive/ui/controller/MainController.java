package org.swdc.archive.ui.controller;

import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.ArchiveService;
import org.swdc.archive.ui.view.MainView;
import org.swdc.fx.FXController;
import org.swdc.fx.anno.Aware;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController extends FXController {

    @Aware
    private ArchiveService archiveService = null;

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
        archiveService.removeFile(file,selected);
    }

    @FXML
    public void addFile() {
        MainView view = getView();
        if (view.getArchiveFile() == null) {
            return;
        }
        ArchiveFile archiveFile = view.getArchiveFile();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加");
        File file = chooser.showOpenDialog(view.getStage());
        if (file == null) {
            return;
        }
        archiveService.addFile(archiveFile,view.getSelectedEntry(),file);
    }

    @FXML
    public void extractAll() {
        MainView view = getView();
        if (view.getArchiveFile() == null){
            return;
        }
        ArchiveFile file = view.getArchiveFile();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(view.getStage());
        archiveService.extractAll(file,target);
    }

    @FXML
    public void extractSelected() {
        MainView view = getView();
        if (view.getArchiveFile() == null){
            return;
        }
        ArchiveEntry selected = view.getSelectedEntry();
        ArchiveFile file = view.getArchiveFile();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(view.getStage());
        if (target == null) {
            return;
        }
        archiveService.extract(file,selected,target);
    }

}
