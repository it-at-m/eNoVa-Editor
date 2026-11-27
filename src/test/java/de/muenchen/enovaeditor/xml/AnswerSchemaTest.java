package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.config.decision.DecisionConfigLoader;
import de.muenchen.enovaeditor.config.manufacturer.ManufacturerInfoLoader;
import de.muenchen.enovaeditor.validation.SchemaValidator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerSchemaTest {

    private static final List<File> INPUTS = List.of(
            new File("demo-output/xjustiz_nachricht_beispiel_ersuchenSachentscheidung_mehrfach.xml"),
            new File("src/test/resources/messages/xjustiz_nachricht_beispiel_ohne_aktenzeichen.xml"),
            new File("src/test/resources/messages/xjustiz_nachricht_beispiel_alle_ersuchen.xml")
    );

    @TempDir
    Path tempDir;

    @TestFactory
    Stream<DynamicTest> inputAndAnswerAreSchemaValid() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        Map<String, List<String>> allowedDecisions = new DecisionConfigLoader().load();

        return INPUTS.stream().map(input -> DynamicTest.dynamicTest(input.getName(), () -> {
            assertEquals(List.of(), validate(validator, input));

            File answer = tempDir.resolve(input.getName().replace(".xml", "-Output.xml")).toFile();
            new XmlWriter().write(transform(input, allowedDecisions), answer);
            assertEquals(List.of(), validate(validator, answer));
        }));
    }

    // Wie im Editor, mit der ersten erlaubten Sachentscheidung je Ersuchen.
    private Document transform(File input, Map<String, List<String>> allowedDecisions) throws Exception {
        Document document = new XmlLoader().load(input);
        ErsuchenSachentscheidungReader reader = new ErsuchenSachentscheidungReader();
        List<DecisionAnswer> decisions = new ArrayList<>();
        for (Node request : reader.read(document)) {
            String code = reader.readRequestCode(request);
            decisions.add(new DecisionAnswer(code, code, allowedDecisions.get(code).get(0)));
        }
        return new AnswerTransformer().transform(document, new AnswerParameters(
                "AZ-110", UUID.randomUUID().toString(), new ManufacturerInfoLoader().load(),
                "Bürgermeister", new File("caseworkers.xml").toURI().toString(), decisions
        ));
    }

    private List<String> validate(SchemaValidator validator, File file) throws Exception {
        return validator.validate(file).errors().stream()
                .map(error -> "%s:%d:%d: %s: %s".formatted(
                        file, error.line(), error.column(), error.path(), error.message()))
                .toList();
    }
}
