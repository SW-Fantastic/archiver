package org.swdc.archive.core.archive;

import org.swdc.fx.container.DefaultContainer;

public class ArchiveResolverManager extends DefaultContainer<ArchiveResolver> {

    @Override
    public boolean isComponentOf(Class aClass) {
        return ArchiveResolver.class.isAssignableFrom(aClass);
    }

}
