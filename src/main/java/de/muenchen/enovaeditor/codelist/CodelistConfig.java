package de.muenchen.enovaeditor.codelist;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class CodelistConfig {
    private static final String CONFIG_FILE = "codelists.properties";

    private final Properties properties = new Properties();

    public CodelistConfig() throws IOException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);

        } catch (IOException exception) {
            throw new IOException("Die Konfigurationsdatei \"" + CONFIG_FILE + "\" konnte nicht gelesen werden.", exception);
        }
    }

    public CodelistDefinition get(String name) {

        return new CodelistDefinition(getRequiredProperty(name, "file"), getRequiredProperty(name, "keyColumn"), getRequiredProperty(name, "valueColumn"), getRequiredProperty(name, "rowElement"), getRequiredProperty(name, "valueElement"), getRequiredProperty(name, "columnAttribute"), getRequiredProperty(name, "simpleValueElement"));
    }

    private String getRequiredProperty(String name, String property) {

        String key = name + "." + property;
        String defaultKey = "default." + property;

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            value = properties.getProperty(defaultKey);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "In \"" + CONFIG_FILE
                            + "\" fehlt die Eigenschaft \""
                            + key
                            + "\" und es ist kein Standardwert \""
                            + defaultKey
                            + "\" definiert."
            );
        }

        return value.trim();
    }
}