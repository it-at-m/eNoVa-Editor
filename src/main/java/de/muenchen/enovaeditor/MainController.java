package de.muenchen.enovaeditor;

import de.muenchen.enovaeditor.browser.BrowserOpener;
import de.muenchen.enovaeditor.codelist.CodelistConfig;
import de.muenchen.enovaeditor.codelist.CodelistDefinition;
import de.muenchen.enovaeditor.codelist.CodelistEntry;
import de.muenchen.enovaeditor.codelist.GenericodeReader;
import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.config.SenderConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader;
import de.muenchen.enovaeditor.config.caseworker.CaseworkerDialog;
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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private final List<DecisionRow> decisionRows = new ArrayList<>();
    private final List<CodelistEntry> decisionEntries = new ArrayList<>();
    private Path openedXmlPath;
    private Path caseworkersFile;
    private boolean caseworkersFromInstallationDirectory;
    private String senderName;
    private Map<String, List<String>> allowedDecisionsByRequestCode = Map.of();
    private Map<String, String> requestLabelsByCode = Map.of();
    @FXML
    private StackPane multipleDecisionBox;

    @FXML
    private GridPane multipleDecisionGrid;

    @FXML
    private ComboBox<CodelistEntry> addRequestComboBox;

    @FXML
    private Button addRequestButton;

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
    private Label caseworkerHint;

    @FXML
    private Button kontaktdatenButton;

    @FXML
    private void initialize() {
        setupBindings();

        configureCaseworkerComboBox();
        configureAddRequestComboBox();
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

            List<DecisionAnswer> decisions = decisionRows.stream()
                    .map(row -> new DecisionAnswer(
                            row.requestCode(),
                            requestLabelsByCode.get(row.requestCode()),
                            row.decisionComboBox().getValue().code()))
                    .toList();

            AnswerParameters parameters = new AnswerParameters(fileNumber.getText(), UUID.randomUUID().toString(), manufacturerInfo, selectedCaseworker.name(), caseworkersFile.toUri().toString(), decisions);

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

    @FXML
    protected void onAddRequest() {
        CodelistEntry request = addRequestComboBox.getValue();

        if (openedDocument.get() == null || request == null) {
            return;
        }

        try {
            addDecisionRow(request.code());
            addRequestComboBox.setValue(null);
        } catch (AppException exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Frage konnte nicht hinzugefügt werden",
                    "Die Frage wurde nicht in das Formular übernommen.",
                    exception,
                    List.of()
            );
        }
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

    private void configureAddRequestComboBox() {
        configureDecisionOptions(addRequestComboBox);
        configureSelectedDecision(addRequestComboBox);
        addRequestComboBox.setConverter(new StringConverter<CodelistEntry>() {
            @Override
            public String toString(CodelistEntry request) {
                return request == null ? "" : formatCodelistEntry(request);
            }

            @Override
            public CodelistEntry fromString(String text) {
                return null;
            }
        });
    }

    private void createDecisionFields(List<Node> requests) throws AppException {

        clearDecisionUi();

        List<String> requestCodes = readRequestCodes(requests);

        for (String requestCode : requestCodes) {
            addDecisionRow(requestCode);
        }

        multipleDecisionBox.setVisible(true);
        multipleDecisionBox.setManaged(true);
    }

    private void addDecisionRow(String requestCode) throws AppException {
        List<CodelistEntry> allowedDecisionEntries = getAllowedDecisionEntries(requestCode);
        ComboBox<CodelistEntry> comboBox = createDecisionComboBox(allowedDecisionEntries);
        decisionRows.add(new DecisionRow(requestCode, comboBox));
        refreshDecisionRows();
    }

    private void refreshDecisionRows() {
        multipleDecisionGrid.getChildren().clear();

        for (int row = 0; row < decisionRows.size(); row++) {
            DecisionRow decisionRow = decisionRows.get(row);
            Label label = new Label(
                    (row + 1) + ". " + formatCodelistEntry(
                            new CodelistEntry(
                                    decisionRow.requestCode(),
                                    requestLabelsByCode.get(decisionRow.requestCode())
                            )
                    )
            );
            label.setWrapText(true);

            Button removeButton = new Button("✕");
            removeButton.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
            removeButton.setTooltip(new Tooltip("Frage entfernen"));
            removeButton.setAccessibleText("Frage entfernen");
            removeButton.setOnAction(event -> {
                decisionRows.remove(decisionRow);
                refreshDecisionRows();
            });

            multipleDecisionGrid.add(label, 0, row);
            multipleDecisionGrid.add(decisionRow.decisionComboBox(), 1, row);
            multipleDecisionGrid.add(removeButton, 2, row);
        }

        updateDecisionMissing();
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

            if (!requestLabelsByCode.containsKey(requestCode)) {
                throw new AppException(
                        "Für das Ersuchen ist keine Bezeichnung konfiguriert.",
                        List.of(new ErrorDetail("Request-Code", requestCode))
                );
            }

            requestCodes.add(requestCode);
        }

        return requestCodes;
    }

    private void updateDecisionMissing() {
        boolean missing = decisionRows.isEmpty()
                || decisionRows.stream().anyMatch(row -> row.decisionComboBox().getValue() == null);

        decisionMissing.set(missing);
    }

    private void configureDecisionComboBox(ComboBox<CodelistEntry> comboBox) {
        configureDecisionOptions(comboBox);
        configureSelectedDecision(comboBox);
    }

    private void configureDecisionOptions(ComboBox<CodelistEntry> comboBox) {
        comboBox.setCellFactory(listView -> {

            listView.setPrefWidth(comboBox.getWidth());

            comboBox.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (newWidth.doubleValue() > 0) {
                    listView.setPrefWidth(newWidth.doubleValue());
                }
            });

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
                        valueText.setText(formatCodelistEntry(item));

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
                    valueText.setText(formatCodelistEntry(item));

                    setText(null);
                    setGraphic(valueText);
                }
            }
        });
    }

    private String formatCodelistEntry(CodelistEntry entry) {
        return entry.value() + " (" + entry.code() + ")";
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

    @FXML
    protected void onKontaktdatenClick() {
        boolean saved;

        try {
            saved = CaseworkerDialog.show(
                    fileStatusLabel.getScene().getWindow(),
                    userCaseworkersFile(),
                    loadTelekommunikationsarten()
            );
        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Kontaktdaten können nicht bearbeitet werden",
                    "Die Kontaktdaten bleiben unverändert.",
                    exception,
                    List.of()
            );
            return;
        }

        if (!saved) {
            return;
        }

        try {
            loadCaseworkers();
        } catch (Exception exception) {
            caseworkersFile = null;
            showCaseworkers(List.of());

            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Kontaktdaten konnten nicht geladen werden",
                    "Die Auswahl der Sachbearbeiter*innen bleibt leer.",
                    exception,
                    List.of()
            );
        }
    }

    private void loadCaseworkers() throws AppException {
        Path installationFile = ApplicationPaths.getApplicationDirectory()
                .resolve(CaseworkerConfigLoader.CONFIG_FILE);

        boolean installationFileExists = Files.exists(installationFile);

        kontaktdatenButton.setVisible(!installationFileExists);
        kontaktdatenButton.setManaged(!installationFileExists);

        List<CaseworkerEntry> caseworkerEntries = List.of();
        Path loadedFile = null;

        if (installationFileExists) {
            caseworkerEntries = caseworkerConfigLoader.load(installationFile);
            loadedFile = installationFile;

        } else {
            Path userFile = userCaseworkersFile();

            if (userFile != null && Files.exists(userFile)) {
                try {
                    caseworkerEntries = caseworkerConfigLoader.load(userFile);
                    loadedFile = userFile;
                } catch (AppException exception) {
                    backUpInvalidUserFile(userFile, exception);
                }

                if (caseworkerEntries.size() > 1) {
                    caseworkerEntries = List.of();
                    loadedFile = null;
                    backUpUserFileWithSeveralEntries(userFile);
                }
            }
        }

        caseworkersFile = loadedFile;
        caseworkersFromInstallationDirectory = installationFileExists;

        showCaseworkers(caseworkerEntries);
    }

    private Path userCaseworkersFile() throws AppException {
        Path userDirectory = ApplicationPaths.getUserDirectory();

        return userDirectory == null
                ? null
                : userDirectory.resolve(CaseworkerConfigLoader.CONFIG_FILE);
    }

    private void backUpInvalidUserFile(Path userFile, AppException problem) {
        List<ErrorDetail> details = new ArrayList<>();
        Path backupFile = renameToInvalid(userFile);

        if (backupFile != null) {
            details.add(new ErrorDetail("Gesicherte Datei", backupFile.toString()));
        } else {
            details.add(new ErrorDetail(
                    "Sicherung fehlgeschlagen",
                    "Die Datei wurde nicht umbenannt und wird beim nächsten Speichern der Kontaktdaten überschrieben."
            ));
        }

        ProblemDialog.show(
                Alert.AlertType.WARNING,
                "Persönliche Kontaktdaten konnten nicht geladen werden",
                "Es sind keine Kontaktdaten vorhanden.",
                problem,
                details
        );
    }

    private void backUpUserFileWithSeveralEntries(Path userFile) {
        Path backupFile = renameToInvalid(userFile);

        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setTitle("Persönliche Kontaktdaten ungültig");
        warning.setHeaderText(null);

        Label message = new Label(backupFile != null
                ? "Die persönlichen Kontaktdaten enthalten mehr als einen Eintrag. "
                + "Die Datei wurde in „" + backupFile.getFileName() + "“ umbenannt. "
                + "Legen Sie Ihre Kontaktdaten über „Kontaktdaten“ neu an."
                : "Die persönlichen Kontaktdaten enthalten mehr als einen Eintrag. "
                + "Die Datei konnte nicht umbenannt werden und wird beim nächsten Speichern "
                + "der Kontaktdaten überschrieben. Legen Sie Ihre Kontaktdaten über „Kontaktdaten“ neu an.");
        message.setWrapText(true);

        warning.getDialogPane().setContent(message);
        warning.getDialogPane().setPrefWidth(600);
        warning.showAndWait();
    }

    private Path renameToInvalid(Path userFile) {
        Path backupFile = userFile.resolveSibling(CaseworkerConfigLoader.CONFIG_FILE + ".ungueltig");

        try {
            Files.move(userFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            return backupFile;
        } catch (IOException e) {
            return null;
        }
    }

    private void showCaseworkers(List<CaseworkerEntry> caseworkerEntries) {
        CaseworkerEntry previousSelection = caseworkerComboBox.getValue();

        double maxTextWidth = 0;

        for (CaseworkerEntry caseworkerEntry : caseworkerEntries) {
            Text text = new Text(caseworkerEntry.name());
            double textWidth = text.getLayoutBounds().getWidth();

            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
        }

        caseworkerComboBox.setPrefWidth(maxTextWidth + CASEWORKER_WIDTH_PADDING);
        caseworkerComboBox.getItems().setAll(caseworkerEntries);
        caseworkerComboBox.setValue(
                previousSelection != null && caseworkerEntries.contains(previousSelection)
                        ? previousSelection
                        : null
        );

        caseworkerHint.setVisible(caseworkerEntries.isEmpty());
        caseworkerHint.setManaged(caseworkerEntries.isEmpty());

        selectPersonalContact();
    }

    private boolean isPersonalContactLoaded() {
        return caseworkersFile != null && !caseworkersFromInstallationDirectory;
    }

    private void selectPersonalContact() {
        if (isPersonalContactLoaded() && caseworkerComboBox.getItems().size() == 1) {
            caseworkerComboBox.setValue(caseworkerComboBox.getItems().getFirst());
        }
    }

    private List<CodelistEntry> loadTelekommunikationsarten() throws AppException {
        CodelistConfig config = new CodelistConfig();
        CodelistDefinition definition = config.get("telekommunikationsart");
        Path codelistFile = resolveCodelistFile(definition);

        return new GenericodeReader().readAll(codelistFile, definition);
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
        addRequestComboBox.getItems().setAll(entries.stream()
                .filter(entry -> allowedDecisionsByRequestCode.containsKey(entry.code()))
                .toList());
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
        for (DecisionRow row : decisionRows) {
            row.decisionComboBox().setValue(null);
        }
        addRequestComboBox.setValue(null);
        selectPersonalContact();
    }

    private void clearDecisionUi() {
        multipleDecisionGrid.getChildren().clear();

        multipleDecisionBox.setVisible(false);
        multipleDecisionBox.setManaged(false);

        decisionRows.clear();
        decisionMissing.set(true);
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
        addRequestComboBox.disableProperty().bind(openedDocument.isNull());
        addRequestButton.disableProperty().bind(openedDocument.isNull().or(addRequestComboBox.valueProperty().isNull()));
    }

    private record DecisionRow(String requestCode, ComboBox<CodelistEntry> decisionComboBox) {
    }

    private void openHtmlInBrowser(
            Path htmlPath,
            String workflowHint,
            List<ErrorDetail> successfulResults
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                browserOpener.open(htmlPath);
            } catch (Exception exception) {
                Platform.runLater(() -> ProblemDialog.show(
                        Alert.AlertType.WARNING,
                        "Browser konnte nicht geöffnet werden",
                        workflowHint,
                        exception,
                        successfulResults
                ));
            }
        });
    }
}
