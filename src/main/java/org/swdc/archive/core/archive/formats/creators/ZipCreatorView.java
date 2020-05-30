package org.swdc.archive.core.archive.formats.creators;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import net.lingala.zip4j.model.ZipParameters;
import org.swdc.archive.ui.UIUtil;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;


import java.io.File;
import java.util.List;

@Scope(ScopeType.MULTI)
@View(title = "创建ZIP文件",dialog = true,background = true)
public class ZipCreatorView extends FXView implements CreatorView<ZipParameters> {

    @Override
    public void initialize() {
        this.createContextMenu();
    }

    private void createContextMenu() {
        ListView<File> fileList = findById("filesList");
        ZipCreatorController creatorController = getLoader().getController();
        MenuItem itemAddFile = UIUtil.createMenuItem("添加文件",creatorController::addFile);
        MenuItem itemAddFolder = UIUtil.createMenuItem("添加文件夹",creatorController::addFolder);
        MenuItem itemRemove = UIUtil.createMenuItem("删除此项",creatorController::removeFile);

        SimpleBooleanProperty disabled = new SimpleBooleanProperty(true);

        fileList.getSelectionModel().selectedItemProperty().addListener((observable,oldItem, newItem) -> {
            if (fileList.getSelectionModel().getSelectedItem() == null) {
                disabled.setValue(true);
            } else {
                disabled.setValue(false);
            }
        });

        itemRemove.disableProperty().bind(disabled);

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(itemAddFile,itemAddFolder,itemRemove);
        fileList.setContextMenu(menu);

    }

    @Override
    public ZipParameters getCreateParameters() {
        ZipCreatorController creatorController = this.getLoader().getController();
        return creatorController.getParam();
    }

    @Override
    public List<File> getFiles() {
        ZipCreatorController creatorController = this.getLoader().getController();
        return creatorController.getFiles();
    }

    @Override
    public File getSaveTarget() {
        ZipCreatorController creatorController = this.getLoader().getController();
        return creatorController.getSaveTarget();
    }
}
