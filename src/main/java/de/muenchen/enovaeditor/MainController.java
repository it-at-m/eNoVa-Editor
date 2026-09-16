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
import de.muenchen.enovaeditor.template.HtmlOutputWriter;
import de.muenchen.enovaeditor.template.TemplateLoader;
import de.muenchen.enovaeditor.template.XPathTemplateRenderer;
import de.muenchen.enovaeditor.util.OutputPathUtil;
import de.muenchen.enovaeditor.xml.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class MainController {

    private static final double DECISION_TEXT_WRAP_WIDTH = 500;
    private static final double FILE_NUMBER_MIN_WIDTH = 150;
    private static final double FILE_NUMBER_PADDING = 30;
    private static final double CASEWORKER_WIDTH_PADDING = 50;

    private final BrowserOpener browserOpener = new BrowserOpener();
    private final XmlLoader xmlLoader = new XmlLoader();
    private final ErsuchenSachentscheidungChecker checker = new ErsuchenSachentscheidungChecker();
    private final TemplateLoader templateLoader = new TemplateLoader();
    private final XPathTemplateRenderer templateRenderer = new XPathTemplateRenderer();
    private final HtmlOutputWriter htmlOutputWriter = new HtmlOutputWriter();
    private final AnswerTransformer answerTransformer = new AnswerTransformer();
    private final XmlWriter xmlWriter = new XmlWriter();
    private final CaseworkerConfigLoader caseworkerConfigLoader = new CaseworkerConfigLoader();
    private final SenderConfigLoader senderConfigLoader = new SenderConfigLoader();

    private final ObjectProperty<Document> openedDocument = new SimpleObjectProperty<>(null);
    private Path openedXmlPath;

    private String senderName;

    @FXML
    private Label fileStatusLabel;

    @FXML
    private Label senderNameLabel;

    @FXML
    private ComboBox<CaseworkerEntry> caseworkerComboBox;

    @FXML
    private ComboBox<CodelistEntry> decisionComboBox;

    @FXML
    private TextField fileNumber;

    @FXML
    private Button generateAnswer;

    @FXML
    private void initialize() {
        setupBindings();

        configureCaseworkerComboBox();
        configureDecisionComboBox();
        configureFileNumberField();

        loadSenderName();
        loadCaseworkers();
        loadDecisions();
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

            String inputTemplate = templateLoader.loadInputTemplate();

            String renderedHtml = templateRenderer.render(inputTemplate, document);

            Path outputHtml = htmlOutputWriter.write(renderedHtml, selectedXmlPath);

            fileStatusLabel.setText(selectedFile.getName());
            clearDecisionFields();
            openedDocument.set(document);
            openedXmlPath = selectedXmlPath;
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
            openedDocument.set(null);
            openedXmlPath = null;
            fileStatusLabel.setText("");
            clearDecisionFields();
            showError("Datei kann nicht verarbeitet werden", e.getMessage());
        }
    }

    @FXML
    protected void onGenerateAnswer() {
        try {
            Document inputDocument = openedDocument.get();

            AnswerParameters parameters = new AnswerParameters(
                    fileNumber.getText(),
                    UUID.randomUUID().toString()
            );

            Document answerDocument = answerTransformer.transform(inputDocument, parameters);
            Path outputXmlPath = OutputPathUtil.createOutputPath(openedXmlPath, "Output", ".xml");
            xmlWriter.write(answerDocument, outputXmlPath.toFile());

            showSuccess(
                    "Antwort wurde erzeugt",
                    "Die Antwort wurde erfolgreich erstellt:\n\n"
                            + outputXmlPath
            );
        } catch (IOException | TransformerException e) {
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

                    setStyle(
                            "-fx-border-color: transparent transparent #d0d0d0 transparent;"
                                    + "-fx-border-width: 0 0 1 0;"
                    );
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

        } catch (IOException e) {
            showError("Sachbearbeiter-Konfiguration konnte nicht geladen werden", e.getMessage());
        }
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
        caseworkerComboBox.setValue(null);
        decisionComboBox.setValue(null);
    }

    private void setupBindings() {
        setupGenerateAnswerButtonBinding();
        setupDecisionFieldsDisabledBinding();
    }

    private void setupGenerateAnswerButtonBinding() {
        BooleanBinding fileNumberMissing = Bindings.createBooleanBinding(
                () -> fileNumber.getText().isBlank(),
                fileNumber.textProperty()
        );

        BooleanBinding caseworkerMissing = caseworkerComboBox
                .getSelectionModel()
                .selectedItemProperty()
                .isNull();

        BooleanBinding decisionMissing = decisionComboBox
                .getSelectionModel()
                .selectedItemProperty()
                .isNull();

        generateAnswer.disableProperty().bind(fileNumberMissing.or(caseworkerMissing).or(decisionMissing).or(openedDocument.isNull()));
    }

    private void setupDecisionFieldsDisabledBinding() {
        fileNumber.disableProperty().bind(openedDocument.isNull());
        caseworkerComboBox.disableProperty().bind(openedDocument.isNull());
        decisionComboBox.disableProperty().bind(openedDocument.isNull());
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

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
