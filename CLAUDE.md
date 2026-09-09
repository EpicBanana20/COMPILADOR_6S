# Contexto del proyecto: COMPILADOR_6S

## Arquitectura general

Proyecto Java 8 (Maven) con GUI en Swing. El lexer y el parser **no usan
ANTLR/JFlex/CUP** — están escritos a mano pero dirigidos por tres tablas CSV
que actúan como el "diseño" del lenguaje:

- `C2A6.csv` — matriz de transiciones del autómata léxico.
- `C3A4_ALEXMEZA.csv` — tabla LL(1) del parser.
- `producciones_gramatica.csv` — 135 producciones de la gramática.

El motor Java (`Compilacion.java`, `Parser.java`) es genérico: interpreta esas
tablas en vez de tener la gramática hardcodeada.

**Importante:** el parser hoy **no construye AST**. Es un parser LL(1)
predictivo no recursivo (pila explícita), que solo valida la entrada y cuenta
producciones. Por eso las acciones semánticas se implementan **embebidas en
el parseo mismo** (traducción dirigida por sintaxis), no como una fase
posterior que recorre un árbol.

## Estado actual (antes de esta tarea)

- **Pila de ámbitos**: ya existe, pero solo trackea IDs de ámbito (pila de
  enteros). No guarda nombre/tipo de las variables — no hay tabla de
  símbolos real todavía.
- **Bandera declaración/ejecución**: existe y se registra en un log, pero no
  se usa todavía para validar nada.
- Los marcadores `800`/`801` (dentro de `main`, producción 82) activan y
  desactivan la bandera declaración/ejecución.
- Los marcadores `802`/`803` (dentro de `def`, producción 80) hacen
  push/pop de un ID a la pila de ámbitos.
- Exportación a Excel ya existe con 4 hojas: `TOKENS`, `Errores`,
  `CONTADORES` (por categoría léxica), `Sintaxis` (conteo de producciones).

## Reglas del lenguaje relevantes para la fase semántica

### Tipo por prefijo léxico (notación húngara obligatoria)

El tipo de todo identificador está codificado en su propio prefijo — no
requiere inferencia, se lee directo del token que ya entrega el lexer:

| Prefijo | Tipo |
|---|---|
| `@` | Cadena |
| `#B` | Binario |
| `#D` | Decimal |
| `#O` | Octal |
| `#X` | Hexadecimal |
| `$` | Real |
| `¿` | Exponencial |
| `¡` | Booleano |
| *(sin prefijo)* | Registro |

### Ámbitos anidados

- `def` puede anidarse: el cuerpo de una función se resuelve como
  `PROGRAMA`, igual que el nivel global, por lo que puede contener otro
  `reg`, otro `var` e incluso otro `def`.
- Regla de resolución de nombres: buscar en el ámbito actual, si no está
  subir al ámbito padre, y así hasta llegar al ámbito global (permite
  *shadowing* legítimo).
- Redeclaración solo se compara **dentro del mismo ámbito** (no contra
  ámbitos superiores).
- **La tabla de símbolos nunca borra filas.** Al cerrar un ámbito (pop de
  la pila), sus símbolos quedan inertes pero no se eliminan — sirven de
  historial para los reportes Excel.

## Tarea 1 — Tabla de símbolos real

Reemplazar la pila de enteros actual por una pila de mapas
(`Map<String, Simbolo>` por ámbito), sin descartar la pila de IDs existente
(se puede mantener en paralelo o fusionar).

Columnas por fila (formato exigido por el profesor, confirmado contra
ejemplo real de un compañero con el mismo lenguaje):

```
id | tipo | Clase | amb | Tarr | DimArr | NoPar | TParr
```

- `Clase` ∈ `{ var, fun, par, reg, arr }`. Usar `arr` en vez de `var` cuando
  la declaración incluye dimensiones de arreglo (ver producción 89, `J4`).
- `Tarr` / `DimArr`: solo aplican a `arr`. `Tarr` = tamaños de cada
  dimensión separados por coma (ej. `"5,8,3"`). `DimArr` = cantidad de
  dimensiones (ej. `3`). En cualquier otra `Clase`, van como `-`.
- `NoPar` / `TParr` cambian de significado según `Clase`:
  - `fun`: `NoPar` = cantidad de parámetros que recibe. `TParr` = ID del
    ámbito que abre su propio cuerpo (vínculo hacia adelante).
  - `par`: `NoPar` = posición ordinal del parámetro (1, 2, 3...). `TParr` =
    nombre de la función dueña (vínculo hacia atrás).
  - `var` / `arr` / `reg`: ambos van como `-`.

