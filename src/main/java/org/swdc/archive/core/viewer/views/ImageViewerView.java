package org.swdc.archive.core.viewer.views;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

@Scope(ScopeType.MULTI)
@View(title = "图片",resizeable = true,background = true)
public class ImageViewerView extends FXView {

    @Override
    public void initialize() {
    }

    public void loadFormArchive(ByteBuffer data, String title) {
        Image image = new Image(new ByteArrayInputStream(data.array()));
        ImageView view = findById("imgView");
        view.setFitHeight(image.getHeight());
        view.setFitWidth(image.getWidth());
        view.setImage(image);
        getStage().setTitle(title);
    }

}
