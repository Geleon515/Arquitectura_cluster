package arqui.grupo5.vendiasender.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Venta implements java.io.Serializable {
    private static final long serialVersionUID = 2L;

    public static final int RECORD_SIZE  = 134;
    public static final int ID_LEN       = 20;
    public static final int VENDEDOR_LEN = 20;
    public static final int FECHA_LEN    = 20;
    public static final int REGION_LEN   = 2;

    private final StringProperty idVenta;
    private final StringProperty idVendedor;
    private final StringProperty fecha;
    private final StringProperty region;
    private final DoubleProperty montoTotal;
    private char estado;

    public Venta(String idVenta, String idVendedor, String fecha, String region, double montoTotal, char estado) {
        this.idVenta    = new SimpleStringProperty(idVenta);
        this.idVendedor = new SimpleStringProperty(idVendedor);
        this.fecha      = new SimpleStringProperty(fecha);
        this.region     = new SimpleStringProperty(region);
        this.montoTotal = new SimpleDoubleProperty(montoTotal);
        this.estado     = estado;
    }

    public StringProperty idVentaProperty()    { return idVenta; }
    public StringProperty idVendedorProperty() { return idVendedor; }
    public StringProperty fechaProperty()      { return fecha; }
    public StringProperty regionProperty()     { return region; }
    public DoubleProperty montoTotalProperty() { return montoTotal; }

    public String getIdVenta()    { return idVenta.get(); }
    public String getIdVendedor() { return idVendedor.get(); }
    public String getFecha()      { return fecha.get(); }
    public String getRegion()     { return region.get(); }
    public double getMontoTotal() { return montoTotal.get(); }
    public char   getEstado()     { return estado; }
    public void   setEstado(char estado) { this.estado = estado; }
}
