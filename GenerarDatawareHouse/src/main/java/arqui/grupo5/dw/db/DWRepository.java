package arqui.grupo5.dw.db;

import arqui.grupo5.dw.model.VentaOperacional;

import java.sql.*;
import java.util.*;

public class DWRepository implements AutoCloseable {

    private final Connection conn;

    private record AggKey(String idVendedor, String idFecha) {}

    public DWRepository(String url, String user, String password) throws SQLException {
        crearSchemaSiNoExiste(url, user, password);
        conn = DriverManager.getConnection(url, user, password);
        crearTablas();
    }

    private void crearSchemaSiNoExiste(String url, String user, String password) throws SQLException {
        String baseUrl = url.substring(0, url.lastIndexOf('/') + 1);
        String dbName  = url.substring(url.lastIndexOf('/') + 1).replaceFirst("[?#].*$", "");
        try (Connection c  = DriverManager.getConnection(baseUrl, user, password);
             Statement  st = c.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        }
    }

    private void crearTablas() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS dim_vendedor (
                    id_vendedor VARCHAR(20) PRIMARY KEY
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS dim_fecha (
                    id_fecha  VARCHAR(7) PRIMARY KEY,
                    mes       INT        NOT NULL,
                    anio      INT        NOT NULL,
                    trimestre INT        NOT NULL
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS fact_ventas (
                    id_vendedor VARCHAR(20) NOT NULL,
                    id_fecha    VARCHAR(7)  NOT NULL,
                    monto_total DOUBLE      NOT NULL DEFAULT 0,
                    cantidad    INT         NOT NULL DEFAULT 0,
                    PRIMARY KEY (id_vendedor, id_fecha),
                    FOREIGN KEY (id_vendedor) REFERENCES dim_vendedor(id_vendedor),
                    FOREIGN KEY (id_fecha)    REFERENCES dim_fecha(id_fecha)
                )
                """);
        }
    }

    public int cargarDW(List<VentaOperacional> ventas) throws SQLException {
        Set<String>           vendedores = new LinkedHashSet<>();
        Set<String>           fechas     = new LinkedHashSet<>();
        Map<AggKey, double[]> agg        = new LinkedHashMap<>();

        for (VentaOperacional v : ventas) {
            String idFecha = parseFecha(v.getFecha());
            vendedores.add(v.getIdVendedor());
            fechas.add(idFecha);
            double[] arr = agg.computeIfAbsent(new AggKey(v.getIdVendedor(), idFecha), k -> new double[2]);
            arr[0] += v.getMontoTotal();
            arr[1]++;
        }

        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO dim_vendedor (id_vendedor) VALUES (?)")) {
                for (String v : vendedores) {
                    ps.setString(1, v);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO dim_fecha (id_fecha, mes, anio, trimestre) VALUES (?,?,?,?)")) {
                for (String f : fechas) {
                    int mes = parseMes(f);
                    ps.setString(1, f);
                    ps.setInt(2, mes);
                    ps.setInt(3, parseAnio(f));
                    ps.setInt(4, calcTrimestre(mes));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO fact_ventas (id_vendedor, id_fecha, monto_total, cantidad) " +
                    "VALUES (?, ?, ?, ?) AS nuevos " +
                    "ON DUPLICATE KEY UPDATE monto_total = nuevos.monto_total, cantidad = nuevos.cantidad")) {
                for (Map.Entry<AggKey, double[]> entry : agg.entrySet()) {
                    ps.setString(1, entry.getKey().idVendedor());
                    ps.setString(2, entry.getKey().idFecha());
                    ps.setDouble(3, entry.getValue()[0]);
                    ps.setInt(4,    (int) entry.getValue()[1]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return agg.size();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // fecha formato "yyyy-MM-dd HH:mm"  →  id_fecha "yyyy-MM"
    private String parseFecha(String fecha) {
        if (fecha == null || fecha.length() < 7) return "0000-00";
        return fecha.substring(0, 7);
    }

    private int parseMes(String idFecha) {
        try { return Integer.parseInt(idFecha.substring(5, 7)); }
        catch (NumberFormatException e) { return 0; }
    }

    private int parseAnio(String idFecha) {
        try { return Integer.parseInt(idFecha.substring(0, 4)); }
        catch (NumberFormatException e) { return 0; }
    }

    private int calcTrimestre(int mes) {
        return (mes - 1) / 3 + 1;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
