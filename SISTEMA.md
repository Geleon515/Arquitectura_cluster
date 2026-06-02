# Sistema Vendia — Documentación General
## LogiMarket Perú S.A. | Arquitectura Unitaria con Servidor Central

---

## Visión general

El sistema está compuesto por **tres aplicaciones independientes** que trabajan en conjunto
para registrar ventas en cada sede y consolidarlas en un servidor central.

```
┌─────────────────────────────────────────┐
│           SEDE (cada cajero)            │
│                                         │
│  1. VendiaApp  →  ventas.dat / .idx     │
│     Registra ventas localmente          │
│                                         │
│  2. VendiaSender  (al cerrar turno)     │
│     Copia ventas pendientes a DATOS\    │
└──────────────────┬──────────────────────┘
                   │  Carpeta compartida DATOS\
                   │  ventas_<ts>.dat  →
                   │  ventas_<ts>.ack  ←
                   ▼
┌─────────────────────────────────────────┐
│           SERVIDOR CENTRAL              │
│                                         │
│  3. VendiaUpdater  (daemon)             │
│     Vigila DATOS\ → INSERT en MySQL     │
│     → escribe .ack                      │
└─────────────────────────────────────────┘
```

---

## Componentes

### 1. VendiaApp — Cajero
Aplicación de escritorio (JavaFX) que el cajero usa durante su turno.

- Registra, busca, modifica y elimina ventas
- Persiste todo en archivos binarios locales (`ventas.dat`, `ventas.idx`)
- Funciona **sin conexión a internet ni servidor**
- Estado inicial de cada venta: `P` (Pendiente)

### 2. VendiaSender — Enviador
Aplicación de escritorio (JavaFX) que se ejecuta **al cerrar el turno**.

- Lee las ventas con estado `P` de `ventas.dat`
- Las escribe a un archivo `ventas_<timestamp>.dat` en la carpeta compartida `DATOS\`
- Se queda haciendo polling esperando que aparezca `ventas_<timestamp>.ack`
  (timeout 30 s)
- Si el `.ack` dice `OK`, marca cada venta como `E` (Enviada) en `ventas.dat`

### 3. VendiaUpdater — Receptor (daemon)
Aplicación de consola que corre permanentemente en el servidor central.

- Vigila la carpeta compartida `DATOS\` cada N milisegundos (polling configurable)
- Por cada archivo `.dat` que no tenga su `.ack` asociado, lee los registros
  binarios y los inserta en MySQL usando **batch insert** transaccional
- Escribe un archivo `.ack` con el mismo nombre base para confirmar al cliente
- Crea la tabla `ventas` automáticamente si no existe

---

## Flujo completo paso a paso

```
Cajero registra una venta en VendiaApp
        │
        ▼
Se escribe en ventas.dat (estado = 'P')
Se actualiza ventas.idx con el nuevo offset
        │
        │   (durante el turno puede haber muchas ventas)
        │
        ▼
Al cerrar turno: cajero abre VendiaSender
        │
        ▼
VendiaSender lee ventas.dat → filtra estado = 'P'
Genera un timestamp y escribe DATOS\ventas_<ts>.dat
con los N registros de 130 bytes c/u
        │
        ▼
VendiaSender se queda esperando DATOS\ventas_<ts>.ack
(polling cada 500 ms, timeout 30 s)
        │
        ▼
VendiaUpdater (loop infinito cada 2 s) detecta el .dat nuevo
        │
        ├─ Verifica que no exista ya su .ack (idempotencia)
        ├─ Lee cada registro de 130 bytes
        ├─ INSERT batch en MySQL (INSERT IGNORE si ya existe)
        ├─ Escribe DATOS\ventas_<ts>.ack con "OK <N>"
        │  (o "ERR <mensaje>" si fallo algo)
        │
        ▼
VendiaSender ve aparecer el .ack y lo lee
        │
        ▼
