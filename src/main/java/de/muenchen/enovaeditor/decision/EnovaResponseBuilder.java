package de.muenchen.enovaeditor.decision;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class EnovaResponseBuilder {

    public static final String XJUSTIZ_NAMESPACE = "http://www.xjustiz.de";
    private static final String RESPONSE_ROOT = "nachricht.enova.entscheidung.2900004";
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public Document buildResponse(Document inputDoc, DecisionData decision) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document responseDoc = docBuilder.newDocument();

        XPath xpath = createXPath();

        // Root element
        Element root = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:" + RESPONSE_ROOT);
        root.setAttribute("xmlns:tns", XJUSTIZ_NAMESPACE);
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        responseDoc.appendChild(root);

        // 1. Nachrichtenkopf
        Element kopf = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:nachrichtenkopf");
        root.appendChild(kopf);

        Element nachrichtenId = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:nachrichtenID");
        nachrichtenId.setTextContent("ENOVA-RESP-" + UUID.randomUUID());
        kopf.appendChild(nachrichtenId);

        Element erstellungszeitpunkt = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:erstellungszeitpunkt");
        erstellungszeitpunkt.setTextContent(LocalDateTime.now().format(ISO_FORMAT));
        kopf.appendChild(erstellungszeitpunkt);

        Element hersteller = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:hersteller");
        hersteller.setTextContent("eNoVa-Editor");
        kopf.appendChild(hersteller);

        String inputMsgId = xpath.evaluate("//tns:nachrichtenkopf/tns:nachrichtenID", inputDoc);
        if (inputMsgId != null && !inputMsgId.isBlank()) {
            Element refMsg = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:referenzierteNachricht");
            refMsg.setTextContent(inputMsgId.trim());
            kopf.appendChild(refMsg);
        }

        // 2. Grunddaten
        Element grunddaten = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:grunddaten");
        root.appendChild(grunddaten);

        Element verfahrensdaten = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:verfahrensdaten");
        grunddaten.appendChild(verfahrensdaten);

        Element instanzdaten = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:instanzdaten");
        verfahrensdaten.appendChild(instanzdaten);

        String behoerdeName = xpath.evaluate("//tns:grunddaten/tns:verfahrensdaten/tns:instanzdaten/tns:auswahl_instanzbehoerde/tns:sonstige", inputDoc);
        if (behoerdeName == null || behoerdeName.isBlank()) {
            behoerdeName = "Gemeindeverwaltung";
        }
        Element instanzbehoerde = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:auswahl_instanzbehoerde");
        Element sonstigeBehoerde = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:sonstige");
        sonstigeBehoerde.setTextContent(behoerdeName.trim());
        instanzbehoerde.appendChild(sonstigeBehoerde);
        instanzdaten.appendChild(instanzbehoerde);

        Element verfahrensgegenstand = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:verfahrensgegenstand");
        Element gegenstand = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:gegenstand");
        gegenstand.setTextContent("Sachentscheidung Vorkaufsrecht: " + decision.decisionType().getShortTitle());
        verfahrensgegenstand.appendChild(gegenstand);
        instanzdaten.appendChild(verfahrensgegenstand);

        if (!decision.fileNumber().isBlank()) {
            Element azInstanz = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:aktenzeichenInstanzbehoerde");
            azInstanz.setTextContent(decision.fileNumber());
            instanzdaten.appendChild(azInstanz);
        }

        // Copy Beteiligungen (Notar, Verkäufer, Käufer) from input
        NodeList beteiligungen = (NodeList) xpath.evaluate("//tns:grunddaten/tns:beteiligung", inputDoc, XPathConstants.NODESET);
        for (int i = 0; i < beteiligungen.getLength(); i++) {
            Node imported = responseDoc.importNode(beteiligungen.item(i), true);
            grunddaten.appendChild(imported);
        }

        // 3. Fachdaten
        Element fachdaten = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:fachdaten");
        root.appendChild(fachdaten);

        Element auswahlGegenstand = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:auswahl_GegenstandDerNachricht");
        fachdaten.appendChild(auswahlGegenstand);

        Element sachentscheidung = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:sachentscheidung");
        auswahlGegenstand.appendChild(sachentscheidung);

        Element artDerEntscheidung = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:artDerEntscheidung");
        Element codeEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:code");
        codeEl.setTextContent(decision.decisionType().getCode());
        artDerEntscheidung.appendChild(codeEl);
        Element bezEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:bezeichnung");
        bezEl.setTextContent(decision.decisionType().getDisplayName());
        artDerEntscheidung.appendChild(bezEl);
        sachentscheidung.appendChild(artDerEntscheidung);

        Element tenorEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:tenor");
        tenorEl.setTextContent(decision.tenor());
        sachentscheidung.appendChild(tenorEl);

        Element rechtsgrundlageEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:rechtsgrundlage");
        rechtsgrundlageEl.setTextContent(decision.decisionType().getLegalBasis());
        sachentscheidung.appendChild(rechtsgrundlageEl);

        if (!decision.reasoning().isBlank()) {
            Element begruendungEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:begruendung");
            begruendungEl.setTextContent(decision.reasoning());
            sachentscheidung.appendChild(begruendungEl);
        }

        Element datumEntscheidungEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:datumDerEntscheidung");
        datumEntscheidungEl.setTextContent(decision.decisionDate().toString());
        sachentscheidung.appendChild(datumEntscheidungEl);

        Element kostenEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:kostenentscheidung");
        Element gebuehrEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:gebuehrenbetrag");
        gebuehrEl.setTextContent(decision.feeAmount());
        kostenEl.appendChild(gebuehrEl);
        sachentscheidung.appendChild(kostenEl);

        // Copy beschreibungDesBetroffenenRechts (Flurstücke, Grundbuchblätter) from input
        NodeList betroffeneRechte = (NodeList) xpath.evaluate("//tns:fachdaten/tns:beschreibungDesBetroffenenRechts", inputDoc, XPathConstants.NODESET);
        for (int i = 0; i < betroffeneRechte.getLength(); i++) {
            Node imported = responseDoc.importNode(betroffeneRechte.item(i), true);
            fachdaten.appendChild(imported);
        }

        // 4. Absender (Behörde & Sachbearbeiter)
        Element absender = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:absender");
        root.appendChild(absender);

        if (!decision.fileNumber().isBlank()) {
            Element azAbsender = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:aktenzeichen");
            azAbsender.setTextContent(decision.fileNumber());
            absender.appendChild(azAbsender);
        }

        Element infoAbsender = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:informationen");
        Element kpAbsender = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:auswahl_kommunikationspartner");
        Element sonstigeKp = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:sonstige");
        sonstigeKp.setTextContent(behoerdeName.trim());
        kpAbsender.appendChild(sonstigeKp);
        infoAbsender.appendChild(kpAbsender);
        absender.appendChild(infoAbsender);

        if (!decision.caseworkerName().isBlank()) {
            Element bearbeiterEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:bearbeiter");
            Element bearbeiterName = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:name");
            bearbeiterName.setTextContent(decision.caseworkerName());
            bearbeiterEl.appendChild(bearbeiterName);

            if (!decision.caseworkerPhone().isBlank()) {
                Element bearbeiterTel = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:telefon");
                bearbeiterTel.setTextContent(decision.caseworkerPhone());
                bearbeiterEl.appendChild(bearbeiterTel);
            }
            absender.appendChild(bearbeiterEl);
        }

        // 5. Urkundendaten (UR-Nr. and Datum aus Anfrage)
        String urNr = xpath.evaluate("//tns:datenDerUrkunde/tns:urNr", inputDoc);
        String urDatum = xpath.evaluate("//tns:datenDerUrkunde/tns:urkundsdatum", inputDoc);
        if ((urNr != null && !urNr.isBlank()) || (urDatum != null && !urDatum.isBlank())) {
            Element urkundeEl = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:datenDerUrkunde");
            if (urNr != null && !urNr.isBlank()) {
                Element nr = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:urNr");
                nr.setTextContent(urNr.trim());
                urkundeEl.appendChild(nr);
            }
            if (urDatum != null && !urDatum.isBlank()) {
                Element datum = responseDoc.createElementNS(XJUSTIZ_NAMESPACE, "tns:urkundsdatum");
                datum.setTextContent(urDatum.trim());
                urkundeEl.appendChild(datum);
            }
            root.appendChild(urkundeEl);
        }

        return responseDoc;
    }

    private XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("tns".equals(prefix)) {
                    return XJUSTIZ_NAMESPACE;
                }
                return javax.xml.XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if (XJUSTIZ_NAMESPACE.equals(namespaceURI)) {
                    return "tns";
                }
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                if (XJUSTIZ_NAMESPACE.equals(namespaceURI)) {
                    return Set.of("tns").iterator();
                }
                return Collections.emptyIterator();
            }
        });
        return xpath;
    }
}
