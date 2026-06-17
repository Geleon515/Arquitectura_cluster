package arqui.grupo5.crosstab.db;

import arqui.grupo5.crosstab.model.CrossTabRow;

import java.sql.*;
import java.util.*;

public class CrossTabRepository implements AutoCloseable {

    private final Connection conn;

    public CrossTabRepository(String url, String user, String pass) throws SQLException {
        conn = DriverManager.getConnection(url, user, pass);
        crearTabla();
    }

    private void crearTabla() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS crosstab_ventas (
                    id_vendedor  VARCHAR(20) NOT NULL,
                    id_producto  VARCHAR(20) NOT NULL DEFAULT 'P000',
                    anio         INT         NOT NULL,
                    q1_monto     DOUBLE      NOT NULL DEFAULT 0,
                    q2_monto     DOUBLE      NOT NULL DEFAULT 0,
                    q3_monto     DOUBLE      NOT NULL DEFAULT 0,
                    q4_monto     DOUBLE      NOT NULL DEFAULT 0,
                    total_anual  DOUBLE      NOT NULL DEFAULT 0,
                    q1_cantidad  INT         NOT NULL DEFAULT 0,
                    q2_cantidad  INT         NOT NULL DEFAULT 0,
                    q3_cantidad  INT         NOT NULL DEFAULT 0,
                    q4_cantidad  INT         NOT NULL DEFAULT 0,
                    total_cant   INT         NOT NULL DEFAULT 0,
                    generado_en  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_vendedor, id_producto, anio)
                )
            """);
        }
    }

    public List<CrossTabRow> computarCrossTab(int anio) throws SQLException {
        String sql = """
            SELECT
                fv.id_vendedor,
                fv.id_producto,
                COALESCE(dp.nombre, fv.id_producto) AS nombre_producto,
                SUM(CASE WHEN df.trimestre = 1 THEN fv.monto_total ELSE 0 END) AS q1_monto,
                SUM(CASE WHEN df.trimestre = 2 THEN fv.monto_total ELSE 0 END) AS q2_monto,
                SUM(CASE WHEN df.trimestre = 3 THEN fv.monto_total ELSE 0 END) AS q3_monto,
                SUM(CASE WHEN df.trimestre = 4 THEN fv.monto_total ELSE 0 END) AS q4_monto,
                SUM(CASE WHEN df.trimestre = 1 THEN fv.cantidad    ELSE 0 END) AS q1_cant,
                SUM(CASE WHEN df.trimestre = 2 THEN fv.cantidad    ELSE 0 END) AS q2_cant,
                SUM(CASE WHEN df.trimestre = 3 THEN fv.cantidad    ELSE 0 END) AS q3_cant,
                SUM(CASE WHEN df.trimestre = 4 THEN fv.cantidad    ELSE 0 END) AS q4_cant
            FROM fact_ventas fv
            JOIN dim_fecha df ON fv.id_fecha = df.id_fecha
            LEFT JOIN dim_producto dp ON fv.id_producto = dp.id_producto
            WHERE df.anio = ?
            GROUP BY fv.id_vendedor, fv.id_producto, dp.nombre
            ORDER BY fv.id_vendedor, fv.id_producto
        """;

        List<CrossTabRow> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new CrossTabRow(
                        rs.getString("id_vendedor"),
                        rs.getString("id_producto"),
                        rs.getString("nombre_producto"),
                        rs.getDouble("q1_monto"), rs.getDouble("q2_monto"),
                        rs.getDouble("q3_monto"), rs.getDouble("q4_monto"),
                        rs.getInt("q1_cant"), rs.getInt("q2_cant"),
                        rs.getInt("q3_cant"), rs.getInt("q4_cant")
                    ));
                }
            }
        }
        return filas;
    }

    public int guardarCrossTab(List<CrossTabRow> filas, int anio) throws SQLException {
        String sql = """
            INSERT INTO crosstab_ventas
                (id_vendedor, id_producto, anio,
                 q1_monto, q2_monto, q3_monto, q4_monto, total_anual,
                 q1_cantidad, q2_cantidad, q3_cantidad, q4_cantidad, total_cant)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            AS nuevos
            ON DUPLICATE KEY UPDATE
                q1_monto    = nuevos.q1_monto,
                q2_monto    = nuevos.q2_monto,
                q3_monto    = nuevos.q3_monto,
                q4_monto    = nuevos.q4_monto,
                total_anual = nuevos.total_anual,
                q1_cantidad = nuevos.q1_cantidad,
                q2_cantidad = nuevos.q2_cantidad,
                q3_cantidad = nuevos.q3_cantidad,
                q4_cantidad = nuevos.q4_cantidad,
                total_cant  = nuevos.total_cant
        """;

        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CrossTabRow r : filas) {
                ps.setString(1, r.getIdVendedor());
                ps.setString(2, r.getIdProducto());
                ps.setInt(3, anio);
                ps.setDouble(4, r.getQ1());
                ps.setDouble(5, r.getQ2());
                ps.setDouble(6, r.getQ3());
                ps.setDouble(7, r.getQ4());
                ps.setDouble(8, r.getTotal());
                ps.setInt(9,  r.getCantQ1());
                ps.setInt(10, r.getCantQ2());
                ps.setInt(11, r.getCantQ3());
                ps.setInt(12, r.getCantQ4());
                ps.setInt(13, r.getCantTotal());
                ps.addBatch();
            }
            int[] res = ps.executeBatch();
            conn.commit();
            return res.length;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Integer> listarAnios() throws SQLException {
        List<Integer> anios = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT DISTINCT anio FROM dim_fecha ORDER BY anio DESC")) {
            while (rs.next()) anios.add(rs.getInt("anio"));
        }
        if (anios.isEmpty()) anios.add(2026);
        return anios;
    }

    public List<String> listarProductos() throws SQLException {
        List<String> lista = new ArrayList<>();
        lista.add("TODOS");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id_producto, nombre FROM dim_producto ORDER BY id_producto")) {
            while (rs.next())
                lista.add(rs.getString("id_producto") + " - " + rs.getString("nombre"));
        }
        return lista;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
