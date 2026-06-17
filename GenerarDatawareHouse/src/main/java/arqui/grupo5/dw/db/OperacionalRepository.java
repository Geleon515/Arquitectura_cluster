package arqui.grupo5.dw.db;

import arqui.grupo5.dw.model.VentaOperacional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OperacionalRepository {

    private final Connection conn;

    public OperacionalRepository(Connection conn) {
        this.conn = conn;
    }

    public List<VentaOperacional> leerVentasEnviadas() throws SQLException {
        String sql = "SELECT id_venta, id_vendedor, id_producto, fecha, monto_total FROM ventas WHERE estado != 'X'";
        List<VentaOperacional> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new VentaOperacional(
                    rs.getString("id_venta"),
                    rs.getString("id_vendedor"),
                    rs.getString("id_producto") != null ? rs.getString("id_producto").trim() : "P000",
                    rs.getString("fecha").trim(),
                    rs.getDouble("monto_total")
                ));
            }
        }
        return lista;
    }
}
