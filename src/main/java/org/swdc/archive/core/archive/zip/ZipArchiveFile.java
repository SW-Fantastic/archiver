package org.swdc.archive.core.archive.zip;

import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.archive.ArchiveProcessor;

import java.io.File;

public class ZipArchiveFile implements ArchiveFile {

    private File file;

    private ArchiveEntry root= null;

    public ZipArchiveFile(File file) {
        this.file = file;
    }

    @Override
    public ArchiveEntry getRootEntry() {
        return root;
    }

    public void setRoot(ArchiveEntry root) {
        this.root = root;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public Class<? extends ArchiveProcessor> processor() {
        return ZipArchiveResolver.class;
    }
}
