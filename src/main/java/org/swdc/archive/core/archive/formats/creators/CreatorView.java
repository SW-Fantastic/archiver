package org.swdc.archive.core.archive.formats.creators;

import org.swdc.fx.FXView;

import java.io.File;
import java.util.List;

public interface CreatorView<T> {

    T getCreateParameters();

    List<File> getFiles();

    File getSaveTarget();

    <T> T getView();

}
