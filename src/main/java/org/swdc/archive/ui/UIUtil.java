package org.swdc.archive.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

public class UIUtil {

    public static MenuItem createMenuItem(String name, EventHandler<ActionEvent> handler) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(handler);
        return item;
    }

}
