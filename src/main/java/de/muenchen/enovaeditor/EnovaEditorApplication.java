package de.muenchen.enovaeditor;

import de.muenchen.enovaeditor.error.ProblemDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

public class EnovaEditorApplication extends Application {

    private static final double INITIAL_WIDTH = 800;
    private static final double INITIAL_HEIGHT = 520;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader =
                    new FXMLLoader(EnovaEditorApplication.class.getResource("main-view.fxml"));

            Parent root = fxmlLoader.load();
            MainController controller = fxmlLoader.getController();
            controller.loadStartupConfiguration();

            Scene scene = new Scene(
                    root,
                    INITIAL_WIDTH,
                    INITIAL_HEIGHT
            );

            stage.setTitle("eNoVA Editor");

            stage.getIcons().add(
                    new Image(
                            Objects.requireNonNull(
                                    EnovaEditorApplication.class.getResourceAsStream("icon.png"),
                                    "icon.png not found"
                            )
                    )
            );

            stage.setScene(scene);
            stage.show();

            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        } catch (Exception exception) {
            ProblemDialog.show(
                    Alert.AlertType.ERROR,
                    "Programm konnte nicht gestartet werden",
                    "Der Programmstart wird beendet.",
                    exception,
                    List.of()
            );
            Platform.exit();
        }
    }
}
