module org.lasantha.fxplanets {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    opens org.lasantha.fxplanets to javafx.fxml;
    exports org.lasantha.fxplanets;
}
