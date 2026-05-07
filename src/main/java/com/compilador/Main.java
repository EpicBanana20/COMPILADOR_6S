package com.compilador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el LookAndFeel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            // INICIALIZACIÓN GUI
            CompiladorGUI ventana = new CompiladorGUI();
            
            // CARGAR MATRIZ LÉXICA
            LecturaMatriz lectorMatriz = new LecturaMatriz();
            lectorMatriz.cargarMatriz("C2A6.csv");
            
            // CARGAR MATRIZ DEL PARSER (Sintaxis)
            lectorMatriz.cargarMatrizParser("C3A4_ALEXMEZA.csv");
            
            // CARGAR PRODUCCIONES GRAMATICALES
            lectorMatriz.cargarProducciones("producciones_gramatica.csv");
            
            // Imprimir para verificar
            System.out.println("\n=== VERIFICACIÓN DE CARGA ===");
            System.out.println("Estados léxicos: " + lectorMatriz.getMatriz().size());
            System.out.println("No terminales: " + lectorMatriz.getMatrizParser().size());
            System.out.println("Producciones: " + lectorMatriz.getProducciones().size());
            
            // Mostrar ejemplo de producciones
            System.out.println("\n--- Ejemplo de producciones ---");
            System.out.println("Prod 1: " + lectorMatriz.getProduccion(1));
            System.out.println("Prod 78: " + lectorMatriz.getProduccion(78));
            System.out.println("Prod 82: " + lectorMatriz.getProduccion(82));
            
            // ABRIR ARCHIVO
            AbrirArchivo logicaAbrir = new AbrirArchivo(ventana);
            ventana.getBtnAbrir().addActionListener(e -> logicaAbrir.ejecutar());

            // COMPILACIÓN
            Compilacion logicaCompilar = new Compilacion(ventana, lectorMatriz);
            ventana.getBtnCompilar().addActionListener(e -> logicaCompilar.ejecutar());

            // CREACIÓN DEL EXCEL
            CrearXLS logicaExcel = new CrearXLS(ventana);
            ventana.getBtnCrearXls().addActionListener(e -> logicaExcel.ejecutar());
            
            ventana.setVisible(true);
        });
    }
}