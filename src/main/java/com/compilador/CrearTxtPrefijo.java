package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import javax.swing.JOptionPane;

public class CrearTxtPrefijo {

    private CompiladorGUI gui;
    private Parser parser;

    public CrearTxtPrefijo(CompiladorGUI gui, Parser parser) {
        this.gui = gui;
        this.parser = parser;
    }

    public void ejecutar() {
        FileDialog dialogo = new FileDialog((Frame) null, "Guardar reporte de Prefijos en TXT", FileDialog.SAVE);
        dialogo.setFile("Reporte_Prefijos_MezaAlex.txt");
        dialogo.setVisible(true);

        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (dir != null && file != null) {
            if (!file.toLowerCase().endsWith(".txt")) {
                file += ".txt";
            }
            String rutaAbsoluta = new File(dir, file).getAbsolutePath();
            List<String> logPrefijos = parser != null ? parser.getLogPrefijos() : new java.util.ArrayList<>();
            generarTxt(rutaAbsoluta, logPrefijos);
        }
    }

    private void generarTxt(String rutaAbsoluta, List<String> logPrefijos) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaAbsoluta))) {
            writer.write("=== Prefijos ===");
            writer.newLine();
            for (String linea : logPrefijos) {
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
