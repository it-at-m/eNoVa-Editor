package de.muenchen.enovaeditor.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

public class ErsuchenSachentscheidungReader {
    private static final String XPATH = "/tns:nachricht.enova.entscheidung.2900003/tns:fachdaten/tns:auswahl_GegenstandDerNachricht/tns:ersuchenSachentscheidung";

    private static final String REQUEST_CODE_XPATH = "tns:ersuchenSachentscheidung/code";

    private final XPathReader xpathReader = new XPathReader();

    public List<Node> read(Document document) throws XPathExpressionException {
        return xpathReader.findNodes(document, XPATH);
    }

    public String readRequestCode(Node request) throws XPathExpressionException {
        return xpathReader.readValue(request, REQUEST_CODE_XPATH).trim();
    }
}
