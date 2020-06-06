package org.swdc.archive.ui.view.dialog;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.ui.DataUtil;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(background = true,dialog = true,title = "属性")
public class FilePropertyView extends FXView {

    private TextField txtName;
    private TextField txtPath;
    private TextField txtSize;
    private CheckBox chkSafe;

    @Override
    public void initialize() {
        this.txtName = findById("txtName");
        this.txtPath = findById("txtPath");
        this.txtSize = findById("txtSize");
        this.chkSafe = findById("chkSafe");
    }

    public void setArchive(ArchiveFile file) {
        txtName.setText(file.getFile().getName());
        txtPath.setText(file.getFile().getAbsolutePath());
        txtSize.setText(DataUtil.getFileSize(file.getFile().length()));
        chkSafe.setSelected(file.isEncrypted());
    }

}
