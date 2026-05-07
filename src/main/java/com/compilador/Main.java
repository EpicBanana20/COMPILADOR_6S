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
            
            // CARGAR MATRIZ
            LecturaMatriz lectorMatriz = new LecturaMatriz();
            lectorMatriz.cargarMatriz("C2A6.csv");
            
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