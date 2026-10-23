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
    private boolean enCabeceraFor;  // entre 824 y 825: asignar a un id no declarado lo declara en el ámbito del for
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
    private String tipoRegPendiente;          // 'reg X' antes del id que se declara en 804
    private boolean omitirDeclaracionPendiente; // el tipo reg no existe: la variable no se inserta
    private List<String> dimsBufferActual;

    // Prefijo de asignaciones (acciones 814-822)
    private Token ultimoTokenConsumido;
    private Stack<Operando> pilaOperandos;
    private Stack<String> pilaOperadores;
    private Stack<int[]> pilaArreglos; // { índice del token id, tamaño de pilaOperandos al abrir }
    private String objetivoArreglo;
    private int profundidadExpresion;
    private int lineaApertura;
    private List<String> logPrefijos;
    private List<String> logPilasExpresion;
    private List<String> logCuadruplos;
    private List<Object[]> resumenTemporales; // por asignación: línea, conteo por tipo de temporal, texto de asignación, errores de tipos
    private List<String> cuadruplosActuales;
    private Map<String, Integer> contadoresTemporales; // un contador por tipo de temporal, global a todo el programa
    private Map<String, Integer> contadoresTemporalesInicio;
    private List<Object[]> erroresTiposPendientes;

    // Semántica 2: registro de reglas {línea, regla, descripción, situación, lexema, ámbito}
    private List<Object[]> registroSemantica2 = new ArrayList<>();
    private Object[] asignacionPendiente;   // {regla, descripción, aceptada, lexema} de la última asignación reducida
    private int reglaCondicion;             // regla de la condición abierta (1010-1013)
    private String palabraCondicion;        // if / elseif / while / do-while
    private Stack<ContextoArreglo> pilaContextosArreglo = new Stack<>(); // accesos id[...] en curso (reglas 1030-1050)
    private Stack<Object[]> pilaSwitch = new Stack<>(); // switch en curso: {tipo base (String o null), lexema de la expresión} (reglas 1060-1061)
    private boolean unarioPropioAbierto;    // un unario fuera de toda expresión abrió su propio contexto (regla 1070)
    private int posInicioFor;               // primer token de la cabecera del for
    private int posInicioForValor2;         // primer token del valor 2 de un for-in
    private int posInicioIncremento;        // primer token del incremento de un for clásico

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

    // Un operando de la pila: su prefijo, el valor que usan los cuádruplos (lexema o temporal) y su tipo
    private static class Operando {
        final String prefijo;
        final String valor;
        final String tipo;

        Operando(String prefijo, String valor, String tipo) {
            this.prefijo = prefijo;
            this.valor = valor;
            this.tipo = tipo;
        }

        @Override
        public String toString() {
            return prefijo;
        }
    }

    // Un índice de un acceso a arreglo: texto, tipo de la expresión y valor si es una constante calculable
    private static class IndiceArreglo {
        String texto;
        String tipo;
        Long valor;
    }

    // Un acceso id[i1, i2, ...] mientras se analiza
    private static class ContextoArreglo {
        Token id;
        int posId;
        List<IndiceArreglo> indices = new ArrayList<>();
        int inicioIndice;
        boolean indicePropio;                 // el índice abrió su propia expresión (fuera de una asignación o condición)
        int operandosAlAbrir;
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
        reiniciarExpresion();
        this.logPrefijos = new ArrayList<>();
        this.logPilasExpresion = new ArrayList<>();
        this.logCuadruplos = new ArrayList<>();
        this.resumenTemporales = new ArrayList<>();
        this.contadoresTemporales = new LinkedHashMap<>();
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
        this.ultimoTokenConsumido = null;
        this.tipoRegPendiente = null;
        this.omitirDeclaracionPendiente = false;
        this.enCabeceraFor = false;
        reiniciarExpresion();
        this.logPrefijos = new ArrayList<>();
        this.logPilasExpresion = new ArrayList<>();
        this.logCuadruplos = new ArrayList<>();
        this.resumenTemporales = new ArrayList<>();
        this.contadoresTemporales = new LinkedHashMap<>();
        this.registroSemantica2 = new ArrayList<>();
        this.asignacionPendiente = null;
        this.pilaContextosArreglo = new Stack<>();
        this.pilaSwitch = new Stack<>();
        this.unarioPropioAbierto = false;

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
                abrirAmbito(tokenActual);
                if (ultimoIdConsumido != null) {
                    boolean duplicado = existeEnAmbito(ambitoPadreActual, ultimoIdConsumido.lexema);
                    if (duplicado) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoPadreActual);
                    }
                    boolean esReg = "registro".equals(CODIGO_A_TIPO.get(ultimoIdConsumido.token));
                    Simbolo fun = new Simbolo(
                        ultimoIdConsumido.lexema,
                        esReg ? "reg" : CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        esReg ? "reg" : "fun",
                        ambitoPadreActual,
                        "", "0", "0",
                        String.valueOf(contadorAmbitos));
                    if (!duplicado) tablaSimbolos.insertar(fun); // un duplicado solo reporta error, no cuenta
                    funcionActual = fun;
                    contadorParametros = 0;
                }
                continue;
            }
            if (cimaPila.equals("803")) {
                pila.pop();
                cerrarAmbito(tokenActual);
                continue;
            }
            if (cimaPila.equals("823")) { // Inicio del for: crea el ámbito de la sentencia
                pila.pop();
                abrirAmbito(tokenActual);
                continue;
            }
            if (cimaPila.equals("826")) { // Fin del cuerpo del for: elimina su ámbito
                pila.pop();
                cerrarAmbito(tokenActual);
                continue;
            }
            if (cimaPila.equals("824") || cimaPila.equals("825")) { // Zona de declaración del for (824 abre, 825 cierra)
                pila.pop();
                boolean abre = cimaPila.equals("824");
                areaDeclaracion = abre;
                enCabeceraFor = abre;
                int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
                logAreas.add("Linea " + lineaCambio + " - Area de declaración - " + (abre ? "True" : "False"));
                logAreas.add("Linea " + lineaCambio + " - Area de ejecución - " + (abre ? "False" : "True"));
                if (abre) {
                    posInicioFor = posicionActual;
                } else if (tokenActual != null && tokenActual.lexema.equals(";")) {
                    evaluarForInicializacion(posInicioFor, posicionActual - 1);
                }
                continue;
            }
            if (cimaPila.equals("827")) { // 'reg X' en una declaración var: X debe ser un reg declarado
                pila.pop();
                tipoRegPendiente = null;
                omitirDeclaracionPendiente = false;
                if (ultimoIdConsumido != null) {
                    if (esRegistroDeclarado(ultimoIdConsumido.lexema)) {
                        tipoRegPendiente = ultimoIdConsumido.lexema;
                    } else {
                        registrarErrorSemantico(545, "Variable no declarada: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido, pilaAmbitos.peek());
                        omitirDeclaracionPendiente = true;
                    }
                }
                continue;
            }
            if (cimaPila.equals("804")) {
                pila.pop();
                declararVariable();
                continue;
            }
            if (cimaPila.equals("813")) {
                pila.pop();
                if (ultimoIdConsumido != null) {
                    int ambitoActual = pilaAmbitos.peek();
                    boolean duplicado = existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema);
                    if (duplicado) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoActual);
                    }
                    Simbolo constante = new Simbolo(
                        ultimoIdConsumido.lexema,
                        CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        "const",
                        ambitoActual,
                        "", "0", "0", "");
                    if (!duplicado) tablaSimbolos.insertar(constante); // un duplicado solo reporta error, no cuenta
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
                    boolean duplicado = existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema);
                    if (duplicado) {
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
                    if (!duplicado) tablaSimbolos.insertar(par); // un duplicado solo reporta error, no cuenta
                    funcionActual.setNoPar(String.valueOf(contadorParametros));
                }
                continue;
            }
            if (cimaPila.equals("812")) {
                pila.pop();
                if (ultimoIdConsumido != null) {
                    boolean asignaEnFor = enCabeceraFor && tokenActual != null && tokenActual.lexema.equals("=");
                    // Regla 1100: el id usado debe estar declarado (el del for se declara aquí mismo)
                    registrarSemantica2(ultimoIdConsumido.linea, 1100, "Variable declarada",
                        asignaEnFor || existeEnAmbitoOAncestro(ultimoIdConsumido.lexema), ultimoIdConsumido.lexema);
                    if (asignaEnFor) {
                        // for (#Dx = 0; ...): la asignación inicial declara una variable local al for,
                        // aunque exista una con ese nombre en un ámbito superior
                        int ambitoFor = pilaAmbitos.peek();
                        if (existeEnAmbito(ambitoFor, ultimoIdConsumido.lexema)) {
                            registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido, ambitoFor);
                        } else {
                            declararVariable();
                        }
                    } else if (!existeEnAmbitoOAncestro(ultimoIdConsumido.lexema)) {
                        registrarErrorSemantico(545, "Variable no declarada: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido, pilaAmbitos.isEmpty() ? -1 : pilaAmbitos.peek());
                    }
                }
                continue;
            }

            // ==================== PREFIJO DE ASIGNACIONES (814-822) ====================
            // Solo actúan dentro de una asignación id ASIG OR (profundidadExpresion > 0).
            if (cimaPila.equals("814")) { // Insertar operando
                pila.pop();
                if (profundidadExpresion > 0 && ultimoTokenConsumido != null) {
                    insertarOperandoExpresion();
                }
                continue;
            }
            if (cimaPila.equals("815")) { // Insertar operador
                pila.pop();
                if (profundidadExpresion == 0 && ultimoTokenConsumido != null && esUnarioSemantica2(ultimoTokenConsumido.lexema)) {
                    // Unario suelto (++#Dx; o !(...);): abre su propio contexto para poder tipar el operando (regla 1070)
                    lineaApertura = ultimoTokenConsumido.linea;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                    profundidadExpresion++;
                    unarioPropioAbierto = true;
                }
                if (profundidadExpresion > 0 && ultimoTokenConsumido != null) {
                    pilaOperadores.push(ultimoTokenConsumido.lexema);
                    logPilasExpresion.add("Linea " + ultimoTokenConsumido.linea + " - Inserción operador: " + ultimoTokenConsumido.lexema
                        + " -> Pila operadores " + formatoPila(pilaOperadores));
                }
                continue;
            }
            if (cimaPila.equals("816")) { // Reducir binario
                pila.pop();
                if (profundidadExpresion > 0) reducir(2);
                continue;
            }
            if (cimaPila.equals("817")) { // Reducir unario
                pila.pop();
                boolean cierraUnarioPropio = unarioPropioAbierto && pilaOperadores.size() == 1;
                if (profundidadExpresion > 0) reducir(1);
                if (cierraUnarioPropio) {
                    unarioPropioAbierto = false;
                    for (Object[] fila : erroresTiposPendientes) {
                        if (gui != null) gui.getModeloErrores().addRow(fila);
                    }
                    contadoresTemporales = contadoresTemporalesInicio; // el unario suelto no deja temporales
                    reiniciarExpresion();
                }
                continue;
            }
            if (cimaPila.equals("818")) { // Reducir ternario
                pila.pop();
                if (profundidadExpresion > 0) reducir(3);
                continue;
            }
            if (cimaPila.equals("819")) { // Abrir arreglo: recuerda dónde empieza (el id) para tratarlo como una sola variable
                pila.pop();
                int base = (profundidadExpresion > 0) ? pilaOperandos.size() - 1 : pilaOperandos.size();
                pilaArreglos.push(new int[] { posicionActual - 1, base });
                continue;
            }
            if (cimaPila.equals("820")) { // Cerrar arreglo: $v[$i+1] queda como un solo operando
                pila.pop();
                if (!pilaArreglos.isEmpty()) {
                    int[] marca = pilaArreglos.pop();
                    StringBuilder texto = new StringBuilder();
                    for (int i = Math.max(marca[0], 0); i < posicionActual && i < tokens.size(); i++) {
                        texto.append(tokens.get(i).lexema);
                    }
                    if (profundidadExpresion > 0) {
                        while (pilaOperandos.size() > marca[1]) pilaOperandos.pop();
                        insertarOperando(texto.toString());
                    } else {
                        objetivoArreglo = texto.toString();
                    }
                }
                continue;
            }
            if (cimaPila.equals("821")) { // Apertura de asignación (id ASIG OR)
                pila.pop();
                if (ultimoTokenConsumido != null) {
                    // Regla 1090: el destino debe ser variable, parámetro o arreglo
                    String textoDestino = ultimoTokenConsumido.lexema;
                    if (textoDestino.equals("]")) {
                        textoDestino = (profundidadExpresion == 0 && objetivoArreglo != null) ? objetivoArreglo
                            : (!pilaOperandos.isEmpty() ? pilaOperandos.peek().valor : textoDestino);
                    }
                    int corchete = textoDestino.indexOf('[');
                    Simbolo simboloDestino = buscarSimbolo(corchete >= 0 ? textoDestino.substring(0, corchete) : textoDestino);
                    String claseDestino = (simboloDestino == null) ? "variant" : simboloDestino.getClase();
                    boolean destinoValido = claseDestino.equals("variant") || claseDestino.equals("var")
                        || claseDestino.equals("par") || claseDestino.equals("arr");
                    registrarSemantica2(ultimoTokenConsumido.linea, 1090, "Tipo valido para recibir valor", destinoValido, textoDestino);
                }
                if (profundidadExpresion == 0 && ultimoTokenConsumido != null) {
                    String destino = (ultimoTokenConsumido.lexema.equals("]") && objetivoArreglo != null)
                        ? objetivoArreglo : ultimoTokenConsumido.lexema;
                    lineaApertura = ultimoTokenConsumido.linea;
                    asignacionPendiente = null;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                    logPilasExpresion.add("Linea " + lineaApertura + " - Apertura de asignación");
                    insertarOperando(destino);
                }
                profundidadExpresion++;
                continue;
            }
            if (cimaPila.equals("822")) { // Cierre de asignación
                pila.pop();
                profundidadExpresion--;
                if (profundidadExpresion == 0) {
                    // Solo se emite si quedó un único prefijo; arreglos o ternario en el lado derecho no se procesan
                    if (pilaOperandos.size() == 1) {
                        if (asignacionPendiente != null) {
                            registrarSemantica2(lineaApertura, (Integer) asignacionPendiente[0], (String) asignacionPendiente[1],
                                (Boolean) asignacionPendiente[2], (String) asignacionPendiente[3]);
                        }
                        String lineaPrefijo = "Linea " + lineaApertura + " - Prefijo: " + pilaOperandos.peek().prefijo;
                        logPrefijos.add(lineaPrefijo);
                        logPilasExpresion.add(lineaPrefijo);
                        logCuadruplos.add("Linea " + lineaApertura);
                        logCuadruplos.addAll(cuadruplosActuales);
                        registrarResumenTemporales();
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                    } else {
                        // Asignación descartada: no deja temporales ni errores de tipos
                        contadoresTemporales = contadoresTemporalesInicio;
                    }
                    reiniciarExpresion();
                }
                continue;
            }

            // ==================== SEMÁNTICA 2: LISTAS DE ARREGLO (833-836) ====================
            // 835 abre el acceso id[...], 833/834 abren y cierran cada índice, 836 cierra el acceso y evalúa las reglas 1030-1050.
            if (cimaPila.equals("835")) {
                pila.pop();
                ContextoArreglo ctx = new ContextoArreglo();
                ctx.posId = posicionActual - 1;
                ctx.id = (ctx.posId >= 0 && ctx.posId < tokens.size()) ? tokens.get(ctx.posId) : ultimoIdConsumido;
                pilaContextosArreglo.push(ctx);
                continue;
            }
            if (cimaPila.equals("833")) {
                pila.pop();
                if (!pilaContextosArreglo.isEmpty()) {
                    ContextoArreglo ctx = pilaContextosArreglo.peek();
                    ctx.inicioIndice = posicionActual;
                    if (profundidadExpresion == 0) {
                        ctx.indicePropio = true;
                        profundidadExpresion++;
                    } else {
                        ctx.indicePropio = false;
                        ctx.operandosAlAbrir = pilaOperandos.size();
                    }
                }
                continue;
            }
            if (cimaPila.equals("834")) {
                pila.pop();
                if (!pilaContextosArreglo.isEmpty()) {
                    ContextoArreglo ctx = pilaContextosArreglo.peek();
                    IndiceArreglo indice = new IndiceArreglo();
                    indice.texto = textoTokens(ctx.inicioIndice, posicionActual - 1);
                    indice.valor = valorConstanteUnica(ctx.inicioIndice, posicionActual - 1);
                    if (ctx.indicePropio) {
                        profundidadExpresion--;
                        indice.tipo = (pilaOperandos.size() == 1) ? pilaOperandos.peek().tipo : "variant";
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                        reiniciarExpresion();
                    } else {
                        indice.tipo = (pilaOperandos.size() > ctx.operandosAlAbrir) ? pilaOperandos.peek().tipo : "variant";
                        // El índice no es operando de la expresión que lo contiene: solo queda el arreglo
                        while (pilaOperandos.size() > ctx.operandosAlAbrir) pilaOperandos.pop();
                    }
                    ctx.indices.add(indice);
                }
                continue;
            }
            if (cimaPila.equals("836")) {
                pila.pop();
                if (!pilaContextosArreglo.isEmpty()) {
                    evaluarAccesoArreglo(pilaContextosArreglo.pop());
                }
                continue;
            }

            // ==================== SEMÁNTICA 2: CONDICIONES (828-832) ====================
            // 828 if, 829 elseif, 830 while, 831 do-while abren la condición; 832 la cierra y registra la regla.
            if (cimaPila.equals("828") || cimaPila.equals("829") || cimaPila.equals("830") || cimaPila.equals("831")) {
                pila.pop();
                if (profundidadExpresion == 0) {
                    switch (cimaPila) {
                        case "828": reglaCondicion = 1010; palabraCondicion = "if"; break;
                        case "829": reglaCondicion = 1011; palabraCondicion = "elseif"; break;
                        case "830": reglaCondicion = 1012; palabraCondicion = "while"; break;
                        default: reglaCondicion = 1013; palabraCondicion = "do-while"; break;
                    }
                    lineaApertura = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                    logPilasExpresion.add("Linea " + lineaApertura + " - Apertura de condición " + palabraCondicion);
                }
                profundidadExpresion++;
                continue;
            }
            if (cimaPila.equals("832")) {
                pila.pop();
                profundidadExpresion--;
                if (profundidadExpresion == 0) {
                    if (pilaOperandos.size() == 1) {
                        Operando resultado = pilaOperandos.peek();
                        boolean aceptada = resultado.tipo.equals("bool") || resultado.tipo.equals("variant");
                        String descripcion = "Condicion " + palabraCondicion + ": se espera Boolean; resultado "
                            + etiquetaTipo(resultado.tipo) + ".";
                        registrarSemantica2(lineaApertura, reglaCondicion, descripcion, aceptada, resultado.valor);
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                    } else {
                        contadoresTemporales = contadoresTemporalesInicio;
                    }
                    reiniciarExpresion();
                }
                continue;
            }

            // ==================== SEMÁNTICA 2: SWITCH (839-842, 849) ====================
            // 839/840 abren y cierran la expresión del switch (1060); 841/842 la de cada case (1061); 849 cierra el switch.
            if (cimaPila.equals("839") || cimaPila.equals("841") || cimaPila.equals("845")) {
                pila.pop();
                if (profundidadExpresion == 0) {
                    lineaApertura = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                }
                profundidadExpresion++;
                continue;
            }
            if (cimaPila.equals("840") || cimaPila.equals("842") || cimaPila.equals("846")) {
                String accion = cimaPila;
                pila.pop();
                profundidadExpresion--;
                if (profundidadExpresion == 0) {
                    Operando resultado = (pilaOperandos.size() == 1) ? pilaOperandos.peek() : null;
                    if (accion.equals("840")) {
                        String tipo = (resultado != null) ? resultado.tipo : "variant";
                        boolean valido = tipo.equals("cadena") || tipo.equals("dec") || tipo.equals("variant");
                        if (resultado != null) {
                            registrarSemantica2(lineaApertura, 1060, "Switch: se espera Cadena o entero; expresion " + etiquetaTipo(tipo) + ".",
                                valido, resultado.valor);
                        }
                        // Variant o expresión inválida: el tipo base lo fija el primer case válido
                        pilaSwitch.push(new Object[] { (valido && !tipo.equals("variant")) ? tipo : null,
                            resultado != null ? resultado.valor : "?" });
                    } else if (accion.equals("842")) {
                        if (resultado != null && !pilaSwitch.isEmpty()) {
                            evaluarCase(resultado);
                        }
                    } else if (resultado != null) { // 846: condición del for
                        boolean aceptada = resultado.tipo.equals("bool") || resultado.tipo.equals("variant");
                        registrarSemantica2(lineaApertura, 1081, "For: la comparacion debe ser Boolean; resultado "
                            + etiquetaTipo(resultado.tipo) + ".", aceptada, resultado.valor);
                    }
                    if (resultado != null) {
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                    } else {
                        contadoresTemporales = contadoresTemporalesInicio;
                    }
                    reiniciarExpresion();
                }
                continue;
            }
            if (cimaPila.equals("849")) {
                pila.pop();
                if (!pilaSwitch.isEmpty()) pilaSwitch.pop();
                continue;
            }

            // ==================== SEMÁNTICA 2: FOR (843-844, 847-848) ====================
            if (cimaPila.equals("843")) { // for-in: termina el valor 1, empieza el valor 2
                pila.pop();
                posInicioForValor2 = posicionActual;
                continue;
            }
            if (cimaPila.equals("844")) { // for-in: termina el valor 2 (1083 cadenas, 1084 arreglos)
                pila.pop();
                evaluarForIn(posInicioFor, posInicioForValor2 - 2, posInicioForValor2, posicionActual - 1);
                continue;
            }
            if (cimaPila.equals("847")) { // for clásico: empieza el incremento
                pila.pop();
                posInicioIncremento = posicionActual;
                continue;
            }
            if (cimaPila.equals("848")) { // for clásico: termina el incremento (1082)
                pila.pop();
                evaluarForIncremento(posInicioIncremento, posicionActual - 1);
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
                    ultimoTokenConsumido = tokenActual;
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

        // Una expresión interrumpida por un error sintáctico queda incompleta: no se emite su prefijo
        if (profundidadExpresion > 0) {
            reiniciarExpresion();
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

    private boolean esRegistroDeclarado(String id) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            Map<String, Simbolo> simbolosAmbito = tablaSimbolos.getIndicePorAmbito().get(pilaAmbitos.get(i));
            Simbolo s = (simbolosAmbito != null) ? simbolosAmbito.get(id) : null;
            if (s != null && s.getClase().equals("reg") && s.getTipo().equals("reg")) return true;
        }
        return false;
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

    /** Declara ultimoIdConsumido en el ámbito actual (acción 804 y declaración implícita en la cabecera del for). */
    private void declararVariable() {
        if (ultimoIdConsumido == null) return;
        int ambitoActual = pilaAmbitos.peek();
        boolean duplicado = existeEnAmbito(ambitoActual, ultimoIdConsumido.lexema);
        if (duplicado) {
            registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoActual);
        }
        Simbolo var = new Simbolo(
            ultimoIdConsumido.lexema,
            tipoRegPendiente != null ? tipoRegPendiente : CODIGO_A_TIPO.get(ultimoIdConsumido.token),
            tipoRegPendiente != null ? "reg" : "var",
            ambitoActual,
            "", "0", "0", "");
        if (!duplicado && !omitirDeclaracionPendiente) tablaSimbolos.insertar(var);
        tipoRegPendiente = null;
        omitirDeclaracionPendiente = false; // un duplicado solo reporta error, no cuenta
        variableActual = var;
        dimsBufferActual = new ArrayList<>();
    }

    private String formatoPilaAmbitos() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pilaAmbitos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(pilaAmbitos.get(i));
        }
        return sb.append("]").toString();
    }

    public List<String> getLogPrefijos() {
        return logPrefijos;
    }

    public List<String> getLogCuadruplos() {
        return logCuadruplos;
    }

    private void abrirAmbito(Token tokenActual) {
        contadorAmbitos++;
        int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
        pilaAmbitos.push(contadorAmbitos);
        logAmbitos.add("Creación ámbito: [" + contadorAmbitos + "," + lineaCambio + "]");
        logAmbitos.add("Pila -> " + formatoPilaAmbitos());
        eventosAmbito.add(new int[] { contadorAmbitos, lineaCambio, 1 });
    }

    private void cerrarAmbito(Token tokenActual) {
        int lineaCambio = (tokenActual != null) ? tokenActual.linea : 0;
        int idEliminado = pilaAmbitos.isEmpty() ? -1 : pilaAmbitos.pop();
        logAmbitos.add("Eliminación ámbito: [" + idEliminado + "," + lineaCambio + "]");
        logAmbitos.add("Pila -> " + formatoPilaAmbitos());
        eventosAmbito.add(new int[] { idEliminado, lineaCambio, 0 });
    }

    public List<Object[]> getResumenTemporales() {
        return resumenTemporales;
    }

    private static final String[] NOMBRES_TEMPORALES = { "TBin", "TDec", "TOct", "THex", "TReal", "Texp", "TCadena", "TBoolean", "TVariant" };

    // Fila de la hoja Temporales: {línea, conteos por tipo (int[9]), "destino -> TTipo", errores de tipos}
    private void registrarResumenTemporales() {
        int[] conteos = new int[NOMBRES_TEMPORALES.length];
        for (int i = 0; i < conteos.length; i++) {
            conteos[i] = contadoresTemporales.getOrDefault(NOMBRES_TEMPORALES[i], 0)
                - contadoresTemporalesInicio.getOrDefault(NOMBRES_TEMPORALES[i], 0);
        }
        String asignacion = "";
        for (int i = cuadruplosActuales.size() - 1; i >= 0; i--) {
            String[] partes = cuadruplosActuales.get(i).split(",", -1);
            if (partes.length == 3 && esAsignacion(partes[0])) {
                asignacion = partes[1] + " -> " + partes[2].replaceFirst("^(T[A-Za-z]+?)\\d+$", "$1");
                break;
            }
        }
        resumenTemporales.add(new Object[] { lineaApertura, conteos, asignacion, erroresTiposPendientes.size() });
    }

    public List<String> getLogPilasExpresion() {
        return logPilasExpresion;
    }

    private void reiniciarExpresion() {
        this.pilaOperandos = new Stack<>();
        this.pilaOperadores = new Stack<>();
        this.pilaArreglos = new Stack<>();
        this.objetivoArreglo = null;
        this.profundidadExpresion = 0;
        this.lineaApertura = 0;
        this.cuadruplosActuales = new ArrayList<>();
        this.erroresTiposPendientes = new ArrayList<>();
    }

    // Operando dentro de la expresión: un identificador no declarado se cuenta como un temporal TVariant
    private void insertarOperandoExpresion() {
        Token t = ultimoTokenConsumido;
        if (CODIGO_A_TIPO.containsKey(t.token) && !existeEnAmbitoOAncestro(t.lexema)) {
            String temporal = nuevoTemporal("variant");
            pilaOperandos.push(new Operando(t.lexema, temporal, "variant"));
            logPilasExpresion.add("Linea " + t.linea + " - Inserción operando: " + t.lexema
                + " (no declarado, " + temporal + ") -> Pila operandos " + formatoPila(pilaOperandos));
        } else {
            insertarOperando(t.lexema);
        }
    }

    // Crea el siguiente temporal del tipo dado (un contador por tipo, global a todo el programa)
    private String nuevoTemporal(String tipo) {
        String nombreTipo = nombreTemporal(tipo);
        int numero = contadoresTemporales.getOrDefault(nombreTipo, 0) + 1;
        contadoresTemporales.put(nombreTipo, numero);
        return nombreTipo + numero;
    }

    private void insertarOperando(String valor) {
        String tipo = tipoDeToken(ultimoTokenConsumido);
        // Un identificador no declarado no tiene tipo conocido: cuenta como variant
        if (ultimoTokenConsumido != null && CODIGO_A_TIPO.containsKey(ultimoTokenConsumido.token)
                && !existeEnAmbitoOAncestro(ultimoTokenConsumido.lexema)) {
            tipo = "variant";
        }
        pilaOperandos.push(new Operando(valor, valor, tipo));
        int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
        logPilasExpresion.add("Linea " + linea + " - Inserción operando: " + valor
            + " -> Pila operandos " + formatoPila(pilaOperandos));
    }

    // Saca el operador de la cima y sus operandos, deja la operación en prefijo como un solo operando
    // y registra el cuádruplo (op, valor1, valor2, temporal) con un temporal tipado
    private void reducir(int aridad) {
        if (pilaOperadores.isEmpty() || pilaOperandos.size() < aridad) return;
        String operador = pilaOperadores.pop();
        Operando[] operandos = new Operando[aridad];
        for (int i = aridad - 1; i >= 0; i--) {
            operandos[i] = pilaOperandos.pop();
        }

        if (aridad == 1 && esUnarioSemantica2(operador)) evaluarUnario(operador, operandos[0]);
        if (aridad == 1 && operador.equals("+")) {
            pilaOperandos.push(operandos[0]);
            return;
        }
        if (aridad == 1 && operador.equals("-")) operador = "neg";

        List<String> prefijos = new ArrayList<>();
        List<String> valores = new ArrayList<>();
        for (Operando o : operandos) {
            prefijos.add(o.prefijo);
            valores.add(o.valor);
        }
        String prefijo = operador + " " + String.join(" ", prefijos);

        if (aridad == 2 && esAsignacion(operador)) {
            // Una variable solo recibe valores de su mismo tipo; variant (p. ej. no declarada) no se valida
            String tipoDestino = operandos[0].tipo;
            String tipoValor = operandos[1].tipo;
            if (!tipoDestino.equals("variant") && !tipoValor.equals("variant") && !tipoDestino.equals(tipoValor)) {
                int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                erroresTiposPendientes.add(new Object[] {
                    "555",
                    "Asignación incompatible (" + operador + "): " + etiquetaTipo(tipoDestino) + " no puede recibir " + etiquetaTipo(tipoValor),
                    operandos[0].valor + " " + operador + " " + operandos[1].valor, "Semántico", String.valueOf(linea) });
            }
            evaluarAsignacionSemantica2(operador, operandos[0], operandos[1]);
            cuadruplosActuales.add(operador + "," + String.join(",", valores));
            pilaOperandos.push(new Operando(prefijo, operandos[0].valor, operandos[0].tipo));
            return;
        }

        String tipo = resolverTipo(operador, operandos);
        String temporal = nuevoTemporal(tipo);
        cuadruplosActuales.add(operador + "," + String.join(",", valores) + (aridad == 1 ? "," : "") + "," + temporal);
        pilaOperandos.push(new Operando(prefijo, temporal, tipo));
    }

    // Reglas 1030 (dimensiones), 1040 (índice entero) y 1050 (rango) de un acceso id[i1, i2, ...]
    private void evaluarAccesoArreglo(ContextoArreglo ctx) {
        if (ctx.id == null) return;
        Simbolo simbolo = buscarSimbolo(ctx.id.lexema);
        if (simbolo == null) return; // no declarado: ya lo reporta la Semántica 1

        int usadas = ctx.indices.size();
        int declaradas = 0;
        List<Integer> tamanos = new ArrayList<>();
        if ("arr".equals(simbolo.getClase()) && simbolo.getTarr() != null && !simbolo.getTarr().isEmpty()) {
            for (String dim : simbolo.getTarr().split(",")) {
                try {
                    tamanos.add(Integer.parseInt(dim.trim()));
                } catch (NumberFormatException e) {
                    tamanos.add(null);
                }
            }
            declaradas = tamanos.size();
        }
        String acceso = textoTokens(ctx.posId, posicionActual); // incluye el ']' que aún no se consumió
        int linea = ctx.id.linea;

        registrarSemantica2(linea, 1030,
            "Lista de arreglo: dimensiones usadas " + usadas + ", declaradas " + declaradas + ".", usadas == declaradas, acceso);
        // Jerarquía 1030 > 1040 > 1050: si una regla falla, las siguientes ya no se evalúan
        if (usadas != declaradas || declaradas == 0) return;

        String malIndice = null;
        for (IndiceArreglo i : ctx.indices) {
            boolean entero = i.tipo.equals("dec") || i.tipo.equals("bin") || i.tipo.equals("oct")
                || i.tipo.equals("hex") || i.tipo.equals("variant");
            if (!entero) {
                malIndice = "el indice " + i.texto + " debe ser entero, es " + etiquetaTipo(i.tipo);
                break;
            }
        }
        registrarSemantica2(linea, 1040,
            malIndice == null ? "Lista de arreglo: los indices son enteros." : "Lista de arreglo: " + malIndice + ".",
            malIndice == null, acceso);
        if (malIndice != null) return;

        String fuera = null;
        for (int d = 0; d < Math.min(usadas, declaradas) && fuera == null; d++) {
            IndiceArreglo i = ctx.indices.get(d);
            Integer tamano = tamanos.get(d);
            if (i.valor != null && tamano != null && (i.valor < 0 || i.valor >= tamano)) {
                fuera = "el indice " + i.valor + " de la dimension " + (d + 1) + " esta fuera del rango 0.." + (tamano - 1);
            }
        }
        boolean sinVerificar = false;
        for (int d = 0; d < Math.min(usadas, declaradas); d++) {
            if (ctx.indices.get(d).valor == null) sinVerificar = true;
        }
        String okDesc = sinVerificar
            ? "Lista de arreglo: indices constantes dentro del rango declarado (los demas no se validan en compilacion)."
            : "Lista de arreglo: los indices estan dentro del rango declarado.";
        registrarSemantica2(linea, 1050,
            fuera == null ? okDesc : "Lista de arreglo: " + fuera + ".",
            fuera == null, acceso);
    }

    private boolean esUnarioSemantica2(String operador) {
        return operador.equals("++") || operador.equals("--") || operador.equals("!") || operador.equals("~");
    }

    // Regla 1070: ++ y -- sobre entero (#D), ! sobre Boolean, ~ sobre un entero (#D, #O, #B, #X). Variant se acepta.
    private void evaluarUnario(String operador, Operando operando) {
        String tipo = operando.tipo;
        boolean aceptada;
        String esperado;
        if (operador.equals("!")) {
            aceptada = tipo.equals("bool");
            esperado = "Boolean";
        } else if (operador.equals("~")) {
            aceptada = tipo.equals("dec") || tipo.equals("oct") || tipo.equals("bin") || tipo.equals("hex");
            esperado = "Decimal, Octal, Binario o Hexadecimal";
        } else {
            aceptada = tipo.equals("dec");
            esperado = "entero (#D)";
        }
        aceptada = aceptada || tipo.equals("variant");
        int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
        registrarSemantica2(linea, 1070, "Operador unario " + operador + ": se espera " + esperado + "; operando "
            + etiquetaTipo(tipo) + ".", aceptada, operando.valor);
    }

    // Regla 1061: el case debe ser Cadena o entero (#D) y del mismo tipo que la expresión del switch.
    // Si la expresión era variant (o inválida), el primer case válido fija el tipo contra el que se comparan los demás.
    private void evaluarCase(Operando caso) {
        Object[] contexto = pilaSwitch.peek();
        String tipoBase = (String) contexto[0];
        String tipo = caso.tipo;
        boolean tipoValido = tipo.equals("cadena") || tipo.equals("dec");
        boolean aceptada;
        String detalle;
        if (tipo.equals("variant")) {
            aceptada = true;
            detalle = "case Variant se acepta";
        } else if (tipoBase == null) {
            aceptada = tipoValido;
            if (tipoValido) contexto[0] = tipo;
            detalle = tipoValido ? "tipo base tomado del case: " + etiquetaTipo(tipo) : "se espera Cadena o entero, es " + etiquetaTipo(tipo);
        } else {
            aceptada = tipoValido && tipo.equals(tipoBase);
            detalle = aceptada ? "case " + etiquetaTipo(tipo) + " igual al switch"
                : "case " + etiquetaTipo(tipo) + " debe ser " + etiquetaTipo(tipoBase);
        }
        int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
        registrarSemantica2(linea, 1061, "Case: " + detalle + ".", aceptada, contexto[1] + " vs " + caso.valor);
    }

    // Tipo y clase de un valor del for-in: un id simple sale de la tabla de símbolos (variant si no está declarado),
    // una constante, de su token; cualquier otra expresión se toma como variant. Devuelve {tipo, clase}.
    private String[] tipoClaseDeRango(int desde, int hasta) {
        if (desde == hasta && desde >= 0 && desde < tokens.size()) {
            Token t = tokens.get(desde);
            if (CODIGO_A_TIPO.containsKey(t.token)) {
                Simbolo s = buscarSimbolo(t.lexema);
                if (s == null) return new String[] { "variant", "variant" };
                String tipo = s.getTipo() == null || s.getTipo().equals("registro") ? "variant" : s.getTipo();
                return new String[] { tipo, s.getClase() };
            }
            return new String[] { tipoDeToken(t), "const" };
        }
        return new String[] { "variant", "variant" };
    }

    // Regla 1080: la inicialización del for clásico debe asignar a un entero (#D); variant se acepta
    private void evaluarForInicializacion(int desde, int hasta) {
        if (desde < 0 || desde > hasta || hasta >= tokens.size()) return;
        Token id = tokens.get(desde);
        boolean asigna = CODIGO_A_TIPO.containsKey(id.token) && desde < hasta && tokens.get(desde + 1).lexema.equals("=");
        boolean aceptada = false;
        String detalle = "la inicializacion debe ser una asignacion a un entero";
        if (asigna) {
            String tipo = tipoDeToken(id);
            if (!existeEnAmbitoOAncestro(id.lexema)) tipo = "variant";
            aceptada = tipo.equals("dec") || tipo.equals("variant");
            detalle = "la variable debe ser entero (#D), es " + etiquetaTipo(tipo);
        }
        registrarSemantica2(id.linea, 1080, "For inicializacion: " + detalle + ".", aceptada, textoTokens(desde, hasta));
    }

    // Regla 1082: el incremento del for clásico debe ser ++, --, += o -= sobre un entero (#D); variant se acepta
    private void evaluarForIncremento(int desde, int hasta) {
        if (desde < 0 || desde > hasta || hasta >= tokens.size()) return;
        Token operadorToken = null;
        Token id = null;
        for (int i = desde; i <= hasta; i++) {
            Token t = tokens.get(i);
            String l = t.lexema;
            if (operadorToken == null && (l.equals("++") || l.equals("--") || l.equals("+=") || l.equals("-="))) operadorToken = t;
            if (id == null && CODIGO_A_TIPO.containsKey(t.token)) id = t;
        }
        boolean aceptada = false;
        String detalle = "el incremento debe ser ++, --, += o -= sobre un entero";
        if (operadorToken != null && id != null) {
            Simbolo s = buscarSimbolo(id.lexema);
            String tipo = (s == null) ? "variant" : s.getTipo();
            aceptada = "dec".equals(tipo) || "variant".equals(tipo);
            detalle = "la variable debe ser entero (#D), es " + etiquetaTipo(tipo);
        }
        registrarSemantica2(tokens.get(desde).linea, 1082, "For incremento/decremento: " + detalle + ".", aceptada, textoTokens(desde, hasta));
    }

    // Reglas 1083 (valor 2 cadena: valor 1 entero) y 1084 (valor 2 arreglo: valor 1 de su mismo tipo). Variant se acepta.
    private void evaluarForIn(int desde1, int hasta1, int desde2, int hasta2) {
        if (desde1 < 0 || hasta2 >= tokens.size() || desde2 > hasta2) return;
        String[] v1 = tipoClaseDeRango(desde1, hasta1);
        String[] v2 = tipoClaseDeRango(desde2, hasta2);
        String lexema = textoTokens(desde1, hasta1) + " : " + textoTokens(desde2, hasta2);
        int linea = tokens.get(desde1).linea;
        boolean variant1 = v1[0].equals("variant");
        boolean variant2 = v2[0].equals("variant");
        if (variant2) {
            if (variant1) {
                registrarSemantica2(linea, 1083, "For-in: ambos valores Variant, se acepta.", true, lexema);
            }
            registrarSemantica2(linea, 1084, "For-in: valor 2 Variant, se toma como arreglo del tipo del valor 1.", true, lexema);
        } else if (v2[1].equals("arr")) {
            boolean aceptada = variant1 || v1[0].equals(v2[0]);
            registrarSemantica2(linea, 1084, aceptada ? "For-in: el valor 1 es del mismo tipo que el arreglo."
                : "For-in: el valor 1 (" + etiquetaTipo(v1[0]) + ") debe ser del mismo tipo que el arreglo (" + etiquetaTipo(v2[0]) + ").",
                aceptada, lexema);
        } else if (v2[0].equals("cadena")) {
            boolean aceptada = variant1 || v1[0].equals("dec");
            registrarSemantica2(linea, 1083, aceptada ? "For-in: cadena recorrida con un entero."
                : "For-in: sobre una cadena el valor 1 debe ser entero (#D), es " + etiquetaTipo(v1[0]) + ".", aceptada, lexema);
        } else {
            registrarSemantica2(linea, 1084, "For-in: el valor 2 debe ser un arreglo o una cadena, es " + etiquetaTipo(v2[0]) + ".", false, lexema);
        }
    }

    private Simbolo buscarSimbolo(String id) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            Map<String, Simbolo> simbolosAmbito = tablaSimbolos.getIndicePorAmbito().get(pilaAmbitos.get(i));
            if (simbolosAmbito != null && simbolosAmbito.containsKey(id)) return simbolosAmbito.get(id);
        }
        return null;
    }

    // Lexemas de los tokens desde 'desde' hasta 'hasta' (ambos incluidos), pegados
    private String textoTokens(int desde, int hasta) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(desde, 0); i <= hasta && i < tokens.size(); i++) {
            sb.append(tokens.get(i).lexema);
        }
        return sb.toString();
    }

    // Valor del índice solo si es una única constante decimal (3, 69...). Una expresión como 4+70 genera un
    // temporal y su valor no se conoce en compilación, así que no se calcula; devuelve null.
    private Long valorConstanteUnica(int desde, int hasta) {
        if (desde != hasta || desde < 0 || desde >= tokens.size()) return null;
        Token t = tokens.get(desde);
        if (!t.token.equals("-55")) return null;
        try {
            return Long.parseLong(t.lexema);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Object[]> getRegistroSemantica2() {
        return registroSemantica2;
    }

    private void registrarSemantica2(int linea, int regla, String descripcion, boolean aceptada, String lexema) {
        int ambito = pilaAmbitos.isEmpty() ? 0 : pilaAmbitos.peek();
        registroSemantica2.add(new Object[] { linea, regla, descripcion, aceptada ? "ACEPT" : "ERROR", lexema, ambito });
    }

    // Reglas 1020-1022: el destino debe ser del mismo tipo que la expresión. Un tipo variant (no declarado) se acepta.
    // Las asignaciones compuestas se validan con la tabla de compatibilidad de su operación (+= SUMA, -= RESTA, *= MULT, /= DIV).
    private void evaluarAsignacionSemantica2(String operador, Operando destino, Operando valor) {
        int regla = operador.equals("=") ? 1020 : operador.equals("+=") ? 1021 : 1022;
        String lexema = destino.valor + " " + operador + " " + valor.valor;
        String tipoDestino = destino.tipo;
        String tipoValor = valor.tipo;
        String prefijoDescripcion = "Asignacion " + operador + ": ";
        boolean aceptada = true;
        String tipoResultado;
        String detalle = null;

        if (tipoDestino.equals("variant") || tipoValor.equals("variant")) {
            tipoResultado = tipoValor.equals("variant") ? tipoDestino : tipoValor;
        } else if (operador.equals("=")) {
            tipoResultado = tipoValor;
            aceptada = tipoDestino.equals(tipoValor);
            if (!aceptada) detalle = "tipos incompatibles entre " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor);
        } else {
            String clave = operador.equals("+=") ? "SUMA" : operador.equals("-=") ? "RESTA" : operador.equals("*=") ? "MULT" : "DIV";
            int[][] tabla = lectorMatriz.getTablaCompatibilidad(clave);
            int celda = (tabla != null) ? tabla[indiceTipo(tipoDestino)][indiceTipo(tipoValor)] : 0;
            if (celda > 0) {
                aceptada = false;
                tipoResultado = "variant";
                detalle = "operacion incompatible entre " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor);
                int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                erroresTiposPendientes.add(new Object[] {
                    String.valueOf(celda),
                    "Tipos incompatibles para '" + operador + "': " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor),
                    lexema, "Semántico", String.valueOf(linea) });
            } else {
                tipoResultado = tipoDeCodigo(celda);
            }
        }
        String descripcion = prefijoDescripcion + (detalle != null ? detalle + "."
            : "destino " + etiquetaTipo(tipoDestino) + ", resultado " + etiquetaTipo(tipoResultado) + ".");
        asignacionPendiente = new Object[] { regla, descripcion, aceptada, lexema };
    }

    private boolean esAsignacion(String operador) {
        return operador.equals("=") || operador.equals("+=") || operador.equals("-=")
            || operador.equals("*=") || operador.equals("/=");
    }

    // Tipo del temporal según la tabla de compatibilidad del operador; si son incompatibles,
    // se anota un error semántico y el temporal queda como variant
    private String resolverTipo(String operador, Operando[] operandos) {
        if (operandos.length == 1) {
            return operador.equals("!") ? "bool" : operandos[0].tipo;
        }

        String clave;
        String nombre;
        switch (operador) {
            case "+": clave = "SUMA"; nombre = "suma"; break;
            case "-": clave = "RESTA"; nombre = "resta"; break;
            case "*": clave = "MULT"; nombre = "multiplicación"; break;
            case "%": case "^": case "<<": case ">>": case ">>>":
                clave = "RESTO"; nombre = "resto/desplazamiento"; break;
            case "/": clave = "DIV"; nombre = "división"; break;
            case "<": case ">": case "<=": case ">=":
                clave = "REL"; nombre = "relacional"; break;
            case "==": case "!=":
                clave = "REL2"; nombre = "igualdad"; break;
            case "&&": case "||": case "&": case "|": case "#":
                clave = "LOG"; nombre = "lógico"; break;
            default:
                clave = null; nombre = null; break;
        }

        // Sin tabla (ternario, operador no cubierto o CSV sin cargar): mismo tipo da ese tipo, distinto variant
        int[][] tabla = (clave != null) ? lectorMatriz.getTablaCompatibilidad(clave) : null;
        if (tabla == null) {
            String tipo = operandos[0].tipo;
            for (int i = 1; i < operandos.length; i++) {
                if (!tipo.equals(operandos[i].tipo)) return "variant";
            }
            return tipo;
        }

        // Celda positiva = código de error semántico definido en el CSV
        int celda = tabla[indiceTipo(operandos[0].tipo)][indiceTipo(operandos[1].tipo)];
        if (celda > 0) {
            int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
            erroresTiposPendientes.add(new Object[] {
                String.valueOf(celda),
                "Tipos incompatibles para '" + operador + "' (" + nombre + "): "
                    + etiquetaTipo(operandos[0].tipo) + " y " + etiquetaTipo(operandos[1].tipo),
                operandos[0].valor + " " + operador + " " + operandos[1].valor, "Semántico", String.valueOf(linea) });
            return "variant";
        }
        return tipoDeCodigo(celda);
    }

    private static final String[] ORDEN_TIPOS = { "bin", "dec", "oct", "hex", "real", "exp", "cadena", "bool", "variant" };

    private int indiceTipo(String tipo) {
        for (int i = 0; i < ORDEN_TIPOS.length; i++) {
            if (ORDEN_TIPOS[i].equals(tipo)) return i;
        }
        return ORDEN_TIPOS.length - 1;
    }

    private String tipoDeCodigo(int codigo) {
        switch (codigo) {
            case -1: return "bin";
            case -2: return "dec";
            case -3: return "oct";
            case -4: return "hex";
            case -5: return "real";
            case -6: return "exp";
            case -7: return "cadena";
            case -8: return "bool";
            default: return "variant";
        }
    }

    private String etiquetaTipo(String tipo) {
        String t = tipo.equals("bool") ? "Boolean" : tipo;
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private String nombreTemporal(String tipo) {
        switch (tipo) {
            case "bin": return "TBin";
            case "dec": return "TDec";
            case "oct": return "TOct";
            case "hex": return "THex";
            case "real": return "TReal";
            case "exp": return "Texp";
            case "cadena": return "TCadena";
            case "bool": return "TBoolean";
            default: return "TVariant";
        }
    }

    // Tipo de un token: los identificadores traen el tipo en su prefijo; las constantes, en su código
    private String tipoDeToken(Token t) {
        if (t == null) return "variant";
        String tipoId = CODIGO_A_TIPO.get(t.token);
        if (tipoId != null) return tipoId.equals("registro") ? "variant" : tipoId;
        switch (t.token) {
            case "-55": return "dec";
            case "-56": return "real";
            case "-57": return "exp";
            case "-53": case "-54": case "-58": return "cadena";
            case "-59": return "bin";
            case "-60": return "oct";
            case "-61": return "hex";
            case "-105": case "-106": return "bool";
            default: return "variant";
        }
    }

    private String formatoPila(Stack<?> pilaExpresion) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pilaExpresion.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(pilaExpresion.get(i));
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