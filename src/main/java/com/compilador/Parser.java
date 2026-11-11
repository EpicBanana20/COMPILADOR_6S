package com.compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
    private Stack<ContextoLlamada> pilaContextosLlamada = new Stack<>(); // llamadas id(...) en curso (reglas 1150-1160)
    private Stack<ContextoFuncion> pilaContextosFuncion = new Stack<>(); // funciones predefinidas en curso (reglas 2002-2018)
    private Set<String> temporalesFuncion = new HashSet<>();             // temporales que son resultado de una función predefinida
    private int posInicioReturn;            // primer token de la expresión del return en curso (reglas 1130-1131)
    private Stack<Object[]> pilaSwitch = new Stack<>(); // switch en curso: {tipo base (String o null), lexema de la expresión} (reglas 1060-1061)
    private boolean unarioPropioAbierto;    // un unario fuera de toda expresión abrió su propio contexto (regla 1070)
    private String ultimoAccesoArreglo;     // texto del último acceso id[...] cerrado (destino de una asignación a arreglo)
    private Map<String, Integer> lineaRutinaPorAmbito = new HashMap<>(); // ámbito de la rutina -> línea de su def (regla 1140)
    private Map<String, Simbolo> rutinaPorAmbito = new HashMap<>();       // ámbito de la rutina -> su símbolo (incluye repetidas)
    private boolean asignacionIncompatibleReportada;                  // la asignación en curso ya dio el error 555
    private Set<String> ambitosConReturn = new HashSet<>();               // ámbitos de rutina donde se vio un return
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
        String lexema;                        // valor del operando del índice (constante, id o temporal)
    }

    // Un argumento de una llamada: su texto, el valor de su operando y su tipo
    private static class ArgumentoLlamada {
        String texto;
        String tipo;
    }

    // Una llamada id(a1, a2, ...) mientras se analiza
    private static class ContextoLlamada {
        Token id;
        int posId;
        int inicioArgumento;
        boolean detenida;                     // una regla 1150 falló: no se evalúan los demás argumentos
        boolean propia;                       // la llamada abrió su propia expresión (fuera de una asignación o condición)
        int operandosAlAbrir;
        List<ArgumentoLlamada> argumentos = new ArrayList<>();
    }

    // Un argumento de una función predefinida (Semántica 3): su texto, tipo y forma (id, constante)
    private static class ArgumentoFuncion {
        String texto;                         // lo que se muestra: lexema, temporal si es resultado de otra función, o el texto de la expresión
        String tipo = "variant";
        String prefijo;
        String valorCuadruplo;
        boolean esId;                         // un solo token identificador
        Long valorConstante;                  // valor si es una única constante decimal
    }

    // Una llamada a función predefinida SQRT(...), concat(...)... mientras se analiza (acciones 853-855)
    private static class ContextoFuncion {
        Token id;
        int posId;
        int inicioArgumento;
        boolean propia;                       // la llamada abrió su propia expresión (sentencia suelta como val(...) o copy(...))
        int operandosAlAbrir;
        String tipoSalida;
        Map<String, Integer> contadoresAlAbrir; // en una sentencia suelta los temporales no se conservan
        List<ArgumentoFuncion> argumentos = new ArrayList<>();
    }

    // Un acceso id[i1, i2, ...] mientras se analiza
    private static class ContextoArreglo {
        Token id;
        int posId;
        List<IndiceArreglo> indices = new ArrayList<>();
        int inicioIndice;
        boolean indicePropio;                 // el índice abrió su propia expresión (fuera de una asignación o condición)
        int operandosAlAbrir;
        boolean cortado;                      // una regla de un índice falló: no se evalúan los demás
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
        this.lineaRutinaPorAmbito = new HashMap<>();
        this.rutinaPorAmbito = new HashMap<>();
        this.ambitosConReturn = new HashSet<>();
        this.asignacionPendiente = null;
        this.pilaContextosArreglo = new Stack<>();
        this.pilaContextosLlamada = new Stack<>();
        this.pilaContextosFuncion = new Stack<>();
        this.temporalesFuncion = new HashSet<>();
        this.pilaSwitch = new Stack<>();
        this.unarioPropioAbierto = false;
        this.ultimoAccesoArreglo = null;

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
                // Reglas 1110 y 1120: el procedimiento (def sin tipo) o la función (def con tipo) se declara en el ámbito
                // que la contiene (antes de abrir el suyo)
                boolean esDef = posicionActual >= 2 && tokens.get(posicionActual - 2).token.equals("-84");
                boolean sinPrefijo = ultimoIdConsumido != null && "registro".equals(CODIGO_A_TIPO.get(ultimoIdConsumido.token));
                boolean esProc = esDef && sinPrefijo;
                // Una rutina repetida ya se reporta como error de ámbito (546): no se registra otra vez en 1110/1120
                if (esDef && ultimoIdConsumido != null && !existeEnAmbito(ambitoPadreActual, ultimoIdConsumido.lexema)) {
                    String tipoRutina = esProc ? "Void" : etiquetaT(CODIGO_A_TIPO.get(ultimoIdConsumido.token));
                    registrarSemantica2(ultimoIdConsumido.linea, esProc ? 1110 : 1120, esProc ? "Procedimiento" : "Funcion",
                        true, ultimoIdConsumido.lexema, tipoRutina);
                }
                abrirAmbito(tokenActual);
                if (ultimoIdConsumido != null) {
                    boolean duplicado = existeEnAmbito(ambitoPadreActual, ultimoIdConsumido.lexema);
                    if (duplicado) {
                        registrarErrorSemantico(546, "Variable ya declarada en este ámbito: '" + ultimoIdConsumido.lexema + "'", ultimoIdConsumido,ambitoPadreActual);
                    }
                    boolean esReg = sinPrefijo && !esProc;
                    Simbolo fun = new Simbolo(
                        ultimoIdConsumido.lexema,
                        esReg ? "reg" : esProc ? "proc" : CODIGO_A_TIPO.get(ultimoIdConsumido.token),
                        esReg ? "reg" : esProc ? "proc" : "fun",
                        ambitoPadreActual,
                        "", "0", "0",
                        String.valueOf(contadorAmbitos));
                    if (!duplicado) tablaSimbolos.insertar(fun); // un duplicado solo reporta error, no cuenta
                    funcionActual = fun;
                    lineaRutinaPorAmbito.put(fun.getTParr(), ultimoIdConsumido.linea);
                    rutinaPorAmbito.put(fun.getTParr(), fun); // también las repetidas: su return y su cuerpo se evalúan igual
                    contadorParametros = 0;
                }
                continue;
            }
            if (cimaPila.equals("803")) {
                pila.pop();
                evaluarRutinaSinReturn();
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
                    String siguiente = (tokenActual != null) ? tokenActual.lexema : "";
                    boolean asignaEnFor = enCabeceraFor && siguiente.equals("=");
                    // for (#Dj; ...) y for (#Dc : @cad): el primer id de la cabecera se declara en el for si no existía
                    boolean declaraEnFor = enCabeceraFor && posicionActual - 1 == posInicioFor
                        && (siguiente.equals(";") || siguiente.equals(":"))
                        && !existeEnAmbitoOAncestro(ultimoIdConsumido.lexema);
                    // El primer id de for (#Dj; ...) y for (#Dc : @cad) no se evalúa con 1100, exista o no
                    boolean primerIdFor = enCabeceraFor && posicionActual - 1 == posInicioFor
                        && (siguiente.equals(";") || siguiente.equals(":"));
                    // Regla 1100: el id usado debe estar declarado (el que declara el for no cuenta como uso)
                    if (!asignaEnFor && !declaraEnFor && !primerIdFor) {
                        registrarSemantica2(ultimoIdConsumido.linea, 1100, "Variable declarada",
                            existeEnAmbitoOAncestro(ultimoIdConsumido.lexema), ultimoIdConsumido.lexema, "id");
                    }
                    if (declaraEnFor) {
                        declararVariable();
                    } else if (asignaEnFor) {
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
                if (ultimoTokenConsumido != null && !enCabeceraFor) { // la inicialización del for la evalúa la regla 1080
                    // Regla 1090: el destino debe ser variable, parámetro o arreglo
                    boolean destinoArreglo = ultimoTokenConsumido.lexema.equals("]") && ultimoAccesoArreglo != null;
                    String textoDestino = destinoArreglo ? ultimoAccesoArreglo : ultimoTokenConsumido.lexema;
                    int corchete = textoDestino.indexOf('[');
                    Simbolo simboloDestino = buscarSimbolo(corchete >= 0 ? textoDestino.substring(0, corchete) : textoDestino);
                    String claseDestino = (simboloDestino == null) ? "variant" : simboloDestino.getClase();
                    boolean destinoValido = claseDestino.equals("variant") || claseDestino.equals("var")
                        || claseDestino.equals("par") || claseDestino.equals("arr");
                    registrarSemantica2(ultimoTokenConsumido.linea, 1090, "Tipo valido para recibir valor", destinoValido,
                        corchete >= 0 ? textoDestino.substring(0, corchete) : textoDestino,
                        "Var/Par/Arr", corchete >= 0 ? textoDestino.substring(0, corchete) : textoDestino);
                }
                if (profundidadExpresion == 0 && ultimoTokenConsumido != null) {
                    boolean destinoArreglo = ultimoTokenConsumido.lexema.equals("]") && ultimoAccesoArreglo != null;
                    String destino = destinoArreglo ? ultimoAccesoArreglo : ultimoTokenConsumido.lexema;
                    lineaApertura = ultimoTokenConsumido.linea;
                    asignacionPendiente = null;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                    logPilasExpresion.add("Linea " + lineaApertura + " - Apertura de asignación");
                    insertarOperando(destino);
                    if (destinoArreglo) {
                        // El destino a[i] es un solo operando con el tipo de los elementos del arreglo
                        pilaOperandos.pop();
                        Simbolo arreglo = buscarSimbolo(destino.substring(0, destino.indexOf('[')));
                        String tipoElemento = (arreglo == null || arreglo.getTipo() == null || arreglo.getTipo().equals("registro"))
                            ? "variant" : arreglo.getTipo();
                        pilaOperandos.push(new Operando(destino, destino, tipoElemento));
                    }
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
                        if (asignacionPendiente != null && !enCabeceraFor) {
                            registrarSemantica2(lineaApertura, (Integer) asignacionPendiente[0], (String) asignacionPendiente[1],
                                (Boolean) asignacionPendiente[2], (String) asignacionPendiente[3], (String) asignacionPendiente[4],
                                (String) asignacionPendiente[5]);
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
                    indice.lexema = indice.texto;
                    if (ctx.indicePropio) {
                        profundidadExpresion--;
                        indice.tipo = (pilaOperandos.size() == 1) ? tipoS2(pilaOperandos.peek()) : "variant";
                        if (pilaOperandos.size() == 1) indice.lexema = pilaOperandos.peek().valor;
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                        reiniciarExpresion();
                    } else {
                        indice.tipo = (pilaOperandos.size() > ctx.operandosAlAbrir) ? tipoS2(pilaOperandos.peek()) : "variant";
                        if (pilaOperandos.size() > ctx.operandosAlAbrir) indice.lexema = pilaOperandos.peek().valor;
                        // El índice no es operando de la expresión que lo contiene: solo queda el arreglo
                        while (pilaOperandos.size() > ctx.operandosAlAbrir) pilaOperandos.pop();
                    }
                    ctx.indices.add(indice);
                    evaluarIndiceArreglo(ctx, indice); // reglas 1030-1050 de esta dimensión
                }
                continue;
            }
            if (cimaPila.equals("836")) {
                pila.pop();
                if (!pilaContextosArreglo.isEmpty()) {
                    ContextoArreglo cierre = pilaContextosArreglo.pop();
                    ultimoAccesoArreglo = textoTokens(cierre.posId, posicionActual); // incluye el ']' aún sin consumir
                    evaluarDimensionesUsadas(cierre);
                }
                continue;
            }

            // ==================== SEMÁNTICA 2: LLAMADAS A FUNCIÓN (837, 838, 850) ====================
            // 837 abre la llamada tras el '(', 850 cierra cada argumento, 838 cierra la llamada y evalúa las reglas 1150-1160.
            if (cimaPila.equals("837")) {
                pila.pop();
                ContextoLlamada llamada = new ContextoLlamada();
                llamada.posId = posicionActual - 2;
                llamada.id = (llamada.posId >= 0 && llamada.posId < tokens.size()) ? tokens.get(llamada.posId) : ultimoIdConsumido;
                llamada.inicioArgumento = posicionActual;
                if (profundidadExpresion == 0) {
                    llamada.propia = true;
                    profundidadExpresion++;
                }
                llamada.operandosAlAbrir = pilaOperandos.size();
                pilaContextosLlamada.push(llamada);
                continue;
            }
            if (cimaPila.equals("850")) {
                pila.pop();
                if (!pilaContextosLlamada.isEmpty()) {
                    ContextoLlamada llamada = pilaContextosLlamada.peek();
                    ArgumentoLlamada argumento = new ArgumentoLlamada();
                    argumento.texto = textoTokensEspaciado(llamada.inicioArgumento, posicionActual - 1);
                    argumento.tipo = "variant";
                    if (pilaOperandos.size() > llamada.operandosAlAbrir) argumento.tipo = tipoS2(pilaOperandos.peek());
                    // El argumento no es operando de la expresión que contiene la llamada
                    while (pilaOperandos.size() > llamada.operandosAlAbrir) pilaOperandos.pop();
                    llamada.argumentos.add(argumento);
                    evaluarArgumentoLlamada(llamada); // 1150 y 1160 de este argumento, en el orden en que se cierran
                    llamada.inicioArgumento = posicionActual + 1; // salta la coma
                }
                continue;
            }
            if (cimaPila.equals("838")) {
                pila.pop();
                if (!pilaContextosLlamada.isEmpty()) {
                    ContextoLlamada llamada = pilaContextosLlamada.pop();
                    if (llamada.propia) {
                        profundidadExpresion--;
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                        reiniciarExpresion();
                    }
                    evaluarLlamada(llamada);
                    // Una llamada a un procedimiento dentro de una expresión vale Void (no devuelve valor)
                    if (!llamada.propia && llamada.id != null && pilaOperandos.size() == llamada.operandosAlAbrir && !pilaOperandos.isEmpty()) {
                        Simbolo rutina = buscarSimbolo(llamada.id.lexema);
                        if (rutina != null && "proc".equals(rutina.getClase())) {
                            Operando idLlamada = pilaOperandos.pop();
                            pilaOperandos.push(new Operando(idLlamada.prefijo, idLlamada.valor, "void"));
                        }
                    }
                }
                continue;
            }

            // ==================== SEMÁNTICA 3: FUNCIONES PREDEFINIDAS (853-856) ====================
            // 853 abre la llamada tras el '(', 854 cierra cada argumento, 855 cierra la llamada y evalúa las reglas 2002-2018.
            // 856 es CLEAR (sin paréntesis).
            if (cimaPila.equals("853")) {
                pila.pop();
                ContextoFuncion funcion = new ContextoFuncion();
                funcion.posId = posicionActual - 2;
                funcion.id = (funcion.posId >= 0 && funcion.posId < tokens.size()) ? tokens.get(funcion.posId) : ultimoTokenConsumido;
                funcion.inicioArgumento = posicionActual;
                if (profundidadExpresion == 0) {
                    funcion.propia = true;
                    funcion.contadoresAlAbrir = new LinkedHashMap<>(contadoresTemporales);
                    profundidadExpresion++;
                }
                funcion.operandosAlAbrir = pilaOperandos.size();
                pilaContextosFuncion.push(funcion);
                continue;
            }
            if (cimaPila.equals("854")) {
                pila.pop();
                if (!pilaContextosFuncion.isEmpty()) {
                    ContextoFuncion funcion = pilaContextosFuncion.peek();
                    int desde = funcion.inicioArgumento;
                    int hasta = posicionActual - 1;
                    ArgumentoFuncion argumento = new ArgumentoFuncion();
                    argumento.texto = textoArgumentoFuncion(desde, hasta);
                    argumento.prefijo = argumento.texto;
                    argumento.valorCuadruplo = argumento.texto;
                    // En una sentencia suelta (copy(...), val(...)) no hay temporales: se muestra el texto del argumento.
                    // Dentro de una asignación se muestra el operando: el lexema o el temporal de la expresión/función.
                    boolean sentenciaSuelta = pilaContextosFuncion.firstElement().propia;
                    if (pilaOperandos.size() > funcion.operandosAlAbrir) {
                        Operando operando = pilaOperandos.peek();
                        argumento.tipo = operando.tipo;
                        argumento.prefijo = operando.prefijo;
                        argumento.valorCuadruplo = operando.valor;
                        if (!sentenciaSuelta) argumento.texto = operando.valor;
                    }
                    boolean empiezaConId = desde >= 0 && desde < tokens.size() && CODIGO_A_TIPO.containsKey(tokens.get(desde).token);
                    // Un id solo, o un elemento de arreglo id[...] completo, cuenta como identificador
                    argumento.esId = empiezaConId && (desde == hasta
                        || (desde + 1 < tokens.size() && tokens.get(desde + 1).lexema.equals("[") && cierreEn(desde + 1) == hasta));
                    // Una llamada a un procedimiento id(...) es Void; null se rechaza en cualquier parámetro
                    if (empiezaConId && desde + 1 < tokens.size() && tokens.get(desde + 1).lexema.equals("(") && cierreEn(desde + 1) == hasta) {
                        Simbolo rutina = buscarSimbolo(tokens.get(desde).lexema);
                        if (rutina != null && "proc".equals(rutina.getClase())) argumento.tipo = "void";
                    }
                    if (desde == hasta && desde >= 0 && desde < tokens.size() && tokens.get(desde).token.equals("-107")) argumento.tipo = "null";
                    argumento.valorConstante = valorConstanteUnica(desde, hasta);
                    // El argumento no es operando de la expresión que contiene la llamada
                    while (pilaOperandos.size() > funcion.operandosAlAbrir) pilaOperandos.pop();
                    funcion.argumentos.add(argumento);
                    funcion.inicioArgumento = posicionActual + 1; // salta la coma
                }
                continue;
            }
            if (cimaPila.equals("855")) {
                pila.pop();
                if (!pilaContextosFuncion.isEmpty()) {
                    ContextoFuncion funcion = pilaContextosFuncion.pop();
                    evaluarFuncion(funcion);
                    if (funcion.propia) {
                        profundidadExpresion--;
                        for (Object[] fila : erroresTiposPendientes) {
                            if (gui != null) gui.getModeloErrores().addRow(fila);
                        }
                        contadoresTemporales = funcion.contadoresAlAbrir; // la sentencia suelta no deja temporales
                        reiniciarExpresion();
                    } else if (funcion.tipoSalida != null) {
                        // El resultado de la función es un operando (un temporal) de la expresión que la contiene
                        List<String> prefijos = new ArrayList<>();
                        List<String> valores = new ArrayList<>();
                        for (ArgumentoFuncion a : funcion.argumentos) {
                            prefijos.add(a.prefijo);
                            valores.add(a.valorCuadruplo);
                        }
                        String nombre = funcion.id.lexema;
                        String temporal = nuevoTemporal(funcion.tipoSalida);
                        temporalesFuncion.add(temporal);
                        cuadruplosActuales.add(nombre + "," + String.join(",", valores) + "," + temporal);
                        pilaOperandos.push(new Operando(nombre + " " + String.join(" ", prefijos), temporal, funcion.tipoSalida));
                    }
                }
                continue;
            }
            if (cimaPila.equals("856")) {
                pila.pop();
                Token clear = (posicionActual >= 1 && posicionActual <= tokens.size()) ? tokens.get(posicionActual - 1) : ultimoTokenConsumido;
                registrarSemantica3(clear.linea, 2001, "CLEAR", true, "-", "Sin parametros", null); // entrada: CLEAR no lleva parámetros
                registrarSalidaFuncion(clear.linea, "clear", "TVoid", "TVoid");
                if (profundidadExpresion > 0) {
                    String temporal = nuevoTemporal("void");
                    temporalesFuncion.add(temporal);
                    pilaOperandos.push(new Operando("CLEAR", temporal, "void"));
                }
                continue;
            }

            // ==================== SEMÁNTICA 2: RETURN (851, 852) ====================
            // 851 abre la expresión del return, 852 la cierra y evalúa las reglas 1130-1131.
            if (cimaPila.equals("851")) {
                pila.pop();
                posInicioReturn = posicionActual;
                if (profundidadExpresion == 0) {
                    lineaApertura = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                    contadoresTemporalesInicio = new LinkedHashMap<>(contadoresTemporales);
                }
                profundidadExpresion++;
                continue;
            }
            if (cimaPila.equals("852")) {
                pila.pop();
                profundidadExpresion--;
                if (profundidadExpresion == 0) {
                    if (pilaOperandos.size() == 1) {
                        evaluarReturn(pilaOperandos.peek(), textoTokensEspaciado(posInicioReturn, posicionActual - 1));
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
                        String tipoResultado = tipoS2(resultado);
                        boolean aceptada = tipoResultado.equals("bool") || tipoResultado.equals("variant");
                        String descripcion = "Condicion " + palabraCondicion + ": se espera Boolean; resultado "
                            + etiquetaTipo(tipoResultado) + ".";
                        registrarSemantica2(lineaApertura, reglaCondicion, descripcion, aceptada, resultado.valor, "Boolean");
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
                        String tipo = (resultado != null) ? tipoS2(resultado) : "variant";
                        boolean valido = tipo.equals("cadena") || tipo.equals("dec") || tipo.equals("variant");
                        if (resultado != null) {
                            registrarSemantica2(lineaApertura, 1060, "Switch: se espera Cadena o entero; expresion " + etiquetaTipo(tipo) + ".",
                                valido, resultado.valor, "Dec/Cadena");
                        }
                        // Variant o expresión inválida: el tipo base lo fija el primer case válido
                        pilaSwitch.push(new Object[] { (valido && !tipo.equals("variant")) ? tipo : null,
                            resultado != null ? resultado.valor : "?" });
                    } else if (accion.equals("842")) {
                        if (resultado != null && !pilaSwitch.isEmpty()) {
                            evaluarCase(resultado);
                        }
                    } else if (resultado != null) { // 846: condición del for
                        String tipoResultado = tipoS2(resultado);
                        boolean aceptada = tipoResultado.equals("bool") || tipoResultado.equals("variant");
                        registrarSemantica2(lineaApertura, 1081, "For: la comparacion debe ser Boolean; resultado "
                            + etiquetaTipo(tipoResultado) + ".", aceptada, resultado.valor, "Boolean");
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
            boolean incompatible = !tipoDestino.equals("variant") && !tipoValor.equals("variant") && !cabeEn(tipoDestino, tipoValor);
            if (incompatible) {
                int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
                erroresTiposPendientes.add(new Object[] {
                    "555",
                    "Asignación incompatible (" + operador + "): " + etiquetaTipo(tipoDestino) + " no puede recibir " + etiquetaTipo(tipoValor),
                    operandos[0].valor + " " + operador + " " + operandos[1].valor, "Semántica 1", String.valueOf(linea) });
            }
            // Si la asignación ya se reportó incompatible (555), no se revisa de nuevo el tipo con la tabla de la operación
            asignacionIncompatibleReportada = incompatible;
            evaluarAsignacionSemantica2(operador, operandos[0], operandos[1]);
            asignacionIncompatibleReportada = false;
            cuadruplosActuales.add(operador + "," + String.join(",", valores));
            pilaOperandos.push(new Operando(prefijo, operandos[0].valor, operandos[0].tipo));
            return;
        }

        String tipo = resolverTipo(operador, operandos);
        String temporal = nuevoTemporal(tipo);
        cuadruplosActuales.add(operador + "," + String.join(",", valores) + (aridad == 1 ? "," : "") + "," + temporal);
        pilaOperandos.push(new Operando(prefijo, temporal, tipo));
    }

    // Tamaños declarados de las dimensiones de un arreglo; vacío si el símbolo no es un arreglo
    private List<Integer> tamanosArreglo(Simbolo simbolo) {
        List<Integer> tamanos = new ArrayList<>();
        if ("arr".equals(simbolo.getClase()) && simbolo.getTarr() != null && !simbolo.getTarr().isEmpty()) {
            for (String dim : simbolo.getTarr().split(",")) {
                try {
                    tamanos.add(Integer.parseInt(dim.trim()));
                } catch (NumberFormatException e) {
                    tamanos.add(null);
                }
            }
        }
        return tamanos;
    }

    // Reglas 1030 (dimensión), 1040 (índice entero) y 1050 (rango) del índice recién cerrado de un acceso id[i1, i2, ...]
    // Jerarquía 1030 > 1040 > 1050: si una regla falla, ni ese índice ni los siguientes evalúan las demás.
    private void evaluarIndiceArreglo(ContextoArreglo ctx, IndiceArreglo indice) {
        if (ctx.id == null || ctx.cortado) return;
        Simbolo simbolo = buscarSimbolo(ctx.id.lexema);
        if (simbolo == null) { // no declarado: ya lo reporta la regla 1100
            ctx.cortado = true;
            return;
        }
        List<Integer> tamanos = tamanosArreglo(simbolo);
        int n = ctx.indices.size();
        int linea = ctx.id.linea;

        boolean existeDimension = n <= tamanos.size();
        registrarSemantica2(linea, 1030,
            "Lista de arreglo: la dimension " + n + (existeDimension ? " esta declarada." : " no esta declarada (declaradas "
                + tamanos.size() + ")."),
            existeDimension, ctx.id.lexema, "Dim " + tamanos.size());
        if (!existeDimension) { ctx.cortado = true; return; }

        boolean entero = indice.tipo.equals("dec") || indice.tipo.equals("variant"); // solo Dec: Bin, Oct, Hex y null no son índice
        registrarSemantica2(linea, 1040,
            entero ? "Lista de arreglo: el indice " + n + " es entero."
                   : "Lista de arreglo: el indice " + indice.texto + " debe ser entero, es " + etiquetaTipo(indice.tipo) + ".",
            entero, indice.lexema, "Dec");
        if (!entero) return; // este índice no evalúa 1050, pero los siguientes sí se evalúan

        Integer tamano = tamanos.get(n - 1);
        boolean fuera = indice.valor != null && tamano != null && (indice.valor < 0 || indice.valor >= tamano);
        String descripcion = fuera
            ? "Lista de arreglo: el indice " + indice.valor + " de la dimension " + n + " esta fuera del rango 0.." + (tamano - 1) + "."
            : indice.valor != null ? "Lista de arreglo: el indice " + n + " esta dentro del rango declarado."
                : "Lista de arreglo: el indice " + n + " no es constante, no se valida en compilacion.";
        registrarSemantica2(linea, 1050, descripcion, !fuera, indice.lexema, "0.." + (tamano != null ? String.valueOf(tamano - 1) : "?"));
        if (fuera) ctx.cortado = true;
    }

    // Regla 1030 al cerrar el acceso: no pueden usarse menos dimensiones de las declaradas
    private void evaluarDimensionesUsadas(ContextoArreglo ctx) {
        if (ctx.id == null || ctx.cortado) return;
        Simbolo simbolo = buscarSimbolo(ctx.id.lexema);
        if (simbolo == null) return;
        int declaradas = tamanosArreglo(simbolo).size();
        int usadas = ctx.indices.size();
        if (usadas < declaradas) {
            registrarSemantica2(ctx.id.linea, 1030,
                "Lista de arreglo: dimensiones usadas " + usadas + ", declaradas " + declaradas + ".", false, ctx.id.lexema, "Dim " + declaradas);
        }
    }

    // Parámetros de la rutina que se llama; null si el id no es una rutina declarada
    private List<Simbolo> parametrosDeLlamada(ContextoLlamada llamada) {
        if (llamada.id == null) return null;
        Simbolo funcion = buscarSimbolo(llamada.id.lexema);
        if (funcion == null || !("fun".equals(funcion.getClase()) || "proc".equals(funcion.getClase()))) return null; // no declarada o no es rutina
        List<Simbolo> parametros = new ArrayList<>();
        for (Simbolo s : tablaSimbolos.getFilas()) {
            if ("par".equals(s.getClase()) && String.valueOf(s.getAmb()).equals(funcion.getTParr())) parametros.add(s);
        }
        return parametros;
    }

    // Reglas 1150 (parámetro declarado) y 1160 (tipo del parámetro) del argumento que acaba de cerrarse (acción 850)
    private void evaluarArgumentoLlamada(ContextoLlamada llamada) {
        List<Simbolo> parametros = llamada.detenida ? null : parametrosDeLlamada(llamada);
        if (parametros == null) return;
        int n = llamada.argumentos.size();
        ArgumentoLlamada argumento = llamada.argumentos.get(n - 1);
        int linea = llamada.id.linea;
        boolean existe = n <= parametros.size();
        registrarSemantica2(linea, 1150,
            "Llamada: el parametro " + n + (existe ? " esta declarado." : " no esta declarado (la funcion recibe " + parametros.size() + ")."),
            existe, argumento.texto, existe ? parametros.get(n - 1).getId() : "-", llamada.id.lexema);
        if (!existe) {
            llamada.detenida = true;
            return;
        }
        Simbolo parametro = parametros.get(n - 1);
        // Un argumento numérico se acepta en un parámetro Real o Exp (se amplía al tipo del parámetro)
        boolean numericoAmpliable = (parametro.getTipo().equals("real") || parametro.getTipo().equals("exp"))
            && (argumento.tipo.equals("bin") || argumento.tipo.equals("dec") || argumento.tipo.equals("oct")
                || argumento.tipo.equals("hex") || argumento.tipo.equals("real") || argumento.tipo.equals("exp"));
        boolean tipoIgual = argumento.tipo.equals("variant") || argumento.tipo.equals(parametro.getTipo()) || numericoAmpliable;
        registrarSemantica2(linea, 1160,
            "Llamada: parametro " + n + " " + etiquetaTipo(parametro.getTipo()) + ", argumento " + etiquetaTipo(argumento.tipo) + ".",
            tipoIgual, argumento.texto, etiquetaT(parametro.getTipo()),
            llamada.id.lexema + "(par " + n + ": " + argumento.texto + ")");
    }

    // Al cerrar la llamada (acción 838): si faltan argumentos, el primer parámetro sin argumento falla la regla 1150
    private void evaluarLlamada(ContextoLlamada llamada) {
        List<Simbolo> parametros = llamada.detenida ? null : parametrosDeLlamada(llamada);
        if (parametros == null || llamada.argumentos.size() >= parametros.size()) return;
        int n = llamada.argumentos.size() + 1;
        registrarSemantica2(llamada.id.linea, 1150, "Llamada: el parametro " + n + " no recibio argumento.", false, "-",
            parametros.get(n - 1).getId(), llamada.id.lexema);
    }

    // Reglas 1130 (return dentro de una función) y 1131 (el tipo retornado es el de la función)
    private void evaluarReturn(Operando valor, String textoExpresion) {
        Simbolo funcion = null;
        for (int i = pilaAmbitos.size() - 1; i >= 0 && funcion == null; i--) {
            funcion = rutinaPorAmbito.get(String.valueOf(pilaAmbitos.get(i)));
        }
        // Fuera de una rutina no se evalúa: la regla 1130 solo se registra para returns dentro de una función o procedimiento
        if (funcion == null) return;
        int linea = (lineaApertura > 0) ? lineaApertura : (ultimoTokenConsumido != null ? ultimoTokenConsumido.linea : 0);
        boolean primerReturn = ambitosConReturn.add(funcion.getTParr()); // false si la función ya tenía un return
        if ("proc".equals(funcion.getClase())) { // regla 1140: un procedimiento no retorna valor
            registrarSemantica2(linea, 1140, "Un procedimiento no debe tener return", false, valor.valor, "Void", "return " + textoExpresion);
            return;
        }
        // La regla 1130 (la función tiene return) se registra una sola vez por función, en su primer return
        if (primerReturn) registrarSemantica2(linea, 1130, "Return dentro de una funcion", true, funcion.getId(), "return");
        boolean tipoIgual = valor.tipo.equals("variant") || valor.tipo.equals(funcion.getTipo());
        registrarSemantica2(linea, 1131,
            "Return: la funcion es " + etiquetaTipo(funcion.getTipo()) + " y retorna " + etiquetaTipo(valor.tipo) + ".",
            tipoIgual, valor.valor, etiquetaT(funcion.getTipo()), "return " + textoExpresion);
    }

    // Al cerrar una rutina sin ningún return:
    //  - un procedimiento se acepta en 1140 (un procedimiento no regresa valor)
    //  - una función falla 1130 (debe contener por lo menos un retorno)
    private void evaluarRutinaSinReturn() {
        if (pilaAmbitos.isEmpty()) return;
        String ambito = String.valueOf(pilaAmbitos.peek());
        Simbolo rutina = rutinaPorAmbito.get(ambito);
        if (rutina == null || ambitosConReturn.contains(ambito)) return;
        Integer lineaDef = lineaRutinaPorAmbito.get(ambito);
        int linea = lineaDef != null ? lineaDef : 0;
        if ("proc".equals(rutina.getClase())) {
            registrarSemantica2(linea, 1140, "Procedimiento sin return", true, rutina.getId(), "Void");
        } else if ("fun".equals(rutina.getClase())) {
            registrarSemantica2(linea, 1130, "La funcion no tiene ningun return", false, rutina.getId(), "return");
        }
    }

    private boolean esUnarioSemantica2(String operador) {
        return operador.equals("++") || operador.equals("--") || operador.equals("!") || operador.equals("~");
    }

    // Regla 1070: ++ y -- sobre entero (#D), ! sobre Boolean, ~ sobre un entero (#D, #O, #B, #X). Variant se acepta.
    private void evaluarUnario(String operador, Operando operando) {
        String tipo = tipoS2(operando);
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
            + etiquetaTipo(tipo) + ".", aceptada, operando.prefijo.contains(" ") ? operando.valor : operando.prefijo,
            operador.equals("!") ? "Boolean" : operador.equals("~") ? "Dec/Bin/Oct/Hex" : "Dec",
            operador.equals("!") || operador.equals("~")
                ? operador + "(" + (operando.prefijo.contains(" ") ? operando.valor : operando.prefijo) + ")"
                : operador + (operando.prefijo.contains(" ") ? operando.valor : operando.prefijo));
    }

    // Regla 1061: el case debe ser Cadena o entero (#D) y del mismo tipo que la expresión del switch.
    // Si la expresión era variant (o inválida), el primer case válido fija el tipo contra el que se comparan los demás.
    private void evaluarCase(Operando caso) {
        Object[] contexto = pilaSwitch.peek();
        String tipoBase = (String) contexto[0];
        String tipo = tipoS2(caso);
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
        registrarSemantica2(linea, 1061, "Case: " + detalle + ".", aceptada, caso.valor,
            tipoBase != null ? etiquetaT(tipoBase) : tipoValido ? etiquetaT(tipo) : "Dec/Cadena", caso.valor);
    }

    // Tipo y clase de un valor del for-in: un id simple sale de la tabla de símbolos (variant si no está declarado),
    // una constante, de su token; cualquier otra expresión se toma como variant. Devuelve {tipo, clase}.
    private String[] tipoClaseDeRango(int desde, int hasta) {
        if (desde == hasta && desde >= 0 && desde < tokens.size()) {
            Token t = tokens.get(desde);
            if (t.token.equals("-107")) return new String[] { "null", "const" };
            if (CODIGO_A_TIPO.containsKey(t.token)) {
                Simbolo s = buscarSimbolo(t.lexema);
                if (s == null) return new String[] { "variant", "variant" };
                String tipo = s.getTipo() == null || s.getTipo().equals("registro") ? "variant" : s.getTipo();
                return new String[] { tipo, s.getClase() };
            }
            return new String[] { tipoDeToken(t), "const" };
        }
        // id[...]: un elemento del arreglo, no el arreglo completo
        if (desde >= 0 && hasta < tokens.size() && hasta > desde + 1
                && CODIGO_A_TIPO.containsKey(tokens.get(desde).token)
                && tokens.get(desde + 1).lexema.equals("[") && tokens.get(hasta).lexema.equals("]")) {
            Simbolo s = buscarSimbolo(tokens.get(desde).lexema);
            if (s == null || s.getTipo() == null || s.getTipo().equals("registro")) return new String[] { "variant", "variant" };
            return new String[] { s.getTipo(), "elemento" };
        }
        // Llamada a una función predefinida (STRLEN(@c)): su tipo es el de la salida de la función
        if (desde >= 0 && hasta < tokens.size() && hasta > desde + 1 && tokens.get(desde + 1).lexema.equals("(")
                && tokens.get(hasta).lexema.equals(")") && cierreEn(desde + 1) == hasta) {
            LecturaMatriz.ReglaSemantica3 salida = lectorMatriz.getReglaSemantica3(tokens.get(desde).lexema);
            if (salida != null && salida.esSalida() && salida.tipos.size() == 1) return new String[] { salida.tipos.get(0), "llamada" };
        }
        return new String[] { "variant", "variant" };
    }

    // Operando que es el literal null: para las reglas de Semántica 2 y 3 es un tipo propio, no un Variant
    private boolean esNull(Operando operando) {
        return operando != null && "null".equals(operando.prefijo);
    }

    private String tipoS2(Operando operando) {
        return esNull(operando) ? "null" : operando.tipo;
    }

    // Asignación de un valor a un destino: del mismo tipo, o un Dec a Real o Exp, o un Real a Exp.
    // Bin, Oct y Hex no se amplían (un Bin no cabe en un Real).
    private boolean cabeEn(String destino, String valor) {
        if (destino.equals(valor)) return true;
        if (destino.equals("real")) return valor.equals("dec");
        if (destino.equals("exp")) return valor.equals("dec") || valor.equals("real");
        return false;
    }

    // Regla 1080: la inicialización del for clásico debe asignar a un entero (#D); variant se acepta
    private void evaluarForInicializacion(int desde, int hasta) {
        if (desde < 0 || desde > hasta || hasta >= tokens.size()) return;
        Token id = tokens.get(desde);
        boolean asigna = CODIGO_A_TIPO.containsKey(id.token) && desde < hasta && tokens.get(desde + 1).lexema.equals("=");
        boolean aceptada = false;
        String detalle = "la inicializacion debe ser un id inicializado con =";
        if (asigna) {
            String tipo = tipoDeToken(id);
            if (!existeEnAmbitoOAncestro(id.lexema)) tipo = "variant";
            aceptada = true;
            detalle = "el id " + id.lexema + " queda inicializado (" + etiquetaTipo(tipo) + ")";
        }
        registrarSemantica2(id.linea, 1080, "For inicializacion: " + detalle + ".", aceptada, id.lexema, "id (inicializado)", textoTokens(desde, hasta));
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
        registrarSemantica2(tokens.get(desde).linea, 1082, "For incremento/decremento: " + detalle + ".", aceptada,
            id != null ? id.lexema : textoTokens(desde, hasta), "++/--", textoTokens(desde, hasta));
    }

    // Reglas 1083 (valor 2 cadena: valor 1 entero) y 1084 (valor 2 arreglo: valor 1 de su mismo tipo). Variant se acepta.
    private void evaluarForIn(int desde1, int hasta1, int desde2, int hasta2) {
        if (desde1 < 0 || hasta2 >= tokens.size() || desde2 > hasta2) return;
        String[] v1 = tipoClaseDeRango(desde1, hasta1);
        String[] v2 = tipoClaseDeRango(desde2, hasta2);
        String valor1 = textoTokens(desde1, hasta1);
        String valor2 = textoTokens(desde2, hasta2);
        String lexema = valor1 + " : " + valor2;
        String topeArreglo = "Arr " + etiquetaT(v1[0]);
        int linea = tokens.get(desde1).linea;
        boolean variant1 = v1[0].equals("variant");
        boolean variant2 = v2[0].equals("variant");
        if (variant2) {
            if (variant1) {
                registrarSemantica2(linea, 1083, "For-in: ambos valores Variant, se acepta.", true, valor1, "Dec", lexema);
            }
            registrarSemantica2(linea, 1084, "For-in: valor 2 Variant, se toma como arreglo del tipo del valor 1.", true, valor2,
                "Arr " + etiquetaT(v1[0]), lexema);
        } else if (v2[1].equals("arr")) {
            boolean aceptada = variant1 || v1[0].equals(v2[0]);
            registrarSemantica2(linea, 1084, aceptada ? "For-in: el valor 1 es del mismo tipo que el arreglo."
                : "For-in: el valor 1 (" + etiquetaTipo(v1[0]) + ") debe ser del mismo tipo que el arreglo (" + etiquetaTipo(v2[0]) + ").",
                aceptada, valor2, topeArreglo, lexema);
        } else if (v2[0].equals("cadena")) {
            // El valor 1 debe ser un identificador (un solo token id), no una constante como 5
            boolean esId = desde1 == hasta1 && CODIGO_A_TIPO.containsKey(tokens.get(desde1).token);
            boolean aceptada = variant1 || (v1[0].equals("dec") && esId);
            String motivo = !esId && v1[0].equals("dec") ? "el valor 1 debe ser un identificador entero (#D), no una constante."
                : "sobre una cadena el valor 1 debe ser entero (#D), es " + etiquetaTipo(v1[0]) + ".";
            registrarSemantica2(linea, 1083, aceptada ? "For-in: cadena recorrida con un entero."
                : "For-in: " + motivo, aceptada, valor1, "Dec", lexema);
        } else {
            registrarSemantica2(linea, 1084, "For-in: el valor 2 debe ser un arreglo o una cadena, es " + etiquetaTipo(v2[0]) + ".", false, valor2, topeArreglo, lexema);
        }
    }

    private Simbolo buscarSimbolo(String id) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            Map<String, Simbolo> simbolosAmbito = tablaSimbolos.getIndicePorAmbito().get(pilaAmbitos.get(i));
            if (simbolosAmbito != null && simbolosAmbito.containsKey(id)) return simbolosAmbito.get(id);
        }
        return null;
    }

    // Posición del paréntesis o corchete que cierra al que está en 'abre' (cuenta los anidados); -1 si no lo encuentra
    private int cierreEn(int abre) {
        String apertura = tokens.get(abre).lexema;
        String cierre = apertura.equals("[") ? "]" : ")";
        int nivel = 0;
        for (int i = abre; i < tokens.size(); i++) {
            String lexema = tokens.get(i).lexema;
            if (lexema.equals(apertura)) nivel++;
            else if (lexema.equals(cierre) && --nivel == 0) return i;
        }
        return -1;
    }

    // Lexemas de los tokens desde 'desde' hasta 'hasta' (ambos incluidos), pegados
    private String textoTokens(int desde, int hasta) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(desde, 0); i <= hasta && i < tokens.size(); i++) {
            sb.append(tokens.get(i).lexema);
        }
        return sb.toString();
    }

    // Igual que textoTokens, pero con un espacio después de cada coma: #Df(1, "a")
    private String textoTokensEspaciado(int desde, int hasta) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(desde, 0); i <= hasta && i < tokens.size(); i++) {
            sb.append(tokens.get(i).lexema);
            if (tokens.get(i).lexema.equals(",") && i < hasta) sb.append(' ');
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

    // ==================== SEMÁNTICA 3: REGLAS 2002-2018 ====================

    // Texto de un argumento tal como se muestra en la evidencia: espacio después de la coma y alrededor de los operadores
    // binarios (#Dx + 1, concat(@c, @d)); el signo unario y ^ van pegados.
    private String textoArgumentoFuncion(int desde, int hasta) {
        StringBuilder sb = new StringBuilder();
        String anterior = null;
        for (int i = Math.max(desde, 0); i <= hasta && i < tokens.size(); i++) {
            String lexema = tokens.get(i).lexema;
            boolean binario = " + - * / % << >> >>> < > <= >= == != && || & | ".contains(" " + lexema + " ");
            boolean unario = (lexema.equals("+") || lexema.equals("-"))
                && (anterior == null || anterior.equals("(") || anterior.equals(",") || "+-*/%<>=!&|".contains(anterior.substring(anterior.length() - 1)));
            if (binario && !unario) sb.append(' ').append(lexema).append(' ');
            else sb.append(lexema);
            if (lexema.equals(",") && i < hasta) sb.append(' ');
            anterior = lexema;
        }
        return sb.toString();
    }

    // Cierra una llamada a función predefinida: valida cada parámetro (reglas de entrada 2002-2010, una fila por parámetro)
    // y registra la salida (reglas 2011-2018). Deja el tipo de salida en la llamada.
    private void evaluarFuncion(ContextoFuncion funcion) {
        String nombre = funcion.id.lexema.toLowerCase();
        int linea = funcion.id.linea;
        boolean primerValido = false;
        String tipoPrimero = "variant";

        for (int i = 0; i < funcion.argumentos.size(); i++) {
            ArgumentoFuncion argumento = funcion.argumentos.get(i);
            String parametro = nombre + "_p" + (i + 1);
            LecturaMatriz.ReglaSemantica3 regla = lectorMatriz.getReglaSemantica3(parametro);
            if (regla == null) continue;
            boolean aceptado = evaluarParametroFuncion(linea, parametro, regla, argumento);
            if (i == 0) {
                primerValido = aceptado;
                tipoPrimero = argumento.tipo;
            }
        }

        LecturaMatriz.ReglaSemantica3 salida = lectorMatriz.getReglaSemantica3(nombre);
        if (salida == null || salida.tipos.isEmpty()) return;
        String tipo;
        if (salida.tipos.size() == 1) {
            tipo = salida.tipos.get(0);                                  // salida fija (Exp, Dec, Cadena, Boolean, Void)
        } else if (primerValido && salida.tipos.contains(tipoPrimero)) {
            tipo = tipoPrimero;                                          // salida = tipo de p1
            if (nombre.equals("pow") && tipo.equals("real")) tipo = "exp"; // pow de un Real da un Exp (por eso $r = POW($r, 2) falla)
        } else {
            tipo = salida.tipos.contains("exp") ? "exp" : "dec";         // p1 inválido: el máximo (Exp) o Dec en inc/dec
        }
        funcion.tipoSalida = tipo;
        registrarSalidaFuncion(linea, nombre, topeTipos(salida.tipos, false), "T" + etiquetaTipo(tipo), salida.regla);
    }

    private void registrarSalidaFuncion(int linea, String nombre, String tope, String valor) {
        LecturaMatriz.ReglaSemantica3 salida = lectorMatriz.getReglaSemantica3(nombre);
        registrarSalidaFuncion(linea, nombre, tope, valor, salida != null ? salida.regla : 2011);
    }

    private void registrarSalidaFuncion(int linea, String nombre, String tope, String valor, int regla) {
        // CLEAR se escribe en mayúsculas en la evidencia; las demás salidas llevan el nombre de la función
        registrarSemantica3(linea, regla, nombre.equals("clear") ? "CLEAR" : nombre, true, valor, tope, null);
    }

    // Texto de "Tope Pila": TDec/TReal/TExp (con /id si la regla acepta identificadores)
    private String topeTipos(List<String> tipos, boolean conId) {
        StringBuilder sb = new StringBuilder();
        for (String t : tipos) {
            if (sb.length() > 0) sb.append('/');
            sb.append('T').append(etiquetaTipo(t));
        }
        if (conId) sb.append("/id");
        return sb.toString();
    }

    private String listaEtiquetas(List<String> tipos) {
        StringBuilder sb = new StringBuilder();
        for (String t : tipos) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(etiquetaTipo(t));
        }
        return sb.toString();
    }

    // Valida un argumento contra la regla de su parámetro; registra la fila y, si falla, el error de Semántica 3
    private boolean evaluarParametroFuncion(int linea, String parametro, LecturaMatriz.ReglaSemantica3 regla, ArgumentoFuncion argumento) {
        String tipo = argumento.tipo;
        String etiqueta = etiquetaTipo(tipo);
        boolean variant = tipo.equals("variant"); // tipo desconocido (id no declarado): no se valida
        boolean aceptado;
        String tope;
        String mensaje;

        if (regla.idCadena) {                                            // 2006: solo un id de Cadena
            aceptado = variant || (argumento.esId && tipo.equals("cadena"));
            tope = "id TCadena";
            mensaje = argumento.esId ? "requiere un id Cadena; recibido " + etiqueta + "."
                : "requiere un identificador; recibido un valor de tipo " + etiqueta + ".";
        } else if (!regla.soloId.isEmpty()) {                            // 2007: solo un id de Bin, Dec, Oct o Hex
            aceptado = variant || (argumento.esId && regla.soloId.contains(tipo));
            tope = "id " + topeTipos(regla.soloId, false);
            mensaje = argumento.esId ? "requiere un id " + listaEtiquetas(regla.soloId) + "; recibido " + etiqueta + "."
                : "requiere un identificador; recibido un valor de tipo " + etiqueta + ".";
        } else if (parametro.equals("val_p3")) {                         // 2004: Dec con valor 2, 8, 10 o 16, o un id Dec
            aceptado = variant || regla.tipos.contains(tipo);
            mensaje = "requiere Dec con valor 2, 8, 10, 16; recibido " + etiqueta + ".";
            Long valor = argumento.valorConstante;
            if (aceptado && valor != null && valor != 2 && valor != 8 && valor != 10 && valor != 16) {
                aceptado = false;
                mensaje = "requiere Dec con valor 2, 8, 10, 16; recibido el valor " + valor + ".";
            }
            tope = "TDec(2,8,10,16)/id";
        } else {
            aceptado = variant || regla.tipos.contains(tipo);
            tope = topeTipos(regla.tipos, regla.id);
            mensaje = "requiere " + listaEtiquetas(regla.tipos) + "; recibido " + etiqueta + ".";
        }

        registrarSemantica3(linea, regla.regla, parametro, aceptado, argumento.texto, tope, mensaje);
        return aceptado;
    }

    // Registra una fila de Semántica 3 (misma lista que Semántica 2, con la función al final). Si falla, va a la tabla de Errores.
    private void registrarSemantica3(int linea, int regla, String funcion, boolean aceptada, String valorReal, String tope, String mensajeError) {
        int ambito = pilaAmbitos.isEmpty() ? 0 : pilaAmbitos.peek();
        registroSemantica2.add(new Object[] { linea, regla, funcion, aceptada ? "ACEPT" : "ERROR", valorReal, ambito, tope, funcion });
        if (!aceptada && gui != null) {
            gui.getModeloErrores().addRow(new Object[] {
                String.valueOf(regla), "ERROR: " + funcion + ": " + mensajeError, funcion + ": " + valorReal, "Semántica 3", String.valueOf(linea) });
        }
    }

    public Map<String, LecturaMatriz.ReglaSemantica3> getReglasSemantica3() {
        return lectorMatriz.getReglasSemantica3();
    }

    public List<Object[]> getRegistroSemantica2() {
        return registroSemantica2;
    }

    // Registra una regla de Semántica 2. valorReal es el valor evaluado y tope lo que se esperaba en el tope de la pila semántica
    // (tipo, rango, clase...). Si la regla falla, también se imprime en la tabla de Errores.
    private void registrarSemantica2(int linea, int regla, String descripcion, boolean aceptada, String valorReal, String tope) {
        registrarSemantica2(linea, regla, descripcion, aceptada, valorReal, tope, valorReal);
    }

    private void registrarSemantica2(int linea, int regla, String descripcion, boolean aceptada, String valorReal, String tope,
                                     String lexemaError) {
        int ambito = pilaAmbitos.isEmpty() ? 0 : pilaAmbitos.peek();
        registroSemantica2.add(new Object[] { linea, regla, descripcion, aceptada ? "ACEPT" : "ERROR", valorReal, ambito, tope });
        // 1100 ya se reporta como error de ámbito (545) y 1020-1022 como errores de Semántica 1: no se repiten en Errores
        if (!aceptada && regla != 1100 && regla != 1110 && regla != 1120 && (regla < 1020 || regla > 1022) && gui != null) {
            gui.getModeloErrores().addRow(new Object[] {
                String.valueOf(regla), mensajeReglaSemantica2(regla, lexemaError), lexemaError, "Semántica 2", String.valueOf(linea) });
        }
    }

    private String mensajeReglaSemantica2(int regla, String lexema) {
        switch (regla) {
            case 1010: return "La expresión debe ser lógica para IF";
            case 1011: return "La expresión debe ser lógica para ELSEIF";
            case 1012: return "La expresión debe ser lógica para WHILE";
            case 1013: return "La expresión debe ser lógica para DO-WHILE";
            case 1030: return "La cantidad de dimensiones en un arreglo debe ser igual a la declarada.";
            case 1040: return "La expresión de la lista de arreglo debe ser entera.";
            case 1050: return "Los límites al usar un arreglo deben estar en el mismo rango de la declaración.";
            case 1060: return "La expresión del switch debe ser de tipo decimal, cadena o variant";
            case 1061: return "El caso es del mismo tipo que la expresion de SWITCH";
            case 1070:
                if (lexema.startsWith("!")) return "El operador ! solo puede ser utilizado con una expresión lógica";
                if (lexema.startsWith("~")) return "El operador ~ solo puede ser utilizado con valores numéricos enteros, octales, binarios o hexadecimales";
                return "El operador unario solo puede ser utilizado con valores numéricos enteros";
            case 1080: return "En el for debe inicializarse un identificador con =";
            case 1081: return "La expresión debe ser lógica para FOR";
            case 1082: return "El incremento o decremento del for debe ser ++id o --id";
            case 1083: return "En el for each de cadenas el identificador debe ser decimal y recorrer una cadena";
            case 1084: return "En el for each el identificador debe ser del mismo tipo que el arreglo";
            case 1090: return "El identificador debe ser de clase variable, parámetro o arreglo para recibir un valor";
            case 1130: return "La función debe contener por lo menos un retorno";
            case 1131: return "El tipo del retorno debe ser igual al tipo de la función";
            case 1140: return "Un procedimiento no debe tener retorno";
            case 1150: return "La cantidad de parámetros debe ser igual a la declarada";
            default: return "El tipo del parámetro debe ser igual al declarado";
        }
    }

    // Etiqueta de tipo con el formato de la evidencia: Dec, Real, Cadena, Boolean...
    private String etiquetaT(String tipo) {
        return etiquetaTipo(tipo);
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
            aceptada = cabeEn(tipoDestino, tipoValor);
            if (!aceptada) detalle = "tipos incompatibles entre " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor);
        } else {
            String clave = operador.equals("+=") ? "SUMA" : operador.equals("-=") ? "RESTA" : operador.equals("*=") ? "MULT" : "DIV";
            int[][] tabla = lectorMatriz.getTablaCompatibilidad(clave);
            int celda = (tabla != null) ? tabla[indiceTipo(tipoDestino)][indiceTipo(tipoValor)] : 0;
            int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
            if (celda > 0) {
                aceptada = false;
                tipoResultado = "variant";
                detalle = "operacion incompatible entre " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor);
                // Una asignación compuesta incompatible es un error de Semántica 2 (regla 1021 para +=, 1022 para -=, *= y /=)
                if (!asignacionIncompatibleReportada) erroresTiposPendientes.add(new Object[] {
                    String.valueOf(regla),
                    "Tipos incompatibles para '" + operador + "': " + etiquetaTipo(tipoDestino) + " y " + etiquetaTipo(tipoValor),
                    lexema, "Semántica 2", String.valueOf(linea) });
            } else {
                tipoResultado = tipoDeCodigo(celda);
                // El resultado de la operación debe caber en el destino (p. ej. #Dx /= 2 da Real y no cabe en Dec)
                if (tipoResultado != null && !tipoResultado.equals("variant") && !cabeEn(tipoDestino, tipoResultado)) {
                    aceptada = false;
                    detalle = "el resultado " + etiquetaTipo(tipoResultado) + " no cabe en " + etiquetaTipo(tipoDestino);
                    if (!asignacionIncompatibleReportada) erroresTiposPendientes.add(new Object[] {
                        String.valueOf(regla),
                        "La asignación compuesta '" + operador + "' no es compatible: el resultado " + etiquetaTipo(tipoResultado)
                            + " no cabe en " + etiquetaTipo(tipoDestino),
                        lexema, "Semántica 2", String.valueOf(linea) });
                }
            }
        }
        String descripcion = prefijoDescripcion + (detalle != null ? detalle + "."
            : "destino " + etiquetaTipo(tipoDestino) + ", resultado " + etiquetaTipo(tipoResultado) + ".");
        asignacionPendiente = new Object[] { regla, descripcion, aceptada, valor.valor, etiquetaT(tipoDestino.equals("variant") ? tipoValor : tipoDestino), lexema };
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

        // Un procedimiento (Void) no puede ser operando: error con el código de la tabla de la operación
        if (clave != null && (operandos[0].tipo.equals("void") || operandos[1].tipo.equals("void"))) {
            int codigo = clave.equals("SUMA") ? 547 : clave.equals("RESTA") ? 548 : clave.equals("MULT") ? 549 : clave.equals("DIV") ? 550
                : clave.equals("REL") ? 551 : clave.equals("LOG") ? 552 : clave.equals("REL2") ? 553 : 554;
            int linea = (ultimoTokenConsumido != null) ? ultimoTokenConsumido.linea : 0;
            erroresTiposPendientes.add(new Object[] {
                String.valueOf(codigo),
                "Tipos incompatibles para '" + operador + "' (" + nombre + "): "
                    + etiquetaTipo(operandos[0].tipo) + " y " + etiquetaTipo(operandos[1].tipo),
                operandos[0].valor + " " + operador + " " + operandos[1].valor, "Semántica 1", String.valueOf(linea) });
            return "variant";
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
                operandos[0].valor + " " + operador + " " + operandos[1].valor, "Semántica 1", String.valueOf(linea) });
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
            case "void": return "TVoid";
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