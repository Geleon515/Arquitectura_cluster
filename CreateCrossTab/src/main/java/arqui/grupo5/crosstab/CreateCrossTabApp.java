package arqui.grupo5.crosstab;

import arqui.grupo5.crosstab.db.CrossTabRepository;
import arqui.grupo5.crosstab.model.CrossTabRow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class CreateCrossTabApp extends Application {

    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TextField    txtUrl, txtUser;
    private PasswordField txtPass;
    private ComboBox<Integer> cmbAnio;
    private TableView<CrossTabRow> tabla;
    private ObservableList<CrossTabRow> datos;
    private TextArea     txtLog;
    private Button       btnGenerar, btnExportar;
    private Label        lblEstado;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("CreateCrossTab — Generador de Cubos OLAP — LogiMarket Peru S.A.");
        stage.setScene(construirEscena());
        stage.setMinWidth(900); stage.setMinHeight(650);
        stage.show();
        cargarConfig();
    }

    private Scene construirEscena() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(root, Priority.ALWAYS);

        root.getChildren().addAll(
            construirHeader(),
            construirFormulario(),
            construirBotones(),
            construirTabla(),
            construirLog(),
            construirStatusBar()
        );
        return new Scene(root, 1000, 700);
    }

    private HBox construirHeader() {
        HBox h = new HBox();
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        h.setPadding(new Insets(0, 20, 0, 0));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPrefHeight(64);

        Region accent = new Region();
        accent.setPrefWidth(5);
        accent.setStyle("-fx-background-color: #818cf8;");

        VBox textos = new VBox(3);
        textos.setPadding(new Insets(12, 0, 12, 20));
        HBox.setHgrow(textos, Priority.ALWAYS);

        Label t = new Label("CreateCrossTab");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        t.setTextFill(Color.web("#818cf8"));

        textos.getChildren().addAll(t);
        h.getChildren().addAll(accent, textos);
        return h;
    }

    private GridPane construirFormulario() {
        GridPane g = new GridPane();
        g.setPadding(new Insets(16, 20, 8, 20));
        g.setHgap(12); g.setVgap(8);
        g.setStyle("-fx-background-color: #1e293b;");

        txtUrl  = campo("jdbc:mysql://localhost:3306/logimarket_dw");
        txtUser = campo("root");
        txtPass = new PasswordField();
        txtPass.setStyle(estiloInput());

        cmbAnio = new ComboBox<>();
        cmbAnio.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-pref-width: 120;");
        cmbAnio.getItems().addAll(2024, 2025, 2026);
        cmbAnio.setValue(2026);

        g.add(label("DW URL"),     0, 0); g.add(txtUrl,  1, 0);
        g.add(label("Usuario"),    2, 0); g.add(txtUser, 3, 0);
        g.add(label("Contraseña"), 4, 0); g.add(txtPass, 5, 0);
        g.add(label("Año"),        6, 0); g.add(cmbAnio, 7, 0);

        ColumnConstraints col1 = new ColumnConstraints(80);
        ColumnConstraints col2 = new ColumnConstraints(220);
        g.getColumnConstraints().addAll(col1, col2, col1, col1, col1, col1, col1, col1);

        return g;
    }

    private HBox construirBotones() {
        btnGenerar  = boton("Generar CrossTab",  "#4f46e5", "#fff");
        btnExportar = boton("Exportar CSV",       "#059669", "#fff");
        btnExportar.setDisable(true);

        btnGenerar.setOnAction(e  -> ejecutarGeneracion());
        btnExportar.setOnAction(e -> exportarCSV());

        HBox h = new HBox(10, btnGenerar, btnExportar);
        h.setPadding(new Insets(10, 20, 10, 20));
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1 0 0 0;");
        return h;
    }

    @SuppressWarnings("unchecked")
    private VBox construirTabla() {
        datos = FXCollections.observableArrayList();
        tabla = new TableView<>(datos);
        tabla.setStyle("-fx-background-color: #0f172a; -fx-table-cell-border-color: #1e293b;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().addAll(
            col("Vendedor",       "idVendedor",     120),
            col("Producto",       "idProducto",      80),
            col("Nombre",         "nombreProducto", 110),
            colMonto("Q1 (Ene-Mar)", "q1"),
            colMonto("Q2 (Abr-Jun)", "q2"),
            colMonto("Q3 (Jul-Sep)", "q3"),
            colMonto("Q4 (Oct-Dic)", "q4"),
            colMonto("TOTAL ANUAL",  "total")
        );

        Label vacio = new Label("Ejecute 'Generar CrossTab' para computar el cubo OLAP.");
        vacio.setTextFill(Color.web("#475569"));
        tabla.setPlaceholder(vacio);

        VBox v = new VBox();
        v.setStyle("-fx-background-color: #0f172a;");
        v.setPadding(new Insets(0, 20, 0, 20));
        VBox.setVgrow(v, Priority.ALWAYS);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        v.getChildren().add(tabla);
        return v;
    }

    private VBox construirLog() {
        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setPrefHeight(110);
        txtLog.setStyle("-fx-background-color: #020617; -fx-text-fill: #4ade80; -fx-font-family: 'Consolas'; -fx-font-size: 12;");

        VBox v = new VBox(txtLog);
        v.setPadding(new Insets(8, 20, 0, 20));
        v.setStyle("-fx-background-color: #0f172a;");
        return v;
    }

    private HBox construirStatusBar() {
        lblEstado = new Label("Listo. Configure la conexion y presione Generar CrossTab.");
        lblEstado.setFont(Font.font("Segoe UI", 12));
        lblEstado.setTextFill(Color.web("#64748b"));

        HBox h = new HBox(lblEstado);
        h.setPadding(new Insets(8, 20, 8, 20));
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1 0 0 0;");
        return h;
    }

    private void ejecutarGeneracion() {
        String url  = txtUrl.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();
        int    anio = cmbAnio.getValue();

        if (url.isEmpty() || user.isEmpty()) {
            log("ERROR: complete la URL y usuario."); return;
        }

        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);
        lblEstado.setText("Procesando...");

        Task<List<CrossTabRow>> tarea = new Task<>() {
            @Override
            protected List<CrossTabRow> call() throws Exception {
                updateMessage("Conectando al DataWarehouse...");
                try (CrossTabRepository repo = new CrossTabRepository(url, user, pass)) {
                    updateMessage("Computando CrossTab para el anio " + anio + "...");
                    List<CrossTabRow> filas = repo.computarCrossTab(anio);
                    if (filas.isEmpty()) {
                        updateMessage("Sin datos para el anio " + anio + " en fact_ventas.");
                        return filas;
                    }
                    updateMessage("Guardando " + filas.size() + " fila(s) en crosstab_ventas...");
                    int guardadas = repo.guardarCrossTab(filas, anio);
                    updateMessage("CrossTab generado: " + filas.size() + " vendedores, " + guardadas + " fila(s) guardadas.");
                    return filas;
                }
            }
        };

        tarea.messageProperty().addListener((obs, o, msg) -> { if (msg != null) log(msg); });

        tarea.setOnSucceeded(e -> {
            List<CrossTabRow> filas = tarea.getValue();
            datos.setAll(filas);
            lblEstado.setText("CrossTab generado — " + filas.size() + " vendedores.");
            btnExportar.setDisable(filas.isEmpty());
            btnGenerar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            log("ERROR: " + (ex != null ? ex.getMessage() : "fallo desconocido"));
            lblEstado.setText("Error al generar el CrossTab.");
            btnGenerar.setDisable(false);
        });

        new Thread(tarea, "crosstab-thread").start();
    }

    private void exportarCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar CrossTab como CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("crosstab_" + cmbAnio.getValue() + ".csv");
        File archivo = fc.showSaveDialog(tabla.getScene().getWindow());
        if (archivo == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println("Vendedor,Q1 (Ene-Mar),Q2 (Abr-Jun),Q3 (Jul-Sep),Q4 (Oct-Dic),Total Anual");
            for (CrossTabRow r : datos) {
                pw.printf("%s,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                    r.getIdVendedor(), r.getQ1(), r.getQ2(), r.getQ3(), r.getQ4(), r.getTotal());
            }
            log("CSV exportado: " + archivo.getAbsolutePath());
        } catch (IOException ex) {
            log("ERROR al exportar: " + ex.getMessage());
        }
    }

    private void cargarConfig() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("crosstab.properties")) {
            p.load(fis);
        } catch (IOException ignored) {}
        txtUrl.setText(p.getProperty("dw.url",     txtUrl.getText()));
        txtUser.setText(p.getProperty("dw.usuario", txtUser.getText()));
        txtPass.setText(p.getProperty("dw.password", ""));
    }

    private void log(String msg) {
        String hora = LocalTime.now().format(T);
        txtLog.appendText("[" + hora + "] " + msg + "\n");
    }

    // ─── helpers UI ───────────────────────────────────────────────────────────

    private TextField campo(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle(estiloInput());
        return tf;
    }

    private String estiloInput() {
        return "-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0; " +
               "-fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-prompt-text-fill: #475569; -fx-font-size: 13;";
    }

    private Label label(String txt) {
        Label l = new Label(txt);
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web("#64748b"));
        return l;
    }

    private Button boton(String txt, String bg, String fg) {
        Button b = new Button(txt);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                   "; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;");
        return b;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private TableColumn<CrossTabRow, String> col(String titulo, String prop, double ancho) {
        TableColumn<CrossTabRow, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(ancho);
        c.setStyle("-fx-alignment: CENTER-LEFT;");
        return c;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private TableColumn<CrossTabRow, Double> colMonto(String titulo, String prop) {
        TableColumn<CrossTabRow, Double> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("S/. %,.2f", v));
                setStyle(v > 0
                    ? "-fx-text-fill: #4ade80; -fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;"
                    : "-fx-text-fill: #475569; -fx-alignment: CENTER-RIGHT;");
            }
        });
        return c;
    }
}
