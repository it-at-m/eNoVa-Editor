package de.muenchen.enovaeditor.codelist;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class CodelistConfig {
    private static final String CONFIG_FILE = "codelists.properties";

    private final Properties properties = new Properties();
    private final Path configFile;

    public CodelistConfig() throws AppException {

        configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);

        } catch (IOException exception) {
            throw new AppException(
                    "Die Konfigurationsdatei \"" + CONFIG_FILE + "\" konnte nicht gelesen werden.",
                    List.of(new ErrorDetail("Datei", configFile.toString())),
                    exception
            );
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    "Die Konfigurationsdatei \"" + CONFIG_FILE + "\" enthält einen ungültigen Eintrag.",
                    List.of(new ErrorDetail("Datei", configFile.toString())),
                    exception
            );
        }
    }

    public CodelistDefinition get(String name) throws AppException {
        String fileName = getRequiredProperty(name, "file");

        try {
            Path.of(fileName);
        } catch (InvalidPathException exception) {
            throw new AppException(
                    "Der Dateipfad für die Codelist \"" + name + "\" ist ungültig.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Codelist", name),
                            new ErrorDetail("Eintrag", "file"),
                            new ErrorDetail("Wert", fileName)
                    ),
                    exception
            );
        }

        return new CodelistDefinition(fileName, getRequiredProperty(name, "keyColumn"), getRequiredProperty(name, "valueColumn"), getRequiredProperty(name, "rowElement"), getRequiredProperty(name, "valueElement"), getRequiredProperty(name, "columnAttribute"), getRequiredProperty(name, "simpleValueElement"));
    }

    private String getRequiredProperty(String name, String property) throws AppException {

        String key = name + "." + property;
        String defaultKey = "default." + property;

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            value = properties.getProperty(defaultKey);
        }

        if (value == null || value.isBlank()) {
            throw new AppException(
                    "In \"" + CONFIG_FILE
                            + "\" fehlt die Eigenschaft \""
                            + key
                            + "\" und es ist kein Standardwert \""
                            + defaultKey
                            + "\" definiert.",
                    List.of(
                            new ErrorDetail("Datei", configFile.toString()),
                            new ErrorDetail("Eintrag", key),
                            new ErrorDetail("Standard-Eintrag", defaultKey)
                    )
            );
        }

        return value.trim();
    }
}
