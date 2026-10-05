package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;

import java.util.List;

public record AnswerParameters(
        String fileNumber,
        String messageId,
        ManufacturerInfo manufacturerInfo,
        String caseworkerName,
        List<String> decisionCodes
) {
}
