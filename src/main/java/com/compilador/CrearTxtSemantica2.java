package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import javax.swing.JOptionPane;

public class CrearTxtSemantica2 {

    private CompiladorGUI gui;
    private Parser parser;

    public CrearTxtSemantica2(CompiladorGUI gui, Parser parser) {
        this.gui = gui;
        this.parser = parser;
    }

    public void ejecutar() {
        FileDialog dialogo = new FileDialog((Frame) null, "Guardar registro de Semántica 2 en TXT", FileDialog.SAVE);
        dialogo.setFile("Semantica2_MezaAlex.txt");
        dialogo.setVisible(true);

        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (dir != null && file != null) {
            if (!file.toLowerCase().endsWith(".txt")) {
                file += ".txt";
            }
            String rutaAbsoluta = new File(dir, file).getAbsolutePath();
            List<Object[]> registro = parser != null ? parser.getRegistroSemantica2() : new java.util.ArrayList<Object[]>();
            generarTxt(rutaAbsoluta, registro);
        }
    }

    private void generarTxt(String rutaAbsoluta, List<Object[]> registro) {
        String[] titulos = { "Linea", "Regla", "Descripcion", "Situacion de la regla", "Lexema", "Ambito" };
        int[] anchos = new int[titulos.length];
        for (int i = 0; i < titulos.length; i++) anchos[i] = titulos[i].length();
        int aceptadas = 0;
        for (Object[] fila : registro) {
            if ("ACEPT".equals(fila[3])) aceptadas++;
            for (int i = 0; i < titulos.length; i++) {
                anchos[i] = Math.max(anchos[i], String.valueOf(fila[i]).length());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(rutaAbsoluta), "UTF-8"))) {
            writer.write(fila(titulos, anchos));
            writer.newLine();
            for (Object[] f : registro) {
                String[] celdas = new String[titulos.length];
                for (int i = 0; i < celdas.length; i++) celdas[i] = String.valueOf(f[i]);
                writer.write(fila(celdas, anchos));
                writer.newLine();
            }
            writer.newLine();
            writer.write("Evaluadas: " + registro.size() + "   Aceptadas: " + aceptadas + "   Rechazadas: " + (registro.size() - aceptadas));
            writer.newLine();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(gui,
                "Error al guardar el archivo: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
        JOptionPane.showMessageDialog(gui,
            "¡El archivo TXT se generó correctamente!",
            "Exportación Exitosa",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // Una línea de la tabla: celdas rellenadas al ancho de su columna y separadas por " | "
    private String fila(String[] celdas, int[] anchos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(celdas[i]);
            if (i < celdas.length - 1) {
                for (int k = celdas[i].length(); k < anchos[i]; k++) sb.append(' ');
            }
        }
        return sb.toString();
    }
}
