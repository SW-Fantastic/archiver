package org.swdc.archive.core.viewer;

import org.swdc.fx.container.DefaultContainer;

public class ViewerManager extends DefaultContainer<AbstractViewer> {
    @Override
    public boolean isComponentOf(Class clazz) {
        return AbstractViewer.class.isAssignableFrom(clazz);
    }
}
