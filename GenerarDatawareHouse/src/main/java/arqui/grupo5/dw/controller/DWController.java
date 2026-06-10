package arqui.grupo5.dw.controller;

import arqui.grupo5.dw.db.DWRepository;
import arqui.grupo5.dw.db.OperacionalRepository;
import arqui.grupo5.dw.model.VentaOperacional;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class DWController {

    @FXML private TextField     txtBdUrl;
    @FXML private TextField     txtBdUser;
    @FXML private PasswordField txtBdPass;

    @FXML private TextField     txtDwUrl;
    @FXML private TextField     txtDwUser;
    @FXML private PasswordField txtDwPass;

    @FXML private Button   btnGenerar;
    @FXML private Label    lblEstado;
    @FXML private TextArea txtLog;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        cargarConfiguracion();
        log("Sistema listo. Configure las conexiones y presione 'Generar DataWarehouse'.");
    }

    private void cargarConfiguracion() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("dw.properties")) {
            props.load(fis);
        } catch (IOException e) {
            log("AVISO: no se encontro dw.properties, usando valores por defecto.");
        }
        txtBdUrl.setText( props.getProperty("bd.url",      "jdbc:mysql://localhost:3306/logimarket"));
        txtBdUser.setText(props.getProperty("bd.usuario",  "root"));
        txtBdPass.setText(props.getProperty("bd.password", ""));
        txtDwUrl.setText( props.getProperty("dw.url",      "jdbc:mysql://localhost:3306/logimarket_dw"));
        txtDwUser.setText(props.getProperty("dw.usuario",  "root"));
        txtDwPass.setText(props.getProperty("dw.password", ""));
    }

    @FXML
    private void onGenerarDW() {
        String bdUrl  = txtBdUrl.getText().trim();
        String bdUser = txtBdUser.getText().trim();
        String bdPass = txtBdPass.getText();
        String dwUrl  = txtDwUrl.getText().trim();
        String dwUser = txtDwUser.getText().trim();
        String dwPass = txtDwPass.getText();

        if (bdUrl.isEmpty() || dwUrl.isEmpty()) {
            log("ERROR: complete las URLs de conexion antes de continuar.");
            return;
        }

        btnGenerar.setDisable(true);
        lblEstado.setText("Procesando...");

        Task<Integer> tarea = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                updateMessage("Conectando al servidor de BD operacional...");
                List<VentaOperacional> ventas;
                try (Connection bdConn = DriverManager.getConnection(bdUrl, bdUser, bdPass)) {
                    ventas = new OperacionalRepository(bdConn).leerVentasEnviadas();
                }
                updateMessage("Leidas " + ventas.size() + " venta(s) del servidor de BD.");

                if (ventas.isEmpty()) {
                    updateMessage("No hay ventas en la BD operacional. El DW no fue modificado.");
                    return 0;
                }

                updateMessage("Conectando al servidor de DataWarehouse...");
                int filas;
                try (DWRepository dw = new DWRepository(dwUrl, dwUser, dwPass)) {
                    updateMessage("Ejecutando ETL...");
                    filas = dw.cargarDW(ventas);
                }
                updateMessage("ETL completado: " + ventas.size() + " venta(s) procesada(s), "
                              + filas + " fila(s) cargada(s) en fact_ventas.");
                return filas;
            }
        };

        tarea.messageProperty().addListener((obs, old, msg) -> {
            if (msg != null && !msg.isEmpty()) log(msg);
        });

        tarea.setOnSucceeded(e -> {
            lblEstado.setText("Completado");
            btnGenerar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            log("ERROR: " + (ex != null ? ex.getMessage() : "fallo desconocido"));
            lblEstado.setText("Error");
            btnGenerar.setDisable(false);
        });

        new Thread(tarea, "etl-thread").start();
    }

    private void log(String msg) {
        String hora = LocalTime.now().format(TIME_FMT);
        txtLog.appendText("[" + hora + "] " + msg + "\n");
    }
}
