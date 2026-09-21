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

    <!-- Name des ausgewählten Sachbearbeiters, der von der Anwendung übergeben wird -->
    <xsl:param name="caseworkerName"/>

    <!-- Code der ausgewählten Sachentscheidung, der von der Anwendung übergeben wird -->
    <xsl:param name="decisionCode"/>

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
    <xsl:variable name="senderName" select="doc('sender-config.xml')/sender/name"/>

    <!-- Liest die Sachbearbeiter aus der externen caseworkers.xml -->
    <xsl:variable name="caseworkers" select="doc('caseworkers.xml')"/>


    <!-- ========================================================= -->
    <!-- Nachrichtenkopf                                           -->
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


    <!-- ========================================================= -->
    <!-- Grunddaten                                                -->
    <!-- ========================================================= -->

    <xsl:template match="tns:grunddaten/tns:verfahrensdaten">

        <!-- Variablen -->

        <!-- Ermittelt die nächste freie Rollennummer -->
        <xsl:variable name="nextRoleNumber" select="max((tns:beteiligung/tns:rolle/tns:rollennummer, 0)) + 1"/>
        <!-- Ermittelt die nächste freie Beteiligtennummer -->
        <xsl:variable name="nextParticipantNumber" select="max((tns:beteiligung/tns:beteiligter/tns:beteiligtennummer, 0)) + 1"/>
        <!-- Wählt den in der Anwendung ausgewählten Sachbearbeiter aus -->
        <xsl:variable name="selectedCaseworker" select="$caseworkers/caseworkers/entry[@name = $caseworkerName]"/>

        <xsl:copy>

            <!-- Übernimmt alle vorhandenen Inhalte der Verfahrensdaten -->
            <xsl:apply-templates select="@* | *"/>

            <!-- Fügt den ausgewählten Sachbearbeiter als neue Beteiligung hinzu -->
            <tns:beteiligung>

                <tns:rolle>
                    <tns:rollennummer>
                        <xsl:value-of select="$nextRoleNumber"/>
                    </tns:rollennummer>

                    <tns:rollenbezeichnung listVersionID="3.5">
                        <code>212</code>
                    </tns:rollenbezeichnung>
                </tns:rolle>

                <tns:beteiligter>
                    <tns:beteiligtennummer>
                        <xsl:value-of select="$nextParticipantNumber"/>
                    </tns:beteiligtennummer>

                    <!-- Übernimmt die Daten des ausgewählten Sachbearbeiters -->
                    <xsl:copy-of select="$selectedCaseworker/tns:auswahl_beteiligter"/>
                </tns:beteiligter>

            </tns:beteiligung>

        </xsl:copy>
    </xsl:template>


    <!-- ========================================================= -->
    <!-- Fachdaten                                                 -->
    <!-- ========================================================= -->

    <!-- Sachentscheidung: Ersetzt das Ersuchen um Sachentscheidung durch die ausgewählte Sachentscheidung -->
    <xsl:template match="tns:fachdaten/tns:auswahl_GegenstandDerNachricht/tns:ersuchenSachentscheidung">
        <tns:sachentscheidung>
            <tns:sachentscheidung listVersionID="1.0">
                <code>
                    <xsl:value-of select="$decisionCode"/>
                </code>
            </tns:sachentscheidung>
        </tns:sachentscheidung>
    </xsl:template>

</xsl:stylesheet>
