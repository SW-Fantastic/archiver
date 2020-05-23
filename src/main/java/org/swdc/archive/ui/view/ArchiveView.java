package org.swdc.archive.ui.view;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.controller.ArchiveViewController;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

@View(stage = false)
@Scope(ScopeType.MULTI)
public class ArchiveView extends FXView {

    @Aware
    private FontawsomeService fontawsomeService = null;

    private ContextMenu tableContextMenu = null;

    private SimpleBooleanProperty writeable = new SimpleBooleanProperty(false);

    @Override
    public void initialize() {
        Button goBack = findById("goBack");
        goBack.setFont(fontawsomeService.getFont(FontSize.MIDDLE_SMALL));
        goBack.setPadding(new Insets(4,4,4,4));
        goBack.setText(fontawsomeService.getFontIcon("level_up"));
        this.createContextMenu();
    }

    private void createContextMenu() {
        ArchiveViewController controller = this.getLoader().getController();

        TableView<ArchiveEntry> archiveTable = findById("archiveTable");
        SimpleBooleanProperty menuSelectionWriteDisable = new SimpleBooleanProperty(true);
        SimpleBooleanProperty menuSelectionReadDisable = new SimpleBooleanProperty(true);


        archiveTable.getSelectionModel().selectedItemProperty().addListener(o -> {

            if (archiveTable.getSelectionModel().getSelectedItem() == null) {
                menuSelectionWriteDisable.setValue(true);
                menuSelectionReadDisable.setValue(true);
            } else {
                ArchiveFile file = archiveTable.getSelectionModel().getSelectedItem().getFile();
                if (file.isWriteable()) {
                    menuSelectionWriteDisable.setValue(false);
                }
                menuSelectionReadDisable.setValue(false);
            }
        });

        this.tableContextMenu = new ContextMenu();
        MenuItem addFile = UIUtil.createMenuItem("添加文件", controller::addFile);
        MenuItem addFolder = UIUtil.createMenuItem("添加文件夹", controller::addFolder);
        MenuItem extractSelect = UIUtil.createMenuItem("解压此文件", controller::extractFile);
        MenuItem delete = UIUtil.createMenuItem("删除文件", controller::deleteFile);
        MenuItem extractAll = UIUtil.createMenuItem("全部解压", controller::extractAll);
        MenuItem rename = UIUtil.createMenuItem("重命名", controller::onRenameItem);

        extractSelect.disableProperty().bind(menuSelectionReadDisable);
        delete.disableProperty().bind(menuSelectionWriteDisable);
        rename.disableProperty().bind(menuSelectionWriteDisable);

        addFile.disableProperty().bind(this.writeable.not());
        addFolder.disableProperty().bind(this.writeable.not());

        this.tableContextMenu.getItems().addAll(addFile, addFolder,
                new SeparatorMenuItem(),
                rename, extractSelect,extractAll,delete);

        archiveTable.setContextMenu(this.tableContextMenu);
    }

    public void refreshTree(ArchiveFile archiveFile) {
        ArchiveViewController controller = getLoader().getController();
        controller.refreshTree(archiveFile);
        writeable.setValue(archiveFile.isWriteable());
    }

    public ArchiveEntry getSelectedEntry() {
        TableView<ArchiveEntry> view = this.findById("archiveTable");
        ArchiveEntry entry = view.getSelectionModel().getSelectedItem();
        if (entry == null) {
            TreeView<ArchiveEntry> archiveTree = this.findById("archiveTree");
            entry = archiveTree.getSelectionModel().getSelectedItem().getValue();
        }
        return entry;
    }

}
