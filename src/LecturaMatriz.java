import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LecturaMatriz {

    // Aquí vivirá nuestra matriz en memoria: Map<EstadoActual, Map<Columna, EstadoDestino>>
    private Map<String, Map<String, String>> matrizTransiciones;

    public LecturaMatriz() {
        // Inicializamos la estructura
        matrizTransiciones = new LinkedHashMap<>();
    }

    /**
     * Lee el archivo CSV y llena la estructura de datos.
     * @param rutaArchivo La ruta donde se encuentra el archivo .csv
     * @return true si se cargó con éxito, false si hubo un error.
     */
    public boolean cargarMatriz(String rutaArchivo) {
        matrizTransiciones.clear(); // Limpiamos por si se vuelve a cargar

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            String[] encabezados = null;

            // 1. Leer la primera línea (los encabezados / alfabeto)
            if ((linea = br.readLine()) != null) {
                // Truco de vida: Excel a veces guarda un caracter invisible al inicio de los CSV 
                // llamado BOM (\uFEFF). Esta línea lo elimina para que no rompa tu estado '0'.
                linea = linea.replace("\uFEFF", ""); 
                encabezados = linea.split(",", -1);
            }

            // 2. Leer el resto de las filas (los estados y sus transiciones)
            while ((linea = br.readLine()) != null) {
                // Si hay una línea completamente en blanco, la saltamos
                if (linea.trim().isEmpty()) continue;

                String[] valores = linea.split(",", -1);
                String estadoActual = valores[0].trim(); // El estado actual es la columna 0

                Map<String, String> transiciones = new LinkedHashMap<>();

                // Llenamos las transiciones para este estado
                for (int i = 1; i < encabezados.length; i++) {
                    String columna = encabezados[i].trim();
                    // Si la fila es más corta que los encabezados, evitamos un error
                    String estadoDestino = (i < valores.length) ? valores[i].trim() : "";
                    
                    transiciones.put(columna, estadoDestino);
                }

                // Guardamos la fila completa en nuestra matriz principal
                matrizTransiciones.put(estadoActual, transiciones);
            }

            System.out.println("✅ Matriz CSV cargada correctamente. Total de estados: " + matrizTransiciones.size());
            return true;

        } catch (IOException e) {
            System.err.println("❌ Error al leer la matriz CSV: " + e.getMessage());
            return false;
        }
    }

    // --- GETTER ---
    // Este método es crucial. Permitirá que la clase "Compilacion" 
    // pueda pedirle esta matriz para empezar a analizar el texto.
    public Map<String, Map<String, String>> getMatriz() {
        return matrizTransiciones;
    }

    // --- MÉTODO DE UTILIDAD ---
    // Imprime la matriz en consola solo con las transiciones que tienen datos.
    public void imprimirMatrizEnConsola() {
        System.out.println("\n--- MAPA DE LA MATRIZ DE TRANSICIONES ---");
        for (Map.Entry<String, Map<String, String>> fila : matrizTransiciones.entrySet()) {
            System.out.print("Estado [" + fila.getKey() + "]: ");
            
            boolean tieneDatos = false;
            for (Map.Entry<String, String> transicion : fila.getValue().entrySet()) {
                if (!transicion.getValue().isEmpty()) {
                    System.out.print("(" + transicion.getKey() + " -> " + transicion.getValue() + ")  ");
                    tieneDatos = true;
                }
            }
            if (!tieneDatos) System.out.print("Sin transiciones");
            System.out.println();
        }
        System.out.println("-----------------------------------------\n");
    }
}