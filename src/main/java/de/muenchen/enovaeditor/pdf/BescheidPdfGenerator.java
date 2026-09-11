package de.muenchen.enovaeditor.pdf;

import de.muenchen.enovaeditor.decision.DecisionData;
import org.w3c.dom.Document;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Reiner, unabhängiger Java-PDF-Generator für amtliche Bescheide und Negativzeugnisse.
 * Erzeugt 100% standardkonforme PDF-1.4-Dokumente ohne externe Abhängigkeiten.
 */
public class BescheidPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void generatePdf(Document inputDoc, DecisionData decision, File outputFile) throws Exception {
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

        PdfBuilder pdf = new PdfBuilder();

        // 1. Kopfzeile
        // Linke Spalte (Behörde / Fachbereich) - strikt begrenzt auf max 265pt Breite (kein Überlappen mit Az-Block)
        float leftY = 785;
        leftY = pdf.drawWrappedText(behoerde, 50, leftY, 265, 11, true, 0.04f, 0.16f, 0.24f);
        pdf.drawText("Fachbereich Liegenschaften / Bauordnung", 50, leftY, 8.5f, false, 0.4f, 0.45f, 0.5f);
        leftY -= 12;

        // Rechte Spalte (Aktenzeichen, Sachbearbeitung, Kontakt, Datum) - rechtsbündig bei 545
        pdf.drawTextRight("Aktenzeichen: " + (decision.fileNumber().isBlank() ? "–" : decision.fileNumber()), 545, 785, 9.5f, true, 0f, 0f, 0f);
        float rightY = 772;
        if (!decision.caseworkerName().isBlank()) {
            pdf.drawTextRight("Sachbearbeitung: " + decision.caseworkerName(), 545, rightY, 8.5f, false, 0.2f, 0.2f, 0.2f);
            rightY -= 12;
        }
        if (!decision.caseworkerPhone().isBlank()) {
            pdf.drawTextRight("Telefon: " + decision.caseworkerPhone(), 545, rightY, 8.5f, false, 0.2f, 0.2f, 0.2f);
            rightY -= 12;
        }
        pdf.drawTextRight("Datum: " + decision.decisionDate().format(DATE_FMT), 545, rightY, 8.5f, false, 0.2f, 0.2f, 0.2f);
        rightY -= 12;

        // Trennlinie sauber unter beiden Spalten
        float lineY = Math.min(Math.min(leftY, rightY) - 6, 735);
        pdf.drawLine(50, lineY, 545, lineY, 1.5f, 0f, 0.47f, 0.71f);

        // 2. Titel & Rechtsgrundlage
        float curY = lineY - 20;
        curY = pdf.drawWrappedTextCenter(decision.decisionType().getDisplayName(), curY, 480, 12, true, 0.04f, 0.16f, 0.24f);
        curY -= 2;
        pdf.drawTextCenter("Rechtsgrundlage: " + decision.decisionType().getLegalBasis(), curY, 8.5f, false, 0.35f, 0.4f, 0.45f);
        curY -= 20;

        // 3. Tabelle mit Verfahrensdaten
        pdf.drawTableRow("Notariat / Notar:", notarName.isEmpty() ? "–" : notarName, 50, curY);
        curY -= 17;
        pdf.drawTableRow("Urkunden-Nr. / Datum:", (urNr.isEmpty() ? "–" : urNr) + (urDatum.isEmpty() ? "" : " vom " + urDatum), 50, curY);
        curY -= 17;
        if (!notarAz.isBlank()) {
            pdf.drawTableRow("Aktenzeichen Notar:", notarAz, 50, curY);
            curY -= 17;
        }
        pdf.drawTableRow("Veräußerer (Beteiligter):", verkaeufer.trim().isEmpty() ? "–" : verkaeufer.trim(), 50, curY);
        curY -= 17;
        pdf.drawTableRow("Erwerber (Beteiligter):", kaeufer.trim().isEmpty() ? "–" : kaeufer.trim(), 50, curY);
        curY -= 17;
        pdf.drawTableRow("Grundbuchbezirk / Blatt:", grundbuch.equals(", Blatt ") ? "–" : grundbuch, 50, curY);
        curY -= 17;
        pdf.drawTableRow("Flurstück / Gemarkung:", (flurstueck.isEmpty() ? "–" : flurstueck) + (!gemarkung.isEmpty() ? " (Gemarkung " + gemarkung + ")" : ""), 50, curY);
        curY -= 22;

