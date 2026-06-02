Se recomienda abrir con .md para mejor visualización
# Manual de instalación — Proyecto Vendia

Hay dos formas de ejecutar el proyecto:

- **Opción A — Ejecutables `.exe`**: la más simple. Solo requiere instalar MySQL; Java ya va incluido dentro de cada ejecutable.
- **Opción B — Desde código con Maven**: requiere instalar JDK 21 y Maven. Útil para desarrollo o si no se tienen los `.exe` generados.

---

## Opción A: Ejecutar con los `.exe`

### Requisito único: MySQL Server 8.x
Descargar desde: https://dev.mysql.com/downloads/mysql/

Durante la instalación, anotar el usuario y contraseña de `root`. Luego crear la base de datos:
```sql
CREATE DATABASE loginmarket;
```
La tabla `ventas` se crea automáticamente al iniciar VendiaUpdater.

### Preparación

**Crear la carpeta compartida DATOS en el escritorio:**
```
mkdir C:\Users\<TuUsuario>\Desktop\DATOS
```
Reemplazar `<TuUsuario>` con el nombre de usuario de Windows.

**Configurar VendiaUpdater:**
Abrir el archivo `VendiaUpdater\target\dist\VendiaUpdater\updater.properties` y ajustar:
```properties
carpeta.datos=C:\Users\<TuUsuario>\Desktop\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/loginmarket
db.usuario=root
db.password=<tu_password_de_mysql>
```

### Ejecución (en este orden)

**1. VendiaUpdater** — abrir `VendiaUpdater\target\dist\VendiaUpdater\VendiaUpdater.exe`
Dejar la ventana de consola abierta. Muestra logs cuando recibe archivos.

**2. VendiaApp** — abrir `VendiaApp\target\dist\VendiaApp\VendiaApp.exe`
Registra ventas. Los archivos `ventas.dat` y `ventas.idx` se crean en la misma carpeta donde está el `.exe`.

**3. VendiaSender** — abrir `VendiaSender\target\dist\VendiaSender\VendiaSender.exe`
En la interfaz: buscar el archivo `ventas.dat` que generó VendiaApp (estará en `VendiaApp\target\dist\VendiaApp\`) y apuntar la carpeta compartida a `DATOS\`.

---

## Opción B: Ejecutar desde código con Maven
Si existe algún problema la ejecución directa con el .exe. también se puede ejecutar directamente desde el código
Siguiendo los siguientes pasos

#### 1. JDK 21
> JavaFX **no requiere instalación separada** — Maven lo descarga automáticamente.

### 2. Apache Maven 3.9+
Descargar desde: https://maven.apache.org/download.cgi
Descomprimir y agregar `bin\` al `PATH`.

Verificar:
```
mvn -version
```

### 3. MySQL Server 8.x
Descargar desde: https://dev.mysql.com/downloads/mysql/

Durante la instalación, anotar el usuario y contraseña de `root`. Luego crear la base de datos:
```sql
CREATE DATABASE loginmarket;
```
La tabla `ventas` se crea automáticamente al iniciar VendiaUpdater.

### Preparación

**Crear la carpeta compartida DATOS en el escritorio:**
```
mkdir C:\Users\<TuUsuario>\Desktop\DATOS
```
Reemplazar `<TuUsuario>` con el nombre de usuario de Windows.

**Configurar VendiaUpdater:**
Abrir el archivo `VendiaUpdater/updater.properties` y ajustar:
```properties
carpeta.datos=C:\Users\<TuUsuario>\Desktop\DATOS
intervalo.polling.ms=2000
db.url=jdbc:mysql://localhost:3306/loginmarket
db.usuario=root
db.password=<tu_password_de_mysql>
```

---

### Ejecución (en este orden)

#### Terminal 1 — VendiaUpdater (debe estar corriendo primero)
```
cd VendiaUpdater
mvn package -q
mvn exec:java
```
Dejar esta terminal abierta. Muestra logs cuando recibe archivos.

#### Terminal 2 — VendiaApp (la app de ventas)
```
cd VendiaApp
mvn javafx:run
```

#### Terminal 3 — VendiaSender (cuando se quiera enviar ventas)
```
cd VendiaSender
mvn javafx:run
```
En la interfaz: seleccionar el archivo `ventas.dat` que genera VendiaApp y apuntar la carpeta compartida a `DATOS\`.

---

### Notas importantes

- Las tres terminales deben abrirse desde la raíz del proyecto (`proyecto_2\`).
- VendiaUpdater debe estar corriendo **antes** de intentar enviar desde VendiaSender, o el envío hará timeout a los 30 segundos.
- La carpeta `DATOS\` debe ser la misma ruta en `updater.properties` y la que se selecciona en VendiaSender.