package de.muenchen.enovaeditor.pdf;

import de.muenchen.enovaeditor.decision.DecisionData;
import de.muenchen.enovaeditor.decision.EnovaDecisionType;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class BescheidPdfGeneratorTest {

    @Test
    void testPdfGenerationWithSampleXml() throws Exception {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        assertTrue(sampleFile.exists(), "Sample file should exist");

        XmlLoader xmlLoader = new XmlLoader();
        Document doc = xmlLoader.load(sampleFile);
        assertNotNull(doc);

        DecisionData decision = DecisionData.of(
                EnovaDecisionType.NEGATIVZEUGNIS_NICHTBESTEHEN,
                "V-2024-8812-B",
                "50,00 EUR",
                "Max Mustermann",
                "+49 89 233-0000",
                "Ein gesetzliches Vorkaufsrecht nach den Vorschriften des Baugesetzbuchs besteht für das bezeichnete Grundstück nicht.",
                "Das Grundstück liegt nicht im Geltungsbereich eines Bebauungsplans oder einer Satzung nach §§ 24, 25 BauGB."
        );

        File tempPdf = File.createTempFile("test_bescheid_", ".pdf");
        tempPdf.deleteOnExit();

        BescheidPdfGenerator generator = new BescheidPdfGenerator();
        generator.generatePdf(doc, decision, tempPdf);

        assertTrue(tempPdf.exists());
        assertTrue(tempPdf.length() > 500);

        byte[] pdfBytes = Files.readAllBytes(tempPdf.toPath());
        String pdfHeader = new String(pdfBytes, 0, Math.min(20, pdfBytes.length));
        assertTrue(pdfHeader.startsWith("%PDF-1.4"));

        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("trailer"));
        assertTrue(pdfContent.contains("startxref"));
        assertTrue(pdfContent.contains("%%EOF"));
    }
}