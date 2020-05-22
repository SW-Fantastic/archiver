package org.swdc.archive.core.archive.formats;

public class WarArchiveResolver extends ZipArchiveResolver {

    @Override
    public String getName() {
        return "Java Web应用发布包";
    }

    @Override
    public String getExtension() {
        return "war";
    }
}
