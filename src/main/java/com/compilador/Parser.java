package com.compilador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Parser {

    private LecturaMatriz lectorMatriz;
    private List<Token> tokens;
    private int posicionActual;
    private Stack<String> pila;
    private List<String> erroresSintacticos;
    private Map<String, Integer> contadoresDiagramasPrincipales;

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

    public Parser(LecturaMatriz lectorMatriz) {
        this.lectorMatriz = lectorMatriz;
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

            System.out.println("\n--- Paso " + pasos + " ---");
            List<String> pilaNormalizada = new ArrayList<>();
            for (String s : pila) {
                pilaNormalizada.add(normalizarNoTerminal(s));
            }
            System.out.println("Pila: " + pilaNormalizada);
            System.out.println("Token actual: " + (tokenActual != null ? tokenActual : "FIN DE ENTRADA"));

            if (cimaPila.equals("$")) {
                if (simboloActual.equals("$")) {
                    System.out.println(">>> ACCEPT: Análisis completado exitosamente!");
                    break;
                } else {
                    System.out.println(">>> ERROR: Se esperaba EOF pero se encontró: " + simboloActual);
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) + ": Se esperaba FIN DE ARCHIVO");
                    analisisExitoso = false;
                    break;
                }
            }

            if (esNoTerminal(cimaPila)) {
                Integer codigoProduccion = obtenerCodigoProduccion(cimaPila, simboloActual);

                if (codigoProduccion != null) {
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
                    System.out.println(">>> ERROR: No hay producción para " + cimaNorm + " con token " + simboloActual);
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) + 
                        ": No se esperaba '" + simboloActual + "' después de '" + cimaNorm + "'");
                    analisisExitoso = false;
                    break;
                }
            } else {
                String cimaNormalizada = normalizarSimboloComparar(cimaPila);
                String tokenNormalizado = normalizarToken(simboloActual);
                
                boolean coinciden = cimaNormalizada.equals(tokenNormalizado);
                
                // Ya no hay chequeos especiales para paréntesis - tokens separados por espacios
                
                if (coinciden) {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    System.out.println(">>> MATCH: " + cimaNorm + " coincide con " + simboloActual);
                    pila.pop();
                    posicionActual++;
                } else {
                    String cimaNorm = normalizarNoTerminal(cimaPila);
                    System.out.println(">>> ERROR: Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActual + "'");
                    erroresSintacticos.add("Error en línea " + (tokenActual != null ? tokenActual.linea : 0) + 
                        ": Se esperaba '" + cimaNorm + "' pero se encontró '" + simboloActual + "'");
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
            }
        }
        return null;
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