package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

// Importaciones de Apache POI
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CrearXLS {

    private CompiladorGUI gui;
    private Parser parser;

    public CrearXLS(CompiladorGUI gui, Parser parser) {
        this.gui = gui;
        this.parser = parser;
    }

    public void ejecutar() {
        FileDialog dialogo = new FileDialog((Frame) null, "Guardar reporte Léxico en Excel", FileDialog.SAVE);
        dialogo.setFile("Reporte_MezaAlex.xlsx"); 
        dialogo.setVisible(true);

        String dir = dialogo.getDirectory();
        String file = dialogo.getFile();

        if (dir != null && file != null) {
            if (!file.toLowerCase().endsWith(".xlsx")) {
                file += ".xlsx";
            }
            String rutaAbsoluta = new File(dir, file).getAbsolutePath();
            Map<String, Integer> contadoresSintaxis = parser != null ? parser.getContadoresDiagramasPrincipales() : new java.util.LinkedHashMap<>();
            int totalErroresSintacticos = parser != null ? parser.getTotalErroresSintacticos() : 0;
            generarExcel(rutaAbsoluta, gui.getModeloTokens(), gui.getModeloErrores(), gui.getModeloPila(), contadoresSintaxis, totalErroresSintacticos);
        }
    }

    private void generarExcel(String rutaAbsoluta, DefaultTableModel modeloTokens, DefaultTableModel modeloErrores, DefaultTableModel modeloContadores, Map<String, Integer> contadoresSintaxis, int totalErroresSintacticos) {
        try (Workbook workbook = new XSSFWorkbook()) {

            // =========================================================
            // CREAR EL ESTILO: Centrado y con "Ajustar Texto" activado
            // =========================================================
            CellStyle estiloCentrado = workbook.createCellStyle();
            estiloCentrado.setAlignment(HorizontalAlignment.CENTER);
            estiloCentrado.setVerticalAlignment(VerticalAlignment.CENTER);
            estiloCentrado.setWrapText(true); // Esto es el "Ajustar texto"

            // --- 1. Hoja de TOKENS ---
            Sheet sheetTokens = workbook.createSheet("TOKENS");
            escribirTablaTokens(sheetTokens, modeloTokens, estiloCentrado);

            // --- 2. Hoja de Errores ---
            Sheet sheetErrores = workbook.createSheet("Errores");
            escribirTablaErrores(sheetErrores, modeloErrores, estiloCentrado);

            // --- 3. Hoja de CONTADORES ---
            Sheet sheetContadores = workbook.createSheet("CONTADORES");
            escribirTablaContadores(sheetContadores, modeloContadores, estiloCentrado);

            // --- 4. Hoja de SINTASIS ---
            Sheet sheetSintaxis = workbook.createSheet("Sintaxis");
            escribirTablaSintaxis(sheetSintaxis, contadoresSintaxis, totalErroresSintacticos, estiloCentrado);

            // Guardamos el archivo físicamente 
            try (FileOutputStream fileOut = new FileOutputStream(rutaAbsoluta)) {
                workbook.write(fileOut);
            }
            
            JOptionPane.showMessageDialog(gui, 
                "¡El archivo Excel se generó correctamente!", 
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

    // Método para estructurar la hoja de TOKENS
    private void escribirTablaTokens(Sheet sheet, DefaultTableModel modelo, CellStyle estilo) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Estado", "Lexema", "Línea"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estilo);
        }

        for (int r = 0; r < modelo.getRowCount(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < 3; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(String.valueOf(modelo.getValueAt(r, c)));
                cell.setCellStyle(estilo);
            }
        }
    }

    // Método para estructurar la hoja de Errores
    private void escribirTablaErrores(Sheet sheet, DefaultTableModel modelo, CellStyle estilo) {
        String[] headers = {"Estado", "Descripción", "Lexema", "Tipo", "Línea"};
        Row headerRow = sheet.createRow(0);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estilo);
        }

        for (int r = 0; r < modelo.getRowCount(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < 5; c++) {
                Object val = modelo.getValueAt(r, c);
                Cell cell = row.createCell(c);
                cell.setCellValue(val != null ? val.toString() : "");
                cell.setCellStyle(estilo);
            }
        }
    }

    // =========================================================
    // HOJA DE CONTADORES (REPLICA EXACTA DE LA PLANTILLA)
    // =========================================================
    private void escribirTablaContadores(Sheet sheet, DefaultTableModel modelo, CellStyle estilo) {
        // 1. Creamos las 3 filas que necesitamos
        Row row0 = sheet.createRow(0); // Fila 1 en Excel (Encabezados Principales)
        Row row1 = sheet.createRow(1); // Fila 2 en Excel (Subcategorías)
        Row row2 = sheet.createRow(2); // Fila 3 en Excel (Los valores numéricos)

        // ¡AQUÍ ESTÁ TU PETICIÓN! Ajustamos la altura de la Fila 2 (row1 en código) a 45 puntos
        row1.setHeightInPoints(45);

        // 2. CREAMOS LOS ENCABEZADOS DE LA FILA 0
        String[] titulosPrincipales = {
            "Errores", "identificadores", "", "", "", "", "", "", "", "", 
            "palabras reservada", "Constantes", "", "", "", "", "", "", "", "",
            "operadores de postfix", "Operadores lógicos binarios", "Operadores de control", 
            "Operadores matemáticos", "Operador exponente", "Operadores de turno", 
            "Operadores relacionales", "Operadores sin igualdad de conversión de tipo", 
            "Operadores lógicos", "Operador ternario", "Operadores de Asignación", 
            "Operadores de agrupamiento"
        };
        
        for (int i = 0; i < titulosPrincipales.length; i++) {
            Cell cell = row0.createCell(i);
            cell.setCellValue(titulosPrincipales[i]);
            cell.setCellStyle(estilo); // Aplicamos centrado y ajuste de texto
        }

        // 3. CREAMOS LAS SUBCATEGORÍAS DE LA FILA 1
        String[] subCategorias = {
            "", "cadena", "Numérica Binario", "Numérica Decimal", "Numérica Octal", 
            "Numérica hexadecimal", "Real", "Exponencial", "Booleanas", "comentarios", 
            "", "cadena", "Numérica Binario", "Numérica Decimal", "Numérica Octal", 
            "Numérica hexadecimal", "Real", "Exponencial", "Booleanas", "nula",
            "", "", "", "", "", "", "", "", "", "", "", ""
        };

        for (int i = 0; i < subCategorias.length; i++) {
            Cell cell = row1.createCell(i);
            cell.setCellValue(subCategorias[i]);
            cell.setCellStyle(estilo); // Aplicamos centrado y ajuste de texto
        }

        // 4. COMBINAMOS LAS CELDAS (FUSIONAR)
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));   // Errores (A1:A2)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 9));   // Identificadores (B1:J1)
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 10, 10)); // Palabras reservada (K1:K2)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 11, 19)); // Constantes (L1:T1)
        
        // Operadores (de la U a la AF se fusionan hacia abajo U1:U2, V1:V2, etc.)
        for (int col = 20; col <= 31; col++) {
            sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
        }

        // 5. PREPARAMOS TODA LA FILA 2 CON CEROS POR DEFECTO
        for (int i = 0; i <= 31; i++) {
            Cell cell = row2.createCell(i);
            cell.setCellValue("0");
            cell.setCellStyle(estilo); // Aplicamos centrado para los números también
        }

        // 6. MAPEAMOS LOS NOMBRES DE TUS TOKENS A SU COLUMNA CORRESPONDIENTE
        Map<String, Integer> mapaColumnas = new HashMap<>();
        
        mapaColumnas.put("Errores Léxicos", 0);
        mapaColumnas.put("Errores Críticos", 0); 
        
        mapaColumnas.put("Cadena Identificador", 1);
        mapaColumnas.put("Numerica Binario Identificador", 2);
        mapaColumnas.put("Numerica Decimal Identificador", 3);
        mapaColumnas.put("Numerica Octal Identificador", 4);
        mapaColumnas.put("Numerica Hexadecimal Identificador", 5);
        mapaColumnas.put("Real Identificador", 6);
        mapaColumnas.put("Exponencial Identificador", 7);
        mapaColumnas.put("Booleana Identificador", 8);
        mapaColumnas.put("Comentarios", 9); 
        
        mapaColumnas.put("Palabras Reservadas", 10);
        
        mapaColumnas.put("Cadena", 11);
        mapaColumnas.put("Numerica Binario", 12);
        mapaColumnas.put("Numerica Decimal", 13);
        mapaColumnas.put("Numerica Octal", 14);
        mapaColumnas.put("Numerica Hexadecimal", 15);
        mapaColumnas.put("Numerica Real", 16);
        mapaColumnas.put("Numerica Exponencial", 17);
        mapaColumnas.put("Constantes Booleanas", 18);
        mapaColumnas.put("Constante nula", 19);
        
        mapaColumnas.put("Operadores postfix", 20);
        mapaColumnas.put("Operadores logicos binarios", 21);
        mapaColumnas.put("Operador de control", 22);
        mapaColumnas.put("Operadores matematicos", 23);
        mapaColumnas.put("Operador exponente", 24);
        mapaColumnas.put("Operadores de turno", 25);
        mapaColumnas.put("Operadores relacionales", 26);
        mapaColumnas.put("Operadores sin igualdad de conversion de tipo", 27);
        mapaColumnas.put("Operadores logicos", 28);
        mapaColumnas.put("Operador ternario", 29);
        mapaColumnas.put("Operadores de asignacion", 30);
        mapaColumnas.put("Operador de agrupamiento", 31);

        // 7. VACIAR LOS DATOS DE LA TABLA A SUS COLUMNAS CORRECTAS
        int totalErrores = 0;

        for (int r = 0; r < modelo.getRowCount(); r++) {
            Object clasificacionObj = modelo.getValueAt(r, 0);
            Object cantidadObj = modelo.getValueAt(r, 1); 
            
            if (clasificacionObj != null && cantidadObj != null) {
                String clasificacion = clasificacionObj.toString();
                int cantidad = Integer.parseInt(cantidadObj.toString());
                
                if (mapaColumnas.containsKey(clasificacion)) {
                    int colDestino = mapaColumnas.get(clasificacion);
                    
                    if (colDestino == 0) {
                        totalErrores += cantidad;
                        row2.getCell(0).setCellValue(String.valueOf(totalErrores));
                    } else {
                        row2.getCell(colDestino).setCellValue(String.valueOf(cantidad));
                    }
                }
            }
        }
    }

    private void escribirTablaSintaxis(Sheet sheet, Map<String, Integer> contadoresSintaxis, int totalErroresSintacticos, CellStyle estilo) {
        String[] diagramas = {
            "PROGRAMA", "LISTA_DE_PARAMETROS", "EXP_PAS", "CONSTANTE_S_SIGNO",
            "CONST_NUMERICA", "OR", "AND", "DECLARACION_CONSTANTES", "FACTOR",
            "ELEVACION", "TERMINO_PASCAL", "SIMPLE_EXP_PASCAL", "STATU",
            "FUNCION", "ASIG", "ARR"
        };

        Row row1 = sheet.createRow(0);
        Row row2 = sheet.createRow(1);

        Cell cellA1 = row1.createCell(0);
        cellA1.setCellValue("ERRORES");
        cellA1.setCellStyle(estilo);

        Cell cellA2 = row2.createCell(0);
        cellA2.setCellValue(String.valueOf(totalErroresSintacticos));
        cellA2.setCellStyle(estilo);

        for (int i = 0; i < diagramas.length; i++) {
            Cell headerCell = row1.createCell(i + 1);
            headerCell.setCellValue(diagramas[i]);
            headerCell.setCellStyle(estilo);

            int conteo = contadoresSintaxis.containsKey(diagramas[i]) ? contadoresSintaxis.get(diagramas[i]) : 0;
            Cell valueCell = row2.createCell(i + 1);
            valueCell.setCellValue(String.valueOf(conteo));
            valueCell.setCellStyle(estilo);
        }
    }
}