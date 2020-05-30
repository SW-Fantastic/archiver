package org.swdc.archive.core.archive.formats.creators;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.swdc.archive.core.archive.formats.ZipArchiveResolver;
import org.swdc.archive.ui.view.cells.PropertyListCell;
import org.swdc.fx.FXController;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ZipCreatorController extends FXController {

    @FXML
    private ComboBox<String> cbxCompressMethod;

    @FXML
    private ComboBox<String> cbxArchiveLevel;

    @FXML
    private ListView<File> fileListView;

    @Getter
    private ObservableList<File> files = FXCollections.observableArrayList();

    @Getter
    private File saveTarget = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbxCompressMethod.getItems().addAll("DEFLATE","STORE");
        cbxCompressMethod.getSelectionModel().select(0);
        cbxArchiveLevel.getItems().addAll("FAST","FASTEST","MAXIMUM","NORMAL","ULTRA");
        cbxArchiveLevel.getSelectionModel().select("NORMAL");
        fileListView.setItems(files);
        fileListView.setCellFactory((lv) -> new PropertyListCell<>("name",File.class));
    }

    protected void addFile(ActionEvent event) {
        ZipCreatorView view = getView();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加文件");
        List<File> files = chooser.showOpenMultipleDialog(view.getStage());
        if (files != null) {
            this.files.addAll(files);
        }
    }

    protected void addFolder(ActionEvent event) {
        ZipCreatorView view = getView();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("添加文件夹");
        File folder = directoryChooser.showDialog(view.getStage());
        if (folder != null) {
            files.add(folder);
        }
    }

    protected void removeFile(ActionEvent event) {
        File file = fileListView.getSelectionModel().getSelectedItem();
        if (file!= null) {
            files.remove(file);
        }
    }

    public ZipParameters getParam() {
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.valueOf(cbxCompressMethod.getSelectionModel().getSelectedItem()));
        parameters.setCompressionLevel(CompressionLevel.valueOf(cbxArchiveLevel.getSelectionModel().getSelectedItem()));
        return parameters;
    }

    @FXML
    public void onCreate() {
        ZipCreatorView view = getView();
        ZipArchiveResolver resolver = findComponent(ZipArchiveResolver.class);
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(resolver.getFilter());
        chooser.setSelectedExtensionFilter(resolver.getFilter());
        chooser.setTitle("压缩文件保存到");
        File file = chooser.showSaveDialog(view.getStage());
        if (file != null) {
            saveTarget = file;
            view.close();
        }
    }

    @FXML
    public void onCancel() {
        ZipCreatorView view = getView();
        saveTarget = null;
        view.close();
    }

}
