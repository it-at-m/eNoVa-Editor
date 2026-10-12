package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public final class ApplicationPaths {

    private ApplicationPaths() {
    }

    public static Path getApplicationDirectory() throws AppException {

        String configuredHome = System.getenv("ENOVA_HOME");

        if (configuredHome != null && !configuredHome.isBlank()) {
            try {
                return Path.of(configuredHome)
                        .toAbsolutePath()
                        .normalize();
            } catch (InvalidPathException e) {
                throw new AppException(
                        "Das in der Umgebungsvariable \"ENOVA_HOME\" konfigurierte Programmverzeichnis ist ungültig.",
                        List.of(
                                new ErrorDetail("Umgebungsvariable", "ENOVA_HOME"),
                                new ErrorDetail("Wert", configuredHome)
                        ),
                        e
                );
            }
        }

        try {
            Path location = Path.of(
                    ApplicationPaths.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();

            if (Files.isRegularFile(location)) {
                return location.getParent();
            }

            return Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath()
                    .normalize();

        } catch (URISyntaxException | InvalidPathException e) {
            throw new AppException(
                    "Das Programmverzeichnis konnte nicht ermittelt werden.",
                    List.of(),
                    e
            );
        }
    }
}
