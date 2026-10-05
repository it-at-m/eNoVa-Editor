package de.muenchen.enovaeditor.config.decision;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionConfigLoader {

    private static final String CONFIG_FILE = "decision-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public Map<String, List<String>> load() throws IOException {

        Path configPath = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);
        Document document;

        try {
            document = xmlLoader.load(configPath.toFile());
        } catch (SAXException e) {
            throw new IOException("Die Datei " + CONFIG_FILE + " enthält ungültiges XML.", e);
        } catch (ParserConfigurationException e) {
            throw new IOException("Der XML-Parser konnte nicht initialisiert werden.", e);
        }

        Element root = document.getDocumentElement();

        if (!root.getTagName().equals("decisionMappings")) {
            throw new IOException("Ungültige " + CONFIG_FILE + ": Erwartetes Root-Element <decisionMappings>, gefunden <" + root.getTagName() + ">.");
        }

        NodeList requestNodes = root.getChildNodes();

        Map<String, List<String>> allowedDecisionsByRequestCode = new LinkedHashMap<>();

        for (int i = 0; i < requestNodes.getLength(); i++) {
            Node child = requestNodes.item(i);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!child.getNodeName().equals("request")) {
                throw new IOException("Ungültige " + CONFIG_FILE + ": Unerwartetes Element <" + child.getNodeName() + "> unter <decisionMappings>.");
            }

            Element requestElement = (Element) child;

            if (!requestElement.hasAttribute("code")) {
                throw new IOException("Ungültige " + CONFIG_FILE + ": <request> benötigt das Attribut 'code'.");
            }

            String requestCode = requestElement.getAttribute("code").trim();

            if (requestCode.isBlank()) {
                throw new IOException("Ungültige " + CONFIG_FILE + ": Das Attribut 'code' von <request> darf nicht leer sein.");
            }

            List<String> allowedDecisionCodes = new ArrayList<>();

            NodeList decisionNodes = requestElement.getChildNodes();

            for (int j = 0; j < decisionNodes.getLength(); j++) {
                Node decisionNode = decisionNodes.item(j);

                if (decisionNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!decisionNode.getNodeName().equals("decision")) {
                    throw new IOException("Ungültige " + CONFIG_FILE + ": Unerwartetes Element <" + decisionNode.getNodeName() + "> unter <request>.");
                }

                Element decisionElement = (Element) decisionNode;

                if (!decisionElement.hasAttribute("code")) {
                    throw new IOException("Ungültige " + CONFIG_FILE + ": <decision> benötigt das Attribut 'code'.");
                }

                String decisionCode = decisionElement.getAttribute("code").trim();

                if (decisionCode.isBlank()) {
                    throw new IOException("Ungültige " + CONFIG_FILE + ": Das Attribut 'code' von <decision> darf nicht leer sein.");
                }

                if (allowedDecisionCodes.contains(decisionCode)) {
                    throw new IOException("Ungültige " + CONFIG_FILE + ": Der Decision-Code '" + decisionCode + "' ist für Request-Code '" + requestCode + "' mehrfach vorhanden.");
                }

                allowedDecisionCodes.add(decisionCode);
            }

            if (allowedDecisionCodes.isEmpty()) {
                throw new IOException("Ungültige " + CONFIG_FILE + ": Request-Code '" + requestCode + "' enthält keine erlaubten Decisions.");
            }

            if (allowedDecisionsByRequestCode.containsKey(requestCode)) {
                throw new IOException("Ungültige " + CONFIG_FILE + ": Der Request-Code '" + requestCode + "' ist mehrfach vorhanden.");
            }

            allowedDecisionsByRequestCode.put(requestCode, allowedDecisionCodes);
        }

        return allowedDecisionsByRequestCode;
    }
}