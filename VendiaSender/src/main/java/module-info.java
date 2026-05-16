module arqui.grupo5.vendiasender {
    requires javafx.controls;
    requires javafx.fxml;

    opens arqui.grupo5.vendiasender            to javafx.fxml;
    opens arqui.grupo5.vendiasender.controller to javafx.fxml;
    opens arqui.grupo5.vendiasender.model      to javafx.base;

    exports arqui.grupo5.vendiasender;
}