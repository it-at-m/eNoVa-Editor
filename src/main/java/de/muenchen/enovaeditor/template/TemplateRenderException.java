package de.muenchen.enovaeditor.template;

public class TemplateRenderException extends Exception {

    private final String templateName;
    private final int line;
    private final int column;
    private final String xpathExpression;

    public TemplateRenderException(
            String message,
            String templateName,
            int line,
            int column,
            String xpathExpression,
            Throwable cause
    ) {
        super(message, cause);

        this.templateName = templateName;
        this.line = line;
        this.column = column;
        this.xpathExpression = xpathExpression;
    }

    public String templateName() {
        return templateName;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public String xpathExpression() {
        return xpathExpression;
    }
}
