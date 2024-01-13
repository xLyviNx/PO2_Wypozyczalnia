module org.projektpo2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    opens org.projektpo2.controllers to javafx.fxml;
    exports org.projektpo2 to javafx.graphics;
    opens org.projektpo2 to javafx.graphics;
}
