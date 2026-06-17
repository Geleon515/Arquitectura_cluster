module arqui.grupo5.vendiasender {
    requires javafx.controls;
    requires javafx.fxml;

    // --- LÍNEA NUEVA: Permiso para usar la librería FTP ---
    requires org.apache.commons.net;

    opens arqui.grupo5.vendiasender            to javafx.fxml;
    opens arqui.grupo5.vendiasender.controller to javafx.fxml;
    opens arqui.grupo5.vendiasender.model      to javafx.base;

    exports arqui.grupo5.vendiasender;
}