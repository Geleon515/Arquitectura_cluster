package arqui.grupo5.vendiasender.controller;

import arqui.grupo5.vendiasender.model.Venta;
import arqui.grupo5.vendiasender.sender.FileSender;
import arqui.grupo5.vendiasender.storage.VentaStorage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SenderController {

    @FXML private TextField txtRutaDat;
    @FXML private TextField txtCarpetaDatos;

    @FXML private TableView<Venta>           tablaPendientes;
    @FXML private TableColumn<Venta, String> colId;
    @FXML private TableColumn<Venta, String> colVendedor;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, Double> colMonto;

    @FXML private Button   btnEnviar;
    @FXML private Label    lblEstado;
    @FXML private TextArea txtLog;

    private final ObservableList<Venta> pendientes = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> data.getValue().idVentaProperty());
        colVendedor.setCellValueFactory(data -> data.getValue().idVendedorProperty());
        colFecha.setCellValueFactory(data -> data.getValue().fechaProperty());
        colMonto.setCellValueFactory(data -> data.getValue().montoTotalProperty().asObject());
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("S/. %.2f", item));
            }
        });

        tablaPendientes.setItems(pendientes);
        log("Sistema iniciado. Seleccione ventas.dat y la carpeta compartida DATOS\\.");
    }

    @FXML
    private void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar ventas.dat");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Datos de ventas (*.dat)", "*.dat")
        );
        File archivo = chooser.showOpenDialog(txtRutaDat.getScene().getWindow());
        if (archivo != null) {
            txtRutaDat.setText(archivo.getAbsolutePath());
            cargarPendientes();
        }
    }

    @FXML
    private void onBrowseCarpeta() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta compartida DATOS");
        File carpeta = chooser.showDialog(txtCarpetaDatos.getScene().getWindow());
        if (carpeta != null) {
            txtCarpetaDatos.setText(carpeta.getAbsolutePath());
        }
    }

    @FXML
    private void onCargar() {
        cargarPendientes();
    }

    private void cargarPendientes() {
        String ruta = txtRutaDat.getText().trim();
        if (ruta.isEmpty()) {
            log("ERROR: especifique la ruta del archivo ventas.dat.");
            return;
        }
        try {
            VentaStorage storage = new VentaStorage(ruta);
            if (!storage.existe()) {
                log("ERROR: el archivo no existe -> " + ruta);
                return;
            }
            List<Venta> lista = storage.leerPendientes();
            pendientes.setAll(lista);
            log("Archivo cargado. " + lista.size() + " venta(s) pendiente(s) encontrada(s).");
            lblEstado.setText(lista.size() + " pendiente(s)");
            btnEnviar.setDisable(lista.isEmpty());
        } catch (Exception e) {
            log("ERROR al leer ventas.dat: " + e.getMessage());
        }
    }

    @FXML
    private void onEnviar() {
        String ruta     = txtRutaDat.getText().trim();
        String carpeta  = txtCarpetaDatos.getText().trim();

        if (ruta.isEmpty() || carpeta.isEmpty()) {
            log("ERROR: complete la ruta del archivo y la carpeta compartida antes de enviar.");
            return;
        }

        List<Venta> aEnviar = List.copyOf(pendientes);
        if (aEnviar.isEmpty()) {
            log("No hay ventas pendientes para enviar.");
            return;
        }

        btnEnviar.setDisable(true);
        lblEstado.setText("Enviando...");

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                FileSender sender = new FileSender(carpeta);
                sender.enviar(aEnviar, msg -> Platform.runLater(() -> log(msg)));

                VentaStorage storage = new VentaStorage(ruta);
                storage.marcarEnviadas(aEnviar);
                return null;
            }
        };

        tarea.setOnSucceeded(e -> {
            pendientes.clear();
            log("Ventas marcadas como enviadas (estado=E) en ventas.dat.");
            lblEstado.setText("Envio completado");
            btnEnviar.setDisable(true);
        });

        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            log("ERROR: " + (ex != null ? ex.getMessage() : "fallo desconocido"));
            lblEstado.setText("Error en el envio");
            btnEnviar.setDisable(false);
        });

        new Thread(tarea, "sender-thread").start();
    }

    private void log(String msg) {
        String hora = LocalTime.now().format(TIME_FMT);
        txtLog.appendText("[" + hora + "] " + msg + "\n");
    }
}
