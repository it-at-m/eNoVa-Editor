package de.muenchen.enovaeditor.codelist;

import de.muenchen.enovaeditor.config.ApplicationPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class CodelistConfig {

    private final Properties properties = new Properties();

    public CodelistConfig() throws IOException {

        Path configFile = ApplicationPaths.getApplicationDirectory().resolve("codelists.properties");

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {

            properties.load(reader);
        }
    }

    public CodelistDefinition get(String name) {

        return new CodelistDefinition(getRequiredProperty(name, "file"), getRequiredProperty(name, "keyColumn"), getRequiredProperty(name, "valueColumn"), getRequiredProperty(name, "rowElement"), getRequiredProperty(name, "valueElement"), getRequiredProperty(name, "columnAttribute"), getRequiredProperty(name, "simpleValueElement"));
    }

    private String getRequiredProperty(String name, String property) {

        String key = name + "." + property;

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            value = properties.getProperty("default." + property);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Codelist-Konfiguration fehlt: " + key);
        }

        return value.trim();
    }
}