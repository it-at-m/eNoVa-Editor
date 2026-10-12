package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.List;

public class SenderConfigLoader {

    private static final String CONFIG_FILE = "sender-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public String loadSenderName() throws AppException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        Document document = xmlLoader.load(configFile.toFile());

        Node nameNode = document.getDocumentElement().getElementsByTagName("name").item(0);

        if (nameNode == null) {
            throw new AppException(
                    "In \"" + CONFIG_FILE + "\" fehlt das Element <name>.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Element", "name")
                    )
            );
        }

        String senderName = nameNode.getTextContent();

        if (senderName == null || senderName.isBlank()) {
            throw new AppException(
                    "In \"" + CONFIG_FILE + "\" ist das Element <name> leer.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Element", "name")
                    )
            );
        }

        return senderName.trim();
    }

}
