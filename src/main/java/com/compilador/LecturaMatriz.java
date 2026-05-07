package com.compilador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clase encargada de la lectura y almacenamiento de la matriz de transiciones desde un archivo CSV.
 */
public class LecturaMatriz {

    private Map<String, Map<String, String>> matrizTransiciones;

    /**
     * Inicializa la estructura de datos principal para almacenar la matriz de transiciones.
     */
    public LecturaMatriz() {
        matrizTransiciones = new LinkedHashMap<>();
    }

    /**
     * Lee un archivo CSV y llena la estructura de datos con los estados y sus transiciones.
     * * @param rutaArchivo La ruta donde se encuentra el archivo .csv a leer.
     * @return true si el archivo se cargó con éxito, false si ocurrió un error de lectura.
     */
    public boolean cargarMatriz(String rutaArchivo) {
        matrizTransiciones.clear(); 

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            String[] encabezados = null;

            if ((linea = br.readLine()) != null) {
                linea = linea.replace("\uFEFF", ""); 
                encabezados = linea.split(",", -1);
            }

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] valores = linea.split(",", -1);
                String estadoActual = valores[0].trim();

                Map<String, String> transiciones = new LinkedHashMap<>();

                for (int i = 1; i < encabezados.length; i++) {
                    String columna = encabezados[i].trim();
                    String estadoDestino = (i < valores.length) ? valores[i].trim() : "";
                    
                    transiciones.put(columna, estadoDestino);
                }

                matrizTransiciones.put(estadoActual, transiciones);
            }

            System.out.println("Matriz CSV cargada correctamente. Total de estados: " + matrizTransiciones.size());
            return true;

        } catch (IOException e) {
            System.err.println("Error al leer la matriz CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna la matriz de transiciones cargada en memoria.
     * * @return Un mapa que contiene los estados como llaves y sus transiciones como valores.
     */
    public Map<String, Map<String, String>> getMatriz() {
        return matrizTransiciones;
    }

    /**
     * Imprime la matriz en consola mostrando únicamente las transiciones que contienen datos.
     */
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