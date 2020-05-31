package org.swdc.archive.core;

import javafx.application.Platform;
import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.archive.ui.view.dialog.PasswordView;
import org.swdc.fx.services.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ArchiveService extends Service {

    private void checkPassword(ArchiveFile file, Runnable next) {
        Platform.runLater(() -> {
            if (file.isEncrypted()) {
                String password = file.getPassword();
                PasswordView passwordView = findView(PasswordView.class);
                if (password == null || password.isBlank() || password.isEmpty()) {
                    passwordView.show();
                    String result = passwordView.getResult();
                    if (result == null || result.isEmpty() || result.isBlank()) {
                        return;
                    }
                    file.setPassword(result);
                    next.run();
                }
            }
            next.run();
        });
    }

    public void addFile(ArchiveFile file,ArchiveEntry position, File target) {
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        if (target == null){
            return;
        }
        checkPassword(file,() -> {
            CompletableFuture.runAsync(() -> resolver.addFile(file,position,target));
        });
    }

    public void addFolder(ArchiveFile file, ArchiveEntry entry, File folder) {
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        if (folder == null){
            return;
        }
        checkPassword(file, () -> {
            CompletableFuture.runAsync(() -> resolver.addFolder(file,entry,folder));
        });
    }

    public void removeFile(ArchiveFile archiveFile, ArchiveEntry target) {
        if (archiveFile == null || target == null){
            return;
        }
        Class resolverClass = archiveFile.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        checkPassword(archiveFile, () -> {
            CompletableFuture.runAsync(() -> resolver.removeFile(archiveFile,target));
        });
    }

    public void extractAll(ArchiveFile file, File target) {
        if (file == null || target == null) {
            return;
        }
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        checkPassword(file, () -> {
            CompletableFuture.runAsync(() -> resolver.extractFiles(file,target));
        });
    }

    public void extract(ArchiveFile file,ArchiveEntry item, File target) {
        if (file == null || item == null || target == null) {
            return;
        }
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        checkPassword(file, () -> {
            CompletableFuture.runAsync(() -> resolver.extractFile(file,item,target));
        });
    }

    public void rename(ArchiveFile file, ArchiveEntry target, String name) {
        if (file == null || name == null || name.isBlank() || target == null) {
            return;
        }
        for(ArchiveEntry entry:target.getChildren()) {
            if (entry.getFileName().equals(name)) {
                return;
            }
        }
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        checkPassword(file, () -> {
            CompletableFuture.runAsync(() -> resolver.rename(file,target,name));
        });
    }

    public void updateComment(ArchiveFile file,String content) {
        if (file == null || content == null || content.isBlank()) {
            return;
        }
        Class resolverClass = file.getResolver();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        resolver.saveComment(file,content);
    }

}
