# Acceso a archivos binarios en Vendia

## Con `RandomAccessFile`

### VendiaApp — `VentaStorage.java`
**Razón obligatoria.** Todas las operaciones necesitan saltar a posiciones arbitrarias:

| Operación | Por qué necesita seek |
|-----------|-----------------------|
| `buscarPorId` | Salta directo al offset guardado en el índice |
| `actualizar` | Lee el registro, vuelve al inicio y sobreescribe |
| `marcarComoEnviada` | Igual: lee, vuelve, sobreescribe |
| `eliminar` | Igual: lee, vuelve, sobreescribe con estado `'X'` |

Sin `seek()` ninguna de estas operaciones sería posible con un stream secuencial.

### VendiaSender — `VentaStorage.java`
**Razón obligatoria solo en `marcarEnviadas`.**

```java
long inicio = pos;
Venta v = leerRegistro(raf);  // avanza 130 bytes
if (ids.contains(v.getIdVenta())) {
    raf.seek(inicio);          // vuelve atrás → obligatorio
    escribirRegistro(raf, v);
}
```

`leerPendientes` en cambio es lectura secuencial pura — podría haberse usado un stream normal, pero se usó `RandomAccessFile` por consistencia con el resto de la clase.

---

## Sin `RandomAccessFile` — streams secuenciales

### VendiaSender — `FileSender.java`
Usa `DataOutputStream` + `BufferedOutputStream`.

**No hay razón técnica que obligue a usar RAF** porque `FileSender` solo crea un archivo nuevo y escribe todos los registros de corrido, de principio a fin, sin volver atrás nunca.

El `BufferedOutputStream` agrupa escrituras pequeñas para reducir operaciones de I/O, de ahí el `flush()` al cerrar.

> Ambas formas producen el mismo formato binario: `writeChar` y `writeDouble` escriben los bytes igual en RAF que en DataOutputStream, por eso VendiaUpdater puede leer sin problema lo que escribe FileSender.
