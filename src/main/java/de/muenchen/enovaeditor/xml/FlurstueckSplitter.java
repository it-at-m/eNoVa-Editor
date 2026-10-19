package de.muenchen.enovaeditor.xml;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

public class FlurstueckSplitter {

    private final XPathReader xPathReader = new XPathReader();

    private static final String FLURSTUECKE_IM_DOKUMENT_XPATH =
            "/tns:nachricht.enova.entscheidung.2900003"
                    + "/tns:fachdaten"
                    + "/tns:beschreibungDesBetroffenenRechts"
                    + "/tns:buchungsstelle"
                    + "/tns:auswahl_buchungsstelle"
                    + "/tns:grundstueck"
                    + "/tns:flurstuecke";

    private static final String FLURSTUECKE_IN_BESCHREIBUNG_XPATH =
            "tns:buchungsstelle"
                    + "/tns:auswahl_buchungsstelle"
                    + "/tns:grundstueck"
                    + "/tns:flurstuecke";

    private static final String BETROFFENES_RECHT_ANCESTOR_XPATH = "ancestor::tns:beschreibungDesBetroffenenRechts";

    private static final String BESCHREIBUNGEN_XPATH = "tns:beschreibungDesBetroffenenRechts";

    public int countFlurstuecke(Document document) throws AppException {
        return findFlurstuecke(document).size();
    }

    public List<Document> split(Document document) throws AppException {
        List<Node> flurstuecke = findFlurstuecke(document);
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < flurstuecke.size(); i++) {
            Document copy = (Document) document.cloneNode(true);
            retainOnlyFlurstueck(copy, i);
            documents.add(copy);
        }
        return documents;
    }

    private void retainOnlyFlurstueck(Document copy, int index) throws AppException {
        List<Node> flurstuecke = findFlurstuecke(copy);
        Node selectedFlurstueck = flurstuecke.get(index);
        Node beschreibungDesBetroffenenRechts = findBeschreibungDesBetroffenenRechts(selectedFlurstueck);
        Node fachdaten = beschreibungDesBetroffenenRechts.getParentNode();
        List<Node> beschreibungen = findBeschreibungen(fachdaten);

        for (Node beschreibung : beschreibungen) {
            if (beschreibung != beschreibungDesBetroffenenRechts) {
                fachdaten.removeChild(beschreibung);
            }
        }

        List<Node> flurstueckeInBeschreibung = findFlurstueckeInBeschreibung(beschreibungDesBetroffenenRechts);

        for (Node flurstueck : flurstueckeInBeschreibung) {
            if (flurstueck != selectedFlurstueck) {
                flurstueck.getParentNode().removeChild(flurstueck);
            }
        }
    }

    private Node findBeschreibungDesBetroffenenRechts(Node flurstueck) throws AppException {
        try {
            List<Node> beschreibungen = xPathReader.findNodes(flurstueck, BETROFFENES_RECHT_ANCESTOR_XPATH);
            if (beschreibungen.size() != 1) {
                throw new AppException(
                        "Für das Flurstück wurde nicht genau eine Beschreibung des betroffenen Rechts gefunden.",
                        List.of(new ErrorDetail(
                                "Anzahl gefundener Beschreibungen",
                                String.valueOf(beschreibungen.size())
                        ))
                );
            }
            return beschreibungen.getFirst();
        } catch (XPathExpressionException e) {
            throw new AppException(
                    "Die Beschreibung des betroffenen Rechts konnte nicht ermittelt werden.",
                    List.of(new ErrorDetail(
                            "XPath-Ausdruck",
                            BETROFFENES_RECHT_ANCESTOR_XPATH
                    )),
                    e
            );
        }
    }

    private List<Node> findBeschreibungen(Node fachdaten) throws AppException {
        try {
            return xPathReader.findNodes(fachdaten, BESCHREIBUNGEN_XPATH);
        } catch (XPathExpressionException e) {
            throw new AppException(
                    "Die Beschreibungen des betroffenen Rechts konnten nicht gelesen werden.",
                    List.of(new ErrorDetail("XPath-Ausdruck", BESCHREIBUNGEN_XPATH)),
                    e
            );
        }
    }

    private List<Node> findFlurstuecke(Document document) throws AppException {
        try {
            return xPathReader.findNodes(document, FLURSTUECKE_IM_DOKUMENT_XPATH);
        } catch (XPathExpressionException e) {
            throw new AppException(
                    "Die Flurstücke konnten nicht aus der Nachricht gelesen werden.",
                    List.of(new ErrorDetail("XPath-Ausdruck", FLURSTUECKE_IM_DOKUMENT_XPATH)),
                    e
            );
        }
    }

    private List<Node> findFlurstueckeInBeschreibung(Node beschreibung) throws AppException {
        try {
            return xPathReader.findNodes(beschreibung, FLURSTUECKE_IN_BESCHREIBUNG_XPATH);
        } catch (XPathExpressionException e) {
            throw new AppException(
                    "Die Flurstücke konnten nicht aus der Nachricht gelesen werden.",
                    List.of(new ErrorDetail("XPath-Ausdruck", FLURSTUECKE_IN_BESCHREIBUNG_XPATH)),
                    e
            );
        }
    }
}
