package arqui.grupo5.vendiaUpdater.db;

import arqui.grupo5.vendiaUpdater.model.Venta;

import java.sql.*;
import java.util.List;

public class MySqlRepository implements AutoCloseable {

    private final Connection conn;

    public MySqlRepository(String url, String user, String password) throws SQLException {
        conn = DriverManager.getConnection(url, user, password);
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS ventas (
                id_venta     VARCHAR(20) PRIMARY KEY,
                id_vendedor  VARCHAR(20) NOT NULL,
                fecha        VARCHAR(20) NOT NULL,
                monto_total  DOUBLE      NOT NULL,
                estado       CHAR(1)     NOT NULL,
                recibido_en  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
            )
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void insertarVenta(Venta v) throws SQLException {
        String sql = """
            INSERT IGNORE INTO ventas (id_venta, id_vendedor, fecha, monto_total, estado)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getIdVenta());
            ps.setString(2, v.getIdVendedor());
            ps.setString(3, v.getFecha());
            ps.setDouble(4, v.getMontoTotal());
            ps.setString(5, String.valueOf(v.getEstado()));
            ps.executeUpdate();
        }
    }

    public void insertarBatch(List<Venta> ventas) throws SQLException {
        if (ventas.isEmpty()) return;

        String sql = """
            INSERT IGNORE INTO ventas (id_venta, id_vendedor, fecha, monto_total, estado)
            VALUES (?, ?, ?, ?, ?)
            """;
        boolean autoCommitPrevio = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Venta v : ventas) {
                ps.setString(1, v.getIdVenta());
                ps.setString(2, v.getIdVendedor());
                ps.setString(3, v.getFecha());
                ps.setDouble(4, v.getMontoTotal());
                ps.setString(5, String.valueOf(v.getEstado()));
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommitPrevio);
        }
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
