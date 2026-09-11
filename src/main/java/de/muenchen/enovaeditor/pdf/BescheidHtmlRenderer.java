package de.muenchen.enovaeditor.pdf;

import de.muenchen.enovaeditor.decision.DecisionData;
import org.w3c.dom.Document;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class BescheidHtmlRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String renderHtml(Document inputDoc, DecisionData decision) {
        XPath xpath = createXPath();

        String behoerde = safeEvaluate(xpath, "//tns:grunddaten/tns:verfahrensdaten/tns:instanzdaten/tns:auswahl_instanzbehoerde/tns:sonstige", inputDoc);
        if (behoerde.isBlank()) behoerde = "Gemeindeverwaltung";

        String notarTitel = safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='208']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:titel", inputDoc);
        String notarVorname = safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='208']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:vorname", inputDoc);
        String notarNachname = safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='208']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:nachname", inputDoc);
        String notarName = ((!notarTitel.isBlank() ? notarTitel + " " : "") + notarVorname + " " + notarNachname).trim();

        String urNr = safeEvaluate(xpath, "//tns:datenDerUrkunde/tns:urNr", inputDoc);
        String urDatum = safeEvaluate(xpath, "//tns:datenDerUrkunde/tns:urkundsdatum", inputDoc);
        String notarAz = safeEvaluate(xpath, "//tns:absender/tns:aktenzeichen", inputDoc);

        String verkaeufer = safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='168']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:vorname", inputDoc)
                + " " + safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='168']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:nachname", inputDoc);

        String kaeufer = safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='072']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:vorname", inputDoc)
                + " " + safeEvaluate(xpath, "//tns:beteiligung[tns:rolle/tns:rollenbezeichnung/code='072']/tns:beteiligter/tns:auswahl_beteiligter/tns:natuerlichePerson/tns:vollerName/tns:nachname", inputDoc);

        String grundbuch = safeEvaluate(xpath, "//tns:fachdaten/tns:beschreibungDesBetroffenenRechts/tns:bundeslandfremdesGrundbuchblatt/tns:grundbuchbezirk", inputDoc)
                + ", Blatt " + safeEvaluate(xpath, "//tns:fachdaten/tns:beschreibungDesBetroffenenRechts/tns:bundeslandfremdesGrundbuchblatt/tns:grundbuchblatt", inputDoc);

        String gemarkung = safeEvaluate(xpath, "//tns:identifikationFlurstueck/tns:gemarkungsschluessel", inputDoc);
        String zaehler = safeEvaluate(xpath, "//tns:identifikationFlurstueck/tns:flurstuecksnummer/tns:zaehler", inputDoc);
        String nenner = safeEvaluate(xpath, "//tns:identifikationFlurstueck/tns:flurstuecksnummer/tns:nenner", inputDoc);
        String flurstueck = (!zaehler.isBlank() ? zaehler : "") + (!nenner.isBlank() ? "/" + nenner : "");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px auto; max-width: 800px; color: #1e293b; line-height: 1.6; background: #fff; }");
        html.append(".header-table { width: 100%; margin-bottom: 25px; border-bottom: 2px solid #004b76; padding-bottom: 15px; }");
        html.append(".header-left { vertical-align: top; text-align: left; width: 60%; }");
        html.append(".header-right { vertical-align: top; text-align: right; font-size: 13px; color: #475569; width: 40%; }");
        html.append(".behoerde-title { font-size: 18px; font-weight: bold; color: #004b76; margin: 0 0 4px 0; }");
        html.append(".title-box { text-align: center; margin: 25px 0; }");
        html.append(".title-main { font-size: 18px; font-weight: bold; color: #0b2a3d; margin-bottom: 4px; }");
        html.append(".title-sub { font-size: 13px; color: #64748b; font-weight: bold; }");
        html.append(".data-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; font-size: 14px; }");
        html.append(".data-table td { padding: 8px 12px; border: 1px solid #e2e8f0; }");
        html.append(".data-table td.label { background-color: #f8fafc; font-weight: bold; width: 35%; color: #334155; }");
        html.append(".tenor-box { background-color: #f0f7ff; border-left: 4px solid #009ee3; padding: 16px 20px; border-radius: 4px; margin: 20px 0; font-size: 15px; font-weight: 600; color: #0b2a3d; line-height: 1.7; }");
        html.append(".section-h { font-size: 15px; font-weight: bold; color: #0b2a3d; margin: 20px 0 8px 0; }");
        html.append(".sign-box { margin-top: 40px; display: flex; justify-content: space-between; font-size: 13px; }");
        html.append(".seal-note { font-size: 11px; color: #94a3b8; margin-top: 40px; text-align: center; border-top: 1px dashed #cbd5e1; padding-top: 12px; }");
        html.append("</style></head><body>");

        // Header
        html.append("<table class='header-table'><tr>");
        html.append("<td class='header-left'>");
        html.append("<div class='behoerde-title'>").append(escape(behoerde)).append("</div>");
        html.append("<div>Fachbereich Liegenschaften / Bauordnung</div>");
        html.append("</td>");
        html.append("<td class='header-right'>");
        html.append("<strong>Az: ").append(escape(decision.fileNumber().isBlank() ? "–" : decision.fileNumber())).append("</strong><br>");
        if (!decision.caseworkerName().isBlank()) {
            html.append("Sachbearbeitung: ").append(escape(decision.caseworkerName())).append("<br>");
        }
        if (!decision.caseworkerPhone().isBlank()) {
            html.append("Telefon: ").append(escape(decision.caseworkerPhone())).append("<br>");
        }
        html.append("Datum: ").append(decision.decisionDate().format(DATE_FMT));
        html.append("</td></tr></table>");

        // Title
        html.append("<div class='title-box'>");
        html.append("<div class='title-main'>").append(escape(decision.decisionType().getDisplayName())).append("</div>");
        html.append("<div class='title-sub'>Rechtsgrundlage: ").append(escape(decision.decisionType().getLegalBasis())).append("</div>");
        html.append("</div>");

        // Table
        html.append("<table class='data-table'>");
        html.append("<tr><td class='label'>Notariat / Notar:</td><td>").append(escape(notarName)).append("</td></tr>");
        html.append("<tr><td class='label'>Urkunden-Nr. / Datum:</td><td>").append(escape(urNr)).append(urDatum.isBlank() ? "" : " vom " + escape(urDatum)).append("</td></tr>");
        if (!notarAz.isBlank()) {
            html.append("<tr><td class='label'>Aktenzeichen Notar:</td><td>").append(escape(notarAz)).append("</td></tr>");
        }
        html.append("<tr><td class='label'>Veräußerer:</td><td>").append(escape(verkaeufer.trim())).append("</td></tr>");
        html.append("<tr><td class='label'>Erwerber:</td><td>").append(escape(kaeufer.trim())).append("</td></tr>");
        html.append("<tr><td class='label'>Grundbuchbezirk / Blatt:</td><td>").append(escape(grundbuch)).append("</td></tr>");
        html.append("<tr><td class='label'>Flurstück / Gemarkung:</td><td>").append(escape(flurstueck)).append(!gemarkung.isBlank() ? " (Gemarkung " + escape(gemarkung) + ")" : "").append("</td></tr>");
        html.append("</table>");

        // Tenor
        html.append("<div class='section-h'>I. Sachentscheidung (Bescheinigung)</div>");
        html.append("<div class='tenor-box'>").append(escape(decision.tenor())).append("</div>");

        // Begründung
        if (!decision.reasoning().isBlank()) {
            html.append("<div class='section-h'>II. Begründung</div>");
            html.append("<p>").append(escape(decision.reasoning())).append("</p>");
        }

        // Kosten
        html.append("<div class='section-h'>III. Kostenentscheidung</div>");
        html.append("<p>Die Gebühr für diese Amtshandlung wird festgesetzt auf: <strong>").append(escape(decision.feeAmount())).append("</strong>.</p>");

        // Rechtsbehelf
        html.append("<div class='section-h'>IV. Rechtsbehelfsbelehrung</div>");
        html.append("<p style='font-size:13px; color:#475569;'>Gegen diesen Bescheid kann innerhalb eines Monats nach Bekanntgabe Klage beim örtlich zuständigen Verwaltungsgericht erhoben werden.</p>");

        // Signature
        html.append("<table style='width:100%; margin-top:35px;'><tr>");
        html.append("<td><span style='color:#94a3b8; font-size:11px;'>[Dienstsiegel / Behördennachweis]</span></td>");
        html.append("<td style='text-align:right;'>Im Auftrag<br><br><br><strong>").append(escape(decision.caseworkerName().isBlank() ? "Sachbearbeitung" : decision.caseworkerName())).append("</strong></td>");
        html.append("</tr></table>");

        html.append("<div class='seal-note'>Hinweis zur elektronischen Form (eNoVA-Gesetz / BNotK): Für die Einreichung beim Grundbuchamt (§§ 29, 137 GBO) gilt der über das beBPo übermittelte strukturierte XJustiz-Datensatz mit qualifizierter elektronischer Signatur (CAdES detached).</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String safeEvaluate(XPath xpath, String expr, Document doc) {
        try {
            String val = xpath.evaluate(expr, doc);
            return val != null ? val.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("tns".equals(prefix)) {
                    return "http://www.xjustiz.de";
                }
                return javax.xml.XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if ("http://www.xjustiz.de".equals(namespaceURI)) {
                    return "tns";
                }
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                if ("http://www.xjustiz.de".equals(namespaceURI)) {
                    return Set.of("tns").iterator();
                }
                return Collections.emptyIterator();
            }
        });
        return xpath;
    }
}
