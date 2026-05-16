package arqui.grupo5.ui;

import arqui.grupo5.controller.VentaController;
import arqui.grupo5.model.Venta;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class VendiaApp extends Application {

    private VentaController controller;

    private TableView<VentaFX> tabla;
    private ObservableList<VentaFX> datosTabla;

    private TextField txtIdVendedor;
    private TextField txtMonto;
    private TextField txtBuscarId;
    private TextField txtNuevoMonto;
    private Label     lblStatus;
    private Label     lblContador;
    private Label     lblResultadoBusqueda;

    // Paleta de colores — tema claro pastel
    private static final String BG_APP     = "#f8fafc";
    private static final String BG_SURFACE = "#ffffff";
    private static final String BG_MUTED   = "#f1f5f9";
    private static final String BORDER     = "#e2e8f0";
    private static final String TEXT_PRI   = "#1e293b";
    private static final String TEXT_SEC   = "#64748b";
    private static final String TEXT_MUT   = "#94a3b8";
    private static final String BRAND      = "#4f46e5";

    // ─── Wrapper JavaFX para TableView ───────────────────────────────────

    public static class VentaFX {
        private final StringProperty idVenta    = new SimpleStringProperty();
        private final StringProperty idVendedor = new SimpleStringProperty();
        private final StringProperty fecha      = new SimpleStringProperty();
        private final DoubleProperty monto      = new SimpleDoubleProperty();
        private final StringProperty estado     = new SimpleStringProperty();

        public VentaFX(Venta v) {
            idVenta.set(v.getIdVenta());
            idVendedor.set(v.getIdVendedor());
            fecha.set(v.getFecha());
            monto.set(v.getMontoTotal());
            estado.set(estadoTexto(v.getEstado()));
        }

        private String estadoTexto(char e) {
            return switch (e) {
                case 'P' -> "Pendiente";
                case 'E' -> "Enviado";
                case 'X' -> "Eliminado";
                default  -> String.valueOf(e);
            };
        }

        public StringProperty idVentaProperty()    { return idVenta; }
        public StringProperty idVendedorProperty() { return idVendedor; }
        public StringProperty fechaProperty()      { return fecha; }
        public DoubleProperty montoProperty()      { return monto; }
        public StringProperty estadoProperty()     { return estado; }
        public String         getIdVenta()         { return idVenta.get(); }
    }

    // ─── Punto de entrada ─────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            controller = new VentaController();
        } catch (IOException e) {
            mostrarError("Error al iniciar el sistema de archivos: " + e.getMessage());
            return;
        }

        primaryStage.setTitle("Vendia — LogiMarket Perú S.A.");
        primaryStage.setScene(construirEscena());
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(560);
        primaryStage.show();

        refrescarTabla();
    }

    // ─── Construcción de la escena ────────────────────────────────────────

    private Scene construirEscena() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_APP + ";");

        root.setTop(construirHeader());
        root.setCenter(construirContenidoPrincipal());
        root.setBottom(construirStatusBar());

        Scene scene = new Scene(root, 1020, 700);
        scene.getStylesheets().add(getClass().getResource("/vendia.css").toExternalForm());
        return scene;
    }

    private HBox construirHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(72);
        header.setStyle(
            "-fx-background-color: " + BG_SURFACE + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 0 0 1 0;"
        );

        // Franja de acento izquierda — fillHeight=true en HBox la estira automáticamente
        Region accentBar = new Region();
        accentBar.setPrefWidth(5);
        accentBar.setStyle("-fx-background-color: " + BRAND + ";");

        VBox textos = new VBox(3);
        textos.setPadding(new Insets(14, 0, 14, 20));
        HBox.setHgrow(textos, Priority.ALWAYS);

        Label titulo = new Label("Vendia");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        titulo.setTextFill(Color.web(BRAND));

        Label subtitulo = new Label("Sistema de Punto de Venta — LogiMarket Perú S.A.");
        subtitulo.setFont(Font.font("Segoe UI", 12));
        subtitulo.setTextFill(Color.web(TEXT_SEC));

        textos.getChildren().addAll(titulo, subtitulo);

        lblContador = new Label("0 registros");
        lblContador.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lblContador.setTextFill(Color.web("#059669"));
        lblContador.setStyle(
            "-fx-background-color: #d1fae5;" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 12;"
        );

        HBox contadorBox = new HBox(lblContador);
        contadorBox.setPadding(new Insets(0, 20, 0, 0));
        contadorBox.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(accentBar, textos, contadorBox);
        return header;
    }

    private SplitPane construirContenidoPrincipal() {
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: " + BG_APP + ";");
        split.setOrientation(Orientation.HORIZONTAL);

        split.getItems().addAll(construirPanelOperaciones(), construirPanelTabla());
        split.setDividerPositions(0.36);
        return split;
    }

    // ─── Panel izquierdo: formularios CRUD ───────────────────────────────

    private ScrollPane construirPanelOperaciones() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: " + BG_APP + ";");

        panel.getChildren().addAll(
                seccionRegistrar(),
                seccionBuscar(),
                seccionModificar(),
                seccionEliminar(),
                seccionActualizar()
        );

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + BG_APP + "; -fx-background: " + BG_APP + ";");
        return scroll;
    }

    private VBox seccionRegistrar() {
        VBox card = crearCard();

        txtIdVendedor = crearTextField("Ej: VEN-001");
        txtMonto      = crearTextField("Ej: 125.50");
        txtMonto.setOnAction(e -> accionRegistrar());

        Button btnRegistrar = crearBoton("Registrar Venta", "#818cf8", "#ffffff");
        btnRegistrar.setOnAction(e -> accionRegistrar());

        card.getChildren().addAll(
                tituloSeccion("Nueva Venta"),
                etiqueta("ID Vendedor"), txtIdVendedor,
                etiqueta("Monto (S/.)"), txtMonto,
                btnRegistrar
        );
        return card;
    }

    private VBox seccionBuscar() {
        VBox card = crearCard();

        txtBuscarId = crearTextField("ID de venta");
        txtBuscarId.setOnAction(e -> accionBuscar());

        Button btnBuscar = crearBoton("Buscar", "#e2e8f0", "#475569");
        btnBuscar.setOnAction(e -> accionBuscar());

        lblResultadoBusqueda = new Label();
        lblResultadoBusqueda.setFont(Font.font("Segoe UI", 12));
        lblResultadoBusqueda.setWrapText(true);
        lblResultadoBusqueda.setMaxWidth(Double.MAX_VALUE);
        lblResultadoBusqueda.setVisible(false);
        lblResultadoBusqueda.setManaged(false);

        card.getChildren().addAll(
                tituloSeccion("Buscar Venta"),
                etiqueta("ID Venta"), txtBuscarId,
                btnBuscar,
                lblResultadoBusqueda
        );
        return card;
    }

    private VBox seccionModificar() {
        VBox card = crearCard();

        Label aviso = new Label("Seleccione un registro en la tabla.");
        aviso.setFont(Font.font("Segoe UI", 11));
        aviso.setTextFill(Color.web(TEXT_MUT));

        txtNuevoMonto = crearTextField("Nuevo monto (S/.)");

        Button btnModificar = crearBoton("Guardar Cambio", "#fde68a", "#92400e");
        btnModificar.setOnAction(e -> accionModificar());

        card.getChildren().addAll(
                tituloSeccion("Modificar Monto"),
                aviso,
                etiqueta("Nuevo Monto (S/.)"), txtNuevoMonto,
                btnModificar
        );
        return card;
    }

    private VBox seccionEliminar() {
        VBox card = crearCard();

        Label aviso = new Label("Seleccione un registro en la tabla.");
        aviso.setFont(Font.font("Segoe UI", 11));
        aviso.setTextFill(Color.web(TEXT_MUT));

        Button btnEliminar = crearBoton("Eliminar Seleccionado", "#fca5a5", "#7f1d1d");
        btnEliminar.setOnAction(e -> accionEliminar());

        card.getChildren().addAll(tituloSeccion("Eliminar Venta"), aviso, btnEliminar);
        return card;
    }

    private VBox seccionActualizar() {
        VBox card = crearCard();

        Button btnRefrescar = crearBoton("Actualizar tabla", "#a7f3d0", "#065f46");
        btnRefrescar.setOnAction(e -> refrescarTabla());

        card.getChildren().addAll(tituloSeccion("Tabla"), btnRefrescar);
        return card;
    }

    // ─── Panel derecho: tabla de ventas ──────────────────────────────────

    private VBox construirPanelTabla() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: " + BG_APP + ";");

        Label titulo = new Label("Registros locales — ventas.dat");
        titulo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        titulo.setTextFill(Color.web(TEXT_PRI));

        tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: " + BG_SURFACE + ";");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().addAll(
                columna("ID Venta",   "idVenta",    200),
                columna("Vendedor",   "idVendedor",  90),
                columna("Fecha",      "fecha",      140),
                columnaDouble("Monto","monto",      110),
                columnaEstado("Estado","estado",    110)
        );

        datosTabla = FXCollections.observableArrayList();
        tabla.setItems(datosTabla);

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(VentaFX item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.estadoProperty().get().contains("Enviado")) {
                    setStyle("-fx-background-color: #f0fdf4;");
                } else if (item.estadoProperty().get().contains("Eliminado")) {
                    setStyle("-fx-background-color: #fef2f2; -fx-opacity: 0.8;");
                } else {
                    setStyle("");
                }
            }
        });

        panel.getChildren().addAll(titulo, tabla);
        return panel;
    }

    // ─── Status bar inferior ──────────────────────────────────────────────

    private HBox construirStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: " + BG_MUTED + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1 0 0 0;"
        );

        lblStatus = new Label("Sistema iniciado. Listo para operar.");
        lblStatus.setFont(Font.font("Segoe UI", 12));
        lblStatus.setTextFill(Color.web(TEXT_SEC));

        bar.getChildren().add(lblStatus);
        return bar;
    }

    // ─── Acciones CRUD ────────────────────────────────────────────────────

    private void accionRegistrar() {
        String vendedor = txtIdVendedor.getText();
        String montoStr = txtMonto.getText();

        if (vendedor.isBlank() || montoStr.isBlank()) {
            setStatus("Complete todos los campos.", "#d97706");
            return;
        }

        try {
            double monto = Double.parseDouble(montoStr.replace(",", "."));
            Venta nueva = controller.registrarVenta(vendedor, monto);
            setStatus("Venta registrada: " + nueva.getIdVenta(), "#059669");
            txtIdVendedor.clear();
            txtMonto.clear();
            refrescarTabla();
        } catch (NumberFormatException e) {
            setStatus("El monto debe ser un número válido.", "#d97706");
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage(), "#dc2626");
        }
    }

    private void accionBuscar() {
        String id = txtBuscarId.getText().trim();
        if (id.isBlank()) { setStatus("Ingrese un ID para buscar.", "#d97706"); return; }

        try {
            Venta v = controller.buscar(id);
            if (v == null) {
                mostrarResultadoBusqueda(
                    "No se encontró: " + id,
                    "#fef3c7", "#92400e"
                );
                setStatus("Venta no encontrada.", "#d97706");
            } else {
                String estado = switch (v.getEstado()) {
                    case 'P' -> "Pendiente";
                    case 'E' -> "Enviado";
                    case 'X' -> "Eliminado";
                    default  -> String.valueOf(v.getEstado());
                };
                mostrarResultadoBusqueda(
                    String.format("Vendedor: %s\nMonto: S/. %.2f\nEstado: %s",
                        v.getIdVendedor(), v.getMontoTotal(), estado),
                    "#d1fae5", "#065f46"
                );
                setStatus("Venta encontrada.", "#059669");
                datosTabla.stream()
                        .filter(vfx -> vfx.getIdVenta().equals(v.getIdVenta()))
                        .findFirst()
                        .ifPresent(vfx -> {
                            tabla.getSelectionModel().select(vfx);
                            tabla.scrollTo(vfx);
                        });
            }
        } catch (Exception e) {
            mostrarResultadoBusqueda("Error: " + e.getMessage(), "#fee2e2", "#991b1b");
            setStatus("Error en la búsqueda.", "#dc2626");
        }
    }

    private void mostrarResultadoBusqueda(String texto, String bg, String fg) {
        lblResultadoBusqueda.setText(texto);
        lblResultadoBusqueda.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-padding: 8 10;" +
            "-fx-background-radius: 6;"
        );
        lblResultadoBusqueda.setVisible(true);
        lblResultadoBusqueda.setManaged(true);
    }

    private void accionModificar() {
        VentaFX seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) { setStatus("Seleccione una venta en la tabla.", "#d97706"); return; }

        String nuevoMontoStr = txtNuevoMonto.getText();
        if (nuevoMontoStr.isBlank()) { setStatus("Ingrese el nuevo monto.", "#d97706"); return; }

        try {
            double nuevoMonto = Double.parseDouble(nuevoMontoStr.replace(",", "."));
            controller.modificarMonto(seleccionada.getIdVenta(), nuevoMonto);
            setStatus("Monto actualizado para: " + seleccionada.getIdVenta(), "#059669");
            txtNuevoMonto.clear();
            refrescarTabla();
        } catch (NumberFormatException e) {
            setStatus("Ingrese un número válido.", "#d97706");
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage(), "#dc2626");
        }
    }

    private void accionEliminar() {
        VentaFX seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) { setStatus("Seleccione una venta en la tabla.", "#d97706"); return; }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar la venta " + seleccionada.getIdVenta() + "?");
        confirmacion.setContentText("El registro quedará marcado como eliminado.");

        confirmacion.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    controller.eliminar(seleccionada.getIdVenta());
                    setStatus("Venta eliminada: " + seleccionada.getIdVenta(), "#059669");
                    refrescarTabla();
                } catch (Exception e) {
                    setStatus("Error: " + e.getMessage(), "#dc2626");
                }
            }
        });
    }

    // ─── Utilidades UI ────────────────────────────────────────────────────

    private void refrescarTabla() {
        try {
            List<Venta> ventas = controller.listarTodas();
            datosTabla.clear();
            ventas.stream()
                  .filter(v -> v.getEstado() != 'X')
                  .map(VentaFX::new)
                  .forEach(datosTabla::add);
            int total      = controller.contarRegistros();
            int pendientes = controller.listarPendientes().size();
            lblContador.setText(total + " registros  |  " + pendientes + " pendientes");
        } catch (IOException e) {
            setStatus("Error al cargar registros: " + e.getMessage(), "#dc2626");
        }
    }

    private void setStatus(String msg, String color) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(Color.web(color));
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    // ─── Helpers de construcción de componentes ───────────────────────────

    private VBox crearCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: " + BG_SURFACE + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );
        return card;
    }

    private Label tituloSeccion(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lbl.setTextFill(Color.web(BRAND));
        return lbl;
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web(TEXT_SEC));
        return lbl;
    }

    private TextField crearTextField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle(
            "-fx-background-color: " + BG_SURFACE + ";" +
            "-fx-text-fill: " + TEXT_PRI + ";" +
            "-fx-prompt-text-fill: " + TEXT_MUT + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 13;" +
            "-fx-padding: 7 10;"
        );
        return tf;
    }

    private Button crearBoton(String texto, String bgColor, String textColor) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btn.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: " + textColor + ";" +
            "-fx-cursor: hand;" +
            "-fx-padding: 8 14;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.82));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<VentaFX, String> columna(String titulo, String propiedad, int ancho) {
        TableColumn<VentaFX, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        col.setPrefWidth(ancho);
        return col;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<VentaFX, Double> columnaDouble(String titulo, String propiedad, int ancho) {
        TableColumn<VentaFX, Double> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        col.setPrefWidth(ancho);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("S/. %.2f", item));
            }
        });
        return col;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<VentaFX, String> columnaEstado(String titulo, String propiedad, int ancho) {
        TableColumn<VentaFX, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        col.setPrefWidth(ancho);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String bg, fg;
                if (item.contains("Enviado")) {
                    bg = "#d1fae5"; fg = "#065f46";
                } else if (item.contains("Eliminado")) {
                    bg = "#fee2e2"; fg = "#991b1b";
                } else {
                    bg = "#e0e7ff"; fg = "#3730a3";
                }
                setStyle(
                    "-fx-background-color: " + bg + ";" +
                    "-fx-text-fill: " + fg + ";" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 2 8;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-font-size: 11;" +
                    "-fx-font-weight: bold;" +
                    "-fx-alignment: CENTER;"
                );
            }
        });
        return col;
    }
}
