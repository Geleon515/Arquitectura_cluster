package arqui.grupo5.viewcrosstab;

import arqui.grupo5.viewcrosstab.db.CrossTabReader;
import arqui.grupo5.viewcrosstab.model.PivotRow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ViewCrossTabApp extends Application {

    private TextField     txtUrl, txtUser;
    private PasswordField txtPass;
    private ComboBox<Integer> cmbAnio;
    private ComboBox<String>  cmbProducto;
    private TableView<PivotRow> tablaPivot;
    private ObservableList<PivotRow> datos;
    private Canvas canvasBar;
    private Canvas canvasPie;
    private Label lblEstado;
    private Button btnCargar;

    private List<String[]> productosDisponibles;
    private String urlActual, userActual, passActual;

    private static final Color[] Q_COLORS = {
        Color.web("#818cf8"), Color.web("#34d399"),
        Color.web("#fb923c"), Color.web("#f472b6")
    };

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("ViewCrossTab — Visualizador de Cubos OLAP 3D — LogiMarket Peru S.A.");
        stage.setScene(construirEscena());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
        cargarConfig();
        if (!txtUrl.getText().isBlank() && !txtUser.getText().isBlank())
            conectarYCargarProductos();
    }

    private Scene construirEscena() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(root, Priority.ALWAYS);
        root.getChildren().addAll(
            construirHeader(),
            construirFormulario(),
            construirTabPane(),
            construirStatusBar()
        );
        return new Scene(root, 1200, 740);
    }

    private HBox construirHeader() {
        HBox h = new HBox();
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        h.setPrefHeight(64);
        h.setAlignment(Pos.CENTER_LEFT);

        Region accent = new Region();
        accent.setPrefWidth(5);
        accent.setStyle("-fx-background-color: #c084fc;");

        VBox textos = new VBox(3);
        textos.setPadding(new Insets(12, 0, 12, 20));
        HBox.setHgrow(textos, Priority.ALWAYS);

        Label t = new Label("ViewCrossTab");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        t.setTextFill(Color.web("#c084fc"));

        textos.getChildren().addAll(t);
        h.getChildren().addAll(accent, textos);
        return h;
    }

    private HBox construirFormulario() {
        txtUrl  = campo("jdbc:mysql://localhost:3306/logimarket_dw");
        txtUser = campo("root");
        txtPass = new PasswordField();
        txtPass.setStyle(estiloInput());

        cmbAnio = new ComboBox<>();
        cmbAnio.setStyle(estiloCmb());
        cmbAnio.getItems().addAll(2024, 2025, 2026);
        cmbAnio.setValue(2025);

        cmbProducto = new ComboBox<>();
        cmbProducto.setStyle(estiloCmb() + " -fx-pref-width: 180;");
        cmbProducto.getItems().add("TODOS — Todos los productos");
        cmbProducto.setValue("TODOS — Todos los productos");

        Button btnConectar = new Button("Conectar");
        btnConectar.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        btnConectar.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; " +
                             "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 7 14;");
        btnConectar.setOnAction(e -> conectarYCargarProductos());

        btnCargar = new Button("Cargar CrossTab");
        btnCargar.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btnCargar.setStyle("-fx-background-color: #6d28d9; -fx-text-fill: #fff; " +
                           "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;");
        btnCargar.setOnAction(e -> cargarDatos());

        HBox h = new HBox(10);
        h.setPadding(new Insets(10, 20, 10, 20));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        h.getChildren().addAll(
            label("URL"), txtUrl,
            label("Usuario"), txtUser,
            label("Contrasena"), txtPass,
            btnConectar,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            label("Anio"), cmbAnio,
            label("Producto"), cmbProducto,
            btnCargar
        );
        HBox.setHgrow(txtUrl, Priority.ALWAYS);
        return h;
    }

    @SuppressWarnings("unchecked")
    private TabPane construirTabPane() {
        // ── Tab 1: Tabla Pivot ─────────────────────────────────────────────
        datos = FXCollections.observableArrayList();
        tablaPivot = new TableView<>(datos);
        tablaPivot.setStyle("-fx-background-color: #0f172a; -fx-table-cell-border-color: #1e293b;");
        tablaPivot.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tablaPivot.getColumns().addAll(
            colVendedor(),
            colMonto("Q1 Monto",    PivotRow::getQ1,      false),
            colMonto("Q2 Monto",    PivotRow::getQ2,      false),
            colMonto("Q3 Monto",    PivotRow::getQ3,      false),
            colMonto("Q4 Monto",    PivotRow::getQ4,      false),
            colMonto("TOTAL ANUAL", PivotRow::getTotal,   true),
            colTrans("Q1 Trans.",   PivotRow::getCantQ1),
            colTrans("Q2 Trans.",   PivotRow::getCantQ2),
            colTrans("Q3 Trans.",   PivotRow::getCantQ3),
            colTrans("Q4 Trans.",   PivotRow::getCantQ4),
            colTrans("Total Trans.",PivotRow::getCantTotal)
        );

        Label vacio = new Label("Conecte y presione 'Cargar CrossTab'.");
        vacio.setTextFill(Color.web("#475569"));
        tablaPivot.setPlaceholder(vacio);

        VBox tabPivot = new VBox(tablaPivot);
        tabPivot.setPadding(new Insets(12));
        tabPivot.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(tablaPivot, Priority.ALWAYS);
        VBox.setVgrow(tabPivot, Priority.ALWAYS);

        // ── Tab 2: Barras ──────────────────────────────────────────────────
        canvasBar = new Canvas(1100, 500);
        dibujarBarVacio();
        ScrollPane scrollBar = new ScrollPane(canvasBar);
        scrollBar.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");
        VBox tabBar = new VBox(scrollBar);
        tabBar.setPadding(new Insets(12));
        tabBar.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(scrollBar, Priority.ALWAYS);
        VBox.setVgrow(tabBar, Priority.ALWAYS);

        // ── Tab 3: Torta ───────────────────────────────────────────────────
        canvasPie = new Canvas(700, 500);
        dibujarPieVacio();
        VBox tabPie = new VBox(canvasPie);
        tabPie.setPadding(new Insets(12));
        tabPie.setStyle("-fx-background-color: #0f172a;");
        tabPie.setAlignment(Pos.CENTER);
        VBox.setVgrow(tabPie, Priority.ALWAYS);

        TabPane tp = new TabPane(
            new Tab("Tabla Cruzada (Pivot)",          tabPivot),
            new Tab("Grafico de Barras por Vendedor", tabBar),
            new Tab("Distribucion por Trimestre",     tabPie)
        );
        tp.getTabs().forEach(t -> t.setClosable(false));
        tp.setStyle("-fx-background-color: #0f172a; -fx-tab-min-height: 36;");
        VBox.setVgrow(tp, Priority.ALWAYS);
        return tp;
    }

    private HBox construirStatusBar() {
        lblEstado = new Label("Presione 'Conectar' para cargar los productos disponibles.");
        lblEstado.setFont(Font.font("Segoe UI", 12));
        lblEstado.setTextFill(Color.web("#64748b"));
        HBox h = new HBox(lblEstado);
        h.setPadding(new Insets(8, 20, 8, 20));
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1 0 0 0;");
        return h;
    }

    // ─── acciones ────────────────────────────────────────────────────────────

    private void conectarYCargarProductos() {
        urlActual  = txtUrl.getText().trim();
        userActual = txtUser.getText().trim();
        passActual = txtPass.getText();
        lblEstado.setText("Conectando...");

        Task<Void> t = new Task<>() {
            List<String[]>  prods;
            List<Integer>   anios;

            @Override protected Void call() throws Exception {
                try (CrossTabReader r = new CrossTabReader(urlActual, userActual, passActual)) {
                    prods = r.listarProductos();
                    anios = r.listarAnios();
                }
                return null;
            }

            @Override protected void succeeded() {
                productosDisponibles = prods;
                cmbProducto.getItems().clear();
                for (String[] p : prods)
                    cmbProducto.getItems().add(p[0].equals("TODOS") ? "TODOS — Todos los productos"
                                               : p[0] + " — " + p[1]);
                cmbProducto.setValue(cmbProducto.getItems().get(0));

                cmbAnio.getItems().clear();
                cmbAnio.getItems().addAll(anios);
                cmbAnio.setValue(anios.get(0));

                guardarConfig();
                lblEstado.setText("Conectado. " + (prods.size() - 1) + " productos disponibles. Presione 'Cargar CrossTab'.");
            }

            @Override protected void failed() {
                lblEstado.setText("Error al conectar: " + getException().getMessage());
            }
        };
        new Thread(t, "connect-thread").start();
    }

    private void cargarDatos() {
        urlActual  = txtUrl.getText().trim();
        userActual = txtUser.getText().trim();
        passActual = txtPass.getText();
        int    anio = cmbAnio.getValue() != null ? cmbAnio.getValue() : 2025;
        String sel  = cmbProducto.getValue() != null ? cmbProducto.getValue() : "TODOS";
        String idProd = sel.startsWith("TODOS") ? "TODOS" : sel.split(" — ")[0].trim();

        btnCargar.setDisable(true);
        lblEstado.setText("Cargando cubo OLAP...");

        Task<Void> tarea = new Task<>() {
            List<PivotRow>      filas;
            Map<String, Double> totalesQ;

            @Override protected Void call() throws Exception {
                try (CrossTabReader reader = new CrossTabReader(urlActual, userActual, passActual)) {
                    filas    = reader.leerCrossTab(anio, idProd);
                    totalesQ = reader.totalesPorTrimestre(anio, idProd);
                }
                return null;
            }

            @Override protected void succeeded() {
                datos.setAll(filas);
                actualizarBarChart(filas);
                actualizarPieChart(totalesQ);
                String prodLabel = idProd.equals("TODOS") ? "todos los productos" : idProd;
                lblEstado.setText("CrossTab cargado — " + filas.size()
                    + " vendedores, anio " + anio + ", " + prodLabel + ".");
                btnCargar.setDisable(false);
            }

            @Override protected void failed() {
                lblEstado.setText("Error: " + getException().getMessage());
                btnCargar.setDisable(false);
            }
        };
        new Thread(tarea, "view-thread").start();
    }

    // ─── gráfico de barras ───────────────────────────────────────────────────

    private void actualizarBarChart(List<PivotRow> filas) {
        if (filas.isEmpty()) { dibujarBarVacio(); return; }

        double W = Math.max(1100, filas.size() * 160 + 120);
        canvasBar.setWidth(W);
        canvasBar.setHeight(500);

        GraphicsContext gc = canvasBar.getGraphicsContext2D();
        gc.clearRect(0, 0, W, 500);
        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(0, 0, W, 500);

        double top = 50, bottom = 400, left = 80, plotH = bottom - top;
        double max = filas.stream()
            .mapToDouble(r -> Math.max(Math.max(r.getQ1(), r.getQ2()), Math.max(r.getQ3(), r.getQ4())))
            .max().orElse(1);
        if (max == 0) max = 1;

        gc.setStroke(Color.web("#334155")); gc.setLineWidth(1);
        gc.strokeLine(left, top, left, bottom);
        gc.strokeLine(left, bottom, W - 20, bottom);

        for (int i = 0; i <= 4; i++) {
            double y = bottom - plotH * i / 4;
            gc.setStroke(Color.web("#1e293b")); gc.strokeLine(left, y, W - 20, y);
            gc.setFill(Color.web("#64748b")); gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(String.format("S/.%,.0f", max * i / 4), 2, y + 4);
        }

        String[] labels = {"Q1", "Q2", "Q3", "Q4"};
        double groupW = (W - left - 20) / filas.size();
        double barW   = Math.min(groupW * 0.18, 28);
        double gap    = (groupW - barW * 4) / 5;

        for (int i = 0; i < filas.size(); i++) {
            PivotRow r = filas.get(i);
            double[] vals = {r.getQ1(), r.getQ2(), r.getQ3(), r.getQ4()};
            double gx = left + i * groupW + gap;

            for (int q = 0; q < 4; q++) {
                double barH = vals[q] / max * plotH;
                gc.setFill(Q_COLORS[q]);
                gc.fillRoundRect(gx + q * (barW + gap / 4), bottom - barH, barW, barH, 4, 4);
            }
            gc.setFill(Color.web("#94a3b8")); gc.setFont(Font.font("Segoe UI", 10));
            gc.save();
            gc.translate(left + i * groupW + groupW / 2, bottom + 6);
            gc.rotate(30);
            gc.fillText(r.getIdVendedor(), 0, 0);
            gc.restore();
        }

        gc.setFill(Color.web("#e2e8f0"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.fillText("Monto por Vendedor y Trimestre (S/.)", left, 28);

        double lx = left;
        for (int q = 0; q < 4; q++) {
            gc.setFill(Q_COLORS[q]); gc.fillRoundRect(lx, 32, 12, 12, 3, 3);
            gc.setFill(Color.web("#94a3b8")); gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText(labels[q], lx + 16, 43);
            lx += 60;
        }
    }

    private void dibujarBarVacio() {
        GraphicsContext gc = canvasBar.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasBar.getWidth(), canvasBar.getHeight());
        gc.setFill(Color.web("#0f172a")); gc.fillRect(0, 0, canvasBar.getWidth(), canvasBar.getHeight());
        gc.setFill(Color.web("#334155")); gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText("Presione 'Cargar CrossTab' para visualizar.", 80, 250);
    }

    // ─── gráfico de torta ────────────────────────────────────────────────────

    private void actualizarPieChart(Map<String, Double> totalesQ) {
        if (totalesQ == null || totalesQ.isEmpty()) { dibujarPieVacio(); return; }

        GraphicsContext gc = canvasPie.getGraphicsContext2D();
        gc.clearRect(0, 0, 700, 500);
        gc.setFill(Color.web("#0f172a")); gc.fillRect(0, 0, 700, 500);

        String[] keys   = {"Q1 (Ene-Mar)", "Q2 (Abr-Jun)", "Q3 (Jul-Sep)", "Q4 (Oct-Dic)"};
        String[] labels = {"Q1 Ene-Mar",   "Q2 Abr-Jun",   "Q3 Jul-Sep",   "Q4 Oct-Dic"};
        double total = totalesQ.values().stream().mapToDouble(d -> d).sum();
        if (total == 0) { dibujarPieVacio(); return; }

        double cx = 240, cy = 270, r = 170, angle = -90;
        for (int i = 0; i < keys.length; i++) {
            double val   = totalesQ.getOrDefault(keys[i], 0.0);
            double sweep = val / total * 360.0;
            gc.setFill(Q_COLORS[i]);
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, angle, sweep, javafx.scene.shape.ArcType.ROUND);
            gc.setStroke(Color.web("#0f172a")); gc.setLineWidth(2);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, angle, sweep, javafx.scene.shape.ArcType.ROUND);
            angle += sweep;
        }

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillText("Distribucion por Trimestre", 450, 80);

        double ly = 110;
        for (int i = 0; i < keys.length; i++) {
            double val = totalesQ.getOrDefault(keys[i], 0.0);
            double pct = val / total * 100;
            gc.setFill(Q_COLORS[i]); gc.fillRoundRect(450, ly, 16, 16, 4, 4);
            gc.setFill(Color.web("#e2e8f0"));
            gc.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            gc.fillText(labels[i], 474, ly + 12);
            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText(String.format("S/. %,.0f  (%.1f%%)", val, pct), 474, ly + 27);
            ly += 50;
        }

    }

    private void dibujarPieVacio() {
        GraphicsContext gc = canvasPie.getGraphicsContext2D();
        gc.clearRect(0, 0, 700, 500);
        gc.setFill(Color.web("#0f172a")); gc.fillRect(0, 0, 700, 500);
        gc.setFill(Color.web("#334155")); gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText("Presione 'Cargar CrossTab' para visualizar.", 80, 250);
    }

    // ─── helpers UI ──────────────────────────────────────────────────────────

    private TableColumn<PivotRow, String> colVendedor() {
        TableColumn<PivotRow, String> c = new TableColumn<>("Vendedor");
        c.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getIdVendedor()));
        c.setPrefWidth(120);
        return c;
    }

    private TableColumn<PivotRow, Double> colMonto(String titulo,
            java.util.function.Function<PivotRow, Double> getter, boolean bold) {
        TableColumn<PivotRow, Double> c = new TableColumn<>(titulo);
        c.setCellValueFactory(data ->
            new javafx.beans.property.SimpleDoubleProperty(
                getter.apply(data.getValue())).asObject());
        String color = bold ? "#f472b6" : "#4ade80";
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("S/. %,.2f", v));
                setStyle("-fx-text-fill: " + (v > 0 ? color : "#475569") +
                         "; -fx-alignment: CENTER-RIGHT;" +
                         (bold ? " -fx-font-weight: bold;" : ""));
            }
        });
        return c;
    }

    private TableColumn<PivotRow, Integer> colTrans(String titulo,
            java.util.function.Function<PivotRow, Integer> getter) {
        TableColumn<PivotRow, Integer> c = new TableColumn<>(titulo);
        c.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(
                getter.apply(data.getValue())).asObject());
        c.setPrefWidth(80);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toString());
                setStyle("-fx-text-fill: #94a3b8; -fx-alignment: CENTER;");
            }
        });
        return c;
    }

    private void cargarConfig() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("crosstab.properties")) {
            p.load(fis);
        } catch (IOException ignored) {}
        txtUrl.setText(p.getProperty("dw.url",      "jdbc:mysql://localhost:3306/logimarket_dw"));
        txtUser.setText(p.getProperty("dw.usuario",  "root"));
        txtPass.setText(p.getProperty("dw.password", ""));
    }

    private void guardarConfig() {
        Properties p = new Properties();
        p.setProperty("dw.url",      urlActual);
        p.setProperty("dw.usuario",  userActual);
        p.setProperty("dw.password", passActual);
        try (FileOutputStream fos = new FileOutputStream("crosstab.properties")) {
            p.store(fos, "ViewCrossTab - configuracion guardada automaticamente");
        } catch (IOException ignored) {}
    }

    private TextField campo(String ph) {
        TextField tf = new TextField();
        tf.setPromptText(ph); tf.setStyle(estiloInput());
        return tf;
    }

    private String estiloInput() {
        return "-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0; " +
               "-fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-prompt-text-fill: #475569; -fx-font-size: 12; -fx-pref-width: 200;";
    }

    private String estiloCmb() {
        return "-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0; " +
               "-fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    private Label label(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web("#64748b"));
        return l;
    }

}
