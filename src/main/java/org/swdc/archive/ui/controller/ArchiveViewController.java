package org.swdc.archive.ui.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.core.ArchiveService;
import org.swdc.archive.ui.events.ViewRefreshEvent;
import org.swdc.archive.ui.view.cells.IconColumnCell;
import org.swdc.archive.ui.view.cells.IconTableColumnCell;
import org.swdc.archive.ui.view.dialog.RenameView;
import org.swdc.fx.FXController;
import org.swdc.fx.anno.Aware;
import org.swdc.fx.anno.Listener;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class ArchiveViewController extends FXController {

    @FXML
    private TreeView<ArchiveEntry> archiveTree;

    @FXML
    private TableView<ArchiveEntry> contentTable;

    @FXML
    private TableColumn<ArchiveEntry, String> nameColumn;
    @FXML
    private TableColumn<ArchiveEntry, String> sizeColumn;
    @FXML
    private TableColumn<ArchiveEntry, Date> dateColumn;
    @FXML
    private TableColumn<ArchiveEntry, ArchiveEntry> iconColumn;

    @FXML
    private TextField txtPath;

    @Aware
    private ArchiveService archiveService = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        archiveTree.setShowRoot(false);
        archiveTree.setOnMouseClicked(this::onTreeItemClick);
        archiveTree.getSelectionModel().selectedItemProperty().addListener(this::onTreeItemChange);
        iconColumn.setCellFactory(col->new IconTableColumnCell(findComponent(IconColumnCell.class)));
        iconColumn.setCellValueFactory(new PropertyValueFactory<>("self"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("lastModifiedDate"));


        contentTable.setOnMouseClicked(this::onTableClick);

        // 防止用户重排header顺序
        Object monitor = new Object();
        contentTable.getColumns().addListener((ListChangeListener<TableColumn>)  change -> {
            change.next();
            synchronized (monitor) {
                if (change.wasReplaced()) {
                    contentTable.getColumns().clear();
                    contentTable.getColumns().addAll(iconColumn,nameColumn,sizeColumn,dateColumn);
                }
            }
        });
    }

    @FXML
    private void goBackLevel() {
        TreeItem<ArchiveEntry> item = archiveTree.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        if (item.getParent() == null) {
            return;
        }
        TreeItem<ArchiveEntry> parent = item.getParent();
        archiveTree.getSelectionModel().select(item.getParent());
        List<ArchiveEntry> children = parent.getValue().getChildren();
        contentTable.getItems().clear();
        contentTable.getItems().addAll(children);
        if (item.isExpanded()){
            item.setExpanded(false);
        }
    }

    private void onTableClick(MouseEvent event) {
        ArchiveEntry entry = contentTable.getSelectionModel().getSelectedItem();
        if (entry == null) {
            return;
        }
        if (event.getClickCount() >= 2 && event.getButton() == MouseButton.PRIMARY) {
            if (entry.isDictionary()) {
                TreeItem<ArchiveEntry> item = archiveTree.getSelectionModel().getSelectedItem();
                List<TreeItem<ArchiveEntry>> children = item.getChildren();
                for (TreeItem<ArchiveEntry> treeEntry: children) {
                    if (treeEntry.getValue().equals(entry)) {
                        treeEntry.setExpanded(true);
                        archiveTree.getSelectionModel().select(treeEntry);
                        contentTable.getItems().clear();
                        contentTable.getItems().addAll(treeEntry.getValue().getChildren());
                    }
                }
            }
        }
    }

    @Listener(ViewRefreshEvent.class)
    public void refreshTable(ViewRefreshEvent refreshEvent) {
        Platform.runLater(() ->{
            ArchiveEntry item = archiveTree.getSelectionModel().getSelectedItem().getValue();
            contentTable.getItems().clear();
            contentTable.getItems().addAll(item.getChildren());
        });
    }

    private void onTreeItemChange(Observable observable, TreeItem<ArchiveEntry> oldEntry, TreeItem<ArchiveEntry> newItem) {
        if (newItem == null) {
            return;
        }
        String path = newItem.getValue().getPath();
        txtPath.setText(path);
    }

    private void onTreeItemClick(MouseEvent event) {
        TreeItem<ArchiveEntry> item = archiveTree.getSelectionModel().getSelectedItem();
        if (item == null) {
            txtPath.setText("");
            return;
        }
        contentTable.getItems().clear();
        contentTable.getItems().addAll(item.getValue().getChildren());
    }

    public void refreshTree(ArchiveFile file) {
        archiveTree.setRoot(file.getRootEntry().toTreeItem(this));
        archiveTree.getSelectionModel().select(archiveTree.getRoot());
        contentTable.getItems().clear();
        contentTable.getItems().addAll(file.getRootEntry().getChildren());
    }

    public void addFile(ActionEvent event) {
        TreeItem<ArchiveEntry> item = archiveTree.getSelectionModel().getSelectedItem();
        ArchiveFile archiveFile = item.getValue().getFile();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加");
        File file = chooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        archiveService.addFile(archiveFile,item.getValue(),file);
    }

    public void deleteFile(ActionEvent event){
        ArchiveEntry item = contentTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        ArchiveFile archiveFile = item.getFile();
        archiveService.removeFile(archiveFile,item);
    }

    public void extractAll(ActionEvent event) {
        ArchiveEntry item = contentTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            item = archiveTree.getRoot().getValue();
        }
        ArchiveFile file = item.getFile();
        if (file == null) {
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(null);
        archiveService.extractAll(file,target);
    }

    public void extractFile(ActionEvent event) {
        ArchiveEntry item = contentTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("解压到");
        File target = directoryChooser.showDialog(null);
        ArchiveFile archiveFile = item.getFile();
        if (archiveFile == null) {
            return;
        }
        archiveService.extract(archiveFile,item,target);
    }

    public void addFolder(ActionEvent e) {
        ArchiveEntry item = contentTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            item = archiveTree.getRoot().getValue();
        }
        ArchiveFile file = item.getFile();
        if (file == null) {
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("添加文件夹");
        File target = directoryChooser.showDialog(null);
        ArchiveFile archiveFile = item.getFile();
        if (archiveFile == null) {
            return;
        }
        archiveService.addFolder(archiveFile,item,target);
    }

    public void onRenameItem(ActionEvent event) {
        ArchiveEntry item = contentTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            item = archiveTree.getRoot().getValue();
        }
        ArchiveFile file = item.getFile();
        if (file == null) {
            return;
        }
        RenameView renameView = findView(RenameView.class);
        renameView.show();
        String newName = renameView.getResult();
        if (newName == null) {
            return;
        }
        archiveService.rename(item.getFile(),item,newName);
    }

}
