package com.compilador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase encargada de la lectura y almacenamiento de la matriz de transiciones desde un archivo CSV.
 * También carga la matriz del parser y las producciones gramaticales.
 */
public class LecturaMatriz {

    private Map<String, Map<String, String>> matrizTransiciones;
    private Map<String, Map<String, Integer>> matrizParser;
    private Map<Integer, List<String>> producciones;

    /**
     * Inicializa la estructura de datos principal para almacenar la matriz de transiciones.
     */
    public LecturaMatriz() {
        matrizTransiciones = new LinkedHashMap<>();
        matrizParser = new LinkedHashMap<>();
        producciones = new LinkedHashMap<>();
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

    // ==================== MÉTODOS PARA MATRIZ DEL PARSER ====================

    /**
     * Lee la matriz del parser (C3A4_ALEXMEZA.csv) y la almacena en memoria.
     * @param rutaArchivo Ruta del archivo CSV de la matriz del parser
     * @return true si se cargó correctamente
     */
    public boolean cargarMatrizParser(String rutaArchivo) {
        matrizParser.clear();

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
                String noTerminal = valores[0].trim();

                Map<String, Integer> reglas = new LinkedHashMap<>();

                for (int i = 1; i < encabezados.length; i++) {
                    String token = encabezados[i].trim();
                    String valorCelda = (i < valores.length) ? valores[i].trim() : "";

                    if (!valorCelda.isEmpty()) {
                        try {
                            reglas.put(token, Integer.parseInt(valorCelda));
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                matrizParser.put(noTerminal, reglas);
            }

            System.out.println("Matriz del Parser cargada correctamente. No Terminales: " + matrizParser.size());
            return true;

        } catch (IOException e) {
            System.err.println("Error al leer la matriz del parser: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna la matriz del parser.
     */
    public Map<String, Map<String, Integer>> getMatrizParser() {
        return matrizParser;
    }

    /**
     * Imprime la matriz del parser en consola.
     */
    public void imprimirMatrizParser() {
        System.out.println("\n--- MATRIZ DEL PARSER (LL1) ---");
        for (Map.Entry<String, Map<String, Integer>> fila : matrizParser.entrySet()) {
            System.out.println("No Terminal: " + fila.getKey());
            for (Map.Entry<String, Integer> regla : fila.getValue().entrySet()) {
                System.out.println("  " + regla.getKey() + " -> Prod: " + regla.getValue());
            }
        }
        System.out.println("-----------------------------------------\n");
    }

    // ==================== MÉTODOS PARA PRODUCCIONES GRAMATICALES ====================

    /**
     * Lee las producciones gramaticales (producciones_gramatica.csv).
     * @param rutaArchivo Ruta del archivo CSV de producciones
     * @return true si se cargó correctamente
     */
    public boolean cargarProducciones(String rutaArchivo) {
        producciones.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                if (linea.trim().isEmpty()) continue;

                String[] valores = linea.split(",", 3);
                if (valores.length < 3) continue;

                try {
                    int codigo = Integer.parseInt(valores[0].trim());
                    String noTerminal = valores[1].trim();
                    String rhs = valores[2].trim();

                    List<String> simbolos = new ArrayList<>();
                    if (!rhs.equals("EPSILON")) {
                        String[] partes = rhs.split(" ");
                        List<String> partesLimpias = new ArrayList<>();
                        for (String parte : partes) {
                            if (!parte.trim().isEmpty()) {
                                partesLimpias.add(parte.trim());
                            }
                        }
                        for (int i = 0; i < partesLimpias.size(); i++) {
                            String actual = partesLimpias.get(i);
                            if (i + 1 < partesLimpias.size()) {
                                String siguiente = partesLimpias.get(i + 1);
                                if ((actual.equals("Const") && (siguiente.equals("Decimal") || siguiente.equals("real") || siguiente.equals("cadena") || siguiente.equals("Exponencial"))) ||
                                    (actual.equals("Const") && siguiente.equals("Octal")) ||
                                    (actual.equals("Const") && siguiente.equals("Hexadecimal")) ||
                                    (actual.equals("CONST") && siguiente.equals("NUMÉRICA"))) {
                                    String combinado = actual + "_" + siguiente;
                                    if (actual.equals("CONST")) combinado = "CONST_NUMERICA";
                                    simbolos.add(combinado);
                                    i++;
                                    continue;
                                }
                                // COMENTADO: Ya no combinamos (OR) - ahora separado por espacios
                                // if (actual.equals("(") && (siguiente.equals("OR") || siguiente.equals("id") || siguiente.equals("("))) {
                                //     int j = i + 1;
                                //     StringBuilder combinado = new StringBuilder("(");
                                //     while (j < partesLimpias.size() && !partesLimpias.get(j).equals(")")) {
                                //         if (combinado.length() > 1) combinado.append("_");
                                //         combinado.append(partesLimpias.get(j));
                                //         j++;
                                //     }
                                //     if (j < partesLimpias.size() && partesLimpias.get(j).equals(")")) {
                                //         combinado.append(")");
                                //         simbolos.add(combinado.toString());
                                //         i = j;
                                //         continue;
                                //     }
                                // }
                            }
                            String simboloProcesado = actual.replace("Á", "A").replace("É", "E").replace("Í", "I")
                                           .replace("Ó", "O").replace("Ú", "U").replace("á", "a")
                                           .replace("é", "e").replace("í", "i").replace("ó", "o")
                                           .replace("ú", "u").replace("TÉRMINO", "TERMINO")
                                           .replace("CONST_NUMÉRICA", "CONST_NUMERICA");
                            
                            if (simboloProcesado.length() > 1 && (simboloProcesado.endsWith("]") || simboloProcesado.endsWith(")"))) {
                                String base = simboloProcesado.substring(0, simboloProcesado.length() - 1);
                                String cierre = simboloProcesado.substring(simboloProcesado.length() - 1);
                                if (!base.isEmpty()) {
                                    simbolos.add(base);
                                }
                                simbolos.add(cierre);
                            } else {
                                simbolos.add(simboloProcesado);
                            }
                        }
                    }

                    producciones.put(codigo, simbolos);

                } catch (NumberFormatException e) {
                    System.err.println("Error: código de producción no válido: " + valores[0]);
                }
            }

            System.out.println("Producciones cargadas correctamente. Total: " + producciones.size());
            return true;

        } catch (IOException e) {
            System.err.println("Error al leer las producciones: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna las producciones gramaticales.
     */
    public Map<Integer, List<String>> getProducciones() {
        return producciones;
    }

    /**
     * Obtiene una producción específica por su código.
     */
    public List<String> getProduccion(int codigo) {
        return producciones.get(codigo);
    }

    /**
     * Imprime todas las producciones en consola.
     */
    public void imprimirProducciones() {
        System.out.println("\n--- PRODUCCIONES GRAMATICALES ---");
        for (Map.Entry<Integer, List<String>> entry : producciones.entrySet()) {
            System.out.print("Código " + entry.getKey() + ": ");
            if (entry.getValue().isEmpty()) {
                System.out.println("EPSILON");
            } else {
                for (String simbolo : entry.getValue()) {
                    System.out.print(simbolo + " ");
                }
                System.out.println();
            }
        }
        System.out.println("-----------------------------------------\n");
    }
}