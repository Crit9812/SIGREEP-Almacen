package Compartido.exportar;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
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

public class ReporteAjusteExporter extends AbstractPDFExporter<Object, ReporteAjusteExporter.DetalleAjustePDF> {

    @Override
    protected TipoOperacion getTipoOperacion() {
        return TipoOperacion.AJUSTE_INVENTARIO;
    }

    // Clase interna para detalles de ajuste
    public static class DetalleAjustePDF {
        private String tipo; // "E" o "S"
        private String claveProducto;
        private String producto;
        private String descripcion;
        private int cantidad;
        private String presentacion;
        private int factor;
        private String lote;
        private String precioUnitario;
        private String precioIva;
        private String precioBruto;
        private String precioTotal;
        private List<UbicacionCompra> ubicaciones;

        public DetalleAjustePDF(String tipo, String claveProducto, String producto, String descripcion,
                                int cantidad, String presentacion, int factor, String lote,
                                String precioUnitario, String precioIva, String precioBruto,
                                String precioTotal, List<UbicacionCompra> ubicaciones) {
            this.tipo = tipo;
            this.claveProducto = claveProducto;
            this.producto = producto;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.lote = lote;
            this.precioUnitario = precioUnitario;
            this.precioIva = precioIva;
            this.precioBruto = precioBruto;
            this.precioTotal = precioTotal;
            this.ubicaciones = ubicaciones != null ? new ArrayList<>(ubicaciones) : new ArrayList<>();
        }

        // Getters
        public String getTipo() { return tipo; }
        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getDescripcion() { return descripcion; }
        public int getCantidad() { return cantidad; }
        public String getPresentacion() { return presentacion; }
        public int getFactor() { return factor; }
        public String getLote() { return lote; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioIva() { return precioIva; }
        public String getPrecioBruto() { return precioBruto; }
        public String getPrecioTotal() { return precioTotal; }
        public List<UbicacionCompra> getUbicaciones() { return new ArrayList<>(ubicaciones); }
    }

    @Override
    protected List<DetalleAjustePDF> convertirItemsADetallesPDF(List<Object> items) {
        List<DetalleAjustePDF> detalles = new ArrayList<>();

        for (Object item : items) {
            if (item instanceof compra) {
                compra c = (compra) item;
                DetalleAjustePDF detalle = new DetalleAjustePDF(
                        "E", // Entrada
                        c.getClaveProducto(),
                        c.getProducto(),
                        c.getDescripcion(),
                        c.getCantidad(),
                        c.getPresentacion(),
                        Integer.parseInt(c.getFactor()),
                        c.getLote(),
                        c.getPrecioEntrada(),
                        c.getPrecioIva(),
                        c.getPrecioBruto(),
                        c.getPrecioTotal(),
                        c.getUbicaciones()
                );
                detalles.add(detalle);
            } else if (item instanceof traspasoSalida) {
                traspasoSalida t = (traspasoSalida) item;
                DetalleAjustePDF detalle = new DetalleAjustePDF(
                        "S", // Salida
                        t.getClaveProducto(),
                        t.getProducto(),
                        t.getDescripcion(),
                        t.getCantidad(),
                        t.getPresentacion(),
                        t.getFactor(),
                        t.getLote(),
                        t.getPrecioEntrada(),
                        t.getPrecioIva(),
                        t.getPrecioBruto(),
                        t.getPrecioTotal(),
                        t.getUbicaciones()
                );
                detalles.add(detalle);
            }
        }
        return detalles;
    }

    @Override
    protected Map<String, List<UbicacionCompra>> extraerUbicacionesPorProducto(List<Object> items) {
        Map<String, List<UbicacionCompra>> mapa = new HashMap<>();

        for (Object item : items) {
            if (item instanceof compra) {
                compra c = (compra) item;
                if (c.getUbicaciones() != null && !c.getUbicaciones().isEmpty()) {
                    mapa.put(c.getClaveProducto(), c.getUbicaciones());
                }
            } else if (item instanceof traspasoSalida) {
                traspasoSalida t = (traspasoSalida) item;
                if (t.getUbicaciones() != null && !t.getUbicaciones().isEmpty()) {
                    mapa.put(t.getClaveProducto(), t.getUbicaciones());
                }
            }
        }
        return mapa;
    }

