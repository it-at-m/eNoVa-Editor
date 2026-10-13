package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TemplateLoader {

    private static final String INPUT_TEMPLATE = "Input.htm";
    private static final String OUTPUT_TEMPLATE = "Output.htm";

    public TemplateSource loadInputTemplate() throws AppException {
        return loadTemplate(INPUT_TEMPLATE);
    }

    public TemplateSource loadOutputTemplate() throws AppException {
        return loadTemplate(OUTPUT_TEMPLATE);
    }

    private TemplateSource loadTemplate(String templateName)
            throws AppException {

        Path templatePath =
                ApplicationFileUtil.resolveReadableFile(templateName);

        String content;

        try {
            content = Files.readString(
                    templatePath,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new AppException(
                    "Das Template konnte nicht gelesen werden.",
                    List.of(
                            new ErrorDetail(
                                    "Template",
                                    templatePath.toString()
                            )
                    ),
                    exception
            );
        }

        return new TemplateSource(
                templatePath,
                content
        );
    }
}
