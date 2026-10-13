package de.muenchen.enovaeditor.error;

import java.util.List;
import java.util.Objects;

public class AppException extends Exception {

    private final List<ErrorDetail> details;

    public AppException(String message, List<ErrorDetail> details) {
        super(requireMessage(message));
        this.details = List.copyOf(Objects.requireNonNull(details, "details"));
    }

    public AppException(String message, List<ErrorDetail> details, Throwable cause) {
        super(requireMessage(message), cause);
        this.details = List.copyOf(Objects.requireNonNull(details, "details"));
    }

    public List<ErrorDetail> details() {
        return details;
    }

    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message");

        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        return message;
    }
}
