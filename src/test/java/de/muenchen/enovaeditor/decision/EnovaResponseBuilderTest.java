package de.muenchen.enovaeditor.decision;

import de.muenchen.enovaeditor.pdf.BescheidPdfGenerator;
import de.muenchen.enovaeditor.xml.XmlLoader;
import de.muenchen.enovaeditor.xml.XmlWriter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class EnovaResponseBuilderTest {

    @Test
    void testBuildResponseAndGeneratePdf() throws Exception {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        assertTrue(sampleFile.exists());

        XmlLoader xmlLoader = new XmlLoader();
        Document inputDoc = xmlLoader.load(sampleFile);

        DecisionData decision = DecisionData.of(
                EnovaDecisionType.NEGATIVZEUGNIS_NICHTAUSUEBUNG,
                "61-VKR-2026/0815",
                "50,00 EUR",
                "Anna Mustermann",
                "089-123456",
                null,
                null
        );

        EnovaResponseBuilder builder = new EnovaResponseBuilder();
        Document responseDoc = builder.buildResponse(inputDoc, decision);

        assertNotNull(responseDoc);
        Element root = responseDoc.getDocumentElement();
        assertEquals("tns:nachricht.enova.entscheidung.2900004", root.getNodeName());
        assertEquals("http://www.xjustiz.de", root.getNamespaceURI());

        // Check XML writing
        File tempXml = File.createTempFile("enova-test-response", ".xml");
        tempXml.deleteOnExit();
        XmlWriter writer = new XmlWriter();
        assertDoesNotThrow(() -> writer.write(responseDoc, tempXml));
        assertTrue(tempXml.length() > 0);

        String xmlContent = Files.readString(tempXml.toPath());
        assertTrue(xmlContent.contains("nachricht.enova.entscheidung.2900004"));
        assertTrue(xmlContent.contains("61-VKR-2026/0815"));
        assertTrue(xmlContent.contains("Anna Mustermann"));
        assertTrue(xmlContent.contains("50,00 EUR"));
        assertTrue(xmlContent.contains("815 / 2026"));

        // Check PDF generation
        File tempPdf = File.createTempFile("enova-test-bescheid", ".pdf");
        tempPdf.deleteOnExit();
        BescheidPdfGenerator pdfGen = new BescheidPdfGenerator();
        assertDoesNotThrow(() -> pdfGen.generatePdf(inputDoc, decision, tempPdf));
        assertTrue(tempPdf.length() > 0);
        assertTrue(tempPdf.length() > 1000, "PDF should have substantial content");
    }
}
