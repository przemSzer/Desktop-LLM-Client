package dev.local.ai.ui.about;

import dev.local.ai.ui.utils.HostServicesProvider;
import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * FXML controller for {@code /fxml/AboutDialog.fxml}.
 */
public final class AboutController {

    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);

    @FXML
    private Label nameLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private TableView<OssLibrary> libraryTable;

    @FXML
    private TableColumn<OssLibrary, OssLibrary> libraryColumn;

    @FXML
    private TableColumn<OssLibrary, String> versionColumn;

    @FXML
    private TableColumn<OssLibrary, OssLibrary> licenseColumn;

    @FXML
    private void initialize() {
        libraryColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        libraryColumn.setCellFactory(column -> new LibraryCell());
        versionColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().version()));
        licenseColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        licenseColumn.setCellFactory(column -> new LicenseCell());
    }

    void setDetails(AppInfo info, List<OssLibrary> libraries) {
        nameLabel.setText(info.name());
        versionLabel.setText("Version " + info.version());
        descriptionLabel.setText(info.description());
        libraryTable.setItems(FXCollections.observableArrayList(libraries));
    }

    private static void open(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        HostServices services = HostServicesProvider.getInstance().getHostServices();
        if (services == null) {
            logger.warn("Host services unavailable, cannot open {}", url);
            return;
        }
        services.showDocument(url);
    }

    private static final class LibraryCell extends TableCell<OssLibrary, OssLibrary> {

        private final Hyperlink name = new Hyperlink();
        private final Label copyright = new Label();
        private final VBox box = new VBox(2, name, copyright);

        private LibraryCell() {
            name.setOnAction(event -> {
                OssLibrary item = getItem();
                if (item != null) {
                    open(item.projectUrl());
                }
            });
            copyright.getStyleClass().add("about-copyright");
            copyright.setWrapText(true);
            copyright.maxWidthProperty().bind(widthProperty().subtract(12));
            box.setAlignment(Pos.CENTER_LEFT);
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(OssLibrary item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            name.setText(item.name());
            boolean hasCopyright = !item.copyright().isBlank();
            copyright.setText(item.copyright());
            copyright.setVisible(hasCopyright);
            copyright.setManaged(hasCopyright);
            setGraphic(box);
        }
    }

    private static final class LicenseCell extends TableCell<OssLibrary, OssLibrary> {

        private final Hyperlink link = new Hyperlink();
        private final Label plain = new Label();

        private LicenseCell() {
            link.setOnAction(event -> {
                OssLibrary item = getItem();
                if (item != null) {
                    open(item.licenseUrl());
                }
            });
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(OssLibrary item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            if (item.licenseUrl().isBlank()) {
                plain.setText(item.license());
                setGraphic(plain);
                return;
            }
            link.setText(item.license());
            setGraphic(link);
        }
    }
}
