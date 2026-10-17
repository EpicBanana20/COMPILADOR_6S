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
        int aceptadas = 0;
        int anchoDescripcion = "Descripcion".length();
        int anchoLexema = "Lexema".length();
        for (Object[] fila : registro) {
            if ("Aceptada".equals(fila[3])) aceptadas++;
            anchoDescripcion = Math.max(anchoDescripcion, String.valueOf(fila[2]).length());
            anchoLexema = Math.max(anchoLexema, String.valueOf(fila[4]).length());
        }

        try (BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(rutaAbsoluta), "UTF-8"))) {
            writer.write("SEMANTICA 2 - REGISTRO DE REGLAS");
            writer.newLine();
            writer.write("Evaluadas: " + registro.size() + "   Aceptadas: " + aceptadas + "   Rechazadas: " + (registro.size() - aceptadas));
            writer.newLine();
            writer.newLine();

            String encabezado = relleno("Linea", 5) + " | " + relleno("Regla", 5) + " | " + relleno("Descripcion", anchoDescripcion)
                + " | " + relleno("Situacion", 9) + " | " + relleno("Lexema", anchoLexema) + " | Ambito";
            writer.write(encabezado);
            writer.newLine();
            StringBuilder separador = new StringBuilder();
            for (int i = 0; i < encabezado.length(); i++) separador.append('-');
            writer.write(separador.toString());
            writer.newLine();

            for (Object[] fila : registro) {
                writer.write(relleno(String.valueOf(fila[0]), 5) + " | " + relleno(String.valueOf(fila[1]), 5)
                    + " | " + relleno(String.valueOf(fila[2]), anchoDescripcion)
                    + " | " + relleno(String.valueOf(fila[3]), 9)
                    + " | " + relleno(String.valueOf(fila[4]), anchoLexema)
                    + " | " + fila[5]);
                writer.newLine();
            }

            JOptionPane.showMessageDialog(gui,
                "¡El archivo TXT se generó correctamente!",
                "Exportación Exitosa",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(gui,
                "Error al guardar el archivo: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private String relleno(String texto, int ancho) {
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < ancho) sb.append(' ');
        return sb.toString();
    }
}
