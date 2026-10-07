package de.muenchen.enovaeditor.error;

import de.muenchen.enovaeditor.template.TemplateRenderException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ErrorInfoFactory {

    private static final String DOCTYPE_FEATURE = "disallow-doctype-decl";

    private static final String ANSWER_TRANSFORM_FILE = "answer-transform.xsl";

    private ErrorInfoFactory() {
    }

    public static ErrorInfo from(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable");

        List<ErrorDetail> details = new ArrayList<>();

        SAXParseException saxParseException = findException(throwable, SAXParseException.class);

        TransformerConfigurationException configurationException = findException(throwable, TransformerConfigurationException.class);

        TemplateRenderException templateException = findException(throwable, TemplateRenderException.class);

        TransformerException transformerException = findTransformerException(throwable);

        boolean answerTransformError = isAnswerTransformError(configurationException);

        if (templateException != null) {

            details.add(new ErrorDetail("Datei", templateException.templateName()));

            addPositionDetail(details, "Template-Position", templateException.line(), templateException.column());

            if (templateException.xpathExpression() != null && !templateException.xpathExpression().isBlank()) {

                details.add(new ErrorDetail("XPath-Ausdruck", templateException.xpathExpression()));
            }

        } else if (answerTransformError) {

            details.add(new ErrorDetail("Datei", ANSWER_TRANSFORM_FILE));

            SourceLocator locator = configurationException.getLocator();

            addPositionDetail(details, "XSLT-Fehler erkannt bei", locator.getLineNumber(), locator.getColumnNumber());

        } else if (saxParseException != null) {

            String fileName = findFileName(saxParseException.getSystemId());

            if (fileName != null) {
                details.add(new ErrorDetail("Datei", fileName));
            }

            addPositionDetail(details, "XML-Fehler erkannt bei", saxParseException.getLineNumber(), saxParseException.getColumnNumber());

        } else if (transformerException != null && transformerException.getLocator() != null) {

            SourceLocator locator = transformerException.getLocator();

            addPositionDetail(details, "Transformationsfehler erkannt bei", locator.getLineNumber(), locator.getColumnNumber());
        }

        return new ErrorInfo(createUserMessage(throwable, templateException, saxParseException, configurationException, transformerException), List.copyOf(details), createStackTrace(throwable));
    }

    private static String createUserMessage(Throwable throwable, TemplateRenderException templateException, SAXParseException saxParseException, TransformerConfigurationException configurationException, TransformerException transformerException) {
        if (templateException != null) {
            return templateException.getMessage();
        }

        if (isAnswerTransformError(configurationException)) {
            return "Die Transformationsregel " + "\"" + ANSWER_TRANSFORM_FILE + "\" " + "enthält einen Fehler und konnte nicht " + "verarbeitet werden.";
        }

        if (isDoctypeNotAllowed(saxParseException)) {
            return "Die ausgewählte XML-Datei enthält eine " + "DOCTYPE-Deklaration. " + "DOCTYPE-Deklarationen werden aus " + "Sicherheitsgründen nicht unterstützt.";
        }

        if (saxParseException != null && saxParseException.getMessage() != null && !saxParseException.getMessage().isBlank()) {
            return saxParseException.getMessage();
        }

        if (configurationException != null) {
            return "Eine Transformationskomponente konnte " + "nicht vorbereitet werden.";
        }

        if (transformerException != null) {
            return "Bei der Erstellung der Antwort ist ein " + "Transformationsfehler aufgetreten.";
        }

        return findMessage(throwable);
    }

    private static boolean isDoctypeNotAllowed(SAXParseException saxParseException) {
        return saxParseException != null && saxParseException.getMessage() != null && saxParseException.getMessage().contains(DOCTYPE_FEATURE);
    }

    private static void addPositionDetail(List<ErrorDetail> details, String label, int lineNumber, int columnNumber) {
        if (lineNumber <= 0) {
            return;
        }

        String position = "Zeile " + lineNumber;

        if (columnNumber > 0) {
            position += ", Spalte " + columnNumber;
        }

        details.add(new ErrorDetail(label, position));
    }

    private static String findMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unbekannter Fehler";
        }

        Throwable current = throwable;

        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }

            current = current.getCause();
        }

        return throwable.getClass().getSimpleName();
    }

    private static String findFileName(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return null;
        }

        try {
            return Path.of(URI.create(systemId)).getFileName().toString();

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isAnswerTransformError(TransformerConfigurationException exception) {
        if (exception == null || exception.getLocator() == null) {
            return false;
        }

        String fileName = findFileName(exception.getLocator().getSystemId());

        return ANSWER_TRANSFORM_FILE.equals(fileName);
    }

    private static String createStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();

        throwable.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    private static TransformerException findTransformerException(Throwable throwable) {
        Throwable current = throwable;
        TransformerException firstTransformerException = null;

        while (current != null) {

            if (current instanceof TransformerException transformerException && !(current instanceof TransformerConfigurationException)) {

                if (firstTransformerException == null) {
                    firstTransformerException = transformerException;
                }

                if (transformerException.getLocator() != null) {
                    return transformerException;
                }
            }

            current = current.getCause();
        }

        return firstTransformerException;
    }

    private static <T extends Throwable> T findException(
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
}
