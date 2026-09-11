package de.muenchen.enovaeditor.decision;

import java.time.LocalDate;

public record DecisionData(
        EnovaDecisionType decisionType,
        String fileNumber,
        String feeAmount,
        String caseworkerName,
        String caseworkerPhone,
        String tenor,
        String reasoning,
        LocalDate decisionDate
) {
    public static DecisionData of(
            EnovaDecisionType type,
            String fileNumber,
            String feeAmount,
            String caseworkerName,
            String caseworkerPhone,
            String tenor,
            String reasoning
    ) {
        return new DecisionData(
                type,
                fileNumber != null ? fileNumber.trim() : "",
                feeAmount != null ? feeAmount.trim() : "gebührenfrei",
                caseworkerName != null ? caseworkerName.trim() : "",
                caseworkerPhone != null ? caseworkerPhone.trim() : "",
                tenor != null && !tenor.isBlank() ? tenor.trim() : type.getDefaultTenor(),
                reasoning != null && !reasoning.isBlank() ? reasoning.trim() : type.getDefaultBegruendung(),
                LocalDate.now()
        );
    }
}
