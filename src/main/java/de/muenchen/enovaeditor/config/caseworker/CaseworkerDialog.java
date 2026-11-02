package de.muenchen.enovaeditor.config.caseworker;

import de.muenchen.enovaeditor.codelist.CodelistEntry;
import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.error.ProblemDialog;
import de.muenchen.enovaeditor.xml.XPathReader;
import de.muenchen.enovaeditor.xml.XmlLoader;
import de.muenchen.enovaeditor.xml.XmlWriter;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CaseworkerDialog {

    private static final String XJUSTIZ_NAMESPACE = "http://www.xjustiz.de";
    private static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

    private static final List<String> ALLOWED_TELEKOMMUNIKATIONSARTEN = List.of("001", "002", "003", "004", "007");

    // Reihenfolgen gemäß XJustiz 3.6.2 (xjustiz_0000_grunddatensatz_3_6.xsd)
    private static final List<String> NATUERLICHE_PERSON_ORDER = List.of(
            "vollerName", "geburt", "tod", "geschlecht", "familienstand", "anschrift",
            "staatsangehoerigkeit", "personalstatut", "beruf", "telekommunikation", "bankverbindung",
            "auswahl_auskunftssperre", "herkunftsland", "sprache", "zustaendigeInstitution",
            "aliasNatuerlichePerson", "steueridentifikationsnummer", "umsatzsteuerID",
            "vorsteuerabzugsberechtigt", "bundeseinheitlicheWirtschaftsnummer",
            "registereintragungNatuerlichePerson", "ausweisdokument"
    );
    private static final List<String> VOLLER_NAME_ORDER = List.of(
            "vorname", "rufname", "titel", "namensvorsatz", "nachname", "geburtsname", "vorname.alt",
            "nachname.alt", "namenszusatz", "geburtsnamensvorsatz", "weitererName"
    );
    private static final List<String> ANSCHRIFT_ORDER = List.of(
            "anschriftstyp", "anschriftenzusatz", "strasse", "hausnummer", "postfachnummer",
            "postleitzahl", "ort", "ortsteil", "auswahl_bundesland", "wohnungsgeber",
            "ehemaligeAnschrift", "derzeitigerAufenthalt", "staat", "erfassungsdatum", "ort.unbekannt",
            "postleitzahl.unbekannt"
    );
    private static final List<String> TELEKOMMUNIKATION_ORDER = List.of(
            "telekommunikationsart", "telekommunikationszusatz", "verbindung"
    );

    private final Document document;
    private final Path userFile;
    private final List<CodelistEntry> telekommunikationsarten;

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final ListView<Element> entryList = new ListView<>();
    private final VBox telekommunikationRows = new VBox(10);

    private final TextField nameField = new TextField();
    private final TextField vornameField = new TextField();
    private final TextField titelField = new TextField();
    private final TextField namensvorsatzField = new TextField();
    private final TextField nachnameField = new TextField();
    private final TextField namenszusatzField = new TextField();
    private final TextField anschriftenzusatzField = new TextField();
    private final TextField strasseField = new TextField();
    private final TextField hausnummerField = new TextField();
    private final TextField postleitzahlField = new TextField();
    private final TextField ortField = new TextField();

    private boolean dirty;
    private boolean fillingForm;

    private CaseworkerDialog(
            Document document,
            Path userFile,
            List<CodelistEntry> telekommunikationsarten
    ) {
        this.document = document;
        this.userFile = userFile;
        this.telekommunikationsarten = telekommunikationsarten.stream()
                .filter(entry -> ALLOWED_TELEKOMMUNIKATIONSARTEN.contains(entry.code()))
                .toList();
    }

    public static boolean show(
            Window owner,
            Path sourceFile,
            boolean fromUserDirectory,
            Path userFile,
            List<CodelistEntry> telekommunikationsarten
    ) throws AppException {

        Document document = sourceFile == null
                ? createEmptyDocument()
                : new XmlLoader().load(sourceFile.toFile());

        CaseworkerDialog caseworkerDialog = new CaseworkerDialog(document, userFile, telekommunikationsarten);

        return caseworkerDialog.showAndWait(owner, sourceFile != null && !fromUserDirectory);
    }

    private boolean showAndWait(Window owner, boolean fromInstallationDirectory) {
        dialog.initOwner(owner);
        dialog.setTitle("Kontaktdaten");
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(saveButtonType, cancelButtonType);

        dialog.getDialogPane().setContent(createContent(fromInstallationDirectory));
        dialog.getDialogPane().setPrefSize(900, 650);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!save()) {
                event.consume();
            }
        });

        dialog.setOnCloseRequest(event -> {
            if (dialog.getResult() != saveButtonType && dirty && !confirmDiscard()) {
                event.consume();
            }
        });

        entryList.getItems().setAll(entries());

        if (!entryList.getItems().isEmpty()) {
            entryList.getSelectionModel().selectFirst();
        }

        return dialog.showAndWait().orElse(cancelButtonType) == saveButtonType;
    }

    private Node createContent(boolean fromInstallationDirectory) {
        entryList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Element entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null : displayName(entry));
            }
        });
        entryList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldEntry, newEntry) -> fillForm(newEntry));

        Button newButton = new Button("Neu");
        newButton.setOnAction(event -> addEntry());

        Button deleteButton = new Button("Löschen");
        deleteButton.setOnAction(event -> deleteSelectedEntry());
        deleteButton.disableProperty().bind(entryList.getSelectionModel().selectedItemProperty().isNull());

        VBox listBox = new VBox(10, entryList, new HBox(10, newButton, deleteButton));
        VBox.setVgrow(entryList, Priority.ALWAYS);
        listBox.setPrefWidth(220);
        listBox.setMinWidth(180);

        VBox form = new VBox(15, createNameGrid(), createGroup("Anschrift", createAnschriftGrid()),
                createGroup("Telekommunikation", createTelekommunikationBox()));
        form.disableProperty().bind(entryList.getSelectionModel().selectedItemProperty().isNull());

        ScrollPane formScrollPane = new ScrollPane(form);
        formScrollPane.setFitToWidth(true);
        formScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        HBox main = new HBox(15, listBox, formScrollPane);
        HBox.setHgrow(formScrollPane, Priority.ALWAYS);
        VBox.setVgrow(main, Priority.ALWAYS);

        Label requiredHint = new Label("* Pflichtfeld");
        HBox requiredHintBox = new HBox(requiredHint);
        requiredHintBox.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(15);

        if (fromInstallationDirectory) {
            Label sourceHint = new Label(
                    "Die Einträge stammen aus dem Installationsverzeichnis. "
                            + "Beim Speichern wird eine persönliche Kopie angelegt."
            );
            sourceHint.setWrapText(true);
            content.getChildren().add(sourceHint);
        }

        content.getChildren().addAll(main, requiredHintBox);

        return content;
    }

    private GridPane createNameGrid() {
        GridPane grid = createFormGrid();

        addRow(grid, 0, "Anzeigename*:", nameField);
        addRow(grid, 1, "Vorname*:", vornameField);
        addRow(grid, 2, "Titel:", titelField);
        addRow(grid, 3, "Namensvorsatz:", namensvorsatzField);
        addRow(grid, 4, "Nachname*:", nachnameField);
        addRow(grid, 5, "Namenszusatz:", namenszusatzField);

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isEditing()) {
                selectedEntry().setAttribute("name", newValue.trim());
                entryList.refresh();
                dirty = true;
            }
        });

        bindNameField(vornameField, "vorname");
        bindNameField(titelField, "titel");
        bindNameField(namensvorsatzField, "namensvorsatz");
        bindNameField(nachnameField, "nachname");
        bindNameField(namenszusatzField, "namenszusatz");

        return grid;
    }

    private GridPane createAnschriftGrid() {
        GridPane grid = createFormGrid();

        addRow(grid, 0, "Anschriftenzusatz:", anschriftenzusatzField);
        addRow(grid, 1, "Straße:", strasseField);
        addRow(grid, 2, "Hausnummer:", hausnummerField);
        addRow(grid, 3, "PLZ:", postleitzahlField);
        addRow(grid, 4, "Ort:", ortField);

        bindAnschriftField(anschriftenzusatzField, "anschriftenzusatz");
        bindAnschriftField(strasseField, "strasse");
        bindAnschriftField(hausnummerField, "hausnummer");
        bindAnschriftField(postleitzahlField, "postleitzahl");
        bindAnschriftField(ortField, "ort");

        return grid;
    }

    private VBox createTelekommunikationBox() {
        Button addButton = new Button("Hinzufügen");
        addButton.setOnAction(event -> addTelekommunikation());

        return new VBox(10, telekommunikationRows, addButton);
    }

    private static GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        return grid;
    }

    private static void addRow(GridPane grid, int row, String labelText, TextField field) {
        grid.add(new Label(labelText), 0, row);
        grid.add(field, 1, row);
    }

    private static StackPane createGroup(String title, Node content) {
        VBox box = new VBox(10, content);
        box.setPadding(new Insets(20, 12, 12, 12));
        box.setStyle("-fx-border-color: #c8c8c8; -fx-border-radius: 4.0;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-background-color: -fx-background; -fx-padding: 0 6 0 6;");
        StackPane.setAlignment(titleLabel, Pos.TOP_LEFT);
        StackPane.setMargin(titleLabel, new Insets(-7, 0, 0, 10));

        StackPane group = new StackPane(box, titleLabel);
        group.setPadding(new Insets(10, 0, 0, 0));

        return group;
    }

    private void bindNameField(TextField field, String localName) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isEditing()) {
                setText(vollerName(selectedEntry()), localName, newValue, VOLLER_NAME_ORDER);
                dirty = true;
            }
        });
    }

    private void bindAnschriftField(TextField field, String localName) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isEditing()) {
                updateAnschrift(localName, newValue);
                dirty = true;
            }
        });
    }

    private boolean isEditing() {
        return !fillingForm && selectedEntry() != null;
    }

    private Element selectedEntry() {
        return entryList.getSelectionModel().getSelectedItem();
    }

    private void fillForm(Element entry) {
        fillingForm = true;

        try {
            if (entry == null) {
                for (TextField field : List.of(nameField, vornameField, titelField, namensvorsatzField,
                        nachnameField, namenszusatzField, anschriftenzusatzField, strasseField,
                        hausnummerField, postleitzahlField, ortField)) {
                    field.clear();
                }
                telekommunikationRows.getChildren().clear();
                return;
            }

            Element vollerName = vollerName(entry);
            Element anschrift = child(natuerlichePerson(entry), "anschrift");

            nameField.setText(entry.getAttribute("name"));
            vornameField.setText(text(vollerName, "vorname"));
            titelField.setText(text(vollerName, "titel"));
            namensvorsatzField.setText(text(vollerName, "namensvorsatz"));
            nachnameField.setText(text(vollerName, "nachname"));
            namenszusatzField.setText(text(vollerName, "namenszusatz"));
            anschriftenzusatzField.setText(text(anschrift, "anschriftenzusatz"));
            strasseField.setText(text(anschrift, "strasse"));
            hausnummerField.setText(text(anschrift, "hausnummer"));
            postleitzahlField.setText(text(anschrift, "postleitzahl"));
            ortField.setText(text(anschrift, "ort"));

            fillTelekommunikationRows(entry);
        } finally {
            fillingForm = false;
        }
    }

    private void fillTelekommunikationRows(Element entry) {
        telekommunikationRows.getChildren().clear();

        for (Element telekommunikation : children(natuerlichePerson(entry), "telekommunikation")) {
            telekommunikationRows.getChildren().add(createTelekommunikationRow(telekommunikation));
        }
    }

    private HBox createTelekommunikationRow(Element telekommunikation) {
        ComboBox<CodelistEntry> artComboBox = new ComboBox<>();
        artComboBox.getItems().setAll(telekommunikationsarten);
        artComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CodelistEntry entry) {
                return entry == null ? "" : entry.value();
            }

            @Override
            public CodelistEntry fromString(String value) {
                return null;
            }
        });

        String code = text(child(telekommunikation, "telekommunikationsart"), "code");

        for (CodelistEntry entry : telekommunikationsarten) {
            if (entry.code().equals(code)) {
                artComboBox.setValue(entry);
            }
        }

        TextField verbindungField = new TextField(text(telekommunikation, "verbindung"));
        HBox.setHgrow(verbindungField, Priority.ALWAYS);

        Button removeButton = new Button("Entfernen");

        artComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            Element art = child(telekommunikation, "telekommunikationsart");
            Element codeElement = child(art, "code");

            if (codeElement == null) {
                codeElement = document.createElementNS(null, "code");
                art.appendChild(codeElement);
            }

            codeElement.setTextContent(newValue == null ? "" : newValue.code());
            dirty = true;
        });

        verbindungField.textProperty().addListener((observable, oldValue, newValue) -> {
            setText(telekommunikation, "verbindung", newValue, TELEKOMMUNIKATION_ORDER);
            dirty = true;
        });

        HBox row = new HBox(10, artComboBox, verbindungField, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);

        removeButton.setOnAction(event -> {
            telekommunikation.getParentNode().removeChild(telekommunikation);
            telekommunikationRows.getChildren().remove(row);
            dirty = true;
        });

        return row;
    }

    private void addTelekommunikation() {
        Element entry = selectedEntry();

        if (entry == null) {
            return;
        }

        Element telekommunikation = createXJustizElement("telekommunikation");
        Element art = createXJustizElement("telekommunikationsart");
        art.setAttribute("listVersionID", "2.3");
        art.appendChild(document.createElementNS(null, "code"));
        telekommunikation.appendChild(art);

        insertOrdered(natuerlichePerson(entry), telekommunikation, NATUERLICHE_PERSON_ORDER);

        telekommunikationRows.getChildren().add(createTelekommunikationRow(telekommunikation));
        dirty = true;
    }

    private void updateAnschrift(String localName, String value) {
        Element natuerlichePerson = natuerlichePerson(selectedEntry());
        Element anschrift = child(natuerlichePerson, "anschrift");

        if (anschrift == null) {
            if (value.isBlank()) {
                return;
            }

            anschrift = createXJustizElement("anschrift");

            Element anschriftstyp = createXJustizElement("anschriftstyp");
            anschriftstyp.setAttribute("listVersionID", "3.0");
            Element code = document.createElementNS(null, "code");
            code.setTextContent("003");
            anschriftstyp.appendChild(code);
            anschrift.appendChild(anschriftstyp);

            insertOrdered(natuerlichePerson, anschrift, NATUERLICHE_PERSON_ORDER);
        }

        setText(anschrift, localName, value, ANSCHRIFT_ORDER);

        for (Element child : childElements(anschrift)) {
            if (!child.getLocalName().equals("anschriftstyp")) {
                return;
            }
        }

        natuerlichePerson.removeChild(anschrift);
    }

    private void addEntry() {
        Element entry = document.createElement("entry");
        entry.setAttribute("name", "");

        Element auswahlBeteiligter = createXJustizElement("auswahl_beteiligter");
        Element natuerlichePerson = createXJustizElement("natuerlichePerson");
        natuerlichePerson.appendChild(createXJustizElement("vollerName"));
        auswahlBeteiligter.appendChild(natuerlichePerson);
        entry.appendChild(auswahlBeteiligter);

        document.getDocumentElement().appendChild(entry);

        entryList.getItems().add(entry);
        entryList.getSelectionModel().select(entry);
        nameField.requestFocus();
        dirty = true;
    }

    private void deleteSelectedEntry() {
        Element entry = selectedEntry();

        if (entry == null) {
            return;
        }

        entry.getParentNode().removeChild(entry);
        entryList.getItems().remove(entry);
        dirty = true;
    }

    private boolean save() {
        for (Element entry : entryList.getItems()) {
            String problem = validate(entry);

            if (problem != null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.setTitle("Kontaktdaten unvollständig");
                alert.setHeaderText(null);
                alert.setContentText(problem);
                alert.showAndWait();

                entryList.getSelectionModel().select(entry);
                return false;
            }
        }

        try {
            if (entryList.getItems().isEmpty()) {
                if (!confirmDeleteAll()) {
                    return false;
                }

                if (userFile != null) {
                    deleteUserFile();
                }

                return true;
            }

            writeUserFile();
            return true;

        } catch (AppException exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Kontaktdaten konnten nicht gespeichert werden",
                    "Die Kontaktdaten wurden nicht gespeichert.",
                    exception,
                    List.of()
            );
            return false;
        }
    }

    private String validate(Element entry) {
        String name = entry.getAttribute("name").trim();

        if (name.isBlank()) {
            return "Bei einem Eintrag fehlt der Anzeigename.";
        }

        String label = "Beim Eintrag „" + name + "“";

        for (Element other : entryList.getItems()) {
            if (other != entry && other.getAttribute("name").trim().equals(name)) {
                return "Der Anzeigename „" + name + "“ ist mehrfach vorhanden.";
            }
        }

        Element vollerName = vollerName(entry);

        if (text(vollerName, "vorname").isBlank()) {
            return label + " fehlt der Vorname.";
        }

        if (text(vollerName, "nachname").isBlank()) {
            return label + " fehlt der Nachname.";
        }

        for (Element telekommunikation : children(natuerlichePerson(entry), "telekommunikation")) {
            String code = text(child(telekommunikation, "telekommunikationsart"), "code");

            if (!ALLOWED_TELEKOMMUNIKATIONSARTEN.contains(code)) {
                return label + " ist bei einer Telekommunikation keine Art ausgewählt.";
            }

            if (text(telekommunikation, "verbindung").isBlank()) {
                return label + " fehlt bei einer Telekommunikation die Verbindung.";
            }
        }

        return null;
    }

    private void writeUserFile() throws AppException {
        if (userFile == null) {
            throw new AppException(
                    "Das Benutzerverzeichnis konnte nicht ermittelt werden. "
                            + "Die Kontaktdaten können daher nicht gespeichert werden.",
                    List.of(new ErrorDetail("Umgebungsvariable", "APPDATA"))
            );
        }

        try {
            Files.createDirectories(userFile.getParent());
        } catch (IOException e) {
            throw new AppException(
                    "Das Verzeichnis für die Kontaktdaten konnte nicht angelegt werden.",
                    List.of(new ErrorDetail("Verzeichnis", userFile.getParent().toString())),
                    e
            );
        }

        removeWhitespaceTextNodes();

        new XmlWriter().write(document, userFile.toFile(), true);
    }

    private void deleteUserFile() throws AppException {
        try {
            Files.deleteIfExists(userFile);
        } catch (IOException e) {
            throw new AppException(
                    "Die persönlichen Kontaktdaten konnten nicht gelöscht werden.",
                    List.of(new ErrorDetail("Datei", userFile.toString())),
                    e
            );
        }
    }

    // XmlWriter rückt ein und behält vorhandene Leerzeichen; sonst wächst die Datei bei jedem Speichern.
    private void removeWhitespaceTextNodes() throws AppException {
        document.normalize();

        try {
            for (org.w3c.dom.Node node : new XPathReader().findNodes(document, "//text()[normalize-space()='']")) {
                node.getParentNode().removeChild(node);
            }
        } catch (XPathExpressionException e) {
            throw new AppException(
                    "Die Kontaktdaten konnten nicht für das Speichern aufbereitet werden.",
                    List.of(new ErrorDetail("Datei", userFile.toString())),
                    e
            );
        }
    }

    private boolean confirmDiscard() {
        return confirm(
                "Die Änderungen wurden nicht gespeichert. Möchten Sie sie verwerfen?",
                "Verwerfen",
                "Zurück"
        );
    }

    private boolean confirmDeleteAll() {
        return confirm(
                "Alle persönlichen Kontaktdaten werden gelöscht. "
                        + "Danach gelten die Kontaktdaten aus dem Installationsverzeichnis.",
                "Löschen",
                "Zurück"
        );
    }

    private boolean confirm(String message, String confirmText, String cancelText) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(dialog.getDialogPane().getScene().getWindow());
        confirmation.setTitle("Kontaktdaten");
        confirmation.setHeaderText(null);
        confirmation.setGraphic(null);
        confirmation.setContentText(message);

        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(confirmButton, cancelButton);

        return confirmation.showAndWait().orElse(cancelButton) == confirmButton;
    }

    private List<Element> entries() {
        List<Element> entries = new ArrayList<>();

        for (Element child : childElements(document.getDocumentElement())) {
            if (child.getTagName().equals("entry")) {
                entries.add(child);
            }
        }

        return entries;
    }

    private static String displayName(Element entry) {
        String name = entry.getAttribute("name").trim();
        return name.isBlank() ? "(ohne Anzeigename)" : name;
    }

    private Element natuerlichePerson(Element entry) {
        return child(child(entry, "auswahl_beteiligter"), "natuerlichePerson");
    }

    private Element vollerName(Element entry) {
        return child(natuerlichePerson(entry), "vollerName");
    }

    private Element createXJustizElement(String localName) {
        return document.createElementNS(XJUSTIZ_NAMESPACE, "tns:" + localName);
    }

    private void setText(Element parent, String localName, String value, List<String> order) {
        String trimmedValue = value.trim();
        Element element = child(parent, localName);

        if (trimmedValue.isBlank()) {
            if (element != null) {
                parent.removeChild(element);
            }
            return;
        }

        if (element == null) {
            element = createXJustizElement(localName);
            insertOrdered(parent, element, order);
        }

        element.setTextContent(trimmedValue);
    }

    private static void insertOrdered(Element parent, Element element, List<String> order) {
        int position = order.indexOf(element.getLocalName());

        for (Element sibling : childElements(parent)) {
            if (order.indexOf(sibling.getLocalName()) > position) {
                parent.insertBefore(element, sibling);
                return;
            }
        }

        parent.appendChild(element);
    }

    private static String text(Element parent, String localName) {
        Element element = child(parent, localName);
        return element == null ? "" : element.getTextContent().trim();
    }

    private static Element child(Element parent, String localName) {
        List<Element> matches = children(parent, localName);
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> matches = new ArrayList<>();

        if (parent == null) {
            return matches;
        }

        for (Element child : childElements(parent)) {
            String childName = child.getLocalName() != null ? child.getLocalName() : child.getTagName();

            if (childName.equals(localName)) {
                matches.add(child);
            }
        }

        return matches;
    }

    private static List<Element> childElements(Element parent) {
        List<Element> elements = new ArrayList<>();

        for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                elements.add(element);
            }
        }

        return elements;
    }

    private static Document createEmptyDocument() throws AppException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement("caseworkers");
            root.setAttributeNS(XMLNS_NAMESPACE, "xmlns:tns", XJUSTIZ_NAMESPACE);
            document.appendChild(root);

            return document;
        } catch (ParserConfigurationException e) {
            throw new AppException(
                    "Die Kontaktdaten konnten nicht angelegt werden.",
                    List.of(),
                    e
            );
        }
    }
}
