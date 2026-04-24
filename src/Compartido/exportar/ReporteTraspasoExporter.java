package Compartido.exportar;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReporteTraspasoExporter extends AbstractPDFExporter<model.DetalleEntrada, ReporteTraspasoExporter.DetalleEntradaPDF> {

    private final Map<String, List<UbicacionCompra>> ubicacionesPorProducto;

    // Clase interna para los detalles del PDF - SIMPLIFICADA
    public static class DetalleEntradaPDF {
        private String claveProducto;
        private String producto;
        private String cantidad;
        private String descripcion;
        private String precioUnitario;
        private String precioTotal;
        private String nombreSucursal;


        public DetalleEntradaPDF(model.DetalleEntrada detalle) {
            this.claveProducto = detalle.getClaveProducto();
            this.producto = extraerNombreBase(detalle.getProducto());
            this.descripcion = obtenerDescripcionProducto(detalle.getClaveProducto());
            this.cantidad = detalle.getCantidad();
            this.precioUnitario = detalle.getPrecioUnitario();
            this.precioTotal = detalle.getPrecioTotal();

            // Intentar obtener el nombre de la sucursal si está disponible
            try {
                this.nombreSucursal = detalle.getNombreSucursal();
            } catch (Exception e) {
                this.nombreSucursal = "No especificado";
            }
        }

        // Getters
        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getCantidad() { return cantidad; }
        public String getDescripcion() { return descripcion; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioTotal() { return precioTotal; }
        public String getNombreSucursal() { return nombreSucursal; }
    }
    private static String extraerNombreBase(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) {
            return "";
        }

        // Buscar el primer guion con espacios " - "
        int indiceGuion = nombreCompleto.indexOf(" - ");
        if (indiceGuion > 0) {
            // Devolver solo la parte antes del guion
            return nombreCompleto.substring(0, indiceGuion).trim();
        }

        // Si no hay guion, devolver el nombre completo
        return nombreCompleto;
    }

    private static String obtenerDescripcionProducto(String claveProducto) {
        if (claveProducto == null || claveProducto.isBlank()) {
            return "";
        }
        try {
            GenericDAO<producto> dao = new GenericDAO<>(producto.class);
            String resumen = dao.obtenerResumenProducto(claveProducto);
            return resumen != null ? resumen : "";
        } catch (Exception e) {
            System.out.println("Error al obtener descripción del producto " + claveProducto + ": " + e.getMessage());
            return "";
        }
    }

    public ReporteTraspasoExporter(Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {
        this.ubicacionesPorProducto = ubicacionesPorProducto;
    }

    @Override
    protected TipoOperacion getTipoOperacion() {
        return TipoOperacion.TRASPASO_ENTRADA;
    }

    @Override
    protected List<DetalleEntradaPDF> convertirItemsADetallesPDF(List<model.DetalleEntrada> items) {
        return items.stream()
                .map(DetalleEntradaPDF::new)
                .toList();
    }

    @Override
    protected Map<String, List<UbicacionCompra>> extraerUbicacionesPorProducto(List<model.DetalleEntrada> items) {
        return ubicacionesPorProducto;
    }

    @Override
    protected String[] getHeaders() {
        return new String[]{"#", "Clave", "Producto", "Descripción", "Cantidad", "Precio Unit.", "Precio Total"};
    }

    @Override
    protected float[] getColumnWidths() {
        return new float[]{40, 80, 120, 100, 70, 80, 80};
    }

    @Override
    protected void dibujarFilaDetalle(org.apache.pdfbox.pdmodel.PDPageContentStream contentStream,
                                      PDType1Font fontDatos,
                                      PDType1Font fontCabecera,
                                      float currentY, float[] scaledWidths,
                                      DetalleEntradaPDF detalle, int filaNum, float xPos) throws IOException {

        float xPosOriginal = xPos; // Guardar la posición X inicial

        // Columna 1: Número
        contentStream.setNonStrokingColor(0, 0, 0); // Negro
        contentStream.beginText();
        contentStream.setFont(fontDatos, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(String.valueOf(filaNum));
        contentStream.endText();
        xPos += scaledWidths[0];

        // Columna 2: Clave
        contentStream.beginText();
        contentStream.setFont(fontDatos, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(detalle.getClaveProducto());
        contentStream.endText();
        xPos += scaledWidths[1];

        // Columna 3: Producto - TRUNCAR SI ES MUY LARGO (igual que en el original)
        String producto = detalle.getProducto();
        if (producto != null && producto.length() > 25) {
            producto = producto.substring(0, 22) + "...";
        }
        contentStream.beginText();
        contentStream.setFont(fontDatos, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(producto != null ? producto : "");
        contentStream.endText();
        xPos += scaledWidths[2];

        // Columna 4: Descripción con 2 líneas automáticas
        String descripcion = detalle.getDescripcion() != null ? detalle.getDescripcion() : "";
        String linea1 = "";
        String linea2 = "";

        int maxLength = 18; // Ajusta según el ancho de tu columna

        if (descripcion.length() > maxLength) {
            // Buscar el último espacio antes del límite para cortar por palabras
            int lastSpace = descripcion.lastIndexOf(" ", maxLength);
            if (lastSpace > 0) {
                linea1 = descripcion.substring(0, lastSpace);
                linea2 = descripcion.substring(lastSpace + 1, Math.min(lastSpace + 1 + maxLength, descripcion.length()));
            } else {
                // No hay espacio, cortar forzosamente
                linea1 = descripcion.substring(0, maxLength);
                linea2 = descripcion.substring(maxLength, Math.min(maxLength * 2, descripcion.length()));
            }
        } else {
            // Si es corta, toda en la primera línea
            linea1 = descripcion;
        }

        contentStream.beginText();
        contentStream.setFont(fontDatos, 9); // Reducido a 9 para que quepa mejor
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 3); // Ajustado para centrar
        contentStream.showText(linea1);
        contentStream.endText();

        if (!linea2.isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15); // 10 unidades más abajo
            contentStream.showText(linea2);
            contentStream.endText();
        }

        xPos += scaledWidths[3];

        // Columna 5: Cantidad
        contentStream.beginText();
        contentStream.setFont(fontDatos, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(detalle.getCantidad() != null ? detalle.getCantidad() : "");
        contentStream.endText();
        xPos += scaledWidths[4];

        // Columna 6: Precio Unitario
        contentStream.beginText();
        contentStream.setFont(fontDatos, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : "");
        contentStream.endText();
        xPos += scaledWidths[5];

        // Columna 7: Precio Total - en rojo (igual que en el original)
        contentStream.setNonStrokingColor(PDFCommons.COLOR_TOTALES[0], PDFCommons.COLOR_TOTALES[1], PDFCommons.COLOR_TOTALES[2]);
        contentStream.beginText();
        contentStream.setFont(fontCabecera, 10);
        contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
        contentStream.showText(detalle.getPrecioTotal() != null ? detalle.getPrecioTotal() : "");
        contentStream.endText();

        // Restaurar color negro para el siguiente texto
        contentStream.setNonStrokingColor(0, 0, 0);
    }

    @Override
    protected float dibujarInformacionGeneral(org.apache.pdfbox.pdmodel.PDPageContentStream contentStream,
                                              PDType1Font fontSubtitulo,
                                              PDType1Font fontNormal,
                                              float pageWidth, float currentY,
                                              List<DetalleEntradaPDF> detalles,
                                              String nombreDestinatario,
                                              String comentario) throws IOException {

        // Título de sección
        contentStream.setNonStrokingColor(PDFCommons.COLOR_SUBTITULOS[0], PDFCommons.COLOR_SUBTITULOS[1], PDFCommons.COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 25;

        // Calcular totales
        int totalProductos = detalles.size();
        int totalCantidad = 0;
        float totalPrecio = 0;

        for (DetalleEntradaPDF detalle : detalles) {
            try {
                if (detalle.getCantidad() != null && !detalle.getCantidad().isEmpty()) {
                    totalCantidad += Integer.parseInt(detalle.getCantidad());
                }
                if (detalle.getPrecioTotal() != null && !detalle.getPrecioTotal().isEmpty()) {
                    String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                    totalPrecio += Float.parseFloat(precioStr);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear número: " + e.getMessage());
            }
        }

        // Columna 1 (izquierda) - información básica
        float col1X = PDFCommons.MARGIN + 10;
        contentStream.setNonStrokingColor(0, 0, 0); // Negro

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Fecha
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(PDFCommons.DATE_FORMATTER));
        contentStream.endText();

        // 2. Hora
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - PDFCommons.LINE_HEIGHT);
        contentStream.showText("Hora: " + ahora.format(PDFCommons.TIME_FORMATTER));
        contentStream.endText();

        // 3. Remitente - usar el primer detalle para obtener la sucursal
        String quienEnvia = !detalles.isEmpty() ? detalles.get(0).getNombreSucursal() : nombreDestinatario;
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (2 * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Remitente: " + quienEnvia);
        contentStream.endText();

        // 4. Destino
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (3 * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Destino: Almacén");
        contentStream.endText();

        // 5. Total de productos
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (4 * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Total de productos: " + totalProductos);
        contentStream.endText();

        // 6. Cantidad total
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (5 * PDFCommons.LINE_HEIGHT));
        contentStream.showText("Cantidad total: " + totalCantidad + " unidades");
        contentStream.endText();

        // 7. Comentario
        float yPosDespuesComentario = currentY - (6 * PDFCommons.LINE_HEIGHT);
        if (comentario != null && !comentario.trim().isEmpty()) {
            String comentarioMostrar = comentario;
            if (comentarioMostrar.length() > 50) {
                comentarioMostrar = comentarioMostrar.substring(0, 47) + "...";
            }
            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (6 * PDFCommons.LINE_HEIGHT));
            contentStream.showText("Comentario: " + comentarioMostrar);
            contentStream.endText();
            yPosDespuesComentario = currentY - (7 * PDFCommons.LINE_HEIGHT);
        }

        String valorTotalTexto = "Valor total: $" + String.format("%,.2f", totalPrecio);
        float textoAncho = fontSubtitulo.getStringWidth(valorTotalTexto) / 1000 * 12;
        float col2X = pageWidth - PDFCommons.MARGIN - textoAncho - 20;

        contentStream.setNonStrokingColor(PDFCommons.COLOR_TOTALES[0], PDFCommons.COLOR_TOTALES[1], PDFCommons.COLOR_TOTALES[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 12);
        contentStream.newLineAtOffset(col2X, currentY - 5);
        contentStream.showText(valorTotalTexto);
        contentStream.endText();

        // Ajustar el retorno
        float yFinal = comentario != null && !comentario.trim().isEmpty()
                ? currentY - 120  // Con comentario
                : currentY - 110; // Sin comentario

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

    // Métodos estáticos para mantener la compatibilidad con el código existente
    public static void exportarReporte(String claveEntrada,
                                       List<model.DetalleEntrada> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
                                       Window owner,
                                       String comentario) {
        if (detalles == null || detalles.isEmpty()) {
            PDFCommons.mostrarError("No hay detalles para exportar.");
            return;
        }

        // Obtener el nombre de la sucursal del primer detalle
        String quienEnvia = "";
        if (!detalles.isEmpty()) {
            try {
                quienEnvia = detalles.get(0).getNombreSucursal();
                if (quienEnvia == null || quienEnvia.isEmpty()) {
                    quienEnvia = "No especificado";
                }
            } catch (Exception e) {
                quienEnvia = "No especificado";
            }
        }

        ReporteTraspasoExporter exporter = new ReporteTraspasoExporter(ubicacionesPorProducto);
        exporter.exportar(claveEntrada, quienEnvia, comentario, detalles, owner);
    }

    public static void exportarReporte(String claveEntrada,
                                       List<model.DetalleEntrada> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
                                       Window owner) {
        exportarReporte(claveEntrada, detalles, ubicacionesPorProducto, owner, "");
    }

}