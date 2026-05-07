package com.compilador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PruebaParser {

    public static void main(String[] args) {
        System.out.println("========== PRUEBA UNICA DEL PARSER ==========\n");

        LecturaMatriz lectorMatriz = new LecturaMatriz();
        lectorMatriz.cargarMatriz("C2A6.csv");
        lectorMatriz.cargarMatrizParser("C3A4_ALEXMEZA.csv");
        lectorMatriz.cargarProducciones("producciones_gramatica.csv");

        System.out.println("=== CSVs cargados correctamente ===\n");

        Parser parser = new Parser(lectorMatriz);

        System.out.println("\n=== PRUEBA: ElseIf (palabra reservada nueva) ===");
        List<Parser.Token> tokens = crearTokensElseIf();
        System.out.println("Tokens: " + tokens + "\n");

        parser.ejecutar(tokens);
        List<String> errores = parser.getErroresSintacticos();

        if (errores.isEmpty()) {
            System.out.println("\n>>> EXITO: La prueba passo!");
        } else {
            System.out.println("\n>>> FALLO: La prueba fallo!");
            System.out.println("Errores: " + errores);
        }

        Map<String, Integer> contadores = parser.getContadoresDiagramasPrincipales();
        int total = 0;
        for (Integer count : contadores.values()) {
            total += count;
        }

        System.out.println("\n========== RESUMEN DE DIAGRAMAS PRINCIPALES ==========");
        System.out.println("TOTAL: " + total);
        System.out.println("-------------------------------------------------------");
        for (Map.Entry<String, Integer> entry : contadores.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("========================================================");
    }

    private static List<Parser.Token> crearTokensElseIf() {
        List<Parser.Token> tokens = new ArrayList<>();
        // var x[5]; - Producción 79
        tokens.add(new Parser.Token("var", "var", 1));
        tokens.add(new Parser.Token("id", "x", 1));
        tokens.add(new Parser.Token("[", "[", 1));
        tokens.add(new Parser.Token("Const_Decimal", "5", 1));
        tokens.add(new Parser.Token("ent", "ent", 1));
        tokens.add(new Parser.Token("]", "]", 1));
        tokens.add(new Parser.Token(";", ";", 1));
        // var y[3]; - Producción 79 (segundo PROGRAMA)
        tokens.add(new Parser.Token("var", "var", 2));
        tokens.add(new Parser.Token("id", "y", 2));
        tokens.add(new Parser.Token("[", "[", 2));
        tokens.add(new Parser.Token("Const_Decimal", "3", 2));
        tokens.add(new Parser.Token("ent", "ent", 2));
        tokens.add(new Parser.Token("]", "]", 2));
        tokens.add(new Parser.Token(";", ";", 2));
        tokens.add(new Parser.Token("$", "$", 2));
        return tokens;
    }
}