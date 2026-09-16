module de.muenchen.enovaeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.desktop;
    requires Saxon.HE;

    opens de.muenchen.enovaeditor to javafx.fxml;
    exports de.muenchen.enovaeditor;
}