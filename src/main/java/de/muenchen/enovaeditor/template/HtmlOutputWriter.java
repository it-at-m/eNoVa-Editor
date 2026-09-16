package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.util.OutputPathUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlOutputWriter {

    public Path write(String html, Path inputXmlPath) throws IOException {

        Path outputPath = OutputPathUtil.createOutputPath(inputXmlPath, ".htm");

        Files.writeString(outputPath, html, StandardCharsets.UTF_8);

        return outputPath;
    }
}