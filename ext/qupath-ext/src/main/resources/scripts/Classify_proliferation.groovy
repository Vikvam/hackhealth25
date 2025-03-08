import qupath.lib.objects.PathAnnotationObject
import qupath.lib.common.ColorTools

// Get all annotations in the current image
def annotations = getAnnotationObjects()

// Check if there are any annotations
if (annotations.isEmpty()) {
    print "No annotations found!"
    return
}

// Iterate through each annotation
annotations.each { annotation ->
    // Get the internal value (assuming it's stored as a measurement)
    def DAB_max = annotation.getMeasurementList()["DAB: Max"]
    def DAB_mean = annotation.getMeasurementList()["DAB: Mean"]
    
    // Define color based on the value
    def color
    if (DAB_max < 0.3 || DAB_mean < 0.25) {
        color = getColorRGB(0, 255, 0) // Green for low values
    } else if (DAB_max < 0.45) {
        color = getColorRGB(255, 255, 0) // Blue for medium values
    } else {
        color = getColorRGB(255, 0, 0) // Red for high values
    }
    
    // Set the annotation's color
    annotation.setColorRGB(color)
}

// Update the hierarchy to apply changes
fireHierarchyUpdate()

print "Colors assigned to annotations!"