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
| `VendiaSender` | Transferencia | Envía los archivos `.dat` al servidor por carpeta compartida |
| `VendiaUpdater` | Servidor BD | Daemon que detecta `.dat`, inserta en MySQL y ejecuta plugins |
| `VendiaWeb` | Aplicación Web | Servidor web Spring Boot con API REST y frontend React |
| `GenerarDatawareHouse` | DataWarehouse | App JavaFX que realiza el ETL desde el Mirror hacia el DW |
| `CreateCrossTab` | OLAP | App JavaFX que genera el cubo OLAP (tabla cruzada por trimestre) |
| `ViewCrossTab` | OLAP | App JavaFX para visualizar el cubo OLAP con gráficos y tabla pivot |

---

## Orden de ejecución

```
1. VendiaUpdater        →  levantar primero (daemon en el Servidor de BD)
2. VendiaWeb            →  levantar en el Servidor de Aplicaciones
3. VendiaApp            →  cajero usa durante todo el turno
4. VendiaSender         →  cajero usa al cerrar el turno
5. VendiaWeb            →  administrador sincroniza al Mirror desde la interfaz web
6. GenerarDatawareHouse →  administrador ejecuta para cargar el DW
7. CreateCrossTab       →  administrador genera el cubo OLAP
8. ViewCrossTab         →  administrador visualiza análisis gerencial
```

> Los pasos 6 y 7 se pueden automatizar con `ejecutar_etl.bat` (ver sección ETL).

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

En todos los archivos de configuración, reemplazar `AQUI_VA_TU_CONTRASENA` con la contraseña real de MySQL:

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
Al iniciar, carga automáticamente los plugins de la carpeta `plugins/`.

**Requisitos previos:**
1. La base de datos `logimarket` debe existir (ver arriba).
2. Crear la carpeta compartida, por ejemplo `C:\Users\ADMIN\Desktop\DATOS`.

**Configurar** `VendiaUpdater/updater.properties`:
```properties
carpeta.datos=C:\\Users\\ADMIN\\Desktop\\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/logimarket
db.usuario=root
db.password=AQUI_VA_TU_CONTRASENA
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

Al iniciar se verá algo como:
```
[23:24:53] PLUGIN cargado: [Alerta de Monto] desde plugin-alerta-monto.jar
[23:24:53] PLUGIN cargado: [Auditoria]       desde plugin-auditoria.jar
[23:24:53] PLUGINS activos: 2
[23:24:53] Watcher activo en C:\...\DATOS (cada 2000 ms).
Daemon activo. Presione ENTER para detener.
```

El daemon revisa la carpeta cada `intervalo.polling.ms` ms. Por cada `.dat` nuevo
hace batch insert en MySQL, ejecuta los plugins y escribe un `.ack` de confirmación.
Detener con **ENTER**.

---

## 2. VendiaWeb — Servidor de Aplicaciones Web (MVC)

Servidor Spring Boot con API REST y frontend React. Se conecta a `logimarket` y `logimarket_mirror`.

**Configurar** `VendiaWeb/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/logimarket
spring.datasource.username=root
spring.datasource.password=AQUI_VA_TU_CONTRASENA

mirror.datasource.url=jdbc:mysql://localhost:3306/logimarket_mirror
mirror.datasource.username=root
mirror.datasource.password=AQUI_VA_TU_CONTRASENA

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

> La sincronización al Mirror se hace desde la pestaña **"Capa Mirror"** en la interfaz web,
> o directamente llamando a `POST /api/mirror/sincronizar`. Debe ejecutarse antes del ETL.

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

Lee el `ventas.dat` generado por VendiaApp, lo copia a la carpeta compartida `DATOS\`
y espera el archivo `.ack` de confirmación del servidor. Marca las ventas como enviadas (**E**).

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

Lee los datos desde `logimarket_mirror` y los carga en `logimarket_dw`.
**Requiere que la sincronización al Mirror se haya ejecutado antes** (paso 5 del orden de ejecución).

**Configurar** `GenerarDatawareHouse/dw.properties`:
```properties
# Origen: servidor Mirror
bd.url=jdbc:mysql://localhost:3306/logimarket_mirror
bd.usuario=root
bd.password=AQUI_VA_TU_CONTRASENA

# Destino: DataWarehouse
dw.url=jdbc:mysql://localhost:3306/logimarket_dw
dw.usuario=root
dw.password=AQUI_VA_TU_CONTRASENA
```

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

**Configurar** `CreateCrossTab/crosstab.properties`:
```properties
dw.url=jdbc:mysql://localhost:3306/logimarket_dw
dw.usuario=root
dw.password=AQUI_VA_TU_CONTRASENA
```

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

> Las credenciales se guardan automáticamente en `crosstab.properties` después de la primera
> conexión exitosa. Desde la segunda vez en adelante los campos se precargan solos.

**Las 3 pestañas disponibles:**

| Pestaña | Contenido |
|---------|-----------|
| Tabla Cruzada (Pivot) | Grilla vendedor × Q1/Q2/Q3/Q4 con montos en S/. y cantidad de transacciones |
| Gráfico de Barras | Barras agrupadas por vendedor, una barra por trimestre |
| Distribución por Trimestre | Gráfico de torta con porcentaje y monto total por Q |

---

## ETL Automatizado — ejecutar_etl.bat

En vez de ejecutar GenerarDatawareHouse y CreateCrossTab manualmente uno por uno,
se puede usar el script `ejecutar_etl.bat` ubicado en la raíz del proyecto. Ejecuta
ambos en secuencia y repite el proceso cada 10 segundos automáticamente.

```powershell
# Desde la carpeta raíz Arquitectura_cluster/
.\ejecutar_etl.bat
```

El script corre en bucle hasta que se cierre la ventana. Útil para mantener el
DataWarehouse y el cubo OLAP actualizados de forma continua durante una sesión de trabajo.

---

## Sistema de Plugins (Arquitectura Microkernel)

`VendiaUpdater` implementa una **Arquitectura Microkernel**: el core (núcleo) solo detecta
archivos `.dat` e inserta ventas en MySQL. Toda funcionalidad adicional está en plugins
`.jar` separados, ubicados en `VendiaUpdater/plugins/`. El core no sabe qué hacen los
plugins — simplemente los invoca uno por uno después de cada inserción.

```
┌─────────────────────────────────────────────────────┐
│                  CORE (VendiaUpdater)               │
│  FolderWatcher → leer .dat → insertar en MySQL      │
│                      │                              │
│              PluginLoader.ejecutarTodos()            │
└──────────────────────┼──────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
 plugin-auditoria.jar      plugin-alerta-monto.jar
