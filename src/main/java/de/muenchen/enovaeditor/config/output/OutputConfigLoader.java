package de.muenchen.enovaeditor.config.output;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class OutputConfigLoader {

    private static final String CONFIG_FILE = "xml-output-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public LineEnding loadLineEnding() throws IOException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        Document document;

        try {
            document = xmlLoader.load(configFile.toFile());

        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Die Konfigurationsdatei \"" + CONFIG_FILE + "\" konnte nicht gelesen werden.", e);
        }

        Node lineEndingNode = document.getDocumentElement().getElementsByTagName("lineEnding").item(0);

        if (lineEndingNode == null) {
            throw new IOException("In \"" + CONFIG_FILE + "\" fehlt das Element <lineEnding>.");
        }

        String value = lineEndingNode.getTextContent();

        if (value == null || value.isBlank()) {
            throw new IOException("In \"" + CONFIG_FILE + "\" ist das Element <lineEnding> leer.");
        }

        String normalizedValue = value.trim();

        try {
            return LineEnding.valueOf(normalizedValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IOException("In \"" + CONFIG_FILE + "\" enthält <lineEnding> den ungültigen Wert \"" + normalizedValue + "\". Erlaubt sind: CRLF, LF, CR, NONE.", e);
        }
    }
}
