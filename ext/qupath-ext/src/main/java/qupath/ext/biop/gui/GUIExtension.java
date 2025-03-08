package qupath.ext.biop.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

public class GUIExtension implements QuPathExtension {
    private static final String btnId = "customCellposeButton";
    
    void addButtonToToolbar(QuPathGUI qugui) {
        ToolBar toolBar = qugui.getToolBar();
        toolBar.getItems().removeIf(item -> item.getId() != null && item.getId().equals(btnId));

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setId(btnId);
        toolBar.getItems().add(separator);
        
        Button testButton = new Button(btnId);
        testButton.setId(btnId);
        toolBar.getItems().add(testButton);
        testButton.setOnAction(e -> {
            System.out.println("Button clicked");
            openMenu(qugui);
        });
    }
    
    void openMenu(QuPathGUI qugui) {
        // TODO: add GUI parameters for setting Cellpose Extension Params
        
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Cellpose GUI");

            // Create UI components
            Label label = new Label("Enter Model Name:");
            TextField textField = new TextField();
            Button button = new Button("Run Cellpose");

            // Layout
            VBox layout = new VBox(10);
            layout.getChildren().addAll(label, textField, button);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));

            // Set up the scene
            Scene scene = new Scene(layout, 300, 150);

            // Set button action
            button.setOnAction(event -> {
                String modelName = textField.getText();
                // Here you can execute your Cellpose script using the modelName
                System.out.println("Running Cellpose with model: " + modelName);
            });

            // Show the stage
            stage.setScene(scene);
            stage.show();
        });
    }
    
    @Override
    public void installExtension(QuPathGUI qugui) {
        addButtonToToolbar(qugui);
    }

    @Override
    public String getName() {
        return "GUI Extension for BIOP Cellpose extension";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }
}
