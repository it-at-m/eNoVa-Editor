package de.muenchen.enovaeditor.error;

import java.util.List;

public record ErrorInfo(
        String message,
        List<ErrorDetail> details,
        String technicalDetails
) {
}