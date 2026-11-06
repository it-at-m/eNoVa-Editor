package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfo;
import de.muenchen.enovaeditor.xml.AnswerParameters;
import de.muenchen.enovaeditor.xml.AnswerTransformer;
import de.muenchen.enovaeditor.xml.DecisionAnswer;
import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungReader;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAnswerTemplateTest {

    private static final File INPUT = new File(
            "demo-output/xjustiz_nachricht_beispiel_ersuchenSachentscheidung_mehrfach.xml"
    );

    private final XPathTemplateRenderer renderer = new XPathTemplateRenderer();
    private final TemplateLoader templateLoader = new TemplateLoader();

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

        String multiple = renderer.render(template, transform(input, List.of(
                new DecisionAnswer("013", "Dritte Frage", "021"),
                new DecisionAnswer("011", "Erste Frage", "023"))));
        assertEquals(2, count(multiple, "<li>"));
        assertTrue(multiple.indexOf("Dritte Frage (013)") < multiple.indexOf("Erste Frage (011)"));
        assertTrue(multiple.contains("(021)"));
        assertTrue(multiple.contains("(023)"));
    }

    private Document transform(Document input, List<DecisionAnswer> decisions) throws Exception {
        return new AnswerTransformer().transform(input, new AnswerParameters(
                "AZ-81", "message-81", new ManufacturerInfo("eNoVA", "München", "1.0"),
                "Bürgermeister", decisions
        ));
    }

    private int count(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
