# Flujo de Datos — VendiaApp
### Archivos indexados, registros binarios y RandomAccessFile

---

## 1. Visión general

El sistema persiste las ventas en **dos archivos binarios** que trabajan en conjunto:

```
ventas.dat   →  datos reales, registros de tamaño fijo (130 bytes c/u)
ventas.idx   →  índice, mapea cada ID de venta a su posición en .dat
```

En memoria se mantiene un **TreeMap** que replica el índice para evitar lecturas de disco en cada búsqueda.

```
                     ┌─────────────────────────────┐
  VentaController    │        VentaStorage          │
  ──────────────►    │                              │
                     │  TreeMap<String, Long>       │  ← índice en memoria
                     │  "VTA-...-001" → 0           │
                     │  "VTA-...-002" → 130         │
                     │  "VTA-...-003" → 260         │
                     └────────┬──────────┬──────────┘
                              │          │
                         ventas.dat   ventas.idx
```

---

## 2. Estructura de ventas.dat

Cada venta ocupa exactamente **130 bytes**, sin importar la longitud real de los textos.
Los strings se rellenan con espacios (`padOrTruncate`) para alcanzar el tamaño fijo.

```
┌────────────────────────────────────────────────────────────────────────┐
│  UN REGISTRO = 130 bytes                                               │
│                                                                        │
│  ┌──────────────┬──────────────┬──────────────┬──────────┬──────────┐ │
│  │   idVenta    │  idVendedor  │    fecha     │  monto   │ estado   │ │
│  │  20 chars    │   20 chars   │   20 chars   │  double  │  char    │ │
│  │  = 40 bytes  │  = 40 bytes  │  = 40 bytes  │ = 8 bytes│ = 2 bytes│ │
│  └──────────────┴──────────────┴──────────────┴──────────┴──────────┘ │
│                                                                        │
│  Total: 40 + 40 + 40 + 8 + 2 = 130 bytes                              │
└────────────────────────────────────────────────────────────────────────┘
```

El archivo completo es una secuencia de estos bloques:

```
ventas.dat
┌──────────────────────┬──────────────────────┬──────────────────────┬──────
│   Registro 0         │   Registro 1         │   Registro 2         │  ...
│   130 bytes          │   130 bytes          │   130 bytes          │
│   offset: 0          │   offset: 130        │   offset: 260        │
└──────────────────────┴──────────────────────┴──────────────────────┴──────
```

> **¿Por qué tamaño fijo?** Permite saltar directamente a cualquier registro con
> `raf.seek(posicion)` sin recorrer el archivo desde el inicio. Es la base del acceso aleatorio.

---

## 3. Estructura de ventas.idx

El índice también es binario y de tamaño fijo: **48 bytes por entrada**.

```
┌─────────────────────────────────────────────────┐
│  UNA ENTRADA DEL ÍNDICE = 48 bytes              │
│                                                 │
│  ┌──────────────────────┬──────────────────┐   │
│  │       idVenta        │     posicion     │   │
│  │      20 chars        │      long        │   │
│  │     = 40 bytes       │    = 8 bytes     │   │
│  └──────────────────────┴──────────────────┘   │
└─────────────────────────────────────────────────┘
```

Ejemplo de cómo luce ventas.idx si hay 3 ventas:

```
ventas.idx
┌────────────────────────────────────────┬────────────┐
│ "VTA-20260516-100000   " (40 bytes)    │  0 (8 B)   │  → registro en offset 0
├────────────────────────────────────────┼────────────┤
│ "VTA-20260516-100001   " (40 bytes)    │ 130 (8 B)  │  → registro en offset 130
├────────────────────────────────────────┼────────────┤
│ "VTA-20260516-100002   " (40 bytes)    │ 260 (8 B)  │  → registro en offset 260
└────────────────────────────────────────┴────────────┘
```

---

## 4. RandomAccessFile — cómo se usa

`RandomAccessFile` (RAF) permite leer y escribir en cualquier posición del archivo,
a diferencia de los streams secuenciales (`FileInputStream`, `BufferedReader`).

Las operaciones clave que usa el proyecto:

| Operación              | Descripción                                           |
|------------------------|-------------------------------------------------------|
| `raf.seek(pos)`        | Mueve el cursor a la posición `pos` en bytes          |
| `raf.getFilePointer()` | Retorna la posición actual del cursor                 |
| `raf.length()`         | Retorna el tamaño total del archivo en bytes          |
| `raf.writeChars(s)`    | Escribe cada char del string como 2 bytes (UTF-16)    |
| `raf.readChar()`       | Lee 2 bytes y los interpreta como un char             |
| `raf.writeDouble(d)`   | Escribe el double como 8 bytes                        |
| `raf.readDouble()`     | Lee 8 bytes y los interpreta como double              |
| `raf.writeChar(c)`     | Escribe el char como 2 bytes                          |
| `raf.setLength(0)`     | Trunca el archivo a 0 bytes (para reescribir el índice)|

---

## 5. Flujo CREATE — Registrar venta

