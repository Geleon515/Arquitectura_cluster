package arquitectura.grupo5;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArchivoVentas {

    private String nombreArchivo;

    public ArchivoVentas(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    // -------------------------------------------------------
    // Calcula la posicion en bytes del registro numero 'indice'
    // Acceso aleatorio: salto directo sin leer registros anteriores
    // -------------------------------------------------------
    private long calcularPosicion(int indice) {
        return (long) indice * Venta.TAM_REGISTRO;
    }

    // -------------------------------------------------------
    // Escribe una venta en la posicion del indice dado
    // -------------------------------------------------------
    public void escribirVenta(Venta v, int indice) {
        try (RandomAccessFile raf = new RandomAccessFile(nombreArchivo, "rw")) {
            raf.seek(calcularPosicion(indice));
            raf.writeInt(v.getId());
            raf.writeInt(v.getCodigoRegion());
            escribirString(raf, v.getProducto(), 30);
            raf.writeDouble(v.getMonto());
            escribirString(raf, v.getFecha(), 12);
        } catch (IOException e) {
            System.out.println("Error escribiendo venta: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Lee una venta desde la posicion del indice dado
    // -------------------------------------------------------
    public Venta leerVenta(int indice) {
        Venta v = null;
        try (RandomAccessFile raf = new RandomAccessFile(nombreArchivo, "r")) {
            raf.seek(calcularPosicion(indice));
            int    id           = raf.readInt();
            int    codigoRegion = raf.readInt();
            String producto     = leerString(raf, 30);
            double monto        = raf.readDouble();
            String fecha        = leerString(raf, 12);
            v = new Venta(id, codigoRegion, producto, monto, fecha);
        } catch (IOException e) {
            System.out.println("Error leyendo venta en indice " + indice + ": " + e.getMessage());
        }
        return v;
    }

    // -------------------------------------------------------
    // Agrega una venta nueva al final del archivo
    // Retorna el indice donde fue guardada
    // -------------------------------------------------------
    public int agregarVenta(Venta v) {
        int indice = contarRegistros();
        v.setId(indice + 1);
        escribirVenta(v, indice);
        return indice;
    }

    // -------------------------------------------------------
    // Cuenta cuantos registros hay en el archivo
    // -------------------------------------------------------
    public int contarRegistros() {
        try (RandomAccessFile raf = new RandomAccessFile(nombreArchivo, "r")) {
            return (int) (raf.length() / Venta.TAM_REGISTRO);
        } catch (IOException e) {
            return 0; // Si el archivo no existe aun, hay 0 registros
        }
    }

    // -------------------------------------------------------
    // Lista todas las ventas del archivo
    // -------------------------------------------------------
    public List<Venta> listarTodas() {
        List<Venta> lista = new ArrayList<>();
        int total = contarRegistros();
        for (int i = 0; i < total; i++) {
            Venta v = leerVenta(i);
            if (v != null) lista.add(v);
        }
        return lista;
    }

    // -------------------------------------------------------
    // Lista ventas filtrando por codigo de region
    // Aqui es donde cada nodo del cluster filtrara su parte
    // -------------------------------------------------------
    public List<Venta> listarPorRegion(int codigoRegion) {
        List<Venta> lista = new ArrayList<>();
        int total = contarRegistros();
        for (int i = 0; i < total; i++) {
            Venta v = leerVenta(i);
            if (v != null && v.getCodigoRegion() == codigoRegion) {
                lista.add(v);
            }
        }
        return lista;
    }

    // -------------------------------------------------------
    // Calcula el total de ventas de una region
    // -------------------------------------------------------
    public double totalPorRegion(int codigoRegion) {
        double total = 0;
        for (Venta v : listarPorRegion(codigoRegion)) {
            total += v.getMonto();
        }
        return total;
    }

    // -------------------------------------------------------
    // Busca una venta por su ID (acceso secuencial)
    // -------------------------------------------------------
    public Venta buscarPorId(int id) {
        int total = contarRegistros();
        for (int i = 0; i < total; i++) {
            Venta v = leerVenta(i);
            if (v != null && v.getId() == id) return v;
        }
        return null;
    }

    // -------------------------------------------------------
    // Helpers: escribe/lee Strings de longitud fija en UTF
    // Rellena con espacios si el texto es mas corto
    // -------------------------------------------------------
    private void escribirString(RandomAccessFile raf, String texto, int maxChars)
            throws IOException {
        if (texto == null) texto = "";
        if (texto.length() > maxChars)
            texto = texto.substring(0, maxChars);
        raf.writeChars(texto);
        // Rellenar con espacios hasta completar maxChars
        for (int i = texto.length(); i < maxChars; i++) {
            raf.writeChar(' ');
        }
    }

    private String leerString(RandomAccessFile raf, int maxChars)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxChars; i++) {
            sb.append(raf.readChar());
        }
        return sb.toString().trim();
    }
}