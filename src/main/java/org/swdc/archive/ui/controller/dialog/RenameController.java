package org.swdc.archive.ui.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.swdc.archive.ui.view.dialog.RenameView;
import org.swdc.fx.FXController;

import java.net.URL;
import java.util.ResourceBundle;

public class RenameController extends FXController {

    @FXML
    private TextField txtName;

    private String result;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    public void onCancel() {
        txtName.setText("");
        RenameView view = getView();
        view.close();
    }

    @FXML
    public void onOK() {
        result = txtName.getText();
        RenameView createView = getView();
        createView.close();
    }

    public String getResult() {
        String result = this.result;
        this.result = null;
        return result;
    }

}
