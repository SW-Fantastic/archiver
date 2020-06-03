package org.swdc.archive.core.viewer;

import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.core.viewer.views.TextViewerView;
import org.swdc.archive.ui.DataUtil;
import org.swdc.fx.FXView;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class TextViewer extends AbstractViewer {
    @Override
    public Class getViewerView() {
        return TextViewerView.class;
    }

    @Override
    public boolean support(String mimeType) {
        return (mimeType.toLowerCase().startsWith("text") || mimeType.endsWith("xml"));
    }

    @Override
    public FXView loadFromArchive(ArchiveFile file, ArchiveEntry entry) {
        ArchiveResolver resolver = (ArchiveResolver) findComponent(file.getResolver());
        ByteBuffer data = resolver.getContent(file,entry);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.array());
        Charset charset = DataUtil.getCharset(inputStream);
        String text = new String(data.array(),charset);
        TextViewerView viewerView = findView(TextViewerView.class);
        viewerView.setContent(entry.getFileName(),text);
        return viewerView;
    }
}
