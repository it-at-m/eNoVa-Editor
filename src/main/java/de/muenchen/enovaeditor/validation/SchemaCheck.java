package de.muenchen.enovaeditor.validation;

import de.muenchen.enovaeditor.error.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SchemaCheck {

    // Nur für die Konsolenausgabe: Namensraum vor Elementnamen ("http://www.xjustiz.de":name), Präfix in Element 'tns:name',
    // führender Fehlercode der Meldung.
    private static final Pattern NAMESPACE = Pattern.compile("\"[^\"]*\":(?=[\\w.-])");
    private static final Pattern SINGLE_NAME_IN_BRACES = Pattern.compile("\\{([\\w.-]+)}");
    private static final Pattern ELEMENT_PREFIX = Pattern.compile("([Ee]lements? ')[A-Za-z_][\\w.-]*:");
    private static final Pattern LEADING_CODE = Pattern.compile("^(cvc-[\\w.-]+): (.*)$", Pattern.DOTALL);

    private static final int VALID = 0;
    private static final int SCHEMA_ERRORS = 1;
    private static final int NOT_CHECKABLE = 2;

    private SchemaCheck() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    private static int run(String[] args) {

        if (args.length == 0) {
            System.out.println("Aufruf: eNoVA-schema-validation.cmd <Datei oder Ordner> [weitere Dateien oder Ordner]");
            System.out.println("Prüft eNoVA-Nachrichten (nachricht.enova.entscheidung.2900003) gegen das XJustiz-Schema.");
            return NOT_CHECKABLE;
        }

        boolean missingXmlFiles = false;
        List<Path> files = new ArrayList<>();

        for (String argument : args) {
            Path path = Path.of(argument);

            if (Files.isDirectory(path)) {
                List<Path> xmlFiles = listXmlFiles(path);

                if (xmlFiles.isEmpty()) {
                    System.out.println(path + ": keine XML-Dateien gefunden");
                    missingXmlFiles = true;
                }

                files.addAll(xmlFiles);
            } else {
                files.add(path);
            }
        }

        SchemaValidator validator;

        try {
            validator = new SchemaValidator();
        } catch (AppException exception) {
            System.out.println("Schemaprüfung nicht möglich: " + exception.getMessage());
            return NOT_CHECKABLE;
        }

        int valid = 0;
        int withErrors = 0;
        int notCheckable = 0;

        for (Path file : files) {
            try {
                SchemaValidator.Result result = validator.validate(file.toFile());

                for (SchemaError error : result.errors()) {
                    System.out.println("%s:%d:%d: %s: %s".formatted(
                            file,
                            error.line(),
                            error.column(),
                            readablePath(error.path()),
                            readableMessage(error.message())
                    ));
                }

                String type = result.messageType() == null ? "" : result.messageType() + ", ";
                if (result.errors().isEmpty()) {
                    System.out.println(file + ": " + type + "gültig (XJustiz " + result.xjustizVersion() + ")");
                    valid++;
                } else {
                    System.out.println(file + ": " + type + result.errors().size() + " Fehler (XJustiz " + result.xjustizVersion() + ")");
                    withErrors++;
                }

            } catch (AppException exception) {
                System.out.println(file + ": nicht geprüft - " + exception.getMessage());
                notCheckable++;
            }
        }

        System.out.println("%d XML-Dateien gefunden: %d gültig, %d mit Fehlern, %d nicht prüfbar".formatted(
                files.size(),
                valid,
                withErrors,
                notCheckable
        ));

        if (notCheckable > 0 || missingXmlFiles) {
            return NOT_CHECKABLE;
        }

        return withErrors > 0 ? SCHEMA_ERRORS : VALID;
    }

    // [1] weglassen, [n] ab dem zweiten gleichnamigen Element behalten.
    private static String readablePath(String path) {
        return path.replace("[1]", "");
    }

    private static String readableMessage(String message) {
        String text = NAMESPACE.matcher(message).replaceAll("");
        text = SINGLE_NAME_IN_BRACES.matcher(text).replaceAll("$1");
        text = ELEMENT_PREFIX.matcher(text).replaceAll("$1");
        return LEADING_CODE.matcher(text).replaceFirst("$2 ($1)");
    }

    private static List<Path> listXmlFiles(Path directory) {

        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .sorted()
                    .toList();

        } catch (IOException exception) {
            System.out.println(directory + ": Ordner konnte nicht gelesen werden");
            return List.of();
        }
    }
}
