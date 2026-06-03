package arqui.grupo5.vendiaUpdater.model;

public class Venta implements java.io.Serializable {
    private static final long serialVersionUID = 2L;

    public static final int RECORD_SIZE  = 134;
    public static final int ID_LEN       = 20;
    public static final int VENDEDOR_LEN = 20;
    public static final int FECHA_LEN    = 20;
    public static final int REGION_LEN   = 2;

    private final String idVenta;
    private final String idVendedor;
    private final String fecha;
    private final String region;
    private final double montoTotal;
    private final char   estado;

    public Venta(String idVenta, String idVendedor, String fecha, String region, double montoTotal, char estado) {
        this.idVenta    = idVenta;
        this.idVendedor = idVendedor;
        this.fecha      = fecha;
        this.region     = region;
        this.montoTotal = montoTotal;
        this.estado     = estado;
    }

    public String getIdVenta()    { return idVenta; }
    public String getIdVendedor() { return idVendedor; }
    public String getFecha()      { return fecha; }
    public String getRegion()     { return region; }
    public double getMontoTotal() { return montoTotal; }
    public char   getEstado()     { return estado; }

    @Override
    public String toString() {
        return String.format("[%s] vendedor=%-20s fecha=%-20s region=%-2s monto=%.2f estado=%c",
            idVenta, idVendedor, fecha, region, montoTotal, estado);
    }
}
