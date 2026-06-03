package arqui.grupo5.model;

import java.io.Serializable;

/**
 * Representa un registro de venta con tamaño fijo para almacenamiento binario.
 *
 * Estructura del registro en ventas.dat (tamaño fijo = 134 bytes):
 * ┌─────────────┬──────────────┬─────────────┬──────────┬────────────┬──────────┐
 * │ id_venta    │ id_vendedor  │ fecha       │ region   │ monto      │ estado   │
 * │ 20 chars    │ 20 chars     │ 20 chars    │ 2 chars  │ 8 bytes    │ 1 char   │
 * │ (40 bytes)  │ (40 bytes)   │ (40 bytes)  │ (4 bytes)│ (double)   │ (2 bytes)│
 * └─────────────┴──────────────┴─────────────┴──────────┴────────────┴──────────┘
 * Total: 40 + 40 + 40 + 4 + 8 + 2 = 134 bytes por registro
 *
 * Estado: 'P' = Pendiente (no enviado), 'E' = Enviado al servidor
 */
public class Venta implements Serializable {

    private static final long serialVersionUID = 2L;

    // Tamaños fijos de campos de texto (en chars, cada char = 2 bytes en Java)
    public static final int TAM_ID_VENTA    = 20;
    public static final int TAM_ID_VENDEDOR = 20;
    public static final int TAM_FECHA       = 20;
    public static final int TAM_REGION      = 2;

    // Tamaño total del registro en bytes:
    // (20+20+20+2) chars * 2 bytes/char + 8 bytes (double) + 2 bytes (char estado)
    public static final int TAM_REGISTRO = (TAM_ID_VENTA + TAM_ID_VENDEDOR + TAM_FECHA + TAM_REGION) * 2 + 8 + 2;

    private String idVenta;      // Identificador único de la venta
    private String idVendedor;   // ID del cajero/vendedor
    private String fecha;        // Fecha en formato "yyyy-MM-dd HH:mm"
    private String region;       // Código INEI de 2 caracteres ("01"-"25")
    private double montoTotal;   // Monto en soles
    private char estado;         // 'P' pendiente, 'E' enviado

    public Venta() {}

    public Venta(String idVenta, String idVendedor, String fecha, double montoTotal) {
        this(idVenta, idVendedor, fecha, "15", montoTotal);
    }

    public Venta(String idVenta, String idVendedor, String fecha, String region, double montoTotal) {
        this.idVenta     = idVenta;
        this.idVendedor  = idVendedor;
        this.fecha       = fecha;
        this.region      = region;
        this.montoTotal  = montoTotal;
        this.estado      = 'P'; // toda venta nueva empieza como Pendiente
    }

    // ─── Getters y Setters ───────────────────────────────────────────────────

    public String getIdVenta()               { return idVenta; }
    public void   setIdVenta(String v)       { this.idVenta = v; }

    public String getIdVendedor()            { return idVendedor; }
    public void   setIdVendedor(String v)    { this.idVendedor = v; }

    public String getFecha()                 { return fecha; }
    public void   setFecha(String v)         { this.fecha = v; }

    public String getRegion()                { return region; }
    public void   setRegion(String v)        { this.region = v; }

    public double getMontoTotal()            { return montoTotal; }
    public void   setMontoTotal(double v)    { this.montoTotal = v; }

    public char   getEstado()                { return estado; }
    public void   setEstado(char v)          { this.estado = v; }

    public boolean isPendiente()             { return estado == 'P'; }
    public boolean isEnviado()               { return estado == 'E'; }

    /**
     * Retorna una cadena de longitud exacta 'len', rellenada con espacios
     * o truncada si es más larga. Necesario para escritura de tamaño fijo.
     */
    public static String padOrTruncate(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    @Override
    public String toString() {
        return String.format("Venta{id='%s', vendedor='%s', fecha='%s', region='%s', monto=%.2f, estado='%c'}",
                idVenta, idVendedor, fecha, region, montoTotal, estado);
    }
}
