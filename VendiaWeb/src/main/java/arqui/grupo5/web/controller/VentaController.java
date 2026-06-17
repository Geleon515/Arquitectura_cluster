package arqui.grupo5.web.controller;

import arqui.grupo5.web.model.Venta;
import arqui.grupo5.web.repository.VentaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private static final DateTimeFormatter ID_FMT   = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VentaRepository repo;

    public VentaController(VentaRepository repo) {
        this.repo = repo;
    }

    // GET /api/ventas  — lista todas
    @GetMapping
    public List<Venta> listarTodas() {
        return repo.listarTodas();
    }

    // GET /api/ventas/estadisticas
    @GetMapping("/estadisticas")
    public Map<String, Integer> estadisticas() {
        return Map.of(
            "total",      repo.contarTotales(),
            "pendientes", repo.contarPendientes()
        );
    }

    // GET /api/ventas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Venta> buscar(@PathVariable String id) {
        Venta v = repo.buscarPorId(id);
        return v != null ? ResponseEntity.ok(v) : ResponseEntity.notFound().build();
    }

    // POST /api/ventas/registrar
    @PostMapping("/registrar")
    public ResponseEntity<Map<String, String>> registrar(@RequestBody Map<String, Object> body) {
        try {
            String idVendedor = (String) body.get("idVendedor");
            String idProducto = body.get("idProducto") != null
                                ? (String) body.get("idProducto") : "P001";
            double monto      = ((Number) body.get("monto")).doubleValue();

            if (idVendedor == null || idVendedor.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "ID vendedor requerido."));
            if (monto <= 0 || monto > 999_999.99)
                return ResponseEntity.badRequest().body(Map.of("error", "Monto inválido."));

            String idVenta = "VTA-" + LocalDateTime.now().format(ID_FMT);
            String fecha   = LocalDateTime.now().format(FECHA_FMT);

            repo.insertar(new Venta(idVenta, idVendedor.trim().toUpperCase(),
                                    idProducto.trim().toUpperCase(), fecha, monto, "P"));

            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta registrada exitosamente.",
                "idVenta", idVenta
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/ventas/{id}/monto
    @PutMapping("/{id}/monto")
    public ResponseEntity<Map<String, String>> actualizarMonto(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            double nuevoMonto = ((Number) body.get("monto")).doubleValue();
            if (nuevoMonto <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "Monto inválido."));

            int filas = repo.actualizarMonto(id, nuevoMonto);
            if (filas == 0)
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Venta no encontrada o ya fue enviada al servidor (estado != P)."));

            return ResponseEntity.ok(Map.of("mensaje", "Monto actualizado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/ventas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String id) {
        try {
            int filas = repo.eliminar(id);
            if (filas == 0)
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Venta no encontrada o ya fue enviada al servidor (estado != P)."));

            return ResponseEntity.ok(Map.of("mensaje", "Venta eliminada lógicamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
