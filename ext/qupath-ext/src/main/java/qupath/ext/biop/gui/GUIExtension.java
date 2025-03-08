package qupath.ext.biop.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.HLineTo;
import javafx.stage.Stage;
import qupath.ext.biop.cellpose.Cellpose2D;
import qupath.ext.biop.cellpose.CellposeBuilder;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

import static qupath.lib.scripting.QP.*;

public class GUIExtension implements QuPathExtension {
    void addButtonToToolbar(QuPathGUI qugui, String btnId, String label, Runnable handler) {
        ToolBar toolBar = qugui.getToolBar();
        toolBar.getItems().removeIf(item -> item.getId() != null && item.getId().equals(btnId));

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setId(btnId);
        toolBar.getItems().add(separator);
        
        Button testButton = new Button(label);
        testButton.setId(btnId);
        toolBar.getItems().add(testButton);
        testButton.setOnAction(e -> {
            System.out.println("Button clicked");
            handler.run();
        });
    }
    
    void segmentationMenu(QuPathGUI qugui) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Cellpose GUI");
            
            // Diameter
            Label diameterLabel = new Label("Enter Cell Diameter [px] (use 0 for automatic inference):");
            TextField diameterField = new TextField();
            diameterField.setText("30");
            
            // Model
            Label modelLabel = new Label("Enter Model Name:");
            TextField modelField = new TextField();
            modelField.setText("cyto3");
            
            // Channel
            Label channelLabel = new Label("Enter Channel Name:");
            TextField channelField = new TextField();
            channelField.setText("0");
            
            // Flow
            Label flowLabel = new Label("Enter Flow Threshold:");
            TextField flowField = new TextField();
            flowField.setText("0");
            
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
            
            TitledPane titledPane = new TitledPane();
            titledPane.setText("Advanced Options");
            VBox tiledBox = new VBox();
            tiledBox.getChildren().addAll(
                modelLabel, modelField,
                channelLabel, channelField,
                flowLabel, flowField,
                resolutionLabel, resolutionField
            );
            titledPane.setContent(tiledBox);
            
            // Button
            Button button = new Button("Run Computation");
            
