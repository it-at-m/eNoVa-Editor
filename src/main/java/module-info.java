module de.muenchen.enovaeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.desktop;

    opens de.muenchen.enovaeditor to javafx.fxml;
    exports de.muenchen.enovaeditor;
}