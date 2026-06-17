package arqui.grupo5.viewcrosstab.model;

public class PivotRow3D {
    private final String idVendedor, idProducto;
    private final double q1, q2, q3, q4;

    public PivotRow3D(String idVendedor, String idProducto,
                      double q1, double q2, double q3, double q4) {
        this.idVendedor = idVendedor;
        this.idProducto = idProducto;
        this.q1 = q1; this.q2 = q2; this.q3 = q3; this.q4 = q4;
    }

    public String getIdVendedor() { return idVendedor; }
    public String getIdProducto() { return idProducto; }
    public double getQ1()  { return q1; }
    public double getQ2()  { return q2; }
    public double getQ3()  { return q3; }
    public double getQ4()  { return q4; }
    public double getTotal() { return q1 + q2 + q3 + q4; }
}
