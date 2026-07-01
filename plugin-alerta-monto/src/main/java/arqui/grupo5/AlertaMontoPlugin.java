package arqui.grupo5;

import arqui.grupo5.plugin.VendiaPlugin;

public class AlertaMontoPlugin implements VendiaPlugin {

    private static final double UMBRAL = 1000.0;

    @Override
    public String getNombre() {
        return "Alerta de Monto";
    }

    @Override
    public void ejecutar(String idVenta, String idVendedor, String idProducto, String fecha, double monto) {
        if (monto > UMBRAL) {
            System.out.println("============================================");
            System.out.println("[PLUGIN AlertaMonto] *** VENTA DE ALTO VALOR ***");
            System.out.println("[PLUGIN AlertaMonto] ID      : " + idVenta);
            System.out.println("[PLUGIN AlertaMonto] Vendedor: " + idVendedor);
            System.out.println("[PLUGIN AlertaMonto] Producto: " + idProducto);
            System.out.println("[PLUGIN AlertaMonto] Monto   : S/. " + String.format("%.2f", monto));
            System.out.println("[PLUGIN AlertaMonto] Supera el umbral de S/. " + String.format("%.2f", UMBRAL));
            System.out.println("============================================");
        }
    }
}
