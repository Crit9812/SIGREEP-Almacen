package Compartido.exportar;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteEntradaExporter extends AbstractPDFExporter<compra, ReporteEntradaExporter.DetalleEntradaPDF> {

    private final TipoOperacion tipoOperacion;

    public ReporteEntradaExporter(TipoOperacion tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    @Override
    protected TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    // Clase interna para detalles de entrada
    public static class DetalleEntradaPDF {
        private String claveProducto;
        private String producto;
        private String descripcion;
        private int cantidad;
        private String presentacion;
        private String factor;
        private String lote;
        private String caducidad;
        private String ubicacionResumen;
        private String nota;
        private String precioUnitario;
        private String precioIva;
        private String precioBruto;
        private String precioTotal;
        private List<UbicacionCompra> ubicaciones;

        public DetalleEntradaPDF(String claveProducto, String producto, String descripcion,
                                 int cantidad, String presentacion, String factor,
                                 String lote, String caducidad, String ubicacionResumen,
                                 String nota, String precioUnitario, String precioIva,
                                 String precioBruto, String precioTotal,
                                 List<UbicacionCompra> ubicaciones) {
            this.claveProducto = claveProducto;
            this.producto = producto;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.lote = lote;
            this.caducidad = caducidad;
            this.ubicacionResumen = ubicacionResumen;
            this.nota = nota;
            this.precioUnitario = precioUnitario;
            this.precioIva = precioIva;
            this.precioBruto = precioBruto;
            this.precioTotal = precioTotal;
            this.ubicaciones = ubicaciones != null ? new ArrayList<>(ubicaciones) : new ArrayList<>();
        }

        // Getters
        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getDescripcion() { return descripcion; }
        public int getCantidad() { return cantidad; }
        public String getPresentacion() { return presentacion; }
        public String getFactor() { return factor; }
        public String getLote() { return lote; }
        public String getCaducidad() { return caducidad; }
        public String getUbicacionResumen() { return ubicacionResumen; }
        public String getNota() { return nota; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioIva() { return precioIva; }
        public String getPrecioBruto() { return precioBruto; }
        public String getPrecioTotal() { return precioTotal; }
        public List<UbicacionCompra> getUbicaciones() { return new ArrayList<>(ubicaciones); }
    }

    @Override
    protected List<DetalleEntradaPDF> convertirItemsADetallesPDF(List<compra> items) {
        List<DetalleEntradaPDF> detalles = new ArrayList<>();
        for (compra item : items) {
            DetalleEntradaPDF detalle = new DetalleEntradaPDF(
                    item.getClaveProducto(),
                    item.getProducto(),
                    item.getDescripcion(),
                    item.getCantidad(),
                    item.getPresentacion(),
                    item.getFactor(),
                    item.getLote(),
                    item.getCaducidad(),
                    item.getUbicacionResumen(),
                    item.getNota(),
                    item.getPrecioEntrada(),
                    item.getPrecioIva(),
                    item.getPrecioBruto(),
                    item.getPrecioTotal(),
                    item.getUbicaciones()
            );
            detalles.add(detalle);
        }
        return detalles;
    }

    @Override
    protected Map<String, List<UbicacionCompra>> extraerUbicacionesPorProducto(List<compra> items) {
        Map<String, List<UbicacionCompra>> mapa = new HashMap<>();
        for (compra item : items) {
            if (item.getUbicaciones() != null && !item.getUbicaciones().isEmpty()) {
                mapa.put(item.getClaveProducto(), item.getUbicaciones());
            }
        }
        return mapa;
    }

    @Override
    protected String[] getHeaders() {
        return ConfiguracionReporte.getConfiguracion(tipoOperacion).getHeaders();
    }

    @Override
    protected float[] getColumnWidths() {
        return ConfiguracionReporte.getConfiguracion(tipoOperacion).getWidths();
    }

    @Override
    protected void dibujarFilaDetalle(PDPageContentStream contentStream,
                                      PDType1Font fontDatos, PDType1Font fontCabecera,
                                      float currentY, float[] scaledWidths,
                                      DetalleEntradaPDF detalle, int filaNum, float xPos) throws IOException {

        contentStream.setNonStrokingColor(0, 0, 0);
        int colIndex = 0;

        // Columna 1: Número
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(String.valueOf(filaNum));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 2: Clave
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getClaveProducto(), 12));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 3: Producto
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getProducto(), 18));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 4: Descripción con 2 líneas inteligentes
        String descripcion = detalle.getDescripcion();
        String linea1 = descripcion;
        String linea2 = "";

        if (descripcion.length() > 16) {
            // Buscar el último espacio antes del carácter 16
            int lastSpace = descripcion.lastIndexOf(" ", 16);
            if (lastSpace > 0) {
                linea1 = descripcion.substring(0, lastSpace);
                linea2 = descripcion.substring(lastSpace + 1, Math.min(lastSpace + 1 + 16, descripcion.length()));
            } else {
                // No hay espacio, cortar forzosamente
                linea1 = descripcion.substring(0, 16);
                linea2 = descripcion.substring(16, Math.min(32, descripcion.length()));
            }
        }

        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 8);
        contentStream.showText(linea1);
        contentStream.endText();

        if (!linea2.isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 18);
            contentStream.showText(linea2);
            contentStream.endText();
        }

        xPos += scaledWidths[colIndex++];

        // Columna 5: Cantidad
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(String.valueOf(detalle.getCantidad()));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Las siguientes columnas dependen del tipo de operación
        if (tipoOperacion == TipoOperacion.COMPRA) {
            // Columna 6: Presentación (Compra)
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getPresentacion(), 8));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 7: Factor (Compra)
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getFactor(), 8));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 8: Lote (Compra)
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getLote(), 8));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 9: Caducidad (Compra)
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getCaducidad(), 10));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 10: Precio Unitario (Compra)
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getPrecioUnitario(), 10));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 11: Precio Total (Compra) - en color especial
            contentStream.setNonStrokingColor(PDFCommons.COLOR_TOTALES[0], PDFCommons.COLOR_TOTALES[1], PDFCommons.COLOR_TOTALES[2]);
            contentStream.beginText();
            contentStream.setFont(fontCabecera, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getPrecioTotal(), 12));
            contentStream.endText();
        } else if (tipoOperacion == TipoOperacion.TRASPASO_ENTRADA) {
            // Para traspaso entrada: Presentación, Factor, Ubicación
            // Columna 6: Presentación
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getPresentacion(), 8));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 7: Factor
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getFactor(), 8));
            contentStream.endText();
            xPos += scaledWidths[colIndex++];

            // Columna 8: Ubicación
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
            contentStream.showText(PDFCommons.truncarTexto(detalle.getUbicacionResumen(), 20));
            contentStream.endText();
        }
    }

    @Override
    protected float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                              PDType1Font fontSubtitulo, PDType1Font fontNormal,
                                              float pageWidth, float currentY,
                                              List<DetalleEntradaPDF> detalles, String nombreDestinatario,
                                              String comentario) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();

        contentStream.setNonStrokingColor(PDFCommons.COLOR_SUBTITULOS[0], PDFCommons.COLOR_SUBTITULOS[1], PDFCommons.COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 25;

        int totalProductos = detalles.size();
        int totalCantidad = 0;
        BigDecimal totalPrecio = BigDecimal.ZERO;

        for (DetalleEntradaPDF detalle : detalles) {
            totalCantidad += detalle.getCantidad();
            try {
                String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                totalPrecio = totalPrecio.add(new BigDecimal(precioStr));
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear número: " + e.getMessage());
            }
        }

        float col1X = PDFCommons.MARGIN + 10;
        contentStream.setNonStrokingColor(0, 0, 0);

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(PDFCommons.DATE_FORMATTER));
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - PDFCommons.LINE_HEIGHT);
        contentStream.showText("Hora: " + ahora.format(PDFCommons.TIME_FORMATTER));
        contentStream.endText();

        int lineCount = 2;

        // Para compra: Proveedor
        if (tipoOperacion == TipoOperacion.COMPRA) {
            String textoProveedor = "Proveedor: ";
            String valorProveedor = nombreDestinatario != null ? nombreDestinatario : "No especificado";

            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
            contentStream.showText(textoProveedor + valorProveedor);
            contentStream.endText();

            lineCount++;
        } else if (tipoOperacion == TipoOperacion.TRASPASO_ENTRADA) {
            // Para traspaso entrada: Origen
            String textoOrigen = "Origen: ";
            String valorOrigen = nombreDestinatario != null ? nombreDestinatario : "No especificado";

            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
            contentStream.showText(textoOrigen + valorOrigen);
            contentStream.endText();

            lineCount++;
        }

        float yPosDespuesComentarioLine = lineCount;

        if (comentario != null && !comentario.trim().isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
            contentStream.showText("Comentario: " + comentario);
            contentStream.endText();

            lineCount++;
            yPosDespuesComentarioLine = lineCount;
        }

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (yPosDespuesComentarioLine * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Total de productos: " + totalProductos);
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - ((yPosDespuesComentarioLine + 1) * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Cantidad total: " + totalCantidad + " unidades");
        contentStream.endText();

        // Solo mostrar total si es compra (tiene precios)
        if (tipoOperacion == TipoOperacion.COMPRA) {
            String valorTotalTexto = "Valor total: $" + String.format("%,.2f", totalPrecio);
            float textoAncho = fontSubtitulo.getStringWidth(valorTotalTexto) / 1000 * 12;

            float col2X = pageWidth - PDFCommons.MARGIN - textoAncho - 20;

            contentStream.setNonStrokingColor(PDFCommons.COLOR_TOTALES[0], PDFCommons.COLOR_TOTALES[1], PDFCommons.COLOR_TOTALES[2]);
            contentStream.beginText();
            contentStream.setFont(fontSubtitulo, 12);
            contentStream.newLineAtOffset(col2X, currentY - 5);
            contentStream.showText(valorTotalTexto);
            contentStream.endText();
        }

        float yFinal = currentY - ((yPosDespuesComentarioLine + 3) * PDFCommons.LINE_HEIGHT + 10);

        return yFinal;
    }

    @Override
    protected String obtenerClaveProducto(DetalleEntradaPDF detalle) {
        return detalle.getClaveProducto();
    }

    @Override
    protected String obtenerNombreProducto(DetalleEntradaPDF detalle) {
        return detalle.getProducto();
    }

    // Métodos estáticos para facilitar el uso
    public static void exportarReporteCompra(String claveCompra,
                                             String nombreProveedor,
                                             String comentario,
                                             List<compra> itemsCompra,
                                             Window owner) {
        new ReporteEntradaExporter(TipoOperacion.COMPRA)
                .exportar(claveCompra, nombreProveedor, comentario, itemsCompra, owner);
    }

    public static void exportarReporteTraspasoEntrada(String claveTraspaso,
                                                      String nombreOrigen,
                                                      String comentario,
                                                      List<compra> itemsTraspaso,
                                                      Window owner) {
        new ReporteEntradaExporter(TipoOperacion.TRASPASO_ENTRADA)
                .exportar(claveTraspaso, nombreOrigen, comentario, itemsTraspaso, owner);
    }
}