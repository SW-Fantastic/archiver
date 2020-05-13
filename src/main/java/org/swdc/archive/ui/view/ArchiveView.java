package org.swdc.archive.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
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

    @Override
    public void initialize() {
        Button goBack = findById("goBack");
        goBack.setFont(fontawsomeService.getFont(FontSize.MIDDLE_SMALL));
        goBack.setPadding(new Insets(4,4,4,4));
        goBack.setText(fontawsomeService.getFontIcon("level_up"));
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
