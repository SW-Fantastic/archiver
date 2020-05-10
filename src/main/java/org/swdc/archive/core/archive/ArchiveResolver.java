package org.swdc.archive.core.archive;

import javafx.stage.FileChooser;
import org.swdc.fx.AppComponent;

public abstract class ArchiveResolver extends AppComponent implements ArchiveProcessor {

    private FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(getName(), "*." + getExtension());

    @Override
    public FileChooser.ExtensionFilter getFilter() {
        return filter;
    }

}
