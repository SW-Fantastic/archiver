package org.swdc.archive.core.archive.formats.creators;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.swdc.archive.core.archive.formats.ZipArchiveResolver;
import org.swdc.archive.core.archive.formats.ZipParam;
import org.swdc.archive.ui.DataUtil;
import org.swdc.archive.ui.UIUtil;
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

    @FXML
    private TextField txtSize;

    @FXML
    private Label lblSize;

    @FXML
    private CheckBox chkSplit;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private CheckBox cbxPassword;

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
        txtSize.textProperty().addListener(o -> {
            String text = txtSize.getText();
            try {
                Long size = Long.parseLong(text);
                lblSize.setText(DataUtil.getFileSize(size));
            }catch (Exception e){
                lblSize.setText("无效");
            }
        });
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

    public ZipParam getParam() {
        ZipParam param = new ZipParam();

        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.valueOf(cbxCompressMethod.getSelectionModel().getSelectedItem()));
        parameters.setCompressionLevel(CompressionLevel.valueOf(cbxArchiveLevel.getSelectionModel().getSelectedItem()));

        if (cbxPassword.isSelected()) {
            if (!txtPassword.getText().isBlank()) {
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(EncryptionMethod.AES);
                param.setPassword(txtPassword.getText().toCharArray());
            }
        }

        param.setParameters(parameters);
        if (chkSplit.isSelected()) {
            try {
                Long size = Long.parseLong(txtSize.getText());
                if (size < 65536) {
                    UIUtil.notification("分卷压缩的分卷大小不能小于65536.",this);
                } else {
                    param.setCreateSplit(true);
                    param.setSplitSize(size);
                }
            } catch (Exception e) {
                UIUtil.notification("分卷压缩的分卷大小不能小于65536.",this);
            }
        }


        return param;
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
