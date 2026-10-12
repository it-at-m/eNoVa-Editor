package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.output.LineEnding;
import de.muenchen.enovaeditor.config.output.OutputConfigLoader;
import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class XmlWriter {

    private final OutputConfigLoader outputConfigLoader = new OutputConfigLoader();

    public void write(Document document, File targetFile) throws AppException {

        LineEnding lineEnding = outputConfigLoader.loadLineEnding();

        Path targetPath = targetFile.toPath();

        Transformer transformer = createTransformer(targetPath);

        String formattedXml = serialize(document, transformer, lineEnding, targetPath);

        try {
            Files.writeString(
                    targetPath,
                    formattedXml,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

        } catch (FileAlreadyExistsException e) {
            throw writeException(
                    "Die XML-Datei konnte nicht gespeichert werden, weil die Zieldatei bereits existiert.",
                    targetPath,
                    e
            );
        } catch (AccessDeniedException e) {
            throw writeException(
                    "Der Schreibzugriff auf die XML-Zieldatei wurde verweigert.",
                    targetPath,
                    e
            );
        } catch (NoSuchFileException e) {
            throw writeException(
                    "Die XML-Datei konnte nicht gespeichert werden, weil ein Bestandteil des Zielpfads nicht vorhanden ist.",
                    targetPath,
                    e
            );
        } catch (FileSystemException e) {
            throw writeException(
                    "Die XML-Datei konnte wegen eines Dateisystemfehlers nicht gespeichert werden.",
                    targetPath,
                    e
            );
        } catch (IOException e) {
            throw writeException(
                    "Die XML-Datei konnte nicht erfolgreich gespeichert werden.",
                    targetPath,
                    e
            );
        }
    }

    private Transformer createTransformer(Path targetPath) throws AppException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();

            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            return transformer;

        } catch (TransformerConfigurationException | IllegalArgumentException e) {
            throw new AppException(
                    "Die XML-Ausgabe konnte nicht vorbereitet werden.",
                    List.of(new ErrorDetail("Zieldatei", targetPath.toString())),
                    e
            );
        }
    }

    private String serialize(
            Document document,
            Transformer transformer,
            LineEnding lineEnding,
            Path targetPath
    ) throws AppException {
        StringWriter writer = new StringWriter();

        try {
            transformer.transform(new DOMSource(document), new StreamResult(writer));

        } catch (TransformerException e) {
            throw new AppException(
                    "Die XML-Datei konnte nicht für das Speichern aufbereitet werden.",
                    transformationDetails(targetPath, e),
                    e
            );
        }

        return applyLineEnding(writer.toString(), lineEnding);
    }

    private List<ErrorDetail> transformationDetails(Path targetPath, TransformerException exception) {
        List<ErrorDetail> details = new ArrayList<>();
        details.add(new ErrorDetail("Zieldatei", targetPath.toString()));

        SourceLocator locator = findLocator(exception);

        if (locator != null) {
            if (locator.getSystemId() != null && !locator.getSystemId().isBlank()) {
                details.add(new ErrorDetail("Fehlerquelle", locator.getSystemId()));
            }

            if (locator.getLineNumber() > 0) {
                String position = "Zeile " + locator.getLineNumber();

                if (locator.getColumnNumber() > 0) {
                    position += ", Spalte " + locator.getColumnNumber();
                }

                details.add(new ErrorDetail("Transformationsfehler erkannt bei", position));
            }
        }

        return List.copyOf(details);
    }

    private SourceLocator findLocator(Throwable throwable) {
        Throwable current = throwable;
        SourceLocator firstLocator = null;

        while (current != null) {
            if (current instanceof TransformerException transformerException) {
                SourceLocator locator = transformerException.getLocator();

                if (locator != null) {
                    if (firstLocator == null) {
                        firstLocator = locator;
                    }

                    if (locator.getLineNumber() > 0) {
                        return locator;
                    }
                }
            }

            current = current.getCause();
        }

        return firstLocator;
    }

    private AppException writeException(String message, Path targetPath, IOException cause) {
        return new AppException(
                message,
                List.of(new ErrorDetail("Zieldatei", targetPath.toString())),
                cause
        );
    }

    private String applyLineEnding(String xml, LineEnding lineEnding) {

        String normalized = xml.replace("\r\n", "\n").replace("\r", "\n");

        return normalized.replace("\n", lineEnding.value());
    }
}
