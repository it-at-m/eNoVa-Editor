package de.muenchen.enovaeditor.config.caseworker;

import de.muenchen.enovaeditor.error.AppException;
import de.muenchen.enovaeditor.error.ErrorDetail;
import de.muenchen.enovaeditor.util.ApplicationFileUtil;
import de.muenchen.enovaeditor.xml.XmlLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CaseworkerConfigLoader {

    private static final String CONFIG_FILE = "caseworkers.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public List<CaseworkerEntry> load() throws AppException {

        Path configPath = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);
        Document document = xmlLoader.load(configPath.toFile());

        Element root = document.getDocumentElement();

        if (!root.getTagName().equals("caseworkers")) {
            throw new AppException(
                    "Ungültige \"" + CONFIG_FILE
                            + "\": Erwartetes Root-Element <caseworkers>, gefunden <"
                            + root.getTagName() + ">.",
                    List.of(
                            new ErrorDetail("Datei", configPath.toString()),
                            new ErrorDetail("Erwartetes Element", "caseworkers"),
                            new ErrorDetail("Gefundenes Element", root.getTagName())
                    )
            );
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
                throw new AppException(
                        "Ungültige \"" + CONFIG_FILE + "\": Unerwartetes Element <"
                                + child.getNodeName() + "> unter <caseworkers>. "
                                + "Erwartet wird dort nur <entry>.",
                        List.of(
                                new ErrorDetail("Datei", configPath.toString()),
                                new ErrorDetail("Unerwartetes Element", child.getNodeName())
                        )
                );
            }

            Element entry = (Element) child;

            if (!entry.hasAttribute("name")) {
                throw new AppException(
                        "Ungültige \"" + CONFIG_FILE
                                + "\": Jedes <entry>-Element muss ein nicht-leeres Attribut 'name' besitzen.",
                        List.of(
                                new ErrorDetail("Datei", configPath.toString()),
                                new ErrorDetail("Element", "entry"),
                                new ErrorDetail("Attribut", "name")
                        )
                );
            }

            String name = entry.getAttribute("name");

            if (name.isBlank()) {
                throw new AppException(
                        "Ungültige \"" + CONFIG_FILE
                                + "\": Das Attribut 'name' eines <entry>-Elements darf nicht leer sein.",
                        List.of(
                                new ErrorDetail("Datei", configPath.toString()),
                                new ErrorDetail("Element", "entry"),
                                new ErrorDetail("Attribut", "name")
                        )
                );
            }

            if (!names.add(name)) {
                throw new AppException(
                        "Ungültige \"" + CONFIG_FILE + "\": Der Sachbearbeitername '"
                                + name + "' ist mehrfach vorhanden.",
                        List.of(
                                new ErrorDetail("Datei", configPath.toString()),
                                new ErrorDetail("Sachbearbeitername", name)
                        )
                );
            }

            caseworkers.add(new CaseworkerEntry(name));
        }

        if (caseworkers.isEmpty()) {
            throw new AppException(
                    "Ungültige \"" + CONFIG_FILE + "\": Es ist kein <entry>-Eintrag definiert.",
                    List.of(new ErrorDetail("Datei", configPath.toString()))
            );
        }

        return caseworkers;
    }


}
