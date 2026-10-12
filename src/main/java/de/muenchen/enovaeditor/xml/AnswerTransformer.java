package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import net.sf.saxon.TransformerFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AnswerTransformer {

    private static final String DOCTYPE_FEATURE = "disallow-doctype-decl";
    private static final String RULE_FILE = "answer-transform.xsl";

    public Document transform(Document document, AnswerParameters parameters) throws AppException {
        Path xsltPath = ApplicationFileUtil.resolveReadableFile(RULE_FILE);

        StreamSource xsltSource = new StreamSource(xsltPath.toFile());

        Transformer transformer;

        try {
            transformer = createTransformer(xsltSource);
        } catch (TransformerConfigurationException exception) {
            throw createTransformationException(
                    "Die Transformationsregel enthält einen Fehler und konnte nicht verarbeitet werden.",
                    xsltPath,
                    exception,
                    "XSLT-Fehler erkannt bei"
            );
        }

        DOMSource domSource = new DOMSource(document);
        DOMResult domResult = new DOMResult();

        transformer.setParameter("fileNumber", parameters.fileNumber());
        transformer.setParameter("messageId", parameters.messageId());
        transformer.setParameter("caseworkerName", parameters.caseworkerName());
        transformer.setParameter("decisionCodes", parameters.decisionCodes());

        transformer.setParameter("productName", parameters.manufacturerInfo().productName());
        transformer.setParameter("manufacturerName", parameters.manufacturerInfo().manufacturerName());
        transformer.setParameter("version", parameters.manufacturerInfo().version());


        try {
            transformer.transform(domSource, domResult);
        } catch (TransformerException exception) {
            throw createTransformationException(
                    "Bei der Erstellung der Antwort ist ein Transformationsfehler aufgetreten.",
                    xsltPath,
                    exception,
                    "Transformationsfehler erkannt bei"
            );
        }

        Node resultNode = domResult.getNode();

        if (resultNode.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document) resultNode;
        }

        throw new AppException(
                "Die Antwort konnte nicht korrekt erstellt werden.",
                List.of(new ErrorDetail(
                        "Transformationsregel",
                        xsltPath.toString()
                ))
        );
    }

    private Transformer createTransformer(StreamSource xsltSource)
            throws TransformerConfigurationException {

        TransformerFactory transformerFactory =
                new TransformerFactoryImpl();

        List<TransformerException> compilationErrors =
                new ArrayList<>();

        transformerFactory.setErrorListener(new ErrorListener() {

            @Override
            public void warning(TransformerException exception) {
                // Warnungen führen nicht zum Abbruch.
            }

            @Override
            public void error(TransformerException exception) {
                compilationErrors.add(exception);
            }

            @Override
            public void fatalError(TransformerException exception) {
                compilationErrors.add(exception);
            }
        });

        try {
            return transformerFactory.newTransformer(xsltSource);

        } catch (TransformerConfigurationException exception) {

            if (!compilationErrors.isEmpty()) {

                TransformerException compilationError =
                        compilationErrors.get(0);

                throw new TransformerConfigurationException(
                        compilationError.getMessage(),
                        compilationError.getLocator(),
                        exception
                );
            }

            throw exception;
        }
    }

    private AppException createTransformationException(
            String message,
            Path xsltPath,
            TransformerException exception,
            String positionLabel
    ) {
        List<ErrorDetail> details = new ArrayList<>();

        details.add(new ErrorDetail(
                "Transformationsregel",
                xsltPath.toString()
        ));

        SAXParseException saxParseException = findException(
                exception,
                SAXParseException.class
        );

        String userMessage = isDoctypeNotAllowed(saxParseException)
                ? "Die XML-Datei enthält eine DOCTYPE-Deklaration. "
                + "DOCTYPE-Deklarationen werden aus Sicherheitsgründen nicht unterstützt."
                : message;

        SourceLocator locator = findLocator(exception);

        String sourceSystemId = null;
        int lineNumber = -1;
        int columnNumber = -1;

        if (saxParseException != null && saxParseException.getLineNumber() > 0) {
            sourceSystemId = saxParseException.getSystemId();
            lineNumber = saxParseException.getLineNumber();
            columnNumber = saxParseException.getColumnNumber();
        } else if (locator != null && locator.getLineNumber() > 0) {
            sourceSystemId = locator.getSystemId();
            lineNumber = locator.getLineNumber();
            columnNumber = locator.getColumnNumber();
        }

        if (lineNumber > 0) {
            addSourceDetail(details, sourceSystemId);
            addPositionDetail(details, positionLabel, lineNumber, columnNumber);

        } else if (saxParseException != null
                && saxParseException.getSystemId() != null
                && !saxParseException.getSystemId().isBlank()) {

            addSourceDetail(details, saxParseException.getSystemId());

        } else if (locator != null) {
            addSourceDetail(details, locator.getSystemId());
        }

        return new AppException(userMessage, details, exception);
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

    private <T extends Throwable> T findException(
            Throwable throwable,
            Class<T> exceptionType
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (exceptionType.isInstance(current)) {
                return exceptionType.cast(current);
            }

            current = current.getCause();
        }

        return null;
    }

    private boolean isDoctypeNotAllowed(SAXParseException exception) {
        return exception != null
                && exception.getMessage() != null
                && exception.getMessage().contains(DOCTYPE_FEATURE);
    }

    private void addSourceDetail(
            List<ErrorDetail> details,
            String systemId
    ) {
        if (systemId == null || systemId.isBlank()) {
            return;
        }

        String source = systemId;

        try {
            URI uri = URI.create(systemId);

            if ("file".equalsIgnoreCase(uri.getScheme())) {
                source = Path.of(uri).toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Die ursprüngliche Quellenangabe bleibt erhalten.
        }

        details.add(new ErrorDetail("Fehlerquelle", source));
    }

    private void addPositionDetail(
            List<ErrorDetail> details,
            String label,
            int lineNumber,
            int columnNumber
    ) {
        if (lineNumber <= 0) {
            return;
        }

        String position = "Zeile " + lineNumber;

        if (columnNumber > 0) {
            position += ", Spalte " + columnNumber;
        }

        details.add(new ErrorDetail(label, position));
    }
}
