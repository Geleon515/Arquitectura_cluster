package arqui.grupo5.cluster;

import arqui.grupo5.model.Venta;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * NodoMaster: Orquestador del cluster que distribuye el procesamiento
 * de ventas entre los Workers y consolida los resultados.
 *
 * Lee Assign.txt para saber qué Workers existen y qué regiones tiene cada uno.
 * Lanza un hilo por Worker, envía las regiones, recibe las ventas filtradas
 * y muestra el resultado consolidado en consola.
 *
 * Uso: java arqui.grupo5.cluster.NodoMaster
 */
public class NodoMaster {

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  NodoMaster — LogiMarket Perú S.A.");
        System.out.println("  Cargando configuración de Assign.txt...");
        System.out.println("═══════════════════════════════════════════");

        // 1. Leer Assign.txt → Map<"ip:puerto", "regiones">
        Map<String, String> asignaciones = cargarAsignaciones("Assign.txt");

        if (asignaciones.isEmpty()) {
            System.err.println("ERROR: No se encontraron asignaciones en Assign.txt");
            System.exit(1);
        }

        System.out.println("Workers configurados: " + asignaciones.size());
        for (Map.Entry<String, String> entry : asignaciones.entrySet()) {
            System.out.println("  " + entry.getKey() + " → regiones: " + entry.getValue());
        }
        System.out.println();

        // 2. Fork: lanzar un hilo por cada Worker
        ExecutorService pool = Executors.newFixedThreadPool(asignaciones.size());
        List<Future<List<Venta>>> futuros = new ArrayList<>();
        List<String> destinos = new ArrayList<>();

        for (Map.Entry<String, String> entry : asignaciones.entrySet()) {
            String destino = entry.getKey();
            String regiones = entry.getValue();
            destinos.add(destino);

            futuros.add(pool.submit(() -> consultarWorker(destino, regiones)));
        }

        // 3. Join: esperar que todos los Workers respondan
        List<Venta> consolidado = new ArrayList<>();
        double montoTotal = 0.0;
        int workersExitosos = 0;

        for (int i = 0; i < futuros.size(); i++) {
            String destino = destinos.get(i);
            try {
                List<Venta> parcial = futuros.get(i).get(30, TimeUnit.SECONDS);
                consolidado.addAll(parcial);
                double subtotal = 0;
                for (Venta v : parcial) subtotal += v.getMontoTotal();
                montoTotal += subtotal;
                workersExitosos++;

                System.out.printf("  [%s] → %d ventas, subtotal: S/. %.2f%n",
                        destino, parcial.size(), subtotal);
            } catch (TimeoutException te) {
                System.err.println("  [" + destino + "] → TIMEOUT: el Worker no respondió en 30 segundos.");
            } catch (ExecutionException ee) {
                System.err.println("  [" + destino + "] → ERROR: " + ee.getCause().getMessage());
            }
        }

        // 4. Mostrar resultado consolidado
        System.out.println();
        System.out.println("═══════════════════════════════════════════");
        System.out.println("       RESULTADO CONSOLIDADO DEL CLUSTER");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  Workers respondieron: " + workersExitosos + " / " + asignaciones.size());
        System.out.println("  Total ventas:         " + consolidado.size());
        System.out.printf("  Monto total:          S/. %.2f%n", montoTotal);
        System.out.println("═══════════════════════════════════════════");

        // Detalle de ventas (opcional, mostrar si hay pocas)
        if (!consolidado.isEmpty() && consolidado.size() <= 50) {
            System.out.println("\nDetalle de ventas:");
            System.out.printf("  %-22s %-12s %-18s %-6s %12s%n",
                    "ID Venta", "Vendedor", "Fecha", "Región", "Monto");
            System.out.println("  " + "-".repeat(74));
            for (Venta v : consolidado) {
                System.out.printf("  %-22s %-12s %-18s %-6s %12.2f%n",
                        v.getIdVenta(), v.getIdVendedor(), v.getFecha(),
                        v.getRegion(), v.getMontoTotal());
            }
        }

        pool.shutdown();
    }

    /**
     * Lee Assign.txt y retorna un mapa con las asignaciones.
     * Formato esperado: IP:PUERTO<tab/espacio>REGIONES_SEPARADAS_POR_COMA
     */
    private static Map<String, String> cargarAsignaciones(String ruta) throws IOException {
        Map<String, String> mapa = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] partes = linea.split("\\s+");
                if (partes.length >= 2) {
                    mapa.put(partes[0], partes[1]);
                }
            }
        }

        return mapa;
    }

    /**
     * Conecta a un Worker, envía las regiones y recibe la lista de ventas.
     */
    @SuppressWarnings("unchecked")
    private static List<Venta> consultarWorker(String destino, String regiones) throws Exception {
        String[] partes = destino.split(":");
        String ip = partes[0];
        int puerto = Integer.parseInt(partes[1]);

        try (Socket socket = new Socket(ip, puerto)) {
            // ObjectOutputStream ANTES de ObjectInputStream (mismo orden que el Worker)
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Enviar regiones
            out.writeObject(regiones);
            out.flush();

            // Recibir lista de ventas
            List<Venta> resultado = (List<Venta>) in.readObject();

            in.close();
            out.close();

            return resultado;

        } catch (ConnectException ce) {
            throw new Exception("No se pudo conectar al Worker en " + destino + ". ¿Está activo?", ce);
        }
    }
}
