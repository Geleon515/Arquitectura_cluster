package arqui.grupo5.viewcrosstab.db;

import arqui.grupo5.viewcrosstab.model.PivotRow;
import arqui.grupo5.viewcrosstab.model.PivotRow3D;

import java.sql.*;
import java.util.*;

public class CrossTabReader implements AutoCloseable {

    private final Connection conn;

    public CrossTabReader(String url, String user, String pass) throws SQLException {
        conn = DriverManager.getConnection(url, user, pass);
    }

    public List<PivotRow> leerCrossTab(int anio, String idProducto) throws SQLException {
        boolean todos = idProducto == null || idProducto.equals("TODOS");
        String sql = todos
            ? """
              SELECT id_vendedor,
                     SUM(q1_monto) q1_monto, SUM(q2_monto) q2_monto,
                     SUM(q3_monto) q3_monto, SUM(q4_monto) q4_monto,
                     SUM(total_anual) total_anual,
                     SUM(q1_cantidad) q1_cantidad, SUM(q2_cantidad) q2_cantidad,
                     SUM(q3_cantidad) q3_cantidad, SUM(q4_cantidad) q4_cantidad,
                     SUM(total_cant) total_cant
              FROM crosstab_ventas WHERE anio = ?
              GROUP BY id_vendedor ORDER BY total_anual DESC
              """
            : """
              SELECT id_vendedor, q1_monto, q2_monto, q3_monto, q4_monto,
                     total_anual, q1_cantidad, q2_cantidad, q3_cantidad,
                     q4_cantidad, total_cant
              FROM crosstab_ventas WHERE anio = ? AND id_producto = ?
              ORDER BY total_anual DESC
              """;

        List<PivotRow> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            if (!todos) ps.setString(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new PivotRow(
                        rs.getString("id_vendedor"),
                        rs.getDouble("q1_monto"), rs.getDouble("q2_monto"),
                        rs.getDouble("q3_monto"), rs.getDouble("q4_monto"),
                        rs.getDouble("total_anual"),
                        rs.getInt("q1_cantidad"), rs.getInt("q2_cantidad"),
                        rs.getInt("q3_cantidad"), rs.getInt("q4_cantidad"),
                        rs.getInt("total_cant")
                    ));
                }
            }
        }
        return filas;
    }

    public Map<String, Double> totalesPorTrimestre(int anio, String idProducto) throws SQLException {
        boolean todos = idProducto == null || idProducto.equals("TODOS");
        String sql = todos
            ? "SELECT SUM(q1_monto) q1, SUM(q2_monto) q2, SUM(q3_monto) q3, SUM(q4_monto) q4 FROM crosstab_ventas WHERE anio = ?"
            : "SELECT SUM(q1_monto) q1, SUM(q2_monto) q2, SUM(q3_monto) q3, SUM(q4_monto) q4 FROM crosstab_ventas WHERE anio = ? AND id_producto = ?";

        Map<String, Double> totales = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            if (!todos) ps.setString(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totales.put("Q1 (Ene-Mar)", rs.getDouble("q1"));
                    totales.put("Q2 (Abr-Jun)", rs.getDouble("q2"));
                    totales.put("Q3 (Jul-Sep)", rs.getDouble("q3"));
                    totales.put("Q4 (Oct-Dic)", rs.getDouble("q4"));
                }
            }
        }
        return totales;
    }

    public List<Integer> listarAnios() throws SQLException {
        List<Integer> anios = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT DISTINCT anio FROM crosstab_ventas ORDER BY anio DESC")) {
            while (rs.next()) anios.add(rs.getInt("anio"));
        }
        if (anios.isEmpty()) anios.add(2026);
        return anios;
    }

    public List<String[]> listarProductos() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        lista.add(new String[]{"TODOS", "Todos los productos"});
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id_producto, nombre FROM dim_producto ORDER BY id_producto")) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id_producto"), rs.getString("nombre")});
        }
        return lista;
    }

    public List<PivotRow3D> leerCrossTabCompleto(int anio) throws SQLException {
        String sql = """
            SELECT id_vendedor, id_producto,
                   q1_monto, q2_monto, q3_monto, q4_monto
            FROM crosstab_ventas
            WHERE anio = ?
            ORDER BY id_vendedor, id_producto
            """;
        List<PivotRow3D> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new PivotRow3D(
                        rs.getString("id_vendedor"),
                        rs.getString("id_producto"),
                        rs.getDouble("q1_monto"),
                        rs.getDouble("q2_monto"),
                        rs.getDouble("q3_monto"),
                        rs.getDouble("q4_monto")
                    ));
                }
            }
        }
        return filas;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
