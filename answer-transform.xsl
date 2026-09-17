<xsl:stylesheet
        version="3.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:tns="http://www.xjustiz.de">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <!-- Standardregel: Gibt es für ein Element keine eigene Regel, wird es kopiert und seine Kinder werden weiterverarbeitet -->
    <xsl:mode on-no-match="shallow-copy"/>


    <!-- ========================================================= -->
    <!-- Eingabeparameter                                          -->
    <!-- ========================================================= -->

    <!-- Aktenzeichen, das aus dem Eingabefeld der Anwendung übergeben wird -->
    <xsl:param name="fileNumber"/>

    <!-- Eindeutige Nachrichten-ID, die von der Anwendung für jede Antwort neu erzeugt wird -->
    <xsl:param name="messageId"/>

    <!-- Produktname der Anwendung, der aus den internen Herstellerinformationen übergeben wird -->
    <xsl:param name="productName"/>

    <!-- Herstellername der Anwendung, der aus den internen Herstellerinformationen übergeben wird -->
    <xsl:param name="manufacturerName"/>

    <!-- Anwendungsversion, die aus der Maven-Projektversion übergeben wird -->
    <xsl:param name="version"/>


    <!-- ========================================================= -->
    <!-- externe Konfiguration                                     -->
    <!-- ========================================================= -->

    <!-- Liest den Absendernamen aus der externen sender-config.xml -->
    <xsl:variable name="senderName"
                  select="doc('sender-config.xml')/sender/name"/>


    <!-- ========================================================= -->
    <!-- Root-Element                                              -->
    <!-- ========================================================= -->

    <!-- Root-Element: Erstellt den äußeren Nachrichten-Wrapper der Antwort -->
    <xsl:template match="tns:nachricht.enova.entscheidung.2900003">
        <xsl:copy>

            <!-- Fügt den Nachrichtenkopf hinzu und wendet dessen Transformationsregel an -->
            <xsl:apply-templates select="tns:nachrichtenkopf"/>

        </xsl:copy>
    </xsl:template>


    <!-- ========================================================= -->
    <!-- Nachrichtenkopf                                          -->
    <!-- ========================================================= -->

    <!-- Nachrichtenkopf: Übernimmt den Nachrichtenkopf ohne vorhandene Herstellerinformationen -->
    <xsl:template match="tns:nachrichtenkopf">
        <xsl:copy>

            <!-- Übernimmt Attribute und verarbeitet alle Kindelemente außer der alten Herstellerinformation -->
            <xsl:apply-templates
                    select="@* | *[not(self::tns:herstellerinformation)]"/>

            <!-- Fügt die Herstellerinformationen der Anwendung am Ende des Nachrichtenkopfs hinzu -->
            <xsl:call-template name="manufacturerInfo"/>

        </xsl:copy>
    </xsl:template>

    <!-- ========================================================= -->
    <!-- Regeln für die Inhalte des Nachrichtenkopfs               -->
    <!-- ========================================================= -->

    <!-- Erstellungszeitpunkt: Ersetzt den vorhandenen Wert durch den aktuellen Zeitpunkt -->
    <xsl:template match="tns:nachrichtenkopf/tns:erstellungszeitpunkt">
        <xsl:copy>
            <xsl:value-of select="current-dateTime()"/>
        </xsl:copy>
    </xsl:template>


    <!-- Absendername: Ersetzt den vorhandenen Kommunikationspartner durch den konfigurierten Absender -->
    <xsl:template
            match="tns:nachrichtenkopf/tns:absender/tns:informationen/tns:auswahl_kommunikationspartner/tns:sonstige">
        <xsl:copy>
            <xsl:value-of select="$senderName"/>
        </xsl:copy>
    </xsl:template>


    <!-- Aktenzeichen: Ersetzt das vorhandene Aktenzeichen durch den Wert aus dem Eingabefeld -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:aktenzeichen">
        <xsl:copy>
            <xsl:value-of select="$fileNumber"/>
        </xsl:copy>
    </xsl:template>


    <!-- Nachrichten-ID: Ersetzt die vorhandene ID durch eine neu erzeugte eindeutige Nachrichten-ID -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:eigeneNachrichtenID">
        <xsl:copy>
            <xsl:value-of select="$messageId"/>
        </xsl:copy>
    </xsl:template>

    <!-- Empfänger: Übernimmt die Daten des ursprünglichen Absenders und wandelt dessen Aktenzeichen in die Empfängerstruktur um -->
    <xsl:template match="tns:nachrichtenkopf/tns:empfaenger">
        <xsl:copy>
            <!-- Übernimmt alle Kindelemente des ursprünglichen Absenders außer dem Aktenzeichen -->
            <xsl:copy-of select="../tns:absender/*[not(self::tns:aktenzeichen)]"/>

            <!-- Übernimmt das Aktenzeichen des ursprünglichen Absenders in die Empfängerstruktur -->
            <tns:auswahl_aktenzeichen>
                <tns:aktenzeichen.freitext>
                    <xsl:value-of select="../tns:absender/tns:aktenzeichen"/>
                </tns:aktenzeichen.freitext>
            </tns:auswahl_aktenzeichen>
        </xsl:copy>
    </xsl:template>

    <!-- Herstellerinformationen: Fügt die internen Herstellerdaten der Anwendung hinzu -->
    <xsl:template name="manufacturerInfo">
        <tns:herstellerinformation>
            <tns:nameDesProdukts>
                <xsl:value-of select="$productName"/>
            </tns:nameDesProdukts>

            <tns:herstellerDesProdukts>
                <xsl:value-of select="$manufacturerName"/>
            </tns:herstellerDesProdukts>

            <tns:version>
                <xsl:value-of select="$version"/>
            </tns:version>
        </tns:herstellerinformation>
    </xsl:template>

</xsl:stylesheet>