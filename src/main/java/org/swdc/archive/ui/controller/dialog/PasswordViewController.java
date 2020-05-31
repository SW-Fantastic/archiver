package org.swdc.archive.ui.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import org.swdc.archive.ui.view.dialog.PasswordView;
import org.swdc.fx.FXController;

import java.net.URL;
import java.util.ResourceBundle;

public class PasswordViewController extends FXController {

    @FXML
    private PasswordField txtPassword;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public String getResult () {
        String result = txtPassword.getText();
        txtPassword.setText("");
        return result;
    }

    @FXML
    public void onOK() {
        PasswordView view = getView();
        view.close();
    }

    @FXML
    public void onCancel() {
        txtPassword.setText("");
        PasswordView view = getView();
        view.close();
    }

}
