package de.muenchen.enovaeditor.template;

import de.muenchen.enovaeditor.codelist.CodelistConfig;
import de.muenchen.enovaeditor.codelist.CodelistDefinition;
import de.muenchen.enovaeditor.codelist.GenericodeReader;
import de.muenchen.enovaeditor.config.ApplicationPaths;
import de.muenchen.enovaeditor.xml.XPathReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathTemplateRenderer {

    private static final Pattern EACH_START = Pattern.compile("\\{\\{#each\\s+xpath:(.+?)}}", Pattern.DOTALL);
    private static final String EACH_END = "{{/each}}";
    private static final Pattern XPATH_PLACEHOLDER = Pattern.compile("\\{\\{xpath:(.+?)}}", Pattern.DOTALL);
    private static final Pattern CODELIST_PLACEHOLDER = Pattern.compile("\\{\\{codelist:([\\w.-]+)\\s+xpath:(.+?)}}", Pattern.DOTALL);
    private final XPathReader xpathReader = new XPathReader();

    public String render(String template, Document document) throws Exception {

        String result = renderEachBlocks(template, document);

        result = renderCodelistPlaceholders(result, document);

        return renderXPathPlaceholders(result, document);
    }

    private String renderEachBlocks(String template, Node context) throws Exception {

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

            List<Node> nodes = xpathReader.findNodes(context, xpathExpression);

            StringBuilder renderedBlock = new StringBuilder();

            for (Node node : nodes) {

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

    private String renderXPathPlaceholders(String template, Node context) throws XPathExpressionException {

        Matcher matcher = XPATH_PLACEHOLDER.matcher(template);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {

            String xpathExpression = matcher.group(1).trim();

            String value = xpathReader.readValue(context, xpathExpression);

            String safeValue = escapeHtml(value);

            matcher.appendReplacement(result, Matcher.quoteReplacement(safeValue));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String renderCodelistPlaceholders(String template, Node context) throws Exception {

        Matcher matcher = CODELIST_PLACEHOLDER.matcher(template);

        StringBuilder result = new StringBuilder();

        CodelistConfig config = new CodelistConfig();

        GenericodeReader reader = new GenericodeReader();

        while (matcher.find()) {

            String codelistName = matcher.group(1).trim();

            String xpathExpression = matcher.group(2).trim();

            String keyValue = xpathReader.readValue(context, xpathExpression).trim();

            CodelistDefinition definition = config.get(codelistName);

            Path file = ApplicationPaths.getApplicationDirectory().resolve("codelists").resolve(definition.file());

            String value = reader.resolve(file, definition, keyValue);

            String safeValue = escapeHtml(value);

            matcher.appendReplacement(result, Matcher.quoteReplacement(safeValue));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String escapeHtml(String value) {

        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
