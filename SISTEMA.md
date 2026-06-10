# Sistema Vendia — Documentación Técnica
## LogiMarket Perú S.A. | Arquitectura Cliente/Servidor

---

## Visión general

El sistema adopta una **arquitectura Cliente/Servidor de 4 capas**. Los ejecutables
residen en el Servidor de Aplicaciones; los clientes los ejecutan desde accesos directos
de red. Los datos operacionales se gestionan en el Servidor de BD y los datos analíticos
en el Servidor de DataWarehouse.

```
CLIENTES                  SERVIDOR DE APPS          SERVIDOR DE BD       SERVIDOR DE DW
─────────                 ────────────────          ──────────────       ──────────────
[Cajero 1]                VendiaApp\                MySQL                MySQL
 └─ acceso directo ──────→ VendiaApp.exe             logimarket           logimarket_dw
[Cajero 2]                VendiaSender\              tabla: ventas        dim_vendedor
 └─ acceso directo ──────→ VendiaSender.exe               ↑              dim_fecha
[Admin]                   GenerarDatawareHouse\           │              fact_ventas
 └─ acceso directo ──────→ GenerarDatawareHouse.exe        │                  ↑
                                                    VendiaUpdater         GenerarDW
                               DATOS\ ────────────→ (daemon)    ──ETL──→ (en demanda)
                          (carpeta compartida)
```

---

## Componentes

### 1. VendiaApp — Cajero
Aplicación de escritorio (JavaFX) que el cajero usa durante su turno.
Reside en el Servidor de Aplicaciones; el cliente la ejecuta desde un acceso directo.

- Registra, busca, modifica y elimina ventas
- Persiste todo en archivos binarios locales del cliente (`ventas.dat`, `ventas.idx`)
- Funciona **sin conexión al servidor** durante el turno
- Estado inicial de cada venta: `P` (Pendiente)

### 2. VendiaSender — Enviador
Aplicación de escritorio (JavaFX) que se ejecuta **al cerrar el turno**.
También reside en el Servidor de Aplicaciones.

- Lee las ventas con estado `P` de `ventas.dat`
- Las deposita en la carpeta compartida `DATOS\` como `ventas_<timestamp>.dat`
- Hace polling esperando el `.ack` del servidor (timeout 30 s)
- Si el `.ack` dice `OK`, marca cada venta como `E` (Enviada) en `ventas.dat`

### 3. VendiaUpdater — Receptor (daemon en Servidor de BD)
Aplicación de consola que corre permanentemente en el Servidor de BD.

- Vigila `DATOS\` cada N milisegundos (configurable)
- Por cada `.dat` sin `.ack`, lee los registros binarios e inserta en MySQL
- Escribe `.ack` con el mismo nombre base para confirmar al cliente
- Crea la tabla `ventas` automáticamente si no existe

### 4. GenerarDatawareHouse — ETL (en Servidor de Aplicaciones)
Aplicación de escritorio (JavaFX) ejecutada por el administrador cuando necesita
actualizar el DataWarehouse para análisis.

- Lee todas las ventas de `logimarket.ventas` (Servidor de BD)
- Crea el schema `logimarket_dw` automáticamente si no existe (Servidor de DW)
- Ejecuta el proceso ETL: transforma y carga datos en un modelo dimensional (star schema)
- Idempotente: ejecutarlo múltiples veces produce el mismo resultado

---

## Flujo completo paso a paso

```
Cajero registra venta en VendiaApp (desde acceso directo al Servidor de Apps)
        │
        ▼
Se escribe en ventas.dat local del cliente (estado = 'P')
        │
        │   (durante el turno, muchas ventas)
        │
        ▼
Al cerrar turno: cajero abre VendiaSender (desde acceso directo)
        │
        ▼
VendiaSender lee ventas.dat → filtra estado = 'P'
Escribe DATOS\ventas_<ts>.dat en carpeta compartida
        │
        ▼
VendiaUpdater (loop en Servidor de BD) detecta el .dat
        ├─ Lee cada registro de 130 bytes
        ├─ INSERT batch en MySQL logimarket.ventas (INSERT IGNORE)
        └─ Escribe DATOS\ventas_<ts>.ack con "OK <N>"
        │
        ▼
VendiaSender recibe el .ack → marca ventas como 'E' en ventas.dat local
        │
        │   (cuando el administrador lo requiera)
        │
        ▼
Admin abre GenerarDatawareHouse (desde acceso directo)
        ├─ Lee logimarket.ventas
        ├─ Agrupa por vendedor + mes
        └─ Carga logimarket_dw: dim_vendedor, dim_fecha, fact_ventas
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

**Estados posibles:**
| Código | Significado |
|--------|-------------|
| `P`    | Pendiente (no enviada aún) |
| `E`    | Enviada y confirmada por el servidor |
| `X`    | Eliminada lógicamente |

### ventas.idx — índice

Cada entrada ocupa **48 bytes**: `idVenta (40 bytes)` + `posición en .dat (8 bytes)`.
Se carga en memoria como `TreeMap<String, Long>` para búsquedas O(log N).

---

## Protocolo de carpeta compartida

| Extensión | Origen | Contenido |
|-----------|--------|-----------|
| `ventas_<ts>.dat` | VendiaSender | Registros binarios de las ventas (N × 130 bytes) |
| `ventas_<ts>.ack` | VendiaUpdater | `OK <N>` o `ERR <mensaje>` |

### Idempotencia

El Updater ignora `.dat` que ya tienen su `.ack`. El `INSERT IGNORE` de MySQL evita
duplicados por clave primaria. El ETL del DW usa `ON DUPLICATE KEY UPDATE`, por lo que
ejecutarlo varias veces produce siempre el mismo resultado.

---

## Esquema MySQL — Servidor de BD (`logimarket`)

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

## Esquema MySQL — Servidor de DW (`logimarket_dw`)

Star schema creado automáticamente por `GenerarDatawareHouse`:

```sql
CREATE TABLE IF NOT EXISTS dim_vendedor (
    id_vendedor VARCHAR(20) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS dim_fecha (
    id_fecha  VARCHAR(7) PRIMARY KEY,   -- ej: "2026-06"
    mes       INT NOT NULL,
    anio      INT NOT NULL,
    trimestre INT NOT NULL              -- 1..4
);

CREATE TABLE IF NOT EXISTS fact_ventas (
    id_vendedor VARCHAR(20) NOT NULL,
    id_fecha    VARCHAR(7)  NOT NULL,
    monto_total DOUBLE      NOT NULL DEFAULT 0,
    cantidad    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id_vendedor, id_fecha),
    FOREIGN KEY (id_vendedor) REFERENCES dim_vendedor(id_vendedor),
    FOREIGN KEY (id_fecha)    REFERENCES dim_fecha(id_fecha)
);
```
