package org.swdc.archive.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

@View(stage = false)
@Scope(ScopeType.MULTI)
public class StartView extends FXView {

    @Aware
    private FontawsomeService fontawsomeService = null;

    @Override
    public void initialize() {
        this.configButtonIcon("open","folder_open");
        this.configButtonIcon("create","plus");
    }

    private void configButtonIcon(String btnId, String iconName) {
        ButtonBase button = findById(btnId);
        button.setFont(fontawsomeService.getFont(FontSize.MIDDLE));
        button.setText(fontawsomeService.getFontIcon(iconName));
        button.setPadding(new Insets(4,4,4,4));
    }
}
