module arqui.grupo5 {
    requires javafx.controls;
    requires javafx.fxml;

    opens arqui.grupo5.ui to javafx.graphics, javafx.base, javafx.controls;
}
