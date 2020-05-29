package org.swdc.archive.core.archive.formats;

public class JarArchiveResolver extends ZipArchiveResolver {

    @Override
    public String getExtension() {
        return "jar";
    }

    @Override
    public String getName() {
        return "java 压缩格式";
    }

    @Override
    public boolean creatable() {
        return true;
    }
}
