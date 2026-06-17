package arqui.grupo5.model;

/**
 * Representa un registro de venta con tamaño fijo para almacenamiento binario.

 * Total: 40 + 40 + 40 + 8 + 2 = 130 bytes por registro
 * Estado: 'P' = Pendiente (no enviado), 'E' = Enviado al servidor
 */
public class Venta {

    // Tamaños fijos de campos de texto (en chars, cada char = 2 bytes en Java)
    public static final int TAM_ID_VENTA    = 20;
    public static final int TAM_ID_VENDEDOR = 20;
    public static final int TAM_FECHA       = 20;
    public static final int TAM_ID_PRODUCTO = 20;

    public static final int TAM_REGISTRO = (TAM_ID_VENTA + TAM_ID_VENDEDOR + TAM_FECHA + TAM_ID_PRODUCTO) * 2 + 8 + 2;

    private String idVenta;      // Identificador único de la venta
    private String idVendedor;   // ID del cajero/vendedor
    private String idProducto;   // ID del producto vendido
    private String fecha;        // Fecha en formato "yyyy-MM-dd HH:mm"
    private double montoTotal;   // Monto en soles
    private char estado;         // 'P' pendiente, 'E' enviado

    public Venta() {}

    public Venta(String idVenta, String idVendedor, String idProducto, String fecha, double montoTotal) {
        this.idVenta     = idVenta;
        this.idVendedor  = idVendedor;
        this.idProducto  = idProducto;
        this.fecha       = fecha;
        this.montoTotal  = montoTotal;
        this.estado      = 'P'; // toda venta nueva empieza como Pendiente
    }

    // ─── Getters y Setters ───────────────────────────────────────────────────

    public String getIdVenta()               { return idVenta; }
    public void   setIdVenta(String v)       { this.idVenta = v; }

    public String getIdVendedor()            { return idVendedor; }
    public void   setIdVendedor(String v)    { this.idVendedor = v; }

    public String getIdProducto()            { return idProducto; }
    public void   setIdProducto(String v)    { this.idProducto = v; }

    public String getFecha()                 { return fecha; }
    public void   setFecha(String v)         { this.fecha = v; }

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
        return String.format("Venta{id='%s', vendedor='%s', producto='%s', fecha='%s', monto=%.2f, estado='%c'}",
                idVenta, idVendedor, idProducto, fecha, montoTotal, estado);
    }
}
