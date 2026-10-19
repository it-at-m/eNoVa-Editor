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
import de.muenchen.enovaeditor.config.decision.DecisionConfigLoader;
import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;
import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfoLoader;
import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.error.ProblemDialog;
import de.muenchen.enovaeditor.help.HelpLink;
import de.muenchen.enovaeditor.template.HtmlWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
import de.muenchen.enovaeditor.template.TemplateSource;
import de.muenchen.enovaeditor.template.XPathTemplateRenderer;
import de.muenchen.enovaeditor.util.OutputPathUtil;
import de.muenchen.enovaeditor.xml.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainController {

    private static final double FILE_NUMBER_MIN_WIDTH = 150;
    private static final double FILE_NUMBER_PADDING = 30;
    private static final double CASEWORKER_WIDTH_PADDING = 50;

    private final BrowserOpener browserOpener = new BrowserOpener();
    private final XmlLoader xmlLoader = new XmlLoader();
    private final ErsuchenSachentscheidungChecker checker = new ErsuchenSachentscheidungChecker();
    private final TemplateLoader templateLoader = new TemplateLoader();
    private final XPathTemplateRenderer templateRenderer = new XPathTemplateRenderer();
    private final HtmlWriter htmlWriter = new HtmlWriter();
    private final AnswerTransformer answerTransformer = new AnswerTransformer();
    private final XmlWriter xmlWriter = new XmlWriter();
    private final CaseworkerConfigLoader caseworkerConfigLoader = new CaseworkerConfigLoader();
    private final SenderConfigLoader senderConfigLoader = new SenderConfigLoader();
    private final ManufacturerInfoLoader manufacturerInfoLoader = new ManufacturerInfoLoader();
    private final ErsuchenSachentscheidungReader ersuchenSachentscheidungReader = new ErsuchenSachentscheidungReader();
    private final DecisionConfigLoader decisionConfigLoader = new DecisionConfigLoader();
    private final FlurstueckSplitter flurstueckSplitter = new FlurstueckSplitter();

    private final ObjectProperty<Document> openedDocument = new SimpleObjectProperty<>(null);

    private final BooleanProperty decisionMissing = new SimpleBooleanProperty(true);
    private final List<ComboBox<CodelistEntry>> decisionComboBoxes = new ArrayList<>();
    private final List<CodelistEntry> decisionEntries = new ArrayList<>();
    private final List<javafx.scene.Node> singleDecisionNodes = new ArrayList<>();
    private Path openedXmlPath;
    private String senderName;
    private Map<String, List<String>> allowedDecisionsByRequestCode = Map.of();
    private Map<String, String> requestLabelsByCode = Map.of();
    @FXML
    private GridPane formGrid;

    @FXML
    private StackPane multipleDecisionBox;

    @FXML
    private GridPane multipleDecisionGrid;

    @FXML
    private Label fileStatusLabel;

    @FXML
    private Label senderNameLabel;

    @FXML
    private ComboBox<CaseworkerEntry> caseworkerComboBox;

    @FXML
    private TextField fileNumber;

    @FXML
    private Button generateAnswer;

    @FXML
    private Hyperlink helpLink;

    @FXML
    private Button splitFlurstueckeButton;

    @FXML
    private Label splitFlurstueckeHint;

    @FXML
    private void initialize() {
        setupBindings();

        configureCaseworkerComboBox();
        configureFileNumberField();
    }

    public void loadStartupConfiguration() throws AppException {
        loadSenderName();
        loadCaseworkers();
        loadDecisions();
        loadDecisionMappings();
        loadRequestLabels();
        HelpLink.configure(helpLink);
    }

    @FXML
    protected void onXmlOpenClick() {
        File selectedFile;

        try {
            selectedFile = chooseXmlFile();
        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Dateiauswahl fehlgeschlagen",
                    "Die bisher geöffnete Datei bleibt unverändert.",
                    exception,
                    List.of()
            );
            return;
        }

        if (selectedFile == null) {
            return;
        }

        Path selectedXmlPath = selectedFile.toPath();

        setSplitControlsVisible(false);

        openedDocument.set(null);
        openedXmlPath = null;

        Path outputHtml = null;

        try {
            Document document = xmlLoader.load(selectedFile);

            checker.check(document);

            int anzahlFlurstuecke = flurstueckSplitter.countFlurstuecke(document);

            List<Node> requests = ersuchenSachentscheidungReader.read(document);

            TemplateSource inputTemplate = templateLoader.loadInputTemplate();

            String renderedHtml = templateRenderer.render(inputTemplate, document);

            Path outputHtmlPath = OutputPathUtil.createOutputPath(selectedXmlPath, ".htm");

            outputHtml = htmlWriter.write(renderedHtml, outputHtmlPath);

            openHtmlInBrowser(
                    outputHtml,
                    "Die HTML-Vorschau wurde gespeichert. Öffnen Sie die HTML-Datei bei Bedarf manuell.",
                    List.of(new ErrorDetail("HTML-Datei", outputHtml.toString()))
            );

            createDecisionFields(requests);

            updateFileStatus(selectedXmlPath);
            clearFormFields();
            openedDocument.set(document);
            openedXmlPath = selectedXmlPath;

            setSplitControlsVisible(anzahlFlurstuecke > 1);

        } catch (Exception exception) {
            openedDocument.set(null);
            openedXmlPath = null;
            updateFileStatus(null);

            clearFormFields();
            clearDecisionUi();

            List<ErrorDetail> context = new ArrayList<>();
            context.add(new ErrorDetail("Ausgewählte Datei", selectedXmlPath.toString()));

            if (outputHtml != null) {
                context.add(new ErrorDetail("Gespeicherte HTML-Datei", outputHtml.toString()));
            }

            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Datei kann nicht verarbeitet werden",
                    "Die ausgewählte XML-Datei wurde nicht in das Formular übernommen.",
                    exception,
                    context
            );
            return;
        }
    }

    @FXML
    protected void onGenerateAnswer() {
        Document answerDocument;
        OutputPathUtil.OutputPaths outputPaths;

        try {
            Document inputDocument = openedDocument.get();

            ManufacturerInfo manufacturerInfo = manufacturerInfoLoader.load();

            CaseworkerEntry selectedCaseworker = caseworkerComboBox.getValue();

            List<String> decisionCodes = decisionComboBoxes.stream().map(ComboBox::getValue).map(CodelistEntry::code).toList();

            AnswerParameters parameters = new AnswerParameters(fileNumber.getText(), UUID.randomUUID().toString(), manufacturerInfo, selectedCaseworker.name(), decisionCodes);

            answerDocument = answerTransformer.transform(inputDocument, parameters);
            outputPaths = OutputPathUtil.createOutputPaths(openedXmlPath, "Output");
            xmlWriter.write(answerDocument, outputPaths.xmlPath().toFile());
        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Antwort konnte nicht erzeugt werden",
                    "Die Antwort-XML wurde nicht erfolgreich gespeichert.",
                    exception,
                    List.of()
            );
            return;
        }

        Path outputHtml;

        try {
            TemplateSource outputTemplate = templateLoader.loadOutputTemplate();

            String renderedOutputHtml = templateRenderer.render(outputTemplate, answerDocument);

            outputHtml = htmlWriter.write(renderedOutputHtml, outputPaths.htmlPath());
        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.WARNING,
                    "Antwort gespeichert – HTML-Ansicht fehlgeschlagen",
                    "Die Antwort-XML wurde erfolgreich gespeichert. Die HTML-Ansicht konnte nicht erstellt werden.",
                    exception,
                    List.of(new ErrorDetail("Gespeicherte XML-Datei", outputPaths.xmlPath().toString()))
            );
            return;
        }

        openHtmlInBrowser(
                outputHtml,
                "Antwort-XML und HTML-Ansicht wurden gespeichert. Öffnen Sie die HTML-Datei bei Bedarf manuell.",
                List.of(
                        new ErrorDetail("Gespeicherte XML-Datei", outputPaths.xmlPath().toString()),
                        new ErrorDetail("HTML-Datei", outputHtml.toString())
                )
        );
    }

    @FXML
    protected void onSplitFlurstuecke() {
        List<Path> savedFiles = new ArrayList<>();

        try {
            Document inputDocument = openedDocument.get();

            if (inputDocument == null || openedXmlPath == null) {
                throw new AppException(
                        "Es ist keine XML-Datei zum Aufteilen geöffnet.",
                        List.of()
                );
            }

            int anzahlFlurstuecke = flurstueckSplitter.countFlurstuecke(inputDocument);

            if (anzahlFlurstuecke < 2) {
                throw new AppException(
                        "Die XML-Datei enthält nicht mehrere Flurstücke.",
                        List.of()
                );
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.initOwner(fileStatusLabel.getScene().getWindow());
            confirmation.setTitle("Flurstücke aufteilen");
            confirmation.setHeaderText(null);
            confirmation.setGraphic(null);

            confirmation.setContentText("""
                    Die XML-Datei enthält %d Flurstücke.
                    Es werden %d Einzeldateien erstellt.
                    
                    Die Originaldatei bleibt unverändert.
                    
                    Möchten Sie fortfahren?
                    """.formatted(anzahlFlurstuecke, anzahlFlurstuecke));

            ButtonType confirmButton = new ButtonType(
                    "Aufteilen", ButtonBar.ButtonData.OK_DONE
            );
            ButtonType cancelButton = new ButtonType(
                    "Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE
            );

            confirmation.getButtonTypes().setAll(confirmButton, cancelButton);

            if (confirmation.showAndWait().orElse(cancelButton) != confirmButton) {
                return;
            }

            List<Document> documents = flurstueckSplitter.split(inputDocument);

            if (documents.size() != anzahlFlurstuecke) {
                throw new AppException(
                        "Die Anzahl der erzeugten XML-Dokumente stimmt nicht mit der Anzahl der Flurstücke überein.",
                        List.of(
                                new ErrorDetail("Erwartet", String.valueOf(anzahlFlurstuecke)),
                                new ErrorDetail("Erzeugt", String.valueOf(documents.size()))
                        )
                );
            }

            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);

                checker.check(document);

                int count = flurstueckSplitter.countFlurstuecke(document);

                if (count != 1) {
                    throw new AppException(
                            "Das erzeugte XML-Dokument enthält nicht genau ein Flurstück.",
                            List.of(
                                    new ErrorDetail("Teildokument", String.valueOf(i + 1)),
                                    new ErrorDetail("Anzahl Flurstücke", String.valueOf(count))
                            )
                    );
                }
            }

            Path outputBase = OutputPathUtil.createOutputPath(
                    openedXmlPath,
                    "Split",
                    ""
            );

            for (int i = 0; i < documents.size(); i++) {
                Path outputPath = outputBase.resolveSibling(
                        outputBase.getFileName() + "-%02d.xml".formatted(i + 1)
                );

                xmlWriter.write(documents.get(i), outputPath.toFile());

                savedFiles.add(outputPath);
            }

        } catch (Exception exception) {
            List<ErrorDetail> details = new ArrayList<>();

            if (openedXmlPath != null) {
                details.add(new ErrorDetail(
                        "Ausgangsdatei",
                        openedXmlPath.toString()
                ));
            }

            for (Path savedFile : savedFiles) {
                details.add(new ErrorDetail(
                        "Bereits gespeicherte Datei",
                        savedFile.toString()
                ));
            }

            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Flurstücke konnten nicht aufgeteilt werden",
                    savedFiles.isEmpty()
                            ? "Es wurden keine neuen XML-Dateien gespeichert."
                            : "Die Aufteilung wurde nur teilweise gespeichert. "
                            + "Bereits erzeugte Dateien bleiben erhalten.",
                    exception,
                    details
            );

            return;
        }

        resetEditorAfterSplit();

        Alert successDialog = new Alert(Alert.AlertType.INFORMATION);
        successDialog.initOwner(fileStatusLabel.getScene().getWindow());
        successDialog.setTitle("Flurstücke erfolgreich aufgeteilt");
        successDialog.setHeaderText(null);

        Label message = new Label("""
                Die XML-Datei wurde erfolgreich in %d Einzeldateien aufgeteilt.
                
                Sie können nun jede Einzeldatei über „XML öffnen“
                laden, bearbeiten und die jeweilige Antwort generieren.
                """.formatted(savedFiles.size()));
        message.setWrapText(true);

        String directory = savedFiles.getFirst().getParent().toString();

        Label location = new Label("Speicherort: " + directory);
        location.setMinWidth(0);
        location.setPrefWidth(0);
        location.setMaxWidth(Double.MAX_VALUE);
        location.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        location.setTooltip(new Tooltip(directory));

        successDialog.getDialogPane().setContent(new VBox(10, message, location));
        successDialog.setResizable(true);
        successDialog.showAndWait();
    }

    private File chooseXmlFile() {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("XML-Datei auswählen");

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));

        return fileChooser.showOpenDialog(fileStatusLabel.getScene().getWindow());
    }

    private void updateFileStatus(Path path) {
        if (path == null) {
            fileStatusLabel.setText("Keine Datei ausgewählt");
            fileStatusLabel.setTooltip(null);
            return;
        }

        String fullPath = path.toAbsolutePath().normalize().toString();

        fileStatusLabel.setText(fullPath);
        fileStatusLabel.setTooltip(new Tooltip(fullPath));
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
    }

    private void createDecisionFields(List<Node> requests) throws AppException {

        clearDecisionUi();

        List<String> requestCodes = readRequestCodes(requests);

        boolean multipleRequests = requestCodes.size() > 1;

        multipleDecisionBox.setVisible(multipleRequests);
        multipleDecisionBox.setManaged(multipleRequests);

        for (int row = 0; row < requestCodes.size(); row++) {
            String requestCode = requestCodes.get(row);

            Label label = createDecisionLabel(requestCode, row, multipleRequests);

            List<CodelistEntry> allowedDecisionEntries = getAllowedDecisionEntries(requestCode);

            ComboBox<CodelistEntry> comboBox = createDecisionComboBox(allowedDecisionEntries);

            addDecisionField(label, comboBox, row, multipleRequests);

            decisionComboBoxes.add(comboBox);
        }

        updateDecisionMissing();
    }

    private void addDecisionField(Label label, ComboBox<CodelistEntry> comboBox, int row, boolean multipleRequests) {

        if (multipleRequests) {
            multipleDecisionGrid.add(label, 0, row);
            multipleDecisionGrid.add(comboBox, 1, row);
        } else {
            int decisionRow = GridPane.getRowIndex(multipleDecisionBox);

            formGrid.add(label, 0, decisionRow);
            formGrid.add(comboBox, 1, decisionRow);

            singleDecisionNodes.add(label);
            singleDecisionNodes.add(comboBox);
        }
    }

    private Label createDecisionLabel(String requestCode, int row, boolean multipleRequests) throws AppException {

        String requestLabel = requestLabelsByCode.get(requestCode);

        if (requestLabel == null) {
            throw new AppException(
                    "Für das Ersuchen ist keine Bezeichnung konfiguriert.",
                    List.of(new ErrorDetail("Request-Code", requestCode))
            );
        }

        Label label = new Label(multipleRequests ? (row + 1) + ". " + requestLabel : "Entscheidung:");

        if (multipleRequests) {
            label.setWrapText(true);
        }

        return label;
    }

    private ComboBox<CodelistEntry> createDecisionComboBox(List<CodelistEntry> allowedDecisionEntries) {

        ComboBox<CodelistEntry> comboBox = new ComboBox<>();

        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getItems().addAll(allowedDecisionEntries);

        configureDecisionComboBox(comboBox);

        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateDecisionMissing());

        return comboBox;
    }

    private List<CodelistEntry> getAllowedDecisionEntries(String requestCode) throws AppException {

        List<String> allowedDecisionCodes = allowedDecisionsByRequestCode.get(requestCode);

        List<CodelistEntry> allowedEntries = decisionEntries.stream()
                .filter(entry -> allowedDecisionCodes.contains(entry.code()))
                .toList();

        if (allowedEntries.isEmpty()) {
            throw new AppException(
                    "Für das Ersuchen ist keine auswählbare Sachentscheidung konfiguriert.",
                    List.of(new ErrorDetail("Request-Code", requestCode))
            );
        }

        return allowedEntries;
    }

    private List<String> readRequestCodes(List<Node> requests) throws AppException {

        List<String> requestCodes = new ArrayList<>();

        for (Node request : requests) {
            String requestCode = ersuchenSachentscheidungReader.readRequestCode(request);

            if (!allowedDecisionsByRequestCode.containsKey(requestCode)) {
                throw new AppException(
                        "Für das Ersuchen ist keine Entscheidungszuordnung konfiguriert.",
                        List.of(new ErrorDetail("Request-Code", requestCode))
                );
            }

            requestCodes.add(requestCode);
        }

        return requestCodes;
    }

    private void updateDecisionMissing() {
        boolean missing = decisionComboBoxes.stream().anyMatch(comboBox -> comboBox.getValue() == null);

        decisionMissing.set(missing);
    }

    private void configureDecisionComboBox(ComboBox<CodelistEntry> comboBox) {
        configureDecisionOptions(comboBox);
        configureSelectedDecision(comboBox);
    }

    private void configureDecisionOptions(ComboBox<CodelistEntry> comboBox) {
        comboBox.setCellFactory(listView -> {

            listView.minWidthProperty().bind(comboBox.widthProperty());
            listView.prefWidthProperty().bind(comboBox.widthProperty());
            listView.maxWidthProperty().bind(comboBox.widthProperty());

            return new ListCell<CodelistEntry>() {

                private final Text valueText = new Text();

                {
                    valueText.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset()), widthProperty(), paddingProperty()));
                }

                @Override
                protected void updateItem(CodelistEntry item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        valueText.setText("");
                        setText(null);
                        setGraphic(null);
                        setStyle(null);
                    } else {
                        valueText.setText(item.value());

                        setText(null);
                        setGraphic(valueText);

                        setStyle("-fx-border-color: transparent transparent #d0d0d0 transparent;" + "-fx-border-width: 0 0 1 0;");
                    }
                }
            };
        });
    }

    private void configureSelectedDecision(ComboBox<CodelistEntry> comboBox) {
        comboBox.setButtonCell(new ListCell<CodelistEntry>() {

            private final Text valueText = new Text();

            {
                valueText.wrappingWidthProperty().bind(widthProperty());
            }

            @Override
            protected void updateItem(CodelistEntry item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    valueText.setText("");
                    setText(null);
                    setGraphic(null);
                } else {
                    valueText.setText(item.value());

                    setText(null);
                    setGraphic(valueText);
                }
            }
        });
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

    private void loadSenderName() throws AppException {
        senderName = senderConfigLoader.loadSenderName();
        senderNameLabel.setText(senderName);
    }

    private void loadCaseworkers() throws AppException {
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
    }

    private void loadDecisions() throws AppException {
        CodelistConfig config = new CodelistConfig();
        CodelistDefinition definition = config.get("sachentscheidung");
        Path codelistFile = resolveCodelistFile(definition);
        List<CodelistEntry> entries = new GenericodeReader().readAll(codelistFile, definition);

        if (entries.isEmpty()) {
            throw new AppException(
                    "Die Sachentscheidung-Codelist enthält keine auswählbaren Einträge.",
                    List.of(new ErrorDetail("Datei", codelistFile.toString()))
            );
        }

        decisionEntries.clear();
        decisionEntries.addAll(entries);
    }

    private void loadRequestLabels() throws AppException {
        CodelistConfig config = new CodelistConfig();
        CodelistDefinition definition = config.get("ersuchenSachentscheidung");
        Path codelistFile = resolveCodelistFile(definition);
        List<CodelistEntry> entries = new GenericodeReader().readAll(codelistFile, definition);

        if (entries.isEmpty()) {
            throw new AppException(
                    "Die Ersuchen-Sachentscheidung-Codelist enthält keine Einträge.",
                    List.of(new ErrorDetail("Datei", codelistFile.toString()))
            );
        }

        Map<String, String> labels = new LinkedHashMap<>();

        for (CodelistEntry entry : entries) {
            if (labels.putIfAbsent(entry.code(), entry.value()) != null) {
                throw new AppException(
                        "Die Ersuchen-Sachentscheidung-Codelist enthält einen Code mehrfach.",
                        List.of(
                                new ErrorDetail("Datei", codelistFile.toString()),
                                new ErrorDetail("Code", entry.code())
                        )
                );
            }
        }

        requestLabelsByCode = Map.copyOf(labels);
    }

    private Path resolveCodelistFile(CodelistDefinition definition) throws AppException {
        return ApplicationPaths.getApplicationDirectory()
                .resolve("codelists")
                .resolve(definition.file());
    }

    private void loadDecisionMappings() throws AppException {
        allowedDecisionsByRequestCode = decisionConfigLoader.load();
    }

    private void clearFormFields() {
        fileNumber.clear();
        caseworkerComboBox.setValue(null);
        for (ComboBox<CodelistEntry> comboBox : decisionComboBoxes) {
            comboBox.setValue(null);
        }
    }

    private void clearDecisionUi() {
        formGrid.getChildren().removeAll(singleDecisionNodes);
        singleDecisionNodes.clear();

        multipleDecisionGrid.getChildren().clear();

        multipleDecisionBox.setVisible(false);
        multipleDecisionBox.setManaged(false);

        decisionComboBoxes.clear();
    }

    private void setSplitControlsVisible(boolean visible) {
        splitFlurstueckeButton.setVisible(visible);
        splitFlurstueckeButton.setManaged(visible);

        splitFlurstueckeHint.setVisible(visible);
        splitFlurstueckeHint.setManaged(visible);
    }

    private void resetEditorAfterSplit() {
        clearFormFields();
        clearDecisionUi();

        openedDocument.set(null);
        openedXmlPath = null;

        updateFileStatus(null);
        setSplitControlsVisible(false);
    }

    private void setupBindings() {
        setupGenerateAnswerButtonBinding();
        setupDecisionFieldsDisabledBinding();
    }

    private void setupGenerateAnswerButtonBinding() {
        BooleanBinding fileNumberMissing = Bindings.createBooleanBinding(() -> fileNumber.getText().isBlank(), fileNumber.textProperty());

        BooleanBinding caseworkerMissing = caseworkerComboBox.getSelectionModel().selectedItemProperty().isNull();

        generateAnswer.disableProperty().bind(fileNumberMissing.or(caseworkerMissing).or(decisionMissing).or(openedDocument.isNull()));
    }

    private void setupDecisionFieldsDisabledBinding() {
        fileNumber.disableProperty().bind(openedDocument.isNull());
        caseworkerComboBox.disableProperty().bind(openedDocument.isNull());
    }

    private void openHtmlInBrowser(
            Path htmlPath,
            String workflowHint,
            List<ErrorDetail> successfulResults
    ) {

        try {
            browserOpener.open(htmlPath);

        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.WARNING,
                    "Browser konnte nicht geöffnet werden",
                    workflowHint,
                    exception,
                    successfulResults
            );
        }
    }
}
