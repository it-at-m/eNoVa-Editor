package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.config.ApplicationPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateLoader {

    private static final String INPUT_TEMPLATE = "Input.htm";
    private static final String OUTPUT_TEMPLATE = "Output.htm";

    public String loadInputTemplate() throws IOException {
        return loadTemplate(INPUT_TEMPLATE);
    }

    public String loadOutputTemplate() throws IOException {
        return loadTemplate(OUTPUT_TEMPLATE);
    }

    private String loadTemplate(String templateName) throws IOException {
        Path templatePath = ApplicationPaths
                .getApplicationDirectory()
                .resolve(templateName);

        if (!Files.isRegularFile(templatePath)) {
            throw new IOException(
                    "Die Datei " + templateName + " wurde nicht gefunden: "
                            + templatePath
            );
        }

        if (!Files.isReadable(templatePath)) {
            throw new IOException(
                    "Die Datei " + templateName + " kann nicht gelesen werden: "
                            + templatePath
            );
        }

        return Files.readString(
                templatePath,
                StandardCharsets.UTF_8
        );
    }
}
