# Vendia — Sistema de Punto de Venta
## LogiMarket Perú S.A. | Arquitectura MVC con N Capas

Sistema distribuido para registrar ventas por sede, consolidarlas en un servidor
central, replicarlas en un servidor Mirror y cargarlas en un DataWarehouse para
análisis OLAP. Implementa el patrón **MVC** en todas sus capas:

| MVC | Componente |
|-----|-----------|
| Modelo | Base de datos MySQL + repositorios JDBC |
| Vista | React + Vite (VendiaWeb) / JavaFX (apps de escritorio) |
| Controlador | Spring Boot REST API (VendiaWeb) / controladores Java (apps) |

---

## Módulos del sistema

| Módulo | Capa | Descripción |
|--------|------|-------------|
| `VendiaApp` | Cliente | App de escritorio JavaFX para cajeros. Almacena ventas en archivos binarios locales |
| `VendiaSender` | FTP | Envía los archivos `.dat` al servidor por carpeta compartida |
| `VendiaUpdater` | Datos | Daemon que detecta archivos `.dat` e inserta en MySQL |
| `VendiaWeb` | Aplicación Web | Servidor web Spring Boot con API REST y frontend React |
| `GenerarDatawareHouse` | DataWarehouse | App JavaFX que realiza el ETL desde el Mirror hacia el DW |
| `CreateCrossTab` | OLAP | App JavaFX que genera el cubo OLAP (tabla cruzada por trimestre) |
| `ViewCrossTab` | OLAP | App JavaFX para visualizar el cubo OLAP con gráficos y tabla pivot |

---

## Orden de uso

```
1. VendiaUpdater        →  levantar primero (daemon en el Servidor de BD)
2. VendiaWeb            →  levantar en el Servidor de Aplicaciones
3. VendiaApp            →  cajero usa durante todo el turno
4. VendiaSender         →  cajero usa al cerrar el turno
5. GenerarDatawareHouse →  administrador ejecuta para actualizar el DW
6. CreateCrossTab       →  administrador genera el cubo OLAP
7. ViewCrossTab         →  administrador visualiza análisis gerencial
```

---

## Bases de datos requeridas

Antes de ejecutar cualquier módulo, crear las tres bases de datos en MySQL:

```sql
CREATE DATABASE logimarket;
CREATE DATABASE logimarket_mirror;
CREATE DATABASE logimarket_dw;
```

> Las tablas se crean **automáticamente** la primera vez que se ejecuta cada módulo.

---

## Configuración de contraseñas

Antes de ejecutar cualquier módulo, reemplazar `tu_password` con la contraseña real de MySQL en los siguientes archivos:

| Archivo | Campo |
|---------|-------|
| `VendiaUpdater/updater.properties` | `db.password` |
| `VendiaWeb/src/main/resources/application.properties` | `spring.datasource.password` y `mirror.datasource.password` |
| `GenerarDatawareHouse/dw.properties` | `bd.password` y `dw.password` |
| `CreateCrossTab/crosstab.properties` | `dw.password` |

> `ViewCrossTab` no necesita configuración manual — al conectarse por primera vez guarda la contraseña automáticamente en `ViewCrossTab/crosstab.properties`.

---

## 1. VendiaUpdater — Daemon en el Servidor de BD

Debe estar corriendo **antes** de que cualquier sede intente enviar ventas.

**Requisitos previos:**
1. La base de datos `logimarket` debe existir (ver arriba).
2. Crear la carpeta compartida, por ejemplo `C:\Users\ADMIN\Desktop\DATOS`.

**Configurar** `VendiaUpdater/updater.properties`:
```properties
carpeta.datos=C:\\Users\\ADMIN\\Desktop\\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/logimarket
db.usuario=root
db.password=tu_password
```

**Compilar:**
```powershell
cd VendiaUpdater
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd VendiaUpdater
.\ejecutar.bat
```

El daemon revisa la carpeta cada `intervalo.polling.ms` ms. Por cada `.dat` nuevo
hace batch insert en MySQL y escribe un `.ack` de confirmación.
La tabla `ventas` se crea automáticamente. Detener con **ENTER**.

---

## 2. VendiaWeb — Servidor de Aplicaciones Web (MVC)

Servidor Spring Boot con API REST y frontend React. Se conecta a `logimarket` y `logimarket_mirror`.

**Configurar** `VendiaWeb/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/logimarket
spring.datasource.username=root
spring.datasource.password=tu_password

mirror.datasource.url=jdbc:mysql://localhost:3306/logimarket_mirror
mirror.datasource.username=root
mirror.datasource.password=tu_password

server.port=8080
```

