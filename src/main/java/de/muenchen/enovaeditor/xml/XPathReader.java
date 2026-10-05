package de.muenchen.enovaeditor.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class XPathReader {

    public List<Node> findNodes(Node context, String xpathExpression)
            throws XPathExpressionException {

        XPath xpath = createXPath();

        NodeList nodeList = (NodeList) xpath.evaluate(
                xpathExpression,
                context,
                XPathConstants.NODESET
        );

        List<Node> nodes = new ArrayList<>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }

        return nodes;
    }

    public String readValue(Node context, String xpathExpression)
            throws XPathExpressionException {

        XPath xpath = createXPath();

        return xpath.evaluate(xpathExpression, context);
    }

    private XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new XJustizNamespaceContext());

        return xpath;
    }
}
