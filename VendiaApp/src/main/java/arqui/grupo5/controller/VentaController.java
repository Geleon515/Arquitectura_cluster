package arqui.grupo5.controller;

import arqui.grupo5.model.Venta;
import arqui.grupo5.storage.VentaStorage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contiene la lógica de negocio y valida los datos antes de
 * pasarlos al Storage. La UI solo habla con este controller.
 */
public class VentaController {

    private final VentaStorage storage;
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public VentaController() throws IOException {
        this.storage = new VentaStorage();
    }

    public String generarIdVenta() {
        return "VTA-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public String getFechaActual() {
        return LocalDateTime.now().format(FORMATO_FECHA);
    }

    public Venta registrarVenta(String idVendedor, double monto) throws IOException {
        // Validaciones de negocio
        if (idVendedor == null || idVendedor.isBlank()) {
            throw new IllegalArgumentException("El ID del vendedor no puede estar vacío.");
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (monto > 999999.99) {
            throw new IllegalArgumentException("El monto excede el límite permitido.");
        }

        Venta venta = new Venta(
                generarIdVenta(),
                idVendedor.trim().toUpperCase(),
                getFechaActual(),
                monto
        );

        storage.insertar(venta);
        return venta;
    }

    public Venta buscar(String idVenta) throws IOException {
        if (idVenta == null || idVenta.isBlank()) return null;
        return storage.buscarPorId(idVenta.trim().toUpperCase());
    }

    public List<Venta> listarTodas() throws IOException {
        return storage.listarTodas();
    }

    public List<Venta> listarPendientes() throws IOException {
        return storage.listarPendientes();
    }

    public boolean modificarMonto(String idVenta, double nuevoMonto) throws IOException {
        if (nuevoMonto <= 0) {
            throw new IllegalArgumentException("El nuevo monto debe ser mayor a cero.");
        }

        Venta venta = storage.buscarPorId(idVenta.trim());
        if (venta == null) {
            throw new IllegalArgumentException("No se encontró la venta con ID: " + idVenta);
        }
        if (venta.isEnviado()) {
            throw new IllegalStateException("No se puede modificar una venta ya enviada al servidor.");
        }

        venta.setMontoTotal(nuevoMonto);
        return storage.actualizar(venta);
    }

    public boolean eliminar(String idVenta) throws IOException {
        return storage.eliminar(idVenta.trim());
    }

    public void marcarComoEnviada(String idVenta) throws IOException {
        storage.marcarComoEnviada(idVenta);
    }

    public int contarRegistros() {
        return storage.contarRegistros();
    }
}
