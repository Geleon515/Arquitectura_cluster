package arqui.grupo5;

import arqui.grupo5.plugin.VendiaPlugin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditoriaPlugin implements VendiaPlugin {

    private static final String ARCHIVO_LOG = "auditoria.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getNombre() {
        return "Auditoria";
    }

    @Override
    public void ejecutar(String idVenta, String idVendedor, String idProducto, String fecha, double monto) {
        String timestamp = LocalDateTime.now().format(FMT);
        String linea = String.format("[%s] VENTA REGISTRADA | id=%s | vendedor=%s | producto=%s | fecha=%s | monto=S/.%.2f",
            timestamp, idVenta, idVendedor, idProducto, fecha, monto);

        System.out.println("[PLUGIN Auditoria] " + linea);

        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_LOG, true))) {
            pw.println(linea);
        } catch (IOException e) {
            System.err.println("[PLUGIN Auditoria] ERROR al escribir log: " + e.getMessage());
        }
    }
}
