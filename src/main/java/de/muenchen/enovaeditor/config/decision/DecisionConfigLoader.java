package de.muenchen.enovaeditor.config.decision;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionConfigLoader {

    private static final String CONFIG_FILE = "decision-config.xml";

    private final XmlLoader xmlLoader = new XmlLoader();

    public Map<String, List<String>> load() throws AppException {

        Path configPath = ApplicationFileUtil.resolveReadableFile(CONFIG_FILE);
        Document document = xmlLoader.load(configPath.toFile());

        Element root = document.getDocumentElement();

        if (!root.getTagName().equals("decisionMappings")) {
            throw invalidConfig(
                    configPath,
                    "Ungültige \"" + CONFIG_FILE + "\": Erwartetes Root-Element "
                            + "<decisionMappings>, gefunden <" + root.getTagName() + ">.",
                    new ErrorDetail("Erwartetes Element", "decisionMappings"),
                    new ErrorDetail("Gefundenes Element", root.getTagName())
            );
        }

        NodeList requestNodes = root.getChildNodes();

        Map<String, List<String>> allowedDecisionsByRequestCode = new LinkedHashMap<>();

        for (int i = 0; i < requestNodes.getLength(); i++) {
            Node child = requestNodes.item(i);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!child.getNodeName().equals("request")) {
                throw invalidConfig(
                        configPath,
                        "Ungültige " + CONFIG_FILE + ": Unerwartetes Element <"
                                + child.getNodeName() + "> unter <decisionMappings>.",
                        new ErrorDetail("Unerwartetes Element", child.getNodeName())
                );
            }

            Element requestElement = (Element) child;

            if (!requestElement.hasAttribute("code")) {
                throw invalidConfig(
                        configPath,
                        "Ungültige " + CONFIG_FILE + ": <request> benötigt das Attribut 'code'.",
                        new ErrorDetail("Element", "request"),
                        new ErrorDetail("Attribut", "code")
                );
            }

            String requestCode = requestElement.getAttribute("code").trim();

            if (requestCode.isBlank()) {
                throw invalidConfig(
                        configPath,
                        "Ungültige " + CONFIG_FILE
                                + ": Das Attribut 'code' von <request> darf nicht leer sein.",
                        new ErrorDetail("Element", "request"),
                        new ErrorDetail("Attribut", "code")
                );
            }

            List<String> allowedDecisionCodes = new ArrayList<>();

            NodeList decisionNodes = requestElement.getChildNodes();

            for (int j = 0; j < decisionNodes.getLength(); j++) {
                Node decisionNode = decisionNodes.item(j);

                if (decisionNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!decisionNode.getNodeName().equals("decision")) {
                    throw invalidConfig(
                            configPath,
                            "Ungültige " + CONFIG_FILE + ": Unerwartetes Element <"
                                    + decisionNode.getNodeName() + "> unter <request>.",
                            new ErrorDetail("Request-Code", requestCode),
                            new ErrorDetail("Unerwartetes Element", decisionNode.getNodeName())
                    );
                }

                Element decisionElement = (Element) decisionNode;

                if (!decisionElement.hasAttribute("code")) {
                    throw invalidConfig(
                            configPath,
                            "Ungültige " + CONFIG_FILE
                                    + ": <decision> benötigt das Attribut 'code'.",
                            new ErrorDetail("Request-Code", requestCode),
                            new ErrorDetail("Element", "decision"),
                            new ErrorDetail("Attribut", "code")
                    );
                }

                String decisionCode = decisionElement.getAttribute("code").trim();

                if (decisionCode.isBlank()) {
                    throw invalidConfig(
                            configPath,
                            "Ungültige " + CONFIG_FILE
                                    + ": Das Attribut 'code' von <decision> darf nicht leer sein.",
                            new ErrorDetail("Request-Code", requestCode),
                            new ErrorDetail("Element", "decision"),
                            new ErrorDetail("Attribut", "code")
                    );
                }

                if (allowedDecisionCodes.contains(decisionCode)) {
                    throw invalidConfig(
                            configPath,
                            "Ungültige " + CONFIG_FILE + ": Der Decision-Code '"
                                    + decisionCode + "' ist für Request-Code '" + requestCode
                                    + "' mehrfach vorhanden.",
                            new ErrorDetail("Request-Code", requestCode),
                            new ErrorDetail("Decision-Code", decisionCode)
                    );
                }

                allowedDecisionCodes.add(decisionCode);
            }

            if (allowedDecisionCodes.isEmpty()) {
                throw invalidConfig(
                        configPath,
                        "Ungültige " + CONFIG_FILE + ": Request-Code '" + requestCode
                                + "' enthält keine erlaubten Decisions.",
                        new ErrorDetail("Request-Code", requestCode)
                );
            }

            if (allowedDecisionsByRequestCode.containsKey(requestCode)) {
                throw invalidConfig(
                        configPath,
                        "Ungültige " + CONFIG_FILE + ": Der Request-Code '" + requestCode
                                + "' ist mehrfach vorhanden.",
                        new ErrorDetail("Request-Code", requestCode)
                );
            }

            allowedDecisionsByRequestCode.put(requestCode, allowedDecisionCodes);
        }

        if (allowedDecisionsByRequestCode.isEmpty()) {
            throw invalidConfig(
                    configPath,
                    "Ungültige \"" + CONFIG_FILE + "\": Es ist kein <request>-Eintrag definiert."
            );
        }

        return allowedDecisionsByRequestCode;
    }

    private static AppException invalidConfig(
            Path configPath,
            String message,
            ErrorDetail... context
    ) {
        List<ErrorDetail> details = new ArrayList<>();
        details.add(new ErrorDetail("Datei", configPath.toString()));
        details.addAll(List.of(context));

        return new AppException(message, details);
    }
}
