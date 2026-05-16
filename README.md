# Vendia — Sistema de Punto de Venta
## LogiMarket Perú S.A.

Sistema para registrar ventas por sede y consolidarlas en un servidor central.
Está compuesto por tres aplicaciones que se usan en un orden específico.

---

## Orden de uso

```
1. VendiaUpdater  →  servidor siempre encendido
2. VendiaApp      →  cajero usa durante todo el turno
3. VendiaSender   →  cajero usa solo al cerrar el turno
```

---

## 1. VendiaUpdater — Levantar el servidor (solo en el servidor central)

Esta app debe estar corriendo **antes** de que cualquier sede intente enviar ventas.
Es una aplicación de consola que queda escuchando en segundo plano.

**Requisito previo — crear la base de datos en MySQL una sola vez:**
```sql
CREATE DATABASE logimarket;
```

**Configurar credenciales** en `VendiaUpdater/updater.properties`:
```properties
puerto=9090
db.url=jdbc:mysql://localhost:3306/logimarket
db.usuario=root
db.password=tu_password
```

**Ejecutar:**
```bash
cd VendiaUpdater
mvn compile exec:java
```

Verás en consola:
```
  VendiaUpdater — LogiMarket Peru S.A.
  Puerto TCP  : 9090
  BD URL      : jdbc:mysql://localhost:3306/logimarket
  ...
Servidor activo. Presione ENTER para detener.
```

El servidor queda esperando conexiones. La tabla `ventas` se crea automáticamente
en MySQL si no existe. Para detenerlo, presionar **ENTER**.

---

## 2. VendiaApp — Registrar ventas (en la computadora del cajero)

El cajero usa esta app durante todo el turno para registrar las ventas del día.
**No necesita internet ni conexión al servidor** — todo se guarda localmente.

**Ejecutar:**
```bash
cd VendiaApp
mvn javafx:run
```

**Operaciones disponibles:**

| Acción | Cómo hacerlo |
|--------|-------------|
| Registrar venta | Ingresar ID de vendedor y monto → botón "Registrar" |
| Buscar venta | Ingresar el ID de venta → botón "Buscar" |
| Modificar monto | Seleccionar venta en la tabla → ingresar nuevo monto → "Modificar" |
| Eliminar venta | Seleccionar venta en la tabla → botón "Eliminar" |

> Las ventas recién registradas aparecen con estado **P** (Pendiente).
> Las ventas eliminadas desaparecen de la tabla pero se conservan en el archivo con estado **X**.

---

## 3. VendiaSender — Enviar ventas al servidor (al cerrar el turno)

Al terminar el turno, el cajero abre esta app para enviar todas las ventas
pendientes al servidor central.

**Ejecutar:**
```bash
cd VendiaSender
mvn javafx:run
```

**Pasos dentro de la app:**

1. **Seleccionar el archivo** → botón "Examinar..." → buscar `ventas.dat`
   (se encuentra en la carpeta `VendiaApp/`)
2. **Ingresar los datos del servidor:**
   - Dirección IP del servidor donde corre VendiaUpdater
   - Puerto (por defecto `9090`)
3. **Hacer clic en "Enviar al Servidor"**

Si todo va bien, verás en el log:
```
Conexion establecida. Enviando 5 registro(s)...
  Enviado: VTA-20260516-143022  |  S/. 150.00
  Enviado: VTA-20260516-151030  |  S/. 89.50
  ...
ACK recibido. Envio completado exitosamente.
```

Las ventas enviadas cambian automáticamente a estado **E** (Enviada) en `ventas.dat`.
Si se ejecuta dos veces por error, el servidor las ignora sin duplicarlas.

---

## Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| Java JDK    | 21            |
| Maven       | 3.8           |
| MySQL       | 8.0 (solo en el servidor) |

---

## Más información

Para detalles técnicos sobre el protocolo TCP, el formato binario de los archivos
y el esquema de la base de datos, ver [`SISTEMA.md`](SISTEMA.md).
