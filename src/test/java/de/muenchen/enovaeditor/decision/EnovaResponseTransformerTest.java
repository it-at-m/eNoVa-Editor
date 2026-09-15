package de.muenchen.enovaeditor.decision;

import de.muenchen.enovaeditor.config.caseworker.CaseworkerEntry;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class EnovaResponseTransformerTest {

    private EnovaResponseTransformer transformer;
    private Document sampleDocument;

    @BeforeEach
    void setUp() throws Exception {
        transformer = new EnovaResponseTransformer();
        XmlLoader xmlLoader = new XmlLoader();
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        sampleDocument = xmlLoader.load(sampleFile);
    }

    @Test
    void testTransformationRetainsXJustiz2900003Root() {
        CaseworkerEntry caseworker = new CaseworkerEntry("Bürgermeister", "<tns:auswahl_beteiligter><tns:natuerlichePerson><tns:vollerName><tns:nachname>Bewiesen</tns:nachname></tns:vollerName></tns:natuerlichePerson></tns:auswahl_beteiligter>");

        Document response = transformer.transform(
                sampleDocument,
                "002",
                "61-VKR-2026/0815",
                "Landeshauptstadt München",
                caseworker
        );

        assertNotNull(response);
        Element root = response.getDocumentElement();
        assertEquals("nachricht.enova.entscheidung.2900003", root.getLocalName());
        assertEquals("http://www.xjustiz.de", root.getNamespaceURI());

        // Check that ersuchenSachentscheidung was removed and sachentscheidung added
        NodeList ersuchenList = root.getElementsByTagNameNS("http://www.xjustiz.de", "ersuchenSachentscheidung");
        assertEquals(0, ersuchenList.getLength(), "ersuchenSachentscheidung muss in der Antwort entfernt sein.");

        NodeList sachentscheidungList = root.getElementsByTagNameNS("http://www.xjustiz.de", "sachentscheidung");
        assertTrue(sachentscheidungList.getLength() > 0, "sachentscheidung muss in der Antwort vorhanden sein.");

        // Check code
        NodeList codeList = root.getElementsByTagName("code");
        boolean found002 = false;
        for (int i = 0; i < codeList.getLength(); i++) {
            if ("002".equals(codeList.item(i).getTextContent().trim())) {
                found002 = true;
                break;
            }
        }
        assertTrue(found002, "Entscheidungscode 002 muss im XML enthalten sein.");
    }

    @Test
    void testSenderAndRecipientSwap() {
        Document response = transformer.transform(
                sampleDocument,
                "001",
                "AZ-12345",
                "Stadt Musterstadt",
                null
        );

        Element root = response.getDocumentElement();

        // Check new absender has municipality data
        NodeList absenderList = root.getElementsByTagNameNS("http://www.xjustiz.de", "absender");
        assertEquals(1, absenderList.getLength());
        Element absender = (Element) absenderList.item(0);
        assertTrue(absender.getTextContent().contains("AZ-12345"));
        assertTrue(absender.getTextContent().contains("Stadt Musterstadt"));

        // Check new empfaenger has notary data
        NodeList empfaengerList = root.getElementsByTagNameNS("http://www.xjustiz.de", "empfaenger");
        assertEquals(1, empfaengerList.getLength());
        Element empfaenger = (Element) empfaengerList.item(0);
        assertTrue(empfaenger.getTextContent().contains("Notar"));
    }

    @Test
    void testPreservesUrkundenAndFlurstueckData() {
        Document response = transformer.transform(
                sampleDocument,
                "003",
                "TEST-AZ",
                "Gemeinde",
                null
        );

        String xml = transformer.documentToXmlString(response);
        assertTrue(xml.contains("815 / 2026"), "Urkundennummer muss erhalten bleiben.");
        assertTrue(xml.contains("2026-09-05"), "Urkundsdatum muss erhalten bleiben.");
        assertTrue(xml.contains("Schwabing"), "Grundbuchbezirk muss erhalten bleiben.");
        assertTrue(xml.contains("091234"), "Gemarkungsschlüssel muss erhalten bleiben.");
        assertTrue(xml.contains("<tns:zaehler>412</tns:zaehler>"), "Flurstückszähler muss erhalten bleiben.");
    }
}
