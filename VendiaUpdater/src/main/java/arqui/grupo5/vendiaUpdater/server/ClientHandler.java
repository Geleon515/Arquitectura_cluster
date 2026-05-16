package arqui.grupo5.vendiaUpdater.server;

import arqui.grupo5.vendiaUpdater.db.MySqlRepository;
import arqui.grupo5.vendiaUpdater.model.Venta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {

    private final Socket         socket;
    private final String         dbUrl;
    private final String         dbUser;
    private final String         dbPassword;
    private final Consumer<String> logger;

    public ClientHandler(Socket socket, String dbUrl, String dbUser, String dbPassword,
                         Consumer<String> logger) {
        this.socket     = socket;
        this.dbUrl      = dbUrl;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;
        this.logger     = logger;
    }

    @Override
    public void run() {
        String origen = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        logger.accept("Conexion aceptada desde " + origen);

        try (socket;
             DataInputStream  in  = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             MySqlRepository  db  = new MySqlRepository(dbUrl, dbUser, dbPassword)) {

            int count = in.readInt();
            logger.accept("Esperando " + count + " registro(s) de " + origen + "...");

            for (int i = 0; i < count; i++) {
                Venta v = leerVenta(in);
                db.insertarVenta(v);
                logger.accept("  OK: " + v);
            }

            out.writeUTF("ACK");
            out.flush();
            logger.accept("ACK enviado. " + count + " venta(s) guardadas en MySQL desde " + origen);

        } catch (Exception e) {
            logger.accept("ERROR con " + origen + ": " + e.getMessage());
        }
    }

    private Venta leerVenta(DataInputStream in) throws Exception {
        String idVenta    = leerChars(in, Venta.ID_LEN).trim();
        String idVendedor = leerChars(in, Venta.VENDEDOR_LEN).trim();
        String fecha      = leerChars(in, Venta.FECHA_LEN).trim();
        double monto      = in.readDouble();
        char   estado     = in.readChar();
        return new Venta(idVenta, idVendedor, fecha, monto, estado);
    }

    private String leerChars(DataInputStream in, int len) throws Exception {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(in.readChar());
        return sb.toString();
    }
}
