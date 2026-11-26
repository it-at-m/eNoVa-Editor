package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;
import de.muenchen.enovaeditor.xml.AnswerParameters;
import de.muenchen.enovaeditor.xml.AnswerTransformer;
import de.muenchen.enovaeditor.xml.DecisionAnswer;
import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungReader;
import de.muenchen.enovaeditor.xml.XmlLoader;
import de.muenchen.enovaeditor.xml.XPathReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAnswerTemplateTest {

    private static final File INPUT = new File(
            "demo-output/xjustiz_nachricht_beispiel_ersuchenSachentscheidung_mehrfach.xml"
    );
    private static final File OUTPUT = new File(
            "demo-output/xjustiz_nachricht_beispiel_ersuchenSachentscheidung_mehrfach-Output.xml"
    );
    private static final File PROPERTY_OUTPUT = new File("demo-output/xjustiz_nachricht-Test2-Output.xml");
    private static final File PROPERTY_INPUT = new File("demo-output/xjustiz_nachricht-Test2.xml");

    private final XPathTemplateRenderer renderer = new XPathTemplateRenderer();
    private final TemplateLoader templateLoader = new TemplateLoader();

    @Test
    void keepsNotaryDetailsInIncomingDocumentWithNestedReference() throws Exception {
        String html = renderer.render(templateLoader.loadInputTemplate(), new XmlLoader().load(INPUT));

        assertTrue(html.contains("Dr."));
        assertTrue(html.contains("Martin"));
        assertTrue(html.contains("Müller"));
        assertTrue(html.contains("030 - 1231239321"));
        assertTrue(html.contains("Aktenzeichen:"));
        assertTrue(html.contains("UVZ-4697"));
        assertTrue(html.contains("13.08.2026"));
        assertTrue(html.contains("LH München"));
        assertTrue(html.contains("Veräußerer (Veräußerin)"));
        assertTrue(html.contains("Erwerber(in)"));
        assertEquals(4, count(html, "(Dies können auch mehrere Personen sein.)"));
    }

    @Test
    void alsoResolvesUnnestedRoleReferences() throws Exception {
        Document input = new XmlLoader().load(INPUT);
        new XPathReader().findNodes(input,
                "//tns:absender/tns:informationen/tns:auswahl_verweisGrunddaten/tns:ref.rollennummer")
                .get(0).setTextContent("1");

        String html = renderer.render(templateLoader.loadInputTemplate(), input);
        assertTrue(html.contains("Notar(in)"));
        assertTrue(html.contains("030 - 1231239321"));
    }

    @Test
    void keepsCaseworkerAndNotaryDetailsInOutgoingDocumentWithNestedReferences() throws Exception {
        String html = renderer.render(templateLoader.loadOutputTemplate(), new XmlLoader().load(OUTPUT));

        assertTrue(html.contains("Antonia"));
        assertTrue(html.contains("Allenthaler"));
        assertTrue(html.contains("vorkaufsrecht@hinterhugeldapfing.de"));
        assertTrue(html.contains("LH 2026-1234"));
        assertTrue(html.contains("Hinterhugeldapfing,"));
        assertTrue(html.contains("24.10.2026"));
        assertTrue(html.contains("Dr."));
        assertTrue(html.contains("Martin"));
        assertTrue(html.contains("Müller"));
        assertFalse(html.contains("030 - 1231239321"));
        assertTrue(html.contains("Veräußerer (Veräußerin)"));
        assertTrue(html.contains("Erwerber(in)"));
        assertEquals(4, count(html, "(Dies können auch mehrere Personen sein.)"));
    }

    @Test
    void displaysSingleAndMultipleRequestsInTheSameList() throws Exception {
        Document input = new XmlLoader().load(INPUT);
        TemplateSource template = templateLoader.loadInputTemplate();

        assertEquals(3, count(renderer.render(template, input), "<li>"));

        List<Node> requests = new ErsuchenSachentscheidungReader().read(input);
        requests.get(1).getParentNode().removeChild(requests.get(1));
        requests.get(2).getParentNode().removeChild(requests.get(2));

        String html = renderer.render(template, input);
        assertEquals(1, count(html, "<li>"));
        assertTrue(html.contains("(011)"));
    }

    @Test
    void displaysEachQuestionWithItsDecisionForSingleAndMultipleAnswers() throws Exception {
        Document input = new XmlLoader().load(INPUT);
        TemplateSource template = templateLoader.loadOutputTemplate();

        String single = renderer.render(template, transform(input, List.of(
                new DecisionAnswer("012", "Zweite Frage", "019"))));
        assertEquals(1, count(single, "<li>"));
        assertTrue(single.contains("Zweite Frage (012)"));
        assertTrue(single.contains("Unsere Entscheidung:"));
        assertTrue(single.contains("(019)"));
        assertTrue(single.contains("Hubert"));
        assertTrue(single.contains("Bewiesen"));
        assertTrue(single.contains("AZ-81"));
        assertFalse(single.contains("030 - 1231239321"));

        String multiple = renderer.render(template, transform(input, List.of(
                new DecisionAnswer("013", "Dritte Frage", "021"),
                new DecisionAnswer("011", "Erste Frage", "023"))));
        assertEquals(2, count(multiple, "<li>"));
        assertTrue(multiple.indexOf("Dritte Frage (013)") < multiple.indexOf("Erste Frage (011)"));
        assertTrue(multiple.contains("(021)"));
        assertTrue(multiple.contains("(023)"));
    }

    @Test
    void rendersOrganisationOwnersAndRepresentativesAndOnlyAvailableBirthDates() throws Exception {
        for (boolean outgoing : List.of(false, true)) {
            Document document = new XmlLoader().load(outgoing ? PROPERTY_OUTPUT : PROPERTY_INPUT);
            XPathReader xpath = new XPathReader();
            for (String role : List.of("057", "088")) {
                Node participant = xpath.findNodes(document,
                        "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='" + role
                                + "']/tns:beteiligter/tns:auswahl_beteiligter").get(0);
                Node person = xpath.findNodes(participant, "tns:natuerlichePerson").get(0);
                Node organisation = document.createElementNS("http://www.xjustiz.de", "tns:organisation");
                Node designation = document.createElementNS("http://www.xjustiz.de", "tns:bezeichnung");
                Node name = document.createElementNS("http://www.xjustiz.de", "tns:bezeichnung.aktuell");
                name.setTextContent("Organisation " + role);
                designation.appendChild(name);
                organisation.appendChild(designation);
                organisation.appendChild(xpath.findNodes(person, "tns:anschrift").get(0).cloneNode(true));
                participant.replaceChild(organisation, person);
            }

            String html = renderer.render(outgoing ? templateLoader.loadOutputTemplate()
                    : templateLoader.loadInputTemplate(), document);
            assertTrue(html.contains("Organisation 057"));
            assertTrue(html.contains("Organisation 088"));
            assertTrue(html.contains("Zenettistraße"));
            assertTrue(html.contains("Oberländerstraße"));
            assertFalse(html.contains("Geboren am"));
        }
    }

    @Test
    void preservesPartialBirthDatesAndOmitsMissingOnes() throws Exception {
        for (boolean outgoing : List.of(false, true)) {
            Document document = new XmlLoader().load(outgoing ? PROPERTY_OUTPUT : PROPERTY_INPUT);
            XPathReader xpath = new XPathReader();
            Node ownerDate = xpath.findNodes(document,
                    "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='057']//tns:geburtsdatum").get(0);
            ownerDate.setTextContent("1965");
            Node representativeDate = xpath.findNodes(document,
                    "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='088']//tns:geburtsdatum").get(0);
            representativeDate.setTextContent("1978-09");
            TemplateSource template = outgoing ? templateLoader.loadOutputTemplate() : templateLoader.loadInputTemplate();

            String html = renderer.render(template, document);
            assertTrue(html.contains("Geboren am 1965"));
            assertTrue(html.contains("Geboren am 1978-09"));
            assertFalse(html.contains("..1965"));

            representativeDate.getParentNode().removeChild(representativeDate);
            assertEquals(1, count(renderer.render(template, document), "Geboren am"));
        }
    }

    @Test
    void keepsSpecialRightsAndTheirHeadingsWithinEachProperty() throws Exception {
        Document document = new XmlLoader().load(PROPERTY_OUTPUT);
        List<Node> properties = new XPathReader().findNodes(document, "//tns:beschreibungDesBetroffenenRechts");
        Node right = new XPathReader().findNodes(properties.get(0),
                "tns:buchungsstelle[tns:auswahl_buchungsstelle/tns:erbbaurecht]").get(0);
        properties.get(1).insertBefore(right.cloneNode(true),
                new XPathReader().findNodes(properties.get(1), "tns:anschrift").get(0));

        for (TemplateSource template : List.of(templateLoader.loadInputTemplate(), templateLoader.loadOutputTemplate())) {
            String html = renderer.render(template, document);
            String[] blocks = html.split("<div class=\"grundstueck-block\">", -1);
            assertEquals(3, blocks.length);
            assertEquals(1, count(blocks[1], "Sonderfall:</span>"));
            assertEquals(1, count(blocks[2], "Sonderfall:</span>"));
            assertEquals(1, count(blocks[1], ">Erbbaurecht</div>"));
            assertEquals(1, count(blocks[2], ">Erbbaurecht</div>"));
            for (int property = 1; property <= 2; property++) {
                assertTrue(blocks[property].lastIndexOf("Größe:") < blocks[property].indexOf("Sonderfall:</span>"));
                assertTrue(blocks[property].indexOf("Sonderfall:</span>") < blocks[property].indexOf(">Erbbaurecht</div>"));
            }
            assertTrue(blocks[1].matches("(?s).*WEG-Einheit\\(en\\):.*Wohnung.*"));
            assertTrue(blocks[1].matches("(?s).*Miteigentum:.*17 Quadratmeter.*"));
            assertFalse(html.contains("getElementById('sonderfall')"));
        }
    }

    private Document transform(Document input, List<DecisionAnswer> decisions) throws Exception {
        return new AnswerTransformer().transform(input, new AnswerParameters(
                "AZ-81", "message-81", new ManufacturerInfo("eNoVA", "München", "1.0"),
                "Bürgermeister", new File("caseworkers.xml").toURI().toString(), decisions
        ));
    }

    private int count(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
