package de.muenchen.enovaeditor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class EnovaEditorApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(EnovaEditorApplication.class.getResource("main-view.fxml"));

        Parent root = fxmlLoader.load();
        MainController controller = fxmlLoader.getController();

        // Screen-aware window sizing (accounts for Windows DPI scaling and taskbar height)
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double initialWidth = Math.min(1200, Math.max(900, visualBounds.getWidth() * 0.90));
        double initialHeight = Math.min(760, Math.max(580, visualBounds.getHeight() * 0.88));

        Scene scene = new Scene(root, initialWidth, initialHeight);

        // Whole-window drag and drop
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".xml")) {
                    controller.loadXmlFile(file);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        stage.setTitle("eNoVa-Editor – beBPo Assistent für Vorkaufsrechtsanfragen & Sachentscheidungen");
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinWidth(850);
        stage.setMinHeight(520);
        stage.setScene(scene);

        var iconStream = EnovaEditorApplication.class.getResourceAsStream("icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }

        // Ensure window is always positioned within visible screen bounds
        stage.setX(visualBounds.getMinX() + Math.max(0, (visualBounds.getWidth() - initialWidth) / 2));
        stage.setY(visualBounds.getMinY() + Math.max(0, (visualBounds.getHeight() - initialHeight) / 2));

        stage.show();

        // Check command-line arguments (e.g. file opened via "Öffnen mit" or drag onto exe/cmd)
        List<String> rawArgs = getParameters().getRaw();
        if (!rawArgs.isEmpty()) {
            String arg = rawArgs.get(0);
            File file = new File(arg);
            if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                Platform.runLater(() -> controller.loadXmlFile(file));
            }
        }
    }
}
