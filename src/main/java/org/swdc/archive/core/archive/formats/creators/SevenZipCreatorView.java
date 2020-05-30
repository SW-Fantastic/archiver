package org.swdc.archive.core.archive.formats.creators;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import org.swdc.archive.core.archive.formats.SevenZipSupport;
import org.swdc.archive.ui.UIUtil;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

import java.io.File;
import java.util.List;

@Scope(ScopeType.MULTI)
@View(title = "创建7z压缩文件", dialog = true,background = true)
public class SevenZipCreatorView extends FXView implements CreatorView<SevenZipSupport.SevenZipCompressLevel> {

    @Override
    public void initialize() {
        createListMenu();
    }

    private void createListMenu() {
        SevenZipCreatorController creatorController = this.getLoader().getController();
        MenuItem itemAddFile = UIUtil.createMenuItem("添加文件",creatorController::addFile);
        MenuItem itemAddFolder = UIUtil.createMenuItem("添加文件夹",creatorController::addFolder);
        MenuItem itemRemove = UIUtil.createMenuItem("删除此项",creatorController::removeFile);
        ListView<File> filesList = findById("filesList");
        SimpleBooleanProperty disabled = new SimpleBooleanProperty(true);

        filesList.getSelectionModel().selectedItemProperty().addListener((observable,oldItem, newItem) -> {
            if (filesList.getSelectionModel().getSelectedItem() == null) {
                disabled.setValue(true);
            } else {
                disabled.setValue(false);
            }
        });
        itemRemove.disableProperty().bind(disabled);
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(itemAddFile,itemAddFolder,itemRemove);
        filesList.setContextMenu(menu);
    }

    @Override
    public SevenZipSupport.SevenZipCompressLevel getCreateParameters() {
        SevenZipCreatorController creatorController = getLoader().getController();
        return creatorController.getArchiveLevel();
    }

    @Override
    public List<File> getFiles() {
        SevenZipCreatorController creatorController = getLoader().getController();
        return creatorController.getFiles();
    }

    @Override
    public File getSaveTarget() {
        SevenZipCreatorController creatorController = getLoader().getController();
        return creatorController.getSaveTarget();
    }

}
