module com.example.smart4aviation {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.google.gson;

    opens com.example.smart4aviation to javafx.fxml;
    exports com.example.smart4aviation;
}