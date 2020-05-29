package org.swdc.archive.core.archive.formats.creators;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import net.lingala.zip4j.model.ZipParameters;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

import java.io.File;
import java.util.List;

@Scope(ScopeType.MULTI)
@View(title = "创建ZIP文件",dialog = true,background = true)
public class ZipCreatorView extends FXView implements CreatorView<ZipParameters> {

    @Aware
    private FontawsomeService fontawsomeService = null;

    @Override
    public void initialize() {
        Button selectBtn = findById("btn-folder");
        selectBtn.setPadding(new Insets(4,4,4,4));
        selectBtn.setFont(fontawsomeService.getFont(FontSize.MIDDLE_SMALL));
        selectBtn.setText(fontawsomeService.getFontIcon("folder_open"));
    }

    @Override
    public ZipParameters getCreateParameters() {
        return null;
    }

    @Override
    public List<File> getFiles() {
        return null;
    }

    @Override
    public File getSaveTarget() {
        return null;
    }
}
