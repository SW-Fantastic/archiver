package org.swdc.archive.ui.view.cells;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.fx.FXView;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Scope;
import org.swdc.fx.anno.ScopeType;
import org.swdc.fx.anno.View;
import org.swdc.fx.resource.icons.FontSize;
import org.swdc.fx.resource.icons.FontawsomeService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@View(stage = false)
@Scope(ScopeType.MULTI)
public class IconColumnCell extends FXView {

    private ArchiveEntry entry;

    @Aware
    private FontawsomeService fontawsomeService = null;

    private Label iconLabel;

    private static Map<String[],String> extensionIconMap = new HashMap<>();

    static {
        extensionIconMap.put(new String[]{
                "rar","zip","7z","bzip","xz","gz","tar"
        }, "file_archive_alt");

        extensionIconMap.put(new String[]{
            "jar","class","jmod"
        }, "coffee");

        extensionIconMap.put(new String[]{
            "exe", "app", "apk"
        },"terminal");

        extensionIconMap.put(new String[] {
           "dll","so","lib","dylib","ocx","a","def"
        }, "briefcase");

        extensionIconMap.put(new String[] {
            "jpg","gif","png","bmp","jpeg","webp","psd","ico","icns"
        },"picture_alt");

        extensionIconMap.put(new String[] {
           "doc", "docx", "rtf"
        }, "file_word_alt");

        extensionIconMap.put(new String[] {
           "ppt","pptx"
        },"file_powerpoint_alt");

        extensionIconMap.put(new String[]{
           "xls","xlsx","xlsb"
        }, "file_excel_alt");

        extensionIconMap.put(new String[]{
           "xml","yml","properties","conf","plist",
           "c","cpp","java","go","js","vbs","cmd","h","hpp",
           "sh","bat","iml","gitignore","fxml","xaml",
           "css","less","scss","html","htm","vue","md"
        },"file_code_alt");

        extensionIconMap.put(new String[] {
          "db","mdf","ldf"
        },"database");

        extensionIconMap.put(new String[] {
           "mp3","ogg","wma","wav","aac","w4a"
        },"file_audio_alt");

        extensionIconMap.put(new String[]{
           "mp4","mov","3gp","flv","rmvb","avi","wmv"
        }, "film");

        extensionIconMap.put(new String[] {
            "txt","inf","ini"
        },"file_text_alt");

    }

    public void setEntry(ArchiveEntry entry) {
        this.entry = entry;
        if (entry != null) {
            if (entry.isDictionary()) {
                iconLabel.setText(fontawsomeService.getFontIcon("folder"));
            } else {
                String[] extensions = entry.getFileName().split("[.]");
                if (extensions.length > 1) {
                    String[] extensionGroup = extensionIconMap
                            .keySet().stream()
                            .filter(exts -> Arrays.asList(exts).contains(extensions[extensions.length - 1]))
                            .findFirst().orElse(null);
                    if (extensionGroup == null) {
                        iconLabel.setText(fontawsomeService.getFontIcon("file"));
                    } else {
                        iconLabel.setText(fontawsomeService.getFontIcon(extensionIconMap.get(extensionGroup)));
                    }
                } else {
                    iconLabel.setText(fontawsomeService.getFontIcon("file"));
                }
            }
        }
    }

    public ArchiveEntry getEntry() {
        return entry;
    }

    @Override
    public void initialize() {
        iconLabel.setFont(fontawsomeService.getFont(FontSize.SMALL));
    }

    @Override
    protected Parent create() {
        HBox hBox = new HBox();
        iconLabel = new Label();
        HBox.setHgrow(hBox, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().add(iconLabel);
        return hBox;
    }


}
