package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import javax.swing.JOptionPane;

public class CrearTxtTemporales {

    private CompiladorGUI gui;
    private Parser parser;

    public CrearTxtTemporales(CompiladorGUI gui, Parser parser) {
        this.gui = gui;
        this.parser = parser;
    }

    public void ejecutar() {
        FileDialog dialogo = new FileDialog((Frame) null, "Guardar reporte de Temporales en TXT", FileDialog.SAVE);
        dialogo.setFile("Reporte_Temporales_MezaAlex.txt");
        dialogo.setVisible(true);

        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (dir != null && file != null) {
            if (!file.toLowerCase().endsWith(".txt")) {
                file += ".txt";
            }
            String rutaAbsoluta = new File(dir, file).getAbsolutePath();
            List<String> logCuadruplos = parser != null ? parser.getLogCuadruplos() : new java.util.ArrayList<>();
            generarTxt(rutaAbsoluta, logCuadruplos);
        }
    }

    private void generarTxt(String rutaAbsoluta, List<String> logCuadruplos) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaAbsoluta))) {
            boolean primero = true;
            for (String linea : logCuadruplos) {
                if (linea.startsWith("Linea ") && !primero) {
                    writer.newLine();
                }
                primero = false;
                writer.write(linea);
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
}
