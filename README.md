# Vendia — Sistema de Punto de Venta
## LogiMarket Perú S.A. | Arquitectura Cliente/Servidor

Sistema para registrar ventas por sede, consolidarlas en un servidor central
y cargarlas en un DataWarehouse para análisis. Los ejecutables residen en el
**Servidor de Aplicaciones**; los clientes los ejecutan mediante accesos directos.

---

## Orden de uso

```
1. VendiaUpdater          →  daemon en el Servidor de BD (siempre encendido)
2. VendiaApp              →  cajero usa durante todo el turno
3. VendiaSender           →  cajero usa solo al cerrar el turno
4. GenerarDatawareHouse   →  administrador ejecuta para actualizar el DW
```

---

## Despliegue en Servidor de Aplicaciones

Los ejecutables deben copiarse a una carpeta compartida, por ejemplo:
```
C:\ServidorApps\Vendia\
├── VendiaApp\
├── VendiaSender\
└── GenerarDatawareHouse\
```

Cada PC cliente crea **accesos directos** que apuntan a esa ruta:
- `C:\ServidorApps\Vendia\VendiaApp\VendiaApp.exe`
- `C:\ServidorApps\Vendia\VendiaSender\VendiaSender.exe`
- `C:\ServidorApps\Vendia\GenerarDatawareHouse\GenerarDatawareHouse.exe`

El cliente actúa solo como unidad de procesamiento: ejecuta la aplicación
usando su propia CPU y RAM, pero el archivo fuente reside en el servidor.

> **Demo en una sola máquina:** crear la carpeta `C:\ServidorApps\Vendia\`,
> copiar los ejecutables ahí y usar accesos directos desde el escritorio.
> Esto simula el rol del Servidor de Aplicaciones sin necesitar red física.

---

## 1. VendiaUpdater — Daemon en el Servidor de BD

Debe estar corriendo **antes** de que cualquier sede intente enviar ventas.

**Requisitos previos:**

1. Crear la base de datos en MySQL (una sola vez):
   ```sql
   CREATE DATABASE logimarket;
   ```
2. Crear la carpeta compartida, por ejemplo `C:\Users\USER\Desktop\DATOS`.

**Configurar** `VendiaUpdater/updater.properties`:
```properties
carpeta.datos=C:\\Users\\USER\\Desktop\\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/logimarket
db.usuario=root
db.password=tu_password
```

**Ejecutar:**
```bash
cd VendiaUpdater
mvn compile exec:java
```

El daemon revisa la carpeta cada `intervalo.polling.ms` ms. Por cada `.dat` nuevo
hace batch insert en MySQL y escribe un `.ack` de confirmación.
La tabla `ventas` se crea automáticamente. Detener con **ENTER**.

---

## 2. VendiaApp — Registrar ventas (en la computadora del cajero)

El cajero usa esta app durante todo el turno. **No requiere conexión al servidor.**

**Ejecutar** (o usar acceso directo al Servidor de Aplicaciones):
```bash
cd VendiaApp
mvn javafx:run
```

| Acción | Cómo hacerlo |
|--------|-------------|
| Registrar venta | ID de vendedor + monto → botón "Registrar" |
| Buscar venta | ID de venta → botón "Buscar" |
| Modificar monto | Seleccionar en tabla → nuevo monto → "Modificar" |
| Eliminar venta | Seleccionar en tabla → botón "Eliminar" |

> Las ventas nuevas tienen estado **P** (Pendiente).

---

## 3. VendiaSender — Enviar ventas al servidor (al cerrar el turno)

**Ejecutar** (o usar acceso directo al Servidor de Aplicaciones):
```bash
cd VendiaSender
mvn javafx:run
```

**Pasos dentro de la app:**

1. **Examinar...** → seleccionar `ventas.dat` (carpeta `VendiaApp/`)
2. **Examinar carpeta...** → seleccionar la misma carpeta `DATOS\` del Updater
3. **Enviar al Servidor**

Log esperado:
```
Escribiendo 5 registro(s) en ventas_20260518_143022.dat...
Archivo enviado. Esperando confirmacion del servidor...
ACK recibido: ventas_20260518_143022.ack
Envio confirmado: OK 5
Ventas marcadas como enviadas (estado=E) en ventas.dat.
```

---

## 4. GenerarDatawareHouse — Cargar el DataWarehouse

Ejecutado por el administrador cuando necesita datos actualizados para análisis.
Se conecta al Servidor de BD (origen) y al Servidor de DW (destino).

**Ejecutar** (o usar acceso directo al Servidor de Aplicaciones):
```bash
cd GenerarDatawareHouse
mvn javafx:run
```

**Configurar** `GenerarDatawareHouse/dw.properties`:
```properties
bd.url=jdbc:mysql://localhost:3306/logimarket
bd.usuario=root
bd.password=tu_password

dw.url=jdbc:mysql://localhost:3306/logimarket_dw
dw.usuario=root
dw.password=tu_password
```

Los campos se precargan automáticamente desde `dw.properties` al abrir la app.
Presionar **"Generar DataWarehouse"**. El schema `logimarket_dw` y sus tablas se
crean automáticamente si no existen.

**Lo que hace el ETL:**
- Lee todas las ventas del Servidor de BD
- Agrupa por vendedor y mes → carga `dim_vendedor`, `dim_fecha`, `fact_ventas`
- Es idempotente: ejecutarlo varias veces no duplica datos

---

## Requisitos

| Herramienta | Versión mínima | Dónde se usa |
|-------------|---------------|--------------|
| Java JDK    | 21            | Todos los módulos |
| Maven       | 3.8           | Todos los módulos |
| MySQL       | 8.0           | Servidor de BD y Servidor de DW |

---

## Más información

- [`SISTEMA.md`](SISTEMA.md) — protocolo de archivos, formato binario, esquemas SQL
- [`JUSTIFICACION_DW.md`](JUSTIFICACION_DW.md) — por qué se usó MySQL para el DataWarehouse
