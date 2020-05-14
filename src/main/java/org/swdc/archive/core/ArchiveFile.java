package org.swdc.archive.core;

import org.swdc.archive.core.archive.ArchiveProcessor;

import java.io.File;
import java.nio.charset.Charset;

public interface ArchiveFile {

    ArchiveEntry getRootEntry();

    File getFile();

    Class<? extends ArchiveProcessor> processor();

    boolean isEncrypted();

    Charset getCharset();

}
