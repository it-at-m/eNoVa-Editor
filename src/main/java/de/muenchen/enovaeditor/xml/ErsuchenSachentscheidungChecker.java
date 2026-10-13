package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.error.AppException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

public class ErsuchenSachentscheidungChecker {

    private static final String XJUSTIZ_NAMESPACE =
            "http://www.xjustiz.de";

    private static final String EXPECTED_ROOT =
            "nachricht.enova.entscheidung.2900003";

    public void check(Document document) throws AppException {

        Element root = document.getDocumentElement();

        if (!XJUSTIZ_NAMESPACE.equals(root.getNamespaceURI())) {
            throw new AppException(
                    "Die ausgewählte Datei ist keine XJustiz-Nachricht.",
                    List.of()
            );
        }

        if (!EXPECTED_ROOT.equals(root.getLocalName())) {
            throw new AppException(
                    "Die ausgewählte Datei ist keine eNoVA-Nachricht 2900003.",
                    List.of()
            );
        }

        Element fachdaten =
                getDirectChild(root, "fachdaten");

        if (fachdaten == null) {
            throw new AppException(
                    "Die Nachricht enthält keine eNoVA-Fachdaten.",
                    List.of()
            );
        }

        Element gegenstand =
                getDirectChild(
                        fachdaten,
                        "auswahl_GegenstandDerNachricht"
                );

        if (gegenstand == null) {
            throw new AppException(
                    "Die Nachricht enthält keinen Gegenstand der Nachricht.",
                    List.of()
            );
        }

        Element ersuchen =
                getDirectChild(
                        gegenstand,
                        "ersuchenSachentscheidung"
                );

        if (ersuchen == null) {
            throw new AppException(
                    "Die ausgewählte Nachricht ist kein Ersuchen um Sachentscheidung.",
                    List.of()
            );
        }
    }

    private Element getDirectChild(
            Element parent,
            String localName
    ) {

        for (Node node = parent.getFirstChild();
             node != null;
             node = node.getNextSibling()) {

            if (node instanceof Element element
                    && XJUSTIZ_NAMESPACE.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {

                return element;
            }
        }

        return null;
    }
}
