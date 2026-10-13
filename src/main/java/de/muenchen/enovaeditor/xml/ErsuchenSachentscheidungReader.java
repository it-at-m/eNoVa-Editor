package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

public class ErsuchenSachentscheidungReader {
    private static final String XPATH = "/tns:nachricht.enova.entscheidung.2900003/tns:fachdaten/tns:auswahl_GegenstandDerNachricht/tns:ersuchenSachentscheidung";

    private static final String REQUEST_CODE_XPATH = "tns:ersuchenSachentscheidung/code";

    private final XPathReader xpathReader = new XPathReader();

    public List<Node> read(Document document) throws AppException {
        try {
            return xpathReader.findNodes(document, XPATH);
        } catch (XPathExpressionException exception) {
            throw new AppException(
                    "Die Ersuchen um Sachentscheidung konnten nicht aus der Nachricht gelesen werden.",
                    List.of(new ErrorDetail("XPath-Ausdruck", XPATH)),
                    exception
            );
        }
    }

    public String readRequestCode(Node request) throws AppException {
        try {
            return xpathReader.readValue(request, REQUEST_CODE_XPATH).trim();
        } catch (XPathExpressionException exception) {
            throw new AppException(
                    "Der Code des Ersuchens um Sachentscheidung konnte nicht gelesen werden.",
                    List.of(new ErrorDetail(
                            "XPath-Ausdruck",
                            REQUEST_CODE_XPATH
                    )),
                    exception
            );
        }
    }
}
