package Compartido.exportar;

import conexion.Conexion;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class exportarPlantilla {

    // =============================
    //  MÉTODO PÚBLICO PRINCIPAL
    // =============================
    public static void exportarPlantilla(String nombreApartado) {
        try {
            ArrayList<String> columnas = obtenerColumnas(nombreApartado);

            if (columnas.isEmpty()) {
                mostrarError("No se encontraron columnas en la tabla: " + nombreApartado);
                return;
            }

            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Seleccionar carpeta para guardar la plantilla");
            File carpeta = dirChooser.showDialog(null);

            if (carpeta == null) {
                mostrarError("No se seleccionó ninguna carpeta.");
                return;
            }

            File archivo = new File(carpeta, "plantilla-" + nombreApartado + ".xlsx");

            generarExcel(nombreApartado, columnas, archivo);

            mostrarExito("Plantilla generada correctamente:\n" + archivo.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al generar la plantilla: " + e.getMessage());
        }
    }

    // =============================
    //   OBTENER COLUMNAS DE LA TABLA
    // =============================
    private static ArrayList<String> obtenerColumnas(String nombreTabla) {
        ArrayList<String> columnas = new ArrayList<>();

        String sql = "DESCRIBE " + nombreTabla;

        try (Connection con = new Conexion().conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                columnas.add(rs.getString("Field"));
            }

        } catch (Exception e) {
            System.out.println("Error al obtener columnas de " + nombreTabla + ": " + e.getMessage());
        }

        return columnas;
    }

    // =============================
    //   GENERAR ARCHIVO EXCEL
    // =============================
    private static void generarExcel(String nombreTabla, ArrayList<String> columnas, File archivo) throws Exception {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(nombreTabla);

        // -------------------------
        // Estilos
        // -------------------------
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontHeader = workbook.createFont();
        fontHeader.setBold(true);
        headerStyle.setFont(fontHeader);

        // -------------------------
        // Fila 1 vacía (como pediste)
        // -------------------------
        sheet.createRow(0);

        // -------------------------
        // Fila 2: Encabezados
        // -------------------------
        Row headerRow = sheet.createRow(1);

        for (int i = 0; i < columnas.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Autosize
        for (int i = 0; i < columnas.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(archivo)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    // =============================
    //   ALERTAS
    // =============================
    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
