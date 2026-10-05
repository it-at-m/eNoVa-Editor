package de.muenchen.enovaeditor.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class XJustizNamespaceContext implements NamespaceContext {

    private static final String XJUSTIZ_NAMESPACE = "http://www.xjustiz.de";

    @Override
    public String getNamespaceURI(String prefix) {
        if ("tns".equals(prefix)) {
            return XJUSTIZ_NAMESPACE;
        }

        return XMLConstants.NULL_NS_URI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (XJUSTIZ_NAMESPACE.equals(namespaceURI)) {
            return "tns";
        }

        return null;
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        if (XJUSTIZ_NAMESPACE.equals(namespaceURI)) {
            return List.of("tns").iterator();
        }

        return Collections.emptyIterator();
    }
}
