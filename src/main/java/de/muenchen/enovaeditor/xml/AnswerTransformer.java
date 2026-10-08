package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import net.sf.saxon.TransformerFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AnswerTransformer {

    private static final String RULE_FILE = "answer-transform.xsl";

    public Document transform(Document document, AnswerParameters parameters) throws IOException, TransformerException {
        Path xsltPath = ApplicationFileUtil.resolveReadableFile(RULE_FILE);

        StreamSource xsltSource = new StreamSource(xsltPath.toFile());

        Transformer transformer = createTransformer(xsltSource);

        DOMSource domSource = new DOMSource(document);
        DOMResult domResult = new DOMResult();

        transformer.setParameter("fileNumber", parameters.fileNumber());
        transformer.setParameter("messageId", parameters.messageId());
        transformer.setParameter("caseworkerName", parameters.caseworkerName());
        transformer.setParameter("decisionCodes", parameters.decisionCodes());

        transformer.setParameter("productName", parameters.manufacturerInfo().productName());
        transformer.setParameter("manufacturerName", parameters.manufacturerInfo().manufacturerName());
        transformer.setParameter("version", parameters.manufacturerInfo().version());


        transformer.transform(domSource, domResult);

        Node resultNode = domResult.getNode();

        if (resultNode.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document) resultNode;
        }

        throw new IllegalStateException(
                "Die Antwort konnte nicht korrekt erstellt werden."
        );
    }

    private Transformer createTransformer(StreamSource xsltSource)
            throws TransformerException {

        TransformerFactory transformerFactory =
                new TransformerFactoryImpl();

        List<TransformerException> compilationErrors =
                new ArrayList<>();

        transformerFactory.setErrorListener(new ErrorListener() {

            @Override
            public void warning(TransformerException exception) {
                // Warnungen führen nicht zum Abbruch.
            }

            @Override
            public void error(TransformerException exception) {
                compilationErrors.add(exception);
            }

            @Override
            public void fatalError(TransformerException exception) {
                compilationErrors.add(exception);
            }
        });

        try {
            return transformerFactory.newTransformer(xsltSource);

        } catch (TransformerConfigurationException exception) {

            if (!compilationErrors.isEmpty()) {

                TransformerException compilationError =
                        compilationErrors.get(0);

                throw new TransformerConfigurationException(
                        compilationError.getMessage(),
                        compilationError.getLocator(),
                        exception
                );
            }

            throw exception;
        }
    }
}
