package de.muenchen.enovaeditor.config.output;

public enum LineEnding {

    CRLF("\r\n"),
    LF("\n"),
    CR("\r"),
    NONE("");

    private final String value;

    LineEnding(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}