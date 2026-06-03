package arqui.grupo5.vendiaUpdater.watcher;

import arqui.grupo5.vendiaUpdater.db.MySqlRepository;
import arqui.grupo5.vendiaUpdater.model.Venta;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Daemon que vigila la carpeta compartida DATOS\ esperando archivos .dat
 * enviados por VendiaSender. Por cada uno los lee de a 130 bytes, los inserta
 * en MySQL via MySqlRepository.insertarBatch(...) y escribe el .ack
 * correspondiente para confirmar al cliente.
 *
 * Reemplaza al antiguo TcpServer + ClientHandler que escuchaban por sockets.
 */
public class FolderWatcher {

    private final Path             carpeta;
    private final long             intervaloMs;
    private final String           dbUrl;
    private final String           dbUser;
    private final String           dbPassword;
    private final Consumer<String> logger;

    private volatile boolean running;
    private Thread           hilo;

    public FolderWatcher(String rutaCarpeta, long intervaloMs,
                         String dbUrl, String dbUser, String dbPassword,
                         Consumer<String> logger) {
        this.carpeta     = new File(rutaCarpeta).toPath();
        this.intervaloMs = intervaloMs;
        this.dbUrl       = dbUrl;
        this.dbUser      = dbUser;
        this.dbPassword  = dbPassword;
        this.logger      = logger;
    }

    public void iniciar() throws IOException {
        if (!Files.isDirectory(carpeta)) {
            throw new IOException("La carpeta compartida no existe: " + carpeta);
        }
        running = true;
        hilo = new Thread(this::loop, "folder-watcher");
        hilo.setDaemon(true);
        hilo.start();
        logger.accept("Watcher activo en " + carpeta + " (cada " + intervaloMs + " ms).");
    }

    public void detener() {
        running = false;
        if (hilo != null) hilo.interrupt();
        logger.accept("Watcher detenido.");
    }

    private void loop() {
        while (running) {
            try {
                List<File> pendientes = listarDatSinAck();
                for (File dat : pendientes) {
                    procesarArchivo(dat);
                }
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.accept("ERROR en loop: " + e.getMessage());
                try { Thread.sleep(intervaloMs); } catch (InterruptedException ignored) { return; }
            }
        }
    }

    /**
     * Lista archivos .dat de la carpeta que aun no tengan su .ack asociado.
     * Esto garantiza idempotencia: si reiniciamos el watcher no reprocesamos
     * archivos ya confirmados.
     */
    private List<File> listarDatSinAck() {
        File[] archivos = carpeta.toFile().listFiles(
            (dir, name) -> name.toLowerCase().endsWith(".dat"));
        if (archivos == null) return List.of();

        Arrays.sort(archivos, Comparator.comparingLong(File::lastModified));

        List<File> pendientes = new ArrayList<>();
        for (File dat : archivos) {
            File ack = new File(carpeta.toFile(), baseSinExtension(dat.getName()) + ".ack");
            if (!ack.exists()) pendientes.add(dat);
        }
        return pendientes;
    }

    private void procesarArchivo(File dat) {
        String nombre = dat.getName();
        File   ack    = new File(carpeta.toFile(), baseSinExtension(nombre) + ".ack");

        logger.accept("Detectado: " + nombre + " (" + dat.length() + " bytes)");

        if (dat.length() == 0 || dat.length() % Venta.RECORD_SIZE != 0) {
            String msg = "tamano invalido (" + dat.length() + " bytes, debe ser multiplo de "
                         + Venta.RECORD_SIZE + ")";
            logger.accept("  ERROR: " + msg);
            escribirAck(ack, "ERR " + msg);
            return;
        }

        try {
            List<Venta> ventas = leerVentas(dat);
            try (MySqlRepository db = new MySqlRepository(dbUrl, dbUser, dbPassword)) {
                db.insertarBatch(ventas);
            }
            logger.accept("  OK: " + ventas.size() + " venta(s) insertada(s) en MySQL.");
            escribirAck(ack, "OK " + ventas.size());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.accept("  ERROR procesando " + nombre + ": " + msg);
            escribirAck(ack, "ERR " + msg);
        }
    }

    private List<Venta> leerVentas(File dat) throws IOException {
        List<Venta> ventas = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(
                java.nio.file.Files.newInputStream(dat.toPath()))) {
            long totalRegistros = dat.length() / Venta.RECORD_SIZE;
            for (long i = 0; i < totalRegistros; i++) {
                String idVenta    = leerChars(in, Venta.ID_LEN).trim();
                String idVendedor = leerChars(in, Venta.VENDEDOR_LEN).trim();
                String fecha      = leerChars(in, Venta.FECHA_LEN).trim();
                String region     = leerChars(in, Venta.REGION_LEN).trim();
                double monto      = in.readDouble();
                char   estado     = in.readChar();
                ventas.add(new Venta(idVenta, idVendedor, fecha, region, monto, estado));
            }
        }
        return ventas;
    }

    private String leerChars(DataInputStream in, int len) throws IOException {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(in.readChar());
        return sb.toString();
    }

    private void escribirAck(File ack, String contenido) {
        try {
            Files.writeString(ack.toPath(), contenido,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.accept("  ERROR escribiendo ACK " + ack.getName() + ": " + e.getMessage());
        }
    }

    private static String baseSinExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? nombre : nombre.substring(0, punto);
    }
}
