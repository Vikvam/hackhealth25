package qupath.ext.biop.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.ext.biop.cellpose.Cellpose2D;
import qupath.ext.biop.cellpose.CellposeBuilder;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.util.Collection;

import static qupath.lib.scripting.QP.getSelectedObjects;

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

            // Model
            Label modelLabel = new Label("Enter Model Name:");
            TextField modelField = new TextField();
            modelField.setText("cyto3");
            
            // Diameter
            Label diameterLabel = new Label("Enter Cell Diameter [px] (use 0 for automatic inference):");
            TextField diameterField = new TextField();
            diameterField.setText("30");

            // Resolution
            Label resolutionLabel = new Label("Pixel Resolution (um/px):");
            TextField resolutionField = new TextField();
            
            // Initialize resolution field with QuPath's current image resolution
            ImageData imageData = qugui.getImageData();
            if (imageData != null) {
                System.out.println();
                double resolution = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons();
                resolutionField.setText(String.valueOf(resolution));
            }
            
            // Button
            Button button = new Button("Run Computation");
            
            // Progress
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            
            // Layout
            VBox layout = new VBox(10);
            layout.getChildren().addAll(
                    modelLabel, modelField, 
                    resolutionLabel, resolutionField, 
                    diameterLabel, diameterField,
                    button,
                    progressIndicator
            );
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));

            // Set up the scene
            Scene scene = new Scene(layout, 300, 150);

            button.setOnAction(event -> {
                String modelName = modelField.getText();
                double resolution = Double.parseDouble(resolutionField.getText());
                double diameter = diameterField.getText().isEmpty() ? 0 : Double.parseDouble(diameterField.getText());

                // Create CellposeBuilder with GUI parameters
                CellposeBuilder cellposeBuilder = Cellpose2D.builder(modelName)
                        .pixelSize(resolution)
                        .diameter(diameter)
                        .channels(0, 0)                     // Assuming grayscale for nuclei detection
                        .classify("Ki67 Nuclei")
                        .measureIntensity()
                        .createAnnotations();
                System.out.print(cellposeBuilder);

                Cellpose2D cellpose = cellposeBuilder.build();
                ImageData<BufferedImage> imageDataForDetection = qugui.getImageData();

                Collection<PathObject> pathObjects = getSelectedObjects();
                if (pathObjects == null || pathObjects.isEmpty()) {
                    System.out.println("Please select a parent object!");
                    return;
                }

                progressIndicator.setVisible(true);
                button.setDisable(true);
                button.setVisible(false);
                Thread thread = new Thread(() -> {
                    cellpose.detectObjects(imageDataForDetection, pathObjects);
                    System.out.println("Cellpose detection script done");
        
                    // Update UI on the JavaFX thread
                    Platform.runLater(() -> {
                        button.setDisable(false); // Enable the button again
                        stage.close(); // Close the popup window
                    });
                });
                thread.start();
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
