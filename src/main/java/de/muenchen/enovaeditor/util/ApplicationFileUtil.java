package de.muenchen.enovaeditor.util;

import de.muenchen.enovaeditor.config.ApplicationPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApplicationFileUtil {

    private ApplicationFileUtil() {
    }

    public static Path resolveReadableFile(String fileName)
            throws IOException {

        Path filePath = ApplicationPaths
                .getApplicationDirectory()
                .resolve(fileName);

        return requireReadableFile(filePath);
    }

    public static Path requireReadableFile(Path filePath)
            throws IOException {

        if (!Files.isRegularFile(filePath)) {
            throw new IOException(
                    "Die Datei "
                            + filePath.getFileName()
                            + " wurde nicht gefunden: "
                            + filePath
            );
        }

        if (!Files.isReadable(filePath)) {
            throw new IOException(
                    "Die Datei "
                            + filePath.getFileName()
                            + " kann nicht gelesen werden: "
                            + filePath
            );
        }

        return filePath;
    }
}
