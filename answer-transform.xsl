<xsl:stylesheet
        version="3.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:tns="http://www.xjustiz.de">

    <xsl:output method="xml" indent="yes"/>

    <!-- Aktenzeichen, das aus dem Eingabefeld der Anwendung übergeben wird -->
    <xsl:param name="fileNumber"/>

    <!-- Eindeutige Nachrichten-ID, die von der Anwendung für jede Antwort neu erzeugt wird -->
    <xsl:param name="messageId"/>

    <!-- Konfiguration: Liest den Absendernamen aus der externen sender-config.xml -->
    <xsl:variable name="senderName" select="doc('sender-config.xml')/sender/name"/>

    <!-- Standardregel: Kopiert alle Elemente, Attribute und Inhalte unverändert -->
    <xsl:mode on-no-match="shallow-copy"/>

    <!-- Erstellungszeitpunkt: Ersetzt den vorhandenen Wert durch den aktuellen Zeitpunkt -->
    <xsl:template match="tns:nachrichtenkopf/tns:erstellungszeitpunkt">
        <xsl:copy>
            <xsl:value-of select="current-dateTime()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Absendername: Ersetzt den vorhandenen Kommunikationspartner durch den konfigurierten Absender -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:informationen/tns:auswahl_kommunikationspartner/tns:sonstige">
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

</xsl:stylesheet>