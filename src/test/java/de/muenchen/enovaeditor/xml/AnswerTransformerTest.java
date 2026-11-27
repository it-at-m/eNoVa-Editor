package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerTransformerTest {

    private static final File INPUT = new File(
            "demo-output/xjustiz_nachricht_beispiel_ersuchenSachentscheidung_mehrfach.xml"
    );
    private static final File PROPERTY_INPUT = new File("demo-output/xjustiz_nachricht-Test2.xml");

    private final XmlLoader xmlLoader = new XmlLoader();
    private final XPathReader xpathReader = new XPathReader();

    @Test
    void omitsRemovedQuestions() throws Exception {
        Document input = xmlLoader.load(INPUT);
        Document output = transform(input, List.of(
                new DecisionAnswer("012", "Erteilung der Genehmigung nach § 2 GVO", "019")));

        assertEquals(List.of("019"), decisionCodes(output));
        assertEquals(List.of("Antwort auf die Frage: Erteilung der Genehmigung nach § 2 GVO (012)"), requestHints(output));
        assertEquals(0, new ErsuchenSachentscheidungReader().read(output).size());
        assertEquals(3, new ErsuchenSachentscheidungReader().read(input).size());
    }

    @Test
    void includesAddedQuestionsInFormOrder() throws Exception {
        Document output = transform(xmlLoader.load(INPUT), List.of(
                new DecisionAnswer("013", "Dritte Frage", "021"),
                new DecisionAnswer("011", "Erste Frage", "023"),
                new DecisionAnswer("001", "Hinzugefügte Frage", "038"),
                new DecisionAnswer("012", "Zweite Frage", "019")));

        assertEquals(List.of("021", "023", "038", "019"), decisionCodes(output));
        assertEquals(List.of(
                "Antwort auf die Frage: Dritte Frage (013)",
                "Antwort auf die Frage: Erste Frage (011)",
                "Antwort auf die Frage: Hinzugefügte Frage (001)",
                "Antwort auf die Frage: Zweite Frage (012)"
        ), requestHints(output));
        assertEquals(0, new ErsuchenSachentscheidungReader().read(output).size());
    }

    @Test
    void includesOwnerAndLegalRepresentativeAsAddresseesForEachDecision() throws Exception {
        Document output = transform(xmlLoader.load(PROPERTY_INPUT), List.of(
                new DecisionAnswer("011", "Erste Frage", "019"),
                new DecisionAnswer("012", "Zweite Frage", "021")));

        List<Node> decisions = xpathReader.findNodes(output, "//tns:auswahl_GegenstandDerNachricht/tns:sachentscheidung");
        assertEquals(2, decisions.size());
        for (Node decision : decisions) {
            List<String> addressees = xpathReader.findNodes(decision,
                            "tns:adressatDerSachentscheidung/tns:ref.rollennummer")
                    .stream().map(Node::getTextContent).toList();
            assertEquals(List.of("3", "4"), addressees);
        }
    }

    private Document transform(Document input, List<DecisionAnswer> decisions) throws Exception {
        return new AnswerTransformer().transform(input, new AnswerParameters(
                "AZ-81", "message-81", new ManufacturerInfo("eNoVA", "München", "1.0"),
                "Bürgermeister", new File("caseworkers.xml").toURI().toString(), decisions
        ));
    }

    private List<String> requestHints(Document document) throws Exception {
        return xpathReader.findNodes(document,
                        "/tns:nachricht.enova.entscheidung.2900003/tns:fachdaten"
                                + "/tns:auswahl_GegenstandDerNachricht/tns:sachentscheidung"
                                + "/tns:hinweise_sonstige")
                .stream().map(Node::getTextContent).toList();
    }

    private List<String> decisionCodes(Document document) throws Exception {
        return xpathReader.findNodes(document,
                        "/tns:nachricht.enova.entscheidung.2900003/tns:fachdaten"
                                + "/tns:auswahl_GegenstandDerNachricht/tns:sachentscheidung"
                                + "/tns:sachentscheidung/code")
                .stream().map(Node::getTextContent).toList();
    }
}
