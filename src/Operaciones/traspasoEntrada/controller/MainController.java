package Operaciones.traspasoEntrada.controller;

import Compartido.exportar.ReporteTraspasoExporter;
import Compartido.helper.OverlayCarga;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.SelectorOrdenPopup;
import Compartido.helper.AtajosTecladoHelper;
import Formularios.controller.ControllerUbicacionTraspaso;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import Operaciones.traspasoEntrada.model.traspasoEntrada;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainController implements ControladorVista {

    // =========================
    // FXML
    // =========================
    @FXML private TableColumn<traspasoEntrada, Void> colDesplegar;
    @FXML private ComboBox<String> miComboBox;
    @FXML private CheckBox miCheckBox;
    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<traspasoEntrada> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblOrdenar;
    @FXML private Region expansor;
    @FXML private TableColumn<traspasoEntrada, Boolean> colSelect;
    @FXML private TableColumn<traspasoEntrada, String> colClaveEntrada;
    @FXML private TableColumn<traspasoEntrada, String> colFecha;
    @FXML private TableColumn<traspasoEntrada, String> colHora;
    @FXML private TableColumn<traspasoEntrada, String> colTotal;
    @FXML private TableColumn<traspasoEntrada, String> colNombreSucural;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    // =========================
    // Estado / datos
    // =========================
    private String criterioOrden = "id";
    private String direccionOrden = "asc";

    private final ObservableList<traspasoEntrada> entradasTraspasoOriginal = FXCollections.observableArrayList();
    private final ObservableList<traspasoEntrada> entradasTraspaso = FXCollections.observableArrayList();

    private final model modeloTraspaso = new model();

    private Image flechaDerechaImage;
    private Image flechaAbajoImage;

    private static final String SUF_DETALLE  = "_DETALLE_";
    private static final String SUF_HEADER   = "_ENCABEZADO_";
    private static final String SUF_CARGANDO = "_CARGANDO_";

    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_HEADER = "-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;";
    private static final String BORDE_COLOR  = "#4A4848";

    private enum RowKind { NORMAL, DETALLE, HEADER, CARGANDO }

    // Estado desplegado
    private final Map<String, Boolean> filasDesplegadas = new ConcurrentHashMap<>();
    // Cache de detalles por entrada
    private final Map<String, List<traspasoEntrada>> detallesPorEntrada = new ConcurrentHashMap<>();
    // Cargas detalle en progreso
    private final Map<String, Task<List<traspasoEntrada>>> cargasDetalle = new ConcurrentHashMap<>();
    // Pre-carga
    private Task<Void> precargaDetallesTask;

    // Cache de nombres de producto (evita consultas en UI)
    private final Map<String, String> nombreProductoCache = new ConcurrentHashMap<>();
    private final Map<String, Task<String>> cargasNombreProducto = new ConcurrentHashMap<>();

    private OverlayCarga overlayCarga;

    private boolean actualizandoEntrada = false;
    private List<traspasoEntrada> pendientesEnEspera;
    private String estadoEntradaEnEspera;
    private String estadoArticulosEnEspera;

    // Para mantener conteo y titulo correcto al continuar
    private List<String> clavesEnProceso = Collections.emptyList();

    // Ejecutores
    private final ExecutorService fxExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            daemonFactory("fx-bg")
    );
    private final ExecutorService prefetchExecutor = Executors.newFixedThreadPool(2, daemonFactory("fx-prefetch"));

    private static ThreadFactory daemonFactory(final String prefix) {
        return new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName(prefix + "-" + t.getId());
                return t;
            }
        };
    }

    // =========================
    // Init
    // =========================
    @FXML
    public void initialize() {
        miComboBox.setItems(FXCollections.observableArrayList("Aceptar", "Rechazar"));
        miComboBox.setValue("Opciones");

        cargarImagenesFlecha();

        Platform.runLater(new Runnable() {
            @Override public void run() {
                bindLayout();

                overlayCarga = new OverlayCarga(root, new Pane());

                configurarTabla();
                cargarTablaAsincrona();
                configurarAtajosTeclado();

                RefrescoHelper.setVistaActual("traspasoEntrada");
                RefrescoHelper.registrarRefresco("traspasoEntrada", new Runnable() {
                    @Override public void run() { actualizarTraspasoEntrada(); }
                });
            }
        });
    }

    private void bindLayout() {
        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

        HBox.setHgrow(expansor, Priority.ALWAYS);
        expansor.setMinWidth(10);
        lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);

        contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
        contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));
    }

    // =========================
    // Async helper
    // =========================
    private <T> void runAsync(final Callable<T> background,
                              final Consumer<T> onSuccessFxThread,
                              final Consumer<Throwable> onErrorFxThread) {
        Task<T> task = new Task<T>() {
            @Override protected T call() throws Exception { return background.call(); }
        };
        task.setOnSucceeded(e -> onSuccessFxThread.accept(task.getValue()));
        task.setOnFailed(e -> onErrorFxThread.accept(task.getException()));
        fxExecutor.execute(task);
    }

    // =========================
    // Refresco / carga principal
    // =========================
    private void actualizarTraspasoEntrada() {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                contenidoTabla.getSelectionModel().clearSelection();
                if (miCheckBox != null) miCheckBox.setSelected(false);
                if (miComboBox != null) miComboBox.setValue("Opciones");
                cancelarProcesosEnCurso();
                limpiarCachesYTablas();
            }
        });
        cargarTablaAsincrona();
    }

    private void cancelarProcesosEnCurso() {
        if (precargaDetallesTask != null) precargaDetallesTask.cancel();
        for (Task<List<traspasoEntrada>> t : cargasDetalle.values()) t.cancel();
        cargasDetalle.clear();

        for (Task<String> t : cargasNombreProducto.values()) t.cancel();
        cargasNombreProducto.clear();
    }

    private void limpiarCachesYTablas() {
        filasDesplegadas.clear();
        detallesPorEntrada.clear();
        nombreProductoCache.clear();

        entradasTraspasoOriginal.clear();
        entradasTraspaso.clear();
        contenidoTabla.refresh();
    }

    private void cargarTablaAsincrona() {
        runAsync(
                new Callable<List<traspasoEntrada>>() {
                    @Override public List<traspasoEntrada> call() { return modeloTraspaso.obtenerPendientes(); }
                },
                new Consumer<List<traspasoEntrada>>() {
                    @Override public void accept(List<traspasoEntrada> datos) {
                        entradasTraspasoOriginal.setAll(datos);

                        filasDesplegadas.clear();
                        detallesPorEntrada.clear();

                        for (Task<List<traspasoEntrada>> t : cargasDetalle.values()) t.cancel();
                        cargasDetalle.clear();

                        if (datos == null || datos.isEmpty()) {
                            entradasTraspaso.clear();
                            contenidoTabla.refresh();
                            return;
                        }

                        aplicarOrdenamiento();
                        iniciarPrecargaDetalles(datos);
                    }
                },
                new Consumer<Throwable>() {
                    @Override public void accept(Throwable ex) {
                        ex.printStackTrace();
                        mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar los traspasos: " + ex.getMessage());
                    }
                }
        );
    }

    // =========================
    // Row helpers
    // =========================
    private static String s(String v) { return v == null ? "" : v; }

    private RowKind kind(traspasoEntrada e) {
        String k = (e == null) ? null : e.getClaveEntrada();
        if (k == null) return RowKind.NORMAL;
        if (k.contains(SUF_CARGANDO)) return RowKind.CARGANDO;
        if (k.contains(SUF_HEADER)) return RowKind.HEADER;
        if (k.contains(SUF_DETALLE)) return RowKind.DETALLE;
        return RowKind.NORMAL;
    }

    private boolean isNormalRow(traspasoEntrada e) { return kind(e) == RowKind.NORMAL; }

    // =========================
    // Expansión rápida de detalles
    // =========================
    private void toggleFilaDesplegada(String claveEntrada) {
        if (claveEntrada == null || claveEntrada.trim().isEmpty()) return;

        boolean desplegada = filasDesplegadas.getOrDefault(claveEntrada, false);
        filasDesplegadas.put(claveEntrada, !desplegada);

        if (desplegada) {
            cancelarCargaDetalle(claveEntrada);
            contraerFila(claveEntrada);
            removerFilaCargando(claveEntrada);
            contenidoTabla.refresh();
            return;
        }

        List<traspasoEntrada> cache = detallesPorEntrada.get(claveEntrada);
        if (cache != null && !cache.isEmpty()) {
            insertarDetallesEnTabla(claveEntrada, cache);
            contenidoTabla.refresh();
            return;
        }

        insertarFilaCargando(claveEntrada);
        contenidoTabla.refresh();
        cargarDetallesEnSegundoPlano(claveEntrada);
    }

    private void cancelarCargaDetalle(String claveEntrada) {
        Task<List<traspasoEntrada>> t = cargasDetalle.remove(claveEntrada);
        if (t != null) t.cancel();
    }

    private void cargarDetallesEnSegundoPlano(final String claveEntrada) {
        if (cargasDetalle.containsKey(claveEntrada)) return;

        final Task<List<traspasoEntrada>> task = new Task<List<traspasoEntrada>>() {
            @Override protected List<traspasoEntrada> call() {
                List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
                return construirFilasDetalle(claveEntrada, detalles);
            }
        };

        task.setOnSucceeded(e -> {
            List<traspasoEntrada> filasDetalle = task.getValue();
            cargasDetalle.remove(claveEntrada);

            if (filasDetalle != null) detallesPorEntrada.put(claveEntrada, filasDetalle);

            removerFilaCargando(claveEntrada);
            if (filasDesplegadas.getOrDefault(claveEntrada, false) && filasDetalle != null && !filasDetalle.isEmpty()) {
                insertarDetallesEnTabla(claveEntrada, filasDetalle);
            }
            contenidoTabla.refresh();
        });

        task.setOnFailed(e -> {
            cargasDetalle.remove(claveEntrada);
            removerFilaCargando(claveEntrada);
            contenidoTabla.refresh();
        });

        cargasDetalle.put(claveEntrada, task);
        fxExecutor.execute(task);
    }

    private void insertarFilaCargando(String claveEntrada) {
        removerFilaCargando(claveEntrada);

        traspasoEntrada loading = new traspasoEntrada("", "Cargando...", "", "", "");
        loading.setClaveEntrada(claveEntrada + SUF_CARGANDO + "1");
        loading.setSeleccionado(false);

        int idx = indiceEntradaPadre(claveEntrada);
        if (idx >= 0) entradasTraspaso.add(idx + 1, loading);
    }

    private void removerFilaCargando(String claveEntrada) {
        final String prefix = claveEntrada + SUF_CARGANDO;
        entradasTraspaso.removeIf(e -> e != null && s(e.getClaveEntrada()).startsWith(prefix));
    }

    private int indiceEntradaPadre(String claveEntrada) {
        for (int i = 0; i < entradasTraspaso.size(); i++) {
            traspasoEntrada e = entradasTraspaso.get(i);
            if (e != null && claveEntrada.equals(e.getClaveEntrada())) return i;
        }
        return -1;
    }

    private void contraerFila(String claveEntrada) {
        List<traspasoEntrada> filasDetalle = detallesPorEntrada.get(claveEntrada);
        if (filasDetalle != null && !filasDetalle.isEmpty()) entradasTraspaso.removeAll(filasDetalle);
    }

    private void insertarDetallesEnTabla(String claveEntrada, List<traspasoEntrada> filasDetalle) {
        entradasTraspaso.removeAll(filasDetalle); // evita doble inserción
        int idx = indiceEntradaPadre(claveEntrada);
        if (idx >= 0) entradasTraspaso.addAll(idx + 1, filasDetalle);
    }

    private List<traspasoEntrada> construirFilasDetalle(String claveEntrada, List<model.DetalleEntrada> detalles) {
        if (detalles == null || detalles.isEmpty()) return Collections.emptyList();

        List<traspasoEntrada> filas = new ArrayList<>(detalles.size() + 1);

        traspasoEntrada header = new traspasoEntrada("Id", "Producto", "Cantidad", "Precio Unitario", "Precio Total");
        header.setClaveEntrada(claveEntrada + SUF_HEADER + "1");
        header.setSeleccionado(false);
        filas.add(header);

        int n = 1;
        for (model.DetalleEntrada d : detalles) {
            String idInterno = claveEntrada + SUF_DETALLE + (n++);
            traspasoEntrada fila = new traspasoEntrada(
                    idInterno,            // claveEntrada interno
                    d.getClaveProducto(), // fecha => ID Producto
                    d.getCantidad(),      // hora  => Cantidad
                    d.getPrecioUnitario(),// total => Precio Unitario
                    d.getPrecioTotal()    // nombreSucursal => Precio Total
            );
            fila.setSeleccionado(false);
            filas.add(fila);

            precargarNombreProducto(d.getClaveProducto());
        }
        return filas;
    }

    // =========================
    // Cache nombres de producto (sin freeze)
    // =========================
    private void precargarNombreProducto(final String idProducto) {
        if (idProducto == null || idProducto.trim().isEmpty() || nombreProductoCache.containsKey(idProducto)) return;

        cargasNombreProducto.computeIfAbsent(idProducto, key -> {
            final Task<String> t = new Task<String>() {
                @Override protected String call() { return modeloTraspaso.obtenerNombreProducto(key); }
            };

            t.setOnSucceeded(e -> {
                String v = s(t.getValue());
                nombreProductoCache.put(key, v.isEmpty() ? key : v);
                cargasNombreProducto.remove(key);
                Platform.runLater(() -> contenidoTabla.refresh());
            });

            t.setOnFailed(e -> {
                nombreProductoCache.putIfAbsent(key, key);
                cargasNombreProducto.remove(key);
                Platform.runLater(() -> contenidoTabla.refresh());
            });

            prefetchExecutor.execute(t);
            return t;
        });
    }

    // =========================
    // Precarga de detalles (expansión instantánea)
    // =========================
    private void iniciarPrecargaDetalles(final List<traspasoEntrada> entradas) {
        if (entradas == null || entradas.isEmpty()) return;
        if (precargaDetallesTask != null && precargaDetallesTask.isRunning()) return;

        precargaDetallesTask = new Task<Void>() {
            @Override protected Void call() {
                for (traspasoEntrada entrada : entradas) {
                    if (isCancelled()) break;
                    if (entrada == null) continue;

                    String clave = entrada.getClaveEntrada();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    if (detallesPorEntrada.containsKey(clave) || cargasDetalle.containsKey(clave)) continue;

                    try {
                        List<model.DetalleEntrada> det = modeloTraspaso.obtenerDetallesEntrada(clave);
                        detallesPorEntrada.put(clave, construirFilasDetalle(clave, det));
                    } catch (Exception ignored) { }
                }
                return null;
            }
        };

        prefetchExecutor.execute(precargaDetallesTask);
    }

    // =========================
    // Tabla / UI
    // =========================
    private void cargarImagenesFlecha() {
        try {
            flechaDerechaImage = new Image(getClass().getResourceAsStream("/img/flecha-derecha.png"));
            flechaAbajoImage = new Image(getClass().getResourceAsStream("/img/flecha-abajo.png"));
            if (flechaAbajoImage.isError()) flechaAbajoImage = flechaDerechaImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        contenidoTabla.setSortPolicy(param -> false);

        configurarColumnaFlecha();

        colSelect.setCellValueFactory(param -> {
            traspasoEntrada item = param.getValue();
            return (item != null && isNormalRow(item)) ? item.seleccionadoProperty() : new SimpleBooleanProperty(false);
        });

        colSelect.setCellFactory(param -> new CheckBoxTableCell<traspasoEntrada, Boolean>() {
            @Override public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                traspasoEntrada rowData = getTableRow() == null ? null : getTableRow().getItem();
                if (rowData != null && !isNormalRow(rowData)) setGraphic(null);
            }
        });

        colClaveEntrada.setCellValueFactory(new PropertyValueFactory<>("claveEntrada"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colNombreSucural.setCellValueFactory(new PropertyValueFactory<>("nombreSucursal"));

        contenidoTabla.setEditable(true);
        contenidoTabla.setItems(entradasTraspaso);

        for (TableColumn<traspasoEntrada, ?> c : new TableColumn[]{ colDesplegar, colSelect, colClaveEntrada, colFecha, colHora, colTotal, colNombreSucural }) {
            c.setStyle(STYLE_CENTER);
        }

        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (traspasoEntrada e : entradasTraspaso) if (e != null && isNormalRow(e)) e.setSeleccionado(newVal);
        });

        configurarCellFactories();
    }

    private void configurarColumnaFlecha() {
        colDesplegar.setCellFactory(param -> new TableCell<traspasoEntrada, Void>() {
            private final ImageView iv = new ImageView();
            private final Button btn = new Button();

            {
                iv.setFitHeight(20);
                iv.setFitWidth(20);
                iv.setPreserveRatio(true);

                btn.setGraphic(iv);
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                btn.setOnAction(e -> {
                    traspasoEntrada item = getTableRow() == null ? null : getTableRow().getItem();
                    if (item != null && isNormalRow(item)) toggleFilaDesplegada(item.getClaveEntrada());
                });
                setAlignment(Pos.CENTER);
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                traspasoEntrada row = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (row == null || !isNormalRow(row)) { setGraphic(null); return; }

                boolean desplegada = filasDesplegadas.getOrDefault(row.getClaveEntrada(), false);
                iv.setImage(desplegada ? flechaAbajoImage : flechaDerechaImage);
                setGraphic(btn);
            }
        });

        colDesplegar.setPrefWidth(40);
        colDesplegar.setResizable(false);
        colDesplegar.setSortable(false);
    }

    private void setBaseCell(TableColumn<traspasoEntrada, String> col,
                             final String headerText,
                             final String loadingText,
                             final String borderStyle,
                             final BiFunction<traspasoEntrada, String, String> detalleTextFn) {
        col.setCellFactory(c -> new TableCell<traspasoEntrada, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                traspasoEntrada row = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (row == null) { setText(null); setStyle(""); return; }

                RowKind k = kind(row);

                if (k == RowKind.CARGANDO) {
                    setText(loadingText);
                    setStyle(loadingText == null || loadingText.isEmpty()
                            ? STYLE_CENTER
                            : (STYLE_CENTER + " -fx-font-style: italic;"));
                    return;
                }

                if (k == RowKind.HEADER) {
                    setText(headerText);
                    setStyle(STYLE_HEADER);
                    return;
                }

                if (k == RowKind.DETALLE) {
                    setText(detalleTextFn.apply(row, item));
                    setStyle(STYLE_CENTER + " " + borderStyle);
                    return;
                }

                setText(item);
                setStyle(STYLE_CENTER);
            }
        });
    }

    private void configurarCellFactories() {
        final String bordeId      = "-fx-border-color: " + BORDE_COLOR + "; -fx-border-width: 0 1 1 1;";
        final String bordeDetalle = "-fx-border-color: " + BORDE_COLOR + "; -fx-border-width: 0 1 1 0;";

        // "Id" -> en detalle muestra row.getFecha() (ID del producto)
        setBaseCell(colClaveEntrada, "Id", "", bordeId, (row, item) -> s(row.getFecha()));

        // "Producto" -> cache + hilo
        colFecha.setCellFactory(c -> new TableCell<traspasoEntrada, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                traspasoEntrada row = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (row == null) { setText(null); setStyle(""); return; }

                RowKind k = kind(row);

                if (k == RowKind.CARGANDO) {
                    setText("Cargando...");
                    setStyle(STYLE_CENTER + " -fx-font-style: italic;");
                    return;
                }

                if (k == RowKind.HEADER) {
                    setText("Producto");
                    setStyle(STYLE_HEADER);
                    return;
                }

                if (k == RowKind.DETALLE) {
                    String idProducto = row.getFecha();
                    String nombre = nombreProductoCache.get(idProducto);

                    if (nombre == null || nombre.trim().isEmpty()) {
                        setText("...");
                        precargarNombreProducto(idProducto);
                    } else {
                        setText(nombre);
                    }

                    setStyle(STYLE_CENTER + " " + bordeDetalle);
                    return;
                }

                setText(item);
                setStyle(STYLE_CENTER);
            }
        });

        // Cantidad / Precio Unitario / Precio Total -> en detalle el item ya es lo correcto
        setBaseCell(colHora, "Cantidad", "", bordeDetalle, (row, item) -> s(item));
        setBaseCell(colTotal, "Precio Unitario", "", bordeDetalle, (row, item) -> s(item));
        setBaseCell(colNombreSucural, "Precio Total", "", bordeDetalle, (row, item) -> s(item));
    }

    // =========================
    // Ordenamiento
    // =========================
    private void aplicarOrdenamiento() {
        List<traspasoEntrada> lista = new ArrayList<>(entradasTraspasoOriginal);

        final Function<traspasoEntrada, String> keyExtractor;
        if ("fecha".equals(criterioOrden)) {
            keyExtractor = e -> s(e.getFecha());
        } else if ("sucursal".equals(criterioOrden)) {
            keyExtractor = e -> s(e.getNombreSucursal()).toLowerCase();
        } else { // "id" default
            keyExtractor = e -> s(e.getClaveEntrada()).toLowerCase();
        }

        Comparator<traspasoEntrada> cmp = Comparator.comparing(keyExtractor);
        if ("desc".equalsIgnoreCase(direccionOrden)) cmp = cmp.reversed();

        lista.sort(cmp);
        actualizarTablaConOrdenamiento(lista);
    }

    private void actualizarTablaConOrdenamiento(List<traspasoEntrada> listaOrdenada) {
        entradasTraspaso.clear();

        for (traspasoEntrada e : listaOrdenada) {
            entradasTraspaso.add(e);

            String clave = e.getClaveEntrada();
            if (filasDesplegadas.getOrDefault(clave, false)) {
                List<traspasoEntrada> det = detallesPorEntrada.get(clave);
                if (det != null && !det.isEmpty()) entradasTraspaso.addAll(det);
            }
        }

        contenidoTabla.refresh();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = Arrays.asList("id", "fecha", "sucursal");

        SelectorOrdenPopup.mostrar(
                (Node) event.getSource(),
                event.getScreenX(),
                event.getScreenY(),
                criterios,
                criterioOrden,
                direccionOrden,
                seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                }
        );
    }

    // =========================
    // Acciones aceptar / rechazar
    // =========================
    @FXML
    public void aplicarAccion() {
        String opcion = miComboBox.getValue();
        if (opcion == null || opcion.equalsIgnoreCase("Opciones")) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona una opción", "Debes seleccionar Aceptar o Rechazar.");
            return;
        }

        List<traspasoEntrada> seleccionados = obtenerSeleccionados();
        if (seleccionados.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Se debe seleccionar alguna entrada.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas continuar?");
        confirmacion.setContentText("Se aplicarán cambios a las entradas seleccionadas.");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) procesarSeleccion(opcion, seleccionados);
        });
    }

    private void procesarSeleccion(String opcion, List<traspasoEntrada> seleccionados) {
        List<String> claves = new ArrayList<>(seleccionados.size());
        for (traspasoEntrada e : seleccionados) claves.add(e.getClaveEntrada());

        boolean rechazar = "Rechazar".equalsIgnoreCase(opcion);
        String nuevoEstadoEntrada = rechazar ? "rechazado" : "disponible";
        String nuevoEstadoArticulos = rechazar ? "rechazado" : "disponible";

        if (rechazar) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Confirmar eliminación");
            a.setHeaderText("¿Estás seguro de rechazar las entradas seleccionadas?");
            a.setContentText("Se eliminarán " + seleccionados.size() + " entrada(s).\nEsta acción no se puede deshacer.");

            ButtonType si = new ButtonType("Sí, rechazar");
            ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(si, cancelar);

            a.showAndWait().ifPresent(r -> { if (r == si) ejecutarActualizacionAsync(claves, nuevoEstadoEntrada, nuevoEstadoArticulos, seleccionados.size()); });
        } else {
            abrirFormularioUbicaciones(seleccionados, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
        }
    }

    private void ejecutarActualizacionAsync(List<String> claves, String nuevoEstadoEntrada, String nuevoEstadoArticulos, int cantidad) {
        runAsync(
                () -> modeloTraspaso.actualizarEstadoEntradas(claves, nuevoEstadoEntrada, nuevoEstadoArticulos),
                actualizado -> {
                    if (!Boolean.TRUE.equals(actualizado)) {
                        mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron actualizar las entradas.");
                        return;
                    }

                    entradasTraspasoOriginal.removeIf(e -> claves.contains(e.getClaveEntrada()));
                    actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));
                    miCheckBox.setSelected(false);

                    String msg = "rechazado".equals(nuevoEstadoEntrada)
                            ? "Se eliminaron " + cantidad + " entrada(s) correctamente."
                            : "Se aceptaron " + cantidad + " entrada(s) correctamente.";

                    mostrarAlerta(Alert.AlertType.INFORMATION, "Operación exitosa", msg);
                },
                ex -> mostrarAlerta(Alert.AlertType.ERROR, "Error", "Fallo actualizando: " + ex.getMessage())
        );
    }

    private List<traspasoEntrada> obtenerSeleccionados() {
        List<traspasoEntrada> out = new ArrayList<>();
        for (traspasoEntrada e : entradasTraspaso) if (e != null && isNormalRow(e) && e.isSeleccionado()) out.add(e);
        return out;
    }

    // =========================
    // Formulario ubicaciones
    // =========================
    private void abrirFormularioUbicaciones(List<traspasoEntrada> seleccionados,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        clavesEnProceso = (claves == null) ? Collections.emptyList() : new ArrayList<>(claves);
        procesarSiguienteUbicacion(new ArrayList<>(seleccionados), clavesEnProceso, nuevoEstadoEntrada, nuevoEstadoArticulos);
    }

    private void procesarSiguienteUbicacion(List<traspasoEntrada> pendientes,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        if (pendientes == null || pendientes.isEmpty()) {
            Platform.runLater(() -> {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Proceso completado", "Se han procesado todos los traspasos seleccionados.");
                cargarTablaAsincrona();
            });
            return;
        }

        traspasoEntrada actual = pendientes.remove(0);
        String claveEntrada = actual.getClaveEntrada();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/ubicacionTraspaso.fxml"));
            Parent viewRoot = loader.load();

            ControllerUbicacionTraspaso controller = loader.getController();
            controller.setClaveEntrada(claveEntrada);

            controller.setOnConfirmCallback(ubicacionesPorProducto ->
                    actualizarEntradaIndividual(claveEntrada, nuevoEstadoEntrada, nuevoEstadoArticulos, pendientes, ubicacionesPorProducto)
            );

            Stage stage = new Stage();
            controller.setStage(stage);

            Scene scene = new Scene(viewRoot, 500, 700);
            stage.setScene(scene);

            int total = claves == null ? 0 : claves.size();
            int procesados = total - pendientes.size();
            stage.setTitle("Ubicaciones - " + claveEntrada + " (" + procesados + "/" + total + ")");

            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(contenidoTabla.getScene().getWindow());
            stage.setResizable(false);
            stage.centerOnScreen();

            stage.setOnHidden(e -> {
                if (actualizandoEntrada) {
                    pendientesEnEspera = new ArrayList<>(pendientes);
                    estadoEntradaEnEspera = nuevoEstadoEntrada;
                    estadoArticulosEnEspera = nuevoEstadoArticulos;
                    return;
                }

                if (!pendientes.isEmpty()) {
                    PauseTransition pause = new PauseTransition(Duration.millis(300));
                    pause.setOnFinished(ev -> procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos));
                    pause.play();
                } else {
                    cargarTablaAsincrona();
                }
            });

            stage.show();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el formulario de ubicaciones para: " + claveEntrada);
            e.printStackTrace();

            if (pendientes != null && !pendientes.isEmpty()) {
                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(ev -> procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos));
                pause.play();
            }
        }
    }

    private void actualizarEntradaIndividual(String claveEntrada,
                                             String nuevoEstadoEntrada,
                                             String nuevoEstadoArticulos,
                                             List<traspasoEntrada> pendientes,
                                             Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {

        actualizandoEntrada = true;
        overlayCarga.mostrar();

        runAsync(
                () -> {
                    model.ResultadoOperacion resultado = modeloTraspaso.actualizarUbicacionesYEstados(
                            claveEntrada,
                            ubicacionesPorProducto,
                            nuevoEstadoEntrada,
                            nuevoEstadoArticulos
                    );

                    List<model.DetalleEntrada> detalles = resultado.isExito()
                            ? modeloTraspaso.obtenerDetallesEntrada(claveEntrada)
                            : null;

                    return new Object[]{resultado, detalles};
                },
                payload -> {
                    model.ResultadoOperacion resultado = (model.ResultadoOperacion) payload[0];
                    @SuppressWarnings("unchecked")
                    List<model.DetalleEntrada> detalles = (List<model.DetalleEntrada>) payload[1];

                    overlayCarga.ocultar();

                    if (resultado.isExito()) {
                        entradasTraspasoOriginal.removeIf(e -> claveEntrada.equals(e.getClaveEntrada()));
                        actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));
                        mostrarConfirmacionReporte(claveEntrada, resultado.getMensaje(), detalles, ubicacionesPorProducto);
                    } else {
                        mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo actualizar el traspaso: " + resultado.getMensaje());
                    }

                    actualizandoEntrada = false;
                    continuarPendientes(pendientes, nuevoEstadoEntrada, nuevoEstadoArticulos);
                },
                ex -> {
                    overlayCarga.ocultar();
                    actualizandoEntrada = false;
                    continuarPendientes(pendientes, nuevoEstadoEntrada, nuevoEstadoArticulos);
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "Fallo actualizando traspaso: " + ex.getMessage());
                }
        );
    }

    private void mostrarConfirmacionReporte(String claveEntrada,
                                            String mensaje,
                                            List<model.DetalleEntrada> detalles,
                                            Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {

        String comentario = modeloTraspaso.obtenerComentarioEntrada(claveEntrada);

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Registro exitoso");
        dialogo.setHeaderText(mensaje);
        dialogo.setContentText("¿Deseas descargar el reporte de este traspaso?");

        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                ReporteTraspasoExporter.exportarReporte(
                        claveEntrada,
                        detalles,
                        ubicacionesPorProducto,
                        (contenidoTabla != null && contenidoTabla.getScene() != null) ? contenidoTabla.getScene().getWindow() : null,
                        comentario
                );
            }
        });
    }

    private void continuarPendientes(List<traspasoEntrada> pendientes,
                                     String nuevoEstadoEntrada,
                                     String nuevoEstadoArticulos) {

        List<traspasoEntrada> pendientesFinal = (pendientesEnEspera != null) ? pendientesEnEspera : pendientes;
        String estadoEntradaFinal = (pendientesEnEspera != null) ? estadoEntradaEnEspera : nuevoEstadoEntrada;
        String estadoArticulosFinal = (pendientesEnEspera != null) ? estadoArticulosEnEspera : nuevoEstadoArticulos;

        pendientesEnEspera = null;
        estadoEntradaEnEspera = null;
        estadoArticulosEnEspera = null;

        if (pendientesFinal != null && !pendientesFinal.isEmpty()) {
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            List<traspasoEntrada> copia = new ArrayList<>(pendientesFinal);
            pause.setOnFinished(e -> procesarSiguienteUbicacion(copia, clavesEnProceso, estadoEntradaFinal, estadoArticulosFinal));
            pause.play();
        } else {
            cargarTablaAsincrona();
        }
    }

    // =========================
    // Utilidades
    // =========================
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.A) {
                if (miCheckBox != null) miCheckBox.setSelected(true);
            } else if (event.getCode() == KeyCode.G) {
                aplicarAccion();
            } else {
                return;
            }
            event.consume();
        });
    }
}
