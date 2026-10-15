<xsl:stylesheet
        version="3.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:tns="http://www.xjustiz.de">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <!-- Entfernt reine Einrückungs-Whitespace-Knoten aus der Eingabe -->
    <xsl:strip-space elements="*"/>

    <!-- Standardregel: Gibt es für ein Element keine eigene Regel, wird es kopiert und seine Kinder werden weiterverarbeitet -->
    <xsl:mode on-no-match="shallow-copy"/>

    <!-- Steuert, ob eNoVA-Kommentare in die erzeugte XML geschrieben werden -->
    <xsl:param name="includeEnovaComments" select="true()"/>

    <!-- SchemaLocation der erzeugten Antwort -->
    <xsl:param name="schemaLocation"
               select="'http://www.xjustiz.de xjustiz_2900_dabag_3_3.xsd'"/>

    <!-- ========================================================= -->
    <!-- Eingabeparameter                                          -->
    <!-- ========================================================= -->

    <!-- Aktenzeichen, das aus dem Eingabefeld der Anwendung übergeben wird -->
    <xsl:param name="fileNumber"/>

    <!-- Eindeutige Nachrichten-ID, die von der Anwendung für jede Antwort neu erzeugt wird -->
    <xsl:param name="messageId"/>

    <!-- Name des ausgewählten Sachbearbeiters, der von der Anwendung übergeben wird -->
    <xsl:param name="caseworkerName"/>

    <!-- Codes der ausgewählten Sachentscheidungen, die von der Anwendung übergeben wird -->
    <xsl:param name="decisionCodes"/>

    <!-- Produktname der Anwendung, der aus den internen Herstellerinformationen übergeben wird -->
    <xsl:param name="productName"/>

    <!-- Herstellername der Anwendung, der aus den internen Herstellerinformationen übergeben wird -->
    <xsl:param name="manufacturerName"/>

    <!-- Anwendungsversion, die aus der Maven-Projektversion übergeben wird -->
    <xsl:param name="version"/>


    <!-- ========================================================= -->
    <!-- Abgeleitete Werte                                         -->
    <!-- ========================================================= -->

    <!-- Ermittelt die nächste freie Rollennummer -->
    <xsl:variable name="nextRoleNumber" select="max((//tns:verfahrensdaten/tns:beteiligung/tns:rolle/tns:rollennummer, 0)) + 1"/>

    <!-- Ermittelt die nächste freie Beteiligtennummer -->
    <xsl:variable name="nextParticipantNumber" select="max((//tns:verfahrensdaten/tns:beteiligung/tns:beteiligter/tns:beteiligtennummer, 0)) + 1"/>


    <!-- Ermittelt alle Rollennummern der Käufer (072) und Verkäufer (168) aus den Grunddaten -->
    <xsl:variable name="addresseeRoleNumbers"
                  select="/tns:nachricht.enova.entscheidung.2900003
                      /tns:grunddaten/tns:verfahrensdaten
                      /tns:beteiligung/tns:rolle
                      [tns:rollenbezeichnung/code = ('072', '168')]
                      /tns:rollennummer"/>


    <!-- ========================================================= -->
    <!-- externe Konfiguration                                     -->
    <!-- ========================================================= -->

    <!-- Liest den Absendernamen aus der externen sender-config.xml -->
    <xsl:variable name="senderName" select="doc('sender-config.xml')/sender/name"/>

    <!-- Liest die Sachbearbeiter aus der externen caseworkers.xml -->
    <xsl:variable name="caseworkers" select="doc('caseworkers.xml')"/>


    <!-- ========================================================= -->
    <!-- Root-Element                                              -->
    <!-- ========================================================= -->

    <!--
        Übernimmt das Root-Element der ursprünglichen XJustiz-Nachricht.
        Ist xsi:schemaLocation bereits vorhanden, wird der Wert ersetzt.
        Fehlt xsi:schemaLocation, wird das Attribut hinzugefügt.
        In beiden Fällen wird der in der XSLT konfigurierte Wert verwendet.
        Die benötigte xmlns:xsi-Deklaration wird bei Bedarf automatisch ergänzt.
        Alle übrigen Attribute und XML-Inhalte werden wie bisher verarbeitet.
    -->
    <xsl:template match="/tns:nachricht.enova.entscheidung.2900003"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

        <xsl:copy>

            <!-- Übernimmt alle Attribute außer der bisherigen schemaLocation -->
            <xsl:apply-templates select="@* except @xsi:schemaLocation"/>

            <!-- Setzt die in der XSLT konfigurierte schemaLocation -->
            <xsl:attribute name="xsi:schemaLocation"
                           select="$schemaLocation"/>

            <xsl:if test="$includeEnovaComments">
                <xsl:comment>eNoVA: xsi:schemaLocation wurde auf den in answer-transform.xsl konfigurierten Wert gesetzt</xsl:comment>
            </xsl:if>

            <!-- Verarbeitet alle weiteren XML-Inhalte wie bisher -->
            <xsl:apply-templates select="node()"/>

        </xsl:copy>

    </xsl:template>


    <!-- ========================================================= -->
    <!-- Nachrichtenkopf                                           -->
    <!-- ========================================================= -->

    <!-- Nachrichtenkopf: Übernimmt den Nachrichtenkopf ohne vorhandene Herstellerinformationen -->
    <xsl:template match="tns:nachrichtenkopf">
        <xsl:copy>

            <!-- Übernimmt Attribute und verarbeitet alle Kindelemente außer der alten Herstellerinformation -->
            <xsl:apply-templates select="@* | node()[not(self::tns:herstellerinformation)]"/>

            <!-- Fügt die Herstellerinformationen der Anwendung am Ende des Nachrichtenkopfs hinzu -->
            <xsl:call-template name="manufacturerInfo"/>

        </xsl:copy>
    </xsl:template>

    <!-- ========================================================= -->
    <!-- Regeln für die Inhalte des Nachrichtenkopfs               -->
    <!-- ========================================================= -->

    <!-- Erstellungszeitpunkt: Ersetzt den vorhandenen Wert durch den aktuellen Zeitpunkt -->
    <xsl:template match="tns:nachrichtenkopf/tns:erstellungszeitpunkt">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Erstellungszeitpunkt wurde durch den aktuellen Systemzeitpunkt ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="current-dateTime()"/>
        </xsl:copy>
    </xsl:template>


    <!-- Absendername: Ersetzt den vorhandenen Kommunikationspartner durch den konfigurierten Absender -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:informationen/tns:auswahl_kommunikationspartner/tns:sonstige">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Kommunikationspartner wurde durch den Wert aus sender-config.xml ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="$senderName"/>
        </xsl:copy>
    </xsl:template>

    <!-- Absender-Rollennummer: Verweist auf die neu angelegte Beteiligung des ausgewählten Sachbearbeiters -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:informationen/tns:auswahl_verweisGrunddaten/tns:ref.rollennummer">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Rollennummer wurde durch die nächste freie Rollennummer aus den vorhandenen Verfahrensdaten ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="$nextRoleNumber"/>
        </xsl:copy>
    </xsl:template>

    <!-- Aktenzeichen: Ersetzt das vorhandene Aktenzeichen durch den Wert aus dem Eingabefeld -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:aktenzeichen">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Aktenzeichen wurde durch den Wert aus dem Eingabefeld ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="$fileNumber"/>
        </xsl:copy>
    </xsl:template>


    <!-- Nachrichten-ID: Ersetzt die vorhandene ID durch eine neu erzeugte eindeutige Nachrichten-ID -->
    <xsl:template match="tns:nachrichtenkopf/tns:absender/tns:eigeneNachrichtenID">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Nachrichten-ID wurde durch eine von der Anwendung neu erzeugte eindeutige Nachrichten-ID ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="$messageId"/>
        </xsl:copy>
    </xsl:template>

    <!-- Empfänger: Übernimmt die Daten des ursprünglichen Absenders und wandelt dessen Aktenzeichen in die Empfängerstruktur um -->
    <xsl:template match="tns:nachrichtenkopf/tns:empfaenger">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Empfängerdaten wurden aus den Daten des ursprünglichen Absenders übernommen</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <!-- Übernimmt alle Kindelemente des ursprünglichen Absenders außer dem Aktenzeichen -->
            <xsl:copy-of select="../tns:absender/node()[not(self::tns:aktenzeichen)]"/>

            <!-- Übernimmt das Aktenzeichen des ursprünglichen Absenders in die Empfängerstruktur -->
            <xsl:if test="$includeEnovaComments">
                <xsl:comment>eNoVA: Empfänger-Aktenzeichen wurde aus dem Aktenzeichen des ursprünglichen Absenders übernommen</xsl:comment>
            </xsl:if>
            <tns:auswahl_aktenzeichen>
                <tns:aktenzeichen.freitext>
                    <xsl:value-of select="../tns:absender/tns:aktenzeichen"/>
                </tns:aktenzeichen.freitext>
            </tns:auswahl_aktenzeichen>
        </xsl:copy>
    </xsl:template>

    <!-- Herstellerinformationen: Fügt die internen Herstellerdaten der Anwendung hinzu -->
    <xsl:template name="manufacturerInfo">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Herstellerinformationen wurden aus den internen Herstellerinformationen der Anwendung übernommen</xsl:comment>
        </xsl:if>
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

    <!-- Instanzbehörde: Ersetzt die bisherige Behördenbezeichnung durch den konfigurierten Absendernamen -->
    <xsl:template match="tns:grunddaten/tns:verfahrensdaten/tns:instanzdaten/tns:auswahl_instanzbehoerde/tns:sonstige">
        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Instanzbehörde wurde durch den Wert aus sender-config.xml ersetzt</xsl:comment>
        </xsl:if>
        <xsl:copy>
            <xsl:value-of select="$senderName"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="tns:grunddaten/tns:verfahrensdaten">

        <!-- Wählt den in der Anwendung ausgewählten Sachbearbeiter aus -->
        <xsl:variable name="selectedCaseworker" select="$caseworkers/caseworkers/entry[@name = $caseworkerName]"/>

        <xsl:copy>

            <!-- Übernimmt alle vorhandenen Inhalte der Verfahrensdaten -->
            <xsl:apply-templates select="@* | node()"/>

            <xsl:if test="$includeEnovaComments">
                <xsl:comment>eNoVA: Beteiligung wurde für den ausgewählten Sachbearbeiter aus caseworkers.xml ergänzt; Rollen- und Beteiligtennummer wurden als nächste freie Nummern aus den vorhandenen Verfahrensdaten ermittelt</xsl:comment>
            </xsl:if>

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
    <!-- Schriftgutobjekte                                         -->
    <!-- ========================================================= -->

    <!-- Ersetzt den gesamten Schriftgutobjekte-Bereich aus der Eingabe-XML durch den Inhalt der externen schriftgutobjekte.xml -->
    <xsl:template match="tns:schriftgutobjekte">

        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Schriftgutobjekte wurden durch den Inhalt aus schriftgutobjekte.xml ersetzt</xsl:comment>
        </xsl:if>

        <xsl:copy-of select="doc('schriftgutobjekte.xml')/tns:schriftgutobjekte"/>

    </xsl:template>


    <!-- ========================================================= -->
    <!-- Fachdaten                                                 -->
    <!-- ========================================================= -->

    <!-- Sachentscheidung: Ersetzt das Ersuchen durch die ausgewählte Sachentscheidung und ergänzt alle Käufer und Verkäufer als Adressaten -->
    <xsl:template match="tns:fachdaten/tns:auswahl_GegenstandDerNachricht/tns:ersuchenSachentscheidung">

        <xsl:variable name="decisionIndex" select="count(preceding-sibling::tns:ersuchenSachentscheidung) + 1"/>

        <xsl:if test="$includeEnovaComments">
            <xsl:comment>eNoVA: Ersuchen um Sachentscheidung wurde durch die zugehörige ausgewählte Sachentscheidung ersetzt</xsl:comment>
        </xsl:if>

        <tns:sachentscheidung>

            <tns:sachentscheidung listVersionID="1.1">
                <code>
                    <xsl:value-of select="$decisionCodes[$decisionIndex]"/>
                </code>
            </tns:sachentscheidung>

            <xsl:if test="$includeEnovaComments and exists($addresseeRoleNumbers)">
                <xsl:comment>eNoVA: Alle Käufer und Verkäufer wurden anhand ihrer Rollennummern als Adressaten der Sachentscheidung referenziert</xsl:comment>
            </xsl:if>

            <!-- Referenziert alle Käufer und Verkäufer -->
            <xsl:for-each select="$addresseeRoleNumbers">
                <tns:adressatDerSachentscheidung>
                    <tns:ref.rollennummer>
                        <xsl:value-of select="."/>
                    </tns:ref.rollennummer>
                </tns:adressatDerSachentscheidung>
            </xsl:for-each>

        </tns:sachentscheidung>

    </xsl:template>

</xsl:stylesheet>
