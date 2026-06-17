package arqui.grupo5.storage;

import arqui.grupo5.model.Venta;

import java.io.*;
import java.util.*;

/**
 * VentaStorage: Gestiona la persistencia local de ventas.
 *
 * Archivos que maneja:
 *   ventas.dat → registros binarios de tamaño fijo (130 bytes c/u)
 *   ventas.idx → índice: mapa de idVenta → posición en bytes en .dat
 *
 * Estructura ventas.idx (también binario, tamaño fijo por entrada):
 *   [idVenta: 20 chars (40 bytes)] [posicion: 8 bytes (long)] = 48 bytes por entrada
 *
 * Operaciones soportadas: CREATE, READ, UPDATE, DELETE (CRUD)
 */
public class VentaStorage {

    private static final String ARCHIVO_DAT = "ventas.dat";
    private static final String ARCHIVO_IDX = "ventas.idx";

    // Se carga al iniciar y se persiste en ventas.idx
    private final TreeMap<String, Long> indice = new TreeMap<>();

    public VentaStorage() throws IOException {
        cargarIndice();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE - Insertar nueva venta
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inserta una nueva venta al final de ventas.dat y actualiza el índice.
     * @throws IllegalArgumentException si ya existe una venta con ese ID
     */
    public void insertar(Venta venta) throws IOException {
        if (indice.containsKey(venta.getIdVenta())) {
            throw new IllegalArgumentException("Ya existe una venta con ID: " + venta.getIdVenta());
        }

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "rw")) {
            long posicion = raf.length();        // posición al final del archivo
            raf.seek(posicion);
            escribirRegistro(raf, venta);

            // actualizar índice en memoria y en disco
            indice.put(venta.getIdVenta(), posicion);
            guardarIndice();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ - Buscar venta por ID (O(log N) con búsqueda en TreeMap)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Busca una venta por su ID usando el índice.
     * @return la Venta encontrada, o null si no existe
     */
    public Venta buscarPorId(String idVenta) throws IOException {
        Long posicion = indice.get(idVenta);
        if (posicion == null) return null;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "r")) {
            raf.seek(posicion);
            return leerRegistro(raf);
        }
    }

    /**
     * Retorna todas las ventas almacenadas en ventas.dat.
     */
    public List<Venta> listarTodas() throws IOException {
        List<Venta> ventas = new ArrayList<>();
        if (!new File(ARCHIVO_DAT).exists()) return ventas;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                ventas.add(leerRegistro(raf));
            }
        }
        return ventas;
    }

    /**
     * Retorna solo las ventas con estado 'P' (pendientes de envío).
     */
    public List<Venta> listarPendientes() throws IOException {
        List<Venta> pendientes = new ArrayList<>();
        for (Venta v : listarTodas()) {
            if (v.isPendiente()) pendientes.add(v);
        }
        return pendientes;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE - Modificar una venta existente
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Actualiza los datos de una venta en su posición exacta del .dat.
     * Solo se puede actualizar si el estado es 'P' (pendiente).
     */
    public boolean actualizar(Venta venta) throws IOException {
        Long posicion = indice.get(venta.getIdVenta());
        if (posicion == null) return false;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "rw")) {
            raf.seek(posicion);
            Venta actual = leerRegistro(raf);

            if (actual.isEnviado()) {
                throw new IllegalStateException("No se puede modificar una venta ya enviada al servidor.");
            }

            raf.seek(posicion); // volver al inicio del registro
            escribirRegistro(raf, venta);
        }
        return true;
    }

    /**
     * Marca una venta como Enviada ('E') después de la sincronización exitosa.
     */
    public void marcarComoEnviada(String idVenta) throws IOException {
        Long posicion = indice.get(idVenta);
        if (posicion == null) return;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "rw")) {
            raf.seek(posicion);
            Venta v = leerRegistro(raf);
            v.setEstado('E');
            raf.seek(posicion);
            escribirRegistro(raf, v);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE - Eliminar (marcado lógico, no físico)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Elimina lógicamente una venta marcando su ID como "ELIMINADO".
     * No se elimina físicamente para mantener integridad del archivo de tamaño fijo.
     * Solo se puede eliminar si el estado es 'P'.
     */
    public boolean eliminar(String idVenta) throws IOException {
        Long posicion = indice.get(idVenta);
        if (posicion == null) return false;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_DAT, "rw")) {
            raf.seek(posicion);
            Venta v = leerRegistro(raf);

            if (v.isEnviado()) {
                throw new IllegalStateException("No se puede eliminar una venta ya enviada al servidor.");
            }

            // Marcar como eliminado: estado = 'X'
            v.setEstado('X');
            v.setIdVenta("ELIMINADO__________"); // sobrescribir ID
            raf.seek(posicion);
            escribirRegistro(raf, v);
        }

        indice.remove(idVenta);
        guardarIndice();
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILIDADES INTERNAS
    // ═══════════════════════════════════════════════════════════════════════

    /** Escribe un registro de tamaño fijo en la posición actual del RAF. */
    private void escribirRegistro(RandomAccessFile raf, Venta v) throws IOException {
        raf.writeChars(Venta.padOrTruncate(v.getIdVenta(),    Venta.TAM_ID_VENTA));
        raf.writeChars(Venta.padOrTruncate(v.getIdVendedor(), Venta.TAM_ID_VENDEDOR));
        raf.writeChars(Venta.padOrTruncate(v.getIdProducto() != null ? v.getIdProducto() : "P000", Venta.TAM_ID_PRODUCTO));
        raf.writeChars(Venta.padOrTruncate(v.getFecha(),      Venta.TAM_FECHA));
        raf.writeDouble(v.getMontoTotal());
        raf.writeChar(v.getEstado());
    }

    /** Lee un registro de tamaño fijo desde la posición actual del RAF. */
    private Venta leerRegistro(RandomAccessFile raf) throws IOException {
        Venta v = new Venta();

        char[] bufId  = new char[Venta.TAM_ID_VENTA];
        for (int i = 0; i < Venta.TAM_ID_VENTA;    i++) bufId[i]  = raf.readChar();

        char[] bufVen = new char[Venta.TAM_ID_VENDEDOR];
        for (int i = 0; i < Venta.TAM_ID_VENDEDOR; i++) bufVen[i] = raf.readChar();

        char[] bufProd = new char[Venta.TAM_ID_PRODUCTO];
        for (int i = 0; i < Venta.TAM_ID_PRODUCTO; i++) bufProd[i] = raf.readChar();

        char[] bufFec = new char[Venta.TAM_FECHA];
        for (int i = 0; i < Venta.TAM_FECHA;       i++) bufFec[i] = raf.readChar();

        v.setIdVenta(    new String(bufId).trim());
        v.setIdVendedor( new String(bufVen).trim());
        v.setIdProducto( new String(bufProd).trim());
        v.setFecha(      new String(bufFec).trim());
        v.setMontoTotal( raf.readDouble());
        v.setEstado(     raf.readChar());
        return v;
    }

    /** Carga el índice desde ventas.idx al TreeMap en memoria. */
    private void cargarIndice() throws IOException {
        File f = new File(ARCHIVO_IDX);
        if (!f.exists()) return;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_IDX, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                char[] bufId = new char[Venta.TAM_ID_VENTA];
                for (int i = 0; i < Venta.TAM_ID_VENTA; i++) bufId[i] = raf.readChar();
                String id  = new String(bufId).trim();
                long   pos = raf.readLong();
                indice.put(id, pos);
            }
        }
    }

    /** Persiste el TreeMap completo en ventas.idx (reescritura total). */
    private void guardarIndice() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_IDX, "rw")) {
            raf.setLength(0); // limpiar archivo
            for (Map.Entry<String, Long> entry : indice.entrySet()) {
                raf.writeChars(Venta.padOrTruncate(entry.getKey(), Venta.TAM_ID_VENTA));
                raf.writeLong(entry.getValue());
            }
        }
    }

    /** Retorna el número de registros totales (incluyendo eliminados). */
    public int contarRegistros() {
        return indice.size();
    }
}
