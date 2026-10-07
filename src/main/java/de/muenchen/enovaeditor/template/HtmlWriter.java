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
            throw new IOException("Die HTML-Datei konnte nicht geschrieben werden: " + outputPath, e);
        }

        return outputPath;
    }
}