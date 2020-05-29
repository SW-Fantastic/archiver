package org.swdc.archive.core.archive.formats.creators;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import org.swdc.fx.FXController;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ZipCreatorController extends FXController {

    @FXML
    private ComboBox<String> cbxArchiveLevel;

    @Getter
    private List<File> files = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbxArchiveLevel.getItems().addAll("DEFLATE","STORE");
        cbxArchiveLevel.getSelectionModel().select(0);
    }

    protected void addFile(ActionEvent event) {
        ZipCreatorView view = getView();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加文件");
        List<File> files = chooser.showOpenMultipleDialog(view.getStage());
        if (files != null) {
            this.files.addAll(files);
            view.refresh();
        }
    }

    protected void addFolder(ActionEvent event) {
        ZipCreatorView view = getView();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("添加文件夹");
        File folder = directoryChooser.showDialog(view.getStage());
        if (folder != null) {
            files.add(folder);
            view.refresh();
        }
    }

}
