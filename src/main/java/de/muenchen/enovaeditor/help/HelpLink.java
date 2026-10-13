package de.muenchen.enovaeditor.help;

import de.muenchen.enovaeditor.browser.BrowserOpener;
import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.error.ProblemDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;

import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class HelpLink {

    private static final String DEFAULT_URL = "https://enova-editor.oss.muenchen.de/";

    private HelpLink() {
    }

    public static void configure(Hyperlink link) {
        link.setVisible(false);
        link.setManaged(false);

        try {
            Path configFile = ApplicationPaths.getApplicationDirectory().resolve("help-config.properties");
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (NoSuchFileException ignored) {
                // Ohne Konfigurationsdatei wird die Standard-Hilfeseite verwendet.
            }

            String url = properties.getProperty("help.url", "").strip();
            if (url.isEmpty()) {
                url = DEFAULT_URL;
            }

            URI uri = URI.create(url);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new AppException(
                        "help.url muss eine vollständige HTTP- oder HTTPS-Adresse enthalten.",
                        List.of(new ErrorDetail("Datei", configFile.toString()), new ErrorDetail("Wert", url))
                );
            }

            link.setOnAction(event -> open(uri));
            link.setVisible(true);
            link.setManaged(true);
        } catch (Exception exception) {
            ProblemDialog.show(Alert.AlertType.WARNING, "Hilfe nicht verfügbar",
                    "Bitte prüfen Sie help.url in help-config.properties.", exception, List.of());
        }
    }

    private static void open(URI uri) {
        try {
            new BrowserOpener().open(uri);
        } catch (Exception exception) {
            ProblemDialog.show(Alert.AlertType.WARNING, "Hilfe konnte nicht geöffnet werden",
                    "Öffnen Sie die Adresse bei Bedarf manuell im Browser.", exception,
                    List.of(new ErrorDetail("Adresse", uri.toString())));
        }
    }
}
