package arqui.grupo5.controller;

import arqui.grupo5.model.Venta;
import arqui.grupo5.storage.VentaStorage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * VentaController: Capa de control (MVC).
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

    // ─── Generador de ID único ────────────────────────────────────────────

    /**
     * Genera un ID de venta basado en timestamp: VTA-20260516-143022
     */
    public String generarIdVenta() {
        return "VTA-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    /**
     * Retorna la fecha y hora actual formateada para mostrar en la UI.
     */
    public String getFechaActual() {
        return LocalDateTime.now().format(FORMATO_FECHA);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────

    /**
     * Registra una nueva venta.
     * @param idVendedor ID del cajero (no vacío)
     * @param monto      monto total en soles (debe ser > 0)
     * @return la Venta creada
     * @throws IllegalArgumentException si los datos son inválidos
     */
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

    /**
     * Busca una venta por ID.
     * @return Venta encontrada o null
     */
    public Venta buscar(String idVenta) throws IOException {
        if (idVenta == null || idVenta.isBlank()) return null;
        return storage.buscarPorId(idVenta.trim().toUpperCase());
    }

    /**
     * Lista todas las ventas (incluyendo enviadas).
     */
    public List<Venta> listarTodas() throws IOException {
        return storage.listarTodas();
    }

    /**
     * Lista solo ventas pendientes de envío al servidor.
     */
    public List<Venta> listarPendientes() throws IOException {
        return storage.listarPendientes();
    }

    /**
     * Modifica el monto de una venta existente.
     * Solo se permite si el estado es 'P' (pendiente).
     */
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

    /**
     * Elimina lógicamente una venta (estado = 'X').
     * Solo se permite si el estado es 'P'.
     */
    public boolean eliminar(String idVenta) throws IOException {
        return storage.eliminar(idVenta.trim());
    }

    /**
     * Marca una venta como enviada. Llamado por VendiaSender tras ACK exitoso.
     */
    public void marcarComoEnviada(String idVenta) throws IOException {
        storage.marcarComoEnviada(idVenta);
    }

    /**
     * Retorna el conteo de registros en el archivo local.
     */
    public int contarRegistros() {
        return storage.contarRegistros();
    }
}
