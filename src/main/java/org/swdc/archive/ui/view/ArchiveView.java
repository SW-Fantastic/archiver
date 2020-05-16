package org.swdc.archive.ui.view;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import lombok.Getter;
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
        SimpleBooleanProperty disabled = new SimpleBooleanProperty(true);
        archiveTable.getSelectionModel().selectedItemProperty().addListener(o -> {
            if (archiveTable.getSelectionModel().getSelectedItem() == null) {
                disabled.setValue(true);
            } else {
                disabled.setValue(false);
            }
        });

        this.tableContextMenu = new ContextMenu();
        MenuItem addFile = UIUtil.createMenuItem("添加文件", controller::addFile);
        MenuItem createFolder = UIUtil.createMenuItem("创建文件夹", null);
        MenuItem addFolder = UIUtil.createMenuItem("添加文件夹", null);
        MenuItem extractSelect = UIUtil.createMenuItem("解压此文件", controller::extractFile);
        MenuItem delete = UIUtil.createMenuItem("删除文件", controller::deleteFile);
        MenuItem extractAll = UIUtil.createMenuItem("全部解压", controller::extractAll);

        extractSelect.disableProperty().bind(disabled);
        delete.disableProperty().bind(disabled);

        this.tableContextMenu.getItems().addAll(addFile,
                createFolder,addFolder,
                new SeparatorMenuItem(),
                extractSelect,extractAll,delete);

        archiveTable.setContextMenu(this.tableContextMenu);
    }

    public void refreshTree(ArchiveFile archiveFile) {
        ArchiveViewController controller = getLoader().getController();
        controller.refreshTree(archiveFile);
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