**Compilar (incluye build del frontend React):**
```powershell
cd VendiaWeb/frontend
npm install        # solo la primera vez
npm run build

cd ..
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd VendiaWeb
.\ejecutar.bat
```

Luego abrir `http://localhost:8080` en el navegador.

**Endpoints REST disponibles:**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/ventas` | Lista todas las ventas activas |
| GET | `/api/ventas/{id}` | Busca una venta por ID |
| GET | `/api/ventas/estadisticas` | Total y pendientes |
| POST | `/api/ventas/registrar` | Registra una nueva venta |
| PUT | `/api/ventas/{id}/monto` | Modifica el monto (solo estado P) |
| DELETE | `/api/ventas/{id}` | Elimina lógicamente (solo estado P) |
| POST | `/api/mirror/sincronizar` | Replica datos de logimarket → logimarket_mirror |
| GET | `/api/mirror/estado` | Verifica conectividad del Mirror |

---

## 3. VendiaApp — Registrar ventas (computadora del cajero)

El cajero usa esta app durante todo el turno. **No requiere conexión al servidor.**
Las ventas se guardan en formato binario (`ventas.dat`) con registros de 170 bytes:
`idVenta(20) + idVendedor(20) + idProducto(20) + fecha(20) + monto(8) + estado(2)`.

**Compilar:**
```powershell
cd VendiaApp
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd VendiaApp
.\ejecutar.bat
```

La tabla muestra: **ID Venta · Vendedor · Producto · Fecha · Monto · Estado**

| Acción | Cómo hacerlo |
|--------|-------------|
| Registrar venta | ID de vendedor + ID de producto + monto → botón "Registrar" |
| Buscar venta | ID de venta → botón "Buscar" |
| Modificar monto | Seleccionar en tabla → nuevo monto → "Modificar" |
| Eliminar venta | Seleccionar en tabla → botón "Eliminar" |

> Las ventas nuevas tienen estado **P** (Pendiente). Solo las pendientes pueden modificarse o eliminarse.

---

## 4. VendiaSender — Enviar ventas al servidor (al cerrar el turno)

Lee el mismo formato de 170 bytes que VendiaApp. Marca las ventas como enviadas (**E**) después del envío.

**Compilar:**
```powershell
cd VendiaSender
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd VendiaSender
.\ejecutar.bat
```

**Pasos dentro de la app:**

1. **Examinar...** → seleccionar `ventas.dat` (carpeta `VendiaApp/`)
2. **Examinar carpeta...** → seleccionar la carpeta `DATOS\` del Updater
3. **Enviar al Servidor**

Log esperado:
```
Escribiendo 5 registro(s) en ventas_20260615_143022.dat...
Archivo enviado. Esperando confirmacion del servidor...
ACK recibido: ventas_20260615_143022.ack
Envio confirmado: OK 5
Ventas marcadas como enviadas (estado=E) en ventas.dat.
```

---

## 5. GenerarDatawareHouse — Cargar el DataWarehouse

Lee los datos desde `logimarket_mirror` (no directamente desde el operacional) y los carga en `logimarket_dw`.

**Compilar:**
```powershell
cd GenerarDatawareHouse
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd GenerarDatawareHouse
.\ejecutar.bat
```

**Configurar** `GenerarDatawareHouse/dw.properties`:
```properties
# Origen: servidor Mirror
bd.url=jdbc:mysql://localhost:3306/logimarket_mirror
bd.usuario=root
bd.password=tu_password

