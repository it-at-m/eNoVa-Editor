package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.codelist.CodelistConfig;
import de.muenchen.enovaeditor.codelist.CodelistDefinition;
import de.muenchen.enovaeditor.codelist.GenericodeReader;
import de.muenchen.enovaeditor.config.ApplicationPaths;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathTemplateRenderer {

    private static final String XJUSTIZ_NAMESPACE = "http://www.xjustiz.de";

    private static final Pattern EACH_START = Pattern.compile("\\{\\{#each\\s+xpath:(.+?)}}", Pattern.DOTALL);

    private static final String EACH_END = "{{/each}}";

    private static final Pattern XPATH_PLACEHOLDER = Pattern.compile("\\{\\{xpath:(.+?)}}", Pattern.DOTALL);

    private static final Pattern CODELIST_PLACEHOLDER = Pattern.compile("\\{\\{codelist:([\\w.-]+)\\s+xpath:(.+?)}}", Pattern.DOTALL);

    public String render(String template, Document document) throws Exception {

        String result = renderEachBlocks(template, document);

        result = renderCodelistPlaceholders(result, document);

        return renderXPathPlaceholders(result, document);
    }

    private String renderEachBlocks(String template, Object context) throws Exception {

        XPath xpath = createXPath();

        Matcher matcher = EACH_START.matcher(template);

        StringBuilder result = new StringBuilder();

        int position = 0;

        while (matcher.find(position)) {

            result.append(template, position, matcher.start());

            String xpathExpression = matcher.group(1).trim();

            int blockStart = matcher.end();

            int blockEnd = findMatchingEachEnd(template, blockStart);

            if (blockEnd == -1) {
                throw new IllegalArgumentException("Fehlendes {{/each}} im Template.");
            }

            String block = template.substring(blockStart, blockEnd);

            NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, context, XPathConstants.NODESET);

            StringBuilder renderedBlock = new StringBuilder();

            for (int i = 0; i < nodes.getLength(); i++) {

                Node node = nodes.item(i);

                String renderedItem = renderEachBlocks(block, node);

                renderedItem = renderCodelistPlaceholders(renderedItem, node);

                renderedItem = renderXPathPlaceholders(renderedItem, node);

                renderedBlock.append(renderedItem);
            }

            result.append(renderedBlock);

            position = blockEnd + EACH_END.length();
        }

        result.append(template.substring(position));

        return result.toString();
    }

    private int findMatchingEachEnd(String template, int blockStart) {

        int depth = 1;
        int position = blockStart;

        while (depth > 0) {

            Matcher nestedStart = EACH_START.matcher(template);

            int nextStart = nestedStart.find(position) ? nestedStart.start() : -1;

            int nextEnd = template.indexOf(EACH_END, position);

            if (nextEnd == -1) {
                return -1;
            }

            if (nextStart != -1 && nextStart < nextEnd) {
                depth++;
                position = nestedStart.end();
            } else {
                depth--;

                if (depth == 0) {
                    return nextEnd;
                }

                position = nextEnd + EACH_END.length();
            }
        }

        return -1;
    }

    private String renderXPathPlaceholders(String template, Object context) throws XPathExpressionException {

        XPath xpath = createXPath();

        Matcher matcher = XPATH_PLACEHOLDER.matcher(template);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {

            String xpathExpression = matcher.group(1).trim();

            String value = xpath.evaluate(xpathExpression, context);

            String safeValue = escapeHtml(value);

            matcher.appendReplacement(result, Matcher.quoteReplacement(safeValue));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String renderCodelistPlaceholders(String template, Object context) throws Exception {

        XPath xpath = createXPath();

        Matcher matcher = CODELIST_PLACEHOLDER.matcher(template);

        StringBuilder result = new StringBuilder();

        CodelistConfig config = new CodelistConfig();

        GenericodeReader reader = new GenericodeReader();

        while (matcher.find()) {

            String codelistName = matcher.group(1).trim();

            String xpathExpression = matcher.group(2).trim();

            String keyValue = xpath.evaluate(xpathExpression, context).trim();

            CodelistDefinition definition = config.get(codelistName);

            Path file = ApplicationPaths.getApplicationDirectory().resolve("codelists").resolve(definition.file());

            String value = reader.resolve(file, definition, keyValue);

            String safeValue = escapeHtml(value);

            matcher.appendReplacement(result, Matcher.quoteReplacement(safeValue));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private XPath createXPath() {

        XPath xpath = XPathFactory.newInstance().newXPath();

        xpath.setNamespaceContext(new XJustizNamespaceContext());

        return xpath;
    }

    private String escapeHtml(String value) {

        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static class XJustizNamespaceContext implements NamespaceContext {

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
                return Set.of("tns").iterator();
            }

            return Collections.emptyIterator();
        }
    }
}
