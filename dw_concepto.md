# DataWarehouse — Conceptos clave

---

## ¿Por qué existe el DataWarehouse?

La BD operacional (`logimarket.ventas`) guarda una fila por cada venta individual.
Hacer consultas analíticas sobre ella (¿cuánto vendió cada vendedor por mes?) recorre
todas las filas cada vez, compitiendo con los cajeros que insertan en tiempo real.

El DataWarehouse pre-calcula las respuestas una sola vez. En lugar de 500,000 filas
de ventas individuales, almacena los totales ya agregados por vendedor y mes.
Las consultas del gerente son instantáneas porque recorren decenas de filas, no millones.

---

## OLTP vs OLAP

| | OLTP | OLAP |
|---|---|---|
| Significado | Online Transaction Processing | Online Analytical Processing |
| Ejemplo en el proyecto | `logimarket.ventas` | `logimarket_dw` |
| Operación principal | INSERT/UPDATE rápidos | SELECT con GROUP BY y JOINs |
| Quién lo usa | El cajero, todo el día | El gerente, cuando lo necesita |
| Filas típicas | Una por transacción | Una por combinación vendedor+mes |
| Optimizado para | Escritura concurrente | Lectura y agregación |

---

## ETL — Extract, Transform, Load

El proceso que conecta la BD operacional con el DW. Tu `GenerarDatawareHouse` es un ETL.

- **Extract**: leer de la fuente (`logimarket.ventas`)
- **Transform**: limpiar, agregar y calcular (agrupar por vendedor+mes, calcular trimestre)
- **Load**: escribir en el DW (`fact_ventas`, `dim_vendedor`, `dim_fecha`)

El ETL se ejecuta bajo demanda, no en tiempo real. Los datos del DW tienen la
antigüedad del último ETL ejecutado.

---

## Star Schema

El modelo de datos usado en el DW. La tabla de hechos está al centro y las dimensiones
alrededor — dibujado parece una estrella.

```
        dim_vendedor
             │
             │ id_vendedor
             │
dim_fecha ───┼─── fact_ventas (monto_total, cantidad)
             │
             │ id_fecha
```

### Tabla de hechos (fact table)

Contiene las métricas numéricas que se suman o cuentan. Siempre tiene claves foráneas
a las dimensiones. En el proyecto: `fact_ventas`.

- `monto_total` — métrica: cuánto dinero
- `cantidad` — métrica: cuántas transacciones
- `id_vendedor`, `id_fecha` — claves hacia las dimensiones

### Tablas de dimensión (dimension tables)

Describen el contexto: quién, cuándo, dónde, qué. En el proyecto: `dim_vendedor` y `dim_fecha`.

- `dim_vendedor` → responde *¿quién vendió?*
- `dim_fecha` → responde *¿cuándo? (mes, trimestre, año)*

### Métricas / medidas

Los números dentro de la fact table que analizas. `monto_total` y `cantidad` son las
métricas del proyecto.

### Granularidad (grain)

Qué representa una fila en la fact table. En el proyecto: *una fila = ventas totales
de un vendedor en un mes*. Es la decisión más importante al diseñar el DW — define
qué tan detallado o agregado están los datos.

---

## Las dimensiones dependen de las preguntas del negocio

No hay una separación única correcta. Cada vez que alguien pregunta *"¿cuánto... por X?"*,
ese "X" es una dimensión candidata.

| Pregunta | Dimensión necesaria |
|---|---|
| ¿Cuánto por vendedor? | `dim_vendedor` |
| ¿Cuánto por mes/trimestre? | `dim_fecha` |
| ¿Cuánto por sede? | `dim_sede` (no implementada) |
| ¿Cuánto por tipo de producto? | `dim_producto` (no implementada) |

El star schema es extensible: agregar una nueva dimensión no rompe las consultas existentes.

---

## Conceptos de carga

**Idempotencia** — ejecutar el ETL múltiples veces produce el mismo resultado.
Se logra con `INSERT IGNORE` en dimensiones y `ON DUPLICATE KEY UPDATE` en `fact_ventas`.

### Qué pasa cuando ejecutas el ETL una segunda vez con ventas nuevas

El ETL no lee el estado actual del DW — lo ignora completamente. Siempre recalcula
desde cero leyendo toda la BD operacional.

Ejemplo: primera ejecución, Juan tiene 2 ventas en enero:

```
BD operacional → agrega en memoria → carga DW
Juan 2026-01 150.00                  (Juan, 2026-01) → 350.00, 2
Juan 2026-01 200.00
```

Segunda ejecución, Juan tiene 1 venta nueva adicional:

```
BD operacional → agrega en memoria → sobreescribe DW
Juan 2026-01 150.00                  (Juan, 2026-01) → 400.00, 3  ✓
Juan 2026-01 200.00
Juan 2026-01  50.00  ← nueva
```

No duplica ni acumula mal porque el `ON DUPLICATE KEY UPDATE` reemplaza con el valor
recalculado, no suma encima del valor anterior:

```sql
-- MAL: sumaría encima del valor anterior → duplicaría datos
ON DUPLICATE KEY UPDATE monto_total = monto_total + VALUES(monto_total)

-- BIEN: reemplaza con el valor recalculado desde cero
ON DUPLICATE KEY UPDATE monto_total = nuevos.monto_total
```

Las ventas eliminadas lógicamente (estado `X`) tampoco afectan el resultado porque
`OperacionalRepository` las excluye en la lectura:

```sql
SELECT ... FROM ventas WHERE estado != 'X'
```

**Surrogate key** — clave artificial (entero autoincremental) que el DW asigna a cada
fila de dimensión, independiente del ID del sistema operacional. Útil para desacoplar
ambos sistemas. El proyecto usa el ID natural directamente (suficiente para la escala académica).

**Slowly Changing Dimension (SCD)** — qué hacer cuando un atributo de una dimensión
cambia. Ejemplo: si Juan cambia de sede, ¿sobreescribes el registro o guardas el historial?
El proyecto no lo necesita, pero es un tema clásico en DWs reales.

---

## Consultas analíticas

**Agregación** — `SUM`, `COUNT`, `AVG` sobre métricas agrupadas por dimensiones.
Operación fundamental en OLAP.

**Roll-up** — subir el nivel de detalle. Ejemplo: de mes → trimestre → año.
Tu `dim_fecha` tiene los tres niveles.

**Drill-down** — bajar el nivel de detalle. Ejemplo: de año → trimestre → mes.

**Slice** — filtrar por un valor fijo de una dimensión. Ejemplo: solo ventas de enero.

**Dice** — filtrar por múltiples dimensiones a la vez. Ejemplo: Juan en el 1er trimestre.

**CrossTab / Pivot** — tabla donde las filas son una dimensión y las columnas son otra.
Ejemplo: filas = vendedores, columnas = meses, celdas = monto total.
El `CreateCrossTab.EXE` opcional del proyecto generaría esto.

---

## Snowflake schema

Variante del star schema donde las dimensiones también se normalizan en subtablas.
Más complejo y raramente vale la pena. El star schema cubre la mayoría de los casos.

---

## Mapa mental

```
BD operacional (OLTP)          ETL                DW (OLAP)
─────────────────────    ──────────────    ─────────────────────
logimarket.ventas        GenerarDW.exe     logimarket_dw
una fila por venta    →  extrae, agrega →  star schema
INSERT rápido            transforma        GROUP BY rápido
cajero escribe           carga             gerente consulta
500,000 filas/año        bajo demanda      ~200 filas/año
```
