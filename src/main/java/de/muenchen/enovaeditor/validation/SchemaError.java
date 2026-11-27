package de.muenchen.enovaeditor.validation;

public record SchemaError(
        int line,
        int column,
        String path,
        String message
) {
}
