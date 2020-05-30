package org.swdc.archive.core.archive.formats.creators;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import org.swdc.archive.core.archive.formats.SevenZArchiveResolver;
import org.swdc.archive.core.archive.formats.SevenZipSupport;
import org.swdc.archive.core.archive.formats.ZipArchiveResolver;
import org.swdc.archive.ui.view.cells.PropertyListCell;
import org.swdc.fx.FXController;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SevenZipCreatorController extends FXController {

    @Getter
    private File saveTarget;

    @FXML
    private ComboBox<String> cbxArchiveLevel;

    @FXML
    private ListView<File> fileListView;

    @Getter
    private ObservableList<File> files = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbxArchiveLevel.getItems().addAll("NORMAL","FAST","FASTEST","MAXIMUM","STORE");
        cbxArchiveLevel.getSelectionModel().select("NORMAL");
        fileListView.setItems(files);
        fileListView.setCellFactory((lv) -> new PropertyListCell<>("name",File.class));
    }

    protected void addFile(ActionEvent event) {
        SevenZipCreatorView view = getView();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加文件");
        List<File> files = chooser.showOpenMultipleDialog(view.getStage());
        if (files != null) {
            this.files.addAll(files);
        }
    }

    protected void addFolder(ActionEvent event) {
        SevenZipCreatorView view = getView();
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

    @FXML
    public void onCancel() {
        saveTarget = null;
        SevenZipCreatorView view = getView();
        view.close();
    }

    @FXML
    public void onCreate() {
        SevenZipCreatorView view = getView();
        SevenZArchiveResolver resolver = findComponent(SevenZArchiveResolver.class);
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

    public SevenZipSupport.SevenZipCompressLevel getArchiveLevel() {
        return SevenZipSupport.SevenZipCompressLevel.valueOf(cbxArchiveLevel.getSelectionModel().getSelectedItem());
    }
}
