package org.swdc.archive.ui.view.dialog;

import javafx.application.Platform;
import org.swdc.archive.ui.controller.dialog.PasswordViewController;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(dialog = true,title = "请输入密码")
public class PasswordView extends FXView {

    public String getResult() {
        PasswordViewController controller = getLoader().getController();
        return controller.getResult();
    }

}
