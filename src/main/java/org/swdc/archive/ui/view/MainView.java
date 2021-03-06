package org.swdc.archive.ui.view;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.ui.view.dialog.ConfigureView;
import org.swdc.archive.ui.view.dialog.FilePropertyView;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

@Scope(ScopeType.CACHE)
@View(background = true,title = "压缩管理",resizeable = true)
public class MainView extends FXView {

    @Aware
    private FontawsomeService fontawsomeService = null;

    @Aware
    private StartView startView = null;

    @Aware
    private ArchiveView archiveView = null;

    @Aware
    private ConfigureView configureView = null;

    @Aware
    private FilePropertyView propertyView = null;

    private SimpleDoubleProperty contentWidthProp = new SimpleDoubleProperty();

    private SimpleDoubleProperty contentHeightProp = new SimpleDoubleProperty();

    private SimpleBooleanProperty writeable = new SimpleBooleanProperty();

    private SimpleBooleanProperty readable = new SimpleBooleanProperty();

    private ArchiveFile archiveFile;

    @Override
    public void initialize() {
        this.configButtonIcon("addFile", "plus","向当前位置添加文件",writeable);
        this.configButtonIcon("removeFile","trash", "移除此处的文件",writeable);
        this.configButtonIcon("btnProp","list_alt","文件属性",readable);
        this.configButtonIcon("btnConf","gear","设置",null);
        MenuButton unArchive = findById("unarchivedp");
        unArchive.setPadding(new Insets(4,4,4,4));
        unArchive.setFont(fontawsomeService.getFont(FontSize.MIDDLE));
        unArchive.setText(fontawsomeService.getFontIcon("tasks"));
        unArchive.setTooltip(new Tooltip("全部解压"));
        unArchive.disableProperty().bind(readable.not());

        Stage stage = getStage();
        stage.setMinWidth(800);
        stage.setMinHeight(520);

        BorderPane content = findById("contentView");
        BorderPane start = startView.getView();

        contentWidthProp.bind(stage.widthProperty().subtract(12));
        contentHeightProp.bind(content.heightProperty().subtract(10));

        start.prefWidthProperty().bind(contentWidthProp);
        start.prefHeightProperty().bind(contentHeightProp);

        BorderPane archContent = archiveView.getView();
        archContent.prefHeightProperty().bind(contentHeightProp);
        archContent.prefWidthProperty().bind(contentWidthProp);
        toStartView();
    }

    private void toStartView(){
        writeable.setValue(false);
        readable.setValue(false);
        BorderPane content = findById("contentView");
        content.setCenter(startView.getView());
    }

    private void configButtonIcon(String btnId, String iconName, String tooltip, SimpleBooleanProperty enable) {
        Button button = findById(btnId);
        button.setFont(fontawsomeService.getFont(FontSize.MIDDLE));
        button.setText(fontawsomeService.getFontIcon(iconName));
        button.setPadding(new Insets(4,4,4,4));
        button.setTooltip(new Tooltip(tooltip));
        if (enable!=null){
            button.disableProperty().bind(enable.not());
        }
    }

    public synchronized void setArchiveFile(ArchiveFile archiveFile) {
        BorderPane content = findById("contentView");
        content.setCenter(archiveView.getView());
        this.archiveFile = archiveFile;
        this.archiveView.refreshTree(archiveFile);
        readable.setValue(true);
        if (archiveFile.isWriteable()) {
            writeable.setValue(true);
        }
        propertyView.setArchive(archiveFile);
    }

    public void showProperty() {
        this.propertyView.show();
    }

    public void showConfigure() {
        this.configureView.show();
    }

    public synchronized ArchiveFile getArchiveFile() {
        return this.archiveFile;
    }

    public ArchiveEntry getSelectedEntry() {
        return this.archiveView.getSelectedEntry();
    }

}
