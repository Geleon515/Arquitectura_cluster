package arqui.grupo5.web.soap;

import arqui.grupo5.web.model.Venta;
import arqui.grupo5.web.repository.VentaRepository;
import arqui.grupo5.web.soap.types.*;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Endpoint
public class VentaEndpoint {

    private static final String NAMESPACE_URI = "http://arqui.grupo5.web/soap/ventas";

    private static final DateTimeFormatter ID_FMT    = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VentaRepository repo;

    public VentaEndpoint(VentaRepository repo) {
        this.repo = repo;
    }

    // ================================================================
    //  Operación 1: getVenta  —  Buscar venta por ID
    // ================================================================
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getVentaRequest")
    @ResponsePayload
    public GetVentaResponse getVenta(@RequestPayload GetVentaRequest request) {
        GetVentaResponse response = new GetVentaResponse();

        Venta v = repo.buscarPorId(request.getIdVenta());
        if (v != null) {
            response.setVenta(mapToInfo(v));
            response.setMensaje("Venta encontrada.");
        } else {
            response.setMensaje("No se encontró la venta con ID: " + request.getIdVenta());
        }

        return response;
    }

    // ================================================================
    //  Operación 2: getAllVentas  —  Listar todas las ventas
    // ================================================================
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAllVentasRequest")
    @ResponsePayload
    public GetAllVentasResponse getAllVentas(@RequestPayload GetAllVentasRequest request) {
        GetAllVentasResponse response = new GetAllVentasResponse();

        List<Venta> ventas = repo.listarTodas();
        for (Venta v : ventas) {
            response.getVentas().add(mapToInfo(v));
        }
        response.setTotal(ventas.size());

        return response;
    }

    // ================================================================
    //  Operación 3: registrarVenta  —  Crear nueva venta
    // ================================================================
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "registrarVentaRequest")
    @ResponsePayload
    public RegistrarVentaResponse registrarVenta(@RequestPayload RegistrarVentaRequest request) {
        RegistrarVentaResponse response = new RegistrarVentaResponse();

        String idVenta = "VTA-" + LocalDateTime.now().format(ID_FMT);
        String fecha   = LocalDateTime.now().format(FECHA_FMT);

        Venta nueva = new Venta(
            idVenta,
            request.getIdVendedor().trim().toUpperCase(),
            request.getIdProducto().trim().toUpperCase(),
            fecha,
            request.getMontoTotal(),
            "P"
        );

        repo.insertar(nueva);

        response.setIdVenta(idVenta);
        response.setMensaje("Venta registrada exitosamente vía SOAP.");

        return response;
    }

    // ================================================================
    //  Helper: Convierte modelo Venta → tipo JAXB VentaInfo
    // ================================================================
    private VentaInfo mapToInfo(Venta v) {
        VentaInfo info = new VentaInfo();
        info.setIdVenta(v.getIdVenta());
        info.setIdVendedor(v.getIdVendedor());
        info.setIdProducto(v.getIdProducto());
        info.setFecha(v.getFecha());
        info.setMontoTotal(v.getMontoTotal());
        info.setEstado(v.getEstado());
        return info;
    }
}
