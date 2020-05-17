package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.fx.AppComponent;

public abstract class ArchiveResolver extends AppComponent implements FileArchiver {

    private FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(getName(), "*." + getExtension());

    @Override
    public FileChooser.ExtensionFilter getFilter() {
        return filter;
    }

    protected void removeEntry(ArchiveEntry parent,ArchiveEntry entry) {
        if(!parent.isDictionary()) {
            return;
        }
        if (parent.getChildren().contains(entry)) {
            parent.getChildren().remove(entry);
            if (entry.isDictionary()) {
                parent.toTreeItem(this).getChildren().remove(entry.toTreeItem(this));
            }
            return;
        }
        for (ArchiveEntry item: parent.getChildren()) {
            if (!item.isDictionary()) {
                continue;
            }
            removeEntry(item,entry);
        }
    }

    public void mountArchiveEntry(ArchiveEntry entry) {
        if (!entry.isDictionary() || entry.hasTreeNode()) {
            return;
        }
        ArchiveEntry parentItem = entry.getParent();
        if (!parentItem.hasTreeNode()) {
            mountArchiveEntry(parentItem);
        }
        if (!entry.hasTreeNode() && entry.isDictionary()) {
            parentItem.toTreeItem(this).getChildren().add(entry.toTreeItem(this));
        }
    }

}
