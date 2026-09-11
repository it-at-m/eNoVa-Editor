package de.muenchen.enovaeditor;

import de.muenchen.enovaeditor.browser.BrowserOpener;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerEntry;
import de.muenchen.enovaeditor.decision.DecisionData;
import de.muenchen.enovaeditor.decision.EnovaDecisionType;
import de.muenchen.enovaeditor.decision.EnovaResponseBuilder;
import de.muenchen.enovaeditor.pdf.BescheidHtmlRenderer;
import de.muenchen.enovaeditor.pdf.BescheidPdfGenerator;
import de.muenchen.enovaeditor.template.HtmlOutputWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
import de.muenchen.enovaeditor.template.XPathTemplateRenderer;
import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungChecker;
import de.muenchen.enovaeditor.xml.XmlLoader;
import de.muenchen.enovaeditor.xml.XmlWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.w3c.dom.Document;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainController {

    private final BrowserOpener browserOpener = new BrowserOpener();
    private final XmlLoader xmlLoader = new XmlLoader();
    private final ErsuchenSachentscheidungChecker checker = new ErsuchenSachentscheidungChecker();
    private final TemplateLoader templateLoader = new TemplateLoader();
    private final XPathTemplateRenderer templateRenderer = new XPathTemplateRenderer();
    private final HtmlOutputWriter htmlOutputWriter = new HtmlOutputWriter();
    private final CaseworkerConfigLoader caseworkerConfigLoader = new CaseworkerConfigLoader();
    private final EnovaResponseBuilder responseBuilder = new EnovaResponseBuilder();
    private final BescheidPdfGenerator pdfGenerator = new BescheidPdfGenerator();
    private final BescheidHtmlRenderer bescheidHtmlRenderer = new BescheidHtmlRenderer();
    private final XmlWriter xmlWriter = new XmlWriter();

    @FXML private VBox dropZone;
    @FXML private Label fileStatusLabel;
    @FXML private Label fileDetailLabel;
    @FXML private ComboBox<CaseworkerEntry> caseworkerComboBox;
    @FXML private Label caseworkerInfoLabel;

    @FXML private ComboBox<EnovaDecisionType> decisionTypeComboBox;
    @FXML private TextField fileNumberField;
    @FXML private TextField feeField;
    @FXML private TextArea tenorArea;
    @FXML private TextArea reasoningArea;

    @FXML private Button exportPackageButton;
    @FXML private Button exportXmlButton;
    @FXML private Button exportPdfButton;
    @FXML private Button openBrowserButton;
    @FXML private Label statusNotificationLabel;

    @FXML private TabPane documentTabPane;
    @FXML private Tab requestTab;
    @FXML private Tab previewTab;
    @FXML private Tab infoTab;

    @FXML private WebView requestWebView;
    @FXML private WebView decisionPreviewWebView;
    @FXML private WebView infoWebView;

    private File currentXmlFile;
    private Document currentDocument;
    private Path currentOutputHtmlPath;

    @FXML
    private void initialize() {
        setupCaseworkerComboBox();
        setupDecisionComboBox();
        setupDragAndDrop();
        setupLivePreviewListeners();
        loadInfoTabContent();
        updateExportButtonStates();
    }

    private void setupCaseworkerComboBox() {
        caseworkerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CaseworkerEntry caseworker) {
                return caseworker == null ? "" : caseworker.name();
            }

            @Override
            public CaseworkerEntry fromString(String string) {
                return null;
            }
        });

        try {
            List<CaseworkerEntry> entries = caseworkerConfigLoader.load();
            caseworkerComboBox.getItems().addAll(entries);
            if (!entries.isEmpty()) {
                caseworkerComboBox.getSelectionModel().select(0);
                updateCaseworkerInfo(entries.get(0));
            }
        } catch (IOException e) {
            caseworkerInfoLabel.setText("Hinweis: caseworkers.xml konnte nicht geladen werden.");
        }

        caseworkerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateCaseworkerInfo(newVal);
            updateBescheidPreview();
        });
    }

    private void updateCaseworkerInfo(CaseworkerEntry entry) {
        if (entry == null) {
            caseworkerInfoLabel.setText("");
            return;
        }
        String phone = entry.extractPhone();
        String id = entry.extractId();
        StringBuilder sb = new StringBuilder();
        if (!phone.isBlank()) {
            sb.append("Tel.: ").append(phone);
        }
        if (!id.isBlank()) {
            if (!sb.isEmpty()) sb.append(" • ");
            sb.append("Kennung: ").append(id);
        }
        caseworkerInfoLabel.setText(sb.toString());
    }

    private void setupDecisionComboBox() {
        decisionTypeComboBox.getItems().addAll(EnovaDecisionType.values());
        decisionTypeComboBox.getSelectionModel().select(EnovaDecisionType.NEGATIVZEUGNIS_NICHTAUSUEBUNG);

        decisionTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(EnovaDecisionType object) {
                return object != null ? object.getDisplayName() : "";
            }

            @Override
            public EnovaDecisionType fromString(String string) {
                return null;
            }
        });

        // Set default tenor and reasoning
        tenorArea.setText(EnovaDecisionType.NEGATIVZEUGNIS_NICHTAUSUEBUNG.getDefaultTenor());
        reasoningArea.setText(EnovaDecisionType.NEGATIVZEUGNIS_NICHTAUSUEBUNG.getDefaultBegruendung());

        decisionTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tenorArea.setText(newVal.getDefaultTenor());
                reasoningArea.setText(newVal.getDefaultBegruendung());
                updateBescheidPreview();
            }
        });
    }

    private void setupDragAndDrop() {
        if (dropZone == null) return;

        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                dropZone.setStyle("-fx-border-color: #0284c7; -fx-border-style: solid; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: #e0f2fe; -fx-padding: 12; -fx-cursor: hand;");
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.setStyle("-fx-border-color: #93c5fd; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: #f8fafc; -fx-padding: 12; -fx-cursor: hand;");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".xml")) {
                    loadXmlFile(file);
                    success = true;
                } else {
                    showError("Ungültige Datei", "Bitte ziehen Sie eine XML-Datei (XJustiz) in dieses Feld.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupLivePreviewListeners() {
        fileNumberField.textProperty().addListener((obs, o, n) -> updateBescheidPreview());
        feeField.textProperty().addListener((obs, o, n) -> updateBescheidPreview());
        tenorArea.textProperty().addListener((obs, o, n) -> updateBescheidPreview());
        reasoningArea.textProperty().addListener((obs, o, n) -> updateBescheidPreview());
    }

    public void loadXmlFile(File selectedFile) {
        if (selectedFile == null || !selectedFile.exists()) return;

        try {
            var document = xmlLoader.load(selectedFile);
            checker.check(document);

            String inputTemplate = templateLoader.loadInputTemplate();
            String renderedHtml = templateRenderer.render(inputTemplate, document);

            this.currentXmlFile = selectedFile;
            this.currentDocument = document;

            // Load into embedded WebView
            requestWebView.getEngine().loadContent(renderedHtml);

            // Write HTML file alongside
            try {
                this.currentOutputHtmlPath = htmlOutputWriter.write(renderedHtml, selectedFile);
            } catch (Exception ignored) {
            }

            // Update Labels
            fileStatusLabel.setText("✅ " + selectedFile.getName());
            extractAndDisplayDetails(document);

            // Suggest file number if empty
            if (fileNumberField.getText().isBlank()) {
                String notarUrNr = safeEvaluate("//tns:datenDerUrkunde/tns:urNr", document);
                if (!notarUrNr.isBlank()) {
                    fileNumberField.setText("61-VKR-" + notarUrNr.replaceAll("\\s+", ""));
                }
            }

            updateBescheidPreview();
            updateExportButtonStates();
            statusNotificationLabel.setText("Datei erfolgreich geladen: " + selectedFile.getName());
            statusNotificationLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");

        } catch (Exception e) {
            fileStatusLabel.setText("❌ Fehler beim Laden");
            fileDetailLabel.setText(e.getMessage());
            statusNotificationLabel.setText("Fehler: " + e.getMessage());
            statusNotificationLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            showError("Datei kann nicht verarbeitet werden", e.getMessage());
        }
    }

    private void extractAndDisplayDetails(Document doc) {
        XPath xpath = createXPath();
        String notarName = safeEvaluate("//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='208']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:vorname", doc)
                + " " + safeEvaluate("//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='208']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:nachname", doc);
        String urNr = safeEvaluate("//tns:datenDerUrkunde/tns:urNr", doc);
        String urDatum = safeEvaluate("//tns:datenDerUrkunde/tns:urkundsdatum", doc);
        String gemarkung = safeEvaluate("//tns:identifikationFlurstueck/tns:gemarkungsschluessel", doc);
        String flurstueck = safeEvaluate("//tns:identifikationFlurstueck/tns:flurstuecksnummer/tns:zaehler", doc);

        fileDetailLabel.setText(String.format("Notariat: %s • Urkunde: %s (vom %s) • Flurstück: %s (Gemarkung: %s)",
                notarName.trim().isEmpty() ? "Unbekannt" : notarName.trim(),
                urNr.isBlank() ? "–" : urNr,
                urDatum.isBlank() ? "–" : urDatum,
                flurstueck.isBlank() ? "–" : flurstueck,
                gemarkung.isBlank() ? "–" : gemarkung));
    }

    private void updateBescheidPreview() {
        if (currentDocument == null) return;
        DecisionData decision = buildCurrentDecisionData();
        String html = bescheidHtmlRenderer.renderHtml(currentDocument, decision);
        decisionPreviewWebView.getEngine().loadContent(html);
    }

    private DecisionData buildCurrentDecisionData() {
        CaseworkerEntry caseworker = caseworkerComboBox.getSelectionModel().getSelectedItem();
        String caseworkerName = caseworker != null ? caseworker.name() : "";
        String caseworkerPhone = caseworker != null ? caseworker.extractPhone() : "";

        return DecisionData.of(
                decisionTypeComboBox.getSelectionModel().getSelectedItem() != null
                        ? decisionTypeComboBox.getSelectionModel().getSelectedItem()
                        : EnovaDecisionType.NEGATIVZEUGNIS_NICHTAUSUEBUNG,
                fileNumberField.getText(),
                feeField.getText(),
                caseworkerName,
                caseworkerPhone,
                tenorArea.getText(),
                reasoningArea.getText()
        );
    }

    @FXML
    protected void onXmlOpenClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("XJustiz-XML-Datei auswählen (eNoVa 2900003)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien (*.xml)", "*.xml"));

        if (currentXmlFile != null && currentXmlFile.getParentFile() != null) {
            fileChooser.setInitialDirectory(currentXmlFile.getParentFile());
        }

        Window window = fileStatusLabel.getScene() != null ? fileStatusLabel.getScene().getWindow() : null;
        File selectedFile = fileChooser.showOpenDialog(window);
        if (selectedFile != null) {
            loadXmlFile(selectedFile);
        }
    }

    @FXML
    protected void onLoadSampleClick() {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        if (sampleFile.exists()) {
            loadXmlFile(sampleFile);
        } else {
            showError("Musterdatei fehlt", "Die Musterdatei samples/xjustiz_beispiel_2900003.xml konnte nicht gefunden werden.");
        }
    }

    @FXML
    protected void onExportPackageClick() {
        if (currentDocument == null || currentXmlFile == null) {
            showWarning("Keine Datei geladen", "Bitte öffnen Sie zuerst eine Eingangs-XML.");
            return;
        }

        File targetDir = currentXmlFile.getParentFile();
        String baseName = removeFileExtension(currentXmlFile.getName());

        File xmlFile = new File(targetDir, baseName + "-sachentscheidung-2900004.xml");
        File pdfFile = new File(targetDir, baseName + "-bescheid-negativzeugnis.pdf");

        DecisionData decision = buildCurrentDecisionData();

        try {
            Document responseDoc = responseBuilder.buildResponse(currentDocument, decision);
            xmlWriter.write(responseDoc, xmlFile);
            pdfGenerator.generatePdf(currentDocument, decision, pdfFile);

            String msg = "✅ beBPo-Paket erfolgreich erzeugt:\n\n"
                    + "1. XJustiz-Antwortdatei:\n" + xmlFile.getName() + "\n"
                    + "   (Hinweis: Vor beBPo-Versand mit CAdES detached qeS signieren)\n\n"
                    + "2. Amtlicher Bescheid / Negativzeugnis (PDF):\n" + pdfFile.getName() + "\n\n"
                    + "Speicherort: " + targetDir.getAbsolutePath();

            statusNotificationLabel.setText("beBPo-Paket exportiert nach: " + targetDir.getName());
            statusNotificationLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("beBPo-Paket erfolgreich erstellt");
            alert.setHeaderText("XML-Antwort & Bescheid-PDF wurden erzeugt");
            alert.setContentText(msg);

            ButtonType openDirButton = new ButtonType("Ordner öffnen");
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(openDirButton, okButton);

            alert.showAndWait().ifPresent(type -> {
                if (type == openDirButton) {
                    try {
                        Desktop.getDesktop().open(targetDir);
                    } catch (Exception ignored) {
                    }
                }
            });

        } catch (Exception e) {
            showError("Fehler beim Erstellen des beBPo-Pakets", e.getMessage());
        }
    }

    @FXML
    protected void onExportXmlClick() {
        if (currentDocument == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("XJustiz-Antwortdatei (2900004) speichern");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien (*.xml)", "*.xml"));
        if (currentXmlFile != null) {
            chooser.setInitialFileName(removeFileExtension(currentXmlFile.getName()) + "-sachentscheidung-2900004.xml");
            chooser.setInitialDirectory(currentXmlFile.getParentFile());
        }

        File target = chooser.showSaveDialog(fileStatusLabel.getScene().getWindow());
        if (target != null) {
            try {
                DecisionData decision = buildCurrentDecisionData();
                Document responseDoc = responseBuilder.buildResponse(currentDocument, decision);
                xmlWriter.write(responseDoc, target);
                statusNotificationLabel.setText("XJustiz-Antwort gespeichert: " + target.getName());
            } catch (Exception e) {
                showError("Export fehlgeschlagen", e.getMessage());
            }
        }
    }

    @FXML
    protected void onExportPdfClick() {
        if (currentDocument == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Bescheid als PDF exportieren");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dokument (*.pdf)", "*.pdf"));
        if (currentXmlFile != null) {
            chooser.setInitialFileName(removeFileExtension(currentXmlFile.getName()) + "-bescheid-negativzeugnis.pdf");
            chooser.setInitialDirectory(currentXmlFile.getParentFile());
        }

        File target = chooser.showSaveDialog(fileStatusLabel.getScene().getWindow());
        if (target != null) {
            try {
                DecisionData decision = buildCurrentDecisionData();
                pdfGenerator.generatePdf(currentDocument, decision, target);
                statusNotificationLabel.setText("PDF gespeichert: " + target.getName());
            } catch (Exception e) {
                showError("PDF-Export fehlgeschlagen", e.getMessage());
            }
        }
    }

    @FXML
    protected void onOpenBrowserClick() {
        if (currentOutputHtmlPath != null) {
            try {
                browserOpener.open(currentOutputHtmlPath);
            } catch (IOException e) {
                showWarning("Browser konnte nicht geöffnet werden", currentOutputHtmlPath.toString());
            }
        }
    }

    @FXML
    protected void onPrintClick() {
        Tab selected = documentTabPane.getSelectionModel().getSelectedItem();
        WebView viewToPrint = (selected == previewTab) ? decisionPreviewWebView : requestWebView;
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(fileStatusLabel.getScene().getWindow())) {
            viewToPrint.getEngine().print(job);
            job.endJob();
        }
    }

    @FXML
    protected void onZoomInClick() {
        requestWebView.setZoom(requestWebView.getZoom() * 1.15);
        decisionPreviewWebView.setZoom(decisionPreviewWebView.getZoom() * 1.15);
    }

    @FXML
    protected void onZoomOutClick() {
        requestWebView.setZoom(requestWebView.getZoom() / 1.15);
        decisionPreviewWebView.setZoom(decisionPreviewWebView.getZoom() / 1.15);
    }

    private void updateExportButtonStates() {
        boolean disabled = (currentDocument == null);
        exportPackageButton.setDisable(disabled);
        exportXmlButton.setDisable(disabled);
        exportPdfButton.setDisable(disabled);
        openBrowserButton.setDisable(disabled);
    }

    private void loadInfoTabContent() {
        String infoHtml = """
                <!DOCTYPE html><html><head><meta charset='UTF-8'>
                <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; padding: 25px; color: #1e293b; }
                h2 { color: #004b76; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }
                h3 { color: #0b2a3d; margin-top: 20px; }
                .card { background: #f8fafc; border-left: 4px solid #009ee3; padding: 14px 18px; margin: 15px 0; border-radius: 4px; }
                .warn { border-left-color: #d97706; background: #fffbeb; }
                code { background: #e2e8f0; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
                li { margin-bottom: 6px; }
                </style></head><body>
                <h2>eNoVA & beBPo – Praxisleitfaden für Behörden</h2>
                <div class='card'>
                <strong>Rechtsgrundlage:</strong> Gesetz zur Digitalisierung des Vollzugs von Immobilienverträgen (eNoVA-Gesetz, BGBl. 2026 I Nr. 192).
                <br><strong>Frist:</strong> Ab dem <strong>1. Januar 2027</strong> ist die elektronische Kommunikation zwischen Notaren und Kommunen für Vorkaufsrechtsanfragen und Zeugnisse (§ 28 BauGB) <strong>ausschließlich elektronisch im XJustiz-Standard verpflichtend</strong>.
                </div>
                
                <h3>Wichtige Schritte für Sachbearbeitende:</h3>
                <ol>
                <li><strong>Eingang:</strong> Notarielle Anfragen treffen als XML-Datensatz (<code>nachricht.enova.entscheidung.2900003</code>) im beBPo ein.</li>
                <li><strong>Prüfung:</strong> Die Datei in den eNoVa-Editor ziehen – Daten (Vertragsparteien, Flurstücke, Urkunde) werden sofort lesbar dargestellt.</li>
                <li><strong>Entscheidung:</strong> Entscheidungstenor (z. B. Negativzeugnis nach § 28 Abs. 1 S. 1 BauGB), Aktenzeichen und Gebühr festlegen.</li>
                <li><strong>Export:</strong> Klick auf <em>„beBPo-Paket exportieren“</em> erzeugt zeitgleich:
                   <ul>
                     <li>Die XJustiz-Antwortdatei (<code>nachricht.enova.entscheidung.2900004</code>).</li>
                     <li>Den amtlichen Bescheid als PDF zur behördlichen Veraktung.</li>
                   </ul>
                </li>
                <li><strong>Signatur vor beBPo-Versand:</strong>
                   <div class='card warn'>
                     <strong>Wichtig (gemäß Bundesnotarkammer):</strong>
                     Positive Bescheide (Negativzeugnis) zur Vorlage beim Grundbuchamt (§§ 29, 137 GBO) müssen mit einer <strong>qualifizierten elektronischen Signatur (qeS) mit Behördennachweis</strong> versehen werden.
                     Zulässiges Signaturformat: <strong>CAdES (detached)</strong>. Das heißt, neben der <code>.xml</code>-Datei entsteht eine separate Signaturdatei (<code>.p7s</code> bzw. <code>.pkcs7</code>). Beide Dateien werden per beBPo an das Notariat übermittelt.
                   </div>
                </li>
                </ol>
                </body></html>
                """;
        infoWebView.getEngine().loadContent(infoHtml);
    }

    private String removeFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot <= 0 ? fileName : fileName.substring(0, lastDot);
    }

    private String safeEvaluate(String expr, Document doc) {
        try {
            return createXPath().evaluate(expr, doc).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) {
                return "tns".equals(prefix) ? "http://www.xjustiz.de" : javax.xml.XMLConstants.NULL_NS_URI;
            }
            @Override public String getPrefix(String namespaceURI) {
                return "http://www.xjustiz.de".equals(namespaceURI) ? "tns" : null;
            }
            @Override public Iterator<String> getPrefixes(String namespaceURI) {
                return "http://www.xjustiz.de".equals(namespaceURI) ? Set.of("tns").iterator() : Collections.emptyIterator();
            }
        });
        return xpath;
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
