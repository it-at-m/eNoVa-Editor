package de.muenchen.enovaeditor.browser;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class BrowserOpener {

    public void open(Path htmlFile) throws AppException {
        open(htmlFile.toUri(), htmlFileDetail(htmlFile),
                "Die HTML-Datei konnte nicht im Standardbrowser geöffnet werden.");
    }

    public void open(URI uri) throws AppException {
        open(uri, List.of(new ErrorDetail("Adresse", uri.toString())),
                "Die Seite konnte nicht im Standardbrowser geöffnet werden.");
    }

    private void open(URI uri, List<ErrorDetail> details, String failureMessage) throws AppException {

        if (!Desktop.isDesktopSupported()) {
            throw new AppException(
                    "Das Öffnen im Browser wird auf diesem System nicht unterstützt.",
                    details
            );
        }

        try {
            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                throw new AppException(
                        "Auf diesem System kann kein Standardbrowser geöffnet werden.",
                        details
                );
            }

            desktop.browse(uri);

        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(
                    failureMessage,
                    details,
                    e
            );
        } catch (SecurityException e) {
            throw new AppException(
                    "Der Zugriff auf den Standardbrowser wurde verweigert.",
                    details,
                    e
            );
        } catch (UnsupportedOperationException e) {
            throw new AppException(
                    "Auf diesem System kann kein Standardbrowser geöffnet werden.",
                    details,
                    e
            );
        }
    }

    private List<ErrorDetail> htmlFileDetail(Path htmlFile) {
        return List.of(new ErrorDetail("HTML-Datei", htmlFile.toString()));
    }
}
