import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Lexico {

    // Usamos LinkedHashMap para mantener el orden exacto de las filas del CSV
    private static Map<String, Map<String, String>> matrizTransiciones = new LinkedHashMap<>();

    public static void main(String[] args) {
        // 1. Cargar la matriz
        cargarMatriz("C2A6.csv");

        // 2. Imprimir la matriz respetando tu orden de columnas
        imprimirMatriz();
    }

    public static void cargarMatriz(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            String[] encabezados = null;

            // Leemos los encabezados (+, -, *, etc.)
            if ((linea = br.readLine()) != null) {
                encabezados = linea.split(",", -1); 
            }

            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",", -1);
                
                String estadoActual = valores[0];
                
                // EL CAMBIO CLAVE: LinkedHashMap respeta el orden de izquierda a derecha
                Map<String, String> transicionesEstado = new LinkedHashMap<>();

                for (int i = 1; i < encabezados.length; i++) {
                    String valor = (i < valores.length) ? valores[i] : ""; 
                    transicionesEstado.put(encabezados[i], valor);
                }

                matrizTransiciones.put(estadoActual, transicionesEstado);
            }
            System.out.println("✅ Matriz cargada exitosamente.\n");

        } catch (IOException e) {
            System.out.println("❌ Error al leer el archivo CSV: " + e.getMessage());
        }
    }

    public static void imprimirMatriz() {
        System.out.println("--- CONTENIDO DE LA MATRIZ (ORDEN ORIGINAL) ---");
        
        for (Map.Entry<String, Map<String, String>> fila : matrizTransiciones.entrySet()) {
            String estado = fila.getKey();
            Map<String, String> transiciones = fila.getValue();
            
            System.out.print("Estado [" + estado + "]: ");
            
            boolean tieneTransiciones = false;
            
            // Como usamos LinkedHashMap, este ciclo recorrerá las columnas de izq a der
            for (Map.Entry<String, String> transicion : transiciones.entrySet()) {
                String columna = transicion.getKey();
                String destino = transicion.getValue();
                
                // Solo imprimimos si la celda NO está vacía
                if (destino != null && !destino.trim().isEmpty()) {
                    System.out.print("(" + columna + " -> " + destino + ")  ");
                    tieneTransiciones = true;
                }
            }
            
            if (!tieneTransiciones) {
                System.out.print("Sin transiciones válidas");
            }
            System.out.println(); 
        }
        System.out.println("-----------------------------------------------");
    }
}