```
Usuario ingresa ID vendedor + monto
         │
         ▼
VentaController.registrarVenta()
  ├─ Valida datos (monto > 0, vendedor no vacío)
  ├─ Genera ID único: "VTA-20260516-143022"
  ├─ Crea objeto Venta con estado = 'P'
  └─► VentaStorage.insertar(venta)
            │
            ├─ 1. Abre ventas.dat en modo "rw"
            │
            ├─ 2. raf.seek(raf.length())
            │      Mueve cursor al final del archivo
            │      Si el archivo tiene 2 registros → cursor en byte 260
            │
            ├─ 3. escribirRegistro(raf, venta)
            │      raf.writeChars(idVenta padded)    → 40 bytes
            │      raf.writeChars(idVendedor padded) → 40 bytes
            │      raf.writeChars(fecha padded)      → 40 bytes
            │      raf.writeDouble(montoTotal)        →  8 bytes
            │      raf.writeChar(estado)              →  2 bytes
            │      Total escrito: 130 bytes
            │
            ├─ 4. indice.put("VTA-...", 260L)
            │      Actualiza el TreeMap en memoria
            │
            └─ 5. guardarIndice()
                   Reescribe ventas.idx completo desde el TreeMap
```

---

## 6. Flujo READ — Buscar por ID

La búsqueda es **O(log N)** gracias al TreeMap (árbol rojo-negro internamente).

```
Usuario ingresa ID: "VTA-20260516-143022"
         │
         ▼
VentaController.buscar(id)
  └─► VentaStorage.buscarPorId(id)
            │
            ├─ 1. indice.get("VTA-20260516-143022")
            │      Busca en el TreeMap en memoria → retorna 260L
            │      Si no existe → retorna null (no va al disco)
            │
            ├─ 2. raf.seek(260)
            │      Salta directamente al byte 260 del archivo
            │      Sin recorrer registros anteriores
            │
            └─ 3. leerRegistro(raf)
                   Lee 20 chars → idVenta    (trim espacios)
                   Lee 20 chars → idVendedor (trim espacios)
                   Lee 20 chars → fecha      (trim espacios)
                   Lee double   → montoTotal
                   Lee char     → estado
                   Retorna objeto Venta
```

> **Ventaja clave:** sin el índice habría que recorrer todo ventas.dat registro
> por registro hasta encontrar el ID. Con el índice se hace un salto directo.

---

## 7. Flujo UPDATE — Modificar monto

El update es **in-place**: se sobreescribe el registro en su misma posición, sin mover
ningún otro registro. Esto es posible solo porque todos los registros tienen el mismo tamaño.

```
Usuario selecciona venta en tabla + ingresa nuevo monto
         │
         ▼
VentaController.modificarMonto(id, nuevoMonto)
  └─► VentaStorage.actualizar(venta)
            │
            ├─ 1. indice.get(id) → posicion = 130L
            │
            ├─ 2. raf.seek(130)
            │      Salta al registro
            │
            ├─ 3. leerRegistro(raf)
            │      Lee el registro actual
            │      Verifica que estado != 'E' (enviado no se modifica)
            │      Cursor queda en byte 260 (inicio del siguiente registro)
            │
            ├─ 4. raf.seek(130)
            │      Regresa al inicio del mismo registro
            │
            └─ 5. escribirRegistro(raf, ventaModificada)
                   Sobreescribe los 130 bytes con el nuevo monto
                   Los demás campos (id, vendedor, fecha, estado) no cambian
```

---

## 8. Flujo DELETE — Eliminación lógica

No se elimina físicamente el registro porque eso rompería el esquema de tamaño fijo
(los offsets del índice dejarían de ser válidos). En cambio se **marca** el registro.

```
Usuario selecciona venta + confirma eliminación
         │
         ▼
VentaController.eliminar(id)
  └─► VentaStorage.eliminar(id)
            │
            ├─ 1. indice.get(id) → posicion = 0L
            │
            ├─ 2. raf.seek(0)
            │
            ├─ 3. leerRegistro(raf)
            │      Verifica que estado != 'E'
            │
            ├─ 4. raf.seek(0)
            │
            ├─ 5. escribirRegistro(raf, ventaModificada)
            │      venta.setEstado('X')
            │      venta.setIdVenta("ELIMINADO__________")
            │      Sobreescribe el registro en disco
            │
            ├─ 6. indice.remove(id)
            │      Elimina del TreeMap en memoria
            │
            └─ 7. guardarIndice()
                   Reescribe ventas.idx sin esa entrada
```

El registro físico permanece en ventas.dat pero con estado `'X'` y un ID
que indica que fue eliminado. La UI lo filtra y no lo muestra.

---

## 9. Ciclo de vida del índice en memoria

El TreeMap es el componente que hace todo eficiente. Su ciclo de vida:

```
Inicio de la aplicación
        │
        ▼
VentaStorage() constructor
  └─► cargarIndice()
        Lee ventas.idx byte a byte
        Por cada entrada de 48 bytes:
          lee idVenta (40 bytes) → String.trim()
          lee posicion (8 bytes) → long
          indice.put(idVenta, posicion)
        TreeMap queda cargado en memoria RAM

        ──────────────────────────────────
        Durante la sesión:
          - INSERT → indice.put() + guardarIndice()
          - DELETE → indice.remove() + guardarIndice()
          - READ   → solo indice.get() (sin tocar disco)
          - UPDATE → solo toca ventas.dat, índice no cambia
        ──────────────────────────────────

        Próximo inicio de la aplicación
          → vuelve a cargar ventas.idx al TreeMap
```

---

## 10. ¿Por qué TreeMap y no HashMap?

`TreeMap` mantiene las claves ordenadas alfabéticamente. Esto permite que
`listarTodas()` devuelva las ventas en orden de ID (que al estar basados
en timestamp, equivale a orden cronológico) sin necesidad de ordenar.

`HashMap` sería O(1) en búsqueda pero sin orden garantizado.
`TreeMap` es O(log N) en búsqueda con orden garantizado — aceptable para
el volumen de un turno de caja.
