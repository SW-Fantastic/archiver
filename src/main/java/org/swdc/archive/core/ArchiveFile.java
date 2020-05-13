package org.swdc.archive.core;

import org.swdc.archive.core.archive.ArchiveProcessor;

import java.io.File;

public interface ArchiveFile {

    ArchiveEntry getRootEntry();

    File getFile();

    Class<? extends ArchiveProcessor> processor();

    boolean isEncrypted();

}