```

### Plugins incluidos

#### `plugin-auditoria.jar`
Registra cada venta en el archivo `auditoria.log` con la fecha/hora exacta, el ID de la
venta, el vendedor, el producto y el monto. Se activa con **cada** venta que llega,
sin importar el monto.

Ejemplo de entrada en `auditoria.log`:
```
[2026-06-30 23:26:06] VENTA REGISTRADA | id=VTA-001 | vendedor=EMANUEL | producto=P001 | fecha=2026-06-30 23:20 | monto=S/.1500.00
```

#### `plugin-alerta-monto.jar`
Monitorea el monto de cada venta. Si supera **S/. 1,000.00**, imprime una alerta visible
en la consola de VendiaUpdater. Las ventas por debajo del umbral se ignoran.

Ejemplo de alerta en consola:
```
============================================
[PLUGIN AlertaMonto] *** VENTA DE ALTO VALOR ***
[PLUGIN AlertaMonto] ID      : VTA-001
[PLUGIN AlertaMonto] Vendedor: EMANUEL
[PLUGIN AlertaMonto] Monto   : S/. 1500.00
[PLUGIN AlertaMonto] Supera el umbral de S/. 1000.00
============================================
```

### Cómo ejecutarlos

Los plugins se ejecutan **automáticamente** con VendiaUpdater. No requieren ningún comando
adicional. Los pasos son:

**1. Verificar que los JARs estén en la carpeta `plugins/`:**
```
VendiaUpdater/
└── plugins/
    ├── plugin-auditoria.jar
    └── plugin-alerta-monto.jar
```

**2. Ejecutar VendiaUpdater normalmente:**
```powershell
cd VendiaUpdater
.\ejecutar.bat
```

**3. Al iniciar, VendiaUpdater confirma qué plugins cargó:**
```
[23:24:53] PLUGIN cargado: [Alerta de Monto] desde plugin-alerta-monto.jar
[23:24:53] PLUGIN cargado: [Auditoria]       desde plugin-auditoria.jar
[23:24:53] PLUGINS activos: 2
[23:24:53] Watcher activo en C:\...\DATOS (cada 2000 ms).
```

**4. Cada vez que llega un `.dat`, los plugins se disparan solos:**
```
[23:26:06] Detectado: ventas_20260630.dat (340 bytes)
[23:26:06]   OK: 2 venta(s) insertada(s) en MySQL.

============================================
[PLUGIN AlertaMonto] *** VENTA DE ALTO VALOR ***
[PLUGIN AlertaMonto] ID      : VTA-DEMO-001
[PLUGIN AlertaMonto] Vendedor: EMANUEL
[PLUGIN AlertaMonto] Monto   : S/. 1500.00
[PLUGIN AlertaMonto] Supera el umbral de S/. 1000.00
============================================
[PLUGIN Auditoria] VENTA REGISTRADA | id=VTA-DEMO-001 | vendedor=EMANUEL | monto=S/.1500.00
[PLUGIN Auditoria] VENTA REGISTRADA | id=VTA-DEMO-002 | vendedor=RENZO   | monto=S/.250.00
```

> `plugin-alerta-monto` solo alertó por VTA-DEMO-001 (S/. 1500 > 1000).
> `plugin-auditoria` registró las dos ventas en el log.

> Para desactivar un plugin basta con eliminar su JAR de la carpeta `plugins/` y reiniciar VendiaUpdater.

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
Arquitectura_cluster/
├── VendiaApp/              # Cliente desktop JavaFX (cajero)
├── VendiaSender/           # Envío por carpeta compartida
├── VendiaUpdater/          # Daemon servidor de BD
│   └── plugins/            # JARs de plugins (cargados dinámicamente)
│       ├── plugin-auditoria.jar
│       └── plugin-alerta-monto.jar
├── VendiaWeb/              # Servidor web Spring Boot + React
│   └── frontend/           # Proyecto React (Vite)
├── GenerarDatawareHouse/   # ETL Mirror → DataWarehouse
├── CreateCrossTab/         # Generador de cubo OLAP (pivot trimestral)
├── ViewCrossTab/           # Visualizador de cubo OLAP (tabla + gráficos)
├── plugin-auditoria/       # Código fuente plugin de auditoría
├── plugin-alerta-monto/    # Código fuente plugin de alertas
└── ejecutar_etl.bat        # Script que automatiza ETL + CrossTab en bucle
```

---

## Flujo completo del sistema

```
[PC Cajero]                    [Servidor BD]                   [Análisis]
VendiaApp              →    VendiaUpdater
(guarda ventas.dat)         (daemon, detecta .dat)
                                    ↓
VendiaSender           →    logimarket (MySQL)
(copia .dat a DATOS\)       (ventas insertadas)
                                    ↓
                            VendiaWeb (REST API)
                            POST /api/mirror/sincronizar
                                    ↓
                            logimarket_mirror (MySQL)
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
