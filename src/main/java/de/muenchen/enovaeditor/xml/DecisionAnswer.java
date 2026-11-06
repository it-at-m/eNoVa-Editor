package de.muenchen.enovaeditor.xml;

public record DecisionAnswer(String requestCode, String requestText, String decisionCode) {

    public String requestHint() {
        return "Antwort auf die Frage: " + requestText + " (" + requestCode + ")";
    }
}
