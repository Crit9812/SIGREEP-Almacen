package Compartido.exportar;

import Operaciones.compra.model.UbicacionCompra;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public abstract class AbstractPDFExporter<T, D> {

    // Métodos abstractos que las subclases deben implementar
    protected abstract TipoOperacion getTipoOperacion();
    protected abstract List<D> convertirItemsADetallesPDF(List<T> items);
    protected abstract Map<String, List<UbicacionCompra>> extraerUbicacionesPorProducto(List<T> items);
    protected abstract String[] getHeaders();
    protected abstract float[] getColumnWidths();
    protected abstract void dibujarFilaDetalle(PDPageContentStream contentStream,
                                               PDType1Font fontDatos, PDType1Font fontCabecera,
                                               float currentY, float[] scaledWidths,
                                               D detalle, int filaNum, float xPos) throws IOException;
    protected abstract float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                                       PDType1Font fontSubtitulo, PDType1Font fontNormal,
                                                       float pageWidth, float currentY,
                                                       List<D> detalles, String nombreDestinatario,
                                                       String comentario) throws IOException;

    // Método principal de exportación (TODO reutilizable)
    public void exportar(String claveOperacion, String nombreDestinatario,
                         String comentario, List<T> items, Window owner) {
        if (items == null || items.isEmpty()) {
            PDFCommons.mostrarError("No hay productos para exportar en la " + getTipoOperacion().getTitulo().toLowerCase() + ".");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el reporte");
        File carpeta = owner != null ? dirChooser.showDialog(owner) : dirChooser.showDialog(null);

        if (carpeta == null) {
            return;
        }

        String fechaHora = LocalDateTime.now().format(PDFCommons.FILE_FORMATTER);
        File archivo = new File(carpeta,
                getTipoOperacion().getPrefijoArchivo() + "_" + claveOperacion + "_" + fechaHora + ".pdf");

        try {
            List<D> detalles = convertirItemsADetallesPDF(items);
            Map<String, List<UbicacionCompra>> ubicacionesPorProducto = extraerUbicacionesPorProducto(items);

            generarReporte(archivo, claveOperacion, nombreDestinatario,
                    comentario, detalles, ubicacionesPorProducto);

            PDFCommons.mostrarExito("Reporte de " + getTipoOperacion().getTitulo().toLowerCase() +
                    " generado exitosamente\n\n" +
                    "Archivo: " + getTipoOperacion().getPrefijoArchivo() + "_" +
                    claveOperacion + "_" + fechaHora + ".pdf\n" +
                    "Ubicación: " + archivo.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            PDFCommons.mostrarError("Error al generar el reporte:\n" + e.getMessage());
        }
    }

    // Método de generación de reporte (TODO reutilizable)
    protected void generarReporte(File archivo, String claveOperacion, String nombreDestinatario,
                                  String comentario, List<D> detalles,
                                  Map<String, List<UbicacionCompra>> ubicacionesPorProducto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            int paginaActual = 1;
            PDPage paginaActualObj = null;
            PDPageContentStream contentStreamActual = null;
            float currentY = 0;
            float pageWidth = 0;
            float pageHeight = 0;

            boolean primeraPagina = true;
            int indiceProducto = 0;
            boolean tablaCompleta = false;
            boolean hayUbicaciones = !ubicacionesPorProducto.isEmpty();
            boolean ubicacionesPendientes = hayUbicaciones;

            int indiceProductoUbicacion = 0;
            int indiceUbicacionDentroProducto = 0;
            String productoActualUbicacion = null;
            boolean primeraPaginaUbicaciones = true;

            boolean todoCompleto = false;
            boolean paginaCreadaEnEstaIteracion = false;

            while (!todoCompleto) {
                paginaCreadaEnEstaIteracion = false;

                if (paginaActualObj == null) {
                    paginaActualObj = new PDPage(PDRectangle.LETTER);
                    document.addPage(paginaActualObj);
                    paginaCreadaEnEstaIteracion = true;

                    if (contentStreamActual != null) {
                        contentStreamActual.close();
                    }

                    contentStreamActual = new PDPageContentStream(document, paginaActualObj);
                    pageWidth = paginaActualObj.getMediaBox().getWidth();
                    pageHeight = paginaActualObj.getMediaBox().getHeight();
                    currentY = pageHeight - PDFCommons.MARGIN;

                    PDType1Font fontTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    currentY = dibujarEncabezado(contentStreamActual, fontTitulo, pageWidth, currentY,
                            claveOperacion, paginaActual);

                    if (primeraPagina) {
                        PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                        PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                        currentY = dibujarInformacionGeneral(contentStreamActual, fontSubtitulo, fontNormal,
                                pageWidth, currentY, detalles, nombreDestinatario, comentario);

                        currentY = dibujarSeccionTabla(contentStreamActual, fontSubtitulo, pageWidth, currentY,
                                "DETALLES DE PRODUCTOS");
                        primeraPagina = false;
                    }
                }

                if (!tablaCompleta) {
                    PDType1Font fontTablaCabecera = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font fontTablaDatos = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                    if (indiceProducto > 0 && currentY == pageHeight - PDFCommons.MARGIN - 80) {
                        currentY = dibujarSeccionTabla(contentStreamActual, fontSubtitulo, pageWidth, currentY,
                                "DETALLES DE PRODUCTOS (CONTINUACIÓN)");
                    }

                    PDFCommons.PaginacionResultado resultadoTabla = dibujarTablaDetallesPaginada(
                            contentStreamActual, fontTablaCabecera, fontTablaDatos,
                            pageWidth, currentY, detalles, indiceProducto,
                            indiceProducto == 0);

                    indiceProducto = resultadoTabla.getIndiceSiguiente();
                    currentY = resultadoTabla.getCurrentY();

                    if (indiceProducto >= detalles.size()) {
                        tablaCompleta = true;

                        if (hayUbicaciones) {
                            float espacioNecesarioParaUbicaciones = 100;
                            if (currentY - espacioNecesarioParaUbicaciones < PDFCommons.MIN_Y) {
                                dibujarPiePagina(contentStreamActual,
                                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                        pageWidth, paginaActual);
                                contentStreamActual.close();
                                paginaActualObj = null;
                                paginaActual++;
                                continue;
                            }
                        } else {
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

                if (tablaCompleta && ubicacionesPendientes) {
                    if (primeraPaginaUbicaciones) {
                        currentY -= 15;

                        float espacioMinimoTitulo = 50;
                        if (currentY - espacioMinimoTitulo < PDFCommons.MIN_Y) {
                            dibujarPiePagina(contentStreamActual,
                                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                    pageWidth, paginaActual);
                            contentStreamActual.close();
                            paginaActualObj = null;
                            paginaActual++;
                            continue;
                        }

                        PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                        currentY = dibujarSeccionTablaUbicaciones(contentStreamActual, fontSubtitulo, pageWidth, currentY, "UBICACIONES DE ORIGEN");
                        primeraPaginaUbicaciones = false;
                    }

                    PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    PDType1Font fontNormalBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                    PDFCommons.PaginacionResultadoUbicaciones resultadoUbicaciones = dibujarUbicacionesPaginadasMejorado(
                            contentStreamActual, fontSubtitulo, fontNormal, fontNormalBold,
                            pageWidth, currentY, ubicacionesPorProducto, detalles,
                            indiceProductoUbicacion, indiceUbicacionDentroProducto,
                            productoActualUbicacion, paginaActual);

                    indiceProductoUbicacion = resultadoUbicaciones.getIndiceProductoSiguiente();
                    indiceUbicacionDentroProducto = resultadoUbicaciones.getIndiceUbicacionSiguiente();
                    productoActualUbicacion = resultadoUbicaciones.getProductoActual();
                    currentY = resultadoUbicaciones.getCurrentY();

                    boolean ubicacionesCompletas = resultadoUbicaciones.isCompleta();

                    if (ubicacionesCompletas) {
                        ubicacionesPendientes = false;
                        dibujarPiePagina(contentStreamActual,
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                pageWidth, paginaActual);
                        contentStreamActual.close();
                        todoCompleto = true;
                        continue;
                    } else {
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

                if (currentY < PDFCommons.MIN_Y && (!tablaCompleta || ubicacionesPendientes)) {
                    PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    dibujarPiePagina(contentStreamActual, fontNormal, pageWidth, paginaActual);
                    contentStreamActual.close();

                    paginaActualObj = null;
                    paginaActual++;
                    continue;
                }
            }

            if (contentStreamActual != null) {
                contentStreamActual.close();
            }

            int totalPaginas = document.getNumberOfPages();
            if (totalPaginas > 0) {
                if (paginaCreadaEnEstaIteracion && todoCompleto) {
                    document.removePage(totalPaginas - 1);
                }
            }

            document.save(archivo);
        }
    }

    // Métodos de dibujo comunes (reutilizables)
    protected float dibujarEncabezado(PDPageContentStream contentStream,
                                      PDType1Font fontTitulo,
                                      float pageWidth,
                                      float currentY,
                                      String claveOperacion,
                                      int numeroPagina) throws IOException {
        contentStream.setNonStrokingColor(PDFCommons.COLOR_BANDA_SUPERIOR[0], PDFCommons.COLOR_BANDA_SUPERIOR[1], PDFCommons.COLOR_BANDA_SUPERIOR[2]);
        contentStream.addRect(PDFCommons.MARGIN, currentY - 60, pageWidth - 2 * PDFCommons.MARGIN, 60);
        contentStream.fill();

        contentStream.setNonStrokingColor(1, 1, 1);
        contentStream.beginText();
        contentStream.setFont(fontTitulo, 20);
        contentStream.newLineAtOffset(PDFCommons.MARGIN + 10, currentY - 30);
        contentStream.showText("REPORTE DE " + getTipoOperacion().getTitulo());
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontTitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN + 10, currentY - 50);
        String textoSubtitulo = getTipoOperacion() == TipoOperacion.VENTA ? "Factura: " : "Clave: ";
        contentStream.showText(textoSubtitulo + claveOperacion + " - Página " + numeroPagina);
        contentStream.endText();

        return currentY - 80;
    }

    protected float dibujarSeccionTabla(PDPageContentStream contentStream,
                                        PDType1Font fontSubtitulo,
                                        float pageWidth,
                                        float currentY,
                                        String tituloSeccion) throws IOException {
        contentStream.setNonStrokingColor(PDFCommons.COLOR_SUBTITULOS[0], PDFCommons.COLOR_SUBTITULOS[1], PDFCommons.COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN, currentY);
        contentStream.showText(tituloSeccion);
        contentStream.endText();

        return currentY - 25;
    }

    protected float dibujarSeccionTablaUbicaciones(PDPageContentStream contentStream,
                                                   PDType1Font fontSubtitulo,
                                                   float pageWidth,
                                                   float currentY,
                                                   String tituloSeccion) throws IOException {
        currentY -= 10;

        contentStream.setNonStrokingColor(PDFCommons.COLOR_SUBTITULOS[0], PDFCommons.COLOR_SUBTITULOS[1], PDFCommons.COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(PDFCommons.MARGIN, currentY);
        contentStream.showText(tituloSeccion);
        contentStream.endText();

        return currentY - 30;
    }

    protected PDFCommons.PaginacionResultado dibujarTablaDetallesPaginada(PDPageContentStream contentStream,
                                                                          PDType1Font fontCabecera,
                                                                          PDType1Font fontDatos,
                                                                          float pageWidth,
                                                                          float currentY,
                                                                          List<D> detalles,
                                                                          int inicioIndice,
                                                                          boolean dibujarCabecera) throws IOException {
        String[] headers = getHeaders();
        float[] columnWidths = getColumnWidths();

        float tableWidth = pageWidth - 2 * PDFCommons.MARGIN;
        float[] scaledWidths = PDFCommons.escalarAnchosColumnas(columnWidths, tableWidth);

        if (dibujarCabecera && inicioIndice == 0) {
            contentStream.setNonStrokingColor(PDFCommons.COLOR_ENCABEZADO_TABLA[0], PDFCommons.COLOR_ENCABEZADO_TABLA[1], PDFCommons.COLOR_ENCABEZADO_TABLA[2]);
            contentStream.addRect(PDFCommons.MARGIN, currentY - 20, tableWidth, 20);
            contentStream.fill();

            contentStream.setNonStrokingColor(0, 0, 0);
            float xPos = PDFCommons.MARGIN;
            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.setFont(fontCabecera, 9);
                contentStream.newLineAtOffset(xPos + PDFCommons.CELL_PADDING, currentY - 15);
                contentStream.showText(headers[i]);
                contentStream.endText();
                xPos += scaledWidths[i];
            }

            currentY -= 25;
        }

        int filaNum = inicioIndice + 1;
        int i = inicioIndice;

        for (; i < detalles.size(); i++) {
            if (currentY - 25 < PDFCommons.MIN_Y) {
                break;
            }

            D detalle = detalles.get(i);

            if (filaNum % 2 == 0) {
                contentStream.setNonStrokingColor(PDFCommons.COLOR_FILA_PAR[0], PDFCommons.COLOR_FILA_PAR[1], PDFCommons.COLOR_FILA_PAR[2]);
            } else {
                contentStream.setNonStrokingColor(PDFCommons.COLOR_FILA_IMPAR[0], PDFCommons.COLOR_FILA_IMPAR[1], PDFCommons.COLOR_FILA_IMPAR[2]);
            }
            contentStream.addRect(PDFCommons.MARGIN, currentY - 20, tableWidth, 20);
            contentStream.fill();

            float xPos = PDFCommons.MARGIN;
            dibujarFilaDetalle(contentStream, fontDatos, fontCabecera, currentY, scaledWidths, detalle, filaNum, xPos);

            currentY -= 25;
            filaNum++;
        }

        if (i > inicioIndice) {
            contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
            contentStream.setLineWidth(0.5f);
            float alturaTabla = (i - inicioIndice) * 25;
            if (dibujarCabecera && inicioIndice == 0) {
                alturaTabla += 25;
            }
            float yInicioTabla = currentY + alturaTabla + (dibujarCabecera && inicioIndice == 0 ? 0 : 25);
            contentStream.addRect(PDFCommons.MARGIN, yInicioTabla - alturaTabla, tableWidth, alturaTabla);
            contentStream.stroke();
        }

        boolean completa = (i == detalles.size());
        return new PDFCommons.PaginacionResultado(i, completa, currentY);
    }

    protected PDFCommons.PaginacionResultadoUbicaciones dibujarUbicacionesPaginadasMejorado(
            PDPageContentStream contentStream,
            PDType1Font fontSubtitulo,
            PDType1Font fontNormal,
            PDType1Font fontNormalBold,
            float pageWidth,
            float currentY,
            Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
            List<D> detalles,
            int indiceProductoInicio,
            int indiceUbicacionInicio,
            String productoContinuacion,
            int paginaActual) throws IOException {

        int indiceProductoActual = indiceProductoInicio;
        int indiceUbicacionActual = indiceUbicacionInicio;
        String productoActual = productoContinuacion;
        boolean continuacionProducto = (productoContinuacion != null);

        if (continuacionProducto) {
            // Buscar el producto que estamos continuando
            for (int i = indiceProductoActual; i < detalles.size(); i++) {
                D detalle = detalles.get(i);
                if (obtenerNombreProducto(detalle).equals(productoContinuacion)) {
                    indiceProductoActual = i;
                    break;
                }
            }
        }

        for (int i = indiceProductoActual; i < detalles.size(); i++) {
            D detalle = detalles.get(i);
            String claveProducto = obtenerClaveProducto(detalle);
            List<UbicacionCompra> ubicaciones = ubicacionesPorProducto.get(claveProducto);

            if (ubicaciones != null && !ubicaciones.isEmpty()) {
                boolean esMismoProductoContinuacion = continuacionProducto &&
                        obtenerNombreProducto(detalle).equals(productoContinuacion);

                float espacioEncabezadoProducto = 20;
                float espacioEncabezadoTabla = 25;

                if (currentY - espacioEncabezadoProducto - espacioEncabezadoTabla < PDFCommons.MIN_Y) {
                    return new PDFCommons.PaginacionResultadoUbicaciones(
                            i, 0, false, currentY, productoActual);
                }

                contentStream.setNonStrokingColor(PDFCommons.COLOR_PRODUCTOS[0], PDFCommons.COLOR_PRODUCTOS[1], PDFCommons.COLOR_PRODUCTOS[2]);
                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 11);
                contentStream.newLineAtOffset(PDFCommons.MARGIN + 10, currentY);

                String textoProducto = "• " + obtenerNombreProducto(detalle) + " (" + claveProducto + ")";
                if (esMismoProductoContinuacion && indiceUbicacionActual > 0) {
                    textoProducto += " (CONTINUACIÓN)";
                }

                contentStream.showText(textoProducto);
                contentStream.endText();

                currentY -= 20;
                productoActual = obtenerNombreProducto(detalle);

                float tablaUbicacionesWidth = pageWidth - 2 * PDFCommons.MARGIN - 40;
                float[] columnWidthsUbic = {200, 100};
                float[] scaledUbicWidths = PDFCommons.escalarAnchosColumnas(columnWidthsUbic, tablaUbicacionesWidth);

                contentStream.setNonStrokingColor(PDFCommons.COLOR_ENCABEZADO_TABLA[0], PDFCommons.COLOR_ENCABEZADO_TABLA[1], PDFCommons.COLOR_ENCABEZADO_TABLA[2]);
                contentStream.addRect(PDFCommons.MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                contentStream.fill();

                contentStream.setNonStrokingColor(0, 0, 0);
                float xPosUbic = PDFCommons.MARGIN + 20;

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

                int inicioUbicaciones = esMismoProductoContinuacion ? indiceUbicacionActual : 0;
                int ubicacionesDibujadas = 0;

                for (int j = inicioUbicaciones; j < ubicaciones.size(); j++) {
                    if (currentY - 25 < PDFCommons.MIN_Y) {
                        boolean completadoProducto = (j == ubicaciones.size() - 1);
                        int siguienteIndiceProducto = completadoProducto ? i + 1 : i;
                        int siguienteIndiceUbicacion = completadoProducto ? 0 : j;

                        return new PDFCommons.PaginacionResultadoUbicaciones(
                                siguienteIndiceProducto, siguienteIndiceUbicacion,
                                false, currentY, productoActual);
                    }

                    UbicacionCompra ubicacion = ubicaciones.get(j);

                    if ((j + 1) % 2 == 0) {
                        contentStream.setNonStrokingColor(PDFCommons.COLOR_FILA_PAR[0], PDFCommons.COLOR_FILA_PAR[1], PDFCommons.COLOR_FILA_PAR[2]);
                    } else {
                        contentStream.setNonStrokingColor(PDFCommons.COLOR_FILA_IMPAR[0], PDFCommons.COLOR_FILA_IMPAR[1], PDFCommons.COLOR_FILA_IMPAR[2]);
                    }
                    contentStream.addRect(PDFCommons.MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                    contentStream.fill();

                    contentStream.setNonStrokingColor(0, 0, 0);
                    xPosUbic = PDFCommons.MARGIN + 20;

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

                currentY -= 10;

                indiceUbicacionActual = 0;
                continuacionProducto = false;
                productoContinuacion = null;

                if (ubicacionesDibujadas == ubicaciones.size()) {
                    continue;
                }
            } else {
                indiceProductoActual = i + 1;
            }
        }

        boolean completado = (indiceProductoActual >= detalles.size());
        return new PDFCommons.PaginacionResultadoUbicaciones(
                detalles.size(), 0, completado, currentY, null);
    }

    protected void dibujarPiePagina(PDPageContentStream contentStream,
                                    PDType1Font fontNormal,
                                    float pageWidth,
                                    int paginaActual) throws IOException {
        String tipoReporte;
        switch (getTipoOperacion()) {
            case VENTA:
                tipoReporte = "venta";
                break;
            case COMPRA:
                tipoReporte = "compra";
                break;
            case TRASPASO_SALIDA:
                tipoReporte = "traspaso de salida";
                break;
            case TRASPASO_ENTRADA:
                tipoReporte = "traspaso de entrada";
                break;
            case AJUSTE_INVENTARIO:
                tipoReporte = "ajuste de inventario";
                break;
            default:
                tipoReporte = "reporte";
        }

        PDFCommons.dibujarPiePagina(contentStream, fontNormal, pageWidth, paginaActual, tipoReporte);
    }

    // Métodos auxiliares para obtener información de los detalles
    protected abstract String obtenerClaveProducto(D detalle);
    protected abstract String obtenerNombreProducto(D detalle);
}