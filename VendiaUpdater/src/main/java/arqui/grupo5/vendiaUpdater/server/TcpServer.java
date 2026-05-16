package arqui.grupo5.vendiaUpdater.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TcpServer {

    private final int            puerto;
    private final String         dbUrl;
    private final String         dbUser;
    private final String         dbPassword;
    private final Consumer<String> logger;

    private ServerSocket    serverSocket;
    private ExecutorService executor;
    private volatile boolean running;

    public TcpServer(int puerto, String dbUrl, String dbUser, String dbPassword,
                     Consumer<String> logger) {
        this.puerto     = puerto;
        this.dbUrl      = dbUrl;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;
        this.logger     = logger;
    }

    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(puerto);
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "handler");
            t.setDaemon(true);
            return t;
        });
        running = true;
        logger.accept("Servidor escuchando en puerto " + puerto + "...");

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    executor.submit(new ClientHandler(client, dbUrl, dbUser, dbPassword, logger));
                } catch (IOException e) {
                    if (running) logger.accept("ERROR al aceptar conexion: " + e.getMessage());
                }
            }
        }, "accept-thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void detener() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        if (executor != null) executor.shutdownNow();
        logger.accept("Servidor detenido.");
    }
}
