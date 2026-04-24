package Compartido.exportar;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteTraspasoSalidaExporter {

    // Formatters
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colores (mismo esquema que traspaso entrada)
    private static final float[] COLOR_BANDA_SUPERIOR = {0.5686f, 0.8314f, 0.5216f}; // Verde #91d485
    private static final float[] COLOR_SUBTITULOS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333
    private static final float[] COLOR_PRODUCTOS = {0.0078f, 0.4549f, 0.7451f}; // Azul #0274be
    private static final float[] COLOR_TOTALES = {0.8510f, 0.3255f, 0.3098f}; // Rojo #d9534f
    private static final float[] COLOR_ENCABEZADO_TABLA = {0.95f, 0.95f, 0.95f};  // Gris claro
    private static final float[] COLOR_FILA_PAR = {0.98f, 0.98f, 0.98f};  // Gris muy claro
    private static final float[] COLOR_FILA_IMPAR = {1.0f, 1.0f, 1.0f};   // Blanco

    // Constantes de diseño
    private static final float MARGIN = 40f;
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_SPACING = 20f;
    private static final float CELL_PADDING = 5f;
    private static final float MIN_Y = MARGIN + 50f; // Margen inferior para contenido + espacio para pie

    // Clase para representar los detalles de salida para el PDF
    public static class DetalleSalidaPDF {
        private String claveProducto;
        private String producto;
        private String descripcion;
        private int cantidad;
        private String presentacion;
        private int factor;
        private String precioUnitario;
        private String precioIva;
        private String precioBruto;
        private String precioTotal;
        private String ubicacionResumen;
        private List<UbicacionCompra> ubicaciones;

        public DetalleSalidaPDF(String claveProducto, String producto, String descripcion,
                                int cantidad, String presentacion, int factor,
                                String precioUnitario, String precioIva, String precioBruto,
                                String precioTotal, String ubicacionResumen,
                                List<UbicacionCompra> ubicaciones) {
            this.claveProducto = claveProducto;
            this.producto = producto;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.precioUnitario = precioUnitario;
            this.precioIva = precioIva;
            this.precioBruto = precioBruto;
            this.precioTotal = precioTotal;
            this.ubicacionResumen = ubicacionResumen;
            this.ubicaciones = ubicaciones != null ? new ArrayList<>(ubicaciones) : new ArrayList<>();
        }

        // Getters
        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getDescripcion() { return descripcion; }
        public int getCantidad() { return cantidad; }
        public String getPresentacion() { return presentacion; }
        public int getFactor() { return factor; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioIva() { return precioIva; }
        public String getPrecioBruto() { return precioBruto; }
        public String getPrecioTotal() { return precioTotal; }
        public String getUbicacionResumen() { return ubicacionResumen; }
        public List<UbicacionCompra> getUbicaciones() { return new ArrayList<>(ubicaciones); }
    }

    public static void exportarReporte(String claveSalida,
                                       String nombreSucursalDestino,
                                       String comentario,
                                       List<traspasoSalida> itemsTraspaso,
                                       Window owner) {
        if (itemsTraspaso == null || itemsTraspaso.isEmpty()) {
            mostrarError("No hay productos para exportar en el traspaso.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el reporte");
        File carpeta = owner != null ? dirChooser.showDialog(owner) : dirChooser.showDialog(null);

        if (carpeta == null) {
            return;
        }

        String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
        File archivo = new File(carpeta, "Reporte_Traspaso_Salida_" + claveSalida + "_" + fechaHora + ".pdf");

        try {
            List<DetalleSalidaPDF> detalles = convertirItemsADetallesPDF(itemsTraspaso);
            Map<String, List<UbicacionCompra>> ubicacionesPorProducto = extraerUbicacionesPorProducto(itemsTraspaso);

            generarReporte(archivo, claveSalida, nombreSucursalDestino, comentario, detalles, ubicacionesPorProducto);

            mostrarExito("Reporte de traspaso de salida generado exitosamente\n\n" +
                    "Archivo: Reporte_Traspaso_Salida_" + claveSalida + "_" + fechaHora + ".pdf\n" +
                    "Ubicación: " + archivo.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al generar el reporte:\n" + e.getMessage());
        }
    }

    private static List<DetalleSalidaPDF> convertirItemsADetallesPDF(List<traspasoSalida> items) {
        List<DetalleSalidaPDF> detalles = new ArrayList<>();
        for (traspasoSalida item : items) {
            DetalleSalidaPDF detalle = new DetalleSalidaPDF(
                    item.getClaveProducto(),
                    item.getProducto(),
                    item.getDescripcion(),
                    item.getCantidad(),
                    item.getPresentacion(),
                    item.getFactor(),
                    item.getPrecioEntrada(),
                    item.getPrecioIva(),
                    item.getPrecioBruto(),
                    item.getPrecioTotal(),
                    item.getUbicacionResumen(),
                    item.getUbicaciones()
            );
            detalles.add(detalle);
        }
        return detalles;
    }

    private static Map<String, List<UbicacionCompra>> extraerUbicacionesPorProducto(List<traspasoSalida> items) {
        Map<String, List<UbicacionCompra>> mapa = new HashMap<>();
        for (traspasoSalida item : items) {
            if (item.getUbicaciones() != null && !item.getUbicaciones().isEmpty()) {
                mapa.put(item.getClaveProducto(), item.getUbicaciones());
            }
        }
        return mapa;
    }

    private static void generarReporte(File archivo,
                                       String claveSalida,
                                       String nombreSucursalDestino,
                                       String comentario,
                                       List<DetalleSalidaPDF> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            int paginaActual = 1;
            PDPage paginaActualObj = null;
            PDPageContentStream contentStreamActual = null;
            float currentY = 0;
            float pageWidth = 0;
            float pageHeight = 0;

            // Variables de estado para control de flujo
            boolean primeraPagina = true;
            int indiceProducto = 0;
            boolean tablaCompleta = false;
            boolean hayUbicaciones = !ubicacionesPorProducto.isEmpty();
            boolean ubicacionesPendientes = hayUbicaciones;

            // Estado para ubicaciones paginadas
            int indiceProductoUbicacion = 0;
            int indiceUbicacionDentroProducto = 0;
            String productoActualUbicacion = null;
            boolean primeraPaginaUbicaciones = true;

            // Variables de control para eliminar última página vacía
            boolean todoCompleto = false;
            boolean paginaCreadaEnEstaIteracion = false;

            while (!todoCompleto) {
                paginaCreadaEnEstaIteracion = false;

                // Si necesitamos una nueva página
                if (paginaActualObj == null) {
                    paginaActualObj = new PDPage(PDRectangle.LETTER);
                    document.addPage(paginaActualObj);
                    paginaCreadaEnEstaIteracion = true;

                    // Cerrar el content stream anterior si existe
                    if (contentStreamActual != null) {
                        contentStreamActual.close();
                    }

                    contentStreamActual = new PDPageContentStream(document, paginaActualObj);
                    pageWidth = paginaActualObj.getMediaBox().getWidth();
                    pageHeight = paginaActualObj.getMediaBox().getHeight();
                    currentY = pageHeight - MARGIN;

                    // Dibujar encabezado en cada nueva página
                    PDType1Font fontTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    currentY = dibujarEncabezado(contentStreamActual, fontTitulo, pageWidth, currentY, claveSalida, paginaActual);

                    // Solo en la primera página: información general
                    if (primeraPagina) {
                        PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                        PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                        currentY = dibujarInformacionGeneral(contentStreamActual, fontSubtitulo, fontNormal,
                                pageWidth, currentY, detalles, nombreSucursalDestino, comentario);

                        // Título de la tabla en primera página
                        currentY = dibujarSeccionTabla(contentStreamActual, fontSubtitulo, pageWidth, currentY, "DETALLES DE PRODUCTOS");
                        primeraPagina = false;
                    }
                }

                // Si todavía tenemos productos en la tabla por dibujar
                if (!tablaCompleta) {
                    PDType1Font fontTablaCabecera = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font fontTablaDatos = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                    // Si no es la primera página y estamos empezando una nueva página de tabla
                    if (indiceProducto > 0 && currentY == pageHeight - MARGIN - 80) {
                        currentY = dibujarSeccionTabla(contentStreamActual, fontSubtitulo, pageWidth, currentY, "DETALLES DE PRODUCTOS (CONTINUACIÓN)");
                    }

                    PaginacionResultado resultadoTabla = dibujarTablaDetallesPaginada(
                            contentStreamActual, fontTablaCabecera, fontTablaDatos,
                            pageWidth, currentY, detalles, indiceProducto,
                            indiceProducto == 0); // dibujarCabecera solo si es el inicio

                    indiceProducto = resultadoTabla.getIndiceSiguiente();
                    currentY = resultadoTabla.getCurrentY();

                    // Verificar si terminamos la tabla
                    if (indiceProducto >= detalles.size()) {
                        tablaCompleta = true;

                        // Verificar si hay espacio suficiente para empezar las ubicaciones en la misma página
                        if (hayUbicaciones) {
                            float espacioNecesarioParaUbicaciones = 100; // Título + espacio para un producto
                            if (currentY - espacioNecesarioParaUbicaciones < MIN_Y) {
                                // No hay espacio, necesitamos nueva página
                                dibujarPiePagina(contentStreamActual,
                                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                        pageWidth, paginaActual);
                                contentStreamActual.close();
                                paginaActualObj = null;
                                paginaActual++;
                                continue;
                            }
                        } else {
                            // No hay ubicaciones, terminar inmediatamente
                            ubicacionesPendientes = false;
                            dibujarPiePagina(contentStreamActual,
                                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                    pageWidth, paginaActual);
                            contentStreamActual.close();
                            todoCompleto = true;
                            continue;
                        }
                    }
                }

                // Si la tabla está completa y hay ubicaciones pendientes
                if (tablaCompleta && ubicacionesPendientes) {
                    // Dibujar título de ubicaciones solo si es la primera página de ubicaciones
                    if (primeraPaginaUbicaciones) {
                        // Agregar espacio extra antes del título de ubicaciones
                        currentY -= 15; // ESPACIO EXTRA AQUÍ

                        // Verificar si tenemos espacio para al menos el título
                        float espacioMinimoTitulo = 50;
                        if (currentY - espacioMinimoTitulo < MIN_Y) {
                            // No hay espacio, necesitamos nueva página
                            dibujarPiePagina(contentStreamActual,
                                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                    pageWidth, paginaActual);
                            contentStreamActual.close();
                            paginaActualObj = null;
                            paginaActual++;
                            continue;
                        }

                        // Dibujar título de ubicaciones
                        PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                        currentY = dibujarSeccionTablaUbicaciones(contentStreamActual, fontSubtitulo, pageWidth, currentY, "UBICACIONES DE ORIGEN");
                        primeraPaginaUbicaciones = false;
                    }

                    // Dibujar ubicaciones (pueden continuar en múltiples páginas)
                    PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    PDType1Font fontNormalBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                    PaginacionResultadoUbicaciones resultadoUbicaciones = dibujarUbicacionesPaginadasMejorado(
                            contentStreamActual, fontSubtitulo, fontNormal, fontNormalBold,
                            pageWidth, currentY, ubicacionesPorProducto, detalles,
                            indiceProductoUbicacion, indiceUbicacionDentroProducto,
                            productoActualUbicacion, paginaActual);

                    // Actualizar estado
                    indiceProductoUbicacion = resultadoUbicaciones.getIndiceProductoSiguiente();
                    indiceUbicacionDentroProducto = resultadoUbicaciones.getIndiceUbicacionSiguiente();
                    productoActualUbicacion = resultadoUbicaciones.getProductoActual();
                    currentY = resultadoUbicaciones.getCurrentY();

                    // Verificar si terminamos todas las ubicaciones
                    boolean ubicacionesCompletas = resultadoUbicaciones.isCompleta();

                    if (ubicacionesCompletas) {
                        ubicacionesPendientes = false;
                        // Dibujar pie de página y terminar
                        dibujarPiePagina(contentStreamActual,
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                pageWidth, paginaActual);
                        contentStreamActual.close();
                        todoCompleto = true;
                        continue;
                    } else {
                        // Las ubicaciones no caben completas, necesitamos nueva página
                        dibujarPiePagina(contentStreamActual,
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                pageWidth, paginaActual);
                        contentStreamActual.close();
                        paginaActualObj = null;
                        paginaActual++;
                        primeraPaginaUbicaciones = false;
                        continue;
                    }
                }

                // Si llegamos al final de la página pero aún tenemos contenido pendiente
                if (currentY < MIN_Y && (!tablaCompleta || ubicacionesPendientes)) {
                    // Dibujar pie de página en la página actual
                    PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    dibujarPiePagina(contentStreamActual, fontNormal, pageWidth, paginaActual);
                    contentStreamActual.close();

                    // Preparar para nueva página
                    paginaActualObj = null;
                    paginaActual++;
                    continue;
                }
            }

            // Cerrar el último content stream si aún está abierto
            if (contentStreamActual != null) {
                contentStreamActual.close();
            }

            // Eliminar última página vacía si fue creada en la última iteración
            int totalPaginas = document.getNumberOfPages();
            if (totalPaginas > 0) {
                if (paginaCreadaEnEstaIteracion && todoCompleto) {
                    // Eliminar la última página
                    document.removePage(totalPaginas - 1);
                }
            }

            document.save(archivo);
        }
    }

    // Clase auxiliar para manejar resultados de paginación de tabla
    private static class PaginacionResultado {
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

    // Clase auxiliar para manejar resultados de paginación de ubicaciones
    private static class PaginacionResultadoUbicaciones {
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

    private static float dibujarEncabezado(PDPageContentStream contentStream,
                                           PDType1Font fontTitulo,
                                           float pageWidth,
                                           float currentY,
                                           String claveSalida,
                                           int numeroPagina) throws IOException {
        // Fondo del encabezado
        contentStream.setNonStrokingColor(COLOR_BANDA_SUPERIOR[0], COLOR_BANDA_SUPERIOR[1], COLOR_BANDA_SUPERIOR[2]);
        contentStream.addRect(MARGIN, currentY - 60, pageWidth - 2 * MARGIN, 60);
        contentStream.fill();

        // Título
        contentStream.setNonStrokingColor(1, 1, 1); // Blanco
        contentStream.beginText();
        contentStream.setFont(fontTitulo, 20);
        contentStream.newLineAtOffset(MARGIN + 10, currentY - 30);
        contentStream.showText("REPORTE DE TRASPASO DE SALIDA");
        contentStream.endText();

        // Subtítulo con número de página
        contentStream.beginText();
        contentStream.setFont(fontTitulo, 14);
        contentStream.newLineAtOffset(MARGIN + 10, currentY - 50);
        contentStream.showText("Clave: " + claveSalida + " - Página " + numeroPagina);
        contentStream.endText();

        return currentY - 80;
    }

    private static float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                                   PDType1Font fontSubtitulo,
                                                   PDType1Font fontNormal,
                                                   float pageWidth,
                                                   float currentY,
                                                   List<DetalleSalidaPDF> detalles,
                                                   String nombreSucursalDestino,
                                                   String comentario) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();

        // Título de sección
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 25;

        // Calcular totales
        int totalProductos = detalles.size();
        int totalCantidad = 0;
        BigDecimal totalPrecio = BigDecimal.ZERO;

        for (DetalleSalidaPDF detalle : detalles) {
            totalCantidad += detalle.getCantidad();
            try {
                String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                totalPrecio = totalPrecio.add(new BigDecimal(precioStr));
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear número: " + e.getMessage());
            }
        }

        // Columna 1 (izquierda) - información básica
        float col1X = MARGIN + 10;
        contentStream.setNonStrokingColor(0, 0, 0);

        // 1. Fecha
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(DATE_FORMATTER));
        contentStream.endText();

        // 2. Hora
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - LINE_HEIGHT);
        contentStream.showText("Hora: " + ahora.format(TIME_FORMATTER));
        contentStream.endText();

        // 3. Sucursal remitente (Almacén)
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (2 * LINE_HEIGHT));
        contentStream.showText("Remitente: Almacén");
        contentStream.endText();

        // 4. Sucursal destino
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (3 * LINE_HEIGHT));
        contentStream.showText("Destino: " + (nombreSucursalDestino != null ? nombreSucursalDestino : "No especificado"));
        contentStream.endText();

        // Ajustar posición Y basado en si hay comentario
        float yPosDespuesComentario = currentY - (4 * LINE_HEIGHT);

        // 5. Comentario (si existe)
        if (comentario != null && !comentario.trim().isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(fontNormal, 11);
            contentStream.newLineAtOffset(col1X, currentY - (4 * LINE_HEIGHT));
            contentStream.showText("Comentario: " + comentario);
            contentStream.endText();
            yPosDespuesComentario = currentY - (5 * LINE_HEIGHT);
        }

        // 6. Total de productos
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, yPosDespuesComentario);
        contentStream.showText("Total de productos: " + totalProductos);
        contentStream.endText();

        // 7. Cantidad total
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, yPosDespuesComentario - LINE_HEIGHT);
        contentStream.showText("Cantidad total: " + totalCantidad + " unidades");
        contentStream.endText();

        // Columna 2 (derecha) - valor total
        String valorTotalTexto = "Valor total: $" + String.format("%,.2f", totalPrecio);
        float textoAncho = fontSubtitulo.getStringWidth(valorTotalTexto) / 1000 * 12;

        float col2X = pageWidth - MARGIN - textoAncho - 20;

        contentStream.setNonStrokingColor(COLOR_TOTALES[0], COLOR_TOTALES[1], COLOR_TOTALES[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 12);
        contentStream.newLineAtOffset(col2X, currentY - 5);
        contentStream.showText(valorTotalTexto);
        contentStream.endText();

        // Ajustar el retorno
        float yFinal = comentario != null && !comentario.trim().isEmpty()
                ? currentY - 110  // Con comentario
                : currentY - 100; // Sin comentario

        return yFinal;
    }

    private static float dibujarSeccionTabla(PDPageContentStream contentStream,
                                             PDType1Font fontSubtitulo,
                                             float pageWidth,
                                             float currentY,
                                             String tituloSeccion) throws IOException {
        // Título de sección
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(tituloSeccion);
        contentStream.endText();

        return currentY - 25;
    }

    // Nuevo método para sección de ubicaciones con más espacio
    private static float dibujarSeccionTablaUbicaciones(PDPageContentStream contentStream,
                                                        PDType1Font fontSubtitulo,
                                                        float pageWidth,
                                                        float currentY,
                                                        String tituloSeccion) throws IOException {
        // ESPACIO EXTRA antes del título
        currentY -= 10;

        // Título de sección
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(tituloSeccion);
        contentStream.endText();

        return currentY - 30; // Más espacio después del título
    }

    private static PaginacionResultado dibujarTablaDetallesPaginada(PDPageContentStream contentStream,
                                                                    PDType1Font fontCabecera,
                                                                    PDType1Font fontDatos,
                                                                    float pageWidth,
                                                                    float currentY,
                                                                    List<DetalleSalidaPDF> detalles,
                                                                    int inicioIndice,
                                                                    boolean dibujarCabecera) throws IOException {
        // Definir anchos de columnas
        float[] columnWidths = {30, 80, 150, 120, 60, 60, 80, 80, 80};
        float tableWidth = pageWidth - 2 * MARGIN;
        float[] scaledWidths = escalarAnchosColumnas(columnWidths, tableWidth);

        // Cabecera de la tabla (si es necesario)
        if (dibujarCabecera && inicioIndice == 0) {
            String[] headers = {"#", "Clave", "Producto", "Descripción", "Cant.", "Present.", "Factor", "P. Unit.", "P. Total"};

            // Dibujar fondo de cabecera
            contentStream.setNonStrokingColor(COLOR_ENCABEZADO_TABLA[0], COLOR_ENCABEZADO_TABLA[1], COLOR_ENCABEZADO_TABLA[2]);
            contentStream.addRect(MARGIN, currentY - 20, tableWidth, 20);
            contentStream.fill();

            // Dibujar texto de cabecera
            contentStream.setNonStrokingColor(0, 0, 0);
            float xPos = MARGIN;
            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.setFont(fontCabecera, 9);
                contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
                contentStream.showText(headers[i]);
                contentStream.endText();
                xPos += scaledWidths[i];
            }

            currentY -= 25;
        }

        // Dibujar filas de datos
        int filaNum = inicioIndice + 1;
        int i = inicioIndice;

        for (; i < detalles.size(); i++) {
            // Verificar si hay espacio para otra fila (incluyendo pie de página)
            if (currentY - 25 < MIN_Y) {
                break;
            }

            DetalleSalidaPDF detalle = detalles.get(i);

            // Alternar colores de fila
            if (filaNum % 2 == 0) {
                contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
            } else {
                contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
            }
            contentStream.addRect(MARGIN, currentY - 20, tableWidth, 20);
            contentStream.fill();

            // Dibujar datos
            contentStream.setNonStrokingColor(0, 0, 0);
            float xPos = MARGIN;

            // Columna 1: Número
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(String.valueOf(filaNum));
            contentStream.endText();
            xPos += scaledWidths[0];

            // Columna 2: Clave
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(truncarTexto(detalle.getClaveProducto(), 12));
            contentStream.endText();
            xPos += scaledWidths[1];

            // Columna 3: Producto
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(truncarTexto(detalle.getProducto(), 18));
            contentStream.endText();
            xPos += scaledWidths[2];

            // Columna 4: Descripción con 2 líneas automáticas
            String descripcion = detalle.getDescripcion();
            String linea1 = "";
            String linea2 = "";

            if (descripcion.length() > 16) {
                // Buscar el último espacio antes del carácter 16 para cortar por palabras
                int lastSpace = descripcion.lastIndexOf(" ", 16);
                if (lastSpace > 0) {
                    linea1 = descripcion.substring(0, lastSpace);
                    linea2 = descripcion.substring(lastSpace + 1, Math.min(lastSpace + 1 + 16, descripcion.length()));
                } else {
                    // No hay espacio, cortar forzosamente
                    linea1 = descripcion.substring(0, 16);
                    linea2 = descripcion.substring(16, Math.min(32, descripcion.length()));
                }
            } else {
                // Si es corta, toda en la primera línea
                linea1 = descripcion;
            }

            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 12); // Ajustado para centrar mejor
            contentStream.showText(linea1);
            contentStream.endText();

            if (!linea2.isEmpty()) {
                contentStream.beginText();
                contentStream.setFont(fontDatos, 9);
                contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 22); // 10 unidades más abajo
                contentStream.showText(linea2);
                contentStream.endText();
            }

            xPos += scaledWidths[3];

            // Columna 5: Cantidad
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(String.valueOf(detalle.getCantidad()));
            contentStream.endText();
            xPos += scaledWidths[4];

            // Columna 6: Presentación
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(truncarTexto(detalle.getPresentacion(), 8));
            contentStream.endText();
            xPos += scaledWidths[5];

            // Columna 7: Factor
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(String.valueOf(detalle.getFactor()));
            contentStream.endText();
            xPos += scaledWidths[6];

            // Columna 8: Precio Unitario
            contentStream.beginText();
            contentStream.setFont(fontDatos, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(truncarTexto(detalle.getPrecioUnitario(), 10));
            contentStream.endText();
            xPos += scaledWidths[7];

            // Columna 9: Precio Total
            contentStream.setNonStrokingColor(COLOR_TOTALES[0], COLOR_TOTALES[1], COLOR_TOTALES[2]);
            contentStream.beginText();
            contentStream.setFont(fontCabecera, 9);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 15);
            contentStream.showText(truncarTexto(detalle.getPrecioTotal(), 12));
            contentStream.endText();

            currentY -= 25;
            filaNum++;
        }

        // Dibujar bordes de la tabla solo si se dibujaron filas
        if (i > inicioIndice) {
            contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
            contentStream.setLineWidth(0.5f);
            float alturaTabla = (i - inicioIndice) * 25;
            if (dibujarCabecera && inicioIndice == 0) {
                alturaTabla += 25; // Incluir altura de cabecera
            }
            float yInicioTabla = currentY + alturaTabla + (dibujarCabecera && inicioIndice == 0 ? 0 : 25);
            contentStream.addRect(MARGIN, yInicioTabla - alturaTabla, tableWidth, alturaTabla);
            contentStream.stroke();
        }

        boolean completa = (i == detalles.size());
        return new PaginacionResultado(i, completa, currentY);
    }

    private static PaginacionResultadoUbicaciones dibujarUbicacionesPaginadasMejorado(
            PDPageContentStream contentStream,
            PDType1Font fontSubtitulo,
            PDType1Font fontNormal,
            PDType1Font fontNormalBold,
            float pageWidth,
            float currentY,
            Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
            List<DetalleSalidaPDF> detalles,
            int indiceProductoInicio,
            int indiceUbicacionInicio,
            String productoContinuacion,
            int paginaActual) throws IOException {

        int indiceProductoActual = indiceProductoInicio;
        int indiceUbicacionActual = indiceUbicacionInicio;
        String productoActual = productoContinuacion;
        boolean continuacionProducto = (productoContinuacion != null);

        // Si estamos continuando un producto específico
        if (continuacionProducto) {
            // Buscar el producto que estamos continuando
            for (int i = indiceProductoActual; i < detalles.size(); i++) {
                DetalleSalidaPDF detalle = detalles.get(i);
                if (detalle.getProducto().equals(productoContinuacion)) {
                    indiceProductoActual = i;
                    break;
                }
            }
        }

        // Recorrer productos
        for (int i = indiceProductoActual; i < detalles.size(); i++) {
            DetalleSalidaPDF detalle = detalles.get(i);
            List<UbicacionCompra> ubicaciones = ubicacionesPorProducto.get(detalle.getClaveProducto());

            if (ubicaciones != null && !ubicaciones.isEmpty()) {
                // Si estamos continuando este producto desde una ubicación específica
                boolean esMismoProductoContinuacion = continuacionProducto &&
                        detalle.getProducto().equals(productoContinuacion);

                // Calcular espacio necesario para el encabezado del producto
                float espacioEncabezadoProducto = 20; // Nombre del producto
                float espacioEncabezadoTabla = 25;    // Cabecera de tabla de ubicaciones

                // Verificar si hay espacio para al menos el encabezado del producto
                if (currentY - espacioEncabezadoProducto - espacioEncabezadoTabla < MIN_Y) {
                    // No hay espacio ni para empezar este producto
                    return new PaginacionResultadoUbicaciones(
                            i, 0, false, currentY, productoActual);
                }

                // Dibujar nombre del producto (con indicador de continuación si aplica)
                contentStream.setNonStrokingColor(COLOR_PRODUCTOS[0], COLOR_PRODUCTOS[1], COLOR_PRODUCTOS[2]);
                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 11);
                contentStream.newLineAtOffset(MARGIN + 10, currentY);

                String textoProducto = "• " + detalle.getProducto() + " (" + detalle.getClaveProducto() + ")";
                if (esMismoProductoContinuacion && indiceUbicacionActual > 0) {
                    textoProducto += " (CONTINUACIÓN)";
                }

                contentStream.showText(textoProducto);
                contentStream.endText();

                currentY -= 20;
                productoActual = detalle.getProducto();

                // Dibujar encabezado de tabla de ubicaciones
                float tablaUbicacionesWidth = pageWidth - 2 * MARGIN - 40;
                float[] columnWidthsUbic = {200, 100};
                float[] scaledUbicWidths = escalarAnchosColumnas(columnWidthsUbic, tablaUbicacionesWidth);

                // Fondo cabecera
                contentStream.setNonStrokingColor(COLOR_ENCABEZADO_TABLA[0], COLOR_ENCABEZADO_TABLA[1], COLOR_ENCABEZADO_TABLA[2]);
                contentStream.addRect(MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                contentStream.fill();

                // Texto cabecera
                contentStream.setNonStrokingColor(0, 0, 0);
                float xPosUbic = MARGIN + 20;

                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 9);
                contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                contentStream.showText("Ubicación");
                contentStream.endText();
                xPosUbic += scaledUbicWidths[0];

                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 9);
                contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                contentStream.showText("Cantidad");
                contentStream.endText();

                currentY -= 25;

                // Dibujar ubicaciones (puede ser desde un índice específico si es continuación)
                int inicioUbicaciones = esMismoProductoContinuacion ? indiceUbicacionActual : 0;
                int ubicacionesDibujadas = 0;

                for (int j = inicioUbicaciones; j < ubicaciones.size(); j++) {
                    // Verificar si hay espacio para otra fila
                    if (currentY - 25 < MIN_Y) {
                        // No hay espacio para más ubicaciones en esta página
                        // Devolver estado para continuar en siguiente página
                        boolean completadoProducto = (j == ubicaciones.size() - 1);
                        int siguienteIndiceProducto = completadoProducto ? i + 1 : i;
                        int siguienteIndiceUbicacion = completadoProducto ? 0 : j;

                        return new PaginacionResultadoUbicaciones(
                                siguienteIndiceProducto, siguienteIndiceUbicacion,
                                false, currentY, productoActual);
                    }

                    UbicacionCompra ubicacion = ubicaciones.get(j);

                    // Alternar colores
                    if ((j + 1) % 2 == 0) {
                        contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
                    } else {
                        contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
                    }
                    contentStream.addRect(MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                    contentStream.fill();

                    // Datos
                    contentStream.setNonStrokingColor(0, 0, 0);
                    xPosUbic = MARGIN + 20;

                    contentStream.beginText();
                    contentStream.setFont(fontNormal, 9);
                    contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                    contentStream.showText(ubicacion.getUbicacion());
                    contentStream.endText();
                    xPosUbic += scaledUbicWidths[0];

                    contentStream.beginText();
                    contentStream.setFont(fontNormal, 9);
                    contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                    contentStream.showText(String.valueOf(ubicacion.getCantidad()));
                    contentStream.endText();

                    currentY -= 25;
                    ubicacionesDibujadas++;
                    indiceUbicacionActual = j + 1;
                }

                // Espacio después del producto
                currentY -= 10;

                // Reiniciar índice de ubicación para el siguiente producto
                indiceUbicacionActual = 0;
                continuacionProducto = false;
                productoContinuacion = null;

                // Si terminamos todas las ubicaciones de este producto
                if (ubicacionesDibujadas == ubicaciones.size()) {
                    // Continuar con el siguiente producto
                    continue;
                }
            } else {
                // Este producto no tiene ubicaciones, pasar al siguiente
                indiceProductoActual = i + 1;
            }
        }

        // Si llegamos aquí, hemos procesado todos los productos
        boolean completado = (indiceProductoActual >= detalles.size());
        return new PaginacionResultadoUbicaciones(
                detalles.size(), 0, completado, currentY, null);
    }

    private static void dibujarPiePagina(PDPageContentStream contentStream,
                                         PDType1Font fontNormal,
                                         float pageWidth,
                                         int paginaActual) throws IOException {
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
        contentStream.showText("Sistema de Gestión de Inventarios GREEP - Reporte de traspaso de salida generado automáticamente");
        contentStream.endText();

        // Número de página
        String textoPagina = "Página " + paginaActual;
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(pageWidth - MARGIN - 50, MARGIN + 5);
        contentStream.showText(textoPagina);
        contentStream.endText();
    }

    private static float[] escalarAnchosColumnas(float[] widths, float totalWidth) {
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

    private static String truncarTexto(String texto, int maxLength) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= maxLength) {
            return texto;
        }
        return texto.substring(0, maxLength - 3) + "...";
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Reporte generado");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al generar reporte");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }
}