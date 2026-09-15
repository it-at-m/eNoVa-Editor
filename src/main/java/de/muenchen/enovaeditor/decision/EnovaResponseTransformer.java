package de.muenchen.enovaeditor.decision;

import de.muenchen.enovaeditor.config.caseworker.CaseworkerEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnovaResponseTransformer {

    private static final String XJUSTIZ_NAMESPACE = "http://www.xjustiz.de";
    private static final String TNS_PREFIX = "tns";

    public Document transform(
            Document sourceDocument,
            String decisionCode,
            String fileNumber,
            String senderName,
            CaseworkerEntry caseworker
    ) {
        if (sourceDocument == null) {
            throw new IllegalArgumentException("Quell-Dokument darf nicht null sein.");
        }
        if (decisionCode == null || decisionCode.isBlank()) {
            throw new IllegalArgumentException("Entscheidungscode darf nicht leer sein.");
        }

        // Clone document to preserve original
        Document responseDoc = (Document) sourceDocument.cloneNode(true);
        Element root = responseDoc.getDocumentElement();

        // 1. Update Erstellungszeitpunkt in nachrichtenkopf
        updateCreationTimestamp(responseDoc, root);

        // 2. Transform ErsuchenSachentscheidung -> Sachentscheidung in fachdaten
        transformFachdatenGegenstand(responseDoc, root, decisionCode);

        // 3. Swap Absender and Empfaenger
        swapSenderAndRecipient(responseDoc, root, fileNumber, senderName, caseworker);

        return responseDoc;
    }

    private void updateCreationTimestamp(Document doc, Element root) {
        Element kopf = findDirectChild(root, "nachrichtenkopf");
        if (kopf == null) {
            kopf = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":nachrichtenkopf");
            root.insertBefore(kopf, root.getFirstChild());
        }

        Element zeitpunkt = findDirectChild(kopf, "erstellungszeitpunkt");
        if (zeitpunkt == null) {
            zeitpunkt = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":erstellungszeitpunkt");
            kopf.appendChild(zeitpunkt);
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        zeitpunkt.setTextContent(now);
    }

    private void transformFachdatenGegenstand(Document doc, Element root, String decisionCode) {
        Element fachdaten = findDirectChild(root, "fachdaten");
        if (fachdaten == null) {
            throw new IllegalStateException("Nachricht enthält keinen fachdaten-Block.");
        }

        Element gegenstand = findDirectChild(fachdaten, "auswahl_GegenstandDerNachricht");
        if (gegenstand == null) {
            gegenstand = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":auswahl_GegenstandDerNachricht");
            fachdaten.insertBefore(gegenstand, fachdaten.getFirstChild());
        }

        // Remove any existing ersuchenSachentscheidung or sachentscheidung
        Element ersuchen = findDirectChild(gegenstand, "ersuchenSachentscheidung");
        if (ersuchen != null) {
            gegenstand.removeChild(ersuchen);
        }
        Element altSachentscheidung = findDirectChild(gegenstand, "sachentscheidung");
        if (altSachentscheidung != null) {
            gegenstand.removeChild(altSachentscheidung);
        }

        // Create new <tns:sachentscheidung><tns:sachentscheidung><code>...</code></tns:sachentscheidung></tns:sachentscheidung>
        Element sachentscheidungWrapper = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":sachentscheidung");
        Element sachentscheidungInner = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":sachentscheidung");
        Element codeElement = doc.createElement("code");
        codeElement.setTextContent(decisionCode);

        sachentscheidungInner.appendChild(codeElement);
        sachentscheidungWrapper.appendChild(sachentscheidungInner);
        gegenstand.appendChild(sachentscheidungWrapper);
    }

    private void swapSenderAndRecipient(
            Document doc,
            Element root,
            String fileNumber,
            String senderName,
            CaseworkerEntry caseworker
    ) {
        Element originalSender = findDirectChild(root, "absender");
        Element originalRecipient = findDirectChild(root, "empfaenger");

        // The incoming absender (notary) becomes the new empfaenger
        if (originalSender != null) {
            Element newEmpfaenger = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":empfaenger");
            // Move children of absender to empfaenger
            Node child = originalSender.getFirstChild();
            while (child != null) {
                Node next = child.getNextSibling();
                newEmpfaenger.appendChild(child);
                child = next;
            }
            if (originalRecipient != null) {
                root.replaceChild(newEmpfaenger, originalRecipient);
            } else {
                root.appendChild(newEmpfaenger);
            }
            root.removeChild(originalSender);
        }

        // Create new absender for the municipality
        Element newAbsender = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":absender");

        if (fileNumber != null && !fileNumber.isBlank()) {
            Element az = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":aktenzeichen");
            az.setTextContent(fileNumber.trim());
            newAbsender.appendChild(az);
        }

        Element info = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":informationen");

        // If caseworker XML snippet exists, attach caseworker as Beteiligter if possible
        boolean attachedCaseworker = false;
        if (caseworker != null && caseworker.xmlBlock() != null && !caseworker.xmlBlock().isBlank()) {
            try {
                int nextRollennummer = getNextRollennummer(root);
                attachCaseworkerBeteiligter(doc, root, caseworker.xmlBlock(), nextRollennummer);

                Element verweis = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":auswahl_verweisGrunddaten");
                Element refRolle = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":ref.rollennummer");
                refRolle.setTextContent(String.valueOf(nextRollennummer));
                verweis.appendChild(refRolle);
                info.appendChild(verweis);
                attachedCaseworker = true;
            } catch (Exception ignored) {
                // Fallback to sonstige
            }
        }

        Element partner = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":auswahl_kommunikationspartner");
        Element sonstige = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":sonstige");
        sonstige.setTextContent(senderName != null && !senderName.isBlank() ? senderName : "Kommune / Bauamt");
        partner.appendChild(sonstige);
        info.appendChild(partner);

        newAbsender.appendChild(info);
        root.appendChild(newAbsender);
    }

    private int getNextRollennummer(Element root) {
        int max = 0;
        NodeList list = root.getElementsByTagNameNS("*", "rollennummer");
        for (int i = 0; i < list.getLength(); i++) {
            try {
                int val = Integer.parseInt(list.item(i).getTextContent().trim());
                if (val > max) {
                    max = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max + 1;
    }

    private void attachCaseworkerBeteiligter(Document doc, Element root, String xmlSnippet, int rollennummer) throws Exception {
        Element grunddaten = findDirectChild(root, "grunddaten");
        if (grunddaten == null) {
            return;
        }

        // Parse snippet
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        String wrapped = "<root xmlns:tns=\"http://www.xjustiz.de\">" + xmlSnippet + "</root>";
        Document snippetDoc = db.parse(new InputSource(new StringReader(wrapped)));

        Element snippetRoot = snippetDoc.getDocumentElement();
        Node auswahl = snippetRoot.getFirstChild();
        while (auswahl != null && auswahl.getNodeType() != Node.ELEMENT_NODE) {
            auswahl = auswahl.getNextSibling();
        }

        if (auswahl != null) {
            Element beteiligung = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":beteiligung");
            Element rolle = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":rolle");
            Element num = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":rollennummer");
            num.setTextContent(String.valueOf(rollennummer));
            rolle.appendChild(num);
            beteiligung.appendChild(rolle);

            Element beteiligter = doc.createElementNS(XJUSTIZ_NAMESPACE, TNS_PREFIX + ":beteiligter");
            Node importedAuswahl = doc.importNode(auswahl, true);
            beteiligter.appendChild(importedAuswahl);
            beteiligung.appendChild(beteiligter);

            grunddaten.appendChild(beteiligung);
        }
    }

    public String documentToXmlString(Document document) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML-Dokument konnte nicht serialisiert werden.", e);
        }
    }

    private Element findDirectChild(Element parent, String localName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                if (localName.equals(el.getLocalName()) || localName.equals(el.getNodeName())) {
                    return el;
                }
            }
        }
        return null;
    }
}
