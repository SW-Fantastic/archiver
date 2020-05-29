module fxArchive {
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires slf4j.api;
    requires org.controlsfx.controls;

    requires lombok;
    requires fx.framework.core;
    requires fx.framework.resource;

    requires org.apache.commons.compress;
    requires zip4j;
    requires org.tukaani.xz;
    requires sevenzipjbinding;
    requires sevenzipjbinding.all.platforms;
    requires cpdetector;

    opens org.swdc.archive.ui.view to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.core.archive.formats.creators to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.ui.controller to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.ui.view.dialog to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.ui.controller.dialog to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.ui.view.cells to
            javafx.fxml,
            fx.framework.core;

    opens org.swdc.archive.core.archive to
            fx.framework.core;

    opens org.swdc.archive.core to
            fx.framework.core,
            javafx.base;

    opens org.swdc.archive.core.archive.formats to
            fx.framework.core;

    opens org.swdc.archive.config to
            fx.framework.core;

    opens views to
            fx.framework.core,
            fx.framework.resource;

    opens viewIcons to
            fx.framework.core,
            fx.framework.resource;

    opens org.swdc.archive to
            javafx.graphics;
}