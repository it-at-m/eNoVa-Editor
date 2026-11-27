package de.muenchen.enovaeditor.validation;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class SchemaValidator {

    private static final String CONFIG_FILE = "xsd/schemas.properties";

    private static final String XJUSTIZ_NAMESPACE =
            "http://www.xjustiz.de";

    private static final String SUPPORTED_ROOT =
            "nachricht.enova.entscheidung.2900003";

    private final Properties properties = new Properties();
    private final Path configFile;
    private final Map<String, Schema> schemas = new HashMap<>();

    // messageType: "Ersuchen", "Antwort" oder null, wenn der Gegenstand der Nachricht fehlt oder unbekannt ist.
    public record Result(String xjustizVersion, String messageType, List<SchemaError> errors) {
    }

    public SchemaValidator() throws AppException {

        configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);

        } catch (IOException | IllegalArgumentException exception) {
            throw new AppException(
                    "Die Konfigurationsdatei \"" + CONFIG_FILE + "\" konnte nicht gelesen werden.",
                    List.of(new ErrorDetail("Datei", configFile.toString())),
                    exception
            );
        }
    }

    public Result validate(File file) throws AppException {

        ApplicationFileUtil.requireReadableFile(file.toPath());

        String xjustizVersion = readXjustizVersion(file);

        Schema schema = getSchema(xjustizVersion);

        List<SchemaError> errors = new ArrayList<>();

        ElementPathFilter pathFilter;

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            factory.setNamespaceAware(true);

            // Sicherheitsmaßnahmen gegen externe XML-Ressourcen / XXE, wie in XmlLoader
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            XMLReader parser = factory.newSAXParser().getXMLReader();

            pathFilter = new ElementPathFilter(parser);

            ValidatorHandler validatorHandler = schema.newValidatorHandler();

            CollectingErrorHandler errorHandler = new CollectingErrorHandler(pathFilter, errors);

            validatorHandler.setErrorHandler(errorHandler);

            pathFilter.setContentHandler(validatorHandler);
            pathFilter.setErrorHandler(errorHandler);

            pathFilter.parse(new InputSource(file.toURI().toString()));

        } catch (SAXParseException exception) {
            throw new AppException(
                    "Die XML-Datei enthält ungültiges XML.",
                    List.of(
                            new ErrorDetail("Datei", file.toString()),
                            new ErrorDetail(
                                    "XML-Fehler erkannt bei",
                                    "Zeile " + exception.getLineNumber() + ", Spalte " + exception.getColumnNumber()
                            ),
                            new ErrorDetail("Parser-Hinweis", String.valueOf(exception.getMessage()))
                    ),
                    exception
            );

        } catch (SAXException | ParserConfigurationException exception) {
            throw new AppException(
                    "Die XML-Datei konnte nicht geprüft werden.",
                    List.of(new ErrorDetail("Datei", file.toString())),
                    exception
            );

        } catch (IOException exception) {
            throw new AppException(
                    "Die XML-Datei konnte nicht gelesen werden.",
                    List.of(new ErrorDetail("Datei", file.toString())),
                    exception
            );
        }

        return new Result(xjustizVersion, pathFilter.messageType(), List.copyOf(errors));
    }

    // Liest nur Wurzelelement und nachrichtenkopf/@xjustizVersion, ohne DTD und externe Entitäten.
    private String readXjustizVersion(File file) throws AppException {

        XMLInputFactory factory = XMLInputFactory.newFactory();

        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        try (InputStream input = Files.newInputStream(file.toPath())) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);

            try {
                int depth = 0;

                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT) {
                        depth++;

                        if (depth == 1 && !isSupportedRoot(reader)) {
                            throw new AppException(
                                    "Kein unterstützter eNoVA-Nachrichtentyp.",
                                    List.of(
                                            new ErrorDetail("Datei", file.toString()),
                                            new ErrorDetail("Erwartet", "{" + XJUSTIZ_NAMESPACE + "}" + SUPPORTED_ROOT),
                                            new ErrorDetail("Gefunden", "{" + reader.getNamespaceURI() + "}" + reader.getLocalName())
                                    )
                            );
                        }

                        if (depth == 2 && "nachrichtenkopf".equals(reader.getLocalName())
                                && XJUSTIZ_NAMESPACE.equals(reader.getNamespaceURI())) {

                            String version = reader.getAttributeValue(null, "xjustizVersion");

                            if (version == null || version.isBlank()) {
                                break;
                            }

                            return version;
                        }

                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        depth--;
                    }
                }
            } finally {
                reader.close();
            }

        } catch (XMLStreamException exception) {
            throw new AppException(
                    "Die XML-Datei enthält ungültiges XML.",
                    List.of(new ErrorDetail("Datei", file.toString())),
                    exception
            );

        } catch (IOException exception) {
            throw new AppException(
                    "Die XML-Datei konnte nicht gelesen werden.",
                    List.of(new ErrorDetail("Datei", file.toString())),
                    exception
            );
        }

        throw new AppException(
                "Die Nachricht enthält keine Angabe zur XJustiz-Version.",
                List.of(new ErrorDetail("Datei", file.toString()))
        );
    }

    private boolean isSupportedRoot(XMLStreamReader reader) {
        return XJUSTIZ_NAMESPACE.equals(reader.getNamespaceURI())
                && SUPPORTED_ROOT.equals(reader.getLocalName());
    }

    private Schema getSchema(String xjustizVersion) throws AppException {

        Schema cached = schemas.get(xjustizVersion);

        if (cached != null) {
            return cached;
        }

        String schemaFile = properties.getProperty(xjustizVersion);

        if (schemaFile == null || schemaFile.isBlank()) {
            throw new AppException(
                    "XJustiz-Version \"" + xjustizVersion + "\" ist in \"" + CONFIG_FILE + "\" nicht konfiguriert.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Konfigurierte Versionen", properties.stringPropertyNames().stream()
                                    .sorted()
                                    .collect(Collectors.joining(", ")))
                    )
            );
        }

        Path schemaPath = ApplicationFileUtil.requireReadableFile(
                configFile.getParent().resolve(schemaFile.trim())
        );

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // Die XSD-Dateien binden einander per xs:import als lokale Dateien ein.
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

            Schema schema = factory.newSchema(schemaPath.toFile());

            schemas.put(xjustizVersion, schema);

            return schema;

        } catch (SAXException exception) {
            throw new AppException(
                    "Das XJustiz-Schema konnte nicht geladen werden.",
                    List.of(
                            new ErrorDetail("Datei", schemaPath.toString()),
                            new ErrorDetail("XJustiz-Version", xjustizVersion)
                    ),
                    exception
            );
        }
    }

    // Führt den Elementpfad vor dem ValidatorHandler, weil dieser Fehler meldet, bevor er das Element weitergibt.
    private static class ElementPathFilter extends XMLFilterImpl {

        private static final List<String> GEGENSTAND = List.of(SUPPORTED_ROOT, "fachdaten", "auswahl_GegenstandDerNachricht");

        private final Deque<String> path = new ArrayDeque<>();
        private final Deque<String> names = new ArrayDeque<>();
        private final Deque<Map<String, Integer>> siblingCounts = new ArrayDeque<>();
        private String messageType;

        ElementPathFilter(XMLReader parent) {
            super(parent);
            siblingCounts.push(new HashMap<>());
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            int position = siblingCounts.peek().merge(localName, 1, Integer::sum);
            if (messageType == null && List.copyOf(names).equals(GEGENSTAND)) {
                // answer-transform.xsl ersetzt ersuchenSachentscheidung durch sachentscheidung.
                messageType = switch (localName) {
                    case "ersuchenSachentscheidung" -> "Ersuchen";
                    case "sachentscheidung" -> "Antwort";
                    default -> null;
                };
            }
            path.addLast(localName + "[" + position + "]");
            names.addLast(localName);
            siblingCounts.push(new HashMap<>());
            super.startElement(uri, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            siblingCounts.pop();
            path.removeLast();
            names.removeLast();
        }

        String currentPath() {
            return "/" + String.join("/", path);
        }

        String messageType() {
            return messageType;
        }
    }

    private record CollectingErrorHandler(ElementPathFilter pathFilter, List<SchemaError> errors)
            implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) {
        }

        @Override
        public void error(SAXParseException exception) {
            errors.add(new SchemaError(
                    exception.getLineNumber(),
                    exception.getColumnNumber(),
                    pathFilter.currentPath(),
                    exception.getMessage()
            ));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }
    }
}
