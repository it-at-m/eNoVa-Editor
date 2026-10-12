package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlLoader {

    private static final String DOCTYPE_FEATURE = "disallow-doctype-decl";

    public Document load(File file) throws AppException {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            // Sicherheitsmaßnahmen gegen externe XML-Ressourcen / XXE
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(file);

        } catch (ParserConfigurationException e) {
            throw new AppException(
                    "Der XML-Parser konnte nicht initialisiert werden.",
                    fileDetails(file),
                    e
            );

        } catch (SAXException e) {
            SAXParseException parseException = findSaxParseException(e);

            String message = isDoctypeNotAllowed(parseException)
                    ? "Die XML-Datei enthält eine DOCTYPE-Deklaration. "
                    + "DOCTYPE-Deklarationen werden aus Sicherheitsgründen nicht unterstützt."
                    : "Die XML-Datei enthält ungültiges XML.";

            throw new AppException(
                    message,
                    xmlDetails(file, parseException),
                    e
            );

        } catch (IOException e) {
            throw new AppException(
                    "Die XML-Datei konnte nicht gelesen werden.",
                    fileDetails(file),
                    e
            );
        }
    }

    private static List<ErrorDetail> fileDetails(File file) {
        return List.of(new ErrorDetail(
                "Datei",
                file.toPath().toAbsolutePath().normalize().toString()
        ));
    }

    private static List<ErrorDetail> xmlDetails(
            File file,
            SAXParseException parseException
    ) {
        List<ErrorDetail> details = new ArrayList<>(fileDetails(file));

        if (parseException != null && parseException.getLineNumber() > 0) {
            String position = "Zeile " + parseException.getLineNumber();

            if (parseException.getColumnNumber() > 0) {
                position += ", Spalte " + parseException.getColumnNumber();
            }

            details.add(new ErrorDetail("XML-Fehler erkannt bei", position));
        }

        if (parseException != null
                && !isDoctypeNotAllowed(parseException)
                && parseException.getMessage() != null
                && !parseException.getMessage().isBlank()) {

            details.add(new ErrorDetail(
                    "Parser-Hinweis",
                    parseException.getMessage()
            ));
        }

        return details;
    }

    private static SAXParseException findSaxParseException(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SAXParseException parseException) {
                return parseException;
            }

            current = current.getCause();
        }

        return null;
    }

    private static boolean isDoctypeNotAllowed(SAXParseException exception) {
        return exception != null
                && exception.getMessage() != null
                && exception.getMessage().contains(DOCTYPE_FEATURE);
    }
}
