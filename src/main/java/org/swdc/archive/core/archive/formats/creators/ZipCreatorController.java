package org.swdc.archive.core.archive.formats.creators;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.swdc.fx.FXController;

import java.net.URL;
import java.util.ResourceBundle;

public class ZipCreatorController extends FXController {

    @FXML
    private ComboBox<String> cbxArchiveLevel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbxArchiveLevel.getItems().addAll("DEFLATE","STORE");
        cbxArchiveLevel.getSelectionModel().select(0);
    }
}
