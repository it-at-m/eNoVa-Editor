# eNoVa-Editor

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io/)
[![XJustiz](https://img.shields.io/badge/XJustiz-eNoVA%202900003%20%2F%202900004-blueviolet.svg)](https://xjustiz.justiz.de/Anwendungsfaelle/eNoVA/index.php)

The **eNoVa-Editor** is a specialized desktop assistant for municipal caseworkers (*Sachbearbeiter/innen*) in Germany to process electronic preemptive right inquiries (*Vorkaufsrechtsanfragen*) submitted by notaries via the **beBPo** (*Besonderes elektronisches Behördenpostfach*).

Starting in **January 2027**, municipalities in Germany are legally required to process preemptive right requests digitally in the structured **XJustiz.enova.2900003** format. The eNoVa-Editor provides a complete, modern workflow to view incoming inquiries, record legal decisions under the German Federal Building Code (*Baugesetzbuch – BauGB*), and generate standard-compliant response datasets (**XJustiz.enova.2900004**) and official printable decisions (PDF).

---

## Key Features

- **Integrated Split-Screen Interface**: Direct preview of incoming notarization requests inside the application window using an embedded rendering engine—no external browser needed.
- **Decision Capture (BauGB)**:
  - *Negative Certificate – Non-exercise* (§ 28 (1) sentence 1 BauGB)
  - *Negative Certificate – Non-existence* (§ 28 (1) sentence 3 BauGB)
  - *Exercise of Preemptive Right* (§ 28 (2) BauGB)
  - *Urban Redevelopment Approval* (§§ 144, 145 BauGB)
  - *Preservation Statute Approval* (§§ 172, 173 BauGB)
- **Live Preview of the Decision Draft**: The decision draft updates in real time as the caseworker enters file numbers, fees, tenor, and reasoning.
- **XJustiz 2900004 Response Generation**: Creates the complete, valid `nachricht.enova.entscheidung.2900004` XML file referencing the incoming message ID, participants, parcels, fee assessment, and municipal tenor.
- **Pure-Java PDF Generation**: Produces official DIN-A4 decision certificates for internal filing (*Veraktung / E-Akte*) without external dependencies—100% compatible with standalone `jlink` runtime distributions.
- **1-Click beBPo Package Export**: Exports both the `2900004.xml` and the `bescheid.pdf` with a single click, ready for qualified electronic signing (qeS / CAdES detached).
- **Fast Input via Drag & Drop**: XML files can be dragged directly onto the app window or launcher script.
- **Caseworker Directory**: Automatic pre-selection of contact details (name, phone, office) via `caseworkers.xml`.
- **High-DPI & Screen Scaling Support**: Dynamic window positioning and responsive layouts prevent off-screen borders on multi-monitor or scaling setups (125% / 150% Windows DPI).
- **Built-in beBPo & Signature Guide**: Step-by-step guidance based on Federal Chamber of Notaries (*Bundesnotarkammer – BNotK*) requirements for municipal caseworkers.

---

## Workflow Overview

```
[Notary via beBPo] ──> [XJustiz 2900003 XML] ──> [eNoVa-Editor]
                                                        │
                      ┌─────────────────────────────────┴─────────────────────────────────┐
                      ▼                                                                   ▼
       [XJustiz 2900004 XML]                                                [Official Decision (PDF)]
              │                                                                   │
              ▼                                                                   ▼
   [Qualified Signature (qeS)]                                            [Municipal File / E-Akte]
   (CAdES detached .p7s / .pkcs7)                                         (Behördliche Veraktung)
              │
              ▼
  [Return to Notary via beBPo]
  (Forwarded to Land Registry / GBO §§ 29, 137)
```

---

## Building and Running

### Prerequisites

- **Java Development Kit (JDK) 21** or later (e.g. Eclipse Temurin 21)
- **Apache Maven 3.9+** (or the included `./mvnw` wrapper)

### Build Standalone Distribution

To build the application and package a standalone, zero-install distribution image with a bundled runtime:

```bash
# Windows
.\mvnw.cmd package -Pdist

# Linux / macOS
./mvnw package -Pdist
```

The self-contained distribution is located in `target/distribution/eNoVa-editor`. It contains a minimal Jlink Java runtime, all configuration files, templates, and sample data. No Java installation is required on caseworkers' machines.

### Running the Application

Double-click `eNoVa-Editor-starten.cmd` or run:

```bash
target\distribution\eNoVa-editor\eNoVa-editor.cmd
```

You can also pass an XML file directly:

```bash
eNoVa-Editor-starten.cmd samples\xjustiz_beispiel_2900003.xml
```

---

## German / Deutsch

Der **eNoVa-Editor** ist ein spezialisierter Fachassistent für Sachbearbeiterinnen und Sachbearbeiter in den Liegenschafts- und Bauverwaltungen deutscher Kommunen zur digitalen Bearbeitung von Vorkaufsrechtsanfragen der Notare über das **beBPo** (*Besonderes elektronisches Behördenpostfach*).

Ab **Januar 2027** sind die Kommunen in Deutschland gesetzlich verpflichtet, Vorkaufsrechtsanfragen der Notariate digital im strukturierten Format **XJustiz.enova.2900003** nach dem eNoVA-Gesetz entgegenzunehmen und zu beantworten. Der eNoVa-Editor deckt diesen gesamten Prozess praxisnah ab: von der übersichtlichen Prüfung des Kaufvertrags über die Erfassung der Sachentscheidung nach BauGB bis zum automatischen Export der Antwortnachricht (**XJustiz.enova.2900004**) und eines veraktungsfähigen Bescheids (PDF).

### Kernfunktionen

1. **Integrierte Dokumentenansicht (Split-Screen & Tabs)**:
   - Notaranfrage direkt im Programmfenster prüfen – kein externer Browser erforderlich.
   - Registerkarten für **Notaranfrage**, **Bescheid-Entwurf** (Echtzeit-Aktualisierung) und **beBPo-Leitfaden**.
   - Integrierte Zoom- und Druckfunktionen.
2. **Praxisgerechte Entscheidungsmaske nach BauGB**:
   - *Negativzeugnis: Vorkaufsrecht wird nicht ausgeübt* (§ 28 Abs. 1 Satz 1 BauGB – Regelfall)
   - *Negativzeugnis: Vorkaufsrecht besteht nicht* (§ 28 Abs. 1 Satz 3 BauGB)
   - *Ausübung des Vorkaufsrechts* (§ 28 Abs. 2 BauGB)
   - *Sanierungsrechtliche Genehmigung* (§§ 144, 145 BauGB)
   - *Genehmigung Erhaltungssatzung* (§§ 172, 173 BauGB)
   - Automatische Vorbelegung von rechtssicheren Tenor- und Begründungstexten sowie Aktenzeichen-Vorschlag.
3. **XJustiz-Antwortdatei (2900004)**:
   - Erzeugt die vollständige, strukturierte XJustiz-Nachricht `nachricht.enova.entscheidung.2900004` mit Übernahme aller Verfahrensdaten, Beteiligten, Flurstücke und getroffenen Sachentscheidung.
4. **Veraktungsfähiger Bescheid als PDF (DIN A4)**:
   - Erzeugt ein amtliches, druckreifes Dokument mit Briefkopf, Urkundendaten, Beschlusskasten, Begründung, Kostenfestsetzung und Dienstsiegelhinweis.
   - 100 % reine Java-Implementierung ohne externe native Bibliotheken (voll modular und jlink-fähig).
5. **1-Klick-Export für das beBPo-Postfach**:
   - Speichert die XML-Antwort (zur qeS-Signatur) und das Bescheid-PDF im Eingangsverzeichnis ab und öffnet den Explorer-Ordner auf Knopfdruck.
6. **Einfache Bedienung & Datenaufnahme**:
   - Drag & Drop von XML-Dateien direkt ins Fenster oder auf das Startskript.
   - Unterstützung für „Öffnen mit“ im Windows Explorer.
   - Mehrere Sachbearbeitende über `caseworkers.xml` konfigurierbar.
   - Vollständige Unterstützung für Windows-Skalierungen (125 % / 150 % DPI).

### Rechtlicher Hintergrund & Signatur (BNotK-Vorgaben)

Nach den Hinweisen der Bundesnotarkammer (BNotK) zur elektronischen Vorkaufsrechtsbescheinigung nach §§ 29, 137 GBO gilt:
- Für die Eintragung im Grundbuch ist ausschließlich der **strukturierte XJustiz-Datensatz (2900004)** maßgeblich.
- Dieser muss von der Behörde mit einer **qualifizierten elektronischen Signatur (qeS)** versehen werden (Format: **CAdES detached**, Dateiendung `.p7s` oder `.pkcs7`).
- Das erzeugte PDF dient der behördlichen Dokumentenablage (**Veraktung in der E-Akte / dem DMS**).

---

## Lizenz & Mitwirken

Dieses Projekt steht unter Open-Source-Lizenz im Rahmen der Open-Government-Initiative der Landeshauptstadt München (it@M).