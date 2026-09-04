package de.muenchen.enovaeditor.xml;

import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XmlWriter {

    public void write(
            Document document,
            File targetFile
    ) throws TransformerException {

        TransformerFactory factory =
                TransformerFactory.newInstance();

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
                ""
        );

        Transformer transformer =
                factory.newTransformer();

        transformer.setOutputProperty(
                OutputKeys.ENCODING,
                "UTF-8"
        );

        transformer.setOutputProperty(
                OutputKeys.INDENT,
                "yes"
        );

        transformer.transform(
                new DOMSource(document),
                new StreamResult(targetFile)
        );
    }
}