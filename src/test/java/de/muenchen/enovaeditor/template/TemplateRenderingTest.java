package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.xml.ErsuchenSachentscheidungChecker;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateRenderingTest {

    @Test
    void testSampleXmlRendersSuccessfully() throws Exception {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        assertTrue(sampleFile.exists(), "Sample file should exist");

        XmlLoader xmlLoader = new XmlLoader();
        Document doc = xmlLoader.load(sampleFile);
        assertNotNull(doc);

        ErsuchenSachentscheidungChecker checker = new ErsuchenSachentscheidungChecker();
        assertDoesNotThrow(() -> checker.check(doc));

        String template = Files.readString(Path.of("Input.htm"));
        XPathTemplateRenderer renderer = new XPathTemplateRenderer();
        String renderedHtml = renderer.render(template, doc);

        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("Johannes"), "Should contain Notar first name");
        assertTrue(renderedHtml.contains("Beispiel"), "Should contain Notar last name");
        assertTrue(renderedHtml.contains("Erika"), "Should contain seller name");
        assertTrue(renderedHtml.contains("Schmidt"), "Should contain buyer name");
        assertTrue(renderedHtml.contains("Schwabing"), "Should contain Grundbuchbezirk");
        assertTrue(renderedHtml.contains("412/5"), "Should contain Flurstücksnummer");
        assertTrue(renderedHtml.contains("Dr. Johannes Beispiel"), "Should contain full Notar name with space");
        assertTrue(renderedHtml.contains("Erika Mustermann"), "Should contain full seller name with space");
        assertTrue(renderedHtml.contains("Maximilian Schmidt"), "Should contain full buyer name with space");
        assertFalse(renderedHtml.contains("Johannes|"), "Should not contain pipe glyph");
        assertFalse(renderedHtml.contains("Erika|"), "Should not contain pipe glyph");
    }

    @Test
    void testOutputTemplateRendersSuccessfully() throws Exception {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        XmlLoader xmlLoader = new XmlLoader();
        Document doc = xmlLoader.load(sampleFile);

        de.muenchen.enovaeditor.decision.EnovaResponseTransformer transformer =
                new de.muenchen.enovaeditor.decision.EnovaResponseTransformer();
        Document responseDoc = transformer.transform(
                doc,
                "002",
                "61-VKR-2026/0815",
                "Landeshauptstadt München",
                null
        );

        String outputTemplate = Files.readString(Path.of("Output.htm"));
        XPathTemplateRenderer renderer = new XPathTemplateRenderer();
        String renderedHtml = renderer.render(outputTemplate, responseDoc);

        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("BESCHEID"), "Should contain BESCHEID header");
        assertTrue(renderedHtml.contains("61-VKR-2026/0815"), "Should contain Aktenzeichen");
        assertTrue(renderedHtml.contains("815 / 2026"), "Should contain Urkunden-Nr");
        assertTrue(renderedHtml.contains("Schwabing"), "Should contain Grundbuchbezirk");
        assertTrue(renderedHtml.contains("412/5"), "Should contain Flurstück");
    }

    @Test
    void testOutputTemplateRendersCaseworkerSuccessfully() throws Exception {
        File sampleFile = new File("samples/xjustiz_beispiel_2900003.xml");
        XmlLoader xmlLoader = new XmlLoader();
        Document doc = xmlLoader.load(sampleFile);

        de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader loader =
                new de.muenchen.enovaeditor.config.caseworker.CaseworkerConfigLoader();
        var caseworkers = loader.load();
        assertFalse(caseworkers.isEmpty(), "Caseworkers should be loaded from caseworkers.xml");
        var antonia = caseworkers.stream().filter(c -> "Antonia".equals(c.name())).findFirst().orElseThrow();

        de.muenchen.enovaeditor.decision.EnovaResponseTransformer transformer =
                new de.muenchen.enovaeditor.decision.EnovaResponseTransformer();
        Document responseDoc = transformer.transform(
                doc,
                "001",
                "AZ-2026-ANTONIA",
                "Gemeinde Hinterhugeldapfing",
                antonia
        );

        String outputTemplate = Files.readString(Path.of("Output.htm"));
        XPathTemplateRenderer renderer = new XPathTemplateRenderer();
        String renderedHtml = renderer.render(outputTemplate, responseDoc);

        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("Antonia"), "Should contain caseworker vorname Antonia");
        assertTrue(renderedHtml.contains("Allenthaler"), "Should contain caseworker nachname Allenthaler");
        assertTrue(renderedHtml.contains("Gemeinde Hinterhugeldapfing"), "Should contain municipality name");
        assertTrue(renderedHtml.contains("vorkaufsrecht@hinterhugeldapfing.de"), "Should contain caseworker email");
    }
}
