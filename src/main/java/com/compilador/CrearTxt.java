package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import javax.swing.JOptionPane;

public class CrearTxt {

    private CompiladorGUI gui;
    private Parser parser;

    public CrearTxt(CompiladorGUI gui, Parser parser) {
        this.gui = gui;
        this.parser = parser;
    }

    public void ejecutar() {
        FileDialog dialogo = new FileDialog((Frame) null, "Guardar reporte de Áreas en TXT", FileDialog.SAVE);
        dialogo.setFile("Reporte_Areas_MezaAlex.txt");
        dialogo.setVisible(true);

        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (dir != null && file != null) {
            if (!file.toLowerCase().endsWith(".txt")) {
                file += ".txt";
            }
            String rutaAbsoluta = new File(dir, file).getAbsolutePath();
            List<String> logAreas = parser != null ? parser.getLogAreas() : new java.util.ArrayList<>();
            generarTxt(rutaAbsoluta, logAreas);
        }
    }

    private void generarTxt(String rutaAbsoluta, List<String> logAreas) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaAbsoluta))) {
            for (String linea : logAreas) {
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
