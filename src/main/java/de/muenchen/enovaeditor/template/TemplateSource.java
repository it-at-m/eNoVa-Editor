package de.muenchen.enovaeditor.template;

import java.nio.file.Path;
import java.util.Objects;

public record TemplateSource(
        Path path,
        String content
) {

    public TemplateSource {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
    }

    public String fileName() {
        return path.getFileName().toString();
    }

    public Position positionAt(int index) {
        if (index < 0 || index > content.length()) {
            throw new IllegalArgumentException(
                    "Ungültige Position im Template: " + index
            );
        }

        int line = 1;
        int column = 1;

        for (int i = 0; i < index; i++) {

            if (content.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        return new Position(line, column);
    }

    public record Position(
            int line,
            int column
    ) {
    }
}