package arqui.grupo5.vendiasender.sender;

import arqui.grupo5.vendiasender.model.Venta;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Envia las ventas pendientes copiando un archivo binario a la carpeta
 * compartida DATOS\ y espera la aparicion del archivo .ack correspondiente
 * como confirmacion de que VendiaUpdater las inserto en MySQL.
 *
 * Reemplaza al antiguo TcpSender que usaba sockets TCP.
 */
public class FileSender {

    private static final DateTimeFormatter TS_FMT       = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final long              POLL_MS      = 500;
    private static final long              TIMEOUT_MS   = 30_000;

    private final String rutaCarpetaDatos;

    public FileSender(String rutaCarpetaDatos) {
        this.rutaCarpetaDatos = rutaCarpetaDatos;
    }

    /**
     * Escribe los registros pendientes en DATOS\ventas_<ts>.dat y se queda
     * esperando hasta TIMEOUT_MS milisegundos a que aparezca el .ack.
     *
     * @throws IOException si la carpeta no existe, falla la escritura, llega
     *         un ACK con error, o se cumple el timeout sin recibir respuesta.
     */
    public void enviar(List<Venta> ventas, Consumer<String> logger) throws IOException {
        File carpeta = new File(rutaCarpetaDatos);
        if (!carpeta.isDirectory()) {
            throw new IOException("La carpeta compartida no existe: " + rutaCarpetaDatos);
        }

        String timestamp  = LocalDateTime.now().format(TS_FMT);
        String nombreBase = "ventas_" + timestamp;
        File   datFile    = new File(carpeta, nombreBase + ".dat");
        File   ackFile    = new File(carpeta, nombreBase + ".ack");

        logger.accept("Escribiendo " + ventas.size() + " registro(s) en " + datFile.getName() + "...");
        escribirRegistros(datFile, ventas);
        logger.accept("Archivo enviado. Esperando confirmacion del servidor...");

        String respuesta = esperarAck(ackFile, logger);
        procesarRespuesta(respuesta, logger);
    }

    private void escribirRegistros(File datFile, List<Venta> ventas) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(datFile)))) {
            for (Venta v : ventas) {
                writeChars(out, v.getIdVenta(),    Venta.ID_LEN);
                writeChars(out, v.getIdVendedor(), Venta.VENDEDOR_LEN);
                writeChars(out, v.getFecha(),      Venta.FECHA_LEN);
                writeChars(out, v.getRegion(),     Venta.REGION_LEN);
                out.writeDouble(v.getMontoTotal());
                out.writeChar(v.getEstado());
            }
            out.flush();
        }
    }

    private String esperarAck(File ackFile, Consumer<String> logger) throws IOException {
        long inicio = System.currentTimeMillis();
        while (System.currentTimeMillis() - inicio < TIMEOUT_MS) {
            if (ackFile.exists()) {
                String contenido = Files.readString(ackFile.toPath()).trim();
                logger.accept("ACK recibido: " + ackFile.getName());
                return contenido;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Espera de ACK interrumpida", e);
            }
        }
        throw new IOException("Timeout: el servidor no respondio en "
                              + (TIMEOUT_MS / 1000) + " segundos. Verifique que VendiaUpdater este corriendo.");
    }

    private void procesarRespuesta(String respuesta, Consumer<String> logger) throws IOException {
        if (respuesta.startsWith("OK")) {
            logger.accept("Envio confirmado: " + respuesta);
            return;
        }
        if (respuesta.startsWith("ERR")) {
            throw new IOException("El servidor reporto un error: " + respuesta);
        }
        throw new IOException("Respuesta de ACK no reconocida: " + respuesta);
    }

    private void writeChars(DataOutputStream out, String s, int len) throws IOException {
        String padded = s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
        for (char c : padded.toCharArray()) out.writeChar(c);
    }

    /** Permite borrar el .dat luego de un envio exitoso (opcional, util si el cliente quiere limpiar). */
    public static boolean borrarSiExiste(Path archivo) {
        try {
            return Files.deleteIfExists(archivo);
        } catch (IOException e) {
            return false;
        }
    }
}
