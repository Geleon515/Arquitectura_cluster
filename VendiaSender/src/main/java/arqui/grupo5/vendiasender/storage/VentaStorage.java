package arqui.grupo5.vendiasender.storage;

import arqui.grupo5.vendiasender.model.Venta;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VentaStorage {
    private final File datFile;

    public VentaStorage(String rutaDat) {
        this.datFile = new File(rutaDat);
    }

    public boolean existe() {
        return datFile.exists();
    }

    /** Lee todos los registros con estado='P' del archivo ventas.dat */
    public List<Venta> leerPendientes() throws IOException {
        List<Venta> pendientes = new ArrayList<>();
        if (!datFile.exists()) return pendientes;

        try (RandomAccessFile raf = new RandomAccessFile(datFile, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Venta v = leerRegistro(raf);
                if (v.getEstado() == 'P') pendientes.add(v);
            }
        }
        return pendientes;
    }

    /** Marca las ventas de la lista como estado='E' (enviado) en ventas.dat */
    public void marcarEnviadas(List<Venta> enviadas) throws IOException {
        Set<String> ids = new HashSet<>();
        for (Venta v : enviadas) ids.add(v.getIdVenta());

        try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
            long pos = 0;
            while (pos < raf.length()) {
                raf.seek(pos);
                long inicio = pos;
                Venta v = leerRegistro(raf);
                if (ids.contains(v.getIdVenta())) {
                    raf.seek(inicio);
                    v.setEstado('E');
                    escribirRegistro(raf, v);
                }
                pos += Venta.RECORD_SIZE;
            }
        }
    }

    private Venta leerRegistro(RandomAccessFile raf) throws IOException {
        String idVenta    = leerString(raf, Venta.ID_LEN);
        String idVendedor = leerString(raf, Venta.VENDEDOR_LEN);
        String fecha      = leerString(raf, Venta.FECHA_LEN);
        String region     = leerString(raf, Venta.REGION_LEN);
        double monto      = raf.readDouble();
        char   estado     = raf.readChar();
        return new Venta(idVenta, idVendedor, fecha, region, monto, estado);
    }

    private void escribirRegistro(RandomAccessFile raf, Venta v) throws IOException {
        writeChars(raf, v.getIdVenta(),    Venta.ID_LEN);
        writeChars(raf, v.getIdVendedor(), Venta.VENDEDOR_LEN);
        writeChars(raf, v.getFecha(),      Venta.FECHA_LEN);
        writeChars(raf, v.getRegion(),     Venta.REGION_LEN);
        raf.writeDouble(v.getMontoTotal());
        raf.writeChar(v.getEstado());
    }

    private String leerString(RandomAccessFile raf, int len) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(raf.readChar());
        return sb.toString().trim();
    }

    private void writeChars(RandomAccessFile raf, String s, int len) throws IOException {
        String padded = s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
        for (char c : padded.toCharArray()) raf.writeChar(c);
    }
}
