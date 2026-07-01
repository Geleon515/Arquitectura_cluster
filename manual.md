Se recomienda abrir con visor .md para mejor visualización

# Manual de Instalación y Ejecución — Proyecto Vendia
## LogiMarket Perú S.A.

---

## Requisitos previos

| Herramienta | Versión mínima | Descarga |
|-------------|---------------|----------|
| Java JDK | 21 | https://adoptium.net |
| Apache Maven | 3.8 | https://maven.apache.org/download.cgi |
| Node.js | 18 | https://nodejs.org |
| MySQL Server | 8.0 | https://dev.mysql.com/downloads/mysql/ |

> JavaFX **no requiere instalación separada** — Maven lo descarga automáticamente.

Verificar instalaciones:
```
java -version
mvn -version
node -version
npm -version
```

---

## Paso 1 — Crear las bases de datos en MySQL

Abrir MySQL y ejecutar:
```sql
CREATE DATABASE logimarket;
CREATE DATABASE logimarket_mirror;
CREATE DATABASE logimarket_dw;
```

> Las tablas dentro de cada base se crean **automáticamente** la primera vez que se ejecuta cada módulo.

---

## Paso 2 — Configurar contraseñas

En cada archivo de propiedades, reemplazar `AQUI_VA_TU_CONTRASENA` con la contraseña real de MySQL:

| Archivo | Campo(s) a reemplazar |
|---------|----------------------|
| `VendiaUpdater/updater.properties` | `db.password` |
| `VendiaWeb/src/main/resources/application.properties` | `spring.datasource.password` y `mirror.datasource.password` |
| `GenerarDatawareHouse/dw.properties` | `bd.password` y `dw.password` |
| `CreateCrossTab/crosstab.properties` | `dw.password` |

> `ViewCrossTab` no necesita configuración manual — guarda la contraseña automáticamente tras la primera conexión exitosa.

---

## Paso 3 — Crear la carpeta compartida

VendiaSender y VendiaUpdater se comunican mediante una carpeta compartida:
```
mkdir C:\Users\<TuUsuario>\Desktop\DATOS
```
Reemplazar `<TuUsuario>` con el nombre de usuario de Windows.

Luego abrir `VendiaUpdater/updater.properties` y ajustar la ruta:
```properties
carpeta.datos=C:\\Users\\<TuUsuario>\\Desktop\\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/logimarket
db.usuario=root
db.password=AQUI_VA_TU_CONTRASENA
```

---

## Paso 4 — Compilar los módulos

Compilar cada módulo una vez antes de ejecutarlo por primera vez.

```powershell
cd VendiaUpdater
mvn clean package -DskipTests

cd ..\VendiaApp
mvn clean package -DskipTests

cd ..\VendiaSender
mvn clean package -DskipTests

cd ..\GenerarDatawareHouse
mvn clean package -DskipTests

cd ..\CreateCrossTab
mvn clean package -DskipTests

cd ..\ViewCrossTab
mvn clean package -DskipTests
```

Para VendiaWeb, compilar también el frontend React:
```powershell
cd VendiaWeb\frontend
npm install
npm run build

cd ..
mvn clean package -DskipTests
```

---

## Paso 5 — Orden de ejecución

### 5.1 VendiaUpdater — levantar primero

```powershell
cd VendiaUpdater
.\ejecutar.bat
```

Al iniciar confirma los plugins cargados y queda esperando archivos:
```
[23:24:53] PLUGIN cargado: [Alerta de Monto] desde plugin-alerta-monto.jar
[23:24:53] PLUGIN cargado: [Auditoria]       desde plugin-auditoria.jar
[23:24:53] PLUGINS activos: 2
[23:24:53] Watcher activo en C:\...\DATOS (cada 2000 ms).
Daemon activo. Presione ENTER para detener.
```

Dejar esta ventana **abierta durante toda la sesión**. Detener con **ENTER**.

---

### 5.2 VendiaWeb — servidor de aplicaciones

```powershell
cd VendiaWeb
.\ejecutar.bat
```

Esperar hasta ver:
```
=== SERVIDOR WEB INICIADO EN http://localhost:8080 ===
```

Abrir `http://localhost:8080` en el navegador. Desde ahí se gestionan ventas y se sincroniza al Mirror.

---

### 5.3 VendiaApp — registrar ventas (PC del cajero)

