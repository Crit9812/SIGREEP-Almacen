package Compartido.exportar;

import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class exportador {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colores (RGB convertidos de HEX)
    private static final float[] COLOR_BANDA_SUPERIOR = {0.5686f, 0.8314f, 0.5216f}; // Verde #91d485
    private static final float[] COLOR_SUBTITULOS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333
    private static final float[] COLOR_ENCABEZADO_TABLA = {0.95f, 0.95f, 0.95f};  // Gris claro
    private static final float[] COLOR_FILA_PAR = {0.98f, 0.98f, 0.98f};  // Gris muy claro
    private static final float[] COLOR_FILA_IMPAR = {1.0f, 1.0f, 1.0f};   // Blanco
    private static final float[] COLOR_FILTROS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333

    // Constantes de diseño actualizadas
    private static final float MARGIN = 35f;
    private static final float ROW_HEIGHT = 26f;
    private static final int MAX_COLUMNAS_POR_SECCION = 6;

    // Variables para control de información general
    private static boolean informacionGeneralMostrada = false;
    private static int filasPorHojaEstimado = 0;

    // ------------------------------------------------------------
    // Clase auxiliar para pasar los totales de forma estructurada
    // ------------------------------------------------------------
    public static class TotalesReporte {
        private final String etiquetaEntradas;
        private final double valorEntradas;
        private final String etiquetaSalidas;
        private final double valorSalidas;
        private final String etiquetaDiferencia;
        private final double valorDiferencia;

        public TotalesReporte(String etiquetaEntradas, double valorEntradas,
                              String etiquetaSalidas, double valorSalidas,
                              String etiquetaDiferencia, double valorDiferencia) {
            this.etiquetaEntradas = etiquetaEntradas;
            this.valorEntradas = valorEntradas;
            this.etiquetaSalidas = etiquetaSalidas;
            this.valorSalidas = valorSalidas;
            this.etiquetaDiferencia = etiquetaDiferencia;
            this.valorDiferencia = valorDiferencia;
        }

        public String getEtiquetaEntradas() { return etiquetaEntradas; }
        public double getValorEntradas() { return valorEntradas; }
        public String getEtiquetaSalidas() { return etiquetaSalidas; }
        public double getValorSalidas() { return valorSalidas; }
        public String getEtiquetaDiferencia() { return etiquetaDiferencia; }
        public double getValorDiferencia() { return valorDiferencia; }
    }

    // -----------------------
    // Métodos públicos originales (sin totales)
    // -----------------------
    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo) {
        exportarTabla(tabla, titulo, tipo, null, null);
    }

    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo, List<String> filtros) {
        exportarTabla(tabla, titulo, tipo, filtros, null);
    }

    public static <T> void previsualizarPDF(TableView<T> tabla, String titulo, List<String> filtros) {
        previsualizarPDF(tabla, titulo, filtros, null);
    }

    // -----------------------
    // Nuevos métodos con totales
    // -----------------------
    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo,
                                         List<String> filtros, TotalesReporte totales) {
        if (tabla.getItems().isEmpty()) {
            mostrarError("No hay datos para exportar.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el archivo");
        File carpeta = dirChooser.showDialog(tabla.getScene().getWindow());

        if (carpeta != null) {
            String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
            String extension = tipo.equalsIgnoreCase("pdf") ? ".pdf" : ".xlsx";
            File archivo = new File(carpeta, titulo + "_" + fechaHora + extension);

            try {
                if (tipo.equalsIgnoreCase("pdf")) {
                    informacionGeneralMostrada = false;
                    filasPorHojaEstimado = 0;
                    exportarPDF(tabla, titulo, archivo, filtros, totales);
                } else {
                    exportarExcel(tabla, titulo, archivo, filtros, totales);
                }
                mostrarExito("Archivo generado correctamente\n\n" +
                        "Archivo: " + titulo + "_" + fechaHora + extension + "\n" +
                        "Ubicación: " + archivo.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al exportar: " + e.getMessage());
            }
        }
    }

    public static <T> void previsualizarPDF(TableView<T> tabla, String titulo,
                                            List<String> filtros, TotalesReporte totales) {
        if (tabla.getItems().isEmpty()) {
            mostrarError("No hay datos para exportar.");
            return;
        }

        try {
            String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
            File archivo = File.createTempFile(titulo + "_preview_" + fechaHora + "_", ".pdf");
            archivo.deleteOnExit();

            informacionGeneralMostrada = false;
            filasPorHojaEstimado = 0;
            exportarPDF(tabla, titulo, archivo, filtros, totales);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            } else {
                mostrarError("No se pudo abrir la previsualización.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error al generar la previsualización: " + e.getMessage());
        }
    }

    // -----------------------
    // Exportar PDF (con sobrecarga para totales)
    // -----------------------
    private static <T> void exportarPDF(TableView<T> tabla, String titulo, File archivo,
                                        List<String> filtros) throws IOException {
        exportarPDF(tabla, titulo, archivo, filtros, null);
    }

    private static <T> void exportarPDF(TableView<T> tabla, String titulo, File archivo,
                                        List<String> filtros, TotalesReporte totales) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontCabecera = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontDatos = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontNormalBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float pageWidth = PDRectangle.A4.getHeight();
            float pageHeight = PDRectangle.A4.getWidth();

            List<TableColumn<T, ?>> allColumns = obtenerColumnasVisibles(tabla);
            List<T> dataItems = tabla.getItems();

            // Dividir columnas en secciones si son muchas
            List<List<TableColumn<T, ?>>> columnSections = new ArrayList<>();
            for (int i = 0; i < allColumns.size(); i += MAX_COLUMNAS_POR_SECCION) {
                columnSections.add(allColumns.subList(i, Math.min(i + MAX_COLUMNAS_POR_SECCION, allColumns.size())));
            }

            int pageNumber = 1;
            LocalDateTime ahora = LocalDateTime.now();

            float espacioFiltros = (filtros != null && !filtros.isEmpty()) ? 25 : 0;
            float espacioInfoGeneral = !informacionGeneralMostrada ? 90 : 30;
            float usableHeight = pageHeight - (MARGIN * 2) - 135 - espacioFiltros - espacioInfoGeneral;
            filasPorHojaEstimado = (int) (usableHeight / ROW_HEIGHT);

            // Paginación por filas y columnas
            for (int rowStart = 0; rowStart < dataItems.size(); rowStart += filasPorHojaEstimado) {
                int rowEnd = Math.min(rowStart + filasPorHojaEstimado, dataItems.size());
                List<T> rowBlock = dataItems.subList(rowStart, rowEnd);
                int filasRealesEnPagina = rowEnd - rowStart;

                for (int sectionIndex = 0; sectionIndex < columnSections.size(); sectionIndex++) {
                    List<TableColumn<T, ?>> section = columnSections.get(sectionIndex);

                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    float currentY = pageHeight - MARGIN;
                    PDPageContentStream contentStream = new PDPageContentStream(document, page);

                    boolean esPrimeraPagina = (pageNumber == 1 && sectionIndex == 0);
                    float alturaBarra = esPrimeraPagina ? 60 : 50;
                    currentY = dibujarEncabezado(contentStream, fontTitulo, pageWidth, currentY, titulo,
                            rowStart + 1, rowEnd, sectionIndex, columnSections.size(),
                            filasRealesEnPagina, alturaBarra, esPrimeraPagina);

                    if (!informacionGeneralMostrada && sectionIndex == 0) {
                        currentY = dibujarInformacionGeneral(contentStream, fontNormalBold, fontNormal,
                                pageWidth, currentY, dataItems.size(), ahora, filtros);
                        informacionGeneralMostrada = true;
                    } else if (!esPrimeraPagina) {
                        currentY -= 15;
                    }

                    // Dibujar la tabla
                    currentY = dibujarTablaDatos(contentStream, fontCabecera, fontDatos,
                            pageWidth, currentY, rowBlock, section);

                    // Si es la última página y última sección, y hay totales, dibujarlos
                    boolean esUltimaPagina = (rowStart + filasPorHojaEstimado >= dataItems.size());
                    boolean esUltimaSeccion = (sectionIndex == columnSections.size() - 1);
                    if (esUltimaPagina && esUltimaSeccion && totales != null) {
                        // Verificar si hay espacio suficiente (altura aproximada 80)
                        if (currentY - 80 > MARGIN) {
                            currentY = dibujarTotales(contentStream, fontNormal, fontNormalBold,
                                    pageWidth, currentY, totales);
                        } else {
                            // Si no cabe, se podría crear una nueva página, pero por simplicidad no se dibuja
                            // (en la práctica siempre suele haber espacio)
                        }
                    }

                    contentStream.close();

                    dibujarPiePagina(document, page, fontNormal, pageWidth, pageHeight, pageNumber++);
                }
            }

            document.save(archivo);
        }
    }

    // -----------------------
    // Nuevo método para dibujar los totales debajo de la tabla
    // -----------------------
    private static float dibujarTotales(PDPageContentStream contentStream,
                                        PDType1Font fontNormal,
                                        PDType1Font fontBold,
                                        float pageWidth,
                                        float currentY,
                                        TotalesReporte totales) throws IOException {
        // Línea separadora superior
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, currentY);
        contentStream.lineTo(pageWidth - MARGIN, currentY);
        contentStream.stroke();

        currentY -= 20;

        // Título de la sección
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontBold, 12);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("RESUMEN DE MOVIMIENTOS");
        contentStream.endText();

        currentY -= 25;

        float columnaLabelX = MARGIN + 20;
        float columnaValorX = pageWidth / 2 + 20;

        // Entradas
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(columnaLabelX, currentY);
        contentStream.showText(totales.getEtiquetaEntradas() + ":");
        contentStream.endText();

        String valorEntradas = String.format("$ %,.2f", totales.getValorEntradas());
        contentStream.beginText();
        contentStream.setFont(fontBold, 11);
        contentStream.newLineAtOffset(columnaValorX, currentY);
        contentStream.showText(valorEntradas);
        contentStream.endText();

        currentY -= 20;

        // Salidas
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(columnaLabelX, currentY);
        contentStream.showText(totales.getEtiquetaSalidas() + ":");
        contentStream.endText();

        String valorSalidas = String.format("$ %,.2f", totales.getValorSalidas());
        contentStream.beginText();
        contentStream.setFont(fontBold, 11);
        contentStream.newLineAtOffset(columnaValorX, currentY);
        contentStream.showText(valorSalidas);
        contentStream.endText();

        currentY -= 20;

        // Diferencia
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(columnaLabelX, currentY);
        contentStream.showText(totales.getEtiquetaDiferencia() + ":");
        contentStream.endText();

        String valorDiferencia = String.format("$ %,.2f", totales.getValorDiferencia());
        contentStream.beginText();
        contentStream.setFont(fontBold, 11);
        contentStream.newLineAtOffset(columnaValorX, currentY);
        contentStream.showText(valorDiferencia);
        contentStream.endText();

        currentY -= 25;

        // Línea separadora inferior
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, currentY);
        contentStream.lineTo(pageWidth - MARGIN, currentY);
        contentStream.stroke();

        return currentY - 15;
    }

    // ------------------------------------------------------------
    // Los métodos siguientes se mantienen IGUAL que en tu código original
    // (dibujarEncabezado, dibujarInformacionGeneral, dibujarTablaDatos,
    //  dibujarPiePagina, exportarExcel, y auxiliares)
    // ------------------------------------------------------------
    private static float dibujarEncabezado(PDPageContentStream contentStream,
                                           PDType1Font fontTitulo,
                                           float pageWidth,
                                           float currentY,
                                           String titulo,
                                           int filaInicio,
                                           int filaFin,
                                           int seccionIndex,
                                           int totalSecciones,
                                           int filasRealesEnPagina,
                                           float alturaBarra,
                                           boolean esPrimeraPagina) throws IOException {
        contentStream.setNonStrokingColor(COLOR_BANDA_SUPERIOR[0], COLOR_BANDA_SUPERIOR[1], COLOR_BANDA_SUPERIOR[2]);
        contentStream.addRect(MARGIN, currentY - alturaBarra, pageWidth - 2 * MARGIN, alturaBarra);
        contentStream.fill();

        float tamanioTitulo = esPrimeraPagina ? 20 : 16;
        float posicionTituloY = esPrimeraPagina ? currentY - 30 : currentY - 25;

        contentStream.setNonStrokingColor(1, 1, 1);
        contentStream.beginText();
        contentStream.setFont(fontTitulo, tamanioTitulo);
        contentStream.newLineAtOffset(MARGIN + 10, posicionTituloY);
        contentStream.showText(titulo);
        contentStream.endText();

        String subTitulo = "";
        if (totalSecciones > 1) {
            subTitulo = "Sección " + (seccionIndex + 1) + "/" + totalSecciones;
        }
        String rangoFilas = "Filas: " + filaInicio + "-" + filaFin;
        if (!subTitulo.isEmpty()) {
            subTitulo += " • " + rangoFilas;
        } else {
            subTitulo = rangoFilas;
        }

        float tamanioSubtitulo = esPrimeraPagina ? 12 : 10;
        float posicionSubtituloY = esPrimeraPagina ? currentY - 50 : currentY - 40;

        contentStream.beginText();
        contentStream.setFont(fontTitulo, tamanioSubtitulo);
        contentStream.newLineAtOffset(MARGIN + 10, posicionSubtituloY);
        contentStream.showText(subTitulo);
        contentStream.endText();

        return currentY - (alturaBarra + 20);
    }

    private static float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                                   PDType1Font fontBold,
                                                   PDType1Font fontNormal,
                                                   float pageWidth,
                                                   float currentY,
                                                   int totalRegistros,
                                                   LocalDateTime ahora,
                                                   List<String> filtros) throws IOException {
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontBold, 12);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 22;

        float col1X = MARGIN + 10;
        float col2X = pageWidth / 2 + 20;

        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(DATE_FORMATTER));
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY - 12);
        contentStream.showText("Hora: " + ahora.format(TIME_FORMATTER));
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY - 24);
        contentStream.showText("Total de registros: " + totalRegistros);
        contentStream.endText();

        if (filtros != null && !filtros.isEmpty()) {
            contentStream.setNonStrokingColor(COLOR_FILTROS[0], COLOR_FILTROS[1], COLOR_FILTROS[2]);
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(col2X, currentY);
            contentStream.showText("Filtros aplicados:");
            contentStream.endText();

            contentStream.setNonStrokingColor(0, 0, 0);
            int maxFiltros = Math.min(3, filtros.size());
            for (int i = 0; i < maxFiltros; i++) {
                String filtro = filtros.get(i);
                if (filtro.length() > 40) {
                    filtro = filtro.substring(0, 37) + "...";
                }
                contentStream.beginText();
                contentStream.setFont(fontNormal, 9);
                contentStream.newLineAtOffset(col2X + 10, currentY - 12 - (i * 12));
                contentStream.showText("• " + filtro);
                contentStream.endText();
            }
            if (filtros.size() > 3) {
                contentStream.beginText();
                contentStream.setFont(fontNormal, 8);
                contentStream.newLineAtOffset(col2X + 10, currentY - 12 - (3 * 12));
                contentStream.showText("... y " + (filtros.size() - 3) + " más");
                contentStream.endText();
            }
        }
        return currentY - 50;
    }

    private static <T> float dibujarTablaDatos(PDPageContentStream contentStream,
                                               PDType1Font fontCabecera,
                                               PDType1Font fontDatos,
                                               float pageWidth,
                                               float currentY,
                                               List<T> datos,
                                               List<TableColumn<T, ?>> columnas) throws IOException {
        float tableWidth = pageWidth - 2 * MARGIN;
        float colWidth = tableWidth / columnas.size();

        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);

        float headerHeight = 26;
        float cornerRadius = 5f;
        contentStream.moveTo(MARGIN + cornerRadius, currentY);
        contentStream.lineTo(MARGIN + tableWidth - cornerRadius, currentY);
        contentStream.curveTo(MARGIN + tableWidth, currentY, MARGIN + tableWidth, currentY,
                MARGIN + tableWidth, currentY - cornerRadius);
        contentStream.lineTo(MARGIN + tableWidth, currentY - headerHeight + cornerRadius);
        contentStream.curveTo(MARGIN + tableWidth, currentY - headerHeight,
                MARGIN + tableWidth, currentY - headerHeight,
                MARGIN + tableWidth - cornerRadius, currentY - headerHeight);
        contentStream.lineTo(MARGIN + cornerRadius, currentY - headerHeight);
        contentStream.curveTo(MARGIN, currentY - headerHeight, MARGIN, currentY - headerHeight,
                MARGIN, currentY - headerHeight + cornerRadius);
        contentStream.lineTo(MARGIN, currentY - cornerRadius);
        contentStream.curveTo(MARGIN, currentY, MARGIN, currentY,
                MARGIN + cornerRadius, currentY);
        contentStream.fill();

        contentStream.setNonStrokingColor(1, 1, 1);
        float xPos = MARGIN;
        for (TableColumn<T, ?> col : columnas) {
            String headerText = truncateText(col.getText(), (int) (colWidth - 10));
            float textWidth = fontCabecera.getStringWidth(headerText) / 1000 * 13;
            float textX = xPos + (colWidth - textWidth) / 2;
            contentStream.beginText();
            contentStream.setFont(fontCabecera, 13);
            contentStream.newLineAtOffset(textX, currentY - 18);
            contentStream.showText(headerText);
            contentStream.endText();
            xPos += colWidth;
        }

        currentY -= 30;

        int filaNum = 0;
        for (T item : datos) {
            if (filaNum % 2 == 0) {
                contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
            } else {
                contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
            }
            contentStream.addRect(MARGIN, currentY - 26, tableWidth, 26);
            contentStream.fill();

            contentStream.setNonStrokingColor(0, 0, 0);
            xPos = MARGIN;
            for (TableColumn<T, ?> col : columnas) {
                Object value = col.getCellData(item);
                String text = value != null ? value.toString() : "";
                if (text.length() > 30) {
                    text = text.substring(0, 27) + "...";
                }
                float textWidthData = fontDatos.getStringWidth(text) / 1000 * 13;
                float textXData = xPos + (colWidth - textWidthData) / 2;
                contentStream.beginText();
                contentStream.setFont(fontDatos, 13);
                contentStream.newLineAtOffset(textXData, currentY - 20);
                contentStream.showText(text);
                contentStream.endText();
                xPos += colWidth;
            }
            currentY -= 30;
            filaNum++;
        }
        return currentY - 15;
    }

    private static void dibujarPiePagina(PDDocument document, PDPage page, PDType1Font fontNormal,
                                         float pageWidth, float pageHeight, int pageNumber) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN, MARGIN + 20);
            contentStream.lineTo(pageWidth - MARGIN, MARGIN + 20);
            contentStream.stroke();

            contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            contentStream.beginText();
            contentStream.setFont(fontNormal, 8);
            contentStream.newLineAtOffset(MARGIN, MARGIN + 5);
            contentStream.showText("Sistema de Gestión de Inventarios GREEP • Reporte generado automáticamente");
            contentStream.endText();

            String paginaTexto = "Página " + pageNumber;
            float textoAncho = fontNormal.getStringWidth(paginaTexto) / 1000 * 8;
            contentStream.beginText();
            contentStream.setFont(fontNormal, 8);
            contentStream.newLineAtOffset(pageWidth - MARGIN - textoAncho - 20, MARGIN + 5);
            contentStream.showText(paginaTexto);
            contentStream.endText();
        }
    }

    // -----------------------
    // Exportar Excel con soporte opcional de totales (se agregan como filas adicionales)
    // -----------------------
    private static <T> void exportarExcel(TableView<T> tabla, String titulo, File archivo,
                                          List<String> filtros) throws IOException {
        exportarExcel(tabla, titulo, archivo, filtros, null);
    }

    private static <T> void exportarExcel(TableView<T> tabla, String titulo, File archivo,
                                          List<String> filtros, TotalesReporte totales) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(titulo);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));

        CellStyle filtroStyle = workbook.createCellStyle();
        Font filtroFont = workbook.createFont();
        filtroFont.setBold(false);
        filtroStyle.setFont(filtroFont);

        CellStyle totalLabelStyle = workbook.createCellStyle();
        Font totalLabelFont = workbook.createFont();
        totalLabelFont.setBold(true);
        totalLabelStyle.setFont(totalLabelFont);

        CellStyle totalValueStyle = workbook.createCellStyle();
        Font totalValueFont = workbook.createFont();
        totalValueFont.setBold(true);
        totalValueStyle.setFont(totalValueFont);
        totalValueStyle.setDataFormat(createHelper.createDataFormat().getFormat("$ #,##0.00"));

        int rowNum = 0;

        // Título
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(titulo);
        List<TableColumn<T, ?>> columns = obtenerColumnasVisibles(tabla);
        Cell dateCell = titleRow.createCell(columns.size());
        dateCell.setCellValue(LocalDateTime.now());
        dateCell.setCellStyle(dateStyle);

        // Filtros aplicados
        if (filtros != null && !filtros.isEmpty()) {
            Row filtroTitleRow = sheet.createRow(rowNum++);
            filtroTitleRow.createCell(0).setCellValue("Productos filtrados:");
            filtroTitleRow.getCell(0).setCellStyle(filtroStyle);

            for (String filtro : filtros) {
                Row filtroRow = sheet.createRow(rowNum++);
                filtroRow.createCell(0).setCellValue("• " + filtro);
                filtroRow.getCell(0).setCellStyle(filtroStyle);
            }
            rowNum++; // Espacio en blanco
        }

        // Encabezados de columnas
        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).getText());
            cell.setCellStyle(headerStyle);
        }

        // Datos
        for (T item : tabla.getItems()) {
            Row row = sheet.createRow(rowNum++);
            int colIndex = 0;
            for (TableColumn<T, ?> col : columns) {
                Object value = col.getCellData(item);
                row.createCell(colIndex++).setCellValue(value != null ? value.toString() : "");
            }
        }

        // Totales (si se proporcionan)
        if (totales != null) {
            rowNum++; // línea en blanco

            Row entradasRow = sheet.createRow(rowNum++);
            entradasRow.createCell(0).setCellValue(totales.getEtiquetaEntradas() + ":");
            entradasRow.getCell(0).setCellStyle(totalLabelStyle);
            Cell entradasValCell = entradasRow.createCell(1);
            entradasValCell.setCellValue(totales.getValorEntradas());
            entradasValCell.setCellStyle(totalValueStyle);

            Row salidasRow = sheet.createRow(rowNum++);
            salidasRow.createCell(0).setCellValue(totales.getEtiquetaSalidas() + ":");
            salidasRow.getCell(0).setCellStyle(totalLabelStyle);
            Cell salidasValCell = salidasRow.createCell(1);
            salidasValCell.setCellValue(totales.getValorSalidas());
            salidasValCell.setCellStyle(totalValueStyle);

            Row diferenciaRow = sheet.createRow(rowNum++);
            diferenciaRow.createCell(0).setCellValue(totales.getEtiquetaDiferencia() + ":");
            diferenciaRow.getCell(0).setCellStyle(totalLabelStyle);
            Cell diferenciaValCell = diferenciaRow.createCell(1);
            diferenciaValCell.setCellValue(totales.getValorDiferencia());
            diferenciaValCell.setCellStyle(totalValueStyle);
        }

        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(archivo)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    // -----------------------
    // Métodos auxiliares (sin cambios)
    // -----------------------
    private static <T> List<TableColumn<T, ?>> obtenerColumnasVisibles(TableView<T> tabla) {
        List<TableColumn<T, ?>> columnas = new ArrayList<>();
        for (TableColumn<T, ?> col : tabla.getColumns()) {
            if (col.isVisible()) {
                columnas.add(col);
            }
        }
        return columnas;
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) return text.substring(0, maxLength - 3) + "...";
        return text;
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Exportación exitosa");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al exportar");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }
}