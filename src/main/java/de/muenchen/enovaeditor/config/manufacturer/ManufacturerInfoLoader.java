package de.muenchen.enovaeditor.config.manufacturer;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class ManufacturerInfoLoader {

    private static final String CONFIG_FILE = "/manufacturer-info.properties";

    public ManufacturerInfo load() throws AppException {
        Properties properties = new Properties();

        try (InputStream inputStream =
                     ManufacturerInfoLoader.class.getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new AppException(
                        "Herstellerinformationen konnten nicht geladen werden.",
                        resourceDetails()
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            throw new AppException(
                    "Die Herstellerinformationen konnten nicht gelesen werden.",
                    resourceDetails(),
                    e
            );
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    "Die Herstellerinformationen enthalten einen ungültigen Eintrag.",
                    resourceDetails(),
                    e
            );
        }

        return new ManufacturerInfo(
                getRequiredProperty(properties, "product.name"),
                getRequiredProperty(properties, "manufacturer.name"),
                getRequiredProperty(properties, "version")
        );
    }

    private String getRequiredProperty(Properties properties, String key)
            throws AppException {

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new AppException(
                    "Herstellerinformation '" + key + "' fehlt oder ist leer.",
                    List.of(
                            new ErrorDetail("Ressource", CONFIG_FILE),
                            new ErrorDetail("Eintrag", key)
                    )
            );
        }

        return value.trim();
    }

    private static List<ErrorDetail> resourceDetails() {
        return List.of(new ErrorDetail("Ressource", CONFIG_FILE));
    }
}
