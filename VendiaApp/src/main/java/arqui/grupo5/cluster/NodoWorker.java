package arqui.grupo5.cluster;

import arqui.grupo5.model.Venta;
import arqui.grupo5.storage.VentaStorage;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * NodoWorker: Servidor TCP que escucha en un puerto y procesa
 * consultas de ventas filtradas por región.
 *
 * Se instancia 3 veces (una por cada partición de regiones).
 * Uso: java arqui.grupo5.cluster.NodoWorker <puerto>
 */
public class NodoWorker {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: NodoWorker <puerto>");
            System.err.println("  Ejemplo: NodoWorker 8001");
            System.exit(1);
        }

        int puerto = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("═══════════════════════════════════════════");
            System.out.println("  NodoWorker — LogiMarket Perú S.A.");
            System.out.println("  Puerto: " + puerto);
            System.out.println("  Esperando conexiones del Master...");
            System.out.println("═══════════════════════════════════════════");

            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("[Worker:" + puerto + "] Conexión recibida del Master.");

                    // ObjectOutputStream ANTES de ObjectInputStream para evitar deadlock
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                    // 1. Leer las regiones asignadas por el Master
                    String regionesRaw = (String) in.readObject();
                    List<String> regiones = Arrays.asList(regionesRaw.split(","));
                    System.out.println("[Worker:" + puerto + "] Regiones recibidas: " + regiones);

                    // 2. Abrir ventas.dat en modo lectura y filtrar
                    VentaStorage storage = new VentaStorage("ventas.dat");
                    List<Venta> resultados = storage.listarPorRegiones(regiones);

                    // 3. Enviar resultados de vuelta al Master
                    // Envolvemos en ArrayList que es Serializable
                    out.writeObject(new ArrayList<>(resultados));
                    out.flush();

                    System.out.println("[Worker:" + puerto + "] Enviadas " + resultados.size() + " ventas al Master.");

                    // Cerrar conexión actual, seguir escuchando
                    in.close();
                    out.close();
                    client.close();

                } catch (Exception e) {
                    System.err.println("[Worker:" + puerto + "] Error procesando petición: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
