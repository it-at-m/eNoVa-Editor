package de.muenchen.enovaeditor.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlWriter {

    public Path write(String html, Path outputPath) throws IOException {

        Files.writeString(outputPath, html, StandardCharsets.UTF_8);

        return outputPath;
    }
}