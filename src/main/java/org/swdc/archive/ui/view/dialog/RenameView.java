package org.swdc.archive.ui.view.dialog;

import org.swdc.archive.ui.controller.dialog.RenameController;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(title = "创建文件夹",dialog = true)
public class RenameView extends FXView {

    public String getResult() {
        RenameController controller = getLoader().getController();
        return controller.getResult();
    }

}
