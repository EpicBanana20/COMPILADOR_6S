package com.compilador;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        dialogo.setFile("Sem2_MezaAlex_1.xlsx"); 
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
            TablaSimbolos tablaSimbolos = parser != null ? parser.getTablaSimbolos() : new TablaSimbolos();
            Map<Integer, Integer> erroresPorAmbito = parser != null ? parser.getErroresPorAmbito() : new java.util.LinkedHashMap<>();
            List<int[]> eventosAmbito = parser != null ? parser.getEventosAmbito() : new ArrayList<>();
            List<Object[]> resumenTemporales = parser != null ? parser.getResumenTemporales() : new ArrayList<>();
            List<Object[]> registroSemantica2 = parser != null ? parser.getRegistroSemantica2() : new ArrayList<>();
            generarExcel(rutaAbsoluta, gui.getModeloTokens(), gui.getModeloErrores(), gui.getModeloPila(), contadoresSintaxis, totalErroresSintacticos, tablaSimbolos, erroresPorAmbito, eventosAmbito, resumenTemporales, registroSemantica2);
        }
    }

    private void generarExcel(String rutaAbsoluta, DefaultTableModel modeloTokens, DefaultTableModel modeloErrores, DefaultTableModel modeloContadores, Map<String, Integer> contadoresSintaxis, int totalErroresSintacticos, TablaSimbolos tablaSimbolos, Map<Integer, Integer> erroresPorAmbito, List<int[]> eventosAmbito, List<Object[]> resumenTemporales, List<Object[]> registroSemantica2) {
        try (Workbook workbook = new XSSFWorkbook()) {

            // =========================================================
            // CREAR EL ESTILO: Centrado y con "Ajustar Texto" activado
            // =========================================================
            CellStyle estiloCentrado = workbook.createCellStyle();
            estiloCentrado.setAlignment(HorizontalAlignment.CENTER);
            estiloCentrado.setVerticalAlignment(VerticalAlignment.CENTER);
            estiloCentrado.setWrapText(true); // Esto es el "Ajustar texto"

            // Estilo destacado (negrita + fondo) para las columnas id/tipo/Clase de "Tabla de Simbolos"
            org.apache.poi.ss.usermodel.Font fontDestacada = workbook.createFont();
            fontDestacada.setBold(true);
            CellStyle estiloDestacado = workbook.createCellStyle();
            estiloDestacado.setAlignment(HorizontalAlignment.CENTER);
            estiloDestacado.setVerticalAlignment(VerticalAlignment.CENTER);
            estiloDestacado.setWrapText(true);
            estiloDestacado.setFont(fontDestacada);
            estiloDestacado.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
            estiloDestacado.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

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

            // --- 5. Hoja de Ámbito ---
            Sheet sheetAmbito = workbook.createSheet("Ámbito");
            escribirTablaAmbito(sheetAmbito, tablaSimbolos, erroresPorAmbito, eventosAmbito, estiloCentrado);

            // --- 6. Hoja de Tabla de Simbolos ---
            Sheet sheetTablaSimbolos = workbook.createSheet("Tabla de Simbolos");
            escribirTablaSimbolos(sheetTablaSimbolos, tablaSimbolos, estiloCentrado, estiloDestacado);

            // --- 7. Hoja de Semantica 1 (temporales por asignación) ---
            org.apache.poi.ss.usermodel.Font fontEncabezado = workbook.createFont();
            fontEncabezado.setBold(true);
            CellStyle estiloEncabezado = workbook.createCellStyle();
            estiloEncabezado.cloneStyleFrom(estiloCentrado);
            estiloEncabezado.setFont(fontEncabezado);
            estiloEncabezado.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            estiloEncabezado.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            estiloEncabezado.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            estiloEncabezado.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            Sheet sheetTemporales = workbook.createSheet("Semantica 1");
            escribirTablaTemporales(sheetTemporales, resumenTemporales, estiloEncabezado, estiloCentrado);

            // --- 8. Hojas de Semantica 2 (registro de reglas y totales por regla) ---
            Sheet sheetSem2Base = workbook.createSheet("Semantica 2-Base");
            escribirSemantica2Base(sheetSem2Base, registroSemantica2, estiloEncabezado, estiloCentrado);
            Sheet sheetSem2Total = workbook.createSheet("Semantica 2-Total");
            escribirSemantica2Total(sheetSem2Total, registroSemantica2, estiloEncabezado, estiloCentrado);

            // --- 9. Hoja de Semantica 3 (entradas y salidas de las funciones predefinidas) ---
            Sheet sheetSem3 = workbook.createSheet("Semantica 3");
            escribirSemantica3(sheetSem3, registroSemantica2, estiloEncabezado, estiloCentrado);

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
        sheet.setColumnWidth(1, 90 * 256); // Columna "Descripción" más ancha para poder leerla sin ajustar manualmente

        String[] headers = {"Estado", "Descripción", "Lexema", "Tipo", "Línea"};
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estilo);
        }

        // Los errores se listan por línea (los de ámbito, Semántica 1 y Semántica 2 se agregan en momentos distintos).
        // El orden es estable: en la misma línea se conserva el orden en que se detectaron.
        List<Integer> orden = new ArrayList<>();
        for (int r = 0; r < modelo.getRowCount(); r++) orden.add(r);
        orden.sort((x, y) -> Integer.compare(lineaDeError(modelo, x), lineaDeError(modelo, y)));

        for (int fila = 0; fila < orden.size(); fila++) {
            int r = orden.get(fila);
            Row row = sheet.createRow(fila + 1);
            for (int c = 0; c < 5; c++) {
                Object val = modelo.getValueAt(r, c);
                Cell cell = row.createCell(c);
                cell.setCellValue(val != null ? val.toString() : "");
                cell.setCellStyle(estilo);
            }
        }
    }

    private int lineaDeError(DefaultTableModel modelo, int fila) {
        try {
            return Integer.parseInt(String.valueOf(modelo.getValueAt(fila, 4)).trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
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
            "Comentarios",
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
            "Numérica hexadecimal", "Real", "Exponencial", "Booleanas", "Registro",
            "", "",
            "cadena", "Numérica Binario", "Numérica Decimal", "Numérica Octal",
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
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 10, 10)); // Comentarios (K1:K2)
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 11, 11)); // Palabras reservada (L1:L2)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 12, 20)); // Constantes (M1:U1)

        // Operadores (de la V a la AG se fusionan hacia abajo V1:V2, W1:W2, etc.)
        for (int col = 21; col <= 32; col++) {
            sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
        }

        // 5. PREPARAMOS TODA LA FILA 2 CON CEROS POR DEFECTO
        for (int i = 0; i <= 32; i++) {
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
        mapaColumnas.put("Registro Identificador", 9);

        mapaColumnas.put("Comentarios", 10);

        mapaColumnas.put("Palabras Reservadas", 11);

        mapaColumnas.put("Cadena", 12);
        mapaColumnas.put("Numerica Binario", 13);
        mapaColumnas.put("Numerica Decimal", 14);
        mapaColumnas.put("Numerica Octal", 15);
        mapaColumnas.put("Numerica Hexadecimal", 16);
        mapaColumnas.put("Numerica Real", 17);
        mapaColumnas.put("Numerica Exponencial", 18);
        mapaColumnas.put("Constantes Booleanas", 19);
        mapaColumnas.put("Constante nula", 20);

        mapaColumnas.put("Operadores postfix", 21);
        mapaColumnas.put("Operadores logicos binarios", 22);
        mapaColumnas.put("Operador de control", 23);
        mapaColumnas.put("Operadores matematicos", 24);
        mapaColumnas.put("Operador exponente", 25);
        mapaColumnas.put("Operadores de turno", 26);
        mapaColumnas.put("Operadores relacionales", 27);
        mapaColumnas.put("Operadores sin igualdad de conversion de tipo", 28);
        mapaColumnas.put("Operadores logicos", 29);
        mapaColumnas.put("Operador ternario", 30);
        mapaColumnas.put("Operadores de asignacion", 31);
        mapaColumnas.put("Operador de agrupamiento", 32);

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

    // Método para estructurar la hoja "Tabla de Simbolos"
    private void escribirTablaSimbolos(Sheet sheet, TablaSimbolos tablaSimbolos, CellStyle estilo, CellStyle estiloDestacado) {
        String[] headers = {"id", "tipo", "Clase", "amb", "Tarr", "DimArr", "NoPar", "TParr"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estiloDestacado);
        }

        int filaExcel = 1;
        for (Simbolo s : tablaSimbolos.getFilas()) {
            Row row = sheet.createRow(filaExcel++);
            String[] valores = {
                s.getId(), s.getTipo(), s.getClase(), String.valueOf(s.getAmb()),
                s.getTarr(), s.getDimArr(), s.getNoPar(), s.getTParr()
            };
            for (int c = 0; c < valores.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(valores[c]);
                cell.setCellStyle(estilo);
            }
        }
    }

    // Método para estructurar la hoja "Ámbito": agrega la Tabla de Simbolos por (ambito, tipo)
    // y suma los errores semánticos (Tarea 2: variable no declarada/duplicada, código 45/46) por
    // ámbito. Cada tipo de error va en su propia hoja: léxico->CONTADORES, sintáctico->Sintaxis,
    // semántico/ámbito->Ámbito (además todos aparecen juntos en la hoja "Errores" general).
    private void escribirTablaAmbito(Sheet sheet, TablaSimbolos tablaSimbolos, Map<Integer, Integer> erroresAmbitoPorAmbito,
                                       List<int[]> eventosAmbito, CellStyle estilo) {
        String[] headers = {"Ambito", "Bin", "Dec", "Oct", "Hex", "Real", "exp", "Cadena", "Boolean", "Errores", "total"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estilo);
        }

        Map<String, Integer> tipoAColumna = new LinkedHashMap<>();
        tipoAColumna.put("bin", 0);
        tipoAColumna.put("dec", 1);
        tipoAColumna.put("oct", 2);
        tipoAColumna.put("hex", 3);
        tipoAColumna.put("real", 4);
        tipoAColumna.put("exp", 5);
        tipoAColumna.put("cadena", 6);
        tipoAColumna.put("bool", 7);

        List<Integer> ordenAmbitos = new ArrayList<>();
        for (int[] evento : eventosAmbito) {
            if (evento[2] == 1) {
                ordenAmbitos.add(evento[0]);
            }
        }

        Map<Integer, int[]> conteosPorAmbito = new LinkedHashMap<>();
        for (Integer id : ordenAmbitos) {
            conteosPorAmbito.put(id, new int[8]);
        }
        for (Simbolo s : tablaSimbolos.getFilas()) {
            int[] fila = conteosPorAmbito.get(s.getAmb());
            if (fila == null) {
                fila = new int[8];
                conteosPorAmbito.put(s.getAmb(), fila);
            }
            Integer col = tipoAColumna.get(s.getTipo());
            if (col != null) {
                fila[col]++;
            }
        }

        int[] totales = new int[10];
        int filaExcel = 1;
        for (Integer id : ordenAmbitos) {
            Row row = sheet.createRow(filaExcel++);

            Cell cAmbito = row.createCell(0);
            cAmbito.setCellValue(String.valueOf(id));
            cAmbito.setCellStyle(estilo);

            int[] conteo = conteosPorAmbito.getOrDefault(id, new int[8]);
            int suma = 0;
            for (int i = 0; i < 8; i++) {
                Cell c = row.createCell(i + 1);
                c.setCellValue(String.valueOf(conteo[i]));
                c.setCellStyle(estilo);
                suma += conteo[i];
                totales[i] += conteo[i];
            }

            int errores = erroresAmbitoPorAmbito.getOrDefault(id, 0);
            Cell cErrores = row.createCell(9);
            cErrores.setCellValue(String.valueOf(errores));
            cErrores.setCellStyle(estilo);
            totales[8] += errores;

            // El total cuenta solo variables por tipo; los errores van en su propia columna
            int total = suma;
            Cell cTotal = row.createCell(10);
            cTotal.setCellValue(String.valueOf(total));
            cTotal.setCellStyle(estilo);
            totales[9] += total;
        }

        Row totalRow = sheet.createRow(filaExcel);
        Cell cTotalLabel = totalRow.createCell(0);
        cTotalLabel.setCellValue("Total");
        cTotalLabel.setCellStyle(estilo);
        for (int i = 0; i < 10; i++) {
            Cell c = totalRow.createCell(i + 1);
            c.setCellValue(String.valueOf(totales[i]));
            c.setCellStyle(estilo);
        }
    }

    // Reglas de Semántica 2, en el orden de la hoja "Semantica 2-Total" de la evidencia
    private static final int[] REGLAS_SEMANTICA2 = {
        1010, 1011, 1012, 1013, 1020, 1021, 1022, 1030, 1040, 1050, 1060, 1061, 1070,
        1080, 1081, 1082, 1083, 1084, 1090, 1100, 1110, 1120, 1130, 1131, 1140, 1150, 1160
    };

    // Hoja "Semantica 2-Base": una fila por regla evaluada. Registro: {linea, regla, descripcion, ACEPT/ERROR, lexema, ambito, tope de pila}
    private void escribirSemantica2Base(Sheet sheet, List<Object[]> registro, CellStyle estiloEncabezado, CellStyle estilo) {
        String[] headers = {"Regla", "Función", "Tope Pila", "Valor Real", "Linea", "Edo", "Ambito"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estiloEncabezado);
        }
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 40 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 8 * 256);
        sheet.setColumnWidth(5, 9 * 256);
        sheet.setColumnWidth(6, 9 * 256);

        int filaExcel = 1;
        for (Object[] fila : registro) {
            Row row = sheet.createRow(filaExcel++);
            Cell cRegla = row.createCell(0);
            cRegla.setCellValue((Integer) fila[1]);
            cRegla.setCellStyle(estilo);
            Cell cFuncion = row.createCell(1);
            cFuncion.setCellValue(fila.length > 7 ? String.valueOf(fila[7]) : "-"); // solo las reglas de Semántica 3 traen función
            cFuncion.setCellStyle(estilo);
            Cell cTope = row.createCell(2);
            cTope.setCellValue(String.valueOf(fila[6]));
            cTope.setCellStyle(estilo);
            Cell cValor = row.createCell(3);
            cValor.setCellValue(String.valueOf(fila[4]));
            cValor.setCellStyle(estilo);
            Cell cLinea = row.createCell(4);
            cLinea.setCellValue((Integer) fila[0]);
            cLinea.setCellStyle(estilo);
            Cell cEdo = row.createCell(5);
            cEdo.setCellValue("ACEPT".equals(fila[3]) ? "Acept" : "ERROR");
            cEdo.setCellStyle(estilo);
            Cell cAmbito = row.createCell(6);
            cAmbito.setCellValue((Integer) fila[5]);
            cAmbito.setCellStyle(estilo);
        }
    }

    // Hoja "Semantica 3": por función (o parámetro), cuántas veces apareció como entrada o salida, cuántas se aceptaron y cuántas fueron error
    private void escribirSemantica3(Sheet sheet, List<Object[]> registro, CellStyle estiloEncabezado, CellStyle estilo) {
        String[] headers = {"Función", "Entradas", "Salida", "Aceptados", "Errores"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estiloEncabezado);
        }
        sheet.setColumnWidth(0, 14 * 256);

        Map<String, LecturaMatriz.ReglaSemantica3> reglas = parser != null ? parser.getReglasSemantica3() : new LinkedHashMap<>();
        java.util.TreeMap<String, LecturaMatriz.ReglaSemantica3> ordenadas = new java.util.TreeMap<>((x, y) -> {
            if (x.equals("CLEAR") != y.equals("CLEAR")) return x.equals("CLEAR") ? -1 : 1; // CLEAR primero, como en la evidencia
            return String.CASE_INSENSITIVE_ORDER.compare(x, y);
        });
        for (LecturaMatriz.ReglaSemantica3 r : reglas.values()) {
            ordenadas.put(r.nombre, r);
        }

        int filaExcel = 1;
        for (Map.Entry<String, LecturaMatriz.ReglaSemantica3> entrada : ordenadas.entrySet()) {
            int aceptados = 0;
            int errores = 0;
            for (Object[] fila : registro) {
                if (fila.length < 8 || !entrada.getKey().equals(fila[7])) continue;
                if ("ACEPT".equals(fila[3])) aceptados++; else errores++;
            }
            Row row = sheet.createRow(filaExcel++);
            Cell cNombre = row.createCell(0);
            cNombre.setCellValue(entrada.getKey());
            cNombre.setCellStyle(estilo);
            Cell cEntradas = row.createCell(1);
            Cell cSalida = row.createCell(2);
            if (entrada.getValue().esSalida()) {
                cSalida.setCellValue(aceptados + errores);
            } else {
                cEntradas.setCellValue(aceptados + errores);
            }
            cEntradas.setCellStyle(estilo);
            cSalida.setCellStyle(estilo);
            Cell cAceptados = row.createCell(3);
            cAceptados.setCellValue(aceptados);
            cAceptados.setCellStyle(estilo);
            Cell cErrores = row.createCell(4);
            cErrores.setCellValue(errores);
            cErrores.setCellStyle(estilo);
        }
    }

    // Hoja "Semantica 2-Total": por regla, cuántas veces apareció, cuántas se aceptaron y cuántas fueron error
    private void escribirSemantica2Total(Sheet sheet, List<Object[]> registro, CellStyle estiloEncabezado, CellStyle estilo) {
        String[] headers = {"Reglas", "Aparecen", "Aceptada", "Errores"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estiloEncabezado);
        }

        Map<Integer, int[]> conteos = new HashMap<>();
        for (int regla : REGLAS_SEMANTICA2) conteos.put(regla, new int[2]);
        for (Object[] fila : registro) {
            int[] c = conteos.get((Integer) fila[1]);
            if (c == null) continue;
            if ("ACEPT".equals(fila[3])) c[0]++; else c[1]++;
        }

        int filaExcel = 1;
        for (int regla : REGLAS_SEMANTICA2) {
            int[] c = conteos.get(regla);
            int[] valores = {c[0] + c[1], c[0], c[1]};
            Row row = sheet.createRow(filaExcel++);
            Cell cRegla = row.createCell(0);
            cRegla.setCellValue(regla);
            cRegla.setCellStyle(estilo);
            for (int i = 0; i < valores.length; i++) {
                Cell cell = row.createCell(i + 1);
                cell.setCellValue(valores[i]);
                cell.setCellStyle(estilo);
            }
        }
    }

    // Hoja "Temporales": una fila por asignación con el conteo de temporales por tipo, la asignación y sus errores
    private void escribirTablaTemporales(Sheet sheet, List<Object[]> resumen, CellStyle estiloEncabezado, CellStyle estilo) {
        String[] headers = {"Linea", "TBin", "TDec", "TOct", "THex", "TReal", "Texp", "TCadena", "TBoolean", "TVariant", "Asignaciones", "Errores"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estiloEncabezado);
        }
        sheet.setColumnWidth(10, 30 * 256);

        int[] totales = new int[9];
        int totalErrores = 0;
        int filaExcel = 1;
        for (Object[] fila : resumen) {
            Row row = sheet.createRow(filaExcel++);
            Cell cLinea = row.createCell(0);
            cLinea.setCellValue((Integer) fila[0]);
            cLinea.setCellStyle(estilo);
            int[] conteos = (int[]) fila[1];
            for (int i = 0; i < conteos.length; i++) {
                Cell c = row.createCell(i + 1);
                c.setCellValue(conteos[i]);
                c.setCellStyle(estilo);
                totales[i] += conteos[i];
            }
            Cell cAsig = row.createCell(10);
            cAsig.setCellValue((String) fila[2]);
            cAsig.setCellStyle(estilo);
            Cell cErr = row.createCell(11);
            cErr.setCellValue((Integer) fila[3]);
            cErr.setCellStyle(estilo);
            totalErrores += (Integer) fila[3];
        }

        Row totalRow = sheet.createRow(filaExcel);
        Cell cLabel = totalRow.createCell(0);
        cLabel.setCellValue("Totales");
        cLabel.setCellStyle(estilo);
        for (int i = 0; i < totales.length; i++) {
            Cell c = totalRow.createCell(i + 1);
            c.setCellValue(totales[i]);
            c.setCellStyle(estilo);
        }
        Cell cAsigTotal = totalRow.createCell(10);
        cAsigTotal.setCellValue(resumen.size());
        cAsigTotal.setCellStyle(estilo);
        Cell cErrTotal = totalRow.createCell(11);
        cErrTotal.setCellValue(totalErrores);
        cErrTotal.setCellStyle(estilo);
    }
}
