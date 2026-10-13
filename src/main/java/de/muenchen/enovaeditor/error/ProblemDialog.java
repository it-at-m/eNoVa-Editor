package de.muenchen.enovaeditor.error;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

public final class ProblemDialog {

    private static final String UNEXPECTED_ERROR_MESSAGE =
            "Ein unerwarteter Fehler ist aufgetreten. Die technischen Details können Sie unten aufklappen.";

    private ProblemDialog() {
    }

    public static void show(
            Alert.AlertType type,
            String title,
            String workflowStatus,
            Throwable problem,
            List<ErrorDetail> additionalDetails
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(problem, "problem");

        List<ErrorDetail> workflowDetails = List.copyOf(
                Objects.requireNonNull(additionalDetails, "additionalDetails")
        );

        String message = UNEXPECTED_ERROR_MESSAGE;
        List<ErrorDetail> problemDetails = List.of();

        if (problem instanceof AppException appException) {
            message = appException.getMessage();
            problemDetails = appException.details();
        }

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(8);

        if (workflowStatus != null && !workflowStatus.isBlank()) {
            content.getChildren().add(createLabel(workflowStatus));
        }

        content.getChildren().add(createLabel(message));

        for (ErrorDetail detail : workflowDetails) {
            content.getChildren().add(createDetailLabel(detail));
        }

        for (ErrorDetail detail : problemDetails) {
            if (!workflowDetails.contains(detail)) {
                content.getChildren().add(createDetailLabel(detail));
            }
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(700);

        TextArea technicalDetails = new TextArea(createStackTrace(problem));
        technicalDetails.setEditable(false);
        technicalDetails.setWrapText(false);
        technicalDetails.setPrefColumnCount(80);
        technicalDetails.setPrefRowCount(20);

        alert.getDialogPane().setExpandableContent(technicalDetails);
        alert.getDialogPane().setExpanded(false);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private static Label createLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private static Label createDetailLabel(ErrorDetail detail) {
        return createLabel(detail.label() + ": " + detail.value());
    }

    private static String createStackTrace(Throwable problem) {
        StringWriter writer = new StringWriter();
        problem.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
