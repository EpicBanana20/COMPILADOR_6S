import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Compilacion {

    private CompiladorGUI gui;
    private LecturaMatriz lectorMatriz;

    // 1. DICCIONARIO DE PALABRAS RESERVADAS
    private static final Set<String> PALABRAS_RESERVADAS = Set.of(
        "if", "else", "switch", "for", "do", "while", "console.log", "forEach", "break", 
        "continue", "let", "const", "undefined", "interface", "typeof", "any", "set", 
        "get", "class", "toLowerCase", "toUpperCase", "length", "trim", "charAt", 
        "startsWith", "endsWith", "indexOf", "Includes", "slice", "replace", "split", 
        "push", "shift", "in", "of", "splice", "concat", "find", "findIndex", "filter", 
        "map", "sort", "reverse"
    );

    public Compilacion(CompiladorGUI gui, LecturaMatriz lectorMatriz) {
        this.gui = gui;
        this.lectorMatriz = lectorMatriz;
    }

    public void ejecutar() {
        // 1. Limpiamos las tablas de la interfaz antes de empezar
        gui.getModeloTokens().setRowCount(0);
        gui.getModeloErrores().setRowCount(0);
        gui.getModeloPila().setRowCount(0);

        // 2. Obtenemos el texto que escribió el usuario y la matriz
        String codigoFuente = gui.getEditorCodigo().getText();
        Map<String, Map<String, String>> matriz = lectorMatriz.getMatriz();

        if (codigoFuente.isEmpty())
            return;
        if (matriz == null || matriz.isEmpty()) {
            System.err.println("❌ Error: La matriz no está cargada.");
            return;
        }

        // 3. Estructura para contar: Map<Clasificación, Map<Elemento, Cantidad>>
        Map<String, Map<String, Integer>> contadores = new LinkedHashMap<>();

        // 4. Variables para recorrer el texto
        String estadoActual = "0";
        StringBuilder lexemaActual = new StringBuilder();
        int lineaActual = 1;

        // Añadimos un espacio final al código para obligar al autómata a procesar el último token
        codigoFuente += " ";

        // 5. EL CICLO PRINCIPAL
        for (int i = 0; i < codigoFuente.length(); i++) {
            char c = codigoFuente.charAt(i);

            // Al leer un salto de línea, pasamos a la siguiente
            if (c == '\n')
                lineaActual++;

            String columna = clasificarCaracter(c);

            Map<String, String> filaEstado = matriz.get(estadoActual);
            
            if (filaEstado == null) {
                String lexError = lexemaActual.toString();
                // Si el error salta por un \n, el token roto era de la línea anterior
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;
                
                registrarError("Error Crítico", "Estado inexistente [" + estadoActual + "]", lexError, lineaRegistro);
                registrarConteo(contadores, "Errores Críticos", lexError);
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                i--;
                if (c == '\n') lineaActual--;
                
                continue; 
            }

            String siguienteEstado = filaEstado.get(columna);

            // Validar si la celda es null o está vacía (Transición no definida)
            if (siguienteEstado == null || siguienteEstado.trim().isEmpty()) {
                String lexError = (lexemaActual.toString() + c).trim();
                // Si el error salta por un \n, el token roto era de la línea anterior
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;
                
                registrarError("Error Léxico", "Transición no definida para '" + c + "'", lexError, lineaRegistro);
                registrarConteo(contadores, "Errores Léxicos", lexError);
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                continue; // Saltamos al siguiente carácter y seguimos buscando tokens
            }

            // MANEJO DE ACEPTACIÓN POR DELIMITADOR (Estado negativo alcanzado)
            if (siguienteEstado.startsWith("-")) {
                String tokenEncontrado = siguienteEstado;
                String palabraFormada = lexemaActual.toString().trim();
                String familia = obtenerAgrupacion(tokenEncontrado);

                // Filtro de Palabras Reservadas
                if (familia.equals("Identificador") && PALABRAS_RESERVADAS.contains(palabraFormada)) {
                    familia = "Palabras reservadas";
                }

                // ==========================================
                // CORRECCIÓN DE LÍNEA: Si un salto de línea delimitó el token, 
                // el token pertenece realmente a la línea anterior.
                // ==========================================
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaRegistro });
                registrarConteo(contadores, familia, palabraFormada); // Se suma al contador
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                i--;
                if (c == '\n')
                    lineaActual--; // Al retroceder, también deshacemos el avance de línea

                continue;
            }

            // Si no estamos simplemente dando vueltas en q0 con espacios, guardamos la letra
            if (!(estadoActual.equals("0") && estadoActual.equals(siguienteEstado))) {
                lexemaActual.append(c);
            }

            estadoActual = siguienteEstado;

            // MANEJO DE ACEPTACIÓN DIRECTA (Llegamos a un estado final)
            if (estadoActual.startsWith("-")) {
                String tokenEncontrado = estadoActual;
                String palabraFormada = lexemaActual.toString().trim();
                String familia = obtenerAgrupacion(tokenEncontrado);

                // Filtro de Palabras Reservadas
                if (familia.equals("Identificador") && PALABRAS_RESERVADAS.contains(palabraFormada)) {
                    familia = "Palabras reservadas";
                }

                // ==========================================
                // CORRECCIÓN DE LÍNEA
                // ==========================================
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaRegistro });
                registrarConteo(contadores, familia, palabraFormada); // Se suma al contador

                estadoActual = "0";
                lexemaActual.setLength(0);

                i--;
                if (c == '\n')
                    lineaActual--; 
            }
        }

        // 6. VOLCAR CONTADORES A LA TABLA PILA AL FINALIZAR
        for (Map.Entry<String, Map<String, Integer>> entryClasificacion : contadores.entrySet()) {
            String clasificacion = entryClasificacion.getKey();
            for (Map.Entry<String, Integer> entryElemento : entryClasificacion.getValue().entrySet()) {
                String elemento = entryElemento.getKey();
                int cantidad = entryElemento.getValue();
                gui.getModeloPila().addRow(new Object[]{clasificacion, elemento, cantidad});
            }
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void registrarConteo(Map<String, Map<String, Integer>> contadores, String clasificacion, String elemento) {
        if (elemento == null || elemento.trim().isEmpty()) return;
        contadores.putIfAbsent(clasificacion, new LinkedHashMap<>());
        Map<String, Integer> elementos = contadores.get(clasificacion);
        elementos.put(elemento, elementos.getOrDefault(elemento, 0) + 1);
    }

    private void registrarError(String tokenError, String descripcion, String lexema, int linea) {
        gui.getModeloErrores().addRow(new Object[] { tokenError, descripcion, lexema, "Léxico", String.valueOf(linea) });
    }

    private String obtenerAgrupacion(String estadoFinal) {
    switch (estadoFinal) {
        case "-1": return "Operadores matematicos";
        case "-2": return "Operadores postfix";
        case "-3": return "Operadores de asignacion";
        case "-4": return "Operadores matematicos";
        case "-5": return "Operadores postfix";
        case "-6": return "Operadores de asignacion";
        case "-7": return "Operadores matematicos";
        case "-8": return "Operador exponente";
        case "-9": return "Operadores de asignacion";
        case "-10": return "Operadores matematicos";
        case "-11": return "Comentario grupal";
        case "-12": return "Comentario lineal";
        case "-13": return "Operadores de asignacion";
        case "-14": return "Operadores matematicos";
        case "-15": return "Operadores de asignacion";
        case "-16": return "Operadores de asignacion";
        case "-17": return "Operadores de asignacion";
        case "-18": return "Operadores relacionales";
        case "-19": return "Operadores sin igualdad de conversion de tipo";
        case "-20": return "Operadores relacionales";
        case "-21": return "Operadores de turno";
        case "-22": return "Operadores relacionales";
        case "-23": return "Operadores relacionales";
        case "-24": return "Operadores de asignacion";
        case "-25": return "Operadores relacionales";
        case "-26": return "Operadores relacionales";
        case "-27": return "Operadores de turno";
        case "-28": return "Operadores de asignacion";
        case "-29": return "Operadores de turno";
        case "-30": return "Operadores de asignacion";
        case "-31": return "Operadores logicos";
        case "-32": return "Operadores relacionales";
        case "-33": return "Operadores sin igualdad de conversion de tipo";
        case "-34": return "Operadores logicos binarios";
        case "-35": return "Operadores de asignacion";
        case "-36": return "Operadores logicos";
        case "-37": return "Operadores logicos binarios";
        case "-38": return "Operadores logicos";
        case "-39": return "Operadores logicos binarios";
        case "-40": return "Operadores de asignacion";
        case "-41": return "Operadores logicos binarios";
        case "-42": return "Operador ternario";
        case "-43": case "-44": case "-45": case "-46": 
            return "Operador de control";
        case "-47": case "-48": case "-49": case "-50": case "-51": case "-52": 
            return "Operador de agrupamiento";
        case "-53": case "-54": 
            return "Cadena";
        case "-55": return "Numerica Decimal";
        case "-56": return "Numerica Real";
        case "-57": case "-58": 
            return "Numerica Exponencial";
        case "-59": return "Numerica Binario";
        case "-60": return "Numerica Octal";
        case "-61": return "Numerica Hexadecimal";
        case "-62": return "Cadena Identificador";
        case "-63": return "Numerica Binario Identificador";
        case "-64": return "Numerica Decimal Identificador";
        case "-65": return "Numerica Octal Identificador";
        case "-66": return "Numerica Hexadecimal Identificador";
        case "-67": return "Real Identificador";
        case "-68": return "Exponencial Identificador";
        case "-69": return "Booleana Identificador";
        case "-70": return "Palabras Reservadas";

        default: 
            return "Agrupación Desconocida (" + estadoFinal + ")";
    }
}

    private String clasificarCaracter(char c) {
        if (c == ' ') return "espacio";
        if (c == '\n') return "\\n";
        if (c == '\t') return "\\t";
        if (c == '\r') return "\\r";

        if (c == '0' || c == '1') return "[0-1]";
        if (c >= '2' && c <= '7') return "[2-7]";
        if (c == '8' || c == '9') return "[8-9]";

        if (c == 'a' || c == 'A') return "[aA]";
        if (c == 'b') return "b";
        if (c == 'B') return "B";
        if (c == 'c' || c == 'C') return "[cC]";
        if (c == 'd') return "d";
        if (c == 'D') return "D";
        if (c == 'e' || c == 'E') return "[eE]";
        if (c == 'f' || c == 'F') return "[fF]";

        if ((c >= 'G' && c <= 'K') || (c >= 'g' && c <= 'k')) return "[G-Kg-k]";
        if (c == 'l' || c == 'L') return "[lL]";
        if (c == 'M' || c == 'N' || c == 'Ñ' || c == 'm' || c == 'n' || c == 'ñ') return "[M-Nm-n]";
        if (c == 'O') return "O";
        if (c >= 'P' && c <= 'W') return "[P-W]";
        if (c == 'X') return "X";
        if (c >= 'Y' && c <= 'Z') return "[Y-Z]";
        if (c == 'o') return "o";
        if (c >= 'p' && c <= 'w') return "[p-w]";
        if (c == 'x') return "x";
        if (c >= 'y' && c <= 'z') return "[y-z]";

        if (c == ',') return "coma";  
        if (c == '\"') return "comillas";
        
        String simbolos = "+-*/%=<>!&|^~?.;:{}[]()'_@#$¿¡";
        if (simbolos.indexOf(c) != -1) {
            return String.valueOf(c);
        }

        return "oc";
    }
}