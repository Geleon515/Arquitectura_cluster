package arqui.grupo5.vendiasender.sender;

import arqui.grupo5.vendiasender.model.Venta;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class TcpSender {
    private final String host;
    private final int    port;

    public TcpSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Protocolo:
     *   Cliente → Servidor: int (cantidad de registros)
     *   Cliente → Servidor: N registros de 130 bytes cada uno (mismo formato que ventas.dat)
     *   Servidor → Cliente: UTF "ACK"
     */
    public void enviar(List<Venta> ventas, Consumer<String> logger) throws IOException {
        logger.accept("Conectando a " + host + ":" + port + "...");

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream  in  = new DataInputStream(socket.getInputStream())) {

            logger.accept("Conexion establecida. Enviando " + ventas.size() + " registro(s)...");

            out.writeInt(ventas.size());
            for (Venta v : ventas) {
                enviarRegistro(out, v);
                logger.accept("  Enviado: " + v.getIdVenta() + "  |  S/. " + String.format("%.2f", v.getMontoTotal()));
            }
            out.flush();

            String respuesta = in.readUTF();
            if (!"ACK".equals(respuesta)) {
                throw new IOException("Respuesta inesperada del servidor: " + respuesta);
            }
            logger.accept("ACK recibido. Envio completado exitosamente.");
        }
    }

    private void enviarRegistro(DataOutputStream out, Venta v) throws IOException {
        writeChars(out, v.getIdVenta(),    Venta.ID_LEN);
        writeChars(out, v.getIdVendedor(), Venta.VENDEDOR_LEN);
        writeChars(out, v.getFecha(),      Venta.FECHA_LEN);
        out.writeDouble(v.getMontoTotal());
        out.writeChar(v.getEstado());
    }

    private void writeChars(DataOutputStream out, String s, int len) throws IOException {
        String padded = s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
        for (char c : padded.toCharArray()) out.writeChar(c);
    }
}
