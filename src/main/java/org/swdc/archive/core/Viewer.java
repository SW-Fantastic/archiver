package org.swdc.archive.core;

import org.swdc.fx.FXView;

public interface Viewer {

    Class getViewerView();

    boolean support(String mimeType);

    FXView loadFromArchive(ArchiveFile file, ArchiveEntry entry);

}
