package org.swdc.archive.ui.view.cells;

import javafx.scene.control.ListCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class PropertyListCell<T> extends ListCell<T> {

    private Method reader = null;

    private static Logger logger = LoggerFactory.getLogger(PropertyListCell.class);

    public PropertyListCell(String propName, Class<T> clazz) {
        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(propName,clazz,"get" + propName.substring(0,1).toUpperCase() + propName.substring(1), null);
            reader = descriptor.getReadMethod();
        } catch (Exception e){
            logger.error("fail to get reader", e);
        }

    }

    @Override
    protected void updateItem(T t, boolean b) {
        super.updateItem(t, b);
        if (t!=null){
            try {
                this.setText(reader.invoke(t).toString());
            } catch (Exception e) {
                logger.error("fail to read property",e);
            }
        } else {
            setText(null);
        }
    }
}
