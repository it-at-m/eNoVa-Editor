package de.muenchen.enovaeditor.config.caseworker;

import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CaseworkerConfigLoader {

    private static final String CONFIG_FILE = "caseworkers.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public List<CaseworkerEntry> load() throws IOException {

        Path configPath = ApplicationPaths.getApplicationDirectory().resolve(CONFIG_FILE);

        if (!Files.isRegularFile(configPath)) {
            throw new IOException("Die Datei caseworkers.xml wurde nicht gefunden: " + configPath);
        }

        if (!Files.isReadable(configPath)) {
            throw new IOException("Die Datei caseworkers.xml kann nicht gelesen werden: " + configPath);
        }

        Document document;

        try {
            document = xmlLoader.load(configPath.toFile());
        } catch (SAXException e) {
            throw new IOException("Die Datei caseworkers.xml enthält ungültiges XML.", e);
        } catch (ParserConfigurationException e) {
            throw new IOException("Der XML-Parser konnte nicht initialisiert werden.", e);
        }

        Element root = document.getDocumentElement();

        if (!root.getTagName().equals("caseworkers")) {
            throw new IOException("Ungültige caseworkers.xml: Erwartetes Root-Element <caseworkers>, gefunden <" + root.getTagName() + ">.");
        }

        NodeList children = root.getChildNodes();

        List<CaseworkerEntry> caseworkers = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!child.getNodeName().equals("entry")) {
                throw new IOException("Ungültige caseworkers.xml: Unerwartetes Element <" + child.getNodeName() + "> unter <caseworkers>. Erwartet wird dort nur <entry>.");
            }

            Element entry = (Element) child;

            if (!entry.hasAttribute("name")) {
                throw new IOException("Ungültige caseworkers.xml: Jedes <entry>-Element muss ein nicht-leeres Attribut 'name' besitzen.");
            }

            String name = entry.getAttribute("name");

            if (name.isBlank()) {
                throw new IOException("Ungültige caseworkers.xml: Das Attribut 'name' eines <entry>-Elements darf nicht leer sein.");
            }

            NodeList entryChildren = entry.getChildNodes();

            int xmlCount = 0;

            Element xmlElement = null;
            for (int j = 0; j < entryChildren.getLength(); j++) {
                Node entryChild = entryChildren.item(j);

                if (entryChild.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!entryChild.getNodeName().equals("xml")) {
                    throw new IOException("Ungültige caseworkers.xml: Im <entry> für '" + name + "' ist das unerwartete Element <" + entryChild.getNodeName() + "> enthalten. Erwartet wird nur <xml>.");
                }
                if (xmlCount == 0) {
                    xmlElement = (Element) entryChild;
                }
                xmlCount++;
            }

            if (xmlCount == 0) {
                throw new IOException("Ungültige caseworkers.xml: Im <entry> für '" + name + "' fehlt das erforderliche <xml>-Element.");
            }

            if (xmlCount > 1) {
                throw new IOException("Ungültige caseworkers.xml: Im <entry> für '" + name + "' darf genau ein <xml>-Element vorhanden sein.");
            }

            NodeList xmlChildren = xmlElement.getChildNodes();

            if (xmlChildren.getLength() != 1
                    || xmlChildren.item(0).getNodeType() != Node.CDATA_SECTION_NODE) {
                throw new IOException(
                        "Ungültige caseworkers.xml: Das <xml>-Element für '"
                                + name
                                + "' muss genau einen CDATA-Block enthalten."
                );
            }

            String xmlBlock = xmlElement.getTextContent();
            caseworkers.add(
                    new CaseworkerEntry(name, xmlBlock)
            );
        }

        return caseworkers;
    }


}
