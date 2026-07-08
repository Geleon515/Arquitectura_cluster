package arqui.grupo5.web.controller;

import arqui.grupo5.web.model.Venta;
import arqui.grupo5.web.repository.VentaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador REST que actúa como proxy para demostrar
 * las operaciones del Web Service SOAP desde el frontend.
 *
 * El frontend envía JSON → este controller ejecuta la misma
 * lógica que el VentaEndpoint SOAP → devuelve JSON al frontend.
 */
@RestController
@RequestMapping("/api/soap")
@CrossOrigin(origins = "*")
public class SoapProxyController {

    private static final DateTimeFormatter ID_FMT    = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VentaRepository repo;

    public SoapProxyController(VentaRepository repo) {
        this.repo = repo;
    }

    /**
     * Simula la operación SOAP: getVenta
     * POST /api/soap/getVenta
     */
    @PostMapping("/getVenta")
    public ResponseEntity<Map<String, Object>> getVenta(@RequestBody Map<String, String> body) {
        String idVenta = body.get("idVenta");
        if (idVenta == null || idVenta.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "El campo idVenta es requerido.",
                "exito", false
            ));
        }

        Venta v = repo.buscarPorId(idVenta.trim());
        if (v != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exito", true);
            result.put("mensaje", "Venta encontrada.");
            result.put("venta", ventaToMap(v));
            result.put("xmlRequest", buildGetVentaXml(idVenta.trim()));
            result.put("xmlResponse", buildGetVentaResponseXml(v));
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.ok(Map.of(
                "exito", false,
                "mensaje", "No se encontró la venta con ID: " + idVenta,
                "xmlRequest", buildGetVentaXml(idVenta.trim()),
                "xmlResponse", buildNotFoundResponseXml(idVenta.trim())
            ));
        }
    }

    /**
     * Simula la operación SOAP: getAllVentas
     * POST /api/soap/getAllVentas
     */
    @PostMapping("/getAllVentas")
    public ResponseEntity<Map<String, Object>> getAllVentas() {
        List<Venta> ventas = repo.listarTodas();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Venta v : ventas) {
            lista.add(ventaToMap(v));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exito", true);
        result.put("mensaje", "Se encontraron " + ventas.size() + " ventas.");
        result.put("total", ventas.size());
        result.put("ventas", lista);
        result.put("xmlRequest", buildGetAllVentasXml());
        result.put("xmlResponse", buildGetAllVentasResponseXml(ventas));
        return ResponseEntity.ok(result);
    }

    /**
     * Simula la operación SOAP: registrarVenta
     * POST /api/soap/registrarVenta
     */
    @PostMapping("/registrarVenta")
    public ResponseEntity<Map<String, Object>> registrarVenta(@RequestBody Map<String, Object> body) {
        try {
            String idVendedor = (String) body.get("idVendedor");
            String idProducto = body.get("idProducto") != null
                                ? (String) body.get("idProducto") : "P001";
            double montoTotal = ((Number) body.get("montoTotal")).doubleValue();

            if (idVendedor == null || idVendedor.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "exito", false,
                    "mensaje", "El campo idVendedor es requerido."
                ));
            }
            if (montoTotal <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "exito", false,
                    "mensaje", "El montoTotal debe ser mayor a 0."
                ));
            }

            String idVenta = "VTA-" + LocalDateTime.now().format(ID_FMT);
            String fecha   = LocalDateTime.now().format(FECHA_FMT);

            Venta nueva = new Venta(
                idVenta,
                idVendedor.trim().toUpperCase(),
                idProducto.trim().toUpperCase(),
                fecha,
                montoTotal,
                "P"
            );
            repo.insertar(nueva);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exito", true);
            result.put("mensaje", "Venta registrada exitosamente vía SOAP.");
            result.put("idVenta", idVenta);
            result.put("xmlRequest", buildRegistrarVentaXml(idVendedor.trim().toUpperCase(), idProducto.trim().toUpperCase(), montoTotal));
            result.put("xmlResponse", buildRegistrarVentaResponseXml(idVenta));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "exito", false,
                "mensaje", "Error: " + e.getMessage()
            ));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private Map<String, Object> ventaToMap(Venta v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idVenta", v.getIdVenta());
        m.put("idVendedor", v.getIdVendedor());
        m.put("idProducto", v.getIdProducto());
        m.put("fecha", v.getFecha());
        m.put("montoTotal", v.getMontoTotal());
        m.put("estado", v.getEstado());
        return m;
    }

    // ─── XML Builders (para mostrar en el frontend) ─────────────────

    private String buildGetVentaXml(String id) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:getVentaRequest>\n"
             + "         <ven:idVenta>" + id + "</ven:idVenta>\n"
             + "      </ven:getVentaRequest>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }

    private String buildGetVentaResponseXml(Venta v) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:getVentaResponse>\n"
             + "         <ven:venta>\n"
             + "            <ven:idVenta>" + v.getIdVenta() + "</ven:idVenta>\n"
             + "            <ven:idVendedor>" + v.getIdVendedor() + "</ven:idVendedor>\n"
             + "            <ven:idProducto>" + v.getIdProducto() + "</ven:idProducto>\n"
             + "            <ven:fecha>" + v.getFecha() + "</ven:fecha>\n"
             + "            <ven:montoTotal>" + v.getMontoTotal() + "</ven:montoTotal>\n"
             + "            <ven:estado>" + v.getEstado() + "</ven:estado>\n"
             + "         </ven:venta>\n"
             + "         <ven:mensaje>Venta encontrada.</ven:mensaje>\n"
             + "      </ven:getVentaResponse>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }

    private String buildNotFoundResponseXml(String id) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:getVentaResponse>\n"
             + "         <ven:mensaje>No se encontró la venta con ID: " + id + "</ven:mensaje>\n"
             + "      </ven:getVentaResponse>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }

    private String buildGetAllVentasXml() {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:getAllVentasRequest/>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }

    private String buildGetAllVentasResponseXml(List<Venta> ventas) {
        StringBuilder sb = new StringBuilder();
        sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n")
          .append("                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n")
          .append("   <soapenv:Body>\n")
          .append("      <ven:getAllVentasResponse>\n");
        for (Venta v : ventas) {
            sb.append("         <ven:ventas>\n")
              .append("            <ven:idVenta>").append(v.getIdVenta()).append("</ven:idVenta>\n")
              .append("            <ven:idVendedor>").append(v.getIdVendedor()).append("</ven:idVendedor>\n")
              .append("            <ven:idProducto>").append(v.getIdProducto()).append("</ven:idProducto>\n")
              .append("            <ven:fecha>").append(v.getFecha()).append("</ven:fecha>\n")
              .append("            <ven:montoTotal>").append(v.getMontoTotal()).append("</ven:montoTotal>\n")
              .append("            <ven:estado>").append(v.getEstado()).append("</ven:estado>\n")
              .append("         </ven:ventas>\n");
        }
        sb.append("         <ven:total>").append(ventas.size()).append("</ven:total>\n")
          .append("      </ven:getAllVentasResponse>\n")
          .append("   </soapenv:Body>\n")
          .append("</soapenv:Envelope>");
        return sb.toString();
    }

    private String buildRegistrarVentaXml(String vendedor, String producto, double monto) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:registrarVentaRequest>\n"
             + "         <ven:idVendedor>" + vendedor + "</ven:idVendedor>\n"
             + "         <ven:idProducto>" + producto + "</ven:idProducto>\n"
             + "         <ven:montoTotal>" + monto + "</ven:montoTotal>\n"
             + "      </ven:registrarVentaRequest>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }

    private String buildRegistrarVentaResponseXml(String idVenta) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
             + "                  xmlns:ven=\"http://arqui.grupo5.web/soap/ventas\">\n"
             + "   <soapenv:Body>\n"
             + "      <ven:registrarVentaResponse>\n"
             + "         <ven:idVenta>" + idVenta + "</ven:idVenta>\n"
             + "         <ven:mensaje>Venta registrada exitosamente vía SOAP.</ven:mensaje>\n"
             + "      </ven:registrarVentaResponse>\n"
             + "   </soapenv:Body>\n"
             + "</soapenv:Envelope>";
    }
}
