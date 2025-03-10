package qupath.ext.biop.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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
import javafx.util.Callback;
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
import java.util.List;
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
            Scene scene = new Scene(layout, 400, 390);

            button.setOnAction(event -> {
                double flow = Double.parseDouble(resolutionField.getText());
                double resolution = Double.parseDouble(resolutionField.getText());
                double diameter = diameterField.getText().isEmpty() ? 0 : Double.parseDouble(diameterField.getText());
                int channel = channelField.getText().isEmpty() ? 0 : Integer.parseInt(channelField.getText());

                // Create CellposeBuilder with GUI parameters
                CellposeBuilder cellposeBuilder = Cellpose2D.builder(modelField.getText())
                        .pixelSize(resolution)
                        .diameter(diameter)
                        .channels(channel)
                        .cellposeChannels(channel,2) //will be run selected channel and then on a green one (detects nuclei)
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
            stage.setAlwaysOnTop(true);

            // Progress
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);

            // Sliders
            Slider slider1 = new Slider(0, 1.5, 0.35);
            Slider slider2 = new Slider(0, 1, 0.20);
            Slider slider3 = new Slider(0, 2, 0.45);

            // Labels for sliders
            Label label1 = new Label("Minimum Positive Value: 0.35");
            Label label2 = new Label("Minimum Positive Mean: 0.20");
            Label label3 = new Label("Intensity Span: 0.45");

            // Update labels when sliders change
            slider1.valueProperty().addListener((obs, oldVal, newVal) ->
                    label1.setText(String.format("Minimum Positive Value: %.2f", newVal.doubleValue())));
            slider2.valueProperty().addListener((obs, oldVal, newVal) ->
                    label2.setText(String.format("Minimum Positive Mean: %.2f", newVal.doubleValue())));
            slider3.valueProperty().addListener((obs, oldVal, newVal) ->
                    label3.setText(String.format("Intensity Span: %.2f", newVal.doubleValue())));

            // Add listener to run function when any slider changes
            ChangeListener<Number> sliderChangeListener = (obs, oldVal, newVal) -> {
                ColorAnnotations colorAnnotations = new ColorAnnotations(
                        slider1.getValue(),
                        slider2.getValue(),
                        slider3.getValue()
                );
                colorAnnotations.colorAnnotations();
            };

            slider1.valueProperty().addListener(sliderChangeListener);
            slider2.valueProperty().addListener(sliderChangeListener);
            slider3.valueProperty().addListener(sliderChangeListener);

            // Initial computation
            sliderChangeListener.changed(null, null, slider1.getValue());

            // Layout
            VBox layout = new VBox(10);
            layout.getChildren().addAll(
                    label1, slider1,
                    label2, slider2,
                    label3, slider3,
                    progressIndicator
            );
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));

            // Force layout size
            layout.setPrefSize(500, 300);

            // Set up the scene
            Scene scene = new Scene(layout, 500, 300);

            // Set explicit Stage size
            stage.setWidth(500);
            stage.setHeight(300);

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
                int n = (int) Math.ceil(annotation.getChildObjectsAsArray().length / 20.);
                int m = (int) Math.ceil(annotation.getMeasurementList().get("Proliferation Index [%]") * annotation.getMeasurementList().get("#Cells") / 20.);
                System.out.println("Hopkins: N=" + n);
                HopkinsStatistcs statistcs = new HopkinsStatistcs(x, y, w, h);
                statistcs.fillDB(annotation);
                double distribution = statistcs.compute(n, m);
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
        private final double negativeThreshold;
        private final double meanNegativeThreshold;
        private final double positive2Threshold;

        int negative;
        int positive1;
        int positive2;
        int positive3;

        public ColorAnnotations(double negativeThreshold, double meanNegativeThreshold, double positive2Threshold) {
            this.negativeThreshold = negativeThreshold;
            this.meanNegativeThreshold = meanNegativeThreshold;
            this.positive2Threshold = positive2Threshold;
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
                Double DAB_max = annotation.getMeasurementList().get("DAB: Max");
                Double DAB_mean = annotation.getMeasurementList().get("DAB: Mean");
                if (DAB_max.isNaN()) DAB_max = annotation.getMeasurementList().get("Nucleus: DAB OD max");
                if (DAB_mean.isNaN()) DAB_mean = annotation.getMeasurementList().get("Nucleus: DAB OD mean");
                double positive1Threshold = (positive2Threshold - 3 * negativeThreshold)/2;
                // Define color based on the value
                int color;
                if (DAB_max < negativeThreshold && DAB_mean < meanNegativeThreshold) {
                    color = getColorRGB(0, 255, 0); // Green for low values
                    annotation.getMeasurementList().put("Proliferation", 0);
                    negative++;
                }
                else if (DAB_max < positive1Threshold || DAB_mean < meanNegativeThreshold * 1.5) {
                    color = getColorRGB(255, 255, 0); // Yellow for mild
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive1++;
                }
                else if (DAB_max < positive2Threshold || DAB_mean < meanNegativeThreshold * 1.8){
                    color = getColorRGB(255, 165, 0); // Yellow for mild
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive2++;
                }
                else {
                    color = getColorRGB(255, 0, 0); // Red for high values
                    annotation.getMeasurementList().put("Proliferation", 1);
                    positive3++;
                }
                // Set the annotation's color
                annotation.setColorRGB(color);
            }

            selection.getMeasurementList().put("#(Proliferation = 0)", negative);
            selection.getMeasurementList().put("#(Proliferation = 1)", positive1);
            selection.getMeasurementList().put("#(Proliferation = 2)", positive2);
            selection.getMeasurementList().put("#(Proliferation = 3)", positive3);
            int total = negative + positive1 + positive2 + positive3;
            selection.getMeasurementList().put("#Cells", total);
            selection.getMeasurementList().put("Proliferation Index [%]", (double) (positive1 + positive2 + positive3) / total);

            // Update the hierarchy to apply changes
            fireHierarchyUpdate();
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
            private double distribution;
            
            public AnnotationData(String name, double cells, double proliferation, double proliferation0, double proliferation1, double proliferation2, double proliferation3, double distribution) {
                this.name = name;
                this.cells = cells;
                this.proliferation = proliferation;
                this.proliferation0 = proliferation0;
                this.proliferation1 = proliferation1;
                this.proliferation2 = proliferation2;
                this.proliferation3 = proliferation3;
                this.distribution = distribution;
            }

            public String getName() {
                return name;
            }

            public double getCells() {
                return cells;
            }
            public double getProliferation() {
                return proliferation;
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

            public double getDistribution() {
                return distribution;
            }
        }

        public static void displayTable(Collection<PathObject> selection) {
            Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setTitle("Annotation Data");

                // Create a TableView
                TableView<AnnotationData> tableView = new TableView<>();
                tableView.setRowFactory(new Callback<TableView<AnnotationData>, TableRow<AnnotationData>>() {
                    @Override
                    public TableRow<AnnotationData> call(TableView<AnnotationData> tableView) {
                        return new TableRow<AnnotationData>() {
                            @Override
                            protected void updateItem(AnnotationData item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && item.getName().equals("Over all regions")) {
                                    setStyle("-fx-background-color: lightblue;");
                                }
                            }
                        };
                    }
                });


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

                TableColumn<AnnotationData, Double> distributionColumn = new TableColumn<>("Distribution Coefficient");
                distributionColumn.setCellValueFactory(new PropertyValueFactory<>("distribution"));

                // Add columns to the TableView
                tableView.getColumns().addAll(
                        nameColumn, cellsColumn,
                        proliferationColumn,
                        proliferation0Column, proliferation1Column, proliferation2Column, proliferation3Column,
                        distributionColumn
                );

                // Create data
                ObservableList<AnnotationData> data = FXCollections.observableArrayList();
                double proliferation0Total = 0;
                double proliferation1Total = 0;
                double proliferation2Total = 0;
                double proliferation3Total = 0;
                for (PathObject annotation : selection) {
                    try {
                        double proliferation = annotation.getMeasurementList().get("Proliferation Index [%]");
                        Double proliferation0 = annotation.getMeasurementList().get("#(Proliferation = 0)");
                        Double proliferation1 = annotation.getMeasurementList().get("#(Proliferation = 1)");
                        Double proliferation2 = annotation.getMeasurementList().get("#(Proliferation = 2)");
                        Double proliferation3 = annotation.getMeasurementList().get("#(Proliferation = 3)");
                        double distribution = annotation.getMeasurementList().get("Distribution Coefficient");
                        double cells = annotation.getMeasurementList().get("#Cells");
                        String name = annotation.getName();
                        if (!proliferation0.isNaN()) proliferation0Total += proliferation0;
                        if (!proliferation1.isNaN()) proliferation1Total += proliferation1;
                        if (!proliferation2.isNaN()) proliferation2Total += proliferation2;
                        if (!proliferation3.isNaN()) proliferation3Total += proliferation3;
                        data.add(new AnnotationData(name != null ? name : "Unnamed", cells, proliferation, proliferation0, proliferation1, proliferation2, proliferation3, distribution));
                    } catch (Exception e) {
                        System.out.println("Error processing annotation: " + e.getMessage());
                    }
                }
                double positiveTotal = proliferation1Total + proliferation2Total + proliferation3Total;
                double total = proliferation0Total + positiveTotal;
                data.add(new AnnotationData("Over all regions", total, positiveTotal / total, proliferation0Total, proliferation1Total, proliferation2Total, proliferation3Total, Double.NaN));

                // Add data to the TableView
                tableView.setItems(data);


                // Layout
                VBox layout = new VBox(10);
                layout.getChildren().addAll(tableView);
                layout.setAlignment(Pos.CENTER);
                layout.setPadding(new Insets(10));

                // Scene
                Scene scene = new Scene(layout, 900, 300);

                // Stage
                stage.setScene(scene);
                stage.show();
            });
        }
    }

    public class HopkinsStatistcs {
        double x, y, w, h;
        VectorSearch vectorSearch;

        public HopkinsStatistcs(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.h = h;
            this.w = w;
            vectorSearch = new VectorSearch();
            vectorSearch.createConnection();
        }

        public void fillDB(PathObject annotation) {
            // Add all Proliferation!=0 to DB
            vectorSearch.dropTable();
            vectorSearch.initEuclidean();
            for (PathObject path : annotation.getChildObjects()) {
                double proliferation = path.getMeasurementList().get("Proliferation");
                if (proliferation != 0) {
                    vectorSearch.InsertEmbeddings(path.getROI().getCentroidX() + "," + path.getROI().getCentroidY());
                }
            }
        }

        public double compute(int n, int m) {
            double randomDist = 0;
            double positiveDist = 0;
            Random random = new Random();

            for (int i = 0; i < n; i++) {
                double randomX = x + random.nextDouble() * w;
                double randomY = y + random.nextDouble() * h;
                randomDist += vectorSearch.nearestNeighbor(randomX + "," + randomY, false);
            }

            List<String> randPoints = vectorSearch.getRandomVectors(m);
            System.out.println("randPoints: " + randPoints.size());

            for (String point : randPoints) {
                positiveDist += vectorSearch.nearestNeighbor(point, true);
            }

            System.out.println("N: " + n + "; M: " + m);
            System.out.println("Random dist: " + randomDist + "; PositiveDist: " + positiveDist);
            return randomDist / (randomDist + positiveDist);
        }
    }
}
