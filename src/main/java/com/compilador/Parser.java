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
    private boolean areaDeclaracion;
    private List<String> logAreas;
    private Stack<Integer> pilaAmbitos;
    private int contadorAmbitos;
    private List<String> logAmbitos;
    private List<int[]> eventosAmbito;
    private Map<Integer, Integer> erroresAmbitoPorAmbito;

    private TablaSimbolos tablaSimbolos;
    private Token ultimoIdConsumido;
    private Token ultimoConstDecimalConsumido;
    private Simbolo funcionActual;
    private int contadorParametros;
    private Simbolo variableActual;
    private List<String> dimsBufferActual;

    private static final Map<String, String> CODIGO_A_TOKEN = new LinkedHashMap<>();

    private static final Map<String, String> CODIGO_A_TIPO = new LinkedHashMap<>();
    static {
        CODIGO_A_TIPO.put("-62", "cadena");
        CODIGO_A_TIPO.put("-63", "bin");
        CODIGO_A_TIPO.put("-64", "dec");
        CODIGO_A_TIPO.put("-65", "oct");
        CODIGO_A_TIPO.put("-66", "hex");
        CODIGO_A_TIPO.put("-67", "real");
        CODIGO_A_TIPO.put("-68", "exp");
        CODIGO_A_TIPO.put("-69", "bool");
        CODIGO_A_TIPO.put("-70", "registro");
    }

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
        CODIGO_A_TOKEN.put("-111", "#");
        CODIGO_A_TOKEN.put("-112", "concat");
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
        this.areaDeclaracion = true;
        this.logAreas = new ArrayList<>();
        this.pilaAmbitos = new Stack<>();
        this.contadorAmbitos = 0;
        this.logAmbitos = new ArrayList<>();
        this.eventosAmbito = new ArrayList<>();
        this.erroresAmbitoPorAmbito = new LinkedHashMap<>();
        this.tablaSimbolos = new TablaSimbolos();
        this.dimsBufferActual = new ArrayList<>();
    }

    public void ejecutar(List<Token> tokensRecibidos) {
        this.tokens = tokensRecibidos;
        this.posicionActual = 0;
        this.pila = new Stack<>();
        this.erroresSintacticos = new ArrayList<>();
        this.contadoresDiagramasPrincipales = new java.util.LinkedHashMap<>();
        this.areaDeclaracion = true;
        this.logAreas = new ArrayList<>();
        this.pilaAmbitos = new Stack<>();
        this.contadorAmbitos = 0;
        this.logAmbitos = new ArrayList<>();
        this.eventosAmbito = new ArrayList<>();
        this.erroresAmbitoPorAmbito = new LinkedHashMap<>();
        this.tablaSimbolos = new TablaSimbolos();
        this.ultimoIdConsumido = null;
        this.ultimoConstDecimalConsumido = null;
        this.funcionActual = null;
        this.contadorParametros = 0;
        this.variableActual = null;
        this.dimsBufferActual = new ArrayList<>();

        pila.push("$");
        pila.push("PROGRAMA");

        pilaAmbitos.push(contadorAmbitos);
        logAmbitos.add("Creación ámbito: [" + contadorAmbitos + ",1]");
        logAmbitos.add("Pila -> " + formatoPilaAmbitos());
        eventosAmbito.add(new int[] { contadorAmbitos, 1, 1 });

        boolean analisisExitoso = true;
        int pasos = 0;

        while (!pila.isEmpty()) {
            pasos++;
            String cimaPila = pila.peek();
            Token tokenActual = (posicionActual < tokens.size()) ? tokens.get(posicionActual) : null;

            String simboloActual = (tokenActual != null) ? tokenActual.token : "$";
            String simboloActualNormalizado = simboloActual.startsWith("-") ? codigoAToken(simboloActual) : simboloActual;
            if (simboloActualNormalizado == null) simboloActualNormalizado = simboloActual;

            if (cimaPila.equals("800")) {
                pila.pop();
                areaDeclaracion = false;
                int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
                String linea1 = "Linea " + lineaCambio + " - Area de declaración - False";
                String linea2 = "Linea " + lineaCambio + " - Area de ejecución - True";
                logAreas.add(linea1);
                logAreas.add(linea2);
                continue;
            }
            if (cimaPila.equals("801")) {
                pila.pop();
                areaDeclaracion = true;
                int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
                String linea1 = "Linea " + lineaCambio + " - Area de ejecución - False";
                String linea2 = "Linea " + lineaCambio + " - Area de declaración - True";
                logAreas.add(linea1);
                logAreas.add(linea2);
                continue;
            }

            if (cimaPila.equals("802")) {
                pila.pop();
                int ambitoPadreActual = pilaAmbitos.peek();
                contadorAmbitos++;
                int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
                pilaAmbitos.push(contadorAmbitos);
                logAmbitos.add("Creación ámbito: [" + contadorAmbitos + "," + lineaCambio + "]");
                logAmbitos.add("Pila -> " + formatoPilaAmbitos());
                eventosAmbito.add(new int[] { contadorAmbitos, lineaCambio, 1 });

                if (ultimoIdConsumido != null) {
                    if (existeEnAmbito(ambitoPadreActual, ultimoIdConsumido.lexema)) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoPadreActual);
                    }
                    Simbolo fun = new Simbolo(
                        ultimoIdConsumido.lexema,
                        CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        "fun",
                        ambitoPadreActual,
                        "", "0", "0",
                        String.valueOf(contadorAmbitos));
                    tablaSimbolos.insertar(fun);
                    funcionActual = fun;
                    contadorParametros = 0;
                }
                continue;
            }
            if (cimaPila.equals("803")) {
                pila.pop();
                int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
                int idEliminado = pilaAmbitos.isEmpty() ? -1 : pilaAmbitos.pop();
                logAmbitos.add("Eliminación ámbito: [" + idEliminado + "," + lineaCambio + "]");
                logAmbitos.add("Pila -> " + formatoPilaAmbitos());
                eventosAmbito.add(new int[] { idEliminado, lineaCambio, 0 });
                continue;
            }
            if (cimaPila.equals("804")) {
                pila.pop();
                if (ultimoIdConsumido != null) {
                    int ambitoActual = pilaAmbitos.peek();
                    if (existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema)) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoActual);
                    }
                    Simbolo var = new Simbolo(
                        ultimoIdConsumido.lexema,
                        CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        "var",
                        ambitoActual,
                        "", "0", "0", "");
                    tablaSimbolos.insertar(var);
                    variableActual = var;
                    dimsBufferActual = new ArrayList<>();
                }
                continue;
            }
            if (cimaPila.equals("813")) {
                pila.pop();
                if (ultimoIdConsumido != null) {
                    int ambitoActual = pilaAmbitos.peek();
                    if (existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema)) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoActual);
                    }
                    Simbolo constante = new Simbolo(
                        ultimoIdConsumido.lexema,
                        CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        "const",
                        ambitoActual,
                        "", "0", "0", "");
                    tablaSimbolos.insertar(constante);
                }
                continue;
            }
            if (cimaPila.equals("806")) {
                pila.pop();
                if (variableActual != null && ultimoConstDecimalConsumido != null) {
                    dimsBufferActual.add(ultimoConstDecimalConsumido.lexema);
                    variableActual.setClase("arr");
                    variableActual.setTarr(String.join(",", dimsBufferActual));
                    variableActual.setDimArr(String.valueOf(dimsBufferActual.size()));
                }
                continue;
            }
            if (cimaPila.equals("811")) {
                pila.pop();
                if (funcionActual != null && ultimoIdConsumido != null) {
                    int ambitoActual = pilaAmbitos.peek();
                    if (existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema)) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoActual);
                    }
                    contadorParametros++;
                    Simbolo par = new Simbolo(
                        ultimoIdConsumido.lexema,
                        CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        "par",
                        ambitoActual,
                        "", "0",
                        String.valueOf(contadorParametros),
                        funcionActual.getId());
                    tablaSimbolos.insertar(par);
                    funcionActual.setNoPar(String.valueOf(contadorParametros));
                }
                continue;
            }
            if (cimaPila.equals("812")) {
                pila.pop();
                if (ultimoIdConsumido != null && !existeEnAmbitoOAncestro(ultimoIdConsumido.lexema)) {
                    registrarErrorSemantico(545, "Variable no declarada: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido, pilaAmbitos.isEmpty() ? -1 : pilaAmbitos.peek());
                }
                continue;
            }

            if (cimaPila.equals("$")) {
                if (simboloActual.equals("$")) {
                    if (!pilaAmbitos.isEmpty()) {
                        int lineaCambio = (tokenActual != null) ? tokenActual.linea
                                : (posicionActual > 0 ? tokens.get(posicionActual - 1).linea : 0);
                        int idEliminado = pilaAmbitos.pop();
                        logAmbitos.add("Eliminación ámbito: [" + idEliminado + "," + lineaCambio + "]");
                        logAmbitos.add("Pila -> " + formatoPilaAmbitos());
                        eventosAmbito.add(new int[] { idEliminado, lineaCambio, 0 });
                    }
                    break;
                } else {
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
                        String lexemaMostrar = (tokenActual != null) ? tokenActual.lexema : simboloActual;
                        
                        erroresSintacticos.add("Error " + codigoProduccion + " en línea " + lineaError + ": " + descripcion);
                        if (gui != null) {
                            gui.getModeloErrores().addRow(new Object[] { String.valueOf(codigoProduccion), descripcion, lexemaMostrar, "Sintáctico", String.valueOf(lineaError) });
                        }
                        analisisExitoso = false;
                        if (tokenActual != null) {
                            posicionActual++;
                        } else {
                            break;
                        }
                        continue;
                    }
                    
                    if (codigoProduccion == 544) {
                        String descripcion = obtenerDescripcionErrorSintactico(codigoProduccion);
                        int lineaError = (tokenActual != null) ? tokenActual.linea : 0;
                        
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
                    if (cimaNormalizada.equals("id")) ultimoIdConsumido = tokenActual;
                    if (cimaNormalizada.equals("Const_Decimal")) ultimoConstDecimalConsumido = tokenActual;
                    pila.pop();
                    posicionActual++;
                } else {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) +
                        ": Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActualNormalizado + "'");
                    if (gui != null) {
                        gui.getModeloErrores().addRow(new Object[] { "SYNTAX", "Se esperaba '" + cimaNorm + "' pero se encontro '" + simboloActualNormalizado + "'", tokenActual != null ? tokenActual.lexema : simboloActual, "Sintáctico", String.valueOf(tokenActual != null ? tokenActual.linea : 0) });
                    }
                    analisisExitoso = false;
                    break;
                }
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
        return codigo >= 512 && codigo <= 543;
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

        if (simbolo.equals(",")) return "coma";

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

        // El autómata emite -53 (comilla doble) o -54 (comilla simple) para cadenas completas.
        // La gramática solo conoce Const_cadena, no el símbolo literal de comilla.
        if (token.equals("\"") || token.equals("'")) {
            return "Const_cadena";
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

    public boolean isAreaDeclaracion() {
        return areaDeclaracion;
    }

    public List<String> getLogAreas() {
        return logAreas;
    }

    public List<String> getLogAmbitos() {
        return logAmbitos;
    }

    private void registrarErrorAmbito(int ambito) {
        if (ambito < 0) return;
        erroresAmbitoPorAmbito.put(ambito, erroresAmbitoPorAmbito.getOrDefault(ambito, 0) + 1);
    }

    private boolean existeEnAmbito(int ambito, String id) {
        Map<String, Simbolo> simbolosAmbito = tablaSimbolos.getIndicePorAmbito().get(ambito);
        return simbolosAmbito != null && simbolosAmbito.containsKey(id);
    }

    private boolean existeEnAmbitoOAncestro(String id) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            if (existeEnAmbito(pilaAmbitos.get(i), id)) return true;
        }
        return false;
    }

    private void registrarErrorSemantico(int codigo, String descripcion, Token token, int ambito) {
        int linea = (token != null) ? token.linea : 0;
        String lexema = (token != null) ? token.lexema : "";
        if (gui != null) {
            gui.getModeloErrores().addRow(new Object[] { String.valueOf(codigo), descripcion, lexema, "Ambito", String.valueOf(linea) });
        }
        registrarErrorAmbito(ambito);
    }

    public TablaSimbolos getTablaSimbolos() {
        return tablaSimbolos;
    }

    public Map<Integer, Integer> getErroresPorAmbito() {
        return erroresAmbitoPorAmbito;
    }

    public List<int[]> getEventosAmbito() {
        return eventosAmbito;
    }

    private String formatoPilaAmbitos() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pilaAmbitos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(pilaAmbitos.get(i));
        }
        return sb.append("]").toString();
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