package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.config.ApplicationPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateLoader {

    private static final String INPUT_TEMPLATE = "Input.htm";

    public String loadInputTemplate() throws IOException {
        Path templatePath = ApplicationPaths
                .getApplicationDirectory()
                .resolve(INPUT_TEMPLATE);

        if (!Files.isRegularFile(templatePath)) {
            throw new IOException(
                    "Die Datei Input.htm wurde nicht gefunden: "
                            + templatePath
            );
        }

        if (!Files.isReadable(templatePath)) {
            throw new IOException(
                    "Die Datei Input.htm kann nicht gelesen werden: "
                            + templatePath
            );
        }

        return Files.readString(
                templatePath,
                StandardCharsets.UTF_8
        );
    }
}
