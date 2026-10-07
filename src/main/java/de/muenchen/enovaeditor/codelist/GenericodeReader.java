package de.muenchen.enovaeditor.codelist;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GenericodeReader {

    private final XmlLoader xmlLoader = new XmlLoader();

    public String resolve(Path file, CodelistDefinition definition, String keyValue) throws Exception {

        Path readableFile = ApplicationFileUtil.requireReadableFile(file);

        Document document = xmlLoader.load(readableFile.toFile());

        NodeList rows = document.getElementsByTagName(definition.rowElement());

        for (int i = 0; i < rows.getLength(); i++) {

            Element row = (Element) rows.item(i);

            String currentKey = findValue(row, definition.keyColumn(), definition);

            if (keyValue.equals(currentKey)) {

                return getRequiredValue(readableFile, row, definition.valueColumn(), definition);
            }
        }

        throw new IllegalArgumentException("Der Wert \"" + keyValue + "\" wurde in der Codelist-Datei \"" + file.getFileName() + "\" nicht gefunden.");
    }

    public List<CodelistEntry> readAll(Path file, CodelistDefinition definition) throws Exception {

        Path readableFile = ApplicationFileUtil.requireReadableFile(file);

        List<CodelistEntry> entries = new ArrayList<>();

        Document document = xmlLoader.load(readableFile.toFile());

        NodeList rows = document.getElementsByTagName(definition.rowElement());

        for (int i = 0; i < rows.getLength(); i++) {

            Element row = (Element) rows.item(i);

            String currentKey = getRequiredValue(readableFile, row, definition.keyColumn(), definition);

            String currentValue = getRequiredValue(readableFile, row, definition.valueColumn(), definition);

            entries.add(new CodelistEntry(currentKey, currentValue));
        }

        return entries;
    }

    private String findValue(Element row, String column, CodelistDefinition definition) {

        NodeList values = row.getElementsByTagName(definition.valueElement());

        for (int i = 0; i < values.getLength(); i++) {

            Element value = (Element) values.item(i);

            String columnRef = value.getAttribute(definition.columnAttribute());

            if (!column.equals(columnRef)) {
                continue;
            }

            NodeList simpleValues = value.getElementsByTagName(definition.simpleValueElement());

            if (simpleValues.getLength() == 0) {
                return "";
            }

            Node simpleValue = simpleValues.item(0);

            return simpleValue.getTextContent().trim();
        }

        return null;
    }

    private String getRequiredValue(Path file, Element row, String column, CodelistDefinition definition) {

        String value = findValue(row, column, definition);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("In der Codelist-Datei \"" + file.getFileName() + "\" fehlt ein Wert für die " + "konfigurierte Spalte \"" + column + "\".");
        }

        return value;
    }
}