    @Override
    protected String[] getHeaders() {
        // Solo 9 columnas: #, Tipo, Clave, Producto, Descripción, Cant., Lote, P. Unit., P. Total
        return new String[]{"#", "Tipo", "Clave", "Producto", "Descripción", "Cant.", "Lote", "P. Unit.", "P. Total"};
    }

    @Override
    protected float[] getColumnWidths() {
        // Anchos ajustados para 9 columnas
        return new float[]{30, 30, 80, 150, 120, 60, 80, 80, 80};
    }

    @Override
    protected void dibujarFilaDetalle(PDPageContentStream contentStream,
                                      PDType1Font fontDatos, PDType1Font fontCabecera,
                                      float currentY, float[] scaledWidths,
                                      DetalleAjustePDF detalle, int filaNum, float xPos) throws IOException {

        contentStream.setNonStrokingColor(0, 0, 0);
        int colIndex = 0;

        // Columna 1: Número
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(String.valueOf(filaNum));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 2: Tipo (E o S) con color
        if ("E".equals(detalle.getTipo())) {
            contentStream.setNonStrokingColor(0, 0.5f, 0); // Verde para entrada
        } else {
            contentStream.setNonStrokingColor(0.8f, 0, 0); // Rojo para salida
        }
        contentStream.beginText();
        contentStream.setFont(fontCabecera, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(detalle.getTipo());
        contentStream.endText();
        xPos += scaledWidths[colIndex++];
        contentStream.setNonStrokingColor(0, 0, 0); // Volver a negro

        // Columna 3: Clave
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getClaveProducto(), 12));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 4: Producto
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getProducto(), 18));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 5: Descripción
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


        // Columna 6: Cantidad
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(String.valueOf(detalle.getCantidad()));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 7: Lote
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getLote(), 8));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 8: Precio Unitario
        contentStream.beginText();
        contentStream.setFont(fontDatos, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getPrecioUnitario(), 10));
        contentStream.endText();
        xPos += scaledWidths[colIndex++];

        // Columna 9: Precio Total
        // Color diferente para E/S
        if ("E".equals(detalle.getTipo())) {
            contentStream.setNonStrokingColor(0, 0.5f, 0); // Verde para entrada
        } else {
            contentStream.setNonStrokingColor(0.8f, 0, 0); // Rojo para salida
        }
        contentStream.beginText();
        contentStream.setFont(fontCabecera, 9);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(PDFCommons.truncarTexto(detalle.getPrecioTotal(), 12));
        contentStream.endText();
    }

    @Override
    protected float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                              PDType1Font fontSubtitulo, PDType1Font fontNormal,
                                              float pageWidth, float currentY,
                                              List<DetalleAjustePDF> detalles, String nombreDestinatario,
                                              String comentario) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();

        contentStream.setNonStrokingColor(PDFCommons.COLOR_SUBTITULOS[0], PDFCommons.COLOR_SUBTITULOS[1], PDFCommons.COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL DEL AJUSTE");
        contentStream.endText();

        currentY -= 25;

        int totalEntradas = 0;
        int totalSalidas = 0;
        int cantidadEntradas = 0;
        int cantidadSalidas = 0;
        BigDecimal valorEntradas = BigDecimal.ZERO;
        BigDecimal valorSalidas = BigDecimal.ZERO;

        for (DetalleAjustePDF detalle : detalles) {
            if ("E".equals(detalle.getTipo())) {
                totalEntradas++;
                cantidadEntradas += detalle.getCantidad();
                try {
                    String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                    valorEntradas = valorEntradas.add(new BigDecimal(precioStr));
                } catch (NumberFormatException e) {
                    System.err.println("Error al parsear número: " + e.getMessage());
                }
            } else {
                totalSalidas++;
                cantidadSalidas += detalle.getCantidad();
                try {
                    String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                    valorSalidas = valorSalidas.add(new BigDecimal(precioStr));
                } catch (NumberFormatException e) {
                    System.err.println("Error al parsear número: " + e.getMessage());
                }
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

        if (comentario != null && !comentario.trim().isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
            contentStream.showText("Comentario: " + comentario);
            contentStream.endText();
            lineCount++;
        }

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Total de entradas (E): " + totalEntradas + " productos");
        contentStream.endText();
        lineCount++;

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Total de salidas (S): " + totalSalidas + " productos");
        contentStream.endText();
        lineCount++;

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Cantidad total entradas: " + cantidadEntradas + " unidades");
        contentStream.endText();
        lineCount++;

        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Cantidad total salidas: " + cantidadSalidas + " unidades");
        contentStream.endText();
        lineCount++;

        // Mostrar totales en la derecha
        String valorEntradasTexto = "Entradas: $" + String.format("%,.2f", valorEntradas);
        String valorSalidasTexto = "Salidas: $" + String.format("%,.2f", valorSalidas);
        BigDecimal neto = valorEntradas.subtract(valorSalidas);
        String valorNetoTexto = "Neto: $" + String.format("%,.2f", neto);

        float textoAncho = fontSubtitulo.getStringWidth(valorNetoTexto) / 1000 * 12;
        float col2X = pageWidth - PDFCommons.MARGIN - textoAncho - 20;

        // Entradas en verde
        contentStream.setNonStrokingColor(0, 0.5f, 0);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 11);
        contentStream.newLineAtOffset(col2X, currentY - 10);
        contentStream.showText(valorEntradasTexto);
        contentStream.endText();

        // Salidas en rojo
        contentStream.setNonStrokingColor(0.8f, 0, 0);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 11);
        contentStream.newLineAtOffset(col2X, currentY - 30);
        contentStream.showText(valorSalidasTexto);
        contentStream.endText();

        // Neto en color según el resultado
        if (neto.compareTo(BigDecimal.ZERO) >= 0) {
            contentStream.setNonStrokingColor(0, 0.5f, 0);
        } else {
            contentStream.setNonStrokingColor(0.8f, 0, 0);
        }
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 11);
        contentStream.newLineAtOffset(col2X, currentY - 50);
        contentStream.showText(valorNetoTexto);
        contentStream.endText();

        // Leyenda de colores
        contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(col1X, currentY - (lineCount * PDFCommons.LINE_HEIGHT + 10));
        contentStream.showText("Leyenda: ");
        contentStream.endText();

        contentStream.setNonStrokingColor(0, 0.5f, 0);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(col1X + 50, currentY - (lineCount * PDFCommons.LINE_HEIGHT + 10));
        contentStream.showText("E = Entrada");
        contentStream.endText();

        contentStream.setNonStrokingColor(0.8f, 0, 0);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(col1X + 130, currentY - (lineCount * PDFCommons.LINE_HEIGHT + 10));
        contentStream.showText("S = Salida");
        contentStream.endText();

        float yFinal = currentY - ((lineCount + 1) * PDFCommons.LINE_HEIGHT + 20);

        return yFinal;
    }

    @Override
    protected String obtenerClaveProducto(DetalleAjustePDF detalle) {
        return detalle.getClaveProducto();
    }

    @Override
    protected String obtenerNombreProducto(DetalleAjustePDF detalle) {
        return detalle.getProducto();
    }

    public static void exportarReporteAjuste(String claveAjuste,
                                             String comentario,
                                             List<Object> itemsAjuste,
                                             Window owner) {
        new ReporteAjusteExporter()
                .exportar(claveAjuste, "", comentario, itemsAjuste, owner);
    }
}