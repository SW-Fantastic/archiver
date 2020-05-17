package org.swdc.archive.core;

import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;
import org.swdc.fx.AppComponent;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArchiveEntry {

    @Getter
    @Setter
    private String fileName;

    @Getter
    @Setter
    private ArchiveEntry parent;

    @Getter
    @Setter
    private boolean dictionary;

    @Getter
    @Setter
    private Date lastModifiedDate;

    @Getter
    @Setter
    private Long size;

    @Getter
    @Setter
    private List<ArchiveEntry> children = new ArrayList<>();

    @Getter
    @Setter
    private ArchiveFile file;

    @Setter
    private Object externalData;

    public <T> T getExternalData() {
        return (T)externalData;
    }

    private TreeItem<ArchiveEntry> treeItem;

    public String getFileSize() {
        if (getSize() == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (this.getSize() < 1024) {
            fileSizeString = df.format((double) getSize()) + "B";
        } else if (this.getSize() < 1048576) {
            fileSizeString = df.format((double) this.getSize() / 1024) + "K";
        } else if (this.getSize() < 1073741824) {
            fileSizeString = df.format((double) this.getSize() / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) getSize() / 1073741824) + "G";
        }
        return fileSizeString;
    }

    /**
     * 提供TableView使用。
     * @return
     */
    public ArchiveEntry getSelf() {
        return this;
    }

    public TreeItem<ArchiveEntry> toTreeItem(AppComponent component) {
        if (this.treeItem == null ) {
            this.treeItem = new TreeItem<>();
            Label icon = new Label();
            FontawsomeService fontawsomeService = component.findComponent(FontawsomeService.class);
            icon.setFont(fontawsomeService.getFont(FontSize.SMALL));
            icon.setText(fontawsomeService.getFontIcon("folder"));
            treeItem.setValue(this);
            treeItem.setExpanded(false);
            treeItem.setGraphic(icon);
            treeItem.expandedProperty().addListener((observableValue, oldState, newState) -> {
                if (newState != null && newState) {
                    icon.setText(fontawsomeService.getFontIcon("folder_open"));
                } else {
                    icon.setText(fontawsomeService.getFontIcon("folder"));
                }
            });
            if (this.isDictionary()) {
                for (ArchiveEntry entry : this.getChildren()) {
                    if (entry.isDictionary()) {
                        treeItem.getChildren().add(entry.toTreeItem(component));
                    }
                }
            }
        }
        return treeItem;
    }

    public String getPath() {
        StringBuilder stringBuilder = new StringBuilder();
        ArchiveEntry parent = this;
        while (parent != null) {
            stringBuilder.insert(0,parent.getFileName());
            if (parent.getParent() != null && !parent.getParent().equals(file.getRootEntry())) {
                stringBuilder.insert(0, '/');
            }
            parent = parent.getParent();
        }
        if (this.isDictionary()) {
            stringBuilder.append('/');
        }
        return stringBuilder.toString();
    }

    public boolean hasTreeNode() {
        return this.treeItem != null;
    }

    @Override
    public String toString() {
        return getFileName();
    }
}
