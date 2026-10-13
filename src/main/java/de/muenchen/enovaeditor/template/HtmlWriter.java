package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class HtmlWriter {

    public Path write(String html, Path outputPath) throws AppException {

        try {
            Files.writeString(
                    outputPath,
                    html,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

        } catch (FileAlreadyExistsException e) {
            throw writeException(
                    "Die HTML-Datei konnte nicht gespeichert werden, weil die Zieldatei bereits existiert.",
                    outputPath,
                    e
            );
        } catch (AccessDeniedException e) {
            throw writeException(
                    "Die HTML-Datei konnte nicht gespeichert werden. Öffnen Sie die XML-Datei in einem Ordner, in dem Sie Schreibrechte haben (z. B. Dokumente).",
                    outputPath,
                    e
            );
        } catch (NoSuchFileException e) {
            throw writeException(
                    "Die HTML-Datei konnte nicht gespeichert werden, weil ein Bestandteil des Zielpfads nicht vorhanden ist.",
                    outputPath,
                    e
            );
        } catch (FileSystemException e) {
            throw writeException(
                    "Die HTML-Datei konnte wegen eines Dateisystemfehlers nicht gespeichert werden.",
                    outputPath,
                    e
            );
        } catch (IOException e) {
            throw writeException(
                    "Die HTML-Datei konnte nicht erfolgreich gespeichert werden.",
                    outputPath,
                    e
            );
        }

        return outputPath;
    }

    private AppException writeException(String message, Path outputPath, IOException cause) {
        return new AppException(
                message,
                List.of(new ErrorDetail("Zieldatei", outputPath.toString())),
                cause
        );
    }
}