            // Progress
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            
            // Layout
            VBox layout = new VBox(15);
            layout.getChildren().addAll(
                    diameterLabel, diameterField,
                    titledPane,
                    button,
                    progressIndicator
            );
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));

            // Set up the scene
            Scene scene = new Scene(layout, 300, 150);

            button.setOnAction(event -> {
                double flow = Double.parseDouble(resolutionField.getText());
                double resolution = Double.parseDouble(resolutionField.getText());
                double diameter = diameterField.getText().isEmpty() ? 0 : Double.parseDouble(diameterField.getText());

                // Create CellposeBuilder with GUI parameters
                CellposeBuilder cellposeBuilder = Cellpose2D.builder(modelField.getText())
                        .pixelSize(resolution)
                        .diameter(diameter)
                        .channels(channelField.getText())                         // Assuming grayscale for nuclei detection
                        .flowThreshold(flow)
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
    
    void proliferationMenu(QuPathGUI qugui) {
        // TODO: add GUI parameters for setting Cellpose Extension Params
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Proliferation");
            
            // TODO: Proliferation 0 / Negative threshold
            
            // Button
            Button button = new Button("Run Computation");
            
            // Progress
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            
            // Layout
            VBox layout = new VBox(10);
            layout.getChildren().addAll(
                    button,
                    progressIndicator
            );
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));

            // Set up the scene
            Scene scene = new Scene(layout, 300, 150);

            button.setOnAction(event -> {
                ColorAnnotations colorAnnotations = new ColorAnnotations(.3, .25, .45);
                // Call the method to color annotations
                colorAnnotations.colorAnnotations();
            });
            
            // Show the stage
            stage.setScene(scene);
            stage.show();
        });
    }
    
    void resultsMenu(QuPathGUI qugui) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Evaluation");
            
            Collection<PathObject> selection = getSelectedObjects();
            
            if (selection.isEmpty()) {
                System.out.println("No annotations selected!");
            }
            
            // Layout
            VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));
            
            VBox details = new VBox(10);
            details.setAlignment(Pos.CENTER);
            details.setPadding(new Insets(10));
            
            // Iterate through each annotation
            double positive = 0;
            double total = 0;
            for (PathObject annotation : selection) {
                double proliferation = annotation.getMeasurementList().get("Proliferation Index [%]");
                double cells = annotation.getMeasurementList().get("#Cells");
                String name = annotation.getName();
                positive += proliferation * cells;
                total += cells;
                details.getChildren().add(new Label((name.isBlank() ? name : "Unnamed") + ": " + cells + " cells; proliferation " + proliferation));
            }
            
            layout.getChildren().addAll(
                    details,
                    new Label("Total:\n " + total + " cells; proliferation " + positive / total)
            );

            // Set up the scene
            Scene scene = new Scene(layout, 300, 150);
            
            // Show the stage
            stage.setScene(scene);
            stage.show();
        });
    }
    
    @Override
    public void installExtension(QuPathGUI qugui) {
        addButtonToToolbar(qugui, "segmentation", "Segmentation", () -> segmentationMenu(qugui));
        addButtonToToolbar(qugui, "proliferation", "Proliferation", () -> proliferationMenu(qugui));
        addButtonToToolbar(qugui, "results", "Results", () -> resultsMenu(qugui));
    }

    @Override
    public String getName() {
        return "GUI Extension for BIOP Cellpose extension";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }
    
    public class ColorAnnotations {
        private final double highestNegativeDAB;
        private final double meanNegativeDAB;
        private final double positive1DAB;
        
        int negative;
        int positive1;
        int positive2;
        
        public ColorAnnotations(double highestNegativeDAB, double meanNegativeDAB, double positive1DAB) {
            this.highestNegativeDAB = highestNegativeDAB;
            this.meanNegativeDAB = meanNegativeDAB;
            this.positive1DAB = positive1DAB;
            negative = 0;
            positive1 = 0;
            positive2 = 0;
        }        
        
        public void colorAnnotations() {
            // Get all annotations in the current image
            PathObject selection = getSelectedObject();
    
            // Check if there are any annotations
            if (selection == null) {
                System.out.println("No annotations selected!");
                // TODO: should be a warning
                return;
            }
    
            // Iterate through each annotation
            for (PathObject annotation : selection.getChildObjects()) {
                // Get the internal value (assuming it's stored as a measurement)
                double DAB_max = annotation.getMeasurementList().get("DAB: Max");
                double DAB_mean = annotation.getMeasurementList().get("DAB: Mean");
                // Define color based on the value
                int color;
                if (DAB_max < highestNegativeDAB || DAB_mean < meanNegativeDAB) {
                    color = getColorRGB(0, 255, 0); // Green for low values
                    annotation.getMeasurementList().put("Proliferation", 0);
                    negative++;
                } else if (DAB_max < positive1DAB) {
                    color = getColorRGB(255, 255, 0); // Yellow for medium values
                    annotation.getMeasurementList().put("Proliferation", 0);
                    positive1++;
                } else {
                    color = getColorRGB(255, 0, 0); // Red for high values
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive2++;
                }
                // Set the annotation's color
                annotation.setColorRGB(color);
            }
            
            selection.getMeasurementList().put("#(Proliferation = 0)", negative);
            selection.getMeasurementList().put("#(Proliferation = 1)", positive1);
            selection.getMeasurementList().put("#(Proliferation = 2)", positive2);
            int total = negative + positive1 + positive2;
            selection.getMeasurementList().put("#Cells", total);
            selection.getMeasurementList().put("Proliferation Index [%]", (double) (positive1 + positive2) / total);
    
            // Update the hierarchy to apply changes
            fireHierarchyUpdate();
    
            System.out.println("Colors assigned to annotations!");
        }
    }
}