### Puntos de enganche por producción (`producciones_gramatica.csv`)

| Producción | Acción semántica |
|---|---|
| `78` — `reg id { id J1 } PROGRAMA` | Insertar fila `Clase=reg` para el nombre del registro. Cada `id` dentro de `J1` (83/84) es un campo del registro. |
| `79` — `var J2 id J4 J5` | Insertar fila `var` o `arr` (según si `J4`/producción 89 trae dimensiones). El `id` dentro de `J2` (85) es solo *lookup* de un tipo `reg` ya existente — **no genera fila propia, no se declara**. |
| `80` — `def id 802 LISTA_DE_PARAMETROS PROGRAMA 803 ; PROGRAMA`, en el marcador `802` | Insertar fila `Clase=fun` (con `NoPar` pendiente hasta procesar la lista de parámetros). Generar nuevo ID de ámbito, hacer `push`, y asignar ese ID a `TParr` de la fila de la función. |
| `1/2/3` — `LISTA_DE_PARAMETROS → ( id A1 )`, `A1 → coma id A1 \| EPSILON` | Insertar fila `Clase=par` por cada `id`, en el ámbito recién abierto, con `NoPar` = posición ordinal y `TParr` = nombre de la función. |
| `80`, marcador `803` | `pop` de la pila de ámbitos. **No tocar la tabla de símbolos** — sus filas permanecen. |
| `21` / `19` / `20` — `FACTOR → id B1` / `++ id B1` / `-- id B1` | **Punto único de chequeo de "uso"**: aquí se dispara la validación de variable no declarada. |

## Tarea 2 — Dos chequeos semánticos

1. **Variable no declarada** (código sugerido: `45`)
   Se dispara en toda producción de *uso* (21/19/20) si el `id` no aparece
   en ningún ámbito activo, recorriendo la pila de ámbitos del tope hacia
   el ámbito global.

2. **Variable declarada duplicada** (código sugerido: `46`)
   Se dispara en toda producción de *declaración* (78, 79, 80, parámetros)
   si el `id` ya existe en el **mismo** ámbito (no se sube a ámbitos
   superiores — así se permite el *shadowing*).

Ambos errores se reportan en la hoja `Errores` **ya existente** (misma
tabla que usan los errores léxicos y sintácticos), agregando el valor
`Ambito` a la columna `tipo de error`.

## Tarea 3 — Exportación a Excel

Agregar 2 hojas nuevas al exportador existente:

- **`Tabla de Simbolos`**: volcado directo de la tabla de símbolos, una fila
  por entrada, en el orden en que se insertaron.
- **`Ámbito`**: una fila por cada ID de ámbito que existió, con columnas
  `Ambito | Bin | Dec | Oct | Hex | Real | exp | Cadena | Boolean | Errores | total`.
  Se alimenta desde el **lexer** (no desde el parser): cada vez que se
  reconoce una constante o un error, se incrementa el contador
  correspondiente en la fila del ámbito activo en ese momento (tope actual
  de la pila de ámbitos). Es el mismo contador léxico global que ya existe,
  particionado por ámbito. Incluir fila `Total` al final sumando todas las
  columnas.

## Plan de trabajo sugerido (pasos separados, no todo de una vez)

1. Diseñar `Simbolo` / `TablaSimbolos` en código, sin tocar el parser
   todavía — revisar el diseño antes de integrarlo.
2. Enganchar las inserciones de declaración (`reg` / `var` / `def` /
   parámetros) y verificar con un caso simple que la tabla se llena
   correctamente, sin chequeos de error todavía.
3. Agregar el chequeo de "variable no declarada" en el punto único de uso
   (producción 21/19/20).
4. Agregar el chequeo de "variable declarada duplicada" en los puntos de
   declaración.
5. Extender el exportador Excel con las 2 hojas nuevas (`Tabla de
   Simbolos` y `Ámbito`).

## Pendiente de confirmar (no asumir, verificar con el profesor o con un caso de prueba)

- Que los marcadores `802`/`803` sean efectivamente el punto correcto para
  abrir/cerrar el ámbito de una función (hoy solo agregan/quitan números
  de la pila — falta confirmar que no haya otro mecanismo paralelo).
- Si la gramática realmente permite `def` anidado en la práctica (se
  dedujo del CSV porque el cuerpo de `def` es `PROGRAMA`, pero conviene
  probarlo corriendo el parser con un caso real).
