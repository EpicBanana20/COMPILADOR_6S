import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.Set; // <-- Importación necesaria para las palabras reservadas

public class Compilacion {

    private CompiladorGUI gui;
    private LecturaMatriz lectorMatriz;

    // 1. DICCIONARIO DE PALABRAS RESERVADAS
    // Usamos un Set porque es extremadamente rápido para buscar palabras
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

        // 3. Variables para recorrer el texto
        String estadoActual = "0";
        StringBuilder lexemaActual = new StringBuilder();
        int lineaActual = 1;

        // Añadimos un espacio final al código para obligar al autómata a procesar el último token
        codigoFuente += " ";

        // 4. EL CICLO PRINCIPAL
        for (int i = 0; i < codigoFuente.length(); i++) {
            char c = codigoFuente.charAt(i);

            if (c == '\n')
                lineaActual++;

            String columna = clasificarCaracter(c);

            Map<String, String> filaEstado = matriz.get(estadoActual);
            if (filaEstado == null) {
                registrarError("Error Crítico", "Estado inexistente [" + estadoActual + "]", lexemaActual.toString(), lineaActual);
                break;
            }

            String siguienteEstado = filaEstado.get(columna);

            // 🛠️ SOLUCIÓN AL NULLPOINTEREXCEPTION: Validar si la celda es null o está vacía
            if (siguienteEstado == null || siguienteEstado.trim().isEmpty()) {
                registrarError("Error Léxico", "Transición no definida para '" + c + "'", lexemaActual.toString() + c, lineaActual);
                estadoActual = "0";
                lexemaActual.setLength(0);
                continue; // Saltamos al siguiente carácter para no quedarnos atrapados
            }

            // MANEJO DE ACEPTACIÓN POR DELIMITADOR (Estado negativo alcanzado)
            if (siguienteEstado.startsWith("-")) {
                String tokenEncontrado = siguienteEstado;
                String palabraFormada = lexemaActual.toString().trim();
                String familia = obtenerAgrupacion(tokenEncontrado);

                // 🧠 MAGIA: Filtro de Palabras Reservadas
                if (familia.equals("Identificador") && PALABRAS_RESERVADAS.contains(palabraFormada)) {
                    familia = "Palabra Reservada";
                }

                gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaActual });
                gui.getModeloPila().addRow(new Object[] { familia, palabraFormada });
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                i--;
                if (c == '\n')
                    lineaActual--;

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

                // 🧠 MAGIA: Filtro de Palabras Reservadas (Segunda validación)
                if (familia.equals("Identificador") && PALABRAS_RESERVADAS.contains(palabraFormada)) {
                    familia = "Palabra Reservada";
                }

                gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaActual });
                gui.getModeloPila().addRow(new Object[] { familia, palabraFormada });

                estadoActual = "0";
                lexemaActual.setLength(0);

                i--;
                if (c == '\n')
                    lineaActual--; 
            }
        }
    }

    private void registrarError(String tokenError, String descripcion, String lexema, int linea) {
        gui.getModeloErrores().addRow(new Object[] { tokenError, descripcion, lexema, "Léxico", String.valueOf(linea) });
    }

    private String obtenerAgrupacion(String estadoFinal) {
        switch (estadoFinal) {
            case "-70": return "Identificador";
            case "-11": return "Comentario Multilínea";
            case "-14": return "Número Entero";
            case "-15": return "Número Real";
            case "-40": return "Operador Aritmético";
            default: return "Agrupación Desconocida (" + estadoFinal + ")";
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