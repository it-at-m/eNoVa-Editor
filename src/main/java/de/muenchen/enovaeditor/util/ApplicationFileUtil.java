package de.muenchen.enovaeditor.util;

import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public final class ApplicationFileUtil {

    private ApplicationFileUtil() {
    }

    public static Path resolveReadableFile(String fileName)
            throws AppException {

        Path filePath = ApplicationPaths
                .getApplicationDirectory()
                .resolve(fileName);

        return requireReadableFile(filePath);
    }

    public static Path requireReadableFile(Path filePath)
            throws AppException {

        BasicFileAttributes attributes;

        try {
            attributes = Files.readAttributes(
                    filePath,
                    BasicFileAttributes.class
            );
        } catch (NoSuchFileException e) {
            throw new AppException(
                    "Die Datei \"" + filePath.getFileName() + "\" wurde nicht gefunden.",
                    List.of(new ErrorDetail("Datei", filePath.toString())),
                    e
            );
        } catch (AccessDeniedException e) {
            throw new AppException(
                    "Auf die Datei \"" + filePath.getFileName() + "\" kann nicht zugegriffen werden.",
                    List.of(new ErrorDetail("Datei", filePath.toString())),
                    e
            );
        } catch (IOException e) {
            throw new AppException(
                    "Der Status der Datei \"" + filePath.getFileName() + "\" konnte nicht ermittelt werden.",
                    List.of(new ErrorDetail("Datei", filePath.toString())),
                    e
            );
        }

        if (!attributes.isRegularFile()) {
            throw new AppException(
                    "Der Pfad \"" + filePath.getFileName() + "\" verweist nicht auf eine reguläre Datei.",
                    List.of(new ErrorDetail("Datei", filePath.toString()))
            );
        }

        if (!Files.isReadable(filePath)) {
            throw new AppException(
                    "Die Datei \"" + filePath.getFileName() + "\" kann nicht gelesen werden.",
                    List.of(new ErrorDetail("Datei", filePath.toString()))
            );
        }

        return filePath;
    }
}
