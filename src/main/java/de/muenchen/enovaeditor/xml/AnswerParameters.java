package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;

public record AnswerParameters(
        String fileNumber,
        String messageId,
        ManufacturerInfo manufacturerInfo,
        String caseworkerName,
        String decisionCode
) {
}
