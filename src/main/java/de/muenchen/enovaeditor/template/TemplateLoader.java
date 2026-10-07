package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateLoader {

    private static final String INPUT_TEMPLATE = "Input.htm";
    private static final String OUTPUT_TEMPLATE = "Output.htm";

    public TemplateSource loadInputTemplate() throws IOException {
        return loadTemplate(INPUT_TEMPLATE);
    }

    public TemplateSource loadOutputTemplate() throws IOException {
        return loadTemplate(OUTPUT_TEMPLATE);
    }

    private TemplateSource loadTemplate(String templateName)
            throws IOException {

        Path templatePath =
                ApplicationFileUtil.resolveReadableFile(templateName);

        String content = Files.readString(
                templatePath,
                StandardCharsets.UTF_8
        );

        return new TemplateSource(
                templatePath,
                content
        );
    }
}