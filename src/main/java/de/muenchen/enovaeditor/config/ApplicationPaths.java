package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.EnovaEditorApplication;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApplicationPaths {

    private ApplicationPaths() {
    }

    public static Path getApplicationDirectory() {

        String configuredHome = System.getenv("ENOVA_HOME");

        if (configuredHome != null && !configuredHome.isBlank()) {
            return Path.of(configuredHome)
                    .toAbsolutePath()
                    .normalize();
        }

        try {
            Path location = Path.of(
                    EnovaEditorApplication.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();

            Path dir = Files.isRegularFile(location) ? location.getParent() : location;
            if (Files.exists(dir.resolve("Input.htm"))) {
                return dir;
            }
            if (dir.getParent() != null && Files.exists(dir.getParent().resolve("Input.htm"))) {
                return dir.getParent();
            }

            Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            if (Files.exists(userDir.resolve("Input.htm"))) {
                return userDir;
            }

            return dir;

        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "Programmverzeichnis konnte nicht ermittelt werden.",
                    e
            );
        }
    }
}
