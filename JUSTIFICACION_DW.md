# Justificación del uso de MySQL como Servidor de DataWarehouse

## ¿Por qué MySQL y no una herramienta especializada?

En entornos empresariales reales, los DataWarehouses utilizan motores especializados
(Amazon Redshift, Google BigQuery, Snowflake, Oracle OLAP) optimizados para consultas
analíticas sobre cientos de millones de registros. Estos usan **almacenamiento columnar**,
compresión masiva y procesamiento paralelo distribuido.

Para este proyecto, se utilizó **MySQL 8.0** como servidor de DataWarehouse por las
siguientes razones justificadas:

---

## 1. El concepto arquitectónico se demuestra completamente

Lo que importa en esta arquitectura no es el motor del DW, sino la **separación entre
dos servidores con roles distintos**:

| Servidor        | Base de datos    | Rol              | Modelo de datos  |
|-----------------|------------------|------------------|------------------|
| Servidor de BD  | `logimarket`     | OLTP (operacional) | Tabla plana de transacciones |
| Servidor de DW  | `logimarket_dw`  | OLAP (analítico) | Star Schema (hechos + dimensiones) |

Que ambos usen MySQL es un detalle de implementación. En producción real,
`logimarket_dw` se reemplazaría por Redshift o BigQuery cambiando únicamente
la URL JDBC en `dw.properties` — el código ETL no cambiaría.

---

## 2. El modelo dimensional (star schema) es correcto

El DW implementa un modelo dimensional estándar:

```
dim_vendedor ──┐
               ├── fact_ventas (monto_total, cantidad)
dim_fecha    ──┘
```

Este diseño permite consultas analíticas reales que serían costosas sobre
`logimarket` (base operacional):

```sql
-- Ventas por vendedor y trimestre
SELECT v.id_vendedor, f.trimestre, f.anio,
       SUM(fv.monto_total) AS total, SUM(fv.cantidad) AS operaciones
FROM fact_ventas fv
JOIN dim_vendedor v ON v.id_vendedor = fv.id_vendedor
JOIN dim_fecha    f ON f.id_fecha    = fv.id_fecha
GROUP BY v.id_vendedor, f.trimestre, f.anio;
```

---

## 3. Separación OLTP vs OLAP lograda

El objetivo central de un DW es **no impactar la BD operacional** con consultas
analíticas pesadas. Eso se cumple: las consultas de análisis se ejecutan contra
`logimarket_dw` mientras `logimarket` sigue sirviendo transacciones de cajeros
sin interferencia.

---

## Conclusión

MySQL es suficiente para demostrar los principios de la arquitectura DW a escala
académica. La decisión se tomó conscientemente para evitar dependencias externas
innecesarias, manteniendo el enfoque en la arquitectura (ETL, star schema,
separación de servidores) en lugar de en la infraestructura de base de datos.
