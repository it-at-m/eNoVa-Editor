package de.muenchen.enovaeditor.config.caseworker;

import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CaseworkerConfigLoader {

    private static final String CONFIG_FILE = "caseworkers.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public List<CaseworkerEntry> load() throws IOException {

        Path configPath = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);
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
        Set<String> names = new HashSet<>();

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

            if (!names.add(name)) {
                throw new IOException(
                        "Ungültige caseworkers.xml: "
                                + "Der Sachbearbeitername '"
                                + name
                                + "' ist mehrfach vorhanden."
                );
            }

            caseworkers.add(new CaseworkerEntry(name));
        }

        return caseworkers;
    }


}