        // 4. Sachentscheidung (Tenor-Box)
        pdf.drawText("I. Sachentscheidung (Bescheinigung)", 50, curY, 10.5f, true, 0.04f, 0.16f, 0.24f);
        curY -= 14;

        // Box zeichnen mit dynamischer Höhe basierend auf Tenor-Text
        int tenorLines = Math.max(1, (int) Math.ceil((decision.tenor().length() * 9.5f * 0.52f) / 470f));
        float boxHeight = Math.max(38, tenorLines * 13 + 18);
        pdf.drawRect(50, curY - boxHeight + 12, 495, boxHeight, 0.94f, 0.97f, 1.0f, true);
        pdf.drawLine(50, curY - boxHeight + 12, 50, curY + 12, 3.5f, 0f, 0.62f, 0.89f);
        pdf.drawWrappedText(decision.tenor(), 60, curY - 2, 475, 9.5f, true, 0.04f, 0.16f, 0.24f);
        curY -= (boxHeight + 14);

        // 5. Begründung
        if (!decision.reasoning().isBlank()) {
            pdf.drawText("II. Begründung", 50, curY, 11, true, 0.04f, 0.16f, 0.24f);
            curY -= 14;
            curY = pdf.drawWrappedText(decision.reasoning(), 50, curY, 495, 9.5f, false, 0.15f, 0.15f, 0.15f);
            curY -= 12;
        }

        // 6. Kostenentscheidung
        pdf.drawText("III. Kostenentscheidung", 50, curY, 11, true, 0.04f, 0.16f, 0.24f);
        curY -= 14;
        pdf.drawText("Die Gebühr für diese Amtshandlung wird festgesetzt auf: " + decision.feeAmount() + ".", 50, curY, 9.5f, false, 0.15f, 0.15f, 0.15f);
        curY -= 20;

        // 7. Rechtsbehelfsbelehrung
        pdf.drawText("IV. Rechtsbehelfsbelehrung", 50, curY, 11, true, 0.04f, 0.16f, 0.24f);
        curY -= 14;
        pdf.drawText("Gegen diesen Bescheid kann innerhalb eines Monats nach Bekanntgabe Klage beim örtlich", 50, curY, 8.5f, false, 0.35f, 0.35f, 0.35f);
        curY -= 11;
        pdf.drawText("zuständigen Verwaltungsgericht erhoben werden.", 50, curY, 8.5f, false, 0.35f, 0.35f, 0.35f);
        curY -= 35;

        // 8. Signaturzeile
        pdf.drawText("[Dienstsiegel / Behördennachweis]", 50, curY, 8, false, 0.6f, 0.6f, 0.6f);
        pdf.drawTextRight("Im Auftrag", 545, curY + 10, 9.5f, false, 0f, 0f, 0f);
        pdf.drawTextRight("___________________________", 545, curY - 15, 9.5f, false, 0.5f, 0.5f, 0.5f);
        pdf.drawTextRight(decision.caseworkerName().isBlank() ? "Sachbearbeitung" : decision.caseworkerName(), 545, curY - 28, 9.5f, true, 0f, 0f, 0f);

