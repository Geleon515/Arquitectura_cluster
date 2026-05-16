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
│     Lee ventas.dat y envía por TCP      │
└──────────────────┬──────────────────────┘
                   │  TCP (puerto 9090)
                   ▼
┌─────────────────────────────────────────┐
│           SERVIDOR CENTRAL              │
│                                         │
│  3. VendiaUpdater  (daemon)             │
│     Recibe datos → INSERT en MySQL      │
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
- Las envía al servidor central por TCP
- Al recibir el `ACK`, marca cada venta como `E` (Enviada) en `ventas.dat`

### 3. VendiaUpdater — Receptor (daemon)
Aplicación de consola que corre permanentemente en el servidor central.

- Escucha conexiones TCP en un puerto configurado
- Recibe los registros binarios y los inserta en MySQL
- Responde `ACK` al cliente para confirmar la recepción
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
Abre socket TCP hacia el servidor
        │
        ├─ Envía: int (cantidad de registros)
        ├─ Envía: N registros de 130 bytes c/u
        │
        ▼
VendiaUpdater recibe la conexión
        │
        ├─ Lee int → sabe cuántos registros esperar
        ├─ Lee cada registro de 130 bytes
        ├─ INSERT en MySQL (INSERT IGNORE si ya existe)
        ├─ Envía: "ACK"
        │
        ▼
VendiaSender recibe ACK
        │
        ▼
Marca las ventas como estado = 'E' en ventas.dat
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

## Protocolo TCP

El protocolo es binario, compatible con `DataOutputStream` / `DataInputStream` de Java:

```
Cliente (VendiaSender)          Servidor (VendiaUpdater)
─────────────────────           ────────────────────────
writeInt(N)              →      readInt()
writeChars(idVenta)      →      readChar() × 20
writeChars(idVendedor)   →      readChar() × 20
writeChars(fecha)        →      readChar() × 20
writeDouble(monto)       →      readDouble()
writeChar(estado)        →      readChar()
 ... (repite N veces)
flush()                  →
                         ←      writeUTF("ACK")
```

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

Solo se requiere que la **base de datos exista previamente**:

```sql
CREATE DATABASE logimarket;
```

---

## Configuración y ejecución

### VendiaApp

```bash
cd VendiaApp
mvn clean compile
mvn javafx:run
```

No requiere configuración adicional. Los archivos `ventas.dat` y `ventas.idx`
se crean automáticamente en la carpeta raíz del proyecto.

---

### VendiaUpdater (arrancar primero)

1. Crear la base de datos en MySQL:
   ```sql
   CREATE DATABASE logimarket;
   ```

2. Editar `VendiaUpdater/updater.properties`:
   ```properties
   puerto=9090
   db.url=jdbc:mysql://localhost:3306/logimarket
   db.usuario=root
   db.password=tu_password
   ```

3. Ejecutar:
   ```bash
   cd VendiaUpdater
   mvn compile exec:java
   ```

El servidor queda escuchando. Presionar **ENTER** para detenerlo.

---

### VendiaSender

```bash
cd VendiaSender
mvn clean compile
mvn javafx:run
```

En la interfaz:
1. Seleccionar el archivo `ventas.dat` generado por VendiaApp
2. Ingresar la IP y puerto del servidor donde corre VendiaUpdater
3. Hacer clic en **"Enviar al Servidor"**

---

## Stack tecnológico

| Componente    | Tecnología                    |
|---------------|-------------------------------|
| VendiaApp     | Java 21, JavaFX 21, Maven     |
| VendiaSender  | Java 21, JavaFX 21, Maven     |
| VendiaUpdater | Java 21, MySQL 8, Maven       |
| Persistencia  | Archivos binarios + MySQL 8   |
| Comunicación  | TCP sockets (DataStream)      |
