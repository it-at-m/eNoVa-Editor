package de.muenchen.enovaeditor.browser;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

public class BrowserOpener {

    public void open(Path htmlFile) throws IOException {

        if (!Desktop.isDesktopSupported()) {
            throw new IOException(
                    "Das Öffnen im Browser wird auf diesem System nicht unterstützt."
            );
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            throw new IOException(
                    "Auf diesem System kann kein Standardbrowser geöffnet werden."
            );
        }

        desktop.browse(
                htmlFile.toUri()
        );
    }
}
