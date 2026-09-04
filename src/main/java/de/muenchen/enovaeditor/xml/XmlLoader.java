package de.muenchen.enovaeditor.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XmlLoader {

    public Document load(File file)
            throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        // Sicherheitsmaßnahmen gegen externe XML-Ressourcen / XXE
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(file);
    }
}