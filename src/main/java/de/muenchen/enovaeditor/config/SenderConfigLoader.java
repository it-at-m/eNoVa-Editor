package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;

public class SenderConfigLoader {

    private static final String CONFIG_FILE = "sender-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public String loadSenderName() throws IOException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        Document document;

        try {
            document = xmlLoader.load(configFile.toFile());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(
                    "Absender-Konfiguration konnte nicht gelesen werden.",
                    e
            );
        }

        Node nameNode = document
                .getDocumentElement()
                .getElementsByTagName("name")
                .item(0);

        if (nameNode == null) {
            throw new IOException("Das Element 'name' fehlt.");
        }

        String senderName = nameNode.getTextContent();

        if (senderName == null || senderName.isBlank()) {
            throw new IOException("Das Element 'name' ist leer.");
        }

        return senderName.trim();
    }

}
