package arqui.grupo5.crosstab.model;

import javafx.beans.property.*;

public class CrossTabRow {

    private final StringProperty  idVendedor     = new SimpleStringProperty();
    private final StringProperty  idProducto     = new SimpleStringProperty();
    private final StringProperty  nombreProducto = new SimpleStringProperty();
    private final DoubleProperty  q1             = new SimpleDoubleProperty();
    private final DoubleProperty  q2             = new SimpleDoubleProperty();
    private final DoubleProperty  q3             = new SimpleDoubleProperty();
    private final DoubleProperty  q4             = new SimpleDoubleProperty();
    private final DoubleProperty  total          = new SimpleDoubleProperty();
    private final IntegerProperty cantQ1         = new SimpleIntegerProperty();
    private final IntegerProperty cantQ2         = new SimpleIntegerProperty();
    private final IntegerProperty cantQ3         = new SimpleIntegerProperty();
    private final IntegerProperty cantQ4         = new SimpleIntegerProperty();
    private final IntegerProperty cantTotal      = new SimpleIntegerProperty();

    public CrossTabRow(String idVendedor, String idProducto, String nombreProducto,
                       double q1, double q2, double q3, double q4,
                       int cq1, int cq2, int cq3, int cq4) {
        this.idVendedor.set(idVendedor);
        this.idProducto.set(idProducto);
        this.nombreProducto.set(nombreProducto);
        this.q1.set(q1); this.q2.set(q2); this.q3.set(q3); this.q4.set(q4);
        this.total.set(q1 + q2 + q3 + q4);
        this.cantQ1.set(cq1); this.cantQ2.set(cq2);
        this.cantQ3.set(cq3); this.cantQ4.set(cq4);
        this.cantTotal.set(cq1 + cq2 + cq3 + cq4);
    }

    public StringProperty  idVendedorProperty()     { return idVendedor; }
    public StringProperty  idProductoProperty()     { return idProducto; }
    public StringProperty  nombreProductoProperty() { return nombreProducto; }
    public DoubleProperty  q1Property()             { return q1; }
    public DoubleProperty  q2Property()             { return q2; }
    public DoubleProperty  q3Property()             { return q3; }
    public DoubleProperty  q4Property()             { return q4; }
    public DoubleProperty  totalProperty()          { return total; }
    public IntegerProperty cantQ1Property()         { return cantQ1; }
    public IntegerProperty cantQ2Property()         { return cantQ2; }
    public IntegerProperty cantQ3Property()         { return cantQ3; }
    public IntegerProperty cantQ4Property()         { return cantQ4; }
    public IntegerProperty cantTotalProperty()      { return cantTotal; }

    public String getIdVendedor()     { return idVendedor.get(); }
    public String getIdProducto()     { return idProducto.get(); }
    public String getNombreProducto() { return nombreProducto.get(); }
    public double getQ1()             { return q1.get(); }
    public double getQ2()             { return q2.get(); }
    public double getQ3()             { return q3.get(); }
    public double getQ4()             { return q4.get(); }
    public double getTotal()          { return total.get(); }
    public int    getCantQ1()         { return cantQ1.get(); }
    public int    getCantQ2()         { return cantQ2.get(); }
    public int    getCantQ3()         { return cantQ3.get(); }
    public int    getCantQ4()         { return cantQ4.get(); }
    public int    getCantTotal()      { return cantTotal.get(); }
}