```powershell
cd VendiaApp
.\ejecutar.bat
```

| Acción | Cómo hacerlo |
|--------|-------------|
| Registrar venta | ID de vendedor + ID de producto + monto → botón "Registrar" |
| Buscar venta | ID de venta → botón "Buscar" |
| Modificar monto | Seleccionar en tabla → nuevo monto → "Modificar" |
| Eliminar venta | Seleccionar en tabla → botón "Eliminar" |

Las ventas nuevas quedan en estado **P** (Pendiente). Solo las pendientes se pueden modificar o eliminar.

---

### 5.4 VendiaSender — enviar ventas al servidor (al cerrar el turno)

```powershell
cd VendiaSender
.\ejecutar.bat
```

Dentro de la app:
1. **Examinar...** → seleccionar `ventas.dat` (carpeta `VendiaApp/`)
2. **Examinar carpeta...** → seleccionar la carpeta `DATOS\` creada en el Paso 3
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

### 5.5 Sincronizar al Mirror (desde VendiaWeb)

En el navegador, ir a `http://localhost:8080` → pestaña **"Capa Mirror"** → botón **"Sincronizar"**.

Esto replica los datos de `logimarket` → `logimarket_mirror`. Debe hacerse **antes** de ejecutar el ETL.

---

### 5.6 GenerarDatawareHouse — cargar el DataWarehouse

```powershell
cd GenerarDatawareHouse
.\ejecutar.bat
```

Los campos se precargan desde `dw.properties`. Presionar **"Generar DataWarehouse"**.

Lo que hace el ETL:
- Lee ventas desde `logimarket_mirror`
- Carga `dim_vendedor`, `dim_fecha` y `fact_ventas` en `logimarket_dw`
- Es idempotente: ejecutarlo varias veces no duplica datos

---

### 5.7 CreateCrossTab — generar cubo OLAP

```powershell
cd CreateCrossTab
.\ejecutar.bat
```

Dentro de la app:
1. Verificar URL, usuario y contraseña del DW
2. Seleccionar el **año** a procesar
3. Presionar **"Generar CrossTab"**
4. Opcional: **"Exportar CSV"**

Genera la tabla `crosstab_ventas` con montos y cantidades por vendedor y trimestre (Q1/Q2/Q3/Q4).

---

### 5.8 ViewCrossTab — visualizar el cubo OLAP

```powershell
cd ViewCrossTab
.\ejecutar.bat
```

| Pestaña | Contenido |
|---------|-----------|
| Tabla Cruzada (Pivot) | Grilla vendedor × Q1/Q2/Q3/Q4 con montos en S/. |
| Gráfico de Barras | Barras agrupadas por vendedor, una por trimestre |
| Distribución por Trimestre | Gráfico de torta con porcentaje por Q |

> Desde la segunda ejecución la app se conecta automáticamente (guarda las credenciales).

---

## ETL Automatizado

En vez de ejecutar GenerarDatawareHouse y CreateCrossTab manualmente, se puede usar el script de la raíz del proyecto:

```powershell
.\ejecutar_etl.bat
```

Ejecuta ambos en secuencia y repite cada 10 segundos hasta que se cierre la ventana.

---

## Plugins (Arquitectura Microkernel)

VendiaUpdater incluye dos plugins `.jar` en la carpeta `plugins/` que se cargan automáticamente al iniciar:

| Plugin | Qué hace | Cuándo se activa |
|--------|----------|-----------------|
| `plugin-auditoria.jar` | Escribe en `auditoria.log` cada venta recibida con timestamp | Siempre |
| `plugin-alerta-monto.jar` | Imprime alerta en consola | Solo si monto > S/. 1,000 |

No requieren ninguna configuración ni comando adicional. Solo deben estar en `VendiaUpdater/plugins/`.

---

## Notas importantes

- **VendiaUpdater debe estar corriendo antes** de intentar enviar desde VendiaSender.
- **Sincronizar al Mirror antes del ETL** — GenerarDatawareHouse lee desde `logimarket_mirror`, no desde `logimarket`.
- La carpeta `DATOS\` debe ser la **misma ruta** en `updater.properties` y la que se selecciona en VendiaSender.
- Si VendiaWeb no inicia, verificar que la contraseña en `application.properties` sea la correcta (no el placeholder).
