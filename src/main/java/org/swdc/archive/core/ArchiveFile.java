package org.swdc.archive.core;

import org.swdc.archive.core.archive.FileArchiver;

import java.io.File;
import java.nio.charset.Charset;

public interface ArchiveFile {

    ArchiveEntry getRootEntry();

    File getFile();

    Class<? extends FileArchiver> processor();

    boolean isEncrypted();

    Charset getCharset();

}
