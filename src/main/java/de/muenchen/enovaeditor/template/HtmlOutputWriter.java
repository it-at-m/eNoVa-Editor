package de.muenchen.enovaeditor.template;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HtmlOutputWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Path write(
            String html,
            File inputXmlFile
    ) throws IOException {

        Path inputPath = inputXmlFile
                .toPath()
                .toAbsolutePath()
                .normalize();

        Path outputDirectory =
                inputPath.getParent();

        String inputFileName =
                inputPath.getFileName().toString();

        String baseName =
                removeFileExtension(inputFileName);

        String timestamp =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String outputFileName =
                baseName
                        + "-"
                        + timestamp
                        + ".htm";

        Path outputPath =
                outputDirectory.resolve(outputFileName);

        Files.writeString(
                outputPath,
                html,
                StandardCharsets.UTF_8
        );

        return outputPath;
    }

    private String removeFileExtension(String fileName) {

        int lastDot =
                fileName.lastIndexOf('.');

        if (lastDot <= 0) {
            return fileName;
        }

        return fileName.substring(0, lastDot);
    }
}