Si .ack = "OK ..." → marca las ventas como estado = 'E' en ventas.dat
Si .ack = "ERR ..." → registra el error, no marca nada
```

---

## Estructura de archivos binarios

### ventas.dat — datos

Cada venta ocupa exactamente **130 bytes**:

| Campo       | Tipo     | Bytes |
|-------------|----------|-------|
| id_venta    | char[20] | 40    |
| id_vendedor | char[20] | 40    |
| fecha       | char[20] | 40    |
| monto_total | double   | 8     |
| estado      | char     | 2     |
| **Total**   |          | **130** |

Los strings se rellenan con espacios hasta completar el tamaño fijo.
Esto permite saltar directamente a cualquier registro con `seek(offset)`.

**Estados posibles:**
| Código | Significado |
|--------|-------------|
| `P`    | Pendiente (no enviada aún) |
| `E`    | Enviada y confirmada por el servidor |
| `X`    | Eliminada lógicamente |

### ventas.idx — índice

Cada entrada ocupa **48 bytes**: `idVenta (40 bytes)` + `posición en .dat (8 bytes)`.

Se carga completo en memoria al iniciar VendiaApp como un `TreeMap<String, Long>`,
permitiendo búsquedas en O(log N) sin recorrer el archivo de datos.

---

## Protocolo de carpeta compartida

La comunicación es por archivos depositados en la carpeta `DATOS\`, que actúa
como un buzón bidireccional entre Sender y Updater. Cada envío produce un par
de archivos relacionados por el mismo nombre base con distintas extensiones.

### Nombres de archivo

| Extensión | Origen | Destino | Contenido |
|-----------|--------|---------|-----------|
| `ventas_<timestamp>.dat` | VendiaSender | VendiaUpdater | Registros binarios de las ventas |
| `ventas_<timestamp>.ack` | VendiaUpdater | VendiaSender | Texto de confirmación o error |

El `<timestamp>` tiene formato `yyyyMMdd_HHmmss` y garantiza que distintos
envíos no se pisen aunque caigan en la misma carpeta.

### Formato del `.dat`

Concatenación de N registros de 130 bytes, **mismo formato** que `ventas.dat`
local del cliente (ver sección "Estructura de archivos binarios"). No hay
cabecera con la cantidad: el Updater calcula `N = length / 130`.

### Formato del `.ack`

Archivo de texto plano con una sola línea:

| Resultado | Formato | Significado |
|-----------|---------|-------------|
| Éxito  | `OK <N>`        | Las N ventas fueron insertadas en MySQL |
| Error  | `ERR <mensaje>` | Algo falló al procesar; el sender no marca como enviadas |

### Secuencia

```
VendiaSender                   Carpeta DATOS\                  VendiaUpdater
────────────                   ──────────────                  ─────────────
write ventas_<ts>.dat   →      ventas_<ts>.dat
                                                               (polling cada 2 s)
                                                               detecta .dat sin .ack
                                                               lee N = length/130
                                                               INSERT batch en MySQL
                                              ventas_<ts>.ack  ← write "OK <N>"
polling cada 500 ms     ←      ventas_<ts>.ack
lee el .ack
marca ventas como 'E'
```

### Idempotencia

Si el Updater se reinicia, al volver a vigilar la carpeta encuentra los `.dat`
viejos pero también encuentra sus `.ack`, así que los ignora — no reprocesa.
Si el Sender se reenvía por error, el `INSERT IGNORE` de MySQL evita duplicados
porque `id_venta` es clave primaria.

---

## Esquema MySQL

La tabla se crea automáticamente al arrancar VendiaUpdater:

```sql
CREATE TABLE IF NOT EXISTS ventas (
    id_venta     VARCHAR(20) PRIMARY KEY,
    id_vendedor  VARCHAR(20) NOT NULL,
    fecha        VARCHAR(20) NOT NULL,
    monto_total  DOUBLE      NOT NULL,
    estado       CHAR(1)     NOT NULL,
    recibido_en  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
```

Solo se requiere que la **base de datos exista previamente**