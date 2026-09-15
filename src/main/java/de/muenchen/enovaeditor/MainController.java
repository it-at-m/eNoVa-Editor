package de.muenchen.enovaeditor;

import de.muenchen.enovaeditor.browser.BrowserOpener;
import de.muenchen.enovaeditor.codelist.CodelistConfig;
import de.muenchen.enovaeditor.codelist.CodelistDefinition;
import de.muenchen.enovaeditor.codelist.CodelistEntry;
import de.muenchen.enovaeditor.codelist.GenericodeReader;
import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.config.SenderConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerEntry;
import de.muenchen.enovaeditor.decision.EnovaResponseTransformer;
import de.muenchen.enovaeditor.template.HtmlOutputWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
import de.muenchen.enovaeditor.template.XPathTemplateRenderer;
import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungChecker;
import de.muenchen.enovaeditor.xml.XmlLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {

    private static final double DECISION_TEXT_WRAP_WIDTH = 250;
    private static final double FILE_NUMBER_MIN_WIDTH = 120;
    private static final double FILE_NUMBER_PADDING = 20;
    private static final double CASEWORKER_WIDTH_PADDING = 30;

    private final BrowserOpener browserOpener = new BrowserOpener();
    private final XmlLoader xmlLoader = new XmlLoader();
    private final ErsuchenSachentscheidungChecker checker = new ErsuchenSachentscheidungChecker();
    private final TemplateLoader templateLoader = new TemplateLoader();
    private final XPathTemplateRenderer templateRenderer = new XPathTemplateRenderer();
    private final HtmlOutputWriter htmlOutputWriter = new HtmlOutputWriter();
    private final CaseworkerConfigLoader caseworkerConfigLoader = new CaseworkerConfigLoader();
    private final SenderConfigLoader senderConfigLoader = new SenderConfigLoader();
    private final EnovaResponseTransformer responseTransformer = new EnovaResponseTransformer();

    private Document openedDocument;
    private File currentOpenedFile;
    private String senderName;

    @FXML
    private Label fileStatusLabel;

    @FXML
    private Label senderNameLabel;

    @FXML
    private ComboBox<CaseworkerEntry> caseworkerComboBox;
 
    @FXML
    private Label caseworkerDetailLabel;

    @FXML
    private ComboBox<CodelistEntry> decisionComboBox;

    @FXML
    private TextField fileNumber;

    @FXML
    private Button previewButton;

    @FXML
    private Button exportButton;

    @FXML
    private Label actionStatusLabel;

    @FXML
    private void initialize() {
        configureCaseworkerComboBox();
        configureDecisionComboBox();
        configureFileNumberField();

        setDecisionFieldsDisabled(true);

        loadSenderName();
        loadCaseworkers();
        loadDecisions();
    }

    @FXML
    protected void onXmlOpenClick() {
        File selectedFile = chooseXmlFile();
        if (selectedFile != null) {
            loadXmlFile(selectedFile);
        }
    }

    @FXML
    protected void onLoadSampleClick() {
        Path samplePath = ApplicationPaths.getApplicationDirectory()
                .resolve("samples")
                .resolve("xjustiz_beispiel_2900003.xml");

        if (Files.exists(samplePath)) {
            loadXmlFile(samplePath.toFile());
        } else {
            showError("Musterdatei nicht gefunden", "Der Pfad " + samplePath + " existiert nicht.");
        }
    }

    public void loadXmlFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        openedDocument = null;
        currentOpenedFile = null;

        try {
            Document document = xmlLoader.load(file);
            checker.check(document);

            openedDocument = document;
            currentOpenedFile = file;

            String inputTemplate = templateLoader.loadInputTemplate();
            String renderedHtml = templateRenderer.render(inputTemplate, document);
            Path outputHtml = htmlOutputWriter.write(renderedHtml, file);

            fileStatusLabel.setText(file.getName());
            clearDecisionFields();
            setDecisionFieldsDisabled(false);

            if (actionStatusLabel != null) {
                actionStatusLabel.setText("");
            }

            try {
                browserOpener.open(outputHtml);
            } catch (IOException e) {
                showWarning(
                        "HTML-Datei wurde erstellt",
                        "Die HTML-Datei wurde erfolgreich erstellt, konnte aber nicht automatisch im Browser geöffnet werden.\n\n"
                                + outputHtml
                );
            }

        } catch (Exception e) {
            openedDocument = null;
            currentOpenedFile = null;
            fileStatusLabel.setText("");
            clearDecisionFields();
            setDecisionFieldsDisabled(true);
            showError("Datei kann nicht verarbeitet werden", e.getMessage());
        }
    }

    @FXML
    protected void onPreviewResponseClick() {
        if (openedDocument == null) {
            showError("Kein Dokument geöffnet", "Bitte öffnen Sie zuerst ein Ersuchen um Sachentscheidung.");
            return;
        }

        CodelistEntry selectedDecision = decisionComboBox.getValue();
        if (selectedDecision == null) {
            showError("Keine Entscheidung ausgewählt", "Bitte wählen Sie eine Sachentscheidung aus.");
            return;
        }

        try {
            Document responseDoc = responseTransformer.transform(
                    openedDocument,
                    selectedDecision.code(),
                    fileNumber.getText(),
                    senderName,
                    caseworkerComboBox.getValue()
            );

            String outputTemplate = templateLoader.loadOutputTemplate();
            String renderedHtml = templateRenderer.render(outputTemplate, responseDoc);

            Path tempOutput = htmlOutputWriter.write(renderedHtml, new File(currentOpenedFile.getParent(), "sachentscheidung_vorschau.xml"));
            browserOpener.open(tempOutput);

            if (actionStatusLabel != null) {
                actionStatusLabel.setText("Antwort-Vorschau geöffnet.");
            }

        } catch (Exception e) {
            showError("Fehler bei der Vorschau-Erstellung", e.getMessage());
        }
    }

    @FXML
    protected void onExportResponseClick() {
        if (openedDocument == null) {
            showError("Kein Dokument geöffnet", "Bitte öffnen Sie zuerst ein Ersuchen um Sachentscheidung.");
            return;
        }

        CodelistEntry selectedDecision = decisionComboBox.getValue();
        if (selectedDecision == null) {
            showError("Keine Entscheidung ausgewählt", "Bitte wählen Sie eine Sachentscheidung aus.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sachentscheidung (XJustiz 2900003) speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));
        fileChooser.setInitialFileName("xjustiz_nachricht_sachentscheidung.xml");

        File targetXmlFile = fileChooser.showSaveDialog(fileStatusLabel.getScene().getWindow());
        if (targetXmlFile == null) {
            return;
        }

        try {
            Document responseDoc = responseTransformer.transform(
                    openedDocument,
                    selectedDecision.code(),
                    fileNumber.getText(),
                    senderName,
                    caseworkerComboBox.getValue()
            );

            // 1. Save transformed XML
            String xmlContent = responseTransformer.documentToXmlString(responseDoc);
            Files.writeString(targetXmlFile.toPath(), xmlContent, StandardCharsets.UTF_8);

            // 2. Render Output.htm and save as HTML
            String outputTemplate = templateLoader.loadOutputTemplate();
            String renderedHtml = templateRenderer.render(outputTemplate, responseDoc);

            String baseName = targetXmlFile.getName().replaceFirst("\\.xml$", "");
            File targetHtmlFile = new File(targetXmlFile.getParentFile(), baseName + "_bescheid.html");
            Files.writeString(targetHtmlFile.toPath(), renderedHtml, StandardCharsets.UTF_8);

            if (actionStatusLabel != null) {
                actionStatusLabel.setText("Erfolgreich exportiert: " + targetXmlFile.getName());
            }

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Export erfolgreich");
            info.setHeaderText("Sachentscheidung erstellt");
            info.setContentText(
                    "Folgende Dateien wurden erstellt:\n\n"
                            + "• XJustiz 2900003 XML: " + targetXmlFile.getName() + "\n"
                            + "• HTML-Bescheid (druckbar / PDF-Export): " + targetHtmlFile.getName()
            );
            info.showAndWait();

        } catch (Exception e) {
            showError("Fehler beim Exportieren", e.getMessage());
        }
    }

    private File chooseXmlFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("XML-Datei auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));
        return fileChooser.showOpenDialog(fileStatusLabel.getScene().getWindow());
    }

    private void configureCaseworkerComboBox() {
        caseworkerComboBox.setConverter(new StringConverter<CaseworkerEntry>() {
            @Override
            public String toString(CaseworkerEntry caseworker) {
                return caseworker == null ? "" : caseworker.name();
            }

            @Override
            public CaseworkerEntry fromString(String s) {
                return null;
            }
        });

        caseworkerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCaseworkerDetail(newVal));
    }

    private void configureDecisionComboBox() {
        decisionComboBox.setCellFactory(listView -> new ListCell<CodelistEntry>() {
            @Override
            protected void updateItem(CodelistEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(null);
                    setGraphic(createDecisionText(item));
                    setStyle("-fx-border-color: transparent transparent #d0d0d0 transparent; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        decisionComboBox.setButtonCell(new ListCell<CodelistEntry>() {
            @Override
            protected void updateItem(CodelistEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(createDecisionText(item));
                }
            }
        });

        decisionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasDecision = (newVal != null) && (openedDocument != null);
            if (previewButton != null) previewButton.setDisable(!hasDecision);
            if (exportButton != null) exportButton.setDisable(!hasDecision);
        });
    }

    private Text createDecisionText(CodelistEntry item) {
        Text text = new Text(item.value());
        text.setWrappingWidth(DECISION_TEXT_WRAP_WIDTH);
        return text;
    }

    private void configureFileNumberField() {
        fileNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            Text text = new Text(newValue);
            text.setFont(fileNumber.getFont());

            double textWidth = text.getLayoutBounds().getWidth();
            double preferredWidth = textWidth + FILE_NUMBER_PADDING;
            double widthWithMin = Math.max(FILE_NUMBER_MIN_WIDTH, preferredWidth);

            fileNumber.setPrefWidth(widthWithMin);
        });
    }

    private void loadSenderName() {
        try {
            senderName = senderConfigLoader.loadSenderName();
            senderNameLabel.setText(senderName);
        } catch (IOException e) {
            showError("Absender-Konfiguration konnte nicht geladen werden", e.getMessage());
        }
    }

    private void loadCaseworkers() {
        try {
            List<CaseworkerEntry> caseworkerEntries = caseworkerConfigLoader.load();
            double maxTextWidth = 0;
            for (CaseworkerEntry caseworkerEntry : caseworkerEntries) {
                Text text = new Text(caseworkerEntry.name());
                double textWidth = text.getLayoutBounds().getWidth();
                if (textWidth > maxTextWidth) {
                    maxTextWidth = textWidth;
                }
            }
            caseworkerComboBox.setPrefWidth(maxTextWidth + CASEWORKER_WIDTH_PADDING);
            caseworkerComboBox.getItems().addAll(caseworkerEntries);
            if (!caseworkerEntries.isEmpty()) {
                caseworkerComboBox.getSelectionModel().selectFirst();
                updateCaseworkerDetail(caseworkerComboBox.getValue());
            }
        } catch (IOException e) {
            showError("Sachbearbeiter-Konfiguration konnte nicht geladen werden", e.getMessage());
        }
    }

    private void updateCaseworkerDetail(CaseworkerEntry entry) {
        if (caseworkerDetailLabel == null) {
            return;
        }
        if (entry == null || entry.xmlBlock() == null || entry.xmlBlock().isBlank()) {
            caseworkerDetailLabel.setText("");
            return;
        }

        String xml = entry.xmlBlock();
        String vorname = extractTag(xml, "vorname");
        String nachname = extractTag(xml, "nachname");
        String zusatz = extractTag(xml, "anschriftenzusatz");
        String verbindung = extractTag(xml, "verbindung");

        StringBuilder sb = new StringBuilder();
        if (!vorname.isEmpty() || !nachname.isEmpty()) {
            sb.append("👤 ").append(vorname).append(" ").append(nachname);
        }
        if (!zusatz.isEmpty()) {
            if (!sb.isEmpty()) sb.append(" • ");
            sb.append(zusatz);
        }
        if (!verbindung.isEmpty()) {
            if (!sb.isEmpty()) sb.append(" • ");
            sb.append(verbindung);
        }
        caseworkerDetailLabel.setText(sb.toString().trim());
    }

    private String extractTag(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<(?:[\\w.-]+:)?" + tagName + "[^>]*>(.*?)</(?:[\\w.-]+:)?" + tagName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private void loadDecisions() {
        try {
            CodelistConfig config = new CodelistConfig();
            CodelistDefinition definition = config.get("sachentscheidung");
            GenericodeReader genericodeReader = new GenericodeReader();
            Path codelistFile = ApplicationPaths.getApplicationDirectory().resolve("codelists").resolve(definition.file());
            List<CodelistEntry> entries = genericodeReader.readAll(codelistFile, definition);
            decisionComboBox.getItems().addAll(entries);
        } catch (Exception e) {
            showError("Sachentscheidung-Codelist konnte nicht geladen werden", e.getMessage());
        }
    }

    private void clearDecisionFields() {
        fileNumber.clear();
        if (!caseworkerComboBox.getItems().isEmpty()) {
            caseworkerComboBox.getSelectionModel().selectFirst();
            updateCaseworkerDetail(caseworkerComboBox.getValue());
        } else {
            caseworkerComboBox.setValue(null);
            if (caseworkerDetailLabel != null) caseworkerDetailLabel.setText("");
        }
        decisionComboBox.setValue(null);
        if (previewButton != null) previewButton.setDisable(true);
        if (exportButton != null) exportButton.setDisable(true);
    }

    private void setDecisionFieldsDisabled(boolean disabled) {
        fileNumber.setDisable(disabled);
        caseworkerComboBox.setDisable(disabled);
        decisionComboBox.setDisable(disabled);
        if (previewButton != null) previewButton.setDisable(true);
        if (exportButton != null) exportButton.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
