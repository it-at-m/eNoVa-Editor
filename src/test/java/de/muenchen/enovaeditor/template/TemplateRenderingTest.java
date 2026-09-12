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
}
