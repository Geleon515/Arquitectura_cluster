package arqui.grupo5.plugin;

public interface VendiaPlugin {
    String getNombre();
    void ejecutar(String idVenta, String idVendedor, String idProducto, String fecha, double monto);
}
