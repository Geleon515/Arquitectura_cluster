module arqui.grupo5 {
    requires javafx.controls;
    requires javafx.fxml;

    opens arqui.grupo5.ui to javafx.graphics, javafx.base, javafx.controls;
    opens arqui.grupo5.model to javafx.base;

    exports arqui.grupo5.model;
    exports arqui.grupo5.storage;
    exports arqui.grupo5.cluster;
}
