package de.muenchen.enovaeditor.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class OutputPathUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OutputPathUtil() {
    }

    public static Path createOutputPath(Path inputPath, String extension) {
        return createOutputPath(inputPath, "", extension);
    }

    public static Path createOutputPath(Path inputPath, String fileNameSuffix, String extension) {
        Path normalizedInputPath = inputPath.toAbsolutePath().normalize();

        Path outputDirectory = normalizedInputPath.getParent();

        String inputFileName = normalizedInputPath.getFileName().toString();

        String baseName = removeFileExtension(inputFileName);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        String suffixPart = fileNameSuffix.isBlank()
                ? ""
                : "-" + fileNameSuffix;

        String outputFileName =
                baseName
                        + suffixPart
                        + "-"
                        + timestamp
                        + extension;

        return outputDirectory.resolve(outputFileName);
    }

    private static String removeFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot <= 0) {
            return fileName;
        }

        return fileName.substring(0, lastDot);
    }
}
