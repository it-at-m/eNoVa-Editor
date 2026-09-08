package de.muenchen.enovaeditor.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ErsuchenSachentscheidungChecker {

    private static final String XJUSTIZ_NAMESPACE =
            "http://www.xjustiz.de";

    private static final String EXPECTED_ROOT =
            "nachricht.enova.entscheidung.2900003";

    public void check(Document document) {

        Element root = document.getDocumentElement();

        if (!XJUSTIZ_NAMESPACE.equals(root.getNamespaceURI())) {
            throw new IllegalArgumentException(
                    "Die ausgewählte Datei ist keine XJustiz-Nachricht."
            );
        }

        if (!EXPECTED_ROOT.equals(root.getLocalName())) {
            throw new IllegalArgumentException(
                    "Die ausgewählte Datei ist keine eNoVa-Nachricht 2900003."
            );
        }

        Element fachdaten =
                getDirectChild(root, "fachdaten");

        if (fachdaten == null) {
            throw new IllegalArgumentException(
                    "Die Nachricht enthält keine eNoVa-Fachdaten."
            );
        }

        Element gegenstand =
                getDirectChild(
                        fachdaten,
                        "auswahl_GegenstandDerNachricht"
                );

        if (gegenstand == null) {
            throw new IllegalArgumentException(
                    "Die Nachricht enthält keinen Gegenstand der Nachricht."
            );
        }

        Element ersuchen =
                getDirectChild(
                        gegenstand,
                        "ersuchenSachentscheidung"
                );

        if (ersuchen == null) {
            throw new IllegalArgumentException(
                    "Die ausgewählte Nachricht ist kein Ersuchen um Sachentscheidung."
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
