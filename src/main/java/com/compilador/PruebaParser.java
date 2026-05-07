package com.compilador;

import java.util.ArrayList;
import java.util.List;

public class PruebaParser {

    public static void main(String[] args) {
        System.out.println("========== PRUEBA DEL PARSER ==========\n");

        LecturaMatriz lectorMatriz = new LecturaMatriz();
        lectorMatriz.cargarMatriz("C2A6.csv");
        lectorMatriz.cargarMatrizParser("C3A4_ALEXMEZA.csv");
        lectorMatriz.cargarProducciones("producciones_gramatica.csv");

        System.out.println("=== CSVs cargados correctamente ===\n");
        
        System.out.println("--- Produccion 82 (PROGRAMA -> main) ---");
        System.out.println(lectorMatriz.getProduccion(82));
        
        System.out.println("\n--- Todas las producciones de PROGRAMA ---");
        for (int i = 78; i <= 82; i++) {
            System.out.println("Prod " + i + ": " + lectorMatriz.getProduccion(i));
        }

        List<Parser.Token> tokensPrueba = new ArrayList<>();

        // Prueba: for (i=0; i<5; i=i+1) { x = 1; }
        tokensPrueba.add(new Parser.Token("main", "main", 1));
        tokensPrueba.add(new Parser.Token("(", "(", 1));
        tokensPrueba.add(new Parser.Token(")", ")", 1));
        tokensPrueba.add(new Parser.Token("{", "{", 1));
        tokensPrueba.add(new Parser.Token("for", "for", 2));
        tokensPrueba.add(new Parser.Token("(", "(", 2));
        tokensPrueba.add(new Parser.Token("id", "i", 2));
        tokensPrueba.add(new Parser.Token("=", "=", 2));
        tokensPrueba.add(new Parser.Token("Const_Decimal", "0", 2));
        tokensPrueba.add(new Parser.Token(";", ";", 2));
        tokensPrueba.add(new Parser.Token("id", "i", 2));
        tokensPrueba.add(new Parser.Token("<", "<", 2));
        tokensPrueba.add(new Parser.Token("Const_Decimal", "5", 2));
        tokensPrueba.add(new Parser.Token(";", ";", 2));
        tokensPrueba.add(new Parser.Token("id", "i", 2));
        tokensPrueba.add(new Parser.Token("=", "=", 2));
        tokensPrueba.add(new Parser.Token("id", "i", 2));
        tokensPrueba.add(new Parser.Token("+", "+", 2));
        tokensPrueba.add(new Parser.Token("Const_Decimal", "1", 2));
        tokensPrueba.add(new Parser.Token(")", ")", 2));
        tokensPrueba.add(new Parser.Token("{", "{", 2));
        tokensPrueba.add(new Parser.Token("id", "x", 2));
        tokensPrueba.add(new Parser.Token("=", "=", 2));
        tokensPrueba.add(new Parser.Token("Const_Decimal", "1", 2));
        tokensPrueba.add(new Parser.Token(";", ";", 2));
        tokensPrueba.add(new Parser.Token("}", "}", 2));
        tokensPrueba.add(new Parser.Token("}", "}", 3));
        tokensPrueba.add(new Parser.Token("$", "$", 3));

        System.out.println("Tokens de prueba:");
        for (Parser.Token t : tokensPrueba) {
            System.out.println("  " + t);
        }
        System.out.println();

        Parser parser = new Parser(lectorMatriz);
        parser.ejecutar(tokensPrueba);
        System.out.println("\n========== FIN DE LA PRUEBA ==========");
    }
}