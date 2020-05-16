package org.swdc.archive.core;

import org.swdc.archive.core.archive.ArchiveResolver;
import org.swdc.fx.services.Service;

import java.io.File;

public class ArchiveService extends Service {

    public void addFile(ArchiveFile file,ArchiveEntry position, File target) {
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        if (target == null){
            return;
        }
        resolver.addFile(file,position,target);
    }

    public void addFolder(ArchiveFile file, ArchiveEntry entry, File folder) {
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        if (folder == null){
            return;
        }
        resolver.addFolder(file,entry,folder);
    }

    public void removeFile(ArchiveFile archiveFile, ArchiveEntry target) {
        if (archiveFile == null || target == null){
            return;
        }
        Class resolverClass = archiveFile.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        resolver.removeFile(archiveFile,target);
    }

    public void extractAll(ArchiveFile file, File target) {
        if (file == null || target == null) {
            return;
        }
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        resolver.extractFiles(file,target);
    }

    public void extract(ArchiveFile file,ArchiveEntry item, File target) {
        if (file == null || item == null || target == null) {
            return;
        }
        Class resolverClass = file.processor();
        ArchiveResolver resolver = (ArchiveResolver) findComponent(resolverClass);
        resolver.extractFile(file,item,target);
    }

}
