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

    private final XmlLoader xmlLoader = new XmlLoader();
    private final XPathReader xpathReader = new XPathReader();

    @Test
    void omitsRemovedQuestions() throws Exception {
        Document input = xmlLoader.load(INPUT);
        Document output = transform(input, List.of("019"));

        assertEquals(List.of("019"), decisionCodes(output));
        assertEquals(0, new ErsuchenSachentscheidungReader().read(output).size());
        assertEquals(3, new ErsuchenSachentscheidungReader().read(input).size());
    }

    @Test
    void includesAddedQuestionsInFormOrder() throws Exception {
        Document output = transform(xmlLoader.load(INPUT), List.of("021", "023", "038", "019"));

        assertEquals(List.of("021", "023", "038", "019"), decisionCodes(output));
        assertEquals(0, new ErsuchenSachentscheidungReader().read(output).size());
    }

    private Document transform(Document input, List<String> decisionCodes) throws Exception {
        return new AnswerTransformer().transform(input, new AnswerParameters(
                "AZ-81", "message-81", new ManufacturerInfo("eNoVA", "München", "1.0"),
                "Bürgermeister", decisionCodes
        ));
    }

    private List<String> decisionCodes(Document document) throws Exception {
        return xpathReader.findNodes(document,
                        "/tns:nachricht.enova.entscheidung.2900003/tns:fachdaten"
                                + "/tns:auswahl_GegenstandDerNachricht/tns:sachentscheidung"
                                + "/tns:sachentscheidung/code")
                .stream().map(Node::getTextContent).toList();
    }
}
