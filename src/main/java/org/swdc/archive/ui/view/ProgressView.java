package org.swdc.archive.ui.view;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(title = "进度", dialog = true)
public class ProgressView extends FXView {

    private Label message;
    private ProgressBar progressBar;

    @Override
    public void initialize() {
        this.message = findById("message");
        this.progressBar = findById("progress");
    }

    public void update(String message, double progress) {
        Platform.runLater(() -> {
            this.message.setText(message);
            this.progressBar.setProgress(progress);
        });
    }

    @Override
    public void show() {
        Platform.runLater(super::show);
    }

    public void finish() {
        Platform.runLater(() -> {
            this.message.setText("");
            this.progressBar.setProgress(0);
            this.close();
        });
    }

}
