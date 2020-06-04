package org.swdc.archive.core.viewer;

import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.core.viewer.views.ImageViewerView;
import org.swdc.fx.FXView;

import java.nio.ByteBuffer;

public class ImageViewer extends AbstractViewer {

    @Override
    public Class getViewerView() {
        return ImageViewerView.class;
    }

    @Override
    public boolean support(String mimeType) {
        String mime = mimeType.toLowerCase();
        if (mime.equals("png") || mime.endsWith("jpeg")||mime.endsWith("gif")) {
            return true;
        }
        return false;
    }

    @Override
    public FXView loadFromArchive(ArchiveFile file, ArchiveEntry entry) {
        ImageViewerView viewerView = findView(ImageViewerView.class);
        ArchiveResolver resolver = (ArchiveResolver) findComponent(file.getResolver());
        ByteBuffer data = resolver.getContent(file,entry);
        viewerView.loadFormArchive(data,entry.getFileName());
        return viewerView;
    }

}
