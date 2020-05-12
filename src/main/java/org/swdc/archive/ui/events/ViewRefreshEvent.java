package org.swdc.archive.ui.events;

import org.swdc.archive.core.ArchiveEntry;
import org.swdc.fx.AppComponent;
import org.swdc.fx.event.AppEvent;

public class ViewRefreshEvent extends AppEvent<ArchiveEntry> {

    public ViewRefreshEvent(ArchiveEntry data, AppComponent source) {
        super(data, source);
    }
}
