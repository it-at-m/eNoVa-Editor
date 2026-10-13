package de.muenchen.enovaeditor.config.output;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class OutputConfigLoader {

    private static final String CONFIG_FILE = "xml-output-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public LineEnding loadLineEnding() throws AppException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        Document document = xmlLoader.load(configFile.toFile());

        Node lineEndingNode = document.getDocumentElement().getElementsByTagName("lineEnding").item(0);

        if (lineEndingNode == null) {
            throw new AppException(
                    "In \"" + CONFIG_FILE + "\" fehlt das Element <lineEnding>.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Element", "lineEnding")
                    )
            );
        }

        String value = lineEndingNode.getTextContent();

        if (value == null || value.isBlank()) {
            throw new AppException(
                    "In \"" + CONFIG_FILE + "\" ist das Element <lineEnding> leer.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Element", "lineEnding")
                    )
            );
        }

        String normalizedValue = value.trim();

        try {
            return LineEnding.valueOf(normalizedValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    "In \"" + CONFIG_FILE + "\" enthält <lineEnding> den ungültigen Wert \""
                            + normalizedValue + "\". Erlaubt sind: CRLF, LF, CR, NONE.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Element", "lineEnding"),
                            new ErrorDetail("Wert", normalizedValue),
                            new ErrorDetail("Erlaubte Werte", "CRLF, LF, CR, NONE")
                    ),
                    e
            );
        }
    }
}
