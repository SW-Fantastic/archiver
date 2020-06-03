package org.swdc.archive.core.viewer.views;

import javafx.scene.control.TextArea;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

@Scope(ScopeType.MULTI)
@View(background = true,title = "查看",resizeable = true)
public class TextViewerView extends FXView {

    @Override
    public void initialize() {

    }

    public void setContent(String title, String content) {
        getStage().setTitle(title);
        TextArea textArea = findById("text");
        textArea.setText(content);
    }

}
