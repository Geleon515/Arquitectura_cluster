package arqui.grupo5.dw.model;

public class VentaOperacional {

    private final String idVenta;
    private final String idVendedor;
    private final String fecha;
    private final double montoTotal;

    public VentaOperacional(String idVenta, String idVendedor, String fecha, double montoTotal) {
        this.idVenta    = idVenta;
        this.idVendedor = idVendedor;
        this.fecha      = fecha;
        this.montoTotal = montoTotal;
    }

    public String getIdVenta()    { return idVenta;    }
    public String getIdVendedor() { return idVendedor; }
    public String getFecha()      { return fecha;      }
    public double getMontoTotal() { return montoTotal; }
}