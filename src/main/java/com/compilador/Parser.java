package com.compilador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Parser {

    private LecturaMatriz lectorMatriz;
    private CompiladorGUI gui;
    private List<Token> tokens;
    private int posicionActual;
    private Stack<String> pila;
    private List<String> erroresSintacticos;
    private Map<String, Integer> contadoresDiagramasPrincipales;

    private static final Map<String, String> CODIGO_A_TOKEN = new LinkedHashMap<>();

    static {
        CODIGO_A_TOKEN.put("-1", "+");
        CODIGO_A_TOKEN.put("-2", "++");
        CODIGO_A_TOKEN.put("-3", "+=");
        CODIGO_A_TOKEN.put("-4", "-");
        CODIGO_A_TOKEN.put("-5", "--");
        CODIGO_A_TOKEN.put("-6", "-=");
        CODIGO_A_TOKEN.put("-7", "*");
        CODIGO_A_TOKEN.put("-8", "**");
        CODIGO_A_TOKEN.put("-9", "*=");
        CODIGO_A_TOKEN.put("-10", "/");
        CODIGO_A_TOKEN.put("-11", "/**/");
        CODIGO_A_TOKEN.put("-12", "//");
        CODIGO_A_TOKEN.put("-13", "/=");
        CODIGO_A_TOKEN.put("-14", "%");
        CODIGO_A_TOKEN.put("-15", "%=");
        CODIGO_A_TOKEN.put("-16", "=");
        CODIGO_A_TOKEN.put("-17", "=>");
        CODIGO_A_TOKEN.put("-18", "==");
        CODIGO_A_TOKEN.put("-19", "===");
        CODIGO_A_TOKEN.put("-20", "<");
        CODIGO_A_TOKEN.put("-21", "<<");
        CODIGO_A_TOKEN.put("-22", "<=");
        CODIGO_A_TOKEN.put("-23", "<>");
        CODIGO_A_TOKEN.put("-24", "<<=");
        CODIGO_A_TOKEN.put("-25", ">");
        CODIGO_A_TOKEN.put("-26", ">=");
        CODIGO_A_TOKEN.put("-27", ">>");
        CODIGO_A_TOKEN.put("-28", ">>=");
        CODIGO_A_TOKEN.put("-29", ">>>");
        CODIGO_A_TOKEN.put("-30", ">>>=");
        CODIGO_A_TOKEN.put("-31", "!");
        CODIGO_A_TOKEN.put("-32", "!=");
        CODIGO_A_TOKEN.put("-33", "!==");
        CODIGO_A_TOKEN.put("-34", "&");
        CODIGO_A_TOKEN.put("-35", "&=");
        CODIGO_A_TOKEN.put("-36", "&&");
        CODIGO_A_TOKEN.put("-37", "|");
        CODIGO_A_TOKEN.put("-38", "||");
        CODIGO_A_TOKEN.put("-39", "^");
        CODIGO_A_TOKEN.put("-40", "^=");
        CODIGO_A_TOKEN.put("-41", "~");
        CODIGO_A_TOKEN.put("-42", "?");
        CODIGO_A_TOKEN.put("-43", ",");
        CODIGO_A_TOKEN.put("-44", ".");
        CODIGO_A_TOKEN.put("-45", ";");
        CODIGO_A_TOKEN.put("-46", ":");
        CODIGO_A_TOKEN.put("-47", "{");
        CODIGO_A_TOKEN.put("-48", "}");
        CODIGO_A_TOKEN.put("-49", "[");
        CODIGO_A_TOKEN.put("-50", "]");
        CODIGO_A_TOKEN.put("-51", "(");
        CODIGO_A_TOKEN.put("-52", ")");
        CODIGO_A_TOKEN.put("-53", "\"");
        CODIGO_A_TOKEN.put("-54", "'");

        CODIGO_A_TOKEN.put("-55", "Const_Decimal");
        CODIGO_A_TOKEN.put("-56", "Const_real");
        CODIGO_A_TOKEN.put("-57", "Const_Exponencial");
        CODIGO_A_TOKEN.put("-58", "Const_cadena");
        CODIGO_A_TOKEN.put("-59", "Binario");
        CODIGO_A_TOKEN.put("-60", "Const_Octal");
        CODIGO_A_TOKEN.put("-61", "Const_Hexadecimal");
        CODIGO_A_TOKEN.put("-62", "id"); // Cadena Identificador
        CODIGO_A_TOKEN.put("-63", "id"); // Binario Identificador
        CODIGO_A_TOKEN.put("-64", "id"); // Decimal Identificador
        CODIGO_A_TOKEN.put("-65", "id"); // Octal Identificador
        CODIGO_A_TOKEN.put("-66", "id"); // Hexadecimal Identificador
        CODIGO_A_TOKEN.put("-67", "id"); // Real Identificador
        CODIGO_A_TOKEN.put("-68", "id"); // Exponencial Identificador
        CODIGO_A_TOKEN.put("-69", "id"); // Booleana Identificador
        CODIGO_A_TOKEN.put("-70", "id");

        CODIGO_A_TOKEN.put("-71", "if");
        CODIGO_A_TOKEN.put("-72", "else");
        CODIGO_A_TOKEN.put("-73", "elseif");
        CODIGO_A_TOKEN.put("-74", "switch");
        CODIGO_A_TOKEN.put("-75", "case");
        CODIGO_A_TOKEN.put("-76", "default");
        CODIGO_A_TOKEN.put("-77", "for");
        CODIGO_A_TOKEN.put("-78", "do");
        CODIGO_A_TOKEN.put("-79", "while");
        CODIGO_A_TOKEN.put("-80", "break");
        CODIGO_A_TOKEN.put("-81", "return");
        CODIGO_A_TOKEN.put("-82", "reg");
        CODIGO_A_TOKEN.put("-83", "var");
        CODIGO_A_TOKEN.put("-84", "def");
        CODIGO_A_TOKEN.put("-85", "Console.read");
        CODIGO_A_TOKEN.put("-86", "Console.log");
        CODIGO_A_TOKEN.put("-87", "CLEAR");
        CODIGO_A_TOKEN.put("-88", "SQRT");
        CODIGO_A_TOKEN.put("-89", "POW");
        CODIGO_A_TOKEN.put("-90", "SQRTV");
        CODIGO_A_TOKEN.put("-91", "STRLEN");
        CODIGO_A_TOKEN.put("-92", "sin");
        CODIGO_A_TOKEN.put("-93", "cos");
        CODIGO_A_TOKEN.put("-94", "tan");
        CODIGO_A_TOKEN.put("-95", "chr");
        CODIGO_A_TOKEN.put("-96", "pred");
        CODIGO_A_TOKEN.put("-97", "succ");
        CODIGO_A_TOKEN.put("-98", "inc");
        CODIGO_A_TOKEN.put("-99", "dec");
        CODIGO_A_TOKEN.put("-100", "sqr");
        CODIGO_A_TOKEN.put("-101", "copy");
        CODIGO_A_TOKEN.put("-102", "val");
        CODIGO_A_TOKEN.put("-103", "str");
        CODIGO_A_TOKEN.put("-104", "console.log");
        CODIGO_A_TOKEN.put("-105", "true");
        CODIGO_A_TOKEN.put("-106", "false");
        CODIGO_A_TOKEN.put("-107", "null");
        CODIGO_A_TOKEN.put("-108", "continue");
        CODIGO_A_TOKEN.put("-109", "class");
        CODIGO_A_TOKEN.put("-110", "main");
    }

    public static class Token {
        public String token;
        public String lexema;
        public int linea;

        public Token(String token, String lexema, int linea) {
            this.token = token;
            this.lexema = lexema;
            this.linea = linea;
        }

        @Override
        public String toString() {
            return token + "('" + lexema + "')";
        }
    }

    public Parser(LecturaMatriz lectorMatriz, CompiladorGUI gui) {
        this.lectorMatriz = lectorMatriz;
        this.gui = gui;
        this.tokens = new ArrayList<>();
        this.pila = new Stack<>();
        this.erroresSintacticos = new ArrayList<>();
        this.contadoresDiagramasPrincipales = new java.util.LinkedHashMap<>();
    }

    public void ejecutar(List<Token> tokensRecibidos) {
        this.tokens = tokensRecibidos;
        this.posicionActual = 0;
        this.pila = new Stack<>();
        this.erroresSintacticos = new ArrayList<>();
        this.contadoresDiagramasPrincipales = new java.util.LinkedHashMap<>();

        System.out.println("\n========== INICIO DEL ANALISIS SINTACTICO ==========");
        System.out.println("Total de tokens recibidos: " + tokens.size());

        pila.push("$");
        pila.push("PROGRAMA");

        System.out.println("\n--- Pila Inicial ---");
        System.out.println(pila);

        boolean analisisExitoso = true;
        int pasos = 0;
        int maxPasos = 1000;

        while (!pila.isEmpty() && pasos < maxPasos) {
            pasos++;
            String cimaPila = pila.peek();
            Token tokenActual = (posicionActual < tokens.size()) ? tokens.get(posicionActual) : null;

            String simboloActual = (tokenActual != null) ? tokenActual.token : "$";
            String simboloActualNormalizado = simboloActual.startsWith("-") ? codigoAToken(simboloActual) : simboloActual;
            if (simboloActualNormalizado == null) simboloActualNormalizado = simboloActual;

            System.out.println("\n--- Paso " + pasos + " ---");
            List<String> pilaNormalizada = new ArrayList<>();
            for (String s : pila) {
                pilaNormalizada.add(normalizarNoTerminal(s));
            }
            System.out.println("Pila: " + pilaNormalizada);
            System.out.println("Token actual: " + (tokenActual != null ? simboloActualNormalizado + "('" + tokenActual.lexema + "')" : "FIN DE ENTRADA"));

            if (cimaPila.equals("$")) {
                if (simboloActual.equals("$")) {
                    System.out.println(">>> ACCEPT: Análisis completado exitosamente!");
                    break;
                } else {
                    System.out.println(">>> ERROR: Se esperaba EOF pero se encontró: " + simboloActual);
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) + ": Se esperaba FIN DE ARCHIVO");
                    if (gui != null) {
                        gui.getModeloErrores().addRow(new Object[] { "SYNTAX", "Se esperaba FIN DE ARCHIVO", "$", "Sintáctico", String.valueOf(tokenActual != null ? tokenActual.linea : 0) });
                    }
                    analisisExitoso = false;
                    break;
                }
            }

            if (esNoTerminal(cimaPila)) {
                Integer codigoProduccion = obtenerCodigoProduccion(cimaPila, simboloActualNormalizado);

                if (codigoProduccion != null) {
                    if (esCodigoError(codigoProduccion)) {
                        String descripcion = obtenerDescripcionErrorSintactico(codigoProduccion);
                        int lineaError = (tokenActual != null) ? tokenActual.linea : 0;
                        
                        System.out.println(">>> ERROR SINTACTICO [" + codigoProduccion + "]: " + descripcion);
                        erroresSintacticos.add("Error " + codigoProduccion + " en línea " + lineaError + ": " + descripcion);
                        if (gui != null) {
                            gui.getModeloErrores().addRow(new Object[] { String.valueOf(codigoProduccion), descripcion, simboloActual, "Sintáctico", String.valueOf(lineaError) });
                        }
                        analisisExitoso = false;
                        break;
                    }
                    
                    pila.pop();
                    List<String> produccion = lectorMatriz.getProduccion(codigoProduccion);
                    String noTerminalNormalizado = normalizarNoTerminal(cimaPila);

                    if (esDiagramaPrincipal(noTerminalNormalizado)) {
                        contadoresDiagramasPrincipales.put(noTerminalNormalizado, 
                            contadoresDiagramasPrincipales.getOrDefault(noTerminalNormalizado, 0) + 1);
                    }

                    System.out.println("Aplicando producción " + codigoProduccion + ": " + noTerminalNormalizado + " -> " + 
                        (produccion.isEmpty() ? "EPSILON" : produccion));

                    if (!produccion.isEmpty()) {
                        for (int i = produccion.size() - 1; i >= 0; i--) {
                            if (!produccion.get(i).equals("EPSILON")) {
                                String simboloProduccion = normalizarSimboloProduccion(produccion.get(i));
                                pila.push(simboloProduccion);
                            }
                        }
                    }
                } else {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    System.out.println(">>> ERROR: No hay producción para " + cimaNorm + " con token " + simboloActualNormalizado);
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) +
                        ": No se esperaba '" + simboloActualNormalizado + "' después de '" + cimaNorm + "'");
                    if (gui != null) {
                        gui.getModeloErrores().addRow(new Object[] { "SYNTAX", "No se esperaba '" + simboloActualNormalizado + "' después de '" + cimaNorm + "'", tokenActual != null ? tokenActual.lexema : simboloActual, "Sintáctico", String.valueOf(tokenActual != null ? tokenActual.linea : 0) });
                    }
                    analisisExitoso = false;
                    break;
                }
            } else {
                String cimaNormalizada = normalizarSimboloComparar(cimaPila);
                String tokenNormalizado = normalizarToken(simboloActualNormalizado);

                boolean coinciden = cimaNormalizada.equals(tokenNormalizado);

                if (coinciden) {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    System.out.println(">>> MATCH: " + cimaNorm + " coincide con " + simboloActualNormalizado);
                    pila.pop();
                    posicionActual++;
                } else {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    System.out.println(">>> ERROR: Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActualNormalizado + "'");
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) +
                        ": Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActualNormalizado + "'");
                    if (gui != null) {
                        gui.getModeloErrores().addRow(new Object[] { "SYNTAX", "Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActualNormalizado + "'", tokenActual != null ? tokenActual.lexema : simboloActual, "Sintáctico", String.valueOf(tokenActual != null ? tokenActual.linea : 0) });
                    }
                    analisisExitoso = false;
                    break;
                }
            }
        }

        if (pasos >= maxPasos) {
            System.out.println(">>> ERROR: Límite de pasos alcanzado");
            erroresSintacticos.add("Error: Límite de pasos del parser alcanzado");
        }

        System.out.println("\n========== RESULTADO DEL ANALISIS SINTACTICO ==========");
        if (analisisExitoso) {
            System.out.println(">>> EL ANALISIS SINTACTICO FUE EXITOSO <<<");
        } else {
            System.out.println(">>> SE ENCONTRARON ERRORES SINTACTICOS <<<");
        }
        System.out.println("Total de pasos: " + pasos);
        System.out.println("Tokens procesados: " + posicionActual + " de " + tokens.size());

        if (!erroresSintacticos.isEmpty()) {
            System.out.println("\n--- Errores Sintácticos ---");
            for (String error : erroresSintacticos) {
                System.out.println("  - " + error);
            }
        }
    }

    private String normalizarNoTerminal(String noTerminal) {
        Map<String, Map<String, Integer>> matrizParser = lectorMatriz.getMatrizParser();
        
        if (matrizParser.containsKey(noTerminal)) {
            return noTerminal;
        }
        
        if (noTerminal.equals("EXP")) return "EXP_PAS";
        if (noTerminal.equals("STATU")) return "STATU";
        if (noTerminal.equals("FACTOR")) return "FACTOR";
        if (noTerminal.equals("NUMERICA")) return "CONST_NUMERICA";
        if (noTerminal.equals("CONST")) return "CONST_NUMERICA";
        if (noTerminal.equals("CONSTANTE")) return "CONSTANTE_S_SIGNO";
        if (noTerminal.equals("TERMINO")) return "TERMINO_PASCAL";
        if (noTerminal.equals("S/SIGNO")) return "CONSTANTE_S_SIGNO";
        if (noTerminal.equals("SIMPLE")) return "SIMPLE_EXP_PASCAL";
        if (noTerminal.equals("ELEVACION")) return "ELEVACION";
        if (noTerminal.equals("TERMINO_PASCAL")) return "TERMINO_PASCAL";
        
        for (String key : matrizParser.keySet()) {
            if (key.startsWith(noTerminal) || noTerminal.startsWith(key.substring(0, Math.min(3, key.length())))) {
                return key;
            }
        }
        
        return noTerminal;
    }

    private boolean esNoTerminal(String simbolo) {
        Map<String, Map<String, Integer>> matrizParser = lectorMatriz.getMatrizParser();
        String normalizado = normalizarNoTerminal(simbolo);
        
        if (matrizParser.containsKey(normalizado)) {
            return true;
        }
        
        return false;
    }

    private boolean esCodigoError(int codigo) {
        return codigo >= 512 && codigo <= 544;
    }

    private String obtenerDescripcionErrorSintactico(int codigoError) {
        switch (codigoError) {
            case 512: return "Se esperaba una expresión";
            case 513: return "Se esperaba un término";
            case 514: return "Se esperaba un factor (constante, identificador, expresión, función o paréntesis)";
            case 515: return "Se esperaba una constante numérica (Binario, Decimal, Octal o Hexadecimal)";
            case 516: return "Se esperaba una constante (numérica, real, cadena, true, false, exponencial o null)";
            case 517: return "Se esperaba una constante con o sin signo";
            case 518: return "Se esperaba una declaración válida (reg, var, def, id, main)";
            case 519: return "Se esperaba una sentencia válida o expresión";
            case 520: return "Se esperaba '[' para iniciar arreglo";
            case 521: return "Se esperaba un operador de asignación (=, +=, -=, *=, /=)";
            case 522: return "Se esperaba el nombre de una función";
            case 523: return "Se esperaba '(' para iniciar la lista de parámetros";
            case 524: return "Se esperaba ',' o ')'";
            case 525: return "Se esperaba ',' o ']'";
            case 526: return "Se esperaba ',' o ';'";
            case 527: return "Se esperaba ',' o '}'";
            case 528: return "Se esperaba ';' o '}'";
            case 529: return "Se esperaba ';' o ':'";
            case 530: return "Se esperaba un operador o continuación válida después del identificador";
            case 531: return "Se esperaba un operador de asignación o cierre";
            case 532: return "Se esperaba '?' o continuación válida";
            case 533: return "Se esperaba una expresión o ')'";
            case 534: return "Se esperaba '^' o continuación válida";
            case 535: return "Se esperaba un operador multiplicativo (*, /, #, %) o continuación válida";
            case 536: return "Se esperaba un operador aditivo (+, -, <<, >>, >>>) o continuación válida";
            case 537: return "Se esperaba un operador relacional (<, >, <=, >=, ==, !=) o continuación válida";
            case 538: return "Se esperaba '&&' o '&' o continuación válida";
            case 539: return "Se esperaba '||' o '|' o continuación válida";
            case 540: return "Se esperaba 'reg' o un identificador";
            case 541: return "Se esperaba '[' o continuación válida";
            case 542: return "Se esperaba 'elseif', 'else' o continuación válida";
            case 543: return "Se esperaba 'case', 'default' o '}'";
            case 544: return "Fin de archivo inesperado: el programa está incompleto";
            default: return "Error sintáctico (código: " + codigoError + ")";
        }
    }

    private Integer obtenerCodigoProduccion(String noTerminal, String token) {
        Map<String, Map<String, Integer>> matrizParser = lectorMatriz.getMatrizParser();
        String noTerminalNormalizado = normalizarNoTerminal(noTerminal);
        Map<String, Integer> reglas = matrizParser.get(noTerminalNormalizado);

        if (reglas != null) {
            if (reglas.containsKey(token)) {
                return reglas.get(token);
            }

            String tokenNormalizado = normalizarToken(token);
            if (reglas.containsKey(tokenNormalizado)) {
                return reglas.get(tokenNormalizado);
            }

            String nombreToken = codigoAToken(token);
            if (nombreToken != null && reglas.containsKey(nombreToken)) {
                return reglas.get(nombreToken);
            }

            if (tokenNormalizado.startsWith("Const_")) {
                String tokenBase = tokenNormalizado.replace("Const_", "");
                if (reglas.containsKey(tokenBase)) {
                    return reglas.get(tokenBase);
                }
            }

            if (token.startsWith("-")) {
                String[] partes = token.split("_");
                if (partes.length > 0 && reglas.containsKey(partes[0])) {
                    return reglas.get(partes[0]);
                }

                if (nombreToken != null && reglas.containsKey(nombreToken)) {
                    return reglas.get(nombreToken);
                }
            }
        }
        return null;
    }

    private String codigoAToken(String codigo) {
        return CODIGO_A_TOKEN.get(codigo);
    }

    private String normalizarSimboloProduccion(String simbolo) {
        String result = simbolo.replace("Á", "A").replace("É", "E").replace("Í", "I")
                           .replace("Ó", "O").replace("Ú", "U").replace("á", "a")
                           .replace("é", "e").replace("í", "i").replace("ó", "o")
                           .replace("ú", "u");
        
        result = result.replace("TÉRMINO", "TERMINO");
        
        if (result.contains(" ") && !result.contains("_")) {
            result = result.replace(" ", "_");
        }
        
        result = result.replace("CONST_NUMÉRICA", "CONST_NUMERICA");
        
        return result;
    }
    
    private String normalizarSimboloComparar(String simbolo) {
        if (simbolo.equals("Const") || simbolo.equals("Decimal")) return "Const_Decimal";
        if (simbolo.equals("real")) return "Const_real";
        if (simbolo.equals("cadena")) return "Const_cadena";
        if (simbolo.equals("Exponencial")) return "Const_Exponencial";
        if (simbolo.equals("Binario")) return "Binario";
        if (simbolo.equals("Octal")) return "Const_Octal";
        if (simbolo.equals("Hexadecimal")) return "Const_Hexadecimal";
        
        if (simbolo.equals("true")) return "true";
        if (simbolo.equals("false")) return "false";
        
        if (simbolo.startsWith("(")) return "(";
        if (simbolo.startsWith("[")) return "[";
        
        return simbolo;
    }

    private String normalizarToken(String token) {
        if (token.equals("id")) {
            return "id";
        }
        
        if (token.equals("true") || token.equals("false")) {
            return token;
        }
        
        if (token.equals("null")) {
            return token;
        }
        
        if (token.equals(",")) {
            return "coma";
        }
        
        if (token.equals("-55")) {
            return "Const_Decimal";
        }
        
        if (token.equals("-56")) {
            return "Const_real";
        }
        
        if (token.equals("-57")) {
            return "Const_Exponencial";
        }
        
        if (token.equals("-58")) {
            return "Const_cadena";
        }
        
        if (token.equals("-59")) {
            return "Binario";
        }
        
        if (token.equals("-60")) {
            return "Const_Octal";
        }
        
        if (token.equals("-61")) {
            return "Const_Hexadecimal";
        }
        
        return token;
    }

    public List<String> getErroresSintacticos() {
        return erroresSintacticos;
    }

    public Map<String, Integer> getContadoresDiagramasPrincipales() {
        return contadoresDiagramasPrincipales;
    }

    public int getTotalErroresSintacticos() {
        return erroresSintacticos.size();
    }

    private boolean esDiagramaPrincipal(String noTerminal) {
        String[] diagramasPrincipales = {
            "PROGRAMA", "STATU", "OR", "AND", "EXP_PAS", "SIMPLE_EXP_PASCAL",
            "TERMINO_PASCAL", "ELEVACION", "FACTOR", "CONSTANTE_S_SIGNO",
            "DECLARACION_CONSTANTES", "CONST_NUMERICA", "ARR", "ASIG", "FUNCION",
            "LISTA_DE_PARAMETROS"
        };
        for (String diag : diagramasPrincipales) {
            if (noTerminal.equals(diag)) {
                return true;
            }
        }
        return false;
    }
}