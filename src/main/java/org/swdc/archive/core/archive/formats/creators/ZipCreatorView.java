package org.swdc.archive.core.archive.formats.creators;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import net.lingala.zip4j.model.ZipParameters;
import org.swdc.archive.ui.UIUtil;
import org.swdc.archive.ui.view.cells.PropertyListCell;
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

    private ObservableList<File> files = FXCollections.observableArrayList();

    @Override
    public void initialize() {
        Button selectBtn = findById("btn-folder");
        selectBtn.setPadding(new Insets(4,4,4,4));
        selectBtn.setFont(fontawsomeService.getFont(FontSize.MIDDLE_SMALL));
        selectBtn.setText(fontawsomeService.getFontIcon("folder_open"));
        ListView<File> fileList = findById("filesList");
        fileList.setCellFactory((lv) -> new PropertyListCell<>("name",File.class));
        fileList.setItems(files);
        this.createContextMenu();
    }

    private void createContextMenu() {
        ListView<File> fileList = findById("filesList");
        ZipCreatorController creatorController = getLoader().getController();
        MenuItem itemAddFile = UIUtil.createMenuItem("添加文件",creatorController::addFile);
        MenuItem itemAddFolder = UIUtil.createMenuItem("添加文件夹",creatorController::addFolder);
        MenuItem itemRemove = UIUtil.createMenuItem("删除此项",null);

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

    public void refresh() {
        ZipCreatorController creatorController = this.getLoader().getController();
        List<File> files = creatorController.getFiles();
        this.files.clear();
        this.files.addAll(files);
    }

    @Override
    public ZipParameters getCreateParameters() {
        return null;
    }

    @Override
    public List<File> getFiles() {
        ZipCreatorController creatorController = this.getLoader().getController();
        return creatorController.getFiles();
    }

    @Override
    public File getSaveTarget() {
        return null;
    }
}
