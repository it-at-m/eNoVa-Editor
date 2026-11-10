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

    public static Path getUserDirectory() throws AppException {

        String configuredUserHome = System.getenv("ENOVA_USER_HOME");

        if (configuredUserHome != null && !configuredUserHome.isBlank()) {
            try {
                return Path.of(configuredUserHome)
                        .toAbsolutePath()
                        .normalize();
            } catch (InvalidPathException e) {
                throw new AppException(
                        "Das in der Umgebungsvariable \"ENOVA_USER_HOME\" konfigurierte Benutzerverzeichnis ist ungültig.",
                        List.of(
                                new ErrorDetail("Umgebungsvariable", "ENOVA_USER_HOME"),
                                new ErrorDetail("Wert", configuredUserHome)
                        ),
                        e
                );
            }
        }

        String appData = System.getenv("APPDATA");

        if (appData == null || appData.isBlank()) {
            return null;
        }

        try {
            return Path.of(appData, "eNoVA-Editor")
                    .toAbsolutePath()
                    .normalize();
        } catch (InvalidPathException e) {
            throw new AppException(
                    "Das Benutzerverzeichnis konnte nicht ermittelt werden.",
                    List.of(
                            new ErrorDetail("Umgebungsvariable", "APPDATA"),
                            new ErrorDetail("Wert", appData)
                    ),
                    e
            );
        }
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
