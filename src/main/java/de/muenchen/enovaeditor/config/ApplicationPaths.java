package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.EnovaEditorApplication;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApplicationPaths {

    private ApplicationPaths() {
    }

    public static Path getApplicationDirectory() {
        try {
            Path location = Path.of(
                    EnovaEditorApplication.class
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

        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "Programmverzeichnis konnte nicht ermittelt werden.",
                    e
            );
        }
    }
}
