package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import net.sf.saxon.TransformerFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.nio.file.Path;

public class AnswerTransformer {

    private static final String RULE_FILE = "answer-transform.xsl";

    public Document transform(Document document) throws IOException, TransformerException {
        Path xsltPath = ApplicationFileUtil.resolveReadableFile(RULE_FILE);

        StreamSource xsltSource = new StreamSource(xsltPath.toFile());

        TransformerFactory transformerFactory = new TransformerFactoryImpl();
        Transformer transformer = transformerFactory.newTransformer(xsltSource);

        DOMSource domSource = new DOMSource(document);
        DOMResult domResult = new DOMResult();
        transformer.transform(domSource, domResult);

        Node resultNode = domResult.getNode();

        if (resultNode.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document) resultNode;
        }

        throw new TransformerException(
                "Die Antwort konnte nicht erzeugt werden, weil kein XML-Dokument erzeugt wurde."
        );
    }
}
