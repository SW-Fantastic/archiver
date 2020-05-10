package org.swdc.archive.config;

import lombok.Getter;
import lombok.Setter;
import org.swdc.fx.anno.Properties;
import org.swdc.fx.properties.FXProperties;

@Properties(value = "config.properties", prefix = "app")
public class AppConfig extends FXProperties {

    @Getter
    @Setter
    private String theme;

}
