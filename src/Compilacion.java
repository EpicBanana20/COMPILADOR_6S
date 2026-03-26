import javax.swing.table.DefaultTableModel;
import java.util.Map;

public class Compilacion {

    private CompiladorGUI gui;
    private LecturaMatriz lectorMatriz;

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

        if (codigoFuente.isEmpty()) return;
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

        // 4. EL CICLO PRINCIPAL (El corazón de tu compilador)
        for (int i = 0; i < codigoFuente.length(); i++) {
            char c = codigoFuente.charAt(i);

            // Contamos las líneas para saber dónde ocurren los errores/tokens
            if (c == '\n') lineaActual++;

            // Clasificamos el carácter para saber qué columna de la matriz buscar
            String columna = clasificarCaracter(c);

            // Buscamos a qué estado debemos saltar
            Map<String, String> filaEstado = matriz.get(estadoActual);
            if (filaEstado == null) {
                registrarError("Error Crítico", "Estado inexistente [" + estadoActual + "]", lexemaActual.toString(), lineaActual);
                break;
            }

            String siguienteEstado = filaEstado.get(columna);

            // MANEJO DE ERRORES: Si la celda está vacía en tu CSV
            if (siguienteEstado == null || siguienteEstado.trim().isEmpty()) {
                // Solo registramos error si estábamos armando una palabra
                if (lexemaActual.length() > 0) {
                    registrarError("ERR_LEX", "Transición vacía desde q" + estadoActual + " con '" + c + "'", lexemaActual.toString() + c, lineaActual);
                } else if (c != ' ' && c != '\n' && c != '\t' && c != '\r') {
                    // Si el carácter en sí mismo es ilegal y no estábamos en ninguna palabra
                    registrarError("ERR_LEX", "Carácter no válido", String.valueOf(c), lineaActual);
                }
                
                // Reiniciamos la máquina para intentar seguir leyendo lo que queda del texto
                estadoActual = "0";
                lexemaActual.setLength(0);
                continue;
            }

            // Si llegamos aquí, la transición es válida.
            // Si no estamos simplemente dando vueltas en q0 con espacios, guardamos la letra
            if (!(estadoActual.equals("0") && estadoActual.equals(siguienteEstado))) {
                lexemaActual.append(c);
            }

            estadoActual = siguienteEstado;

            // MANEJO DE ACEPTACIÓN: ¿Llegamos a un estado final (número negativo)?
            if (estadoActual.startsWith("-")) {
                String tokenEncontrado = estadoActual;
                String palabraFormada = lexemaActual.toString().trim();

                // 1. Lo mandamos a la tabla de Tokens
                gui.getModeloTokens().addRow(new Object[]{tokenEncontrado, palabraFormada, lineaActual});
                
                // 2. Lo mandamos a la tabla de la Pila Secuencial
                String familia = obtenerAgrupacion(tokenEncontrado);
                gui.getModeloPila().addRow(new Object[]{familia, palabraFormada});

                // 3. Reiniciamos la máquina para buscar la siguiente palabra
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                // TRUCO IMPORTANTE: Si el autómata aceptó porque leyó un separador (ej. espacio o salto),
                // tenemos que retroceder el lector un paso para no "comernos" ese separador.
                // *Nota: Esto depende de cómo diseñaste tu autómata. Si tus estados finales se alcanzan 
                // CON el delimitador, descontamos i.
                i--; 
                if (c == '\n') lineaActual--; // Ajustamos la línea si retrocedimos un salto
            }
        }
    }

    private void registrarError(String tokenError, String descripcion, String lexema, int linea) {
        gui.getModeloErrores().addRow(new Object[]{tokenError, descripcion, lexema, "Léxico", String.valueOf(linea)});
    }

    // --- EL DICCIONARIO DE FAMILIAS ---
    // AQUI TENDRÁS QUE CAMBIAR ESTOS NÚMEROS POR LOS TUYOS REALES LUEGO
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

    // --- EL CLASIFICADOR DE CARACTERES (El que hicimos al principio) ---
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
        if (c == 'M' || c == 'N' || c == 'Ñ' || c == 'm' || c == 'n' || c == 'ñ') return "[M-Ñm-ñ]";
        if (c == 'O') return "O";
        if (c >= 'P' && c <= 'W') return "[P-W]";
        if (c == 'X') return "X";
        if (c >= 'Y' && c <= 'Z') return "[Y-Z]";
        if (c == 'o') return "o";
        if (c >= 'p' && c <= 'w') return "[p-w]";
        if (c == 'x') return "x";
        if (c >= 'y' && c <= 'z') return "[y-z]";

        // Símbolos
        if (c == ',') return "coma"; // Acuérdate de cambiar esto en tu CSV como te mencioné antes
        
        String simbolos = "+-*/%=<>!&|^~?.;:{}[]()\"'_@#$¿¡";
        if (simbolos.indexOf(c) != -1) {
            return String.valueOf(c);
        }

        return "oc";
    }
}