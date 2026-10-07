package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.output.LineEnding;
import de.muenchen.enovaeditor.config.output.OutputConfigLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class XmlWriter {

    private final OutputConfigLoader outputConfigLoader = new OutputConfigLoader();

    public void write(Document document, File targetFile) throws TransformerException, IOException {

        LineEnding lineEnding = outputConfigLoader.loadLineEnding();

        TransformerFactory factory = TransformerFactory.newInstance();

        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = factory.newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(document), new StreamResult(writer));

        String xml = writer.toString();

        String formattedXml = applyLineEnding(xml, lineEnding);

        try {
            Files.writeString(targetFile.toPath(), formattedXml, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IOException("Die XML-Datei konnte nicht geschrieben werden: " + targetFile.toPath(), e);
        }
    }

    private String applyLineEnding(String xml, LineEnding lineEnding) {

        String normalized = xml.replace("\r\n", "\n").replace("\r", "\n");

        return normalized.replace("\n", lineEnding.value());
    }
}
