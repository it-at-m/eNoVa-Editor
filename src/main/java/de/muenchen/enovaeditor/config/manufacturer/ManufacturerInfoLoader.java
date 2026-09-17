package de.muenchen.enovaeditor.config.manufacturer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ManufacturerInfoLoader {

    private static final String CONFIG_FILE = "/manufacturer-info.properties";

    public ManufacturerInfo load() throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream =
                     ManufacturerInfoLoader.class.getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new IOException(
                        "Herstellerinformationen konnten nicht geladen werden."
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }

        return new ManufacturerInfo(
                getRequiredProperty(properties, "product.name"),
                getRequiredProperty(properties, "manufacturer.name"),
                getRequiredProperty(properties, "version")
        );
    }

    private String getRequiredProperty(Properties properties, String key)
            throws IOException {

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IOException(
                    "Herstellerinformation '" + key + "' fehlt oder ist leer."
            );
        }

        return value.trim();
    }
}
