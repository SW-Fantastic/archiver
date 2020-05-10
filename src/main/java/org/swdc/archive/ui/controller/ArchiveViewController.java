package org.swdc.archive.ui.controller;

import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveFile;
import org.swdc.archive.ui.view.ArchiveView;
import org.swdc.archive.ui.view.cells.IconColumnCell;
import org.swdc.archive.ui.view.cells.IconTableColumnCell;
import org.swdc.fx.FXController;

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
    }

}
