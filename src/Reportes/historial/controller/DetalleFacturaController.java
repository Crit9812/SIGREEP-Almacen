package Reportes.historial.controller;

import javafx.scene.Node;
import Consultas.producto.model.producto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import Compartido.exportar.ReporteAjusteExporter;
import Compartido.model.DAO.GenericDAO;
import Compartido.model.IvaConfigService;
import Compartido.exportar.ReporteEntradaExporter;
import Compartido.exportar.ReporteSalidaExporter;
import Compartido.exportar.ReporteTraspasoExporter;
import Compartido.helper.OverlayCarga;
import Compartido.sesion.PermisosRol;
import Reportes.historial.model.HistorialFactura;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class DetalleFacturaController {

    @FXML private StackPane root;
    @FXML private Pane overlayPane;
    @FXML private Label lblTitulo;
    @FXML private VBox contenedorDetalles;
    @FXML private CheckBox chkDetallado;
    @FXML private Button btnCerrar, btnCancelar, btnCancelarEntrada, btnDescargar;
    @FXML private ScrollPane scrollPane;

    private static final ObservableList<String> PRESENTACIONES = FXCollections.unmodifiableObservableList(
            FXCollections.observableArrayList("paquete", "pz", "caja", "bolsa", "pieza", "rollo",
                    "litro", "kilogramo", "metro", "unidad"));
    private static final DateTimeFormatter DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_FORMAT_GUION = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<String, Map<String, String>> COLUMNAS_CACHE = new ConcurrentHashMap<>();

    private HistorialFactura historial;
    private Stage stage;
    private Runnable onRefresh;
    private OverlayCarga overlayCarga, overlayCargaGlobal;
    private volatile boolean procesandoCancelacion = false;
    private final boolean soloLecturaReportes = !PermisosRol.esAdministrador();

    @FXML
    public void initialize() {
        if (chkDetallado != null) chkDetallado.selectedProperty().addListener((o, a, n) -> cargarDetalles());
        if (root != null && overlayPane != null) overlayCarga = new OverlayCarga(root, overlayPane);
        actualizarEstadoUI();
    }

    public void setHistorial(HistorialFactura historial) {
        this.historial = historial;
        actualizarEstadoUI();
        cargarDetalles();
    }
    public void setStage(Stage stage) { this.stage = stage; if (stage != null) stage.setResizable(false); }
    public void setOnRefresh(Runnable onRefresh) { this.onRefresh = onRefresh; }
    public void setOverlayCargaGlobal(OverlayCarga overlay) { this.overlayCargaGlobal = overlay; }

    @FXML private void cerrarVentana() {
        if (stage != null) stage.close();
        else if (btnCerrar != null && btnCerrar.getScene() != null) btnCerrar.getScene().getWindow().hide();
    }

    @FXML private void cancelarMovimiento() {
        if (soloLecturaReportes) return;
        if (historial == null) return;
        String mov = historial.getMovimiento();
        if ("Salida".equalsIgnoreCase(mov)) cancelarSalida();
        else if ("Ajuste".equalsIgnoreCase(mov)) cancelarAjuste();
    }

    @FXML private void cancelarSalida() {
        if (soloLecturaReportes) return;
        if (!esMovimientoValido("Salida")) return;
        Integer salidaId = parseInteger(historial.getClaveMovimiento());
        if (salidaId == null || salidaId <= 0) return;

        confirmarCancelacion("salida", () -> {
            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    return ejecutarCancelacionSalidaDB(salidaId);
                }
            };
            task.setOnSucceeded(e -> {
                if (Boolean.TRUE.equals(task.getValue())) {
                    finalizarCancelacionConExito("Cancelación de salida exitosa.");
                } else {
                    finalizarCancelacionSinMensaje();
                }
            });
            task.setOnFailed(e -> {
                if (task.getException() != null) task.getException().printStackTrace();
                finalizarCancelacionSinMensaje();
            });
            new Thread(task, "cancelar-salida").start();
        });
    }

    private boolean ejecutarCancelacionSalidaDB(Integer salidaId) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (!puedeCancelarSalida(conn, salidaId)) { conn.rollback(); return false; }

                Map<String, String> colsDetalle = obtenerColumnasCached(conn, "detalle_Salida");
                Map<String, String> colsArticulo = obtenerColumnasCached(conn, "articulo");
                Map<String, String> colsDetalleArticulo = obtenerColumnasCached(conn, "detalleArticulo");
                Map<String, String> colsDetalleEntrada = obtenerColumnasCached(conn, "detalle_Entrada");
                Map<String, String> colsEntrada = obtenerColumnasCached(conn, "entradas");
                Map<String, String> colsSalida = obtenerColumnasCached(conn, "salidas");

                String colDetalleId = resolverColumna(colsDetalle, "idDetalleSalida", "id", "id_detalle_salida");
                String colDetalleClaveSalida = resolverColumna(colsDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
                String colDetalleCantidad = resolverColumna(colsDetalle, "cantidad", "cantidadSalida", "cantidad_salida");
                String colDetallePrecioBruto = resolverColumna(colsDetalle, "precioBrutoTotalSalida", "precioBrutoTotal", "precio_bruto");
                String colDetallePrecioTotal = resolverColumna(colsDetalle, "precioTotalSalida", "precioTotal", "precio_total");
                String colDetalleEstado = resolverColumna(colsDetalle, "estado", "Estado");
                String colSalidaId = resolverColumna(colsSalida, "idSalida", "id", "id_salida");
                String colSalidaEstado = resolverColumna(colsSalida, "Estado", "estado");
                String colSalidaPrecioNeto = resolverColumna(colsSalida, "precioNetoSalida", "precioNeto", "precio_neto");
                String colSalidaPrecioTotal = resolverColumna(colsSalida, "precioTotalSalida", "precioTotal", "precio_total");

                String colArticuloId = resolverColumna(colsArticulo, "idArticulo", "id", "id_articulo");
                String colArticuloDetalleEntrada = resolverColumna(colsArticulo, "idDetalleEntrada", "id_detalle_entrada",
                        "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                String colArticuloDetalleSalida = resolverColumna(colsArticulo, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");
                String colArticuloEstado = resolverColumna(colsArticulo, "Estado", "estado");

                String colDetalleArticuloIdArticulo = resolverColumna(colsDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
                String colDetalleArticuloSalida = resolverColumna(colsDetalleArticulo, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");
                String colDetalleArticuloEstado = resolverColumna(colsDetalleArticulo, "estado", "Estado");

                String colDetalleEntradaId = resolverColumna(colsDetalleEntrada, "idDetalleEntrada", "id", "id_detalle_entrada");
                String colDetalleEntradaClaveEntrada = resolverColumna(colsDetalleEntrada, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
                String colDetalleEntradaEstado = resolverColumna(colsDetalleEntrada, "estado", "Estado");
                String colEntradaId = resolverColumna(colsEntrada, "idEntrada", "id", "id_entrada");
                String colEntradaEstado = resolverColumna(colsEntrada, "Estado", "estado");

                if (colDetalleId == null || colDetalleClaveSalida == null || colSalidaId == null || colSalidaEstado == null) {
                    conn.rollback();
                    return false;
                }

                List<Integer> detallesSalida = consultarIds(conn, "detalle_Salida", colDetalleId, colDetalleClaveSalida, salidaId);
                if (detallesSalida.isEmpty()) { conn.rollback(); return false; }

                Set<Integer> detalleEntradaIds = new HashSet<>();

                if (colArticuloDetalleEntrada != null && colArticuloDetalleSalida != null) {
                    detalleEntradaIds.addAll(consultarDetalleEntradaPorSalida(conn, colArticuloDetalleEntrada,
                            colArticuloDetalleSalida, detallesSalida));
                }

                if (colArticuloDetalleSalida != null && colArticuloEstado != null) {
                    actualizarEstadoArticulos(conn, colArticuloDetalleSalida, colArticuloEstado,
                            "disponible", true, detallesSalida);
                }

                if (!detallesSalida.isEmpty() && colDetalleArticuloSalida != null && colDetalleArticuloIdArticulo != null
                        && colArticuloId != null && colArticuloDetalleEntrada != null) {
                    detalleEntradaIds.addAll(consultarDetalleEntradaPorDetalleArticulo(conn, colArticuloId,
                            colArticuloDetalleEntrada, colDetalleArticuloIdArticulo, colDetalleArticuloSalida, detallesSalida));
                }

                if (colDetalleArticuloSalida != null && colDetalleArticuloEstado != null) {
                    actualizarDetalleArticuloSalida(conn, colDetalleArticuloSalida, colDetalleArticuloEstado,
                            detallesSalida);
                }

                if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaEstado != null) {
                    actualizarEstadoDetalleEntrada(conn, colDetalleEntradaId, colDetalleEntradaEstado,
                            "activo", detalleEntradaIds);
                }

                if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaClaveEntrada != null
                        && colEntradaId != null && colEntradaEstado != null) {
                    Set<Integer> entradasIds = consultarEntradasPorDetalles(conn, colDetalleEntradaId,
                            colDetalleEntradaClaveEntrada, detalleEntradaIds);
                    if (!entradasIds.isEmpty()) {
                        actualizarEstadoEntradas(conn, colEntradaId, colEntradaEstado,
                                "disponible", entradasIds);
                    }
                }

                actualizarDetallesSalida(conn, colDetalleId, colDetalleCantidad, colDetallePrecioBruto,
                        colDetallePrecioTotal, colDetalleEstado, detallesSalida);

                actualizarSalida(conn, colSalidaId, colSalidaEstado, colSalidaPrecioNeto,
                        colSalidaPrecioTotal, salidaId);

                conn.commit();
                notificarActualizacion();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }


    @FXML private void cancelarEntrada() {

        if (soloLecturaReportes) return;
        if (!esMovimientoValido("Entrada")) return;
        Integer entradaId = parseInteger(historial.getClaveMovimiento());
        if (entradaId == null || entradaId <= 0) return;

        confirmarCancelacion("entrada", () -> {
            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    return ejecutarCancelacionEntrada(entradaId);
                }
            };
            task.setOnSucceeded(e -> {
                if (Boolean.TRUE.equals(task.getValue()))
                    finalizarCancelacionConExito("Cancelación de entrada exitosa.");
                else finalizarCancelacionSinMensaje();
            });
            task.setOnFailed(e -> {
                e.getSource().getException().printStackTrace();
                finalizarCancelacionSinMensaje();
            });
            new Thread(task, "cancelar-entrada").start();
        });
    }

    private boolean ejecutarCancelacionEntrada(Integer entradaId) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (!puedeCancelarEntrada(conn, entradaId)) { conn.rollback(); return false; }

                Map<String, String> colsDetalle = obtenerColumnasCached(conn, "detalle_Entrada");
                Map<String, String> colsArticulo = obtenerColumnasCached(conn, "articulo");
                Map<String, String> colsDetalleArticulo = obtenerColumnasCached(conn, "detalleArticulo");
                Map<String, String> colsEntrada = obtenerColumnasCached(conn, "entradas");

                String colDetalleId = resolverColumna(colsDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
                String colDetalleClave = resolverColumna(colsDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
                String colDetalleCantidad = resolverColumna(colsDetalle, "cantidad", "cantidadEntrada");
                String colDetallePrecioBruto = resolverColumna(colsDetalle, "precioBrutoTotal", "precioBruto", "precio_bruto");
                String colDetallePrecioTotal = resolverColumna(colsDetalle, "precioTotal", "precio_total");
                String colDetalleEstado = resolverColumna(colsDetalle, "estado", "Estado");

                String colArticuloId = resolverColumna(colsArticulo, "idArticulo", "id", "id_articulo");
                String colArticuloDetalle = resolverColumna(colsArticulo, "idDetalleEntrada", "id_detalle_entrada",
                        "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                String colArticuloEstado = resolverColumna(colsArticulo, "Estado", "estado");

                String colDetalleArticuloIdArticulo = resolverColumna(colsDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
                String colDetalleArticuloEstado = resolverColumna(colsDetalleArticulo, "estado", "Estado");

                String colEntradaId = resolverColumna(colsEntrada, "idEntrada", "id", "id_entrada");
                String colEntradaPrecioNeto = resolverColumna(colsEntrada, "precioNetoEntrada", "precioNeto", "precio_neto");
                String colEntradaPrecioTotal = resolverColumna(colsEntrada, "precioTotalEntrada", "precioTotal", "precio_total");
                String colEntradaEstado = resolverColumna(colsEntrada, "Estado", "estado");

                if (colDetalleId == null || colDetalleClave == null || colEntradaId == null ||
                        colEntradaEstado == null || colArticuloDetalle == null || colArticuloEstado == null) {
                    conn.rollback(); return false;
                }

                List<Integer> detallesEntrada = consultarIds(conn, "detalle_Entrada", colDetalleId, colDetalleClave, entradaId);

                if (!detallesEntrada.isEmpty()) {
                    actualizarEstadoArticulosPorEntrada(conn, colArticuloDetalle, colArticuloEstado, detallesEntrada);

                    if (colDetalleArticuloIdArticulo != null && colDetalleArticuloEstado != null && colArticuloId != null) {
                        actualizarDetalleArticuloPorEntrada(conn, colArticuloId, colArticuloDetalle, colArticuloEstado,
                                colDetalleArticuloIdArticulo, colDetalleArticuloEstado, detallesEntrada);
                    }

                    actualizarDetallesEntrada(conn, colDetalleId, colDetalleCantidad, colDetallePrecioBruto,
                            colDetallePrecioTotal, colDetalleEstado, detallesEntrada);
                }

                actualizarEntrada(conn, colEntradaId, colEntradaEstado, colEntradaPrecioNeto,
                        colEntradaPrecioTotal, entradaId);

                conn.commit();
                notificarActualizacion();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @FXML private void cancelarAjuste() {
        if (soloLecturaReportes) return;
        if (!esMovimientoValido("Ajuste")) return;
        String ajusteId = historial.getClaveMovimiento();
        if (ajusteId == null || ajusteId.isBlank()) return;

        confirmarCancelacion("ajuste", () -> {
            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    return ejecutarCancelacionAjusteDB(ajusteId);
                }
            };
            task.setOnSucceeded(e -> {
                if (Boolean.TRUE.equals(task.getValue())) {
                    finalizarCancelacionConExito("Cancelación de ajuste exitosa.");
                } else {
                    finalizarCancelacionSinMensaje();
                }
            });
            task.setOnFailed(e -> {
                if (task.getException() != null) task.getException().printStackTrace();
                finalizarCancelacionSinMensaje();
            });
            new Thread(task, "cancelar-ajuste").start();
        });
    }

    private boolean ejecutarCancelacionAjusteDB(String ajusteId) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (!puedeCancelarAjuste(conn, ajusteId)) { conn.rollback(); return false; }

                Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
                Map<String, String> colsDetSal = obtenerColumnasCached(conn, "detalle_Salida");
                Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
                Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
                Map<String, String> colsAjuste = obtenerColumnasCached(conn, "ajuste_inventario");
                Map<String, String> colsEnt = obtenerColumnasCached(conn, "entradas");

                String colDetEntId = resolverColumna(colsDetEnt, "idDetalleEntrada", "id", "id_detalle_entrada");
                String colDetEntClave = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
                String colDetEntCant = resolverColumna(colsDetEnt, "cantidad", "cantidadEntrada");
                String colDetEntPrecioBruto = resolverColumna(colsDetEnt, "precioBrutoTotal", "precioBruto", "precio_bruto");
                String colDetEntPrecioTotal = resolverColumna(colsDetEnt, "precioTotal", "precio_total");
                String colDetEntEstado = resolverColumna(colsDetEnt, "estado", "Estado");

                String colDetSalId = resolverColumna(colsDetSal, "idDetalleSalida", "id", "id_detalle_salida");
                String colDetSalClave = resolverColumna(colsDetSal, "claveSalida", "idSalida", "id_salida", "salida_id");
                String colDetSalCant = resolverColumna(colsDetSal, "cantidad", "cantidadSalida", "cantidad_salida");
                String colDetSalPrecioBruto = resolverColumna(colsDetSal, "precioBrutoTotalSalida", "precioBrutoTotal", "precio_bruto");
                String colDetSalPrecioTotal = resolverColumna(colsDetSal, "precioTotalSalida", "precioTotal", "precio_total");
                String colDetSalEstado = resolverColumna(colsDetSal, "estado", "Estado");

                String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
                String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                        "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                String colArtDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");
                String colArtEstado = resolverColumna(colsArt, "Estado", "estado");

                String colDetArtIdArticulo = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
                String colDetArtSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");
                String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");

                String colAjusteId = resolverColumna(colsAjuste, "idAjuste", "id", "id_ajuste");
                String colAjusteEstado = resolverColumna(colsAjuste, "estado", "Estado");
                String colAjustePrecioNeto = resolverColumna(colsAjuste, "precioNeto", "precio_neto");
                String colAjustePrecioTotal = resolverColumna(colsAjuste, "precioTotal", "precio_total");
                String colEntId = resolverColumna(colsEnt, "idEntrada", "id", "id_entrada");
                String colEntEstado = resolverColumna(colsEnt, "Estado", "estado");

                if (colDetEntId == null || colDetEntClave == null || colDetSalId == null ||
                        colDetSalClave == null || colAjusteId == null || colAjusteEstado == null) {
                    conn.rollback();
                    return false;
                }

                List<Integer> detallesEntrada = consultarIds(conn, "detalle_Entrada", colDetEntId, colDetEntClave, ajusteId);
                if (!detallesEntrada.isEmpty()) {
                    if (colArtDetEnt != null && colArtEstado != null) {
                        actualizarEstadoArticulos(conn, colArtDetEnt, colArtEstado, "eliminado", false, detallesEntrada);
                    }
                    if (colArtId != null && colDetArtIdArticulo != null && colDetArtEstado != null) {
                        actualizarDetalleArticuloPorEntrada(conn, colArtId, colArtDetEnt, colArtEstado,
                                colDetArtIdArticulo, colDetArtEstado, detallesEntrada);
                    }
                    actualizarDetallesEntrada(conn, colDetEntId, colDetEntCant, colDetEntPrecioBruto,
                            colDetEntPrecioTotal, colDetEntEstado, detallesEntrada);
                }

                List<Integer> detallesSalida = consultarIds(conn, "detalle_Salida", colDetSalId, colDetSalClave, ajusteId);
                Set<Integer> detalleEntradaIds = new HashSet<>();

                if (!detallesSalida.isEmpty()) {
                    if (colArtDetEnt != null && colArtDetSal != null) {
                        detalleEntradaIds.addAll(consultarDetalleEntradaPorSalida(conn, colArtDetEnt,
                                colArtDetSal, detallesSalida));
                    }
                    if (colArtDetSal != null && colArtEstado != null) {
                        actualizarEstadoArticulos(conn, colArtDetSal, colArtEstado, "disponible", true, detallesSalida);
                    }
                    if (!detallesSalida.isEmpty() && colDetArtSal != null && colDetArtIdArticulo != null
                            && colArtId != null && colArtDetEnt != null) {
                        detalleEntradaIds.addAll(consultarDetalleEntradaPorDetalleArticulo(conn, colArtId,
                                colArtDetEnt, colDetArtIdArticulo, colDetArtSal, detallesSalida));
                    }
                    if (colDetArtSal != null && colDetArtEstado != null) {
                        actualizarDetalleArticuloSalida(conn, colDetArtSal, colDetArtEstado, detallesSalida);
                    }
                    actualizarDetallesSalida(conn, colDetSalId, colDetSalCant, colDetSalPrecioBruto,
                            colDetSalPrecioTotal, colDetSalEstado, detallesSalida);
                }

                if (!detalleEntradaIds.isEmpty() && colDetEntId != null && colDetEntEstado != null) {
                    actualizarEstadoDetalleEntrada(conn, colDetEntId, colDetEntEstado, "activo", detalleEntradaIds);
                }

                if (!detalleEntradaIds.isEmpty() && colDetEntId != null && colDetEntClave != null
                        && colEntId != null && colEntEstado != null) {
                    Set<Integer> entradasIds = consultarEntradasPorDetalles(conn, colDetEntId,
                            colDetEntClave, detalleEntradaIds);
                    if (!entradasIds.isEmpty()) {
                        actualizarEstadoEntradas(conn, colEntId, colEntEstado, "disponible", entradasIds);
                    }
                }

                actualizarAjuste(conn, colAjusteId, colAjusteEstado, colAjustePrecioNeto,
                        colAjustePrecioTotal, ajusteId);

                conn.commit();
                notificarActualizacion();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // === MÉTODOS DE UTILIDAD DB ===

    private Map<String, String> obtenerColumnasCached(Connection conn, String tabla) throws SQLException {
        if (conn == null) return Collections.emptyMap();
        return COLUMNAS_CACHE.computeIfAbsent(tabla, t -> {
            try {
                Map<String, String> cols = new HashMap<>();
                try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, t, null)) {
                    while (rs.next()) {
                        String nombre = rs.getString("COLUMN_NAME");
                        if (nombre != null) cols.put(nombre.trim().toLowerCase(), nombre.trim());
                    }
                }
                return cols;
            } catch (SQLException e) {
                return Collections.emptyMap();
            }
        });
    }

    private String resolverColumna(Map<String, String> columnas, String... alternativas) {
        for (String alt : alternativas) {
            if (alt != null) {
                String res = columnas.get(alt.toLowerCase());
                if (res != null) return res;
            }
        }
        return null;
    }

    private List<Integer> consultarIds(Connection conn, String tabla, String colId, String colClave, Object valor) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT `" + colId + "` FROM " + tabla + " WHERE `" + colClave + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParam(ps, 1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private void setParam(PreparedStatement ps, int idx, Object val) throws SQLException {
        if (val instanceof Integer) ps.setInt(idx, (Integer) val);
        else if (val instanceof String) ps.setString(idx, (String) val);
        else ps.setObject(idx, val);
    }

    private String placeholders(int n) {
        if (n <= 0) return "";
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    private int ejecutarConteo(Connection conn, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParam(ps, 1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void actualizarEstadoArticulos(Connection conn, String colDetalle, String colEstado,
                                           String nuevoEstado, boolean ponerNulo, List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "UPDATE articulo SET `" + colEstado + "` = ?" +
                (ponerNulo ? ", `" + colDetalle + "` = NULL" : "") +
                " WHERE `" + colDetalle + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer id : ids) {
                ps.setString(1, nuevoEstado);
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void actualizarEstadoArticulosPorEntrada(Connection conn, String colDetalle, String colEstado,
                                                     List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "UPDATE articulo SET `" + colEstado + "` = 'eliminado' WHERE `" + colDetalle +
                "` = ? AND LOWER(`" + colEstado + "`) <> 'segmentado'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer id : ids) {
                ps.setInt(1, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void actualizarDetalleArticuloSalida(Connection conn, String colDetalleSalida,
                                                 String colEstado, List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "UPDATE detalleArticulo SET `" + colEstado + "` = ?, `" +
                colDetalleSalida + "` = NULL WHERE `" + colDetalleSalida + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer id : ids) {
                ps.setString(1, "disponible");
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void actualizarDetalleArticuloPorEntrada(Connection conn, String colArtId, String colArtDetEnt,
                                                     String colArtEstado,
                                                     String colDetArtIdArticulo, String colDetArtEstado,
                                                     List<Integer> detallesEntrada) throws SQLException {
        if (detallesEntrada.isEmpty()) return;
        String sqlNoSegmentado = "UPDATE detalleArticulo da JOIN articulo a ON a.`" + colArtId + "` = da.`" + colDetArtIdArticulo +
                "` SET da.`" + colDetArtEstado + "` = ? WHERE a.`" + colArtDetEnt + "` = ? AND LOWER(a.`" + colArtEstado + "`) <> 'segmentado'";
        try (PreparedStatement ps = conn.prepareStatement(sqlNoSegmentado)) {
            for (Integer detalleId : detallesEntrada) {
                ps.setString(1, "eliminado");
                ps.setInt(2, detalleId);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        String sqlSegmentado = "UPDATE detalleArticulo da JOIN articulo a ON a.`" + colArtId + "` = da.`" + colDetArtIdArticulo +
                "` SET da.`" + colDetArtEstado + "` = ? WHERE a.`" + colArtDetEnt + "` = ? AND LOWER(a.`" + colArtEstado + "`) = 'segmentado'";
        try (PreparedStatement ps = conn.prepareStatement(sqlSegmentado)) {
            for (Integer detalleId : detallesEntrada) {
                ps.setString(1, "eliminado");
                ps.setInt(2, detalleId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Set<Integer> consultarDetalleEntradaPorSalida(Connection conn, String colDetEnt,
                                                          String colDetSal, List<Integer> ids) throws SQLException {
        Set<Integer> result = new HashSet<>();
        if (ids.isEmpty()) return result;
        String sql = "SELECT DISTINCT `" + colDetEnt + "` FROM articulo WHERE `" + colDetSal + "` IN (" +
                placeholders(ids.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getInt(1));
            }
        }
        return result;
    }

    private Set<Integer> consultarDetalleEntradaPorDetalleArticulo(Connection conn, String colArtId,
                                                                   String colArtDetEnt, String colDetArtIdArticulo,
                                                                   String colDetArtSal, List<Integer> ids) throws SQLException {
        Set<Integer> result = new HashSet<>();
        if (ids.isEmpty()) return result;
        String sql = "SELECT DISTINCT a.`" + colArtDetEnt + "` AS idDetalleEntrada FROM detalleArticulo da " +
                "JOIN articulo a ON a.`" + colArtId + "` = da.`" + colDetArtIdArticulo + "` " +
                "WHERE da.`" + colDetArtSal + "` IN (" + placeholders(ids.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getInt("idDetalleEntrada"));
            }
        }
        return result;
    }

    private void actualizarEstadoDetalleEntrada(Connection conn, String colId, String colEstado,
                                                String nuevoEstado, Set<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "UPDATE detalle_Entrada SET `" + colEstado + "` = ? WHERE `" + colId + "` IN (" +
                placeholders(ids.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, nuevoEstado);
            for (Integer id : ids) ps.setInt(idx++, id);
            ps.executeUpdate();
        }
    }

    private Set<Integer> consultarEntradasPorDetalles(Connection conn, String colDetId, String colDetClave,
                                                      Set<Integer> detalleIds) throws SQLException {
        Set<Integer> result = new HashSet<>();
        if (detalleIds.isEmpty()) return result;
        String sql = "SELECT DISTINCT `" + colDetClave + "` FROM detalle_Entrada WHERE `" +
                colDetId + "` IN (" + placeholders(detalleIds.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : detalleIds) ps.setInt(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer val = parseInteger(rs.getObject(1));
                    if (val != null && val > 0) result.add(val);
                }
            }
        }
        return result;
    }

    private void actualizarEstadoEntradas(Connection conn, String colId, String colEstado,
                                          String nuevoEstado, Set<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "UPDATE entradas SET `" + colEstado + "` = ? WHERE `" + colId + "` IN (" +
                placeholders(ids.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, nuevoEstado);
            for (Integer id : ids) ps.setInt(idx++, id);
            ps.executeUpdate();
        }
    }

    private void actualizarDetallesSalida(Connection conn, String colId, String colCant,
                                          String colBruto, String colTotal, String colEstado,
                                          List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        StringBuilder sql = new StringBuilder("UPDATE detalle_Salida SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampo(sql, valores, colCant, 0);
        agregarCampo(sql, valores, colBruto, BigDecimal.ZERO);
        agregarCampo(sql, valores, colTotal, BigDecimal.ZERO);
        agregarCampo(sql, valores, colEstado, "desactivado");
        if (valores.isEmpty()) return;
        sql.append(" WHERE `").append(colId).append("` = ?");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (Integer id : ids) {
                int idx = 1;
                for (Object val : valores) ps.setObject(idx++, val);
                ps.setInt(idx, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void actualizarDetallesEntrada(Connection conn, String colId, String colCant,
                                           String colBruto, String colTotal, String colEstado,
                                           List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        StringBuilder sql = new StringBuilder("UPDATE detalle_Entrada SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampo(sql, valores, colCant, 0);
        agregarCampo(sql, valores, colBruto, BigDecimal.ZERO);
        agregarCampo(sql, valores, colTotal, BigDecimal.ZERO);
        agregarCampo(sql, valores, colEstado, "desactivado");
        if (valores.isEmpty()) return;
        sql.append(" WHERE `").append(colId).append("` = ?");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (Integer id : ids) {
                int idx = 1;
                for (Object val : valores) ps.setObject(idx++, val);
                ps.setInt(idx, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void actualizarSalida(Connection conn, String colId, String colEstado,
                                  String colNeto, String colTotal, Integer salidaId) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE salidas SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampo(sql, valores, colNeto, BigDecimal.ZERO);
        agregarCampo(sql, valores, colTotal, BigDecimal.ZERO);
        agregarCampo(sql, valores, colEstado, "cancelado");
        if (valores.isEmpty()) return;
        sql.append(" WHERE `").append(colId).append("` = ?");
        valores.add(salidaId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) ps.setObject(i + 1, valores.get(i));
            ps.executeUpdate();
        }
    }

    private void actualizarEntrada(Connection conn, String colId, String colEstado,
                                   String colNeto, String colTotal, Integer entradaId) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE entradas SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampo(sql, valores, colNeto, BigDecimal.ZERO);
        agregarCampo(sql, valores, colTotal, BigDecimal.ZERO);
        agregarCampo(sql, valores, colEstado, "cancelado");
        if (valores.isEmpty()) return;
        sql.append(" WHERE `").append(colId).append("` = ?");
        valores.add(entradaId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) ps.setObject(i + 1, valores.get(i));
            ps.executeUpdate();
        }
    }

    private void actualizarAjuste(Connection conn, String colId, String colEstado,
                                  String colNeto, String colTotal, String ajusteId) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ajuste_inventario SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampo(sql, valores, colNeto, BigDecimal.ZERO);
        agregarCampo(sql, valores, colTotal, BigDecimal.ZERO);
        agregarCampo(sql, valores, colEstado, "cancelado");
        if (valores.isEmpty()) return;
        sql.append(" WHERE `").append(colId).append("` = ?");
        valores.add(ajusteId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) ps.setObject(i + 1, valores.get(i));
            ps.executeUpdate();
        }
    }

    private void agregarCampo(StringBuilder sql, List<Object> valores, String columna, Object valor) {
        if (columna == null) return;
        if (!valores.isEmpty()) sql.append(", ");
        sql.append("`").append(columna).append("` = ?");
        valores.add(valor);
    }

    // === VALIDACIONES DE CANCELACIÓN ===

    private boolean puedeCancelarEntrada(Connection conn, Integer entradaId) throws SQLException {
        String estado = obtenerEstadoEntrada(conn, entradaId);
        if (estado != null && List.of("cancelado", "finalizada", "finalizado", "revision", "revisión")
                .contains(estado.trim().toLowerCase())) return false;
        return entradaSoloTieneEstadosCancelables(conn, entradaId);
    }

    private boolean entradaSoloTieneEstadosCancelables(Connection conn, Integer entradaId) throws SQLException {
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Entrada");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        String colDetId = resolverColumna(colsDet, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetClave = resolverColumna(colsDet, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArtDet = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtEstado = resolverColumna(colsArt, "Estado", "estado");
        String colDetArtIdArticulo = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");
        if (colDetId == null || colDetClave == null || colArtDet == null || colArtEstado == null) return false;

        String sqlArt = "SELECT COUNT(*) FROM articulo a JOIN detalle_Entrada d ON a.`" + colArtDet + "` = d.`" +
                colDetId + "` WHERE d.`" + colDetClave + "` = ? AND LOWER(a.`" + colArtEstado + "`) != 'eliminado'";
        int totalArt = ejecutarConteo(conn, sqlArt, entradaId);
        String sqlArtNoPerm = "SELECT COUNT(*) FROM articulo a JOIN detalle_Entrada d ON a.`" + colArtDet + "` = d.`" +
                colDetId + "` WHERE d.`" + colDetClave + "` = ? AND LOWER(a.`" + colArtEstado +
                "`) NOT IN ('eliminado', 'disponible', 'segmentado')";
        int artNoPerm = ejecutarConteo(conn, sqlArtNoPerm, entradaId);

        int segmentadosNoDisponibles = 0;
        if (colArtId != null && colDetArtIdArticulo != null && colDetArtEstado != null) {
            String sqlSegmentadosNoDisponibles = "SELECT COUNT(*) FROM articulo a " +
                    "JOIN detalle_Entrada d ON a.`" + colArtDet + "` = d.`" + colDetId + "` " +
                    "WHERE d.`" + colDetClave + "` = ? AND LOWER(a.`" + colArtEstado + "`) = 'segmentado' " +
                    "AND (NOT EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`" + colDetArtIdArticulo + "` = a.`" + colArtId +
                    "` AND LOWER(da.`" + colDetArtEstado + "`) = 'disponible') " +
                    "OR EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`" + colDetArtIdArticulo + "` = a.`" + colArtId +
                    "` AND LOWER(da.`" + colDetArtEstado + "`) <> 'disponible'))";
            segmentadosNoDisponibles = ejecutarConteo(conn, sqlSegmentadosNoDisponibles, entradaId);
        }

        return artNoPerm == 0 && totalArt > 0 && segmentadosNoDisponibles == 0;
    }

    private boolean puedeCancelarSalida(Connection conn, Integer salidaId) throws SQLException {
        String estado = obtenerEstadoSalida(conn, salidaId);
        return estado == null || !"cancelado".equalsIgnoreCase(estado);
    }

    private boolean puedeCancelarAjuste(Connection conn, String ajusteId) throws SQLException {
        String estado = obtenerEstadoAjuste(conn, ajusteId);
        if (estado != null && "cancelado".equalsIgnoreCase(estado)) return false;
        return !ajusteTieneDetalleEntradaDesactivado(conn, ajusteId) &&
                !ajusteTieneVentasEnEntrada(conn, ajusteId);
    }

    private boolean ajusteTieneDetalleEntradaDesactivado(Connection conn, String ajusteId) throws SQLException {
        Map<String, String> cols = obtenerColumnasCached(conn, "detalle_Entrada");
        String colClave = resolverColumna(cols, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colEstado = resolverColumna(cols, "estado", "Estado");
        if (colClave == null || colEstado == null) return false;
        String sql = "SELECT COUNT(*) FROM detalle_Entrada WHERE `" + colClave + "` = ? AND LOWER(`" + colEstado + "`) = 'desactivado'";
        return ejecutarConteo(conn, sql, ajusteId) > 0;
    }

    private boolean ajusteTieneVentasEnEntrada(Connection conn, String ajusteId) throws SQLException {
        return false;
    }

    private String obtenerEstadoEntrada(Connection conn, Integer id) throws SQLException {
        return obtenerEstadoMovimiento(conn, "entradas", "idEntrada", id, "estado", "Estado");
    }

    private String obtenerEstadoSalida(Connection conn, Integer id) throws SQLException {
        return obtenerEstadoMovimiento(conn, "salidas", "idSalida", id, "estado", "Estado");
    }

    private String obtenerEstadoAjuste(Connection conn, String id) throws SQLException {
        return obtenerEstadoMovimiento(conn, "ajuste_inventario", "idAjuste", id, "estado", "Estado");
    }

    private String obtenerEstadoMovimiento(Connection conn, String tabla, String colId,
                                           Object valor, String... colEstadoAlt) throws SQLException {
        Map<String, String> cols = obtenerColumnasCached(conn, tabla);
        String idCol = resolverColumna(cols, colId, "id", tabla + "_id");
        String estadoCol = resolverColumna(cols, colEstadoAlt);
        if (idCol == null || estadoCol == null) return null;
        String sql = "SELECT `" + estadoCol + "` FROM " + tabla + " WHERE `" + idCol + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParam(ps, 1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // === CARGA DE DETALLES ===

    private void cargarDetalles() {
        if (historial == null) { mostrarSinDetalles(); return; }
        boolean detallado = chkDetallado != null && chkDetallado.isSelected();

        Task<List<DetalleLinea>> task = new Task<>() {
            @Override protected List<DetalleLinea> call() { return obtenerDetalles(detallado); }
        };
        task.setOnSucceeded(e -> renderizarDetalles(task.getValue(), detallado));
        task.setOnFailed(e -> mostrarSinDetalles());
        new Thread(task, "cargar-detalles").start();
    }

    private List<DetalleLinea> obtenerDetalles(boolean detallado) {
        String mov = historial.getMovimiento(), clave = historial.getClaveMovimiento();
        if (mov == null || clave == null || clave.isBlank()) return List.of();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return List.of();
            switch (mov.toLowerCase()) {
                case "entrada": return obtenerDetallesEntrada(conn, clave, detallado, "Entrada");
                case "salida": return obtenerDetallesSalida(conn, clave, detallado, "Salida");
                case "ajuste":
                    List<DetalleLinea> res = new ArrayList<>();
                    res.addAll(obtenerDetallesEntrada(conn, clave, detallado, "Entrada"));
                    res.addAll(obtenerDetallesSalida(conn, clave, detallado, "Salida"));
                    return res;
                default: return List.of();
            }
        } catch (SQLException e) { e.printStackTrace(); return List.of(); }
    }

    private List<DetalleLinea> obtenerDetallesEntrada(Connection conn, String clave, boolean detallado, String tipo) throws SQLException {
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Entrada");
        Map<String, String> colsProd = obtenerColumnasCached(conn, "productos");

        String colDetId = resolverColumna(colsDet, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClave = resolverColumna(colsDet, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colProd = resolverColumna(colsDet, "claveProducto", "idProducto", "id_producto", "producto_id");
        if (colDetId == null || colClave == null || colProd == null) return List.of();

        String colCant = resolverColumna(colsDet, "cantidad", "cantidadEntrada");
        String colPrecioUnit = resolverColumna(colsDet, "precioUnitario", "precioEntrada", "costoEntrada");
        String colPrecioIva = resolverColumna(colsDet, "precioIVA", "precioIva", "precio_iva");
        String colPrecioTotal = resolverColumna(colsDet, "precioTotal", "precio_total");
        String colNota = resolverColumna(colsDet, "Nota", "nota", "comentario", "observaciones");
        String colEstado = resolverColumna(colsDet, "estado", "Estado");

        String colProdId = resolverColumna(colsProd, "id", "idProducto", "claveProducto");
        String colProdNom = resolverColumna(colsProd, "nombre", "Nombre", "producto");

        boolean hasJoin = colProdId != null && colProdNom != null;
        String join = hasJoin ? "LEFT JOIN productos p ON d.`" + colProd + "` = p.`" + colProdId + "`" : "";
        String prodExpr = hasJoin ? "p.`" + colProdNom + "`" : "d.`" + colProd + "`";
        String orden = colEstado != null ? "CASE WHEN LOWER(d.`" + colEstado + "`) = 'activo' THEN 0 ELSE 1 END, " : "";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle, d.`%s` AS claveProducto, %s AS cantidad, %s AS precioUnitario,
                       %s AS precioIva, %s AS precioTotal, %s AS nota, %s AS producto
                FROM detalle_Entrada d %s WHERE d.`%s` = ?
                ORDER BY %s producto, idDetalle
                """, colDetId, colProd, columnaOrNull(colCant), columnaOrNull(colPrecioUnit),
                columnaOrNull(colPrecioIva), columnaOrNull(colPrecioTotal), columnaOrNull(colNota),
                prodExpr, join, colClave, orden);

        Map<Integer, DetalleLinea> lineasPorId = new HashMap<>();
        List<DetalleLinea> lineas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("idDetalle");
                    DetalleLinea linea = new DetalleLinea(tipo,
                            valorTexto(rs.getObject("producto")),
                            valorTexto(rs.getObject("claveProducto")),
                            valorTexto(rs.getObject("cantidad")),
                            valorTexto(rs.getObject("precioUnitario")),
                            valorTexto(rs.getObject("precioIva")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota")), id);
                    lineas.add(linea);
                    lineasPorId.put(id, linea);
                }
            }
        }
        if (detallado && !lineasPorId.isEmpty()) cargarArticulosEntrada(conn, clave, lineasPorId);
        return lineas;
    }

    private List<DetalleLinea> obtenerDetallesSalida(Connection conn, String clave, boolean detallado, String tipo) throws SQLException {
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Salida");
        Map<String, String> colsProd = obtenerColumnasCached(conn, "productos");
        Map<String, String> colsSal = obtenerColumnasCached(conn, "salidas");

        String colDetId = resolverColumna(colsDet, "idDetalleSalida", "id", "id_detalle_salida");
        String colClave = resolverColumna(colsDet, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colProd = resolverColumna(colsDet, "claveProductoSalida", "claveProducto", "idProducto", "id_producto", "producto_id");
        if (colDetId == null || colClave == null || colProd == null) return List.of();

        String colCant = resolverColumna(colsDet, "cantidad", "cantidadSalida", "cantidad_salida");
        String colPrecioUnit = resolverColumna(colsDet, "precioUnitarioSalida", "precioUnitario", "precioSalida", "precio_salida");
        String colPrecioIva = resolverColumna(colsDet, "precioIVASalida", "precioIVA", "precioIva", "precio_iva");
        String colPrecioTotal = resolverColumna(colsDet, "precioTotalSalida", "precioTotal", "precio_total");
        String colNota = resolverColumna(colsDet, "Nota", "nota", "comentario", "observaciones");
        String colEstado = resolverColumna(colsDet, "estado", "Estado");

        String colProdId = resolverColumna(colsProd, "id", "idProducto", "claveProducto");
        String colProdNom = resolverColumna(colsProd, "nombre", "Nombre", "producto");
        String colSalId = resolverColumna(colsSal, "idSalida", "id", "id_salida");
        String colTipoSal = resolverColumna(colsSal, "tipoSalida", "tipo", "tipo_salida");

        boolean hasJoinProd = colProdId != null && colProdNom != null;
        String joinProd = hasJoinProd ? "LEFT JOIN productos p ON d.`" + colProd + "` = p.`" + colProdId + "`" : "";
        String prodExpr = hasJoinProd ? "p.`" + colProdNom + "`" : "d.`" + colProd + "`";

        boolean hasJoinSal = colSalId != null && colTipoSal != null;
        String joinSal = hasJoinSal ? "LEFT JOIN salidas s ON d.`" + colClave + "` = s.`" + colSalId + "`" : "";
        String tipoExpr = hasJoinSal ? "s.`" + colTipoSal + "`" : "NULL";
        String orden = colEstado != null ? "CASE WHEN LOWER(d.`" + colEstado + "`) = 'activo' THEN 0 ELSE 1 END, " : "";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle, d.`%s` AS claveProducto, %s AS cantidad, %s AS precioUnitario,
                       %s AS precioIva, %s AS precioTotal, %s AS nota, %s AS producto, %s AS tipoSalida
                FROM detalle_Salida d %s %s WHERE d.`%s` = ?
                ORDER BY %s producto, idDetalle
                """, colDetId, colProd, columnaOrNull(colCant), columnaOrNull(colPrecioUnit),
                columnaOrNull(colPrecioIva), columnaOrNull(colPrecioTotal), columnaOrNull(colNota),
                prodExpr, tipoExpr, joinProd, joinSal, colClave, orden);

        Map<Integer, DetalleLinea> lineasPorId = new HashMap<>();
        List<DetalleLinea> lineas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("idDetalle");
                    DetalleLinea linea = new DetalleLinea(tipo,
                            valorTexto(rs.getObject("producto")),
                            valorTexto(rs.getObject("claveProducto")),
                            valorTexto(rs.getObject("cantidad")),
                            valorTexto(rs.getObject("precioUnitario")),
                            valorTexto(rs.getObject("precioIva")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota")), id);
                    linea.tipoSalida = valorTexto(rs.getObject("tipoSalida"));
                    lineas.add(linea);
                    lineasPorId.put(id, linea);
                }
            }
        }
        if (detallado && !lineasPorId.isEmpty()) cargarArticulosSalida(conn, clave, lineasPorId);
        return lineas;
    }

    private void cargarArticulosEntrada(Connection conn, String clave, Map<Integer, DetalleLinea> lineasPorId) throws SQLException {
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Entrada");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        Map<String, String> colsUbi = obtenerColumnasCached(conn, "ubicaciones");
        Map<String, String> colsProd = obtenerColumnasCached(conn, "productos");

        String colDetId = resolverColumna(colsDet, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetClave = resolverColumna(colsDet, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colDetProd = resolverColumna(colsDet, "claveProducto", "idProducto", "id_producto", "producto_id");
        String colArtDet = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        if (colDetId == null || colDetClave == null || colDetProd == null || colArtDet == null) return;

        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtLote = resolverColumna(colsArt, "lote");
        String colArtCad = resolverColumna(colsArt, "caducidad");
        String colArtUbi = resolverColumna(colsArt, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArtPres = resolverColumna(colsArt, "presentacion");
        String colArtFact = resolverColumna(colsArt, "factor");
        String colArtEstado = resolverColumna(colsArt, "Estado", "estado");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArtDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

        String colDetArtIdArticulo = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");

        String colUbiId = resolverColumna(colsUbi, "id", "idUbicacion", "ubicacion_id");
        String colUbiNom = resolverColumna(colsUbi, "nombre", "Nombre", "ubicacion");
        String colProdId = resolverColumna(colsProd, "id", "idProducto", "claveProducto");
        String colProdNom = resolverColumna(colsProd, "nombre", "Nombre", "producto");

        boolean hasJoinUbi = colArtUbi != null && colUbiId != null && colUbiNom != null;
        String joinUbi = hasJoinUbi ? "LEFT JOIN ubicaciones u ON a.`" + colArtUbi + "` = u.`" + colUbiId + "`" : "";
        String ubiExpr = hasJoinUbi ? "u.`" + colUbiNom + "`" : "NULL";

        boolean hasJoinProd = colProdId != null && colProdNom != null;
        String joinProd = hasJoinProd ? "LEFT JOIN productos p ON d.`" + colDetProd + "` = p.`" + colProdId + "`" : "";
        String prodExpr = hasJoinProd ? "p.`" + colProdNom + "`" : "d.`" + colDetProd + "`";

        String orden = colArtEstado != null ? "CASE WHEN LOWER(a.`" + colArtEstado + "`) = 'eliminado' THEN 1 ELSE 0 END, " : "";

        String exprDetalleArticuloBloqueado = "0";
        if (colArtId != null && colDetArtIdArticulo != null && colDetArtEstado != null) {
            exprDetalleArticuloBloqueado = "EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`"
                    + colDetArtIdArticulo + "` = a.`" + colArtId + "` AND LOWER(da.`"
                    + colDetArtEstado + "`) IN ('pendiente','vendido','eliminado'))";
        }

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle, %s AS idArticulo, %s AS lote, %s AS caducidad, %s AS presentacion,
                       %s AS factor, %s AS ubicacion, %s AS producto, %s AS estadoArticulo,
                       %s AS detalleEntrada, %s AS detalleSalida, %s AS tieneDetalleArticuloBloqueado
                FROM articulo a JOIN detalle_Entrada d ON a.`%s` = d.`%s` %s %s
                WHERE d.`%s` = ? ORDER BY %s producto, lote, caducidad, ubicacion, idArticulo
                """, colDetId, columnaSeguro("a", colArtId), columnaSeguro("a", colArtLote),
                columnaSeguro("a", colArtCad), columnaSeguro("a", colArtPres), columnaSeguro("a", colArtFact),
                ubiExpr, prodExpr, columnaSeguro("a", colArtEstado), columnaSeguro("a", colArtDetEnt),
                columnaSeguro("a", colArtDetSal), exprDetalleArticuloBloqueado,
                colArtDet, colDetId, joinUbi, joinProd, colDetClave, orden);

        Map<Integer, DetalleArticulo> articulosPorId = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDet = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDet);
                    if (linea == null) continue;
                    DetalleArticulo art = new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida"),
                            rs.getBoolean("tieneDetalleArticuloBloqueado"));
                    linea.articulos.add(art);
                    articulosPorId.put(art.idArticulo, art);
                }
            }
        }
        if (!articulosPorId.isEmpty()) cargarDetallesArticuloPorArticulo(conn, articulosPorId, false);
    }

    private void cargarArticulosSalida(Connection conn, String clave, Map<Integer, DetalleLinea> lineasPorId) throws SQLException {
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Salida");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        Map<String, String> colsUbi = obtenerColumnasCached(conn, "ubicaciones");
        Map<String, String> colsProd = obtenerColumnasCached(conn, "productos");

        String colDetId = resolverColumna(colsDet, "idDetalleSalida", "id", "id_detalle_salida");
        String colDetClave = resolverColumna(colsDet, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colDetProd = resolverColumna(colsDet, "claveProductoSalida", "claveProducto", "idProducto", "id_producto", "producto_id");
        String colArtDet = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        if (colDetId == null || colDetClave == null || colDetProd == null || colArtDet == null) return;

        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtLote = resolverColumna(colsArt, "lote");
        String colArtCad = resolverColumna(colsArt, "caducidad");
        String colArtUbi = resolverColumna(colsArt, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArtPres = resolverColumna(colsArt, "presentacion");
        String colArtFact = resolverColumna(colsArt, "factor");
        String colArtEstado = resolverColumna(colsArt, "Estado", "estado");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArtDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

        String colDetArtIdArticulo = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");

        String colUbiId = resolverColumna(colsUbi, "id", "idUbicacion", "ubicacion_id");
        String colUbiNom = resolverColumna(colsUbi, "nombre", "Nombre", "ubicacion");
        String colProdId = resolverColumna(colsProd, "id", "idProducto", "claveProducto");
        String colProdNom = resolverColumna(colsProd, "nombre", "Nombre", "producto");

        boolean hasJoinUbi = colArtUbi != null && colUbiId != null && colUbiNom != null;
        String joinUbi = hasJoinUbi ? "LEFT JOIN ubicaciones u ON a.`" + colArtUbi + "` = u.`" + colUbiId + "`" : "";
        String ubiExpr = hasJoinUbi ? "u.`" + colUbiNom + "`" : "NULL";

        boolean hasJoinProd = colProdId != null && colProdNom != null;
        String joinProd = hasJoinProd ? "LEFT JOIN productos p ON d.`" + colDetProd + "` = p.`" + colProdId + "`" : "";
        String prodExpr = hasJoinProd ? "p.`" + colProdNom + "`" : "d.`" + colDetProd + "`";

        String orden = colArtEstado != null ? "CASE WHEN LOWER(a.`" + colArtEstado + "`) = 'eliminado' THEN 1 ELSE 0 END, " : "";

        String exprDetalleArticuloBloqueado = "0";
        if (colArtId != null && colDetArtIdArticulo != null && colDetArtEstado != null) {
            exprDetalleArticuloBloqueado = "EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`"
                    + colDetArtIdArticulo + "` = a.`" + colArtId + "` AND LOWER(da.`"
                    + colDetArtEstado + "`) IN ('pendiente','vendido','eliminado'))";
        }

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle, %s AS idArticulo, %s AS lote, %s AS caducidad, %s AS presentacion,
                       %s AS factor, %s AS ubicacion, %s AS producto, %s AS estadoArticulo,
                       %s AS detalleEntrada, %s AS detalleSalida, %s AS tieneDetalleArticuloBloqueado
                FROM articulo a JOIN detalle_Salida d ON a.`%s` = d.`%s` %s %s
                WHERE d.`%s` = ? ORDER BY %s producto, lote, caducidad, ubicacion, idArticulo
                """, colDetId, columnaSeguro("a", colArtId), columnaSeguro("a", colArtLote),
                columnaSeguro("a", colArtCad), columnaSeguro("a", colArtPres), columnaSeguro("a", colArtFact),
                ubiExpr, prodExpr, columnaSeguro("a", colArtEstado), columnaSeguro("a", colArtDetEnt),
                columnaSeguro("a", colArtDetSal), exprDetalleArticuloBloqueado,
                colArtDet, colDetId, joinUbi, joinProd, colDetClave, orden);

        Map<Integer, DetalleArticulo> articulosPorId = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDet = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDet);
                    if (linea == null) continue;
                    DetalleArticulo art = new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida"),
                            rs.getBoolean("tieneDetalleArticuloBloqueado"));
                    linea.articulos.add(art);
                    articulosPorId.put(art.idArticulo, art);
                }
            }
        }
        if (!articulosPorId.isEmpty()) cargarDetallesArticuloPorArticulo(conn, articulosPorId, true);
        cargarArticulosSegmentadosDesdeDetalleSalida(conn, lineasPorId, articulosPorId);
    }

    private void cargarArticulosSegmentadosDesdeDetalleSalida(Connection conn, Map<Integer, DetalleLinea> lineasPorId,
                                                              Map<Integer, DetalleArticulo> articulosPorId) throws SQLException {
        if (conn == null || lineasPorId.isEmpty()) return;

        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsUbi = obtenerColumnasCached(conn, "ubicaciones");

        String colDetArtSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        if (colDetArtSal == null) return;

        String colDetArtId = resolverColumna(colsDetArt, "idDetalle", "id", "id_detalle");
        String colDetArtArt = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colDetArtLote = resolverColumna(colsDetArt, "lote");
        String colDetArtCad = resolverColumna(colsDetArt, "caducidad");
        String colDetArtPres = resolverColumna(colsDetArt, "presentacion");
        String colDetArtFact = resolverColumna(colsDetArt, "factor");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");
        String colDetArtUbi = resolverColumna(colsDetArt, "ubicacion", "idUbicacion", "id_ubicacion");

        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtLote = resolverColumna(colsArt, "lote");
        String colArtCad = resolverColumna(colsArt, "caducidad");
        String colArtPres = resolverColumna(colsArt, "presentacion");
        String colArtFact = resolverColumna(colsArt, "factor");
        String colArtEstado = resolverColumna(colsArt, "Estado", "estado");

        String colUbiId = resolverColumna(colsUbi, "id", "idUbicacion", "ubicacion_id");
        String colUbiNom = resolverColumna(colsUbi, "nombre", "Nombre", "ubicacion");

        boolean hasJoinArt = colArtId != null && colDetArtArt != null;
        String joinArt = hasJoinArt ? "LEFT JOIN articulo a ON da.`" + colDetArtArt + "` = a.`" + colArtId + "`" : "";

        boolean hasJoinUbi = colDetArtUbi != null && colUbiId != null && colUbiNom != null;
        String joinUbi = hasJoinUbi ? "LEFT JOIN ubicaciones u ON da.`" + colDetArtUbi + "` = u.`" + colUbiId + "`" : "";
        String ubiExpr = hasJoinUbi ? "u.`" + colUbiNom + "`" : "NULL";

        List<Integer> detalleSalidaIds = new ArrayList<>(lineasPorId.keySet());

        String idDetExpr = colDetArtId != null ? "da.`" + colDetArtId + "`" : "NULL";
        String idArtExpr = colDetArtArt != null ? "da.`" + colDetArtArt + "`" : "NULL";

        String sql = String.format("""
                SELECT %s AS idDetalle, da.`%s` AS idDetalleSalida, %s AS idArticulo,
                       %s AS lote, %s AS caducidad, %s AS presentacion, %s AS factor,
                       %s AS estado, %s AS ubicacion, %s AS articuloLote,
                       %s AS articuloCaducidad, %s AS articuloPresentacion,
                       %s AS articuloFactor, %s AS articuloEstado
                FROM detalleArticulo da %s %s
                WHERE da.`%s` IN (%s) ORDER BY da.`%s`
                """, idDetExpr, colDetArtSal, idArtExpr,
                columnaSeguro("da", colDetArtLote), columnaSeguro("da", colDetArtCad),
                columnaSeguro("da", colDetArtPres), columnaSeguro("da", colDetArtFact),
                columnaSeguro("da", colDetArtEstado), ubiExpr,
                columnaSeguro("a", colArtLote), columnaSeguro("a", colArtCad),
                columnaSeguro("a", colArtPres), columnaSeguro("a", colArtFact),
                columnaSeguro("a", colArtEstado), joinArt, joinUbi,
                colDetArtSal, placeholders(detalleSalidaIds.size()), colDetArtSal);

        Map<Integer, DetalleArticulo> virtualesPorDetSal = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < detalleSalidaIds.size(); i++) ps.setInt(i + 1, detalleSalidaIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer idDetSal = parseInteger(rs.getObject("idDetalleSalida"));
                    if (idDetSal == null) continue;
                    Integer idArt = parseInteger(rs.getObject("idArticulo"));
                    if (idArt != null && articulosPorId != null && articulosPorId.containsKey(idArt)) continue;

                    DetalleLinea linea = lineasPorId.get(idDetSal);
                    if (linea == null) continue;

                    String loteArt = valorTexto(rs.getObject("articuloLote"));
                    String cadArt = valorTexto(rs.getObject("articuloCaducidad"));
                    String presArt = valorTexto(rs.getObject("articuloPresentacion"));
                    String factArt = valorTexto(rs.getObject("articuloFactor"));
                    String estadoArt = valorTexto(rs.getObject("articuloEstado"));
                    if (estadoArt.isBlank()) estadoArt = "segmentado";

                    DetalleArticulo virt = virtualesPorDetSal.get(idDetSal);
                    if (virt == null) {
                        String estadoArtTmp = estadoArt;
                        if (estadoArtTmp.isBlank()) estadoArtTmp = "segmentado";
                        virt = new DetalleArticulo("", loteArt, cadArt, presArt, factArt,
                                estadoArtTmp, 0, null, idDetSal, false);
                        virtualesPorDetSal.put(idDetSal, virt);
                        linea.articulos.add(virt);
                    }

                    virt.detallesSegmentados.add(new DetalleArticuloSegmentado(
                            valorTexto(rs.getObject("idDetalle")),
                            parseInteger(rs.getObject("idArticulo")),
                            parseInteger(rs.getObject("idDetalleSalida")),
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estado")),
                            linea.precioUnitario));
                }
            }
        }
    }

    private void cargarDetallesArticuloPorArticulo(Connection conn, Map<Integer, DetalleArticulo> articulosPorId,
                                                   boolean filtrarPorDetSal) throws SQLException {
        if (articulosPorId.isEmpty()) return;

        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        Map<String, String> colsUbi = obtenerColumnasCached(conn, "ubicaciones");

        String colDetArtArt = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        if (colDetArtArt == null) return;

        String colDetArtId = resolverColumna(colsDetArt, "idDetalle", "id", "id_detalle");
        String colDetArtSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colDetArtLote = resolverColumna(colsDetArt, "lote");
        String colDetArtCad = resolverColumna(colsDetArt, "caducidad");
        String colDetArtPres = resolverColumna(colsDetArt, "presentacion");
        String colDetArtFact = resolverColumna(colsDetArt, "factor");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");
        String colDetArtUbi = resolverColumna(colsDetArt, "ubicacion", "idUbicacion", "id_ubicacion");

        String colUbiId = resolverColumna(colsUbi, "id", "idUbicacion", "ubicacion_id");
        String colUbiNom = resolverColumna(colsUbi, "nombre", "Nombre", "ubicacion");

        boolean hasJoinUbi = colDetArtUbi != null && colUbiId != null && colUbiNom != null;
        String joinUbi = hasJoinUbi ? "LEFT JOIN ubicaciones u ON da.`" + colDetArtUbi + "` = u.`" + colUbiId + "`" : "";
        String ubiExpr = hasJoinUbi ? "u.`" + colUbiNom + "`" : "NULL";

        List<Integer> articulosIds = new ArrayList<>(articulosPorId.keySet());

        String idDetExpr = colDetArtId != null ? "da.`" + colDetArtId + "`" : "NULL";
        String idDetSalExpr = colDetArtSal != null ? "da.`" + colDetArtSal + "`" : "NULL";

        String sql = String.format("""
                SELECT %s AS idDetalle, da.`%s` AS idArticulo, %s AS lote, %s AS caducidad,
                       %s AS presentacion, %s AS factor, %s AS estado, %s AS ubicacion,
                       %s AS idDetalleSalida
                FROM detalleArticulo da %s
                WHERE da.`%s` IN (%s) ORDER BY da.`%s`
                """, idDetExpr, colDetArtArt,
                columnaSeguro("da", colDetArtLote), columnaSeguro("da", colDetArtCad),
                columnaSeguro("da", colDetArtPres), columnaSeguro("da", colDetArtFact),
                columnaSeguro("da", colDetArtEstado), ubiExpr, idDetSalExpr,
                joinUbi, colDetArtArt, placeholders(articulosIds.size()), colDetArtArt);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < articulosIds.size(); i++) ps.setInt(i + 1, articulosIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer idArt = parseInteger(rs.getObject("idArticulo"));
                    if (idArt == null) continue;
                    DetalleArticulo art = articulosPorId.get(idArt);
                    if (art == null) continue;

                    if (filtrarPorDetSal && art.detalleSalidaId != null) {
                        Integer idDetSal = parseInteger(rs.getObject("idDetalleSalida"));
                        if (idDetSal == null || !art.detalleSalidaId.equals(idDetSal)) continue;
                    }

                    art.detallesSegmentados.add(new DetalleArticuloSegmentado(
                            valorTexto(rs.getObject("idDetalle")),
                            idArt,
                            parseInteger(rs.getObject("idDetalleSalida")),
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estado")),
                            ""));
                }
            }
        }
    }

    // === RENDERIZADO ===

    private void renderizarDetalles(List<DetalleLinea> lineas, boolean detallado) {
        if (contenedorDetalles == null) return;
        contenedorDetalles.getChildren().clear();
        if (lineas == null || lineas.isEmpty()) { mostrarSinDetalles(); return; }

        boolean esSalidaCancelada = false, esEntradaCancelada = false;
        if (historial != null) {
            try (Connection conn = new Conexion().conectar()) {
                if (conn != null) {
                    String mov = historial.getMovimiento(), clave = historial.getClaveMovimiento();
                    if ("Salida".equalsIgnoreCase(mov)) {
                        Integer id = parseInteger(clave);
                        if (id != null) esSalidaCancelada = "cancelado".equalsIgnoreCase(obtenerEstadoSalida(conn, id));
                    } else if ("Entrada".equalsIgnoreCase(mov)) {
                        Integer id = parseInteger(clave);
                        if (id != null) esEntradaCancelada = "cancelado".equalsIgnoreCase(obtenerEstadoEntrada(conn, id));
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        Map<String, List<DetalleLinea>> agrupadas = lineas.stream().collect(Collectors.groupingBy(l -> l.tipo, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<DetalleLinea>> entry : agrupadas.entrySet()) {
            int contador = 1;
            for (DetalleLinea linea : entry.getValue()) {
                VBox card = crearCardDetalle(linea, contador++);
                if (detallado) {
                    VBox seccionArticulos = crearSeccionArticulos(linea);
                    if (seccionArticulos != null) card.getChildren().add(seccionArticulos);
                }
                agregarBotonesAccion(card, linea);
                contenedorDetalles.getChildren().add(card);
            }
        }
    }

    private VBox crearCardDetalle(DetalleLinea linea, int contador) {
        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);

        Label titulo = new Label(contador + ". " + obtenerPrefijoTipoDetalle(linea) + valorTexto(linea.producto));
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50; -fx-padding: 0 0 5 0;");
        titulo.setWrapText(true);

        VBox detalles = new VBox(4);
        detalles.setStyle("-fx-padding: 0 0 0 18;");
        detalles.getChildren().addAll(
                crearLineaDetalle("Clave", linea.claveProducto),
                crearLineaDetalle("Cantidad", linea.cantidad),
                crearLineaDetalle("Precio unitario", linea.precioUnitario),
                crearLineaDetalle("Precio total", linea.precioTotal),
                crearLineaDetalle("Nota", linea.nota));

        card.getChildren().addAll(titulo, detalles);
        return card;
    }

    private VBox crearSeccionArticulos(DetalleLinea linea) {
        if (linea.articulos.isEmpty()) return null;

        VBox listaArticulos = new VBox(8);
        listaArticulos.setStyle("-fx-padding: 10 0 0 0;");
        Label titulo = new Label("Artículos Detallados:");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #34495e; -fx-padding: 0 0 8 0;");
        listaArticulos.getChildren().add(titulo);

        int[] idx = {1};
        boolean esEntrada = "Entrada".equalsIgnoreCase(linea.tipo);
        for (DetalleArticulo art : linea.articulos) {
            HBox cardArt = crearCardArticulo(art, idx[0]++, esEntrada, linea);
            if (cardArt != null) listaArticulos.getChildren().add(cardArt);
        }
        return listaArticulos;
    }

    private HBox crearCardArticulo(DetalleArticulo art, int index, boolean esEntrada, DetalleLinea linea) {
        if (art == null) return null;

        HBox card = new HBox(12);
        card.setStyle("-fx-padding: 14; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        card.setAlignment(Pos.CENTER_LEFT);

        HBox botones = new HBox(8);
        botones.setAlignment(Pos.CENTER_LEFT);

        boolean puedeEditar, puedeEliminar;
        if (esEntrada) {
            boolean bloqueadoPorDetalle = art.tieneDetalleArticuloPendienteOVendido();
            if (art.esSegmentado()) {
                // En entradas para segmentados los botones viven en el artículo principal
                // y se bloquean sólo con estados vendido/pendiente en sus detalles sincronizados.
                puedeEditar = art.idArticulo > 0 && !art.esPendiente() && !art.esEliminado() && !art.esVendido() && !bloqueadoPorDetalle;
                puedeEliminar = art.idArticulo > 0 && !art.esPendiente() && !art.esEliminado() && !art.esVendido() && !bloqueadoPorDetalle;
            } else {
                // Entrada: solo permitir si NO está pendiente, eliminado, vendido ni segmentado
                puedeEditar = art.idArticulo > 0 && !art.esPendiente() && !art.esEliminado() && !art.esVendido() && !art.esSegmentado();
                puedeEliminar = art.idArticulo > 0 && !art.esPendiente() && !art.esEliminado() && !art.esVendido() && !art.esSegmentado();
            }
        } else {
            // SALIDA: NUNCA mostrar botones para artículos segmentados
            // En salidas, los artículos segmentados solo deben ser informativos
            if (art.esSegmentado()) {
                // No permitir edición/eliminación de artículos segmentados en salidas
                puedeEditar = true;
                puedeEliminar = true;
            } else {
                // Para artículos NO segmentados en salidas:
                // permitir si NO está eliminado (el resto de estados se pueden editar/eliminar)
                puedeEditar = art.idArticulo > 0 && !art.esEliminado();
                puedeEliminar = art.idArticulo > 0 && !art.esEliminado();
            }
        }

        if (puedeEditar && !soloLecturaReportes) {
            botones.getChildren().add(crearBotonIcono("/img/editar.png", "Editar", e -> editarArticulo(art, esEntrada)));
        }
        if (puedeEliminar && !soloLecturaReportes) {
            botones.getChildren().add(crearBotonIcono("/img/eliminar.png", "Eliminar", e -> eliminarArticulo(art, esEntrada)));
        }

        Label numero = new Label(index + ".");
        numero.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #91d485; -fx-min-width: 25; -fx-padding: 0 5 0 0;");

        VBox info = new VBox(6);
        info.setStyle("-fx-padding: 0 0 0 10;");

        List<Node> elementosInfo = new ArrayList<>();

        elementosInfo.add(new HBox(15, crearEtiquetaDetalleElegante("Lote:", valorTexto(art.lote))));
        elementosInfo.add(new HBox(15, crearEtiquetaDetalleElegante("Presentación:", valorTexto(art.presentacion)),
                crearEtiquetaDetalleElegante("Factor:", valorTexto(art.factor))));
        elementosInfo.add(new HBox(15, crearEtiquetaDetalleElegante("Caducidad:", valorTexto(art.caducidad))));

        if (art.esDisponible()) {
            elementosInfo.add(new HBox(15, crearEtiquetaDetalleElegante("Ubicación:", valorTexto(art.ubicacion))));
        }
        elementosInfo.add(new HBox(15, crearLabelEstado(art.estado)));
        info.getChildren().addAll(elementosInfo);

        if (art.esSegmentado() && art.tieneDetallesSegmentados()) {
            CheckBox chk = new CheckBox("Mostrar detalles segmentados");
            chk.getStyleClass().add("check-detalles-segmentados");
            VBox cont = new VBox(6);
            cont.setVisible(false);
            cont.setManaged(false);
            cont.setStyle("-fx-padding: 8 0 0 6;");

            int[] idxSeg = {1};
            for (DetalleArticuloSegmentado ds : art.detallesSegmentados) {
                VBox item = new VBox(6);
                item.setStyle("-fx-background-color: #eef3f7; -fx-padding: 8; -fx-background-radius: 6;");

                HBox enc = new HBox(8);
                enc.setAlignment(Pos.CENTER_LEFT);
                Label lblDet = crearEtiquetaTituloSegmentado("Detalle #" + idxSeg[0]++);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox btnSeg = new HBox(6);
                btnSeg.setAlignment(Pos.CENTER_RIGHT);

                boolean bloqueadoEntrada = esEntrada && (ds.esVendido() || ds.esPendiente() || ds.esEliminado());
                if (!soloLecturaReportes && ds.idDetalle != null && !ds.idDetalle.isBlank() && !ds.esSegmentado() && !bloqueadoEntrada) {
                    btnSeg.getChildren().addAll(
                            crearBotonIcono("/img/editar.png", "Editar", ev -> editarDetalleArticuloSegmentadoSalida(ds, esEntrada, linea)),
                            crearBotonIcono("/img/eliminar.png", "Eliminar", ev -> eliminarDetalleArticuloSegmentadoSalida(ds, esEntrada)));
                }
                enc.getChildren().addAll(lblDet, spacer, btnSeg);

                item.getChildren().addAll(enc,
                        crearEtiquetaDetalleElegante("Precio unitario:", valorTexto(ds.precioUnitario)),
                        crearEtiquetaDetalleElegante("Ubicación:", valorTexto(ds.ubicacion)),
                        crearEtiquetaDetalleElegante("Estado:", valorTexto(ds.estado)));
                cont.getChildren().add(item);
            }

            chk.selectedProperty().addListener((o, ov, nv) -> {
                cont.setVisible(nv);
                cont.setManaged(nv);
            });
            info.getChildren().addAll(chk, cont);
        }

        card.getChildren().addAll(numero, info, botones);
        HBox.setHgrow(info, Priority.ALWAYS);
        return card;
    }

    private void agregarBotonesAccion(VBox card, DetalleLinea linea) {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setStyle("-fx-padding: 10 0 0 0;");

        boolean puedeEditarEntrada = "Entrada".equalsIgnoreCase(linea.tipo) && linea.tieneArticulosSinPendienteOVendido();
        if (puedeEditarEntrada && !soloLecturaReportes) {
            Button btn = new Button("Editar precio unitario");
            btn.setOnAction(e -> editarPrecioEntrada(linea));
            btn.getStyleClass().add("boton-formulario");
            botones.getChildren().add(btn);
        }

        boolean puedeEditarSalida = "Salida".equalsIgnoreCase(linea.tipo) && linea.esVenta() &&
                tieneArticulosODetallesEnDetalleSalida(linea.idDetalle);
        if (puedeEditarSalida && !soloLecturaReportes) {
            Button btn = new Button("Editar precio salida");
            btn.setOnAction(e -> editarPrecioSalida(linea));
            btn.getStyleClass().add("boton-formulario");
            botones.getChildren().add(btn);
        }

        if (!botones.getChildren().isEmpty()) card.getChildren().add(botones);
    }

    // === ACCIONES DE EDICIÓN ===

    private void editarPrecioEntrada(DetalleLinea linea) {
        editarPrecio(linea, true);
    }

    private void editarPrecioSalida(DetalleLinea linea) {
        editarPrecio(linea, false);
    }

    private void editarPrecio(DetalleLinea linea, boolean esEntrada) {
        if (linea == null || linea.idDetalle <= 0) return;
        BigDecimal precioActual = parseDecimal(linea.precioUnitario);
        TextField txtPrecio = new TextField(precioActual != null ? precioActual.toPlainString() : "");

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Editar precio " + (esEntrada ? "unitario" : "salida"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(new VBox(8, new Label("Precio:"), txtPrecio));

        dlg.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            BigDecimal nuevo = parseDecimal(txtPrecio.getText());
            if (nuevo == null) {
                mostrarAdvertencia("Precio inválido", "Ingresa un precio válido.");
                return;
            }
            actualizarPrecioDetalle(esEntrada ? "detalle_Entrada" : "detalle_Salida", linea, nuevo, esEntrada);
        });
    }

    private void actualizarPrecioDetalle(String tabla, DetalleLinea linea, BigDecimal nuevoPrecio, boolean esEntrada) {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return;
            Map<String, String> cols = obtenerColumnasCached(conn, tabla);
            String colId = resolverColumna(cols, esEntrada ? "idDetalleEntrada" : "idDetalleSalida", "id",
                    esEntrada ? "id_detalle_entrada" : "id_detalle_salida");
            String colPUnit = resolverColumna(cols, esEntrada ? "precioUnitario" : "precioUnitarioSalida",
                    esEntrada ? "precioEntrada" : "precioSalida", esEntrada ? "precio_entrada" : "precio_salida", "precioSalidaUnitario");
            if (colId == null || colPUnit == null) return;

            BigDecimal cantidad = parseDecimal(linea.cantidad);
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal ivaRate = obtenerTasaIva(linea.precioUnitario, linea.precioIva);
            BigDecimal precioIva = nuevoPrecio.multiply(BigDecimal.ONE.add(ivaRate)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioBruto = nuevoPrecio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioTotal = precioIva.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            String colPIva = resolverColumna(cols, esEntrada ? "precioIVA" : "precioIVASalida", "precioIva", "precio_iva");
            String colPBruto = resolverColumna(cols, esEntrada ? "precioBrutoTotal" : "precioBrutoTotalSalida", "precioBrutoTotal", "precio_bruto");
            String colPTotal = resolverColumna(cols, esEntrada ? "precioTotal" : "precioTotalSalida", "precioTotal", "precio_total");

            StringBuilder sql = new StringBuilder("UPDATE " + tabla + " SET ");
            List<Object> valores = new ArrayList<>();
            agregarCampo(sql, valores, colPUnit, nuevoPrecio.setScale(2, RoundingMode.HALF_UP));
            agregarCampo(sql, valores, colPIva, precioIva);
            agregarCampo(sql, valores, colPBruto, precioBruto);
            agregarCampo(sql, valores, colPTotal, precioTotal);
            if (valores.isEmpty()) return;
            sql.append(" WHERE `").append(colId).append("` = ?");
            valores.add(linea.idDetalle);

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < valores.size(); i++) ps.setObject(i + 1, valores.get(i));
                ps.executeUpdate();
            }

            boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
            if (esAjuste) actualizarTotalesAjustePorPrecio(conn, tabla, linea, nuevoPrecio, precioIva, cantidad, esEntrada);
            else if (esEntrada) actualizarTotalesEntradaPorPrecio(conn, linea, nuevoPrecio, precioIva, cantidad);
            else if (!esEntrada && linea.esVenta()) actualizarTotalesSalidaPorPrecio(conn, linea, nuevoPrecio, precioIva, cantidad);

            notificarActualizacion();
            cargarDetalles();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void actualizarTotalesSalidaPorPrecio(Connection conn, DetalleLinea linea, BigDecimal nuevoPUnit,
                                                  BigDecimal nuevoPIva, BigDecimal cant) throws SQLException {
        Integer salidaId = obtenerClaveMovimientoDetalle(conn, "detalle_Salida", linea.idDetalle, false);
        if (salidaId == null) return;

        BigDecimal oldPUnit = parseDecimal(linea.precioUnitario);
        BigDecimal oldPIva = parseDecimal(linea.precioIva);
        if (oldPUnit == null || oldPIva == null || cant == null) return;

        BigDecimal deltaNeto = nuevoPUnit.subtract(oldPUnit).multiply(cant).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deltaTotal = nuevoPIva.subtract(oldPIva).multiply(cant).setScale(2, RoundingMode.HALF_UP);

        Map<String, String> cols = obtenerColumnasCached(conn, "salidas");
        String colId = resolverColumna(cols, "idSalida", "id", "id_salida");
        String colNeto = resolverColumna(cols, "precioNetoSalida", "precioNeto", "precio_neto");
        String colTotal = resolverColumna(cols, "precioTotalSalida", "precioTotal", "precio_total");
        if (colId == null) return;

        BigDecimal netoAct = null, totalAct = null;
        String sql = "SELECT " + (colNeto != null ? "`" + colNeto + "`" : "NULL") + " AS neto, " +
                (colTotal != null ? "`" + colTotal + "`" : "NULL") + " AS total FROM salidas WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    netoAct = parseDecimal(rs.getObject("neto"));
                    totalAct = parseDecimal(rs.getObject("total"));
                }
            }
        }

        StringBuilder up = new StringBuilder("UPDATE salidas SET ");
        List<Object> vals = new ArrayList<>();
        if (colNeto != null && netoAct != null) {
            agregarCampo(up, vals, colNeto, netoAct.add(deltaNeto).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        if (colTotal != null && totalAct != null) {
            agregarCampo(up, vals, colTotal, totalAct.add(deltaTotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        if (vals.isEmpty()) return;
        up.append(" WHERE `").append(colId).append("` = ?");
        vals.add(salidaId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private void actualizarTotalesEntradaPorPrecio(Connection conn, DetalleLinea linea, BigDecimal nuevoPUnit,
                                                   BigDecimal nuevoPIva, BigDecimal cant) throws SQLException {
        Integer entradaId = obtenerClaveMovimientoDetalle(conn, "detalle_Entrada", linea.idDetalle, true);
        if (entradaId == null) return;

        BigDecimal oldPUnit = parseDecimal(linea.precioUnitario);
        BigDecimal oldPIva = parseDecimal(linea.precioIva);
        if (oldPUnit == null || oldPIva == null || cant == null) return;

        BigDecimal deltaNeto = nuevoPUnit.subtract(oldPUnit).multiply(cant).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deltaTotal = nuevoPIva.subtract(oldPIva).multiply(cant).setScale(2, RoundingMode.HALF_UP);

        Map<String, String> cols = obtenerColumnasCached(conn, "entradas");
        String colId = resolverColumna(cols, "idEntrada", "id", "id_entrada");
        String colNeto = resolverColumna(cols, "precioNetoEntrada", "precioNeto", "precio_neto");
        String colTotal = resolverColumna(cols, "precioTotalEntrada", "precioTotal", "precio_total");
        if (colId == null) return;

        BigDecimal netoAct = null, totalAct = null;
        String sql = "SELECT " + (colNeto != null ? "`" + colNeto + "`" : "NULL") + " AS neto, " +
                (colTotal != null ? "`" + colTotal + "`" : "NULL") + " AS total FROM entradas WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    netoAct = parseDecimal(rs.getObject("neto"));
                    totalAct = parseDecimal(rs.getObject("total"));
                }
            }
        }

        StringBuilder up = new StringBuilder("UPDATE entradas SET ");
        List<Object> vals = new ArrayList<>();
        if (colNeto != null && netoAct != null) {
            agregarCampo(up, vals, colNeto, netoAct.add(deltaNeto).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        if (colTotal != null && totalAct != null) {
            agregarCampo(up, vals, colTotal, totalAct.add(deltaTotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        if (vals.isEmpty()) return;
        up.append(" WHERE `").append(colId).append("` = ?");
        vals.add(entradaId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private Integer obtenerClaveMovimientoDetalle(Connection conn, String tabla, int detalleId, boolean esEntrada) throws SQLException {
        Map<String, String> cols = obtenerColumnasCached(conn, tabla);
        String colId = resolverColumna(cols, esEntrada ? "idDetalleEntrada" : "idDetalleSalida", "id",
                esEntrada ? "id_detalle_entrada" : "id_detalle_salida");
        String colClave = resolverColumna(cols, esEntrada ? "claveEntrada" : "claveSalida",
                esEntrada ? "idEntrada" : "idSalida", esEntrada ? "id_entrada" : "id_salida");
        if (colId == null || colClave == null) return null;
        String sql = "SELECT `" + colClave + "` FROM " + tabla + " WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? parseInteger(rs.getObject(1)) : null;
            }
        }
    }

    private void actualizarTotalesAjustePorPrecio(Connection conn, String tabla, DetalleLinea linea,
                                                  BigDecimal nuevoPUnit, BigDecimal nuevoPIva,
                                                  BigDecimal cant, boolean esEntrada) throws SQLException {
        BigDecimal oldPUnit = parseDecimal(linea.precioUnitario);
        BigDecimal oldPIva = parseDecimal(linea.precioIva);
        if (oldPUnit == null || oldPIva == null || cant == null) return;

        BigDecimal deltaNeto = nuevoPUnit.subtract(oldPUnit).multiply(cant).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deltaTotal = nuevoPIva.subtract(oldPIva).multiply(cant).setScale(2, RoundingMode.HALF_UP);
        if (!esEntrada) { deltaNeto = deltaNeto.negate(); deltaTotal = deltaTotal.negate(); }

        Integer ajusteId = obtenerClaveMovimientoDetalle(conn, tabla, linea.idDetalle, esEntrada);
        actualizarTotalesAjuste(conn, ajusteId, deltaNeto, deltaTotal);
    }

    private void actualizarTotalesAjustePorClave(Connection conn, String claveAjuste,
                                                BigDecimal deltaNeto, BigDecimal deltaTotal) throws SQLException {
        if (claveAjuste == null || claveAjuste.isBlank()) return;

        Integer ajusteNumerico = parseInteger(claveAjuste);
        if (ajusteNumerico != null && ajusteNumerico > 0) {
            actualizarTotalesAjuste(conn, ajusteNumerico, deltaNeto, deltaTotal);
            return;
        }

        Map<String, String> cols = obtenerColumnasCached(conn, "ajuste_inventario");
        String colId = resolverColumna(cols, "idAjuste", "id", "id_ajuste");
        String colNeto = resolverColumna(cols, "precioNeto", "precio_neto");
        String colTotal = resolverColumna(cols, "precioTotal", "precio_total");
        if (colId == null) return;

        BigDecimal netoAct = null, totalAct = null;
        String sql = "SELECT " + (colNeto != null ? "`" + colNeto + "`" : "NULL") + " AS neto, " +
                (colTotal != null ? "`" + colTotal + "`" : "NULL") + " AS total FROM ajuste_inventario WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveAjuste);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    netoAct = parseDecimal(rs.getObject("neto"));
                    totalAct = parseDecimal(rs.getObject("total"));
                }
            }
        }

        StringBuilder up = new StringBuilder("UPDATE ajuste_inventario SET ");
        List<Object> vals = new ArrayList<>();
        if (colNeto != null && deltaNeto != null) {
            BigDecimal base = netoAct != null ? netoAct : BigDecimal.ZERO;
            agregarCampo(up, vals, colNeto, base.add(deltaNeto).setScale(2, RoundingMode.HALF_UP));
        }
        if (colTotal != null && deltaTotal != null) {
            BigDecimal base = totalAct != null ? totalAct : BigDecimal.ZERO;
            agregarCampo(up, vals, colTotal, base.add(deltaTotal).setScale(2, RoundingMode.HALF_UP));
        }
        if (vals.isEmpty()) return;
        up.append(" WHERE `").append(colId).append("` = ?");
        vals.add(claveAjuste);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private void actualizarTotalesAjuste(Connection conn, Integer ajusteId, BigDecimal deltaNeto, BigDecimal deltaTotal) throws SQLException {
        if (ajusteId == null || ajusteId <= 0) return;
        Map<String, String> cols = obtenerColumnasCached(conn, "ajuste_inventario");
        String colId = resolverColumna(cols, "idAjuste", "id", "id_ajuste");
        String colNeto = resolverColumna(cols, "precioNeto", "precio_neto");
        String colTotal = resolverColumna(cols, "precioTotal", "precio_total");
        if (colId == null) return;

        BigDecimal netoAct = null, totalAct = null;
        String sql = "SELECT " + (colNeto != null ? "`" + colNeto + "`" : "NULL") + " AS neto, " +
                (colTotal != null ? "`" + colTotal + "`" : "NULL") + " AS total FROM ajuste_inventario WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ajusteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    netoAct = parseDecimal(rs.getObject("neto"));
                    totalAct = parseDecimal(rs.getObject("total"));
                }
            }
        }

        StringBuilder up = new StringBuilder("UPDATE ajuste_inventario SET ");
        List<Object> vals = new ArrayList<>();
        if (colNeto != null && deltaNeto != null) {
            BigDecimal base = netoAct != null ? netoAct : BigDecimal.ZERO;
            agregarCampo(up, vals, colNeto, base.add(deltaNeto).setScale(2, RoundingMode.HALF_UP));
        }
        if (colTotal != null && deltaTotal != null) {
            BigDecimal base = totalAct != null ? totalAct : BigDecimal.ZERO;
            agregarCampo(up, vals, colTotal, base.add(deltaTotal).setScale(2, RoundingMode.HALF_UP));
        }
        if (vals.isEmpty()) return;
        up.append(" WHERE `").append(colId).append("` = ?");
        vals.add(ajusteId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private boolean tieneArticulosODetallesEnDetalleSalida(int detalleSalidaId) {
        if (detalleSalidaId <= 0) return false;
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return false;
            Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
            String colArtDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            if (colArtDetSal == null) return false;
            String sql = "SELECT COUNT(*) FROM articulo WHERE `" + colArtDetSal + "` = ?";
            return ejecutarConteo(conn, sql, detalleSalidaId) > 0;
        } catch (SQLException e) { return false; }
    }

    // === EDICIÓN/ELIMINACIÓN DE ARTÍCULOS ===

    /**
     * Edita un artículo.
     * @param articulo   Artículo a editar
     * @param esEntrada  true si se está editando desde una entrada (movimiento Entrada o parte de Ajuste)
     */
    private void editarArticulo(DetalleArticulo articulo, boolean esEntrada) {
        if (articulo == null || articulo.idArticulo <= 0) return;

        if (esEntrada) {
            // Entrada: no permitir si está pendiente, eliminado o vendido.
            if (articulo.esPendiente() || articulo.esEliminado() || articulo.esVendido()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede editar un artículo con estado pendiente, eliminado o vendido en una entrada.");
                return;
            }
            if (!articulo.esSegmentado() && articulo.tieneDetalleArticuloPendienteOVendido()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede editar un artículo con detalles en estado pendiente, vendido o eliminado.");
                return;
            }
        } else {
            // SALIDA: Permitir edición incluso para artículos segmentados
            // Solo validar que no esté eliminado (los segmentados pueden editarse)
            if (articulo.esEliminado()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede editar un artículo eliminado.");
                return;
            }
        }

        boolean esSegmentado = articulo.esSegmentado();
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(esSegmentado ? "Editar artículo segmentado" : "Editar artículo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        DialogPane dp = dlg.getDialogPane();
        dp.setPadding(new Insets(0));
        dp.setStyle("-fx-background-color: white; -fx-border-color: white;");

        VBox main = new VBox();
        main.setStyle("-fx-background-color: white;");
        main.setPadding(new Insets(0));

        Label titulo = new Label(esSegmentado ? "Editar artículo segmentado" : "Editar artículo");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 0 10 0;");
        titulo.setAlignment(Pos.CENTER);
        titulo.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(titulo, new Insets(10, 0, 10, 0));

        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(0, 20, 0, 20));
        contenido.setStyle("-fx-background-color: white;");

        TextField txtLote = new TextField(valorTexto(articulo.lote));
        txtLote.setPrefWidth(180);
        DatePicker dpCad = new DatePicker();
        dpCad.setEditable(true);
        dpCad.getEditor().setDisable(false);
        dpCad.getEditor().setStyle("-fx-opacity: 1.0; -fx-background-color: white;");
        dpCad.setPromptText("yyyy/MM/dd");
        configurarDatePickerEditable(dpCad);
        String cadTexto = valorTexto(articulo.caducidad);
        if (!cadTexto.isBlank()) dpCad.setValue(parsearFechaCaducidadTexto(cadTexto));
        dpCad.setPrefWidth(180);

        HBox fila1 = new HBox(15);
        fila1.setAlignment(Pos.CENTER_LEFT);

        VBox vboxLote = new VBox(5);
        vboxLote.getChildren().addAll(new Label("Lote:"), txtLote);
        HBox.setHgrow(vboxLote, Priority.ALWAYS);

        VBox vboxCad = new VBox(5);
        vboxCad.getChildren().addAll(new Label("Caducidad:"), dpCad);
        HBox.setHgrow(vboxCad, Priority.ALWAYS);

        fila1.getChildren().addAll(vboxLote, vboxCad);

        ComboBox<String> cbPres = new ComboBox<>(PRESENTACIONES);
        cbPres.setEditable(false);
        cbPres.setPrefWidth(180);
        String presAct = valorTexto(articulo.presentacion).toLowerCase();
        for (String p : PRESENTACIONES) {
            if (p.equalsIgnoreCase(presAct)) {
                cbPres.setValue(p);
                break;
            }
        }
        if (cbPres.getValue() == null && !PRESENTACIONES.isEmpty()) {
            cbPres.setValue(PRESENTACIONES.get(0));
        }

        TextField txtFact = new TextField(valorTexto(articulo.factor));
        txtFact.setPrefWidth(180);

        HBox fila2 = new HBox(15);
        fila2.setAlignment(Pos.CENTER_LEFT);

        VBox vboxPres = new VBox(5);
        vboxPres.getChildren().addAll(new Label("Presentación:"), cbPres);
        HBox.setHgrow(vboxPres, Priority.ALWAYS);

        VBox vboxFact = new VBox(5);
        vboxFact.getChildren().addAll(new Label("Factor:"), txtFact);
        HBox.setHgrow(vboxFact, Priority.ALWAYS);

        fila2.getChildren().addAll(vboxPres, vboxFact);

        ComboBox<String> cbUbi = new ComboBox<>(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        cbUbi.setEditable(true);
        cbUbi.setPrefWidth(375);
        String ubiAct = valorTexto(articulo.ubicacion);
        if (!ubiAct.isBlank() && !"Sin ubicación".equalsIgnoreCase(ubiAct)) {
            cbUbi.setValue(ubiAct);
        }

        HBox fila3 = new HBox(15);
        fila3.setAlignment(Pos.CENTER_LEFT);

        VBox vboxUbi = new VBox(5);
        vboxUbi.getChildren().addAll(new Label("Ubicación:"), cbUbi);
        HBox.setHgrow(vboxUbi, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        fila3.getChildren().addAll(vboxUbi, spacer);

        if (esSegmentado) {
            contenido.getChildren().add(fila1);
        } else {
            contenido.getChildren().addAll(fila1, fila2, fila3);
        }

        HBox contBotones = new HBox(15);
        contBotones.setAlignment(Pos.CENTER);
        contBotones.setPadding(new Insets(20));
        contBotones.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        ButtonBar bar = (ButtonBar) dp.lookup(".button-bar");
        if (bar != null) {
            bar.setVisible(false);
            bar.setManaged(false);
            bar.setPrefHeight(0);
            bar.setMinHeight(0);
            bar.setMaxHeight(0);
        }

        Button btnOkOrig = (Button) dp.lookupButton(ButtonType.OK);
        Button btnCancelOrig = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnOkOrig != null) {
            btnOkOrig.setVisible(false);
            btnOkOrig.setManaged(false);
        }
        if (btnCancelOrig != null) {
            btnCancelOrig.setVisible(false);
            btnCancelOrig.setManaged(false);
        }

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 4;");
        btnAceptar.setPrefWidth(120);
        btnAceptar.setOnAction(e -> {
            if (!esSegmentado) {
                if (cbUbi.getValue() == null || cbUbi.getValue().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "La ubicación es requerida.");
                    return;
                }
                if (cbPres.getValue() == null || cbPres.getValue().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "La presentación es requerida.");
                    return;
                }
                if (txtFact.getText() == null || txtFact.getText().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "El factor es requerido.");
                    return;
                }
            }
            dlg.setResult(ButtonType.OK);
            dlg.close();
        });

        contBotones.getChildren().add(btnAceptar);
        main.getChildren().addAll(titulo, contenido, contBotones);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        dp.setContent(main);
        dp.setPrefWidth(380);
        dp.setPrefHeight(430);
        dlg.initModality(Modality.APPLICATION_MODAL);
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            dlg.initOwner(btnCerrar.getScene().getWindow());
        }

        dlg.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) return;

                Integer ubicacionId = null;
                if (!esSegmentado) {
                    String ubiText = cbUbi.getValue();
                    if (ubiText != null && !ubiText.isBlank()) {
                        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'")) {
                            ps.setString(1, ubiText.trim());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) ubicacionId = rs.getInt(1);
                            }
                        }
                        if (ubicacionId == null) {
                            mostrarAdvertencia("Ubicación inválida", "No se encontró la ubicación.");
                            return;
                        }
                    }
                }

                Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
                String colId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
                String colLote = resolverColumna(colsArt, "lote");
                String colCad = resolverColumna(colsArt, "caducidad");
                String colUbi = resolverColumna(colsArt, "ubicacion", "idUbicacion", "id_ubicacion");
                String colPres = resolverColumna(colsArt, "presentacion");
                String colFact = resolverColumna(colsArt, "factor");

                if (colId == null) return;

                StringBuilder sql = new StringBuilder("UPDATE articulo SET ");
                List<Object> vals = new ArrayList<>();

                agregarCampo(sql, vals, colLote, valorTexto(txtLote.getText()));

                LocalDate fechaCad = parsearFechaCaducidadEditable(dpCad);
                if (fechaCad == null && !dpCad.getEditor().getText().trim().isEmpty()) {
                    mostrarAdvertencia("Caducidad inválida", "Ingresa una fecha válida.");
                    return;
                }
                java.sql.Date fechaSQL = fechaCad != null ? java.sql.Date.valueOf(fechaCad) : null;
                agregarCampo(sql, vals, colCad, fechaSQL);

                if (!esSegmentado) {
                    agregarCampo(sql, vals, colUbi, ubicacionId);
                    agregarCampo(sql, vals, colPres, valorTexto(cbPres.getValue()));
                    Integer factor = parseInteger(txtFact.getText());
                    agregarCampo(sql, vals, colFact, factor);
                }

                if (vals.isEmpty()) return;
                sql.append(" WHERE `").append(colId).append("` = ?");
                vals.add(articulo.idArticulo);

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                    ps.executeUpdate();
                }

                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo actualizar el artículo: " + e.getMessage());
            }
        });
    }

    /**
     * Elimina un artículo (cambia su estado).
     * @param articulo   Artículo a eliminar
     * @param esEntrada  true si se está eliminando desde una entrada
     */
    private void eliminarArticulo(DetalleArticulo articulo, boolean esEntrada) {
        if (articulo == null || articulo.idArticulo <= 0) return;

        if (esEntrada) {
            // Entrada: no permitir si está pendiente, eliminado o vendido.
            if (articulo.esPendiente() || articulo.esEliminado() || articulo.esVendido()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede eliminar un artículo con estado pendiente, eliminado o vendido en una entrada.");
                return;
            }
            if (!articulo.esSegmentado() && articulo.tieneDetalleArticuloPendienteOVendido()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede eliminar un artículo con detalles en estado pendiente, vendido o eliminado.");
                return;
            }
        } else {
            // SALIDA: Permitir eliminación para artículos segmentados
            // Solo validar que no esté eliminado
            if (articulo.esEliminado()) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede eliminar un artículo eliminado.");
                return;
            }
            // Para artículos segmentados en salidas, permitimos la eliminación
        }

        // El resto del método continúa igual...
        boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());

        confirmarCancelacion("artículo", () -> {
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) return;

                Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
                String colId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
                String colEstado = resolverColumna(colsArt, "Estado", "estado");
                String colDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");

                if (colId == null || colEstado == null) return;

                // Para SALIDA, incluyendo artículos segmentados
                if (articulo.detalleSalidaId != null) {
                    // Es una salida (el artículo tiene detalleSalidaId)
                    if (colDetSal == null) return;

                    // Actualizar el estado del artículo a "disponible" y quitar la referencia a la salida
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colEstado + "` = ?, `" + colDetSal + "` = NULL WHERE `" + colId + "` = ?")) {
                        ps.setString(1, "disponible");
                        ps.setInt(2, articulo.idArticulo);
                        ps.executeUpdate();
                    }

                    // Ajustar totales de la salida
                    String claveAjuste = ajustarTotalesSalida(conn, articulo, esAjuste);

                    // Si el artículo es segmentado, también debemos marcar sus detalles como disponibles
                    if (articulo.esSegmentado() && !articulo.detallesSegmentados.isEmpty()) {
                        marcarDetallesSincronizadosDeArticulo(conn, articulo.idArticulo, "disponible", true);
                    }

                    // Reactivar el origen (detalle_Entrada asociado) tanto para artículos normales
                    // como segmentados en salidas individuales.
                    reactivarOrigenDesdeArticulo(conn, articulo.idArticulo);

                    actualizarEstadoDetalleSalidaSiVacio(conn, articulo.detalleSalidaId, !esAjuste);

                    if (esAjuste) {
                        actualizarEstadoAjusteSiVacio(conn, claveAjuste);
                    }
                } else {
                    // Es una entrada (no tiene detalleSalidaId)
                    if (articulo.esSegmentado()) {
                        // En entradas segmentadas, eliminar en el artículo principal marca como eliminado
                        // a sus detalleArticulo sincronizados.
                        marcarDetallesSincronizadosDeArticulo(conn, articulo.idArticulo, "eliminado", false);
                        actualizarEstadoEntradaPorJerarquia(conn, obtenerEntradaDesdeArticulo(conn, articulo.idArticulo));
                    } else {
                        // Es una entrada no segmentada.
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE articulo SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?")) {
                            ps.setString(1, "eliminado");
                            ps.setInt(2, articulo.idArticulo);
                            ps.executeUpdate();
                        }

                        Integer ajusteId = ajustarTotalesEntrada(conn, articulo, esAjuste);
                        actualizarEstadoDetalleEntradaSiVacio(conn, articulo.detalleEntradaId, !esAjuste);
                        if (!esAjuste) {
                            actualizarEstadoEntradaPorJerarquia(conn, obtenerEntradaDesdeArticulo(conn, articulo.idArticulo));
                        }

                        if (esAjuste) {
                            actualizarEstadoAjusteSiVacio(conn, ajusteId);
                        }
                    }
                }

                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo eliminar el artículo: " + e.getMessage());
            }
        });
    }

    private String ajustarTotalesSalida(Connection conn, DetalleArticulo art, boolean esAjuste) throws SQLException {
        if (art == null || art.detalleSalidaId == null) return null;

        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Salida");
        String colDetId = resolverColumna(colsDet, "idDetalleSalida", "id", "id_detalle_salida");
        String colClave = resolverColumna(colsDet, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colCant = resolverColumna(colsDet, "cantidad", "cantidadSalida", "cantidad_salida");
        String colPUnit = resolverColumna(colsDet, "precioUnitarioSalida", "precioUnitario", "precioSalida", "precio_salida");
        String colPIva = resolverColumna(colsDet, "precioIVASalida", "precioIVA", "precioIva", "precio_iva");
        String colPBruto = resolverColumna(colsDet, "precioBrutoTotalSalida", "precioBrutoTotal", "precio_bruto");
        String colPTotal = resolverColumna(colsDet, "precioTotalSalida", "precioTotal", "precio_total");

        if (colDetId == null || colClave == null || colCant == null || colPUnit == null || colPIva == null) return null;

        String sql = "SELECT `" + colClave + "` AS clave, `" + colCant + "` AS cant, `" + colPUnit + "` AS pUnit, `" +
                colPIva + "` AS pIva FROM detalle_Salida WHERE `" + colDetId + "` = ?";

        String claveId = null;
        BigDecimal cant = null, pUnit = null, pIva = null;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, art.detalleSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveId = Objects.toString(rs.getObject("clave"), "").trim();
                    cant = parseDecimal(rs.getObject("cant"));
                    pUnit = parseDecimal(rs.getObject("pUnit"));
                    pIva = parseDecimal(rs.getObject("pIva"));
                }
            }
        }

        if (cant == null || pUnit == null || pIva == null) return null;

        BigDecimal nuevaCant = cant.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal nuevoBruto = pUnit.multiply(nuevaCant).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoTotal = pIva.multiply(nuevaCant).setScale(2, RoundingMode.HALF_UP);

        StringBuilder up = new StringBuilder("UPDATE detalle_Salida SET ");
        List<Object> vals = new ArrayList<>();
        agregarCampo(up, vals, colCant, nuevaCant.intValue());
        agregarCampo(up, vals, colPBruto, nuevoBruto);
        agregarCampo(up, vals, colPTotal, nuevoTotal);
        up.append(" WHERE `").append(colDetId).append("` = ?");
        vals.add(art.detalleSalidaId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }

        if (claveId == null || claveId.isBlank()) return null;

        if (esAjuste) {
            actualizarTotalesAjustePorClave(conn, claveId, pUnit.negate(), pIva.negate());
        }

        return claveId;
    }

    private Integer ajustarTotalesEntrada(Connection conn, DetalleArticulo art, boolean esAjuste) throws SQLException {
        if (art == null || art.detalleEntradaId == null) return null;

        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Entrada");
        String colDetId = resolverColumna(colsDet, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClave = resolverColumna(colsDet, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colCant = resolverColumna(colsDet, "cantidad", "cantidadEntrada");
        String colPUnit = resolverColumna(colsDet, "precioUnitario", "precioEntrada", "precio_entrada");
        String colPIva = resolverColumna(colsDet, "precioIVA", "precioIva", "precio_iva");
        String colPBruto = resolverColumna(colsDet, "precioBrutoTotal", "precioBruto", "precio_bruto");
        String colPTotal = resolverColumna(colsDet, "precioTotal", "precio_total");

        if (colDetId == null || colClave == null || colCant == null || colPUnit == null || colPIva == null) return null;

        String sql = "SELECT `" + colClave + "` AS clave, `" + colCant + "` AS cant, `" + colPUnit + "` AS pUnit, `" +
                colPIva + "` AS pIva FROM detalle_Entrada WHERE `" + colDetId + "` = ?";

        Integer claveId = null;
        BigDecimal cant = null, pUnit = null, pIva = null;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, art.detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveId = parseInteger(rs.getObject("clave"));
                    cant = parseDecimal(rs.getObject("cant"));
                    pUnit = parseDecimal(rs.getObject("pUnit"));
                    pIva = parseDecimal(rs.getObject("pIva"));
                }
            }
        }

        if (cant == null || pUnit == null || pIva == null) return null;

        BigDecimal nuevaCant = cant.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal nuevoBruto = pUnit.multiply(nuevaCant).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoTotal = pIva.multiply(nuevaCant).setScale(2, RoundingMode.HALF_UP);

        StringBuilder up = new StringBuilder("UPDATE detalle_Entrada SET ");
        List<Object> vals = new ArrayList<>();
        agregarCampo(up, vals, colCant, nuevaCant.intValue());
        agregarCampo(up, vals, colPBruto, nuevoBruto);
        agregarCampo(up, vals, colPTotal, nuevoTotal);
        up.append(" WHERE `").append(colDetId).append("` = ?");
        vals.add(art.detalleEntradaId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }

        if (claveId == null || claveId <= 0) return null;

        if (esAjuste) {
            actualizarTotalesAjuste(conn, claveId, pUnit.negate(), pIva.negate());
        }

        return claveId;
    }

    private void actualizarEstadoDetalleSalidaSiVacio(Connection conn, Integer detSalId, boolean actSal) throws SQLException {
        if (detSalId == null) return;

        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Salida");
        String colDetId = resolverColumna(colsDet, "idDetalleSalida", "id", "id_detalle_salida");
        String colDetEstado = resolverColumna(colsDet, "estado", "Estado");

        if (colDetId == null || colDetEstado == null) return;

        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        String colArtDetSal = resolverColumna(colsArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

        if (colArtDetSal == null) return;

        String sqlCountArt = "SELECT COUNT(*) FROM articulo WHERE `" + colArtDetSal + "` = ?";
        int articulosActivos = ejecutarConteo(conn, sqlCountArt, detSalId);

        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        String colDetArtSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String sqlCountDetArt = (colDetArtSal == null)
                ? null
                : "SELECT COUNT(*) FROM detalleArticulo WHERE `" + colDetArtSal + "` = ?";
        int detallesActivos = (sqlCountDetArt == null) ? 0 : ejecutarConteo(conn, sqlCountDetArt, detSalId);

        if (articulosActivos > 0 || detallesActivos > 0) return;

        try (PreparedStatement ps = conn.prepareStatement("UPDATE detalle_Salida SET `" + colDetEstado + "` = ? WHERE `" + colDetId + "` = ?")) {
            ps.setString(1, "desactivado");
            ps.setInt(2, detSalId);
            ps.executeUpdate();
        }

        if (actSal) {
            String colClave = resolverColumna(colsDet, "claveSalida", "idSalida", "id_salida", "salida_id");
            if (colClave == null) return;

            String claveSalida = null;
            String sqlSalida = "SELECT `" + colClave + "` FROM detalle_Salida WHERE `" + colDetId + "` = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSalida)) {
                ps.setInt(1, detSalId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) claveSalida = Objects.toString(rs.getObject(1), "").trim();
                }
            }

            Integer salidaId = parseInteger(claveSalida);
            if (salidaId == null || salidaId <= 0) return;

            Map<String, String> colsSal = obtenerColumnasCached(conn, "salidas");
            String colSalId = resolverColumna(colsSal, "idSalida", "id", "id_salida");
            String colSalEstado = resolverColumna(colsSal, "Estado", "estado");

            if (colSalId != null && colSalEstado != null) {
                int pendientesSalida = contarDetallesSalidaConContenido(conn, salidaId, colDetId, colClave, colArtDetSal, colDetArtSal);
                if (pendientesSalida > 0) {
                    return;
                }
                String sqlUpdSal = "UPDATE salidas SET `" + colSalEstado + "` = ? WHERE `" + colSalId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdSal)) {
                    ps.setString(1, "cancelado");
                    ps.setInt(2, salidaId);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void actualizarEstadoDetalleEntradaSiVacio(Connection conn, Integer detEntId, boolean actEnt) throws SQLException {
        if (detEntId == null) return;

        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Entrada");
        String colDetId = resolverColumna(colsDet, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetEstado = resolverColumna(colsDet, "estado", "Estado");

        if (colDetId == null || colDetEstado == null) return;

        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");

        if (colArtDetEnt == null) return;

        String sqlCount = "SELECT COUNT(*) FROM articulo WHERE `" + colArtDetEnt + "` = ?";
        if (ejecutarConteo(conn, sqlCount, detEntId) > 0) return;

        try (PreparedStatement ps = conn.prepareStatement("UPDATE detalle_Entrada SET `" + colDetEstado + "` = ? WHERE `" + colDetId + "` = ?")) {
            ps.setString(1, "desactivado");
            ps.setInt(2, detEntId);
            ps.executeUpdate();
        }

        if (actEnt) {
            String colClave = resolverColumna(colsDet, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
            if (colClave == null) return;

            Integer entradaId = null;
            String sqlEnt = "SELECT `" + colClave + "` FROM detalle_Entrada WHERE `" + colDetId + "` = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlEnt)) {
                ps.setInt(1, detEntId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) entradaId = rs.getInt(1);
                }
            }

            if (entradaId == null) return;

            Map<String, String> colsEnt = obtenerColumnasCached(conn, "entradas");
            String colEntId = resolverColumna(colsEnt, "idEntrada", "id", "id_entrada");
            String colEntEstado = resolverColumna(colsEnt, "Estado", "estado");

            if (colEntId != null && colEntEstado != null) {
                actualizarEstadoEntradaPorJerarquia(conn, entradaId);
            }
        }
    }

    private void actualizarEstadoEntradaPorJerarquia(Connection conn, Integer entradaId) throws SQLException {
        if (entradaId == null || entradaId <= 0) return;

        Map<String, String> colsEnt = obtenerColumnasCached(conn, "entradas");
        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");

        String colEntId = resolverColumna(colsEnt, "idEntrada", "id", "id_entrada");
        String colEntEstado = resolverColumna(colsEnt, "Estado", "estado");
        String colDetEntId = resolverColumna(colsDetEnt, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetEntClave = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArtEstado = resolverColumna(colsArt, "Estado", "estado");
        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colDetArtIdArticulo = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colDetArtEstado = resolverColumna(colsDetArt, "estado", "Estado");

        if (colEntId == null || colEntEstado == null || colDetEntId == null || colDetEntClave == null
                || colArtDetEnt == null || colArtEstado == null) {
            return;
        }

        String sqlConteoArticulo = """
                SELECT LOWER(a.`%s`) AS estado, COUNT(*) AS total
                FROM articulo a
                INNER JOIN detalle_Entrada de ON a.`%s` = de.`%s`
                WHERE de.`%s` = ?
                  AND LOWER(a.`%s`) <> 'segmentado'
                GROUP BY LOWER(a.`%s`)
                """.formatted(colArtEstado, colArtDetEnt, colDetEntId, colDetEntClave, colArtEstado, colArtEstado);

        Map<String, Integer> conteos = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sqlConteoArticulo)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conteos.merge(textoSeguro(rs.getString("estado")).toLowerCase(Locale.ROOT), rs.getInt("total"), Integer::sum);
                }
            }
        }

        if (colArtId != null && colDetArtIdArticulo != null && colDetArtEstado != null) {
            String sqlConteoDetalle = """
                    SELECT LOWER(da.`%s`) AS estado, COUNT(*) AS total
                    FROM detalleArticulo da
                    INNER JOIN articulo a ON da.`%s` = a.`%s`
                    INNER JOIN detalle_Entrada de ON a.`%s` = de.`%s`
                    WHERE de.`%s` = ?
                      AND LOWER(a.`%s`) = 'segmentado'
                    GROUP BY LOWER(da.`%s`)
                    """.formatted(colDetArtEstado, colDetArtIdArticulo, colArtId, colArtDetEnt, colDetEntId,
                    colDetEntClave, colArtEstado, colDetArtEstado);

            try (PreparedStatement ps = conn.prepareStatement(sqlConteoDetalle)) {
                ps.setInt(1, entradaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        conteos.merge(textoSeguro(rs.getString("estado")).toLowerCase(Locale.ROOT), rs.getInt("total"), Integer::sum);
                    }
                }
            }
        }

        int disponibles = conteos.getOrDefault("disponible", 0);
        int pendientes = conteos.getOrDefault("pendiente", 0);
        int vendidos = conteos.getOrDefault("vendido", 0);
        int finalizados = conteos.getOrDefault("finalizado", 0) + conteos.getOrDefault("finalizada", 0);
        int eliminados = conteos.getOrDefault("eliminado", 0);
        int totalConsiderado = conteos.values().stream().mapToInt(Integer::intValue).sum();

        String nuevoEstado;
        if (disponibles > 0) {
            nuevoEstado = "disponible";
        } else if (totalConsiderado > 0 && vendidos > 0 && (vendidos + eliminados) == totalConsiderado) {
            nuevoEstado = "finalizado";
        } else if (pendientes > 0) {
            nuevoEstado = "pendiente";
        } else if (totalConsiderado > 0 && finalizados == totalConsiderado) {
            nuevoEstado = "finalizado";
        } else if (totalConsiderado > 0 && eliminados == totalConsiderado) {
            nuevoEstado = "cancelado";
        } else if (finalizados > 0) {
            nuevoEstado = "finalizado";
        } else {
            nuevoEstado = "cancelado";
        }

        String sqlUpd = "UPDATE entradas SET `" + colEntEstado + "` = ? WHERE `" + colEntId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpd)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, entradaId);
            ps.executeUpdate();
        }
    }

    private Integer obtenerEntradaDesdeArticulo(Connection conn, Integer articuloId) throws SQLException {
        if (articuloId == null || articuloId <= 0) return null;

        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colDetEntId = resolverColumna(colsDetEnt, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetEntClave = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");

        if (colArtId == null || colArtDetEnt == null || colDetEntId == null || colDetEntClave == null) return null;

        String sql = "SELECT de.`" + colDetEntClave + "` FROM articulo a "
                + "INNER JOIN detalle_Entrada de ON a.`" + colArtDetEnt + "` = de.`" + colDetEntId + "` "
                + "WHERE a.`" + colArtId + "` = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articuloId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return parseInteger(rs.getObject(1));
            }
        }
        return null;
    }

    private void actualizarEstadoAjusteSiVacio(Connection conn, Integer ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId <= 0) return;
        actualizarEstadoAjusteSiVacio(conn, String.valueOf(ajusteId));
    }

    private void actualizarEstadoAjusteSiVacio(Connection conn, String ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId.isBlank()) return;

        Map<String, String> colsAjuste = obtenerColumnasCached(conn, "ajuste_inventario");
        String colId = resolverColumna(colsAjuste, "idAjuste", "id", "id_ajuste");
        String colEstado = resolverColumna(colsAjuste, "estado", "Estado");
        if (colId == null || colEstado == null) return;

        int detallesActivos = 0;

        Map<String, String> colsDetSal = obtenerColumnasCached(conn, "detalle_Salida");
        String colDetSalClave = resolverColumna(colsDetSal, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colDetSalEstado = resolverColumna(colsDetSal, "estado", "Estado");
        if (colDetSalClave != null && colDetSalEstado != null) {
            String sql = "SELECT COUNT(*) FROM detalle_Salida WHERE `" + colDetSalClave + "` = ? AND LOWER(`" + colDetSalEstado + "`) <> 'desactivado'";
            detallesActivos += ejecutarConteo(conn, sql, ajusteId);
        }

        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
        String colDetEntClave = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colDetEntEstado = resolverColumna(colsDetEnt, "estado", "Estado");
        if (colDetEntClave != null && colDetEntEstado != null) {
            String sql = "SELECT COUNT(*) FROM detalle_Entrada WHERE `" + colDetEntClave + "` = ? AND LOWER(`" + colDetEntEstado + "`) <> 'desactivado'";
            detallesActivos += ejecutarConteo(conn, sql, ajusteId);
        }

        String nuevoEstado = detallesActivos > 0 ? "disponible" : "cancelado";
        try (PreparedStatement ps = conn.prepareStatement("UPDATE ajuste_inventario SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?")) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, ajusteId);
            ps.executeUpdate();
        }
    }

    private void editarDetalleArticuloSegmentadoSalida(DetalleArticuloSegmentado detalle, boolean esEntrada,
                                                       DetalleLinea linea) {
        // Si es entrada, no permitir editar si está vendido, pendiente o eliminado
        if (esEntrada) {
            if (detalle != null && (detalle.esVendido() || detalle.esPendiente() || detalle.esEliminado())) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede modificar un detalleArticulo con estado vendido, eliminado o pendiente en una entrada.");
                return;
            }
        }

        if (detalle == null || detalle.idDetalle == null || detalle.idDetalle.isBlank()) {
            mostrarAdvertencia("Acción no permitida", "Detalle inválido.");
            return;
        }
        if (detalle.esEliminado() || detalle.esSegmentado()) {
            mostrarAdvertencia("Acción no permitida", "No se puede editar un detalle segmentado eliminado o segmentado.");
            return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Editar detalle segmentado");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> cbUbi = new ComboBox<>();
        cbUbi.setEditable(false);
        TextField txtLote = new TextField(valorTexto(detalle.lote));
        DatePicker dpCad = new DatePicker();
        configurarDatePickerEditable(dpCad);
        LocalDate cadActual = parsearFechaCaducidadTexto(detalle.caducidad);
        if (cadActual != null) {
            dpCad.setValue(cadActual);
            dpCad.getEditor().setText(cadActual.format(DATE_FORMAT_SLASH));
        } else {
            dpCad.getEditor().setText(valorTexto(detalle.caducidad));
        }
        TextField txtPrecio = new TextField(valorTexto(detalle.precioUnitario));

        try (Connection conn = new Conexion().conectar()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) cbUbi.getItems().add(valorTexto(rs.getObject(1)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!valorTexto(detalle.ubicacion).isBlank()) {
            cbUbi.setValue(valorTexto(detalle.ubicacion));
        }

        dlg.getDialogPane().setContent(new VBox(8,
                new Label("Lote:"), txtLote,
                new Label("Caducidad:"), dpCad,
                new Label("Precio unitario:"), txtPrecio,
                new Label("Ubicación:"), cbUbi));

        dlg.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;

            String ubi = cbUbi.getValue();
            if (ubi == null || ubi.isBlank()) {
                mostrarAdvertencia("Campo requerido", "Selecciona una ubicación.");
                return;
            }

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) return;
                conn.setAutoCommit(false);

                Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
                String colId = resolverColumna(colsDetArt, "idDetalle", "id", "id_detalle");
                String colUbi = resolverColumna(colsDetArt, "ubicacion", "idUbicacion", "id_ubicacion");
                String colLote = resolverColumna(colsDetArt, "lote");
                String colCad = resolverColumna(colsDetArt, "caducidad");
                String colPUnitDetArt = resolverColumna(colsDetArt, "precioUnitario", "precioSalida", "precio_entrada", "precio");
                String colPIvaDetArt = resolverColumna(colsDetArt, "precioIVA", "precioIva", "precio_iva");
                String colPTotalDetArt = resolverColumna(colsDetArt, "precioTotal", "precio_total");
                String colPBrutoDetArt = resolverColumna(colsDetArt, "precioBrutoTotal", "precioBruto", "precio_bruto");

                if (colId == null || colUbi == null) {
                    mostrarAdvertencia("Error", "No se encontraron las columnas necesarias.");
                    return;
                }

                Integer idUbi = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM ubicaciones WHERE nombre = ? LIMIT 1")) {
                    ps.setString(1, ubi);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) idUbi = rs.getInt(1);
                    }
                }

                if (idUbi == null) {
                    mostrarAdvertencia("Ubicación inválida", "No se encontró la ubicación.");
                    return;
                }

                LocalDate fechaCad = parsearFechaCaducidadEditable(dpCad);
                if (fechaCad == null && !valorTexto(dpCad.getEditor().getText()).isBlank()) {
                    mostrarAdvertencia("Caducidad inválida", "Ingresa una fecha válida.");
                    conn.rollback();
                    return;
                }

                BigDecimal nuevoPrecio = parseDecimal(txtPrecio.getText());
                if (nuevoPrecio == null || nuevoPrecio.compareTo(BigDecimal.ZERO) < 0) {
                    mostrarAdvertencia("Precio inválido", "Ingresa un precio unitario válido.");
                    conn.rollback();
                    return;
                }
                nuevoPrecio = nuevoPrecio.setScale(2, RoundingMode.HALF_UP);

                BigDecimal tasaIva = obtenerTasaIva(linea != null ? linea.precioUnitario : null, linea != null ? linea.precioIva : null);
                BigDecimal precioIvaUnit = nuevoPrecio.multiply(BigDecimal.ONE.add(tasaIva)).setScale(2, RoundingMode.HALF_UP);

                Integer idDetalleSalida = detalle.detalleSalidaId != null ? detalle.detalleSalidaId : (linea != null ? linea.idDetalle : null);

                actualizarDetalleArticuloSegmentado(conn, colId, colUbi, colLote, colCad, colPUnitDetArt,
                        colPIvaDetArt, colPBrutoDetArt, colPTotalDetArt, detalle.idDetalle, idDetalleSalida,
                        idUbi, valorTexto(txtLote.getText()), fechaCad != null ? java.sql.Date.valueOf(fechaCad) : null,
                        nuevoPrecio, precioIvaUnit);

                if (!esEntrada && idDetalleSalida != null && idDetalleSalida > 0) {
                    actualizarPreciosDetalleSalidaYOrigen(conn, idDetalleSalida, nuevoPrecio, precioIvaUnit, linea);
                }

                conn.commit();

                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo actualizar el detalle: " + e.getMessage());
            }
        });
    }

    private void actualizarDetalleArticuloSegmentado(Connection conn, String colId, String colUbi, String colLote,
                                                     String colCad, String colPUnit, String colPIva,
                                                     String colPBruto, String colPTotal, String idDetalle,
                                                     Integer idDetalleSalida, Integer idUbi, String lote,
                                                     java.sql.Date caducidad, BigDecimal precioUnit, BigDecimal precioIvaUnit) throws SQLException {
        if (colId == null) return;

        String where = " WHERE `" + colId + "` = ?";
        List<Object> whereVals = new ArrayList<>();
        whereVals.add(idDetalle);

        if (idDetalleSalida != null && idDetalleSalida > 0) {
            String colDetSal = resolverColumna(obtenerColumnasCached(conn, "detalleArticulo"),
                    "idDetalleSalida", "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
            if (colDetSal != null) {
                where = " WHERE `" + colDetSal + "` = ?";
                whereVals.clear();
                whereVals.add(idDetalleSalida);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE detalleArticulo SET ");
        List<Object> vals = new ArrayList<>();
        agregarCampo(sql, vals, colUbi, idUbi);
        agregarCampo(sql, vals, colLote, lote);
        agregarCampo(sql, vals, colCad, caducidad);
        agregarCampo(sql, vals, colPUnit, precioUnit);
        agregarCampo(sql, vals, colPIva, precioIvaUnit);

        BigDecimal cantidad = BigDecimal.ONE;
        agregarCampo(sql, vals, colPBruto, precioUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP));
        agregarCampo(sql, vals, colPTotal, precioIvaUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP));
        if (vals.isEmpty()) return;

        sql.append(where);
        vals.addAll(whereVals);

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private void actualizarPreciosDetalleSalidaYOrigen(Connection conn, int idDetalleSalida,
                                                       BigDecimal nuevoPUnit, BigDecimal nuevoPIvaUnit,
                                                       DetalleLinea linea) throws SQLException {
        Map<String, String> colsDetSal = obtenerColumnasCached(conn, "detalle_Salida");
        String colIdDetSal = resolverColumna(colsDetSal, "idDetalleSalida", "id", "id_detalle_salida");
        String colCantDetSal = resolverColumna(colsDetSal, "cantidad", "cantidadSalida", "cantidad_salida");
        String colPUnitDetSal = resolverColumna(colsDetSal, "precioUnitarioSalida", "precioUnitario", "precioSalida", "precio_salida");
        String colPIvaDetSal = resolverColumna(colsDetSal, "precioIVASalida", "precioIVA", "precioIva", "precio_iva");
        String colPBrutoDetSal = resolverColumna(colsDetSal, "precioBrutoTotalSalida", "precioBrutoTotal", "precio_bruto");
        String colPTotalDetSal = resolverColumna(colsDetSal, "precioTotalSalida", "precioTotal", "precio_total");

        if (colIdDetSal != null) {
            BigDecimal cantidad = BigDecimal.ZERO;
            if (colCantDetSal != null) {
                String sqlCant = "SELECT `" + colCantDetSal + "` FROM detalle_Salida WHERE `" + colIdDetSal + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlCant)) {
                    ps.setInt(1, idDetalleSalida);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) cantidad = parseDecimal(rs.getObject(1));
                    }
                }
            }
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) cantidad = BigDecimal.ONE;

            StringBuilder upDetSal = new StringBuilder("UPDATE detalle_Salida SET ");
            List<Object> vals = new ArrayList<>();
            agregarCampo(upDetSal, vals, colPUnitDetSal, nuevoPUnit);
            agregarCampo(upDetSal, vals, colPIvaDetSal, nuevoPIvaUnit);
            agregarCampo(upDetSal, vals, colPBrutoDetSal, nuevoPUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP));
            agregarCampo(upDetSal, vals, colPTotalDetSal, nuevoPIvaUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP));
            if (!vals.isEmpty()) {
                upDetSal.append(" WHERE `").append(colIdDetSal).append("` = ?");
                vals.add(idDetalleSalida);
                try (PreparedStatement ps = conn.prepareStatement(upDetSal.toString())) {
                    for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                    ps.executeUpdate();
                }
            }
        }

        actualizarDetalleEntradaDesdeDetalleSalida(conn, idDetalleSalida, nuevoPUnit, nuevoPIvaUnit);
        if (linea != null) {
            BigDecimal cantLinea = parseDecimal(linea.cantidad);
            if (cantLinea == null || cantLinea.compareTo(BigDecimal.ZERO) <= 0) cantLinea = BigDecimal.ONE;
            actualizarTotalesSalidaPorPrecio(conn, linea, nuevoPUnit, nuevoPIvaUnit, cantLinea);
        }
    }

    private void actualizarDetalleEntradaDesdeDetalleSalida(Connection conn, int idDetalleSalida,
                                                            BigDecimal nuevoPUnit, BigDecimal nuevoPIvaUnit) throws SQLException {
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");

        String colDetArtSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colDetArtArt = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colArtDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colDetEntId = resolverColumna(colsDetEnt, "idDetalleEntrada", "id", "id_detalle_entrada");
        if (colDetArtSal == null || colDetArtArt == null || colArtId == null || colArtDetEnt == null || colDetEntId == null) return;

        Set<Integer> detallesEntrada = new HashSet<>();
        Set<Integer> entradasPadre = new HashSet<>();
        String sql = "SELECT DISTINCT a.`" + colArtDetEnt + "` AS detEnt FROM detalleArticulo da " +
                "JOIN articulo a ON a.`" + colArtId + "` = da.`" + colDetArtArt + "` WHERE da.`" + colDetArtSal + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetalleSalida);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer id = parseInteger(rs.getObject("detEnt"));
                    if (id != null && id > 0) detallesEntrada.add(id);
                }
            }
        }

        if (detallesEntrada.isEmpty()) return;

        String colCantEnt = resolverColumna(colsDetEnt, "cantidad", "cantidadEntrada");
        String colClaveEnt = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colPUnitEnt = resolverColumna(colsDetEnt, "precioUnitario", "precioEntrada", "precio_entrada");
        String colPIvaEnt = resolverColumna(colsDetEnt, "precioIVA", "precioIva", "precio_iva");
        String colPBrutoEnt = resolverColumna(colsDetEnt, "precioBrutoTotal", "precioBruto", "precio_bruto");
        String colPTotalEnt = resolverColumna(colsDetEnt, "precioTotal", "precio_total");

        for (Integer idDetEnt : detallesEntrada) {
            BigDecimal cant = BigDecimal.ONE;
            if (colCantEnt != null) {
                String sqlCant = "SELECT `" + colCantEnt + "` FROM detalle_Entrada WHERE `" + colDetEntId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlCant)) {
                    ps.setInt(1, idDetEnt);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) cant = parseDecimal(rs.getObject(1));
                    }
                }
            }
            if (cant == null || cant.compareTo(BigDecimal.ZERO) <= 0) cant = BigDecimal.ONE;

            StringBuilder upDetEnt = new StringBuilder("UPDATE detalle_Entrada SET ");
            List<Object> vals = new ArrayList<>();
            agregarCampo(upDetEnt, vals, colPUnitEnt, nuevoPUnit);
            agregarCampo(upDetEnt, vals, colPIvaEnt, nuevoPIvaUnit);
            agregarCampo(upDetEnt, vals, colPBrutoEnt, nuevoPUnit.multiply(cant).setScale(2, RoundingMode.HALF_UP));
            agregarCampo(upDetEnt, vals, colPTotalEnt, nuevoPIvaUnit.multiply(cant).setScale(2, RoundingMode.HALF_UP));

            if (!vals.isEmpty()) {
                upDetEnt.append(" WHERE `").append(colDetEntId).append("` = ?");
                vals.add(idDetEnt);
                try (PreparedStatement ps = conn.prepareStatement(upDetEnt.toString())) {
                    for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                    ps.executeUpdate();
                }
            }

            if (colClaveEnt != null) {
                String sqlClave = "SELECT `" + colClaveEnt + "` FROM detalle_Entrada WHERE `" + colDetEntId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlClave)) {
                    ps.setInt(1, idDetEnt);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Integer entradaId = parseInteger(rs.getObject(1));
                            if (entradaId != null && entradaId > 0) entradasPadre.add(entradaId);
                        }
                    }
                }
            }
        }

        for (Integer entradaId : entradasPadre) {
            recalcularTotalesEntrada(conn, entradaId);
        }
    }

    private void recalcularTotalesEntrada(Connection conn, int entradaId) throws SQLException {
        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
        String colDetClave = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colDetBruto = resolverColumna(colsDetEnt, "precioBrutoTotal", "precioBruto", "precio_bruto");
        String colDetTotal = resolverColumna(colsDetEnt, "precioTotal", "precio_total");
        if (colDetClave == null || colDetBruto == null || colDetTotal == null) return;

        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        String sqlSuma = "SELECT COALESCE(SUM(`" + colDetBruto + "`),0) AS neto, COALESCE(SUM(`" + colDetTotal + "`),0) AS total " +
                "FROM detalle_Entrada WHERE `" + colDetClave + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSuma)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    neto = parseDecimal(rs.getObject("neto"));
                    total = parseDecimal(rs.getObject("total"));
                }
            }
        }
        if (neto == null) neto = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;

        Map<String, String> colsEnt = obtenerColumnasCached(conn, "entradas");
        String colId = resolverColumna(colsEnt, "idEntrada", "id", "id_entrada");
        String colNeto = resolverColumna(colsEnt, "precioNetoEntrada", "precioNeto", "precio_neto");
        String colTotal = resolverColumna(colsEnt, "precioTotalEntrada", "precioTotal", "precio_total");
        if (colId == null) return;

        StringBuilder up = new StringBuilder("UPDATE entradas SET ");
        List<Object> vals = new ArrayList<>();
        agregarCampo(up, vals, colNeto, neto.setScale(2, RoundingMode.HALF_UP));
        agregarCampo(up, vals, colTotal, total.setScale(2, RoundingMode.HALF_UP));
        if (vals.isEmpty()) return;
        up.append(" WHERE `").append(colId).append("` = ?");
        vals.add(entradaId);

        try (PreparedStatement ps = conn.prepareStatement(up.toString())) {
            for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
            ps.executeUpdate();
        }
    }

    private void eliminarDetalleArticuloSegmentadoSalida(DetalleArticuloSegmentado detalle, boolean esEntrada) {
        // Si es entrada, no permitir eliminar si está vendido, pendiente o eliminado
        if (esEntrada) {
            if (detalle != null && (detalle.esVendido() || detalle.esPendiente() || detalle.esEliminado())) {
                mostrarAdvertencia("Acción no permitida",
                        "No se puede eliminar un detalleArticulo con estado vendido, eliminado o pendiente en una entrada.");
                return;
            }
        }

        if (detalle == null || detalle.idDetalle == null || detalle.idDetalle.isBlank()) {
            mostrarAdvertencia("Acción no permitida", "Detalle inválido.");
            return;
        }
        if (detalle.esEliminado() || detalle.esSegmentado()) {
            mostrarAdvertencia("Acción no permitida", "No se puede eliminar un detalle segmentado eliminado o segmentado.");
            return;
        }

        confirmarCancelacion("detalle segmentado", () -> {
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) return;

                Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
                String colId = resolverColumna(colsDetArt, "idDetalle", "id", "id_detalle");
                String colEstado = resolverColumna(colsDetArt, "estado", "Estado");

                if (colId == null || colEstado == null) {
                    mostrarAdvertencia("Error", "No se encontraron las columnas necesarias.");
                    return;
                }

                String colDetSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");

                Integer detalleSalidaId = obtenerDetalleSalidaDesdeDetalleArticulo(conn, detalle.idDetalle, colId, colDetSal);
                Integer articuloId = obtenerArticuloDesdeDetalleArticulo(conn, detalle.idDetalle, colId,
                        resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id"));

                // Determinar tipo real por relación en BD para evitar inconsistencias del contexto de UI.
                // Si detalleArticulo está ligado a detalleSalida => salida (disponible).
                // Si no tiene detalleSalida => entrada (eliminado).
                boolean esSalida = detalleSalidaId != null && detalleSalidaId > 0;
                String nuevoEstado = esSalida ? "disponible" : "eliminado";

                String sql = !esSalida || colDetSal == null
                        ? "UPDATE detalleArticulo SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?"
                        : "UPDATE detalleArticulo SET `" + colEstado + "` = ?, `" + colDetSal + "` = NULL WHERE `" + colId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nuevoEstado);
                    ps.setString(2, detalle.idDetalle);
                    ps.executeUpdate();
                }

                if (esSalida && articuloId != null && articuloId > 0) {
                    reactivarOrigenDesdeArticulo(conn, articuloId);
                }

                if (!esSalida && articuloId != null && articuloId > 0) {
                    Integer entradaId = obtenerEntradaDesdeArticulo(conn, articuloId);
                    actualizarEstadoEntradaPorJerarquia(conn, entradaId);
                }

                if (esSalida && detalleSalidaId != null) {
                    boolean esAjusteSalida = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
                    actualizarEstadoDetalleSalidaSiVacio(conn, detalleSalidaId, !esAjusteSalida);
                    if (esAjusteSalida) {
                        String claveAjuste = obtenerClaveSalidaDesdeDetalleSalida(conn, detalleSalidaId);
                        actualizarEstadoAjusteSiVacio(conn, claveAjuste);
                    }
                }

                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo eliminar el detalle: " + e.getMessage());
            }
        });
    }

    private void marcarDetallesSincronizadosDeArticulo(Connection conn, int idArticulo, String estado,
                                                       boolean limpiarDetalleSalida) throws SQLException {
        Map<String, String> colsDetArt = obtenerColumnasCached(conn, "detalleArticulo");
        String colIdArt = resolverColumna(colsDetArt, "idArticulo", "id_articulo", "articulo_id");
        String colEstado = resolverColumna(colsDetArt, "estado", "Estado");
        String colDetSal = resolverColumna(colsDetArt, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

        if (colIdArt == null || colEstado == null) return;

        String sql = "UPDATE detalleArticulo SET `" + colEstado + "` = ?"
                + ((limpiarDetalleSalida && colDetSal != null) ? ", `" + colDetSal + "` = NULL" : "")
                + " WHERE `" + colIdArt + "` = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idArticulo);
            ps.executeUpdate();
        }
    }

    private String obtenerClaveSalidaDesdeDetalleSalida(Connection conn, Integer detalleSalidaId) throws SQLException {
        if (detalleSalidaId == null || detalleSalidaId <= 0) return null;
        Map<String, String> colsDet = obtenerColumnasCached(conn, "detalle_Salida");
        String colDetId = resolverColumna(colsDet, "idDetalleSalida", "id", "id_detalle_salida");
        String colClave = resolverColumna(colsDet, "claveSalida", "idSalida", "id_salida", "salida_id");
        if (colDetId == null || colClave == null) return null;

        String sql = "SELECT `" + colClave + "` FROM detalle_Salida WHERE `" + colDetId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Objects.toString(rs.getObject(1), "").trim();
            }
        }
        return null;
    }

    private Integer obtenerDetalleSalidaDesdeDetalleArticulo(Connection conn, String idDetalle,
                                                             String colId, String colDetSal) throws SQLException {
        if (idDetalle == null || idDetalle.isBlank() || colId == null || colDetSal == null) return null;
        String sql = "SELECT `" + colDetSal + "` FROM detalleArticulo WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return parseInteger(rs.getObject(1));
            }
        }
        return null;
    }

    private Integer obtenerArticuloDesdeDetalleArticulo(Connection conn, String idDetalle,
                                                        String colId, String colIdArticulo) throws SQLException {
        if (idDetalle == null || idDetalle.isBlank() || colId == null || colIdArticulo == null) return null;
        String sql = "SELECT `" + colIdArticulo + "` FROM detalleArticulo WHERE `" + colId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return parseInteger(rs.getObject(1));
            }
        }
        return null;
    }

    private void reactivarOrigenDesdeArticulo(Connection conn, int idArticulo) throws SQLException {
        Map<String, String> colsArt = obtenerColumnasCached(conn, "articulo");
        String colArtId = resolverColumna(colsArt, "idArticulo", "id", "id_articulo");
        String colDetEnt = resolverColumna(colsArt, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        if (colArtId == null || colDetEnt == null) return;

        Integer detalleEntradaId = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT `" + colDetEnt + "` FROM articulo WHERE `" + colArtId + "` = ?")) {
            ps.setInt(1, idArticulo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) detalleEntradaId = parseInteger(rs.getObject(1));
            }
        }
        if (detalleEntradaId == null || detalleEntradaId <= 0) return;

        Map<String, String> colsDetEnt = obtenerColumnasCached(conn, "detalle_Entrada");
        String colDetEntId = resolverColumna(colsDetEnt, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetEntEstado = resolverColumna(colsDetEnt, "estado", "Estado");
        String colClaveEnt = resolverColumna(colsDetEnt, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        if (colDetEntId == null || colDetEntEstado == null) return;

        Integer entradaId = null;
        String sqlDet = "UPDATE detalle_Entrada SET `" + colDetEntEstado + "` = 'activo' WHERE `" + colDetEntId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDet)) {
            ps.setInt(1, detalleEntradaId);
            ps.executeUpdate();
        }
        if (colClaveEnt != null) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT `" + colClaveEnt + "` FROM detalle_Entrada WHERE `" + colDetEntId + "` = ?")) {
                ps.setInt(1, detalleEntradaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) entradaId = parseInteger(rs.getObject(1));
                }
            }
        }

        if (entradaId != null && entradaId > 0) {
            Map<String, String> colsEnt = obtenerColumnasCached(conn, "entradas");
            String colEntId = resolverColumna(colsEnt, "idEntrada", "id", "id_entrada");
            String colEntEstado = resolverColumna(colsEnt, "Estado", "estado");
            if (colEntId != null && colEntEstado != null) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE entradas SET `" + colEntEstado + "` = 'disponible' WHERE `" + colEntId + "` = ?")) {
                    ps.setInt(1, entradaId);
                    ps.executeUpdate();
                }
            }
        }
    }

    private int contarDetallesSalidaConContenido(Connection conn, Integer salidaId,
                                                 String colDetId, String colClave,
                                                 String colArtDetSal, String colDetArtSal) throws SQLException {
        if (salidaId == null || colDetId == null || colClave == null || colArtDetSal == null) return 0;

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM detalle_Salida ds WHERE ds.`")
                .append(colClave).append("` = ? AND (")
                .append("EXISTS (SELECT 1 FROM articulo a WHERE a.`").append(colArtDetSal).append("` = ds.`").append(colDetId).append("`)");

        if (colDetArtSal != null) {
            sql.append(" OR EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`").append(colDetArtSal)
                    .append("` = ds.`").append(colDetId).append("`)");
        }
        sql.append(")");

        return ejecutarConteo(conn, sql.toString(), salidaId);
    }

    @FXML
    private void descargarReporte() {
        if (historial == null || !puedeDescargarMovimiento()) {
            return;
        }

        String movimiento = textoSeguro(historial.getMovimiento()).toLowerCase(Locale.ROOT);
        String tipoMovimiento = textoSeguro(historial.getTipoMovimiento()).toLowerCase(Locale.ROOT);
        String clave = textoSeguro(historial.getClaveMovimiento());
        String comentario = textoSeguro(historial.getNota());
        Window owner = obtenerOwnerVentana();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }

            if ("entrada".equals(movimiento)) {
                if ("compra".equals(tipoMovimiento)) {
                    ReporteEntradaExporter.exportarReporteCompra(
                            clave,
                            textoSeguro(historial.getExterno()),
                            comentario,
                            construirItemsCompra(conn, clave),
                            owner
                    );
                    return;
                }

                List<Operaciones.traspasoEntrada.model.model.DetalleEntrada> detalles =
                        construirDetallesTraspasoEntrada(conn, clave);
                Map<String, List<UbicacionCompra>> ubicaciones = construirUbicacionesPorProducto(conn, clave, true);
                ReporteTraspasoExporter.exportarReporte(clave, detalles, ubicaciones, owner, comentario);
                return;
            }

            if ("salida".equals(movimiento)) {
                List<traspasoSalida> itemsSalida = construirItemsSalida(conn, clave);
                if ("venta".equals(tipoMovimiento)) {
                    ReporteSalidaExporter.exportarReporteVenta(
                            textoSeguro(historial.getFactura()),
                            textoSeguro(historial.getExterno()),
                            comentario,
                            itemsSalida,
                            owner
                    );
                } else {
                    ReporteSalidaExporter.exportarReporteTraspasoSalida(
                            clave,
                            textoSeguro(historial.getExterno()),
                            comentario,
                            itemsSalida,
                            owner
                    );
                }
                return;
            }

            if ("ajuste".equals(movimiento)) {
                ReporteAjusteExporter.exportarReporteAjuste(
                        clave,
                        comentario,
                        construirItemsAjuste(conn, clave),
                        owner
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAdvertencia("Error", "No fue posible descargar el reporte del movimiento.");
        }
    }

    private Window obtenerOwnerVentana() {
        if (root != null && root.getScene() != null) {
            return root.getScene().getWindow();
        }
        return stage;
    }

    private boolean puedeDescargarMovimiento() {
        if (historial == null) {
            return false;
        }
        return !"cancelado".equalsIgnoreCase(textoSeguro(historial.getEstado()));
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private List<compra> construirItemsCompra(Connection conn, String claveEntrada) throws SQLException {
        List<DetalleLinea> lineas = obtenerDetallesEntrada(conn, claveEntrada, true, "Entrada");
        List<compra> items = new ArrayList<>();

        for (DetalleLinea linea : lineas) {
            int cantidad = parseInteger(linea.cantidad) != null ? parseInteger(linea.cantidad) : 0;
            String lote = "";
            String caducidad = "";
            String presentacion = "";
            String factor = "";
            List<UbicacionCompra> ubicaciones = new ArrayList<>();

            if (!linea.articulos.isEmpty()) {
                DetalleArticulo articulo = linea.articulos.get(0);
                lote = textoSeguro(articulo.lote);
                caducidad = textoSeguro(articulo.caducidad);
                presentacion = textoSeguro(articulo.presentacion);
                factor = textoSeguro(articulo.factor);
                ubicaciones = agruparUbicaciones(linea.articulos);
            }

            String resumen = "";
            if (linea.claveProducto != null && !linea.claveProducto.isBlank()) {
                try {
                    GenericDAO<producto> dao = new GenericDAO<>(producto.class);
                    resumen = dao.obtenerResumenProducto(linea.claveProducto);
                    if (resumen == null) resumen = "";
                } catch (Exception e) {
                    resumen = "";
                }
            }


            compra item = new compra(
                    textoSeguro(linea.claveProducto),
                    textoSeguro(linea.producto),
                    resumen,
                    lote,
                    caducidad,
                    cantidad,
                    "",
                    presentacion,
                    factor,
                    ubicaciones,
                    textoSeguro(linea.precioUnitario),
                    textoSeguro(linea.precioIva),
                    "",
                    textoSeguro(linea.precioTotal),
                    false,
                    "",
                    textoSeguro(historial.getExterno())
            );
            item.setNota(textoSeguro(linea.nota));
            items.add(item);
        }

        return items;
    }

    private List<traspasoSalida> construirItemsSalida(Connection conn, String claveSalida) throws SQLException {
        List<DetalleLinea> lineas = obtenerDetallesSalida(conn, claveSalida, true, "Salida");
        List<traspasoSalida> items = new ArrayList<>();

        for (DetalleLinea linea : lineas) {
            int cantidad = parseInteger(linea.cantidad) != null ? parseInteger(linea.cantidad) : 0;
            String lote = "";
            String caducidad = "";
            String presentacion = "";
            int factor = 1;
            List<UbicacionCompra> ubicaciones = new ArrayList<>();

            if (!linea.articulos.isEmpty()) {
                DetalleArticulo articulo = linea.articulos.get(0);
                lote = textoSeguro(articulo.lote);
                caducidad = textoSeguro(articulo.caducidad);
                presentacion = textoSeguro(articulo.presentacion);
                Integer factorValue = parseInteger(articulo.factor);
                factor = factorValue != null ? factorValue : 1;
                ubicaciones = agruparUbicaciones(linea.articulos);
            }

            String resumen = "";
            if (linea.claveProducto != null && !linea.claveProducto.isBlank()) {
                try {
                    GenericDAO<producto> dao = new GenericDAO<>(producto.class);
                    resumen = dao.obtenerResumenProducto(linea.claveProducto);
                    if (resumen == null) resumen = "";
                } catch (Exception e) {
                    resumen = "";
                }
            }

            traspasoSalida item = new traspasoSalida(
                    textoSeguro(linea.claveProducto),
                    textoSeguro(linea.producto),
                    resumen,
                    lote,
                    caducidad,
                    cantidad,
                    presentacion,
                    factor,
                    ubicaciones,
                    textoSeguro(linea.precioUnitario),
                    textoSeguro(linea.precioIva),
                    "",
                    textoSeguro(linea.precioTotal)
            );
            item.setNota(textoSeguro(linea.nota));
            items.add(item);
        }

        return items;
    }

    private List<Operaciones.traspasoEntrada.model.model.DetalleEntrada> construirDetallesTraspasoEntrada(Connection conn, String claveEntrada) throws SQLException {
        List<DetalleLinea> lineas = obtenerDetallesEntrada(conn, claveEntrada, false, "Entrada");
        List<Operaciones.traspasoEntrada.model.model.DetalleEntrada> detalles = new ArrayList<>();
        String sucursal = textoSeguro(historial.getExterno());

        for (DetalleLinea linea : lineas) {
            detalles.add(new Operaciones.traspasoEntrada.model.model.DetalleEntrada(
                    textoSeguro(linea.claveProducto),
                    textoSeguro(linea.producto),
                    textoSeguro(linea.cantidad),
                    textoSeguro(linea.precioUnitario),
                    textoSeguro(linea.precioTotal),
                    sucursal
            ));
        }

        return detalles;
    }

    private List<Object> construirItemsAjuste(Connection conn, String claveAjuste) throws SQLException {
        List<Object> items = new ArrayList<>();
        items.addAll(construirItemsCompra(conn, claveAjuste));
        items.addAll(construirItemsSalida(conn, claveAjuste));
        return items;
    }

    private Map<String, List<UbicacionCompra>> construirUbicacionesPorProducto(Connection conn,
                                                                                String clave,
                                                                                boolean entrada) throws SQLException {
        Map<String, List<UbicacionCompra>> mapa = new HashMap<>();
        List<DetalleLinea> lineas = entrada
                ? obtenerDetallesEntrada(conn, clave, true, "Entrada")
                : obtenerDetallesSalida(conn, clave, true, "Salida");

        for (DetalleLinea linea : lineas) {
            List<UbicacionCompra> ubicaciones = agruparUbicaciones(linea.articulos);
            if (!ubicaciones.isEmpty()) {
                mapa.put(textoSeguro(linea.claveProducto), ubicaciones);
            }
        }

        return mapa;
    }

    private List<UbicacionCompra> agruparUbicaciones(List<DetalleArticulo> articulos) {
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleArticulo articulo : articulos) {
            String nombreUbicacion = textoSeguro(articulo.ubicacion);
            if (nombreUbicacion.isBlank()) {
                continue;
            }
            cantidades.put(nombreUbicacion, cantidades.getOrDefault(nombreUbicacion, 0) + 1);
        }

        List<UbicacionCompra> ubicaciones = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cantidades.entrySet()) {
            ubicaciones.add(new UbicacionCompra(entry.getKey(), entry.getValue()));
        }
        return ubicaciones;
    }

    // === UTILIDADES ===

    private void actualizarEstadoUI() {

        if (lblTitulo != null) {
            lblTitulo.setText(historial == null ? "Detalles" : "Detalles - " + historial.getMovimiento());
        }

        if (btnCancelar == null) return;

        boolean mostrarCancelar = false, mostrarEntrada = false;
        String textoBoton = "Cancelar";

        if (historial != null) {
            try (Connection conn = new Conexion().conectar()) {
                if (conn != null) {
                    String mov = historial.getMovimiento();
                    String clave = historial.getClaveMovimiento();

                    if ("Salida".equalsIgnoreCase(mov)) {
                        Integer id = parseInteger(clave);
                        if (id != null && id > 0 && puedeCancelarSalida(conn, id)) {
                            mostrarCancelar = true;
                            textoBoton = "Cancelar salida";
                        }
                    } else if ("Ajuste".equalsIgnoreCase(mov)) {
                        if (clave != null && !clave.isBlank() && puedeCancelarAjuste(conn, clave)) {
                            mostrarCancelar = true;
                            textoBoton = "Cancelar ajuste";
                        }
                    }

                    if ("Entrada".equalsIgnoreCase(mov) && btnCancelarEntrada != null) {
                        Integer id = parseInteger(clave);
                        if (id != null && id > 0 && puedeCancelarEntrada(conn, id)) {
                            mostrarEntrada = true;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        boolean habilitarDescarga = puedeDescargarMovimiento();
        if (btnDescargar != null) {
            btnDescargar.setVisible(habilitarDescarga);
            btnDescargar.setManaged(habilitarDescarga);
            btnDescargar.setDisable(!habilitarDescarga);
        }

        if (soloLecturaReportes) {
            btnCancelar.setVisible(false);
            btnCancelar.setManaged(false);
            btnCancelar.setDisable(true);
            if (btnCancelarEntrada != null) {
                btnCancelarEntrada.setVisible(false);
                btnCancelarEntrada.setManaged(false);
                btnCancelarEntrada.setDisable(true);
            }
            return;
        }

        btnCancelar.setText(textoBoton);
        btnCancelar.setVisible(mostrarCancelar);
        btnCancelar.setManaged(mostrarCancelar);
        btnCancelar.setDisable(!mostrarCancelar);

        if (btnCancelarEntrada != null) {
            btnCancelarEntrada.setVisible(mostrarEntrada);
            btnCancelarEntrada.setManaged(mostrarEntrada);
            btnCancelarEntrada.setDisable(!mostrarEntrada);
        }
    }

    private boolean esMovimientoValido(String tipo) {
        return historial != null && tipo.equalsIgnoreCase(historial.getMovimiento());
    }

    private void confirmarCancelacion(String objeto, Runnable accion) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar " + objeto);
        confirm.setHeaderText(null);
        confirm.setContentText("¿Deseas cancelar el/la " + objeto + " seleccionado?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) accion.run();
        });
    }

    private void setProcesandoCancelacion(boolean enProceso) {
        procesandoCancelacion = enProceso;
        if (btnCancelar != null) btnCancelar.setDisable(enProceso);
        if (btnCancelarEntrada != null) btnCancelarEntrada.setDisable(enProceso);
        if (btnCerrar != null) btnCerrar.setDisable(enProceso);
        if (root != null) root.setDisable(enProceso);
    }

    private void mostrarCargandoCancelacion() {
        if (overlayCargaGlobal != null) overlayCargaGlobal.mostrar();
        if (overlayCarga != null) overlayCarga.mostrar();
    }

    private void ocultarCargandoCancelacion() {
        if (overlayCarga != null) overlayCarga.ocultar();
        if (overlayCargaGlobal != null) overlayCargaGlobal.ocultar();
    }

    private void finalizarCancelacionConExito(String msg) {
        finalizarCancelacionSinMensaje();
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Cancelación");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.showAndWait();
        cerrarVentana();
    }

    private void finalizarCancelacionSinMensaje() {
        ocultarCargandoCancelacion();
        setProcesandoCancelacion(false);
    }

    private void notificarActualizacion() {
        if (onRefresh != null) Platform.runLater(onRefresh);
    }

    private void mostrarSinDetalles() {
        Platform.runLater(() -> {
            if (contenedorDetalles != null)
                contenedorDetalles.getChildren().setAll(new Label("Sin detalles disponibles."));
        });
    }

    private void mostrarAdvertencia(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private String obtenerPrefijoTipoDetalle(DetalleLinea linea) {
        if (linea == null || historial == null || !"Ajuste".equalsIgnoreCase(historial.getMovimiento())) return "";
        if ("Entrada".equalsIgnoreCase(linea.tipo)) return "[Entrada] ";
        if ("Salida".equalsIgnoreCase(linea.tipo)) return "[Salida] ";
        return "";
    }

    private BigDecimal obtenerTasaIva(String pUnitStr, String pIvaStr) {
        BigDecimal pUnit = parseDecimal(pUnitStr);
        BigDecimal pIva = parseDecimal(pIvaStr);
        if (pUnit == null || pIva == null || pUnit.compareTo(BigDecimal.ZERO) == 0) return IvaConfigService.getIvaTasa();
        BigDecimal tasa = pIva.divide(pUnit, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        return tasa.compareTo(BigDecimal.ZERO) < 0 ? IvaConfigService.getIvaTasa() : tasa;
    }

    private List<String> obtenerUbicacionesActivas() {
        List<String> ubi = new ArrayList<>();
        try (Connection conn = new Conexion().conectar()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) ubi.add(rs.getString("nombre"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ubi;
    }

    private Button crearBotonIcono(String icono, String fallback, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button();
        try {
            ImageView img = new ImageView(new Image(getClass().getResourceAsStream(icono)));
            img.setFitWidth(14);
            img.setFitHeight(14);
            btn.setGraphic(img);
        } catch (Exception e) {
            btn.setText(fallback);
        }
        btn.setStyle("-fx-background-color: #333; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4;");
        btn.setOnAction(handler);
        return btn;
    }

    private Label crearEtiquetaTituloSegmentado(String titulo) {
        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        return lbl;
    }

    private Label crearEtiquetaDetalleElegante(String titulo, String valor) {
        Label lbl = new Label(titulo + " " + (valor.isEmpty() ? "N/A" : valor));
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        return lbl;
    }

    private Label crearLabelEstado(String estado) {
        Label lbl = new Label("Estado: " + valorTexto(estado));
        String color = "#333";
        if (estado != null) {
            String low = estado.toLowerCase();
            if ("disponible".equals(low)) color = "#91d485";
            else if ("pendiente".equals(low)) color = "#e74c3c";
            else if ("vendido".equals(low)) color = "#333";
            else if ("eliminado".equals(low)) color = "#333";
            else if ("ajustado".equals(low)) color = "#333";
        }
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 12;");
        return lbl;
    }

    private Label crearLineaDetalle(String titulo, String valor) {
        return new Label(titulo + ": " + valorTexto(valor));
    }

    private String valorTexto(Object o) {
        if (o == null) return "";
        String s = o.toString();
        return s.isBlank() ? "" : s;
    }

    private String columnaOrNull(String col) {
        return col != null ? "d.`" + col + "`" : "NULL";
    }

    private String columnaSeguro(String alias, String col) {
        return col != null ? alias + ".`" + col + "`" : "NULL";
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        return parseDecimal(o.toString());
    }

    private Integer parseInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        return parseInteger(o.toString());
    }

    private LocalDate parsearFechaCaducidadTexto(String texto) {
        String limpio = valorTexto(texto).trim();
        if (limpio.isEmpty()) return null;
        try { return LocalDate.parse(limpio, DATE_FORMAT_SLASH); }
        catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(limpio, DATE_FORMAT_GUION); }
        catch (DateTimeParseException ignored) {}
        return null;
    }

    private LocalDate parsearFechaCaducidadEditable(DatePicker dp) {
        if (dp == null) return null;
        String txt = dp.getEditor() != null ? valorTexto(dp.getEditor().getText()).trim() : "";
        if (txt.isEmpty()) {
            dp.setValue(null);
            return null;
        }
        LocalDate fecha = parsearFechaCaducidadTexto(txt);
        if (fecha != null) dp.setValue(fecha);
        return fecha;
    }

    private void configurarDatePickerEditable(DatePicker dp) {
        if (dp == null) return;
        dp.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate d) {
                return d != null ? d.format(DATE_FORMAT_SLASH) : "";
            }
            @Override
            public LocalDate fromString(String s) {
                return parsearFechaCaducidadTexto(s);
            }
        });
    }

    // === CLASES INTERNAS ===

    private static class DetalleLinea {
        final String tipo, producto, claveProducto, cantidad, precioUnitario, precioIva, precioTotal, nota;
        final int idDetalle;
        final List<DetalleArticulo> articulos = new ArrayList<>();
        String tipoSalida;

        DetalleLinea(String t, String prod, String clave, String cant, String pUnit, String pIva,
                     String pTotal, String n, int id) {
            tipo = t;
            producto = prod;
            claveProducto = clave;
            cantidad = cant;
            precioUnitario = pUnit;
            precioIva = pIva;
            precioTotal = pTotal;
            nota = n;
            idDetalle = id;
        }

        boolean esVenta() {
            return tipoSalida != null && "venta".equalsIgnoreCase(tipoSalida.trim());
        }

        boolean tieneArticulosDisponibles() {
            return !articulos.isEmpty() && articulos.stream().allMatch(DetalleArticulo::esDisponible);
        }

        boolean tieneArticulosSinPendienteOVendido() {
            return !articulos.isEmpty() && articulos.stream().allMatch(a ->
                    !a.esPendiente() && !a.esVendido() && (a.esDisponible() || a.esAjustado()));
        }
    }

    private static class DetalleArticulo {
        final String ubicacion, lote, caducidad, presentacion, factor, estado;
        final int idArticulo;
        final Integer detalleEntradaId, detalleSalidaId;
        final boolean tieneDetalleArticuloPendienteOVendido;
        final List<DetalleArticuloSegmentado> detallesSegmentados = new ArrayList<>();

        DetalleArticulo(String ubi, String l, String cad, String pres, String fac, String est,
                        int id, Object detEnt, Object detSal, boolean tiene) {
            ubicacion = ubi;
            lote = l;
            caducidad = cad;
            presentacion = pres;
            factor = fac;
            estado = est;
            idArticulo = id;
            detalleEntradaId = parseInteger(detEnt);
            detalleSalidaId = parseInteger(detSal);
            tieneDetalleArticuloPendienteOVendido = tiene;
        }

        boolean esDisponible() { return "disponible".equalsIgnoreCase(estado); }
        boolean esPendiente() { return "pendiente".equalsIgnoreCase(estado); }
        boolean esVendido() { return "vendido".equalsIgnoreCase(estado); }
        boolean esAjustado() { return "ajustado".equalsIgnoreCase(estado); }
        boolean esEliminado() { return "eliminado".equalsIgnoreCase(estado); }
        boolean esSegmentado() { return "segmentado".equalsIgnoreCase(estado); }
        boolean esDetalleEntrada() { return detalleEntradaId != null; }
        boolean esDetalleSalida() { return detalleSalidaId != null; }
        boolean tieneDetalleArticuloPendienteOVendido() { return tieneDetalleArticuloPendienteOVendido; }
        boolean tieneDetallesSegmentados() { return !detallesSegmentados.isEmpty(); }

        static Integer parseInteger(Object valor) {
            if (valor == null) return null;
            if (valor instanceof Number) return ((Number) valor).intValue();
            String texto = valor.toString();
            if (texto.isBlank()) return null;
            try { return Integer.valueOf(texto.trim()); }
            catch (NumberFormatException e) { return null; }
        }
    }

    private static class DetalleArticuloSegmentado {
        final String idDetalle, ubicacion, lote, caducidad, presentacion, factor, estado, precioUnitario;
        final Integer idArticulo, detalleSalidaId;

        DetalleArticuloSegmentado(String id, Integer idArt, Integer detSal, String u, String l, String cad,
                                  String p, String f, String e, String precioUnit) {
            idDetalle = id;
            idArticulo = idArt;
            detalleSalidaId = detSal;
            ubicacion = u;
            lote = l;
            caducidad = cad;
            presentacion = p;
            factor = f;
            estado = e;
            precioUnitario = precioUnit;
        }

        boolean esEliminado() { return "eliminado".equalsIgnoreCase(estado); }
        boolean esSegmentado() { return "segmentado".equalsIgnoreCase(estado); }
        boolean esPendiente() { return "pendiente".equalsIgnoreCase(estado); }
        boolean esVendido() { return "vendido".equalsIgnoreCase(estado); }
    }
}
