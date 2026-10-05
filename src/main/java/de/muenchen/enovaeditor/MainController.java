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
import de.muenchen.enovaeditor.template.HtmlWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private void initialize() {
        setupBindings();

        configureCaseworkerComboBox();
        configureFileNumberField();

        loadSenderName();
        loadCaseworkers();
        loadDecisions();
        loadDecisionMappings();
        loadRequestLabels();
    }

    @FXML
    protected void onXmlOpenClick() {

        File selectedFile = chooseXmlFile();

        if (selectedFile == null) {
            return;
        }

        Path selectedXmlPath = selectedFile.toPath();

        openedDocument.set(null);
        openedXmlPath = null;

        try {
            Document document = xmlLoader.load(selectedFile);

            checker.check(document);

            List<Node> requests = ersuchenSachentscheidungReader.read(document);

            String inputTemplate = templateLoader.loadInputTemplate();

            String renderedHtml = templateRenderer.render(inputTemplate, document);

            Path outputHtmlPath = OutputPathUtil.createOutputPath(selectedXmlPath, ".htm");

            Path outputHtml = htmlWriter.write(renderedHtml, outputHtmlPath);

            openHtmlInBrowser(outputHtml);

            createDecisionFields(requests);

            fileStatusLabel.setText(selectedFile.getName());
            clearFormFields();
            openedDocument.set(document);
            openedXmlPath = selectedXmlPath;

        } catch (Exception e) {
            openedDocument.set(null);
            openedXmlPath = null;
            fileStatusLabel.setText("");

            clearFormFields();
            clearDecisionUi();

            showError("Datei kann nicht verarbeitet werden", e.getMessage());
        }
    }

    @FXML
    protected void onGenerateAnswer() {
        try {
            Document inputDocument = openedDocument.get();

            ManufacturerInfo manufacturerInfo = manufacturerInfoLoader.load();

            CaseworkerEntry selectedCaseworker = caseworkerComboBox.getValue();

            List<String> decisionCodes = decisionComboBoxes.stream().map(ComboBox::getValue).map(CodelistEntry::code).toList();

            AnswerParameters parameters = new AnswerParameters(fileNumber.getText(), UUID.randomUUID().toString(), manufacturerInfo, selectedCaseworker.name(), decisionCodes);

            Document answerDocument = answerTransformer.transform(inputDocument, parameters);
            OutputPathUtil.OutputPaths outputPaths = OutputPathUtil.createOutputPaths(openedXmlPath, "Output");
            xmlWriter.write(answerDocument, outputPaths.xmlPath().toFile());

            String outputTemplate = templateLoader.loadOutputTemplate();

            String renderedOutputHtml = templateRenderer.render(outputTemplate, answerDocument);

            Path outputHtml = htmlWriter.write(renderedOutputHtml, outputPaths.htmlPath());

            openHtmlInBrowser(outputHtml);

        } catch (Exception e) {
            showError("Antwort konnte nicht erzeugt werden", e.getMessage());
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
    }

    private void createDecisionFields(List<Node> requests) throws XPathExpressionException {

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

    private Label createDecisionLabel(String requestCode, int row, boolean multipleRequests) {

        String requestLabel = requestLabelsByCode.get(requestCode);

        if (requestLabel == null) {
            throw new IllegalArgumentException("Kein Label für Request-Code '" + requestCode + "' gefunden.");
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

    private List<CodelistEntry> getAllowedDecisionEntries(String requestCode) {

        List<String> allowedDecisionCodes = allowedDecisionsByRequestCode.get(requestCode);

        return decisionEntries.stream().filter(entry -> allowedDecisionCodes.contains(entry.code())).toList();
    }

    private List<String> readRequestCodes(List<Node> requests) throws XPathExpressionException {

        List<String> requestCodes = new ArrayList<>();

        for (Node request : requests) {
            String requestCode = ersuchenSachentscheidungReader.readRequestCode(request);

            if (!allowedDecisionsByRequestCode.containsKey(requestCode)) {
                throw new IllegalArgumentException("Kein Decision-Mapping für Request-Code '" + requestCode + "' gefunden.");
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

        } catch (IOException e) {
            showError("Sachbearbeiter*in-Konfiguration konnte nicht geladen werden", e.getMessage());
        }
    }

    private void loadDecisions() {
        try {
            List<CodelistEntry> entries = loadCodelistEntries("sachentscheidung");

            decisionEntries.clear();
            decisionEntries.addAll(entries);

        } catch (Exception e) {
            showError("Sachentscheidung-Codelist konnte nicht geladen werden", e.getMessage());
        }
    }

    private void loadRequestLabels() {
        try {
            requestLabelsByCode = loadCodelistEntries("ersuchenSachentscheidung").stream().collect(Collectors.toMap(CodelistEntry::code, CodelistEntry::value));

        } catch (Exception e) {
            showError("Ersuchen-Sachentscheidung-Codelist konnte nicht geladen werden", e.getMessage());
        }
    }

    private List<CodelistEntry> loadCodelistEntries(String configKey) throws Exception {
        CodelistConfig config = new CodelistConfig();

        CodelistDefinition definition = config.get(configKey);

        GenericodeReader genericodeReader = new GenericodeReader();

        Path codelistFile = ApplicationPaths.getApplicationDirectory().resolve("codelists").resolve(definition.file());

        return genericodeReader.readAll(codelistFile, definition);
    }

    private void loadDecisionMappings() {
        try {
            allowedDecisionsByRequestCode = decisionConfigLoader.load();
        } catch (IOException e) {
            showError("Entscheidungskonfiguration konnte nicht geladen werden", e.getMessage());
        }
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

    private void openHtmlInBrowser(Path htmlPath) {
        try {
            browserOpener.open(htmlPath);
        } catch (IOException e) {
            showWarning("HTML-Datei wurde erstellt", "Die HTML-Datei wurde erfolgreich erstellt, " + "konnte aber nicht automatisch im Browser geöffnet werden.\n\n" + htmlPath);
        }
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
