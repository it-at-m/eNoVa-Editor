package de.muenchen.enovaeditor.codelist;

import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;

public class GenericodeReader {

    private final XmlLoader xmlLoader = new XmlLoader();

    public String resolve(Path file, CodelistDefinition definition, String keyValue) throws Exception {

        Document document = xmlLoader.load(file.toFile());

        NodeList rows = document.getElementsByTagName(definition.rowElement());

        for (int i = 0; i < rows.getLength(); i++) {

            Element row = (Element) rows.item(i);

            String currentKey = findValue(row, definition.keyColumn(), definition);

            if (keyValue.equals(currentKey)) {

                String result = findValue(row, definition.valueColumn(), definition);

                if (result == null) {
                    throw new IllegalArgumentException("Ausgabewert für '" + keyValue + "' wurde nicht gefunden.");
                }

                return result;
            }
        }

        throw new IllegalArgumentException("Wert '" + keyValue + "' wurde in der Codeliste nicht gefunden.");
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
}