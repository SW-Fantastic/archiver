package org.swdc.archive.ui.controller.dialog;

import javafx.fxml.FXML;
import org.swdc.archive.config.AppConfig;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.view.dialog.ConfigureView;
import org.swdc.fx.FXController;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigureController extends FXController {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    public void save() {
        ConfigureView view = getView();
        try {
            AppConfig config = findProperties(AppConfig.class);
            config.saveProperties();
            view.getStage().close();
        }catch (Exception e) {
            UIUtil.notification("保存设置失败",this);
        }
    }

    @FXML
    public void cancel() {
        ConfigureView view = getView();
        view.getStage().close();
    }

}
