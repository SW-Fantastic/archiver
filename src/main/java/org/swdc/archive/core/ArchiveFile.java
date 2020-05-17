package org.swdc.archive.core;

import lombok.Getter;
import lombok.Setter;
import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.nio.charset.Charset;

public class ArchiveFile  {

    private File file;

    private ArchiveEntry root= null;

    private Charset charset = Charset.defaultCharset();

    @Getter
    @Setter
    private Class resolver = null;

    public ArchiveFile(File file) {
        this.file = file;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public ArchiveEntry getRootEntry() {
        return root;
    }

    public void setRoot(ArchiveEntry root) {
        this.root = root;
    }

    public Charset getCharset() {
        return charset;
    }

    public File getFile() {
        return file;
    }

    public boolean isEncrypted() {
        try {
            ZipFile file = new ZipFile(getFile());
            return file.isEncrypted();
        } catch (Exception e){
            return false;
        }
    }
}
