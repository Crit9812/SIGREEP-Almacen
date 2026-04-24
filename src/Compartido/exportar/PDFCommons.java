package Compartido.exportar;

import javafx.scene.control.Alert;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PDFCommons {
    // Formatters
    public static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colores
    public static final float[] COLOR_BANDA_SUPERIOR = {0.5686f, 0.8314f, 0.5216f};
    public static final float[] COLOR_SUBTITULOS = {0.2f, 0.2f, 0.2f};
    public static final float[] COLOR_PRODUCTOS = {0.0078f, 0.4549f, 0.7451f};
    public static final float[] COLOR_TOTALES = {0.8510f, 0.3255f, 0.3098f};
    public static final float[] COLOR_ENCABEZADO_TABLA = {0.95f, 0.95f, 0.95f};
    public static final float[] COLOR_FILA_PAR = {0.98f, 0.98f, 0.98f};
    public static final float[] COLOR_FILA_IMPAR = {1.0f, 1.0f, 1.0f};

    // Constantes de diseño
    public static final float MARGIN = 40f;
    public static final float LINE_HEIGHT = 14f;
    public static final float SECTION_SPACING = 20f;
    public static final float CELL_PADDING = 5f;
    public static final float MIN_Y = MARGIN + 50f;

    // Métodos utilitarios
    public static float[] escalarAnchosColumnas(float[] widths, float totalWidth) {
        float[] scaled = new float[widths.length];
        float sum = 0;
        for (float width : widths) {
            sum += width;
        }
        float scaleFactor = totalWidth / sum;
        for (int i = 0; i < widths.length; i++) {
            scaled[i] = widths[i] * scaleFactor;
        }
        return scaled;
    }

    public static String truncarTexto(String texto, int maxLength) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= maxLength) {
            return texto;
        }
        return texto.substring(0, maxLength - 3) + "...";
    }

    public static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Reporte generado");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    public static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al generar reporte");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    // Métodos de dibujo comunes
    public static void dibujarPiePagina(PDPageContentStream contentStream,
                                        PDType1Font fontNormal,
                                        float pageWidth,
                                        int paginaActual,
                                        String tipoReporte) throws IOException {
        // Línea separadora
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, MARGIN + 20);
        contentStream.lineTo(pageWidth - MARGIN, MARGIN + 20);
        contentStream.stroke();

        // Texto del pie de página
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(MARGIN, MARGIN + 5);
        contentStream.showText("Sistema de Gestión de Inventarios GREEP - Reporte de " + tipoReporte + " generado automáticamente");
        contentStream.endText();

        // Número de página
        String textoPagina = "Página " + paginaActual;
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(pageWidth - MARGIN - 50, MARGIN + 5);
        contentStream.showText(textoPagina);
        contentStream.endText();
    }

    // Clases auxiliares para paginación
    public static class PaginacionResultado {
        private int indiceSiguiente;
        private boolean completa;
        private float currentY;

        public PaginacionResultado(int indiceSiguiente, boolean completa, float currentY) {
            this.indiceSiguiente = indiceSiguiente;
            this.completa = completa;
            this.currentY = currentY;
        }

        public int getIndiceSiguiente() { return indiceSiguiente; }
        public boolean isCompleta() { return completa; }
        public float getCurrentY() { return currentY; }
    }

    public static class PaginacionResultadoUbicaciones {
        private int indiceProductoSiguiente;
        private int indiceUbicacionSiguiente;
        private boolean completa;
        private float currentY;
        private String productoActual;

        public PaginacionResultadoUbicaciones(int indiceProductoSiguiente, int indiceUbicacionSiguiente,
                                              boolean completa, float currentY, String productoActual) {
            this.indiceProductoSiguiente = indiceProductoSiguiente;
            this.indiceUbicacionSiguiente = indiceUbicacionSiguiente;
            this.completa = completa;
            this.currentY = currentY;
            this.productoActual = productoActual;
        }

        public int getIndiceProductoSiguiente() { return indiceProductoSiguiente; }
        public int getIndiceUbicacionSiguiente() { return indiceUbicacionSiguiente; }
        public boolean isCompleta() { return completa; }
        public float getCurrentY() { return currentY; }
        public String getProductoActual() { return productoActual; }
    }
}