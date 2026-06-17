package arqui.grupo5.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Capa Mirror: replica los datos del servidor operacional (BD principal)
 * hacia el servidor Mirror. Implementa el patrón de replicación activa
 * utilizado en arquitecturas distribuidas de N capas.
 */
@RestController
@RequestMapping("/api/mirror")
@CrossOrigin(origins = "*")
public class MirrorController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.datasource.url}")
    private String mainUrl;

    @Value("${spring.datasource.username}")
    private String mainUser;

    @Value("${spring.datasource.password}")
    private String mainPass;

    @Value("${mirror.datasource.url}")
    private String mirrorUrl;

    @Value("${mirror.datasource.username}")
    private String mirrorUser;

    @Value("${mirror.datasource.password}")
    private String mirrorPass;

    // POST /api/mirror/sincronizar — copia todos los registros activos al Mirror
    @PostMapping("/sincronizar")
    public Map<String, Object> sincronizar() {
        try {
            List<Object[]> ventas = leerVentasPrincipales();
            int copiadas          = escribirEnMirror(ventas);

            return Map.of(
                "estado",             "OK",
                "registrosLeidos",    ventas.size(),
                "registrosCopiados",  copiadas,
                "servidorOrigen",     mainUrl,
                "servidorMirror",     mirrorUrl,
                "timestamp",          LocalDateTime.now().format(TS_FMT)
            );
        } catch (Exception e) {
            return Map.of(
                "estado",   "ERROR",
                "mensaje",  e.getMessage(),
                "timestamp", LocalDateTime.now().format(TS_FMT)
            );
        }
    }

    // GET /api/mirror/estado — verifica conectividad y cuenta registros en el Mirror
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        try (Connection c = DriverManager.getConnection(mirrorUrl, mirrorUser, mirrorPass)) {
            int total = 0;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ventas WHERE estado != 'X'")) {
                if (rs.next()) total = rs.getInt(1);
            }
            return Map.of(
                "estado",             "CONECTADO",
                "servidorMirror",     mirrorUrl,
                "registrosEnMirror",  total,
                "timestamp",          LocalDateTime.now().format(TS_FMT)
            );
        } catch (Exception e) {
            return Map.of(
                "estado",         "DESCONECTADO",
                "servidorMirror", mirrorUrl,
                "mensaje",        e.getMessage(),
                "timestamp",      LocalDateTime.now().format(TS_FMT)
            );
        }
    }

    // ─── Métodos privados de replicación ─────────────────────────────────

    private List<Object[]> leerVentasPrincipales() throws SQLException {
        List<Object[]> ventas = new ArrayList<>();
        try (Connection c  = DriverManager.getConnection(mainUrl, mainUser, mainPass);
             Statement  st = c.createStatement();
             ResultSet  rs = st.executeQuery(
                 "SELECT id_venta, id_vendedor, fecha, monto_total, estado FROM ventas WHERE estado != 'X'")) {
            while (rs.next()) {
                ventas.add(new Object[]{
                    rs.getString("id_venta"),
                    rs.getString("id_vendedor"),
                    rs.getString("fecha"),
                    rs.getDouble("monto_total"),
                    rs.getString("estado")
                });
            }
        }
        return ventas;
    }

    private int escribirEnMirror(List<Object[]> ventas) throws SQLException {
        if (ventas.isEmpty()) return 0;

        try (Connection c = DriverManager.getConnection(mirrorUrl, mirrorUser, mirrorPass)) {
            // Crear tabla espejo si no existe
            try (Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS ventas (
                        id_venta    VARCHAR(20) PRIMARY KEY,
                        id_vendedor VARCHAR(20) NOT NULL,
                        fecha       VARCHAR(20) NOT NULL,
                        monto_total DOUBLE      NOT NULL,
                        estado      CHAR(1)     NOT NULL,
                        recibido_en TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }

            String sql = """
                INSERT INTO ventas (id_venta, id_vendedor, fecha, monto_total, estado)
                VALUES (?, ?, ?, ?, ?)
                AS nuevos
                ON DUPLICATE KEY UPDATE
                    monto_total = nuevos.monto_total,
                    estado      = nuevos.estado
                """;

            int copiadas = 0;
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Object[] v : ventas) {
                    ps.setString(1, (String) v[0]);
                    ps.setString(2, (String) v[1]);
                    ps.setString(3, (String) v[2]);
                    ps.setDouble(4, (Double) v[3]);
                    ps.setString(5, (String) v[4]);
                    ps.addBatch();
                }
                int[] resultados = ps.executeBatch();
                c.commit();
                for (int r : resultados) if (r > 0) copiadas++;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            return copiadas;
        }
    }
}
