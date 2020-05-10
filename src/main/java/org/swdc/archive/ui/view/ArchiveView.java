package org.swdc.archive.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
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

}
