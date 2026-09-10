package de.muenchen.enovaeditor;

import de.muenchen.enovaeditor.browser.BrowserOpener;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerEntry;
import de.muenchen.enovaeditor.template.HtmlOutputWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
import de.muenchen.enovaeditor.template.XPathTemplateRenderer;
import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungChecker;
import de.muenchen.enovaeditor.xml.XmlLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MainController {

    private final BrowserOpener browserOpener =
            new BrowserOpener();
    private final XmlLoader xmlLoader =
            new XmlLoader();
    private final ErsuchenSachentscheidungChecker checker =
            new ErsuchenSachentscheidungChecker();
    private final TemplateLoader templateLoader =
            new TemplateLoader();
    private final XPathTemplateRenderer templateRenderer =
            new XPathTemplateRenderer();
    private final HtmlOutputWriter htmlOutputWriter =
            new HtmlOutputWriter();
    private final CaseworkerConfigLoader caseworkerConfigLoader =
            new CaseworkerConfigLoader();
    @FXML
    private Label fileStatusLabel;
    @FXML
    private ComboBox<CaseworkerEntry> caseworkerComboBox;

    @FXML
    private void initialize() {

        caseworkerComboBox.setConverter(
                new StringConverter<CaseworkerEntry>() {

                    @Override
                    public String toString(CaseworkerEntry caseworker) {
                        return caseworker == null
                                ? ""
                                : caseworker.name();
                    }

                    @Override
                    public CaseworkerEntry fromString(String string) {
                        return null;
                    }
                }
        );

        try {
            List<CaseworkerEntry> caseworkerEntries =
                    caseworkerConfigLoader.load();

            caseworkerComboBox.getItems().addAll(caseworkerEntries);
        } catch (IOException e) {
            showError(
                    "Sachbearbeiter-Konfiguration konnte nicht geladen werden",
                    e.getMessage()
            );
        }

    }

    @FXML
    protected void onXmlOpenClick() {

        File selectedFile = chooseXmlFile();

        if (selectedFile == null) {
            return;
        }

        try {
            var document = xmlLoader.load(selectedFile);

            checker.check(document);

            String inputTemplate =
                    templateLoader.loadInputTemplate();

            String renderedHtml =
                    templateRenderer.render(
                            inputTemplate,
                            document
                    );

            Path outputHtml =
                    htmlOutputWriter.write(
                            renderedHtml,
                            selectedFile
                    );

            fileStatusLabel.setText(
                    selectedFile.getName()
            );

            try {
                browserOpener.open(outputHtml);
            } catch (IOException e) {
                showWarning(
                        "HTML-Datei wurde erstellt",
                        "Die HTML-Datei wurde erfolgreich erstellt, "
                                + "konnte aber nicht automatisch im Browser geöffnet werden.\n\n"
                                + outputHtml
                );
            }

        } catch (Exception e) {

            fileStatusLabel.setText("");

            showError(
                    "Datei kann nicht verarbeitet werden",
                    e.getMessage()
            );
        }
    }

    private File chooseXmlFile() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "XML-Datei auswählen"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "XML-Dateien",
                        "*.xml"
                )
        );

        return fileChooser.showOpenDialog(
                fileStatusLabel
                        .getScene()
                        .getWindow()
        );
    }

    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.WARNING);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
