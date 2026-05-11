package arquitectura.grupo5;

import java.util.List;
import java.util.Scanner;

public class Main {

    static ArchivoVentas archivo = new ArchivoVentas("ventas.dat");
    static Scanner sc = new Scanner(System.in);

    // -------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  SISTEMA DE VENTAS - V1 Arquitectura Unitaria");
        System.out.println("==============================================");

        // Cargar datos de ejemplo si el archivo esta vacio
        if (archivo.contarRegistros() == 0) {
            cargarDatosEjemplo();
        }

        int opcion;
        do {
            mostrarMenu();
            opcion = leerInt("Opcion: ");
            switch (opcion) {
                case 1: agregarVenta();       break;
                case 2: listarTodas();        break;
                case 3: buscarPorId();        break;
                case 4: listarPorRegion();    break;
                case 5: totalPorRegion();     break;
                case 6: resumenGeneral();     break;
                case 0: System.out.println("Saliendo..."); break;
                default: System.out.println("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    // -------------------------------------------------------
    static void mostrarMenu() {
        System.out.println("\n----------------------------------------------");
        System.out.println(" 1. Agregar venta");
        System.out.println(" 2. Listar todas las ventas");
        System.out.println(" 3. Buscar venta por ID");
        System.out.println(" 4. Listar ventas por region");
        System.out.println(" 5. Total de ventas por region");
        System.out.println(" 6. Resumen general (todas las regiones)");
        System.out.println(" 0. Salir");
        System.out.println("----------------------------------------------");
    }

    // -------------------------------------------------------
    static void agregarVenta() {
        System.out.println("\n-- AGREGAR VENTA --");
        System.out.println("Regiones: 1=Lima  2=Ancash  3=Arequipa  4=Ayacucho");
        System.out.println("          5=Cajamarca  6=Cusco  9=La Libertad  10=Lambayeque");
        int region    = leerInt("Codigo de region: ");
        sc.nextLine();
        String producto = leerTexto("Producto: ");
        double monto    = leerDouble("Monto (S/.): ");
        sc.nextLine();
        String fecha    = leerTexto("Fecha (dd/mm/aaaa): ");

        Venta v = new Venta(0, region, producto, monto, fecha);
        int indice = archivo.agregarVenta(v);
        System.out.println("Venta registrada en indice " + indice + " con ID " + v.getId());
    }

    // -------------------------------------------------------
    static void listarTodas() {
        System.out.println("\n-- TODAS LAS VENTAS --");
        List<Venta> lista = archivo.listarTodas();
        if (lista.isEmpty()) {
            System.out.println("No hay ventas registradas.");
            return;
        }
        System.out.printf("%-6s %-12s %-30s %-14s %-12s%n",
                "ID", "Region", "Producto", "Monto", "Fecha");
        System.out.println("-".repeat(78));
        for (Venta v : lista) {
            System.out.printf("%-6d %-12s %-30s S/. %10.2f  %-12s%n",
                    v.getId(), v.getNombreRegion(), v.getProducto(),
                    v.getMonto(), v.getFecha());
        }
        System.out.println("Total registros: " + lista.size());
    }

    // -------------------------------------------------------
    static void buscarPorId() {
        System.out.println("\n-- BUSCAR POR ID --");
        int id = leerInt("ID a buscar: ");
        Venta v = archivo.buscarPorId(id);
        if (v == null) {
            System.out.println("No se encontro venta con ID " + id);
        } else {
            System.out.println("Encontrado: " + v);
        }
    }

    // -------------------------------------------------------
    static void listarPorRegion() {
        System.out.println("\n-- LISTAR POR REGION --");
        int codigo = leerInt("Codigo de region: ");
        List<Venta> lista = archivo.listarPorRegion(codigo);
        if (lista.isEmpty()) {
            System.out.println("No hay ventas para esa region.");
            return;
        }
        System.out.println("Ventas de region codigo " + codigo + ":");
        for (Venta v : lista) {
            System.out.println("  " + v);
        }
    }

    // -------------------------------------------------------
    static void totalPorRegion() {
        System.out.println("\n-- TOTAL POR REGION --");
        int codigo = leerInt("Codigo de region: ");
        double total = archivo.totalPorRegion(codigo);
        Venta ejemplo = new Venta(0, codigo, "", 0, "");
        System.out.printf("Total ventas region %s: S/. %.2f%n",
                ejemplo.getNombreRegion(), total);
    }

    // -------------------------------------------------------
    static void resumenGeneral() {
        System.out.println("\n-- RESUMEN GENERAL POR REGION --");
        System.out.printf("%-15s  %8s  %12s%n", "Region", "Ventas", "Total S/.");
        System.out.println("-".repeat(40));
        double granTotal = 0;
        int[] codigos = {1, 2, 3, 4, 5, 6, 9, 10};
        for (int cod : codigos) {
            List<Venta> lista = archivo.listarPorRegion(cod);
            if (!lista.isEmpty()) {
                double total = archivo.totalPorRegion(cod);
                Venta ej = new Venta(0, cod, "", 0, "");
                System.out.printf("%-15s  %8d  %12.2f%n",
                        ej.getNombreRegion(), lista.size(), total);
                granTotal += total;
            }
        }
        System.out.println("-".repeat(40));
        System.out.printf("%-15s  %8d  %12.2f%n",
                "TOTAL GENERAL", archivo.contarRegistros(), granTotal);
    }

    // -------------------------------------------------------
    // Carga 10 ventas de ejemplo al iniciar por primera vez
    // -------------------------------------------------------
    static void cargarDatosEjemplo() {
        System.out.println("Cargando datos de ejemplo...");
        Object[][] datos = {
                {1,  "Laptop HP",          2500.00, "01/04/2025"},
                {2,  "Impresora Epson",     450.00, "02/04/2025"},
                {1,  "Mouse Logitech",       85.00, "03/04/2025"},
                {3,  "Teclado Mecanico",    320.00, "03/04/2025"},
                {2,  "Monitor LG 24",      1100.00, "04/04/2025"},
                {1,  "Disco SSD 1TB",       380.00, "05/04/2025"},
                {6,  "Camara Web HD",       210.00, "05/04/2025"},
                {3,  "Auriculares Sony",    290.00, "06/04/2025"},
                {1,  "Tablet Samsung",      950.00, "07/04/2025"},
                {2,  "Proyector Epson",    3200.00, "08/04/2025"},

        };
        for (Object[] d : datos) {
            Venta v = new Venta(0, (int)d[0], (String)d[1], (double)d[2], (String)d[3]);
            archivo.agregarVenta(v);
        }
        System.out.println("10 ventas de ejemplo cargadas.\n");
    }

    // -------------------------------------------------------
    // Helpers de lectura segura
    // -------------------------------------------------------
    static int leerInt(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Ingrese un numero entero."); }
        }
    }

    static double leerDouble(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try { return Double.parseDouble(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Ingrese un numero valido."); }
        }
    }

    static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine().trim();
    }
}