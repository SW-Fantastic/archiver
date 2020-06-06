package org.swdc.archive.ui.view.dialog;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.swdc.archive.config.AppConfig;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(title = "配置设置", background = true,dialog = true)
public class ConfigureView extends FXView {

    @Override
    public void initialize() {
        AppConfig config = findProperties(AppConfig.class);
        Node editor = config.getEditor();
        BorderPane content = findById("content");
        content.setCenter(editor);
    }
}
