package arqui.grupo5.dw.db;

import arqui.grupo5.dw.model.VentaOperacional;
import java.sql.*;
import java.util.*;

public class DWRepository implements AutoCloseable {

    private final Connection conn;
    private record AggKey(String idVendedor, String idFecha) {}

    public DWRepository(String url, String user, String password) throws SQLException {
        conn = DriverManager.getConnection(url, user, password);
    }

    public int cargarDW(List<VentaOperacional> ventas) throws SQLException {
        Map<AggKey, double[]> agg = new LinkedHashMap<>();

        // 1. Agrupar la data en memoria
        for (VentaOperacional v : ventas) {
            String idFecha = parseFecha(v.getFecha());
            double[] arr = agg.computeIfAbsent(new AggKey(v.getIdVendedor(), idFecha), k -> new double[2]);
            arr[0] += v.getMontoTotal(); // Suma de montos
            arr[1]++;                    // Conteo de cantidad
        }

        // 2. Delegar la inserción al Procedimiento Almacenado mediante CALL
        String sqlCall = "{CALL SP_CargarRegistroDW(?, ?, ?, ?, ?, ?, ?)}";

        conn.setAutoCommit(false);
        try (CallableStatement cs = conn.prepareCall(sqlCall)) {
            for (Map.Entry<AggKey, double[]> entry : agg.entrySet()) {
                String idF = entry.getKey().idFecha();
                int mes = parseMes(idF);

                cs.setString(1, entry.getKey().idVendedor());
                cs.setString(2, idF);
                cs.setInt(3, mes);
                cs.setInt(4, parseAnio(idF));
                cs.setInt(5, calcTrimestre(mes));
                cs.setDouble(6, entry.getValue()[0]);
                cs.setInt(7, (int) entry.getValue()[1]);

                cs.addBatch();
            }
            cs.executeBatch();
            conn.commit();
            return agg.size();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private String parseFecha(String fecha) { return (fecha == null || fecha.length() < 7) ? "0000-00" : fecha.substring(0, 7); }
    private int parseMes(String idFecha) { try { return Integer.parseInt(idFecha.substring(5, 7)); } catch (Exception e) { return 0; } }
    private int parseAnio(String idFecha) { try { return Integer.parseInt(idFecha.substring(0, 4)); } catch (Exception e) { return 0; } }
    private int calcTrimestre(int mes) { return (mes - 1) / 3 + 1; }

    @Override
    public void close() throws SQLException { if (conn != null && !conn.isClosed()) conn.close(); }
}