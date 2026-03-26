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
            // 1. Creamos la interfaz visual
            CompiladorGUI ventana = new CompiladorGUI();
            
            // 2. Creamos la clase lógica y le pasamos la ventana
            AbrirArchivo logicaAbrir = new AbrirArchivo(ventana);
            
            // 3. ¡LA CONEXIÓN! Le decimos al botón de la GUI qué debe hacer al hacer clic
            ventana.getBtnAbrir().addActionListener(e -> logicaAbrir.ejecutar());

            // 4. Mostramos la ventana
            ventana.setVisible(true);
        });
    }
}