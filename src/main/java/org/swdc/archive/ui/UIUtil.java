package org.swdc.archive.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.swdc.archive.ui.view.MessageView;
import org.swdc.fx.AppComponent;
import org.swdc.fx.FXView;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UIUtil {

    public static MenuItem createMenuItem(String name, EventHandler<ActionEvent> handler) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(handler);
        return item;
    }

    public static String exceptionToString(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        return writer.toString();
    }

    public static void notification(String content, AppComponent scope){
        notification(content,scope,null);
    }

    public static void notification(String content, AppComponent scope, Stage owner) {
        Platform.runLater(() -> {
            MessageView msgView = scope.findComponent(MessageView.class);
            msgView.setText(content);
            Notifications.create()
                    .owner(owner)
                    .graphic(msgView.getView())
                    .position(Pos.CENTER)
                    .hideAfter(Duration.seconds(2))
                    .hideCloseButton()
                    .show();
        });
    }

}
