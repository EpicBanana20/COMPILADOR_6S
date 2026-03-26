import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class AbrirArchivo {

    private CompiladorGUI gui;

    public AbrirArchivo(CompiladorGUI gui) {
        this.gui = gui;
    }

    public void ejecutar() {

        // Crear el diálogo nativo de Windows para abrir archivos
        FileDialog dialogo = new FileDialog((Frame) null, "Selecciona un archivo", FileDialog.LOAD);

        // Mostrar la ventana
        dialogo.setVisible(true);

        // Obtener archivo seleccionado
        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (file != null) {
            File archivo = new File(dir, file);

            // Mostrar la ruta en tu GUI
            gui.getLabelRutaArchivo().setText(archivo.getAbsolutePath());

            // Leer el contenido del archivo en tu editor
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                gui.getEditorCodigo().read(br, null);
            } catch (Exception ex) {
                System.err.println("Error al leer el archivo: " + ex.getMessage());
            }
        }
    }
}