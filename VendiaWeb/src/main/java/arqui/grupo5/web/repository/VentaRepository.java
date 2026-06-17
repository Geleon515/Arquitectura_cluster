package arqui.grupo5.web.repository;

import arqui.grupo5.web.model.Venta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VentaRepository {

    private final JdbcTemplate jdbc;

    public VentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ventas (
                id_venta     VARCHAR(20) PRIMARY KEY,
                id_vendedor  VARCHAR(20) NOT NULL,
                id_producto  VARCHAR(20) NOT NULL DEFAULT 'P000',
                fecha        VARCHAR(20) NOT NULL,
                monto_total  DOUBLE      NOT NULL,
                estado       CHAR(1)     NOT NULL DEFAULT 'P',
                recibido_en  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
            )
        """);
        try {
            jdbc.execute("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS id_producto VARCHAR(20) NOT NULL DEFAULT 'P000'");
        } catch (Exception ignored) {}
    }

    private final RowMapper<Venta> mapper = (rs, rowNum) -> new Venta(
        rs.getString("id_venta"),
        rs.getString("id_vendedor"),
        rs.getString("id_producto"),
        rs.getString("fecha"),
        rs.getDouble("monto_total"),
        rs.getString("estado")
    );

    public List<Venta> listarTodas() {
        return jdbc.query(
            "SELECT * FROM ventas WHERE estado != 'X' ORDER BY recibido_en DESC",
            mapper
        );
    }

    public Venta buscarPorId(String idVenta) {
        List<Venta> r = jdbc.query(
            "SELECT * FROM ventas WHERE id_venta = ?", mapper, idVenta
        );
        return r.isEmpty() ? null : r.get(0);
    }

    public int insertar(Venta v) {
        return jdbc.update(
            "INSERT IGNORE INTO ventas (id_venta, id_vendedor, id_producto, fecha, monto_total, estado) VALUES (?,?,?,?,?,?)",
            v.getIdVenta(), v.getIdVendedor(), v.getIdProducto(), v.getFecha(), v.getMontoTotal(), v.getEstado()
        );
    }

    public int actualizarMonto(String idVenta, double nuevoMonto) {
        return jdbc.update(
            "UPDATE ventas SET monto_total = ? WHERE id_venta = ? AND estado = 'P'",
            nuevoMonto, idVenta
        );
    }

    public int eliminar(String idVenta) {
        return jdbc.update(
            "UPDATE ventas SET estado = 'X' WHERE id_venta = ? AND estado = 'P'",
            idVenta
        );
    }

    public int contarTotales() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM ventas WHERE estado != 'X'", Integer.class);
        return n != null ? n : 0;
    }

    public int contarPendientes() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM ventas WHERE estado = 'P'", Integer.class);
        return n != null ? n : 0;
    }

    public List<Venta> listarTodas(JdbcTemplate otroJdbc) {
        return otroJdbc.query(
            "SELECT * FROM ventas WHERE estado != 'X' ORDER BY recibido_en DESC",
            mapper
        );
    }
}
