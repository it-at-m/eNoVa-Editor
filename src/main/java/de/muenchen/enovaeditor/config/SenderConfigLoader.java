package de.muenchen.enovaeditor.config;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SenderConfigLoader {

    private static final String CONFIG_FILE = "sender-config.properties";

    public String loadSenderName() throws IOException {

        Path configFile = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);

        Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String senderName = properties.getProperty("sender.name");
        if (senderName == null || senderName.isBlank()) {
            throw new IOException("Die Eigenschaft 'sender.name' fehlt oder ist leer.");
        }
        return senderName.trim();
    }

}
