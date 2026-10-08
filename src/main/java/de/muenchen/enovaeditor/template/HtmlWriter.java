package de.muenchen.enovaeditor.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlWriter {

    public Path write(String html, Path outputPath) throws IOException {

        try {
            Files.writeString(outputPath, html, StandardCharsets.UTF_8);

        } catch (IOException e) {
            String errorMessage = String.format(
                "Die HTML-Datei konnte nicht geschrieben werden:%n%s%n" +
                "Öffnen Sie die XML-Datei bitte in einem Verzeichnis, in dem Sie Schreibrechte haben. (z. B. Dokumente)",
                outputPath
            );
            throw new IOException(errorMessage, e);
        }

        return outputPath;
    }
}
