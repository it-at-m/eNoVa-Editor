package de.muenchen.enovaeditor.util;

import de.muenchen.enovaeditor.config.ApplicationPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApplicationFileUtil {

    private ApplicationFileUtil() {
    }

    public static Path resolveReadableFile(String fileName) throws IOException {
        Path filePath = ApplicationPaths
                .getApplicationDirectory()
                .resolve(fileName);

        if (!Files.isRegularFile(filePath)) {
            throw new IOException(
                    "Die Datei " + fileName + " wurde nicht gefunden: "
                            + filePath
            );
        }

        if (!Files.isReadable(filePath)) {
            throw new IOException(
                    "Die Datei " + fileName + " kann nicht gelesen werden: "
                            + filePath
            );
        }

        return filePath;
    }
}
