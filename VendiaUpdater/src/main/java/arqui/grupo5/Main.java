package arqui.grupo5;

import arqui.grupo5.vendiaUpdater.server.TcpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Main {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        Properties cfg = cargarConfig("updater.properties");

        int    puerto = Integer.parseInt(cfg.getProperty("puerto",   "9090"));
        String dbUrl  = cfg.getProperty("db.url",      "jdbc:mysql://localhost:3306/logimarket");
        String dbUser = cfg.getProperty("db.usuario",  "root");
        String dbPass = cfg.getProperty("db.password", "");

        System.out.println("=================================================");
        System.out.println("  VendiaUpdater — LogiMarket Peru S.A.");
        System.out.println("=================================================");
        System.out.println("  Puerto TCP  : " + puerto);
        System.out.println("  BD URL      : " + dbUrl);
        System.out.println("  BD usuario  : " + dbUser);
        System.out.println("=================================================");
        System.out.println();

        TcpServer servidor = new TcpServer(puerto, dbUrl, dbUser, dbPass, Main::log);
        servidor.iniciar();

        System.out.println("Servidor activo. Presione ENTER para detener.");
        System.in.read();
        servidor.detener();
    }

    private static Properties cargarConfig(String ruta) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(ruta)) {
            props.load(fis);
            System.out.println("Configuracion cargada desde: " + ruta);
        } catch (IOException e) {
            System.out.println("AVISO: no se encontro '" + ruta + "', usando valores por defecto.");
        }
        return props;
    }

    private static void log(String msg) {
        String hora = LocalTime.now().format(TIME_FMT);
        System.out.println("[" + hora + "] " + msg);
    }
}
