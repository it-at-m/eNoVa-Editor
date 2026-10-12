package de.muenchen.enovaeditor.browser;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class BrowserOpener {

    public void open(Path htmlFile) throws AppException {

        if (!Desktop.isDesktopSupported()) {
            throw new AppException(
                    "Das Öffnen im Browser wird auf diesem System nicht unterstützt.",
                    htmlFileDetail(htmlFile)
            );
        }

        try {
            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                throw new AppException(
                        "Auf diesem System kann kein Standardbrowser geöffnet werden.",
                        htmlFileDetail(htmlFile)
                );
            }

            desktop.browse(
                    htmlFile.toUri()
            );

        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(
                    "Die HTML-Datei konnte nicht im Standardbrowser geöffnet werden.",
                    htmlFileDetail(htmlFile),
                    e
            );
        } catch (SecurityException e) {
            throw new AppException(
                    "Der Zugriff auf den Standardbrowser wurde verweigert.",
                    htmlFileDetail(htmlFile),
                    e
            );
        } catch (UnsupportedOperationException e) {
            throw new AppException(
                    "Auf diesem System kann kein Standardbrowser geöffnet werden.",
                    htmlFileDetail(htmlFile),
                    e
            );
        }
    }

    private List<ErrorDetail> htmlFileDetail(Path htmlFile) {
        return List.of(new ErrorDetail("HTML-Datei", htmlFile.toString()));
    }
}