# Destino: DataWarehouse
dw.url=jdbc:mysql://localhost:3306/logimarket_dw
dw.usuario=root
dw.password=tu_password
```

Los campos se precargan automáticamente desde `dw.properties` al abrir la app.
Presionar **"Generar DataWarehouse"**. El schema `logimarket_dw` y sus tablas se
crean automáticamente si no existen.

**Lo que hace el ETL:**
- Lee todas las ventas del Mirror (`logimarket_mirror`)
- Agrupa por vendedor y mes → carga `dim_vendedor`, `dim_fecha`, `fact_ventas`
- Es idempotente: ejecutarlo varias veces no duplica datos

---

## 6. CreateCrossTab — Generar cubo OLAP

Genera la tabla cruzada (pivot) de ventas por vendedor y trimestre a partir del DataWarehouse.

**Compilar:**
```powershell
cd CreateCrossTab
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd CreateCrossTab
.\ejecutar.bat
```

**Pasos dentro de la app:**
1. Verificar o completar la URL del DW, usuario y contraseña
2. Seleccionar el **año** a procesar
3. Presionar **"Generar CrossTab"**
4. La tabla `crosstab_ventas` se crea automáticamente en `logimarket_dw`
5. Opcionalmente presionar **"Exportar CSV"** para guardar los resultados

**Tabla generada (`crosstab_ventas`):**

| id_vendedor | anio | q1_monto | q2_monto | q3_monto | q4_monto | total_anual | q1_cantidad | ... |
|-------------|------|----------|----------|----------|----------|-------------|-------------|-----|
| V001 | 2026 | 1500.00 | 2300.00 | 890.00 | 3100.00 | 7790.00 | 12 | ... |

> Ejecutarlo varias veces es seguro: usa UPSERT (no duplica registros).

---

## 7. ViewCrossTab — Visualizar cubo OLAP

Visualiza los datos generados por CreateCrossTab con tabla pivot y gráficos.

**Requisito previo:** haber ejecutado **CreateCrossTab** al menos una vez para el año que se quiere ver.

**Compilar:**
```powershell
cd ViewCrossTab
mvn clean package -DskipTests
```

**Ejecutar:**
```powershell
cd ViewCrossTab
.\ejecutar.bat
```

> Las credenciales se guardan automáticamente en `crosstab.properties` después de la primera conexión exitosa. Desde la segunda vez en adelante los campos se precargan solos y la app se conecta automáticamente al iniciar.

**Las 3 pestañas disponibles:**

| Pestaña | Contenido |
|---------|-----------|
| Tabla Cruzada (Pivot) | Grilla vendedor × Q1/Q2/Q3/Q4 con montos en S/. y cantidad de transacciones |
| Gráfico de Barras | Barras agrupadas por vendedor, una barra por trimestre |
| Distribución por Trimestre | Gráfico de torta con porcentaje y monto total por Q |

---

## Ejecución desde IntelliJ IDEA

Si preferís no usar PowerShell, todos los módulos se pueden correr directamente desde IntelliJ:

| Módulo | Cómo ejecutar desde IntelliJ |
|--------|------------------------------|
| `VendiaWeb` | Abrir `VendiaWebApplication.java` → clic en ▶ verde (Spring Boot detectado automáticamente) |
| `VendiaUpdater` | Clic derecho sobre `Main.java` → **Run 'Main'** |
| `VendiaApp` | Panel Maven (barra derecha) → módulo → **Plugins → javafx → `javafx:run`** |
| `VendiaSender` | Panel Maven → módulo → **Plugins → javafx → `javafx:run`** |
| `GenerarDatawareHouse` | Panel Maven → módulo → **Plugins → javafx → `javafx:run`** |
| `CreateCrossTab` | Panel Maven → módulo → **Plugins → javafx → `javafx:run`** |
| `ViewCrossTab` | Panel Maven → módulo → **Plugins → javafx → `javafx:run`** |

> Si el panel Maven no aparece: `View → Tool Windows → Maven`
>
> Para los módulos JavaFX **no uses** Run directo sobre el `main` — va a fallar por el module-path de JavaFX. Siempre usá `javafx:run` desde el panel Maven.

---

## Requisitos

| Herramienta | Versión mínima | Dónde se usa |
|-------------|---------------|--------------|
| Java JDK    | 21            | Todos los módulos Java |
| Maven       | 3.8           | Todos los módulos Java |
| Node.js     | 18            | VendiaWeb / frontend React |
| npm         | 9             | VendiaWeb / frontend React |
| MySQL       | 8.0           | Servidor de BD, Mirror y DW |

---

## Estructura del proyecto

```
Logimarket/
├── VendiaApp/              # Cliente desktop JavaFX (cajero)
├── VendiaSender/           # Envío por carpeta compartida
├── VendiaUpdater/          # Daemon servidor de BD
├── VendiaWeb/              # Servidor web Spring Boot + React
│   └── frontend/           # Proyecto React (Vite)
├── GenerarDatawareHouse/   # ETL Mirror → DataWarehouse
├── CreateCrossTab/         # Generador de cubo OLAP (pivot trimestral)
└── ViewCrossTab/           # Visualizador de cubo OLAP (tabla + gráficos)
```

---

## Flujo completo del sistema

```
[PC Cajero]                [Servidor]                      [Análisis]
VendiaApp         →    VendiaUpdater   →   logimarket (MySQL)
(ventas.dat local)     (daemon)                 ↓
                                           VendiaWeb (REST API)
VendiaSender      →    carpeta DATOS          ↓
(envío .dat)           (shared folder)    Mirror: logimarket_mirror
                                               ↓
                                       GenerarDatawareHouse (ETL)
                                               ↓
                                       logimarket_dw
                                       (dim_vendedor, dim_fecha, fact_ventas)
                                               ↓
                                         CreateCrossTab
                                       (genera crosstab_ventas)
                                               ↓
                                          ViewCrossTab
                                     (tabla pivot + gráficos OLAP)
```
