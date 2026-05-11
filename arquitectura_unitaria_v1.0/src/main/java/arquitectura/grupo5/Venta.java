package arquitectura.grupo5;

public class Venta {

    // Tamanio fijo de cada campo en bytes (para RandomAccessFile)
    // Esto permite calcular la posicion exacta de cualquier registro
    public static final int TAM_ID        = 4;   // int
    public static final int TAM_REGION    = 2;   // int (codigo departamento: 01-25)
    public static final int TAM_PRODUCTO  = 60;  // 30 chars x 2 bytes (UTF)
    public static final int TAM_MONTO     = 8;   // double
    public static final int TAM_FECHA     = 24;  // 12 chars x 2 bytes (UTF)

    // Tamanio total del registro en bytes
    public static final int TAM_REGISTRO  = TAM_ID + TAM_REGION + TAM_PRODUCTO
            + TAM_MONTO + TAM_FECHA;
    // = 4 + 2 + 60 + 8 + 20 = 94 bytes por registro

    // Atributos
    private int    id;
    private int    codigoRegion;   // 15=Lima, 02=Ancash, 03=Arequipa, etc.
    private String producto;
    private double monto;
    private String fecha;          // formato: dd/mm/aaaa

    // Constructor completo
    public Venta(int id, int codigoRegion, String producto, double monto, String fecha) {
        this.id           = id;
        this.codigoRegion = codigoRegion;
        this.producto     = producto;
        this.monto        = monto;
        this.fecha        = fecha;
    }

    // Constructor vacio (para leer del archivo)
    public Venta() {}

    // Getters y setters
    public int    getId()            { return id; }
    public int    getCodigoRegion()  { return codigoRegion; }
    public String getProducto()      { return producto; }
    public double getMonto()         { return monto; }
    public String getFecha()         { return fecha; }

    public void setId(int id)                    { this.id = id; }
    public void setCodigoRegion(int c)           { this.codigoRegion = c; }
    public void setProducto(String p)            { this.producto = p; }
    public void setMonto(double m)               { this.monto = m; }
    public void setFecha(String f)               { this.fecha = f; }

    // Nombre legible de la region segun codigo INEI
    public String getNombreRegion() {
        switch (codigoRegion) {
            case  1: return "Lima";
            case  2: return "Ancash";
            case  3: return "Arequipa";
            case  4: return "Ayacucho";
            case  5: return "Cajamarca";
            case  6: return "Cusco";
            case  7: return "Huancavelica";
            case  8: return "Junin";
            case  9: return "La Libertad";
            case 10: return "Lambayeque";
            case 11: return "Loreto";
            case 12: return "Moquegua";
            case 13: return "Puno";
            case 14: return "San Martin";
            case 15: return "Tacna";
            default: return "Region " + codigoRegion;
        }
    }

    @Override
    public String toString() {
        return String.format("[%04d] %-10s | %-28s | S/. %10.2f | %s",
                id, getNombreRegion(), producto, monto, fecha);
    }
}