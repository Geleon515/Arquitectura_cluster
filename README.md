# Arquitectura_cluster

## Version 1: Arquitectura Unitaria y Acceso Aleatorio
Esta fase inicial se centra en la gestión eficiente de información persistente mediante el uso de archivos binarios y punteros de memoria.

### 🏗️ Diseño de la Solución
* **Acceso Aleatorio (`RandomAccessFile`)**: A diferencia de los archivos de texto convencionales, se emplea la clase `RandomAccessFile` para saltar directamente a cualquier registro. El sistema calcula la posición exacta en bytes mediante la fórmula:
    > $$Posición = Índice \times Tamaño$$
* **Persistencia Binaria**: El sistema genera automáticamente un archivo **`.dat`**, garantizando que la información se almacene de forma compacta.

### 🧩 Componentes del Sistema
1.  **`Venta.java` (Modelo)**: Define un registro de **longitud fija (98 bytes)**. Establece campos específicos para ID, Código de Región (Ubigeo), Producto, Monto y Fecha, asegurando que cada bloque de datos sea predecible.
2.  **`ArchivoVentas.java` (Motor de Datos)**: Es el núcleo del sistema. Gestiona las operaciones CRUD, el cálculo de *offsets* y las rutinas de filtrado por región que facilitarán el balanceo de carga.
3.  **`Main.java` (Orquestador)**: Proporciona la interfaz de consola para el usuario y gestiona la carga inicial de datos de prueba para validar la integridad del archivo `.dat`.

---

## 🌐 Versión 2: Cluster Computing (En Desarrollo)

La arquitectura actual está diseñada para escalar hacia un entorno distribuido sin modificar la lógica base. Los siguientes pasos para la V2 incluyen:
* **Comunicación TCP/IP**: Implementación de **Sockets** para la conexión entre el Servidor Central y los Nodos de procesamiento.
* **Balanceo de Carga (`Assign.txt`)**: Integración de un archivo de configuración que distribuye los códigos de región (Ubigeo) entre diferentes Hostnames o IPs.
* **Procesamiento Paralelo**: Delegación de la lógica de `ArchivoVentas` a cada nodo, permitiendo que procesen segmentos específicos del archivo maestro en paralelo.

# Version 2
