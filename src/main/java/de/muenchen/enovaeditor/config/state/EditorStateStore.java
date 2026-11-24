package de.muenchen.enovaeditor.config.state;

import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.error.AppException;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class EditorStateStore {

    private static final String STATE_FILE = "editor-state.properties";

    private static final String LAST_XML_DIRECTORY = "lastXmlDirectory";

    // Der zuletzt verwendete Ordner ist nur eine Komfortfunktion: Fehler beim Lesen werden ignoriert.
    public Path loadLastXmlDirectory() {

        try {
            Path stateFile = stateFile();

            if (stateFile == null || !Files.isRegularFile(stateFile)) {
                return null;
            }

            Properties properties = new Properties();

            try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            String value = properties.getProperty(LAST_XML_DIRECTORY, "").strip();

            return value.isEmpty() ? null : Path.of(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void saveLastXmlDirectory(Path directory) {

        try {
            Path stateFile = stateFile();

            if (stateFile == null || directory == null) {
                return;
            }

            Properties properties = new Properties();
            properties.setProperty(LAST_XML_DIRECTORY, directory.toAbsolutePath().toString());

            Files.createDirectories(stateFile.getParent());

            try (Writer writer = Files.newBufferedWriter(stateFile, StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
        } catch (Exception ignored) {
            // Ist das Benutzerverzeichnis z. B. schreibgeschützt, arbeitet der Editor ohne gespeicherten Ordner weiter.
        }
    }

    private Path stateFile() throws AppException {
        Path userDirectory = ApplicationPaths.getUserDirectory();

        return userDirectory == null ? null : userDirectory.resolve(STATE_FILE);
    }
}
