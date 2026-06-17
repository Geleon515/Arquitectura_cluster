package arqui.grupo5.dw.db;

import arqui.grupo5.dw.model.VentaOperacional;
import java.sql.CallableStatement;
import java.sql.Connection;
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
        List<VentaOperacional> lista = new ArrayList<>();
        // El requerimiento exige usar CALL
        String sql = "{CALL SP_ExtraerDatosDW()}";

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(new VentaOperacional(
                        rs.getString("id_venta"),
                        rs.getString("id_vendedor"),
                        rs.getString("fecha").trim(),
                        rs.getDouble("monto_total")
                ));
            }
        }
        return lista;
    }
}