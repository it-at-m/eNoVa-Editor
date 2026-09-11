package de.muenchen.enovaeditor.decision;

public enum EnovaDecisionType {

    NEGATIVZEUGNIS_NICHTAUSUEBUNG(
            "002",
            "Negativzeugnis: Vorkaufsrecht wird nicht ausgeübt (§ 28 Abs. 1 S. 1 BauGB)",
            "Negativzeugnis (Nichtausübung)",
            "§ 28 Abs. 1 Satz 1 BauGB",
            "Es wird hiermit bescheinigt, dass die Gemeinde das gesetzliche Vorkaufsrecht an dem im Notarvertrag bezeichneten Grundbesitz nicht ausübt.",
            "Die gesetzlichen Voraussetzungen für die Ausübung eines Vorkaufsrechts nach § 24 BauGB liegen nicht vor bzw. das Wohl der Allgemeinheit erfordert die Ausübung des Vorkaufsrechts nicht."
    ),
    NEGATIVZEUGNIS_NICHTBESTEHEN(
            "001",
            "Negativzeugnis: Vorkaufsrecht besteht nicht (§ 28 Abs. 1 S. 3 BauGB)",
            "Negativzeugnis (Nichtbestehen)",
            "§ 28 Abs. 1 Satz 3 BauGB",
            "Es wird hiermit bescheinigt, dass ein gesetzliches Vorkaufsrecht der Gemeinde nach dem Baugesetzbuch an dem im Notarvertrag bezeichneten Grundbesitz nicht besteht.",
            "Das Grundstück liegt nicht im Geltungsbereich eines Bebauungsplans oder einer Satzung nach §§ 24, 25 BauGB, die ein Vorkaufsrecht begründen würde."
    ),
    VORKAUFSRECHT_AUSGEUEBT(
            "003",
            "Ausübung des Vorkaufsrechts (§ 28 Abs. 2 BauGB)",
            "Ausübung des Vorkaufsrechts",
            "§ 28 Abs. 2 BauGB i.V.m. § 24 BauGB",
            "Die Gemeinde übt das ihr nach § 24 BauGB an dem veräußerten Grundstück zustehende Vorkaufsrecht hiermit förmlich aus.",
            "Die Ausübung des Vorkaufsrechts ist durch das Wohl der Allgemeinheit gerechtfertigt, insbesondere zur Deckung des kommunalen Wohn- und Infrastrukturbedarfs."
    ),
    SANIERUNGSRECHTLICHE_GENEHMIGUNG(
            "004",
            "Sanierungsrechtliche Genehmigung (§§ 144, 145 BauGB)",
            "Sanierungsrechtliche Genehmigung",
            "§§ 144, 145 BauGB",
            "Die sanierungsrechtliche Genehmigung für den bezeichneten Grundstückskaufvertrag wird hiermit erteilt.",
            "Das Rechtsgeschäft beeinträchtigt die Durchführung der städtebaulichen Sanierungsmaßnahme nicht."
    ),
    ERHALTUNGSSATZUNG_GENEHMIGUNG(
            "007",
            "Genehmigung im Bereich Erhaltungssatzung (§§ 172, 173 BauGB)",
            "Genehmigung Erhaltungssatzung",
            "§§ 172, 173 BauGB",
            "Die Genehmigung für den bezeichneten Grundstückskaufvertrag im Geltungsbereich der Erhaltungssatzung wird hiermit erteilt.",
            "Belange des Erhalts der städtebaulichen Eigenart oder der Zusammensetzung der Wohnbevölkerung werden nicht gefährdet."
    );

    private final String code;
    private final String displayName;
    private final String shortTitle;
    private final String legalBasis;
    private final String defaultTenor;
    private final String defaultBegruendung;

    EnovaDecisionType(String code, String displayName, String shortTitle, String legalBasis, String defaultTenor, String defaultBegruendung) {
        this.code = code;
        this.displayName = displayName;
        this.shortTitle = shortTitle;
        this.legalBasis = legalBasis;
        this.defaultTenor = defaultTenor;
        this.defaultBegruendung = defaultBegruendung;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public String getDefaultTenor() {
        return defaultTenor;
    }

    public String getDefaultBegruendung() {
        return defaultBegruendung;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
