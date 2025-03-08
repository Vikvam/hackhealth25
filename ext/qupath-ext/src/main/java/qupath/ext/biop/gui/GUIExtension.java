package qupath.ext.biop.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.ext.biop.cellpose.Cellpose2D;
import qupath.ext.biop.cellpose.CellposeBuilder;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.roi.interfaces.ROI;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Random;

import static qupath.lib.gui.scripting.QPEx.getQuPath;
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
            channelField.setText("Red");
            
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
            Collection<PathObject> selection = getSelectedObjects();
            AnnotationTable.displayTable(selection);
        });
    }
    
    void distributionMenu(QuPathGUI qugui) {
        Platform.runLater(() -> {
            Collection<PathObject> selection = getSelectedObjects();
            for (PathObject annotation : selection) {
                // Get ROI bounding box
                ROI roi = annotation.getROI();
                double x, y, w, h;
                if (roi != null) {
                    // Get the bounding box coordinates
                    x = roi.getBoundsX();
                    y = roi.getBoundsY();
                    w = roi.getBoundsWidth();
                    h = roi.getBoundsHeight();
                    System.out.println("Bounding Box: x=" + x + ", y=" + y + ", width=" + w + ", height=" + h);
                } else {
                    System.out.println("No ROI available for this PathObject");
                    return;
                }
                int n = annotation.getChildObjectsAsArray().length;
                System.out.println("Hopkins: N=" + n);
                HopkinsStatistcs statistcs = new HopkinsStatistcs(x, y, w, h);
                statistcs.fillDB(annotation);
                double distribution = statistcs.compute(n);
                System.out.println("Hopkins: coeff=" + distribution);
                annotation.getMeasurementList().put("Distribution Coefficient", distribution);
            }
        });
    }
    
    @Override
    public void installExtension(QuPathGUI qugui) {
        addButtonToToolbar(qugui, "segmentation", "Segmentation", () -> segmentationMenu(qugui));
        addButtonToToolbar(qugui, "proliferation", "Proliferation", () -> proliferationMenu(qugui));
        addButtonToToolbar(qugui, "results", "Results", () -> resultsMenu(qugui));
        addButtonToToolbar(qugui, "distribution", "Distribution", () -> distributionMenu(qugui));

        ObservableList<PathClass> pathClasses = getQuPath().getAvailablePathClasses();
        PathClass[] listOfClasses = {
                getPathClass("Proliferation=0", makeRGB(0, 255, 0)),
                getPathClass("Proliferation=1", makeRGB(200, 180, 0)),
                getPathClass("Proliferation=2", makeRGB(230, 100, 0)),
                getPathClass("Proliferation=3", makeRGB(255, 0, 0))
        };
        pathClasses.setAll(listOfClasses);
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
        int positive3;
        
        public ColorAnnotations(double highestNegativeDAB, double meanNegativeDAB, double positive1DAB) {
            this.highestNegativeDAB = highestNegativeDAB;
            this.meanNegativeDAB = meanNegativeDAB;
            this.positive1DAB = positive1DAB;
            negative = 0;
            positive1 = 0;
            positive2 = 0;
            positive3 = 0;
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
                    // color = getColorRGB(0, 255, 0); 
                    color = PathClass.getInstance("Proliferation=0").getColor();
                    annotation.getMeasurementList().put("Proliferation", 0);
                    negative++;
                } else if (DAB_max < positive1DAB) {
                    // color = getColorRGB(255, 0, 0); 
                    color = PathClass.getInstance("Proliferation=1").getColor();
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive1++;
                } else {
                    // color = getColorRGB(255, 0, 0); 
                    color = PathClass.getInstance("Proliferation=3").getColor();
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive2++;
                }
                // TODO: Add Proliferation3
                // Set the annotation's color
                annotation.setColorRGB(color);
            }
            
            selection.getMeasurementList().put("#(Proliferation = 0)", negative);
            selection.getMeasurementList().put("#(Proliferation = 1)", positive1);
            selection.getMeasurementList().put("#(Proliferation = 2)", positive2);
            selection.getMeasurementList().put("#(Proliferation = 3)", positive3);
            int positive = positive1 + positive2 + positive3;
            double total = negative + positive;
            selection.getMeasurementList().put("#Cells", total);
            selection.getMeasurementList().put("Proliferation Index [%]", positive / total);
    
            // Update the hierarchy to apply changes
            fireHierarchyUpdate();
    
            System.out.println("Colors assigned to annotations!");
        }
    }
    
    public class AnnotationTable {
        public static class AnnotationData {
            private String name;
            private double cells;
            private double proliferation;
            private double proliferation0;
            private double proliferation1;
            private double proliferation2;
            private double proliferation3;
    
            public AnnotationData(String name, double cells, double proliferation, double proliferation0, double proliferation1, double proliferation2, double proliferation3) {
                this.name = name;
                this.cells = cells;
                this.proliferation0 = proliferation0;
                this.proliferation1 = proliferation1;
                this.proliferation2 = proliferation2;
                this.proliferation3 = proliferation3;
            }
    
            public String getName() {
                return name;
            }
    
            public double getCells() {
                return cells;
            }
    
            public double getProliferation0() {
                return proliferation0;
            }
    
            public double getProliferation1() {
                return proliferation1;
            }
    
            public double getProliferation2() {
                return proliferation2;
            }
    
            public double getProliferation3() {
                return proliferation3;
            }
        }
    
        public static void displayTable(Collection<PathObject> selection) {
            Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setTitle("Annotation Data");
    
                // Create a TableView
                TableView<AnnotationData> tableView = new TableView<>();
    
                // Create columns
                TableColumn<AnnotationData, String> nameColumn = new TableColumn<>("Name");
                nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    
                TableColumn<AnnotationData, Double> cellsColumn = new TableColumn<>("Cells");
                cellsColumn.setCellValueFactory(new PropertyValueFactory<>("cells"));
    
                TableColumn<AnnotationData, Double> proliferationColumn = new TableColumn<>("Proliferation Index [%]");
                proliferationColumn.setCellValueFactory(new PropertyValueFactory<>("proliferation"));
    
                TableColumn<AnnotationData, Double> proliferation0Column = new TableColumn<>("#(Proliferation = 0)");
                proliferation0Column.setCellValueFactory(new PropertyValueFactory<>("proliferation0"));
    
                TableColumn<AnnotationData, Double> proliferation1Column = new TableColumn<>("#(Proliferation = 1)");
                proliferation1Column.setCellValueFactory(new PropertyValueFactory<>("proliferation1"));
    
                TableColumn<AnnotationData, Double> proliferation2Column = new TableColumn<>("#(Proliferation = 2)");
                proliferation2Column.setCellValueFactory(new PropertyValueFactory<>("proliferation2"));
    
                TableColumn<AnnotationData, Double> proliferation3Column = new TableColumn<>("#(Proliferation = 3)");
                proliferation3Column.setCellValueFactory(new PropertyValueFactory<>("proliferation3"));
    
                // Add columns to the TableView
                tableView.getColumns().addAll(
                        nameColumn, cellsColumn,
                        proliferationColumn,
                        proliferation0Column, proliferation1Column, proliferation2Column, proliferation3Column
                );
    
                // Create data
                ObservableList<AnnotationData> data = FXCollections.observableArrayList();
                double positive = 0;
                double total = 0;
    
                for (PathObject annotation : selection) {
                    try {
                        double proliferation = annotation.getMeasurementList().get("Proliferation Index [%]");
                        double proliferation0 = annotation.getMeasurementList().get("#(Proliferation = 0)");
                        double proliferation1 = annotation.getMeasurementList().get("#(Proliferation = 1)");
                        double proliferation2 = annotation.getMeasurementList().get("#(Proliferation = 2)");
                        double proliferation3 = annotation.getMeasurementList().get("#(Proliferation = 3)");
                        double cells = annotation.getMeasurementList().get("#Cells");
                        String name = annotation.getName();
                        positive += proliferation * cells;
                        total += cells;
                        data.add(new AnnotationData(name != null ? name : "Unnamed", cells, proliferation, proliferation0, proliferation1, proliferation2, proliferation3));
                    } catch (Exception e) {
                        System.out.println("Error processing annotation: " + e.getMessage());
                    }
                }
    
                // Add data to the TableView
                tableView.setItems(data);
    
                // Add total label
                Label totalLabel = new Label("Over all regions: " + total + " cells; proliferation " + positive / total);
    
                // Layout
                VBox layout = new VBox(10);
                layout.getChildren().addAll(tableView, totalLabel);
                layout.setAlignment(Pos.CENTER);
                layout.setPadding(new Insets(10));
    
                // Scene
                Scene scene = new Scene(layout, 400, 300);
    
                // Stage
                stage.setScene(scene);
                stage.show();
            });
        }
    }

    public class HopkinsStatistcs {
        double x, y, w, h;
        
        public HopkinsStatistcs(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.h = h;
            this.w = w;
        }
        
        public void fillDB(PathObject annotation) {
            // Add all Proliferation!=0 to DB
            for (PathObject path : annotation.getChildObjects()) {
                double proliferation = path.getMeasurementList().get("Proliferation Index [%]");
                if (!Double.isNaN(proliferation) && proliferation != 0) {
                    // TODO (me): Add to DB
                }
            }  
        }
        
        public double compute(int n) {
            double randomDist = 0;
            double positiveDist = 0;
            Random random = new Random();
            
            for (int i = 0; i < n; i++) {
                double randomX = x + random.nextDouble() * w;
                double randomY = y + random.nextDouble() * h;
                // TODO: query NN
            }
            
            for (int i = 0; i < n; i++) {
                // TODO: query for random point in DB
                // TODO: query NN
            }
            
            return randomDist / (randomDist + positiveDist);
        }
    }
}
