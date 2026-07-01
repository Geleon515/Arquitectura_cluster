# Sistema Vendia — Documentación Técnica
## LogiMarket Perú S.A. | Arquitectura MVC N-Capas

---

## Visión general

El sistema adopta una **arquitectura MVC de N Capas** distribuida en múltiples nodos.
Los cajeros registran ventas offline, las envían al servidor de BD, desde donde
se replican a un Mirror, se procesan en un DataWarehouse y se visualizan como cubo OLAP.

```
[PC Cajero]          [Servidor de Aplicaciones]    [Servidor BD]      [Servidor DW]
────────────         ──────────────────────────    ─────────────      ─────────────
VendiaApp            VendiaWeb (Spring Boot)        MySQL              MySQL
(ventas.dat)    →    REST API + React UI        →   logimarket    →    logimarket_dw
                                                    (OLTP)             (OLAP)
VendiaSender    →    carpeta DATOS\             →   logimarket_mirror
(envío .dat)         VendiaUpdater (daemon)          (Mirror)
                     + plugins .jar
```

---

## Componentes

### 1. VendiaApp — Cajero
Aplicación de escritorio (JavaFX) que el cajero usa durante su turno.

- Registra, busca, modifica y elimina ventas
- Persiste todo en archivos binarios locales (`ventas.dat`, `ventas.idx`)
- Funciona **sin conexión al servidor** durante el turno
- Estado inicial de cada venta: `P` (Pendiente)

### 2. VendiaSender — Enviador
Aplicación de escritorio (JavaFX) ejecutada al cerrar el turno.

- Lee las ventas con estado `P` de `ventas.dat`
- Las deposita en la carpeta compartida `DATOS\` como `ventas_<timestamp>.dat`
- Hace polling esperando el `.ack` del servidor
- Si el `.ack` dice `OK`, marca cada venta como `E` (Enviada)

### 3. VendiaUpdater — Receptor (daemon en Servidor de BD)
Aplicación de consola que corre permanentemente. Implementa **Arquitectura Microkernel**
con un sistema de plugins cargados dinámicamente desde la carpeta `plugins/`.

- Vigila `DATOS\` cada N milisegundos (configurable)
- Por cada `.dat` sin `.ack`, inserta en MySQL con batch insert
- Escribe `.ack` para confirmar al cliente
- Después de cada inserción, ejecuta todos los plugins en cadena

### 4. VendiaWeb — Servidor de Aplicaciones Web
Servidor Spring Boot con API REST y frontend React.

- Gestión de ventas (CRUD) vía interfaz web en `http://localhost:8080`
- Sincronización de datos de `logimarket` → `logimarket_mirror` (Capa Mirror)
- Endpoints REST para todas las operaciones

### 5. GenerarDatawareHouse — ETL
Aplicación de escritorio (JavaFX) ejecutada por el administrador.

- Lee las ventas de `logimarket_mirror`
- Carga el star schema en `logimarket_dw`: `dim_vendedor`, `dim_fecha`, `fact_ventas`
- Idempotente: ejecutarlo múltiples veces produce el mismo resultado

### 6. CreateCrossTab — Generador de cubo OLAP
Aplicación de escritorio (JavaFX) que genera la tabla pivote trimestral.

- Lee `logimarket_dw` y genera `crosstab_ventas`
- Agrupa por vendedor × trimestre (Q1/Q2/Q3/Q4)
- Permite exportar a CSV

### 7. ViewCrossTab — Visualizador OLAP
Aplicación de escritorio (JavaFX) que muestra el cubo en 3 vistas:
- **Tabla pivot**: vendedor × Q1/Q2/Q3/Q4 con montos y cantidades
- **Gráfico de barras**: barras agrupadas por vendedor y trimestre
- **Gráfico de torta**: distribución porcentual por trimestre

---

## Flujo completo paso a paso

