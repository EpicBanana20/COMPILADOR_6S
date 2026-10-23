package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AbrirArchivo {

    private CompiladorGUI gui;

    public AbrirArchivo(CompiladorGUI gui) {
        this.gui = gui;
    }

    public void ejecutar() {

        FileDialog dialogo = new FileDialog((Frame) null, "Selecciona un archivo", FileDialog.LOAD);

        dialogo.setFile("*.txt"); // Windows: el diálogo nativo filtra por este patrón
        dialogo.setFilenameFilter((carpeta, nombre) -> nombre.toLowerCase().endsWith(".txt")); // otros sistemas

        dialogo.setVisible(true);
        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (file != null && file.toLowerCase().endsWith(".txt")) {
            File archivo = new File(dir, file);

            gui.getLabelRutaArchivo().setText(archivo.getAbsolutePath());

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                gui.getEditorCodigo().read(br, null);
            } catch (Exception ex) {
                System.err.println("Error al leer el archivo: " + ex.getMessage());
            }
        }
    }
}