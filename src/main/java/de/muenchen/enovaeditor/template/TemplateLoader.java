package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateLoader {

    private static final String INPUT_TEMPLATE = "Input.htm";

    public String loadInputTemplate() throws IOException {
        Path templatePath = ApplicationFileUtil.resolveReadableFile(INPUT_TEMPLATE);

        return Files.readString(
                templatePath,
                StandardCharsets.UTF_8
        );
    }
}