```
Cajero registra venta en VendiaApp (sin conexión)
        │
        ▼
Se escribe en ventas.dat local (estado = 'P')
        │
        │   (durante el turno, muchas ventas)
        │
        ▼
Al cerrar turno: cajero abre VendiaSender
        │
        ▼
VendiaSender lee ventas.dat → filtra estado = 'P'
Copia DATOS\ventas_<ts>.dat en carpeta compartida
        │
        ▼
VendiaUpdater detecta el .dat
        ├─ Lee cada registro de 170 bytes
        ├─ INSERT batch en MySQL logimarket (UPSERT)
        ├─ Ejecuta plugins: auditoria.jar, alerta-monto.jar
        └─ Escribe DATOS\ventas_<ts>.ack con "OK <N>"
        │
        ▼
VendiaSender recibe el .ack → marca ventas como 'E' en ventas.dat
        │
        ▼
Administrador sincroniza Mirror desde VendiaWeb
        │   POST /api/mirror/sincronizar
        ▼
logimarket → logimarket_mirror (datos replicados)
        │
        ▼
Admin ejecuta GenerarDatawareHouse (ETL)
        ├─ Lee logimarket_mirror
        └─ Carga logimarket_dw: dim_vendedor, dim_fecha, fact_ventas
        │
        ▼
Admin ejecuta CreateCrossTab
        └─ Genera crosstab_ventas (pivot trimestral)
        │
        ▼
Admin abre ViewCrossTab
        └─ Visualiza tabla pivot + gráfico de barras + gráfico de torta
```

---

## Estructura de archivos binarios

### ventas.dat — datos

Cada venta ocupa exactamente **170 bytes** (cada campo de texto se escribe con `writeChars`,
que usa 2 bytes por carácter en formato Java):

| Campo       | Chars | Bytes |
|-------------|-------|-------|
| id_venta    | 20    | 40    |
| id_vendedor | 20    | 40    |
| id_producto | 20    | 40    |
| fecha       | 20    | 40    |
| monto_total | —     | 8     |
| estado      | 1     | 2     |
| **Total**   |       | **170** |

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
| `ventas_<ts>.dat` | VendiaSender | Registros binarios de las ventas (N × 170 bytes) |
| `ventas_<ts>.ack` | VendiaUpdater | `OK <N>` o `ERR <mensaje>` |

### Idempotencia

El Updater ignora `.dat` que ya tienen su `.ack`. El `INSERT ... ON DUPLICATE KEY UPDATE`
de MySQL evita duplicados. El ETL del DW también usa `ON DUPLICATE KEY UPDATE`, por lo que
ejecutarlo varias veces produce siempre el mismo resultado.

---

## Esquema MySQL — `logimarket` (operacional)

```sql
CREATE TABLE IF NOT EXISTS ventas (
    id_venta     VARCHAR(20) PRIMARY KEY,
    id_vendedor  VARCHAR(20) NOT NULL,
    id_producto  VARCHAR(20) NOT NULL,
    fecha        VARCHAR(20) NOT NULL,
    monto_total  DOUBLE      NOT NULL,
    estado       CHAR(1)     NOT NULL,
    recibido_en  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
```

## Esquema MySQL — `logimarket_mirror` (Mirror)

Misma estructura que `logimarket`. Se sincroniza desde VendiaWeb con UPSERT para
mantener consistencia sin duplicar datos.

## Esquema MySQL — `logimarket_dw` (DataWarehouse)

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

CREATE TABLE IF NOT EXISTS crosstab_ventas (
    id_vendedor  VARCHAR(20) NOT NULL,
    anio         INT         NOT NULL,
    q1_monto     DOUBLE      DEFAULT 0,
    q2_monto     DOUBLE      DEFAULT 0,
    q3_monto     DOUBLE      DEFAULT 0,
    q4_monto     DOUBLE      DEFAULT 0,
    total_anual  DOUBLE      DEFAULT 0,
    q1_cantidad  INT         DEFAULT 0,
    q2_cantidad  INT         DEFAULT 0,
    q3_cantidad  INT         DEFAULT 0,
    q4_cantidad  INT         DEFAULT 0,
    PRIMARY KEY (id_vendedor, anio)
);
```
