package Compartido.importar;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import conexion.Conexion;

public class importador {

    // Método genérico existente
    public static void importarExcel(String tabla, String columnaId) {// AVISO DE CONFIRMACIÓN ANTES DE IMPORTAR
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION);
        aviso.setTitle("Importación");
        aviso.setHeaderText("Aviso de sobrescritura");
        aviso.setContentText("Algunos registros existentes podrían modificarse al importar.\n¿Desea continuar?");
        if (aviso.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return; // Usuario canceló, no continuar
        }

        // Selección de archivo Excel
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(null);
        if (archivo == null) return;

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = new Conexion().conectar()) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // Encabezado en la segunda fila
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                mostrarError("No se encontró la fila de encabezado en la segunda fila.");
                return;
            }

            // Obtener nombres de columnas
            List<String> columnas = new ArrayList<>();
            for (Cell cell : headerRow) {
                String colName = getCellAsString(cell).trim();
                if (!colName.isEmpty()) columnas.add(colName);
            }

            // Índice de columna ID
            int idxColumnaId = -1;
            for (int i = 0; i < columnas.size(); i++) {
                if (columnas.get(i).equalsIgnoreCase(columnaId)) {
                    idxColumnaId = i;
                    break;
                }
            }
            if (idxColumnaId == -1) {
                mostrarError("No se encontró la columna '" + columnaId + "' en la segunda fila.");
                return;
            }

            // Construir SQL dinámico con ON DUPLICATE KEY UPDATE
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabla).append(" (");
            for (String col : columnas) sql.append("`").append(col).append("`,");
            sql.deleteCharAt(sql.length() - 1).append(") VALUES (");
            for (int i = 0; i < columnas.size(); i++) sql.append("?,");
            sql.deleteCharAt(sql.length() - 1).append(") ON DUPLICATE KEY UPDATE ");

            for (int i = 0; i < columnas.size(); i++) {
                if (i != idxColumnaId) sql.append("`").append(columnas.get(i)).append("`=VALUES(`").append(columnas.get(i)).append("`),");
            }
            sql.deleteCharAt(sql.length() - 1); // eliminar última coma

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            // Procesar filas desde la tercera
            for (int i = 2; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Object[] values = new Object[columnas.size()];
                for (int j = 0; j < columnas.size(); j++) {
                    Cell cell = row.getCell(j);
                    boolean isId = (j == idxColumnaId);
                    values[j] = getCellValue(cell, isId);

                    if (isId && values[j] == null) {
                        mostrarError("Fila " + (i + 1) + ": La columna '" + columnaId + "' no puede estar vacía.");
                        return;
                    }
                }

                // Asignar parámetros y ejecutar
                for (int j = 0; j < columnas.size(); j++) {
                    stmt.setObject(j + 1, values[j]);
                }
                stmt.executeUpdate();
            }

            mostrarExito("Importación finalizada correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al importar: " + e.getMessage());
        }
    }

    // NUEVO MÉTODO ESPECÍFICO PARA PRODUCTOS
    public static void importarProductosExcel() {
        // AVISO DE CONFIRMACIÓN ANTES DE IMPORTAR
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION);
        aviso.setTitle("Importación de Productos");
        aviso.setHeaderText("Aviso de sobrescritura");
        aviso.setContentText("Algunos registros existentes podrían modificarse al importar.\n¿Desea continuar?");
        if (aviso.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel de Productos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(null);
        if (archivo == null) return;

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = new Conexion().conectar()) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                mostrarError("No se encontró la fila de encabezado en la segunda fila.");
                return;
            }

            List<String> columnas = new ArrayList<>();
            for (Cell cell : headerRow) {
                String colName = getCellAsString(cell).trim();
                if (!colName.isEmpty()) columnas.add(colName);
            }

            StringBuilder sql = new StringBuilder("INSERT INTO productos (");
            for (String col : columnas) sql.append("`").append(col).append("`,");
            sql.deleteCharAt(sql.length() - 1).append(") VALUES (");
            for (int i = 0; i < columnas.size(); i++) sql.append("?,");
            sql.deleteCharAt(sql.length() - 1).append(") ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < columnas.size(); i++) {
                if (!columnas.get(i).equalsIgnoreCase("id")) {
                    sql.append("`").append(columnas.get(i)).append("`=VALUES(`")
                            .append(columnas.get(i)).append("`),");
                }
            }
            sql.deleteCharAt(sql.length() - 1);

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            // CATEGORÍAS PERMITIDAS (EXACTAS)
            Set<String> categoriasPermitidas = Set.of(
                    "cristaleria",
                    "consumibles",
                    "equipo",
                    "reactivos"
            );

            for (int i = 2; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Object[] values = new Object[columnas.size()];

                for (int j = 0; j < columnas.size(); j++) {
                    Cell cell = row.getCell(j);
                    String colName = columnas.get(j);
                    Object cellValue = getCellValue(cell, colName.equalsIgnoreCase("id"));

                    // ===== VALIDACIÓN CORRECTA DE CATEGORIA =====
                    if (colName.equalsIgnoreCase("categoria") && cellValue != null) {
                        String categoria = cellValue.toString().trim();
                        if (!categoriasPermitidas.contains(categoria)) {
                            mostrarError(
                                    "No se pudo realizar la importación.\n\n" +
                                            "Motivo: valor no permitido en la columna 'categoria'.\n" +
                                            "Fila: " + (i + 1) + "\n" +
                                            "Valor recibido: '" + categoria + "'\n" +
                                            "Valores permitidos: cristaleria, consumibles, equipo, reactivos"
                            );
                            return;
                        }
                    }
                    // ===== FIN VALIDACIÓN =====

                    if (colName.equalsIgnoreCase("marca") && cellValue != null) {
                        String nombreMarca = cellValue.toString().trim();
                        Integer idMarca = obtenerIdPorNombre(conn, "marcas", "id", "nombre", nombreMarca);

                        if (idMarca == null) {
                            PreparedStatement insertMarca = conn.prepareStatement(
                                    "INSERT INTO marcas (nombre) VALUES (?)",
                                    Statement.RETURN_GENERATED_KEYS
                            );
                            insertMarca.setString(1, nombreMarca);
                            insertMarca.executeUpdate();

                            ResultSet rs = insertMarca.getGeneratedKeys();
                            if (rs.next()) idMarca = rs.getInt(1);
                        }
                        cellValue = idMarca;
                    }

                    if (colName.equalsIgnoreCase("etiqueta") && cellValue != null) {
                        String nombreEtiqueta = cellValue.toString().trim();
                        Integer idEtiqueta = obtenerIdPorNombre(conn, "etiquetas", "id", "nombre", nombreEtiqueta);

                        if (idEtiqueta == null) {
                            PreparedStatement insertEtiqueta = conn.prepareStatement(
                                    "INSERT INTO etiquetas (nombre) VALUES (?)",
                                    Statement.RETURN_GENERATED_KEYS
                            );
                            insertEtiqueta.setString(1, nombreEtiqueta);
                            insertEtiqueta.executeUpdate();

                            ResultSet rs = insertEtiqueta.getGeneratedKeys();
                            if (rs.next()) idEtiqueta = rs.getInt(1);
                        }
                        cellValue = idEtiqueta;
                    }

                    values[j] = cellValue;
                }

                for (int j = 0; j < columnas.size(); j++) {
                    stmt.setObject(j + 1, values[j]);
                }
                stmt.executeUpdate();
            }

            mostrarExito("Importación de productos finalizada correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al importar productos: " + e.getMessage());
        }
    }




    // Obtener ID por nombre (para marcas y etiquetas)
    private static Integer obtenerIdPorNombre(Connection conn, String tabla, String columnaId, String columnaNombre, String nombre) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " + columnaId + " FROM " + tabla + " WHERE " + columnaNombre + " = ?")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(columnaId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // MÉTODOS EXISTENTES
    private static Object getCellValue(Cell cell, boolean forId) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String str = cell.getStringCellValue().trim();
                if (forId && str.matches("\\d+")) return Integer.parseInt(str);
                return str.isEmpty() ? null : str;
            case NUMERIC:
                if (forId) return (int) cell.getNumericCellValue();
                return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
            case BOOLEAN: return cell.getBooleanCellValue();
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        String fStr = cell.getStringCellValue().trim();
                        if (forId && fStr.matches("\\d+")) return Integer.parseInt(fStr);
                        return fStr.isEmpty() ? null : fStr;
                    case NUMERIC:
                        if (forId) return (int) cell.getNumericCellValue();
                        return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
                    case BOOLEAN: return cell.getBooleanCellValue();
                    default: return null;
                }
            default: return null;
        }
    }

    private static String getCellAsString(Cell cell) {
        Object value = getCellValue(cell, false);
        return value != null ? value.toString() : "";
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // MÉTODO ESPECÍFICO PARA IMPORTAR CLAVES CON VALIDACIÓN DE FK
    public static void importarClavesExcel() {

        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION);
        aviso.setTitle("Importación de Claves");
        aviso.setHeaderText("Aviso de sobrescritura");
        aviso.setContentText("Algunos registros existentes podrían modificarse al importar.\n¿Desea continuar?");
        if (aviso.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel de Claves");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(null);
        if (archivo == null) return;

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = new Conexion().conectar()) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                mostrarError("No se encontró la fila de encabezado en la segunda fila.");
                return;
            }

            List<String> columnas = new ArrayList<>();
            for (Cell cell : headerRow) {
                String colName = getCellAsString(cell).trim();
                if (!colName.isEmpty()) columnas.add(colName);
            }

            int idxIdAlterno = columnas.indexOf("idAlterno");
            int idxIdProveedor = columnas.indexOf("idProveedor");
            int idxIdProducto = columnas.indexOf("idProducto");

            if (idxIdAlterno == -1 || idxIdProveedor == -1 || idxIdProducto == -1) {
                mostrarError("El archivo debe contener las columnas: idAlterno, idProveedor, idProducto.");
                return;
            }

            StringBuilder sql = new StringBuilder("INSERT INTO claves (");
            for (String col : columnas) sql.append("`").append(col).append("`,");
            sql.deleteCharAt(sql.length() - 1).append(") VALUES (");
            for (int i = 0; i < columnas.size(); i++) sql.append("?,");
            sql.deleteCharAt(sql.length() - 1).append(") ON DUPLICATE KEY UPDATE ");

            for (int i = 0; i < columnas.size(); i++) {
                if (i != idxIdAlterno) {
                    sql.append("`").append(columnas.get(i)).append("`=VALUES(`")
                            .append(columnas.get(i)).append("`),");
                }
            }
            sql.deleteCharAt(sql.length() - 1);

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            PreparedStatement validarProveedor = conn.prepareStatement(
                    "SELECT 1 FROM proveedores WHERE id = ?"
            );
            PreparedStatement validarProducto = conn.prepareStatement(
                    "SELECT 1 FROM productos WHERE id = ?"
            );

            for (int i = 2; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Object[] values = new Object[columnas.size()];

                for (int j = 0; j < columnas.size(); j++) {
                    Cell cell = row.getCell(j);
                    values[j] = getCellValue(cell, j == idxIdAlterno);
                }

                // ===== VALIDACIÓN ID PROVEEDOR =====
                if (values[idxIdProveedor] != null) {
                    validarProveedor.setObject(1, values[idxIdProveedor]);
                    ResultSet rs = validarProveedor.executeQuery();
                    if (!rs.next()) {
                        mostrarError(
                                "No se pudo realizar la importación.\n\n" +
                                        "Motivo: idProveedor no existe.\n" +
                                        "Fila: " + (i + 1) + "\n" +
                                        "Columna: idProveedor\n" +
                                        "Valor recibido: " + values[idxIdProveedor]
                        );
                        return;
                    }
                }

                // ===== VALIDACIÓN ID PRODUCTO =====
                if (values[idxIdProducto] != null) {
                    validarProducto.setObject(1, values[idxIdProducto]);
                    ResultSet rs = validarProducto.executeQuery();
                    if (!rs.next()) {
                        mostrarError(
                                "No se pudo realizar la importación.\n\n" +
                                        "Motivo: idProducto no existe.\n" +
                                        "Fila: " + (i + 1) + "\n" +
                                        "Columna: idProducto\n" +
                                        "Valor recibido: " + values[idxIdProducto]
                        );
                        return;
                    }
                }

                for (int j = 0; j < columnas.size(); j++) {
                    stmt.setObject(j + 1, values[j]);
                }

                stmt.executeUpdate();
            }

            mostrarExito("Importación de claves finalizada correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al importar claves: " + e.getMessage());
        }
    }

}
