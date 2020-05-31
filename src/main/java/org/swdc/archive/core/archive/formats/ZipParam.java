package org.swdc.archive.core.archive.formats;

import lombok.Data;
import net.lingala.zip4j.model.ZipParameters;

@Data
public class ZipParam {

    private ZipParameters parameters;

    private long splitSize = -1;

    private boolean createSplit = false;

    private char[] password;

}
