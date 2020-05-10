package org.swdc.archive.ui.view.cells;

import javafx.scene.control.TableCell;
import org.swdc.archive.core.ArchiveEntry;

public class IconTableColumnCell extends TableCell<ArchiveEntry, ArchiveEntry> {

    private IconColumnCell columnCell;

    public IconTableColumnCell(IconColumnCell columnCell) {
        this.columnCell = columnCell;
    }

    @Override
    protected void updateItem(ArchiveEntry s, boolean b) {
        super.updateItem(s, b);
        if (getItem() == null) {
            setGraphic(null);
            return;
        }
        columnCell.setEntry(getItem());
        setGraphic(columnCell.getView());
    }
}
