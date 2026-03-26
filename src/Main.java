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
            // 1. Inicializamos la GUI
            CompiladorGUI ventana = new CompiladorGUI();
            
            // 2. Cargamos la Matriz
            LecturaMatriz lectorMatriz = new LecturaMatriz();
            lectorMatriz.cargarMatriz("C2A6.csv");
            
            // 3. Conectamos "Abrir Archivo"
            AbrirArchivo logicaAbrir = new AbrirArchivo(ventana);
            ventana.getBtnAbrir().addActionListener(e -> logicaAbrir.ejecutar());

            // 4. ¡LA NUEVA CONEXIÓN DE COMPILACIÓN!
            Compilacion logicaCompilar = new Compilacion(ventana, lectorMatriz);
            ventana.getBtnCompilar().addActionListener(e -> logicaCompilar.ejecutar());

            // 5. Mostramos la ventana
            ventana.setVisible(true);
        });
    }
}