package de.muenchen.enovaeditor.config.caseworker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CaseworkerEntry(String name, String xmlBlock) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("<telefon>(.*?)</telefon>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_PATTERN = Pattern.compile("<kennung>(.*?)</kennung>", Pattern.CASE_INSENSITIVE);

    public String extractPhone() {
        if (xmlBlock == null) return "";
        Matcher m = PHONE_PATTERN.matcher(xmlBlock);
        return m.find() ? m.group(1).trim() : "";
    }

    public String extractId() {
        if (xmlBlock == null) return "";
        Matcher m = ID_PATTERN.matcher(xmlBlock);
        return m.find() ? m.group(1).trim() : "";
    }
}
