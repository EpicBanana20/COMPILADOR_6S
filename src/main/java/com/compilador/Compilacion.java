package com.compilador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Compilacion {

    private CompiladorGUI gui;
    private LecturaMatriz lectorMatriz;
    private List<Parser.Token> tokensAnalizados;

    private static final Map<String, String> PALABRAS_RESERVADAS = new LinkedHashMap<>();

    static {
        String[] palabras = {
            // Control
            "if", "else", "elseif", "switch", "case", "default", "for", "do", "while", "break", "return",
            // Declaraciones
            "reg", "var", "def",
            // Consola
            "Console.read", "Console.log",
            // Funciones integradas
            "CLEAR", "SQRT", "POW", "SQRTV", "STRLEN", "sin", "cos", "tan", "chr", "pred", "succ", "inc", "dec", "sqr", "copy", "val", "str",
            // Extra (mantener algunas)
            "console.log", "true", "false", "null", "continue", "class", "main"
        };
        int tokenActual = -71;
        for (String palabra : palabras) {
            PALABRAS_RESERVADAS.put(palabra, String.valueOf(tokenActual));
            tokenActual--;
        }
    }

    public Compilacion(CompiladorGUI gui, LecturaMatriz lectorMatriz) {
        this.gui = gui;
        this.lectorMatriz = lectorMatriz;
        this.tokensAnalizados = new ArrayList<>();
    }

    public List<Parser.Token> getTokensAnalizados() {
        return tokensAnalizados;
    }

    public void ejecutar() {

        gui.getModeloTokens().setRowCount(0);
        gui.getModeloErrores().setRowCount(0);
        gui.getModeloPila().setRowCount(0);
        tokensAnalizados.clear();

        String codigoFuente = gui.getEditorCodigo().getText();
        Map<String, Map<String, String>> matriz = lectorMatriz.getMatriz();

        if (codigoFuente.isEmpty())
            return;
        if (matriz == null || matriz.isEmpty()) {
            System.err.println(" Error: La matriz no está cargada.");
            return;
        }

        Map<String, Integer> contadores = new LinkedHashMap<>();

        String estadoActual = "0";
        StringBuilder lexemaActual = new StringBuilder();
        int lineaActual = 1;

        codigoFuente += "\0";

        for (int i = 0; i < codigoFuente.length(); i++) {
            char c = codigoFuente.charAt(i);

            if (c == '\n')
                lineaActual++;

            String columna = clasificarCaracter(c);
            Map<String, String> filaEstado = matriz.get(estadoActual);
            
            if (filaEstado == null) {
                String lexError = lexemaActual.toString();
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                registrarError("500", "Estado inexistente [" + estadoActual + "]", lexError, lineaRegistro);
                registrarConteo(contadores, "Errores Críticos");
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                i--;
                if (c == '\n') lineaActual--;
                
                continue; 
            }

            String siguienteEstado = filaEstado.get(columna);

            if (siguienteEstado == null || siguienteEstado.trim().isEmpty()) {
                String lexError = (lexemaActual.toString() + c).trim();
                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                registrarError("500", "Transición no definida para '" + c + "'", lexError, lineaRegistro);
                registrarConteo(contadores, "Errores Léxicos");
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                continue; 
            }

            // ====================================================================
            // DETECCIÓN DE ERRORES DIRECTOS DE LA MATRIZ (500, 501, 502, etc.)
            // ====================================================================
            if (siguienteEstado.length() >= 3 && (siguienteEstado.startsWith("50") || siguienteEstado.startsWith("51"))) {
                String lexError = (lexemaActual.toString() + c).replace("\0", "").trim();
                int lineaRegistro = (c == '\n' || c == '\0') ? lineaActual - 1 : lineaActual;
                String descError = obtenerDescripcionError(siguienteEstado);
                
                registrarError(siguienteEstado, descError, lexError, lineaRegistro);
                registrarConteo(contadores, "Errores Léxicos");
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                continue; 
            }

            // ====================================================================
            // MANEJO DE ACEPTACIÓN POR DELIMITADOR (Estado negativo alcanzado)
            // ====================================================================
            if (siguienteEstado.startsWith("-")) {
                String tokenEncontrado = siguienteEstado;
                String palabraFormada = lexemaActual.toString().trim();
                String familia = obtenerAgrupacion(tokenEncontrado);

                // RESTRICCIÓN: Solo aplicamos lógica de diccionario si el autómata arrojó -70
                if (tokenEncontrado.equals("-70")) {
                    if (PALABRAS_RESERVADAS.containsKey(palabraFormada)) {
                        tokenEncontrado = PALABRAS_RESERVADAS.get(palabraFormada); 
                        
                        if (palabraFormada.equals("true") || palabraFormada.equals("false")) {
                            familia = "Constantes Booleanas";
                        } else if (palabraFormada.equals("null")) {
                            familia = "Constante nula";
                        } else {
                            familia = "Palabras Reservadas";
                        }
                    } else {
                        int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;
                        registrarError("500", "Palabra reservada no reconocida", palabraFormada, lineaRegistro);
                        registrarConteo(contadores, "Errores Léxicos");
                        
                        estadoActual = "0";
                        lexemaActual.setLength(0);
                        
                        i--;
                        if (c == '\n') lineaActual--;
                        
                        continue;
                    }
                }

                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                if (!tokenEncontrado.equals("-11") && !tokenEncontrado.equals("-12")) {
                    gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaRegistro });
                    tokensAnalizados.add(new Parser.Token(tokenEncontrado, palabraFormada, lineaRegistro));
                }
                
                registrarConteo(contadores, familia); 
                
                estadoActual = "0";
                lexemaActual.setLength(0);
                
                i--;
                if (c == '\n') lineaActual--; 

                continue;
            }

            if (!(estadoActual.equals("0") && estadoActual.equals(siguienteEstado))) {
                lexemaActual.append(c);
            }

            estadoActual = siguienteEstado;


            if (estadoActual.startsWith("-")) {
                String tokenEncontrado = estadoActual;
                String palabraFormada = lexemaActual.toString().trim();
                String familia = obtenerAgrupacion(tokenEncontrado);

                if (tokenEncontrado.equals("-70")) {
                    if (PALABRAS_RESERVADAS.containsKey(palabraFormada)) {
                        tokenEncontrado = PALABRAS_RESERVADAS.get(palabraFormada);
                        
                        if (palabraFormada.equals("true") || palabraFormada.equals("false")) {
                            familia = "Constantes Booleanas";
                        } else if (palabraFormada.equals("null")) {
                            familia = "Constante nula";
                        } else {
                            familia = "Palabras Reservadas";
                        }
                    } else {
                        int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;
                        registrarError("500", "Palabra reservada no reconocida", palabraFormada, lineaRegistro);
                        registrarConteo(contadores, "Errores Léxicos");
                        
                        estadoActual = "0";
                        lexemaActual.setLength(0);
                        
                        i--;
                        if (c == '\n') lineaActual--;
                        
                        continue;
                    }
                }

                int lineaRegistro = (c == '\n') ? lineaActual - 1 : lineaActual;

                if (!tokenEncontrado.equals("-11") && !tokenEncontrado.equals("-12")) {
                    gui.getModeloTokens().addRow(new Object[] { tokenEncontrado, palabraFormada, lineaRegistro });
                    tokensAnalizados.add(new Parser.Token(tokenEncontrado, palabraFormada, lineaRegistro));
                }
                
                registrarConteo(contadores, familia);

                estadoActual = "0";
                lexemaActual.setLength(0);

                i--;
                if (c == '\n')
                    lineaActual--; 
            }
        }

        List<String> ordenDeseado = Arrays.asList(
            "Operadores matematicos", "Operadores postfix", "Operadores de asignacion",
            "Operador exponente", "Operadores relacionales", 
            "Operadores sin igualdad de conversion de tipo", "Operadores de turno",
            "Operadores logicos", "Operadores logicos binarios", "Operador ternario",
            "Operador de control", "Operador de agrupamiento",
            "Palabras Reservadas",
            "Comentarios",
            "Cadena",
            "Numerica Binario", "Numerica Decimal", "Numerica Octal", "Numerica Hexadecimal", 
            "Numerica Real", "Numerica Exponencial",
            "Constantes Booleanas", "Constante nula",
            "Cadena Identificador", "Numerica Binario Identificador", 
            "Numerica Decimal Identificador", "Numerica Octal Identificador", 
            "Numerica Hexadecimal Identificador", "Real Identificador", 
            "Exponencial Identificador", "Booleana Identificador"
        );

        Set<String> categoriasImpresas = new HashSet<>();
        
        for (String categoria : ordenDeseado) {
            if (contadores.containsKey(categoria)) {
                gui.getModeloPila().addRow(new Object[]{categoria, contadores.get(categoria)});
                categoriasImpresas.add(categoria);
            }
        }

        for (Map.Entry<String, Integer> entry : contadores.entrySet()) {
            if (!categoriasImpresas.contains(entry.getKey())) {
                gui.getModeloPila().addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void registrarConteo(Map<String, Integer> contadores, String clasificacion) {
        if (clasificacion == null || clasificacion.trim().isEmpty()) return;
        contadores.put(clasificacion, contadores.getOrDefault(clasificacion, 0) + 1);
    }

    private void registrarError(String tokenError, String descripcion, String lexema, int linea) {
        gui.getModeloErrores().addRow(new Object[] { tokenError, descripcion, lexema, "Léxico", String.valueOf(linea) });
    }

    private String obtenerDescripcionError(String estadoError) {
        switch (estadoError) {
            case "500": return "Error: Se espera un caracter valido";
            case "501": return "Error: Cadena sin cerrar";
            case "502": return "Error: Flotante incompleto";
            case "503": return "Error: Exponente incompleto";
            case "504": return "Error: Base numerica invalida";
            case "505": return "Error: Binario incompleto";
            case "506": return "Error: Octal incompleto";
            case "507": return "Error: Hexadecimal incompleto";
            case "508": return "Error: Identificador incompleto";
            case "509": return "Error: Se esperaba B,D,O,X";
            case "510": return "Error: Se esperaba un elemento alfabetico";
            case "511": return "Error: Comentario sin cerrar";
            default: return "Error léxico (" + estadoError + ")";
        }
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
            
            case "-11": case "-12": 
                return "Comentarios";
                
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
            case "-111": return "Operadores matematicos";

            default: 
                return "Agrupación Desconocida (" + estadoFinal + ")";
        }
    }

    private String clasificarCaracter(char c) {
        if (c == '\0') return "EOF";
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