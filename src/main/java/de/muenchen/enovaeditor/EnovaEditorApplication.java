package de.muenchen.enovaeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class EnovaEditorApplication extends Application {

    private static final double INITIAL_WIDTH = 800;
    private static final double INITIAL_HEIGHT = 520;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(EnovaEditorApplication.class.getResource("main-view.fxml"));

        Scene scene = new Scene(
                fxmlLoader.load(),
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
    }
}
