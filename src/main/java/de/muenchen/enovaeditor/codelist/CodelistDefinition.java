package de.muenchen.enovaeditor.codelist;

public record CodelistDefinition(

        // Name der Codelist-Datei im Ordner "codelists"
        String file,

        // Spalte, in der nach dem Schlüssel gesucht wird
        String keyColumn,

        // Spalte, deren Wert zurückgegeben werden soll
        String valueColumn,

        // XML-Element für einen Eintrag
        String rowElement,

        // XML-Element für einen Spaltenwert
        String valueElement,

        // Attribut, das angibt, zu welcher Spalte ein Wert gehört
        String columnAttribute,

        // XML-Element mit dem eigentlichen Textwert
        String simpleValueElement

) {
}