        // 9. Fußnote eNoVA / BNotK
        pdf.drawLine(50, 60, 545, 60, 0.5f, 0.8f, 0.8f, 0.8f);
        pdf.drawTextCenter("Hinweis: Dieses Dokument dient der behördlichen Veraktung. Für die Einreichung beim Grundbuchamt (§§ 29, 137 GBO)", 48, 7.5f, false, 0.5f, 0.55f, 0.6f);
        pdf.drawTextCenter("gilt der über das beBPo übermittelte strukturierte XJustiz-Datensatz mit qualifizierter elektronischer Signatur (CAdES detached).", 38, 7.5f, false, 0.5f, 0.55f, 0.6f);

        byte[] pdfBytes = pdf.build();
        try (OutputStream os = new FileOutputStream(outputFile)) {
            os.write(pdfBytes);
        }
    }

    private String safeEvaluate(XPath xpath, String expr, Document doc) {
        try {
            String val = xpath.evaluate(expr, doc);
            return val != null ? val.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) {
                return "tns".equals(prefix) ? "http://www.xjustiz.de" : javax.xml.XMLConstants.NULL_NS_URI;
            }
            @Override public String getPrefix(String namespaceURI) {
                return "http://www.xjustiz.de".equals(namespaceURI) ? "tns" : null;
            }
            @Override public Iterator<String> getPrefixes(String namespaceURI) {
                return "http://www.xjustiz.de".equals(namespaceURI) ? Set.of("tns").iterator() : Collections.emptyIterator();
            }
        });
        return xpath;
    }

    /**
     * Schlanker PDF-1.4-Erzeuger für A4-Dokumente.
     */
    private static class PdfBuilder {
        private final ByteArrayOutputStream streamContent = new ByteArrayOutputStream();

        public void drawText(String text, float x, float y, float size, boolean bold, float r, float g, float b) throws Exception {
            String font = bold ? "/F2" : "/F1";
            String encText = encodePdfText(text);
            String cmd = String.format(java.util.Locale.US,
                    "BT %s %.1f Tf %.3f %.3f %.3f rg %.1f %.1f Td (%s) Tj ET\n",
                    font, size, r, g, b, x, y, encText);
            streamContent.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
        }

        public void drawTextRight(String text, float xRight, float y, float size, boolean bold, float r, float g, float b) throws Exception {
            float approxWidth = text.length() * size * 0.52f;
            drawText(text, xRight - approxWidth, y, size, bold, r, g, b);
        }

        public void drawTextCenter(String text, float y, float size, boolean bold, float r, float g, float b) throws Exception {
            float approxWidth = text.length() * size * 0.52f;
            float x = (595.28f - approxWidth) / 2f;
            drawText(text, Math.max(50, Math.min(x, 545 - approxWidth)), y, size, bold, r, g, b);
        }

        public float drawWrappedTextCenter(String text, float y, float maxWidth, float size, boolean bold, float r, float g, float b) throws Exception {
            if (text == null || text.isBlank()) return y;
            int maxCharsPerLine = (int) (maxWidth / (size * 0.52f));
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            float curY = y;

            for (String word : words) {
                if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                    drawTextCenter(currentLine.toString(), curY, size, bold, r, g, b);
                    curY -= (size * 1.35f);
                    currentLine = new StringBuilder(word);
                } else {
                    if (!currentLine.isEmpty()) currentLine.append(" ");
                    currentLine.append(word);
                }
            }
            if (!currentLine.isEmpty()) {
                drawTextCenter(currentLine.toString(), curY, size, bold, r, g, b);
                curY -= (size * 1.35f);
            }
            return curY;
        }

        public void drawLine(float x1, float y1, float x2, float y2, float width, float r, float g, float b) throws Exception {
            String cmd = String.format(java.util.Locale.US,
                    "%.2f w %.3f %.3f %.3f RG %.1f %.1f m %.1f %.1f l S\n",
                    width, r, g, b, x1, y1, x2, y2);
            streamContent.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
        }

        public void drawRect(float x, float y, float w, float h, float r, float g, float b, boolean fill) throws Exception {
            String cmd = String.format(java.util.Locale.US,
                    "%.3f %.3f %.3f rg %.1f %.1f %.1f %.1f re %s\n",
                    r, g, b, x, y, w, h, fill ? "f" : "s");
            streamContent.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
        }

        public void drawTableRow(String label, String value, float x, float y) throws Exception {
            drawRect(x, y - 4, 160, 16, 0.97f, 0.98f, 0.99f, true);
            drawLine(x, y - 4, x + 495, y - 4, 0.5f, 0.9f, 0.92f, 0.94f);
            drawText(label, x + 5, y, 9, true, 0.2f, 0.25f, 0.3f);
            String safeVal = (value != null && value.length() > 68) ? value.substring(0, 65) + "..." : (value == null ? "–" : value);
            drawText(safeVal, x + 170, y, 9, false, 0.1f, 0.1f, 0.1f);
        }

        public float drawWrappedText(String text, float x, float y, float maxWidth, float size, boolean bold, float r, float g, float b) throws Exception {
            if (text == null || text.isBlank()) return y;
            int maxCharsPerLine = (int) (maxWidth / (size * 0.52f));
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            float curY = y;

            for (String word : words) {
                if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                    drawText(currentLine.toString(), x, curY, size, bold, r, g, b);
                    curY -= (size * 1.35f);
                    currentLine = new StringBuilder(word);
                } else {
                    if (!currentLine.isEmpty()) currentLine.append(" ");
                    currentLine.append(word);
                }
            }
            if (!currentLine.isEmpty()) {
                drawText(currentLine.toString(), x, curY, size, bold, r, g, b);
                curY -= (size * 1.35f);
            }
            return curY;
        }

        private String encodePdfText(String text) {
            if (text == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                switch (c) {
                    case '\r': break;
                    case '\n': sb.append(" "); break;
                    case '(': sb.append("\\("); break;
                    case ')': sb.append("\\)"); break;
                    case '\\': sb.append("\\\\"); break;
                    case 'ä': sb.append("\u00E4"); break;
                    case 'ö': sb.append("\u00F6"); break;
                    case 'ü': sb.append("\u00FC"); break;
                    case 'Ä': sb.append("\u00C4"); break;
                    case 'Ö': sb.append("\u00D6"); break;
                    case 'Ü': sb.append("\u00DC"); break;
                    case 'ß': sb.append("\u00DF"); break;
                    case '§': sb.append("\u00A7"); break;
                    case '–': sb.append("-"); break;
                    case '—': sb.append("-"); break;
                    case '•': sb.append("*"); break;
                    default:
                        if (c < 128) {
                            sb.append(c);
                        } else {
                            sb.append("?");
                        }
                }
            }
            return sb.toString();
        }

        public byte[] build() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            out.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 1: Catalog
            offsets.add(out.size());
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 2: Pages
            offsets.add(out.size());
            out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 3: Page (A4: 595.28 x 841.89)
            offsets.add(out.size());
            out.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 4: Font F1 (Helvetica)
            offsets.add(out.size());
            out.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 5: Font F2 (Helvetica-Bold)
            offsets.add(out.size());
            out.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 6: Content stream
            byte[] streamBytes = streamContent.toByteArray();
            offsets.add(out.size());
            out.write(String.format("6 0 obj\n<< /Length %d >>\nstream\n", streamBytes.length).getBytes(StandardCharsets.ISO_8859_1));
            out.write(streamBytes);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Xref
            int startXref = out.size();
            out.write(String.format("xref\n0 %d\n0000000000 65535 f \n", offsets.size() + 1).getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                out.write(String.format(java.util.Locale.US, "%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }

            // Trailer
            out.write(String.format(java.util.Locale.US,
                    "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                    offsets.size() + 1, startXref).getBytes(StandardCharsets.ISO_8859_1));

            return out.toByteArray();
        }
    }
}
