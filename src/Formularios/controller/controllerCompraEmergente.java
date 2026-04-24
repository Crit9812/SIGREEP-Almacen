package Formularios.controller;

import Compartido.controller.productoCboxController;
import Compartido.model.IvaConfigService;
import Formularios.model.modelNuevoTraspasoSalida;
import Formularios.utilities.helperCompraEmergente;
import Operaciones.compra.controller.MainController;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.compra.model.model;
import conexion.conexionFTP;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class controllerCompraEmergente {

    @FXML private VBox contenedorUbicaciones;
    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtDescripcion;
    @FXML private ImageView previewImage;
    @FXML private TextField txtLote;
    @FXML private DatePicker dpCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;
    @FXML private TextField txtNota;
    @FXML private TextField txtPrecioEntrada;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private String prefillPresentacion;
    private String prefillFactor;
    private static final int MAX_FILAS = 10;
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final model modeloCompras = new model();

    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private ObservableList<compra> itemsCompra;
    private MainController mainController;
    private productoCboxController productoController;

    private String proveedorId;
    private String proveedorNombre;

    private boolean inicializado = false;
    private compra itemParaEditar;
    private boolean cargandoEdicion = false;

    private String ultimoIdProductoDescripcion = "";
    private boolean seleccionarClaveAlternaPendiente = false;
    private String tituloFormulario = "Compra";

    // Cache para imágenes (ya existía). Se mantiene.
    private final Map<String, Image> cacheImagenes = new HashMap<>();

    // Ubicaciones dinámicas
    private final List<UbicacionRow> filasUbicacion = new ArrayList<>();
    private int contadorFilas = 0;

    // Ajuste inventario
    private boolean modoAjusteInventario = false;
    private final modelNuevoTraspasoSalida modeloPreciosAjuste = new modelNuevoTraspasoSalida();

    // Prefill
    private String prefillProductoId;
    private String prefillProductoNombre;
    private String prefillDescripcion;
    private String prefillUbicacion;

    // ==========
    // Optimización BD/Async:
    // Evita resultados tardíos sobrescribiendo el UI (race conditions) y reduce llamadas repetidas.
    // ==========
    private final AtomicLong tokenPrecio = new AtomicLong(0);
    private final AtomicLong tokenImagen = new AtomicLong(0);
    private final AtomicLong tokenUbicaciones = new AtomicLong(0);

    // Cache simple para precio último producto (reduce hits a BD cuando el usuario cambia y regresa)
    private final Map<String, BigDecimal> cachePrecioUltimoProducto = new HashMap<>();

    // Debounce para ajuste inventario (evita disparar consulta por cada tecla en lote/factor)
    private PauseTransition debouncePrecioAjuste;
    private PauseTransition debounceLoteDuplicado;
    private String ultimoAvisoDuplicado = "";

    @FXML
    public void initialize() {
        if (lblTitulo != null) lblTitulo.setText(tituloFormulario);

        productoController = new productoCboxController();
        if (proveedorId != null && !proveedorId.isBlank()) {
            productoController.inicializarConProveedor(cbClaveProducto, cbProductoNombre, cbClaveAlterna, proveedorId);
        } else {
            productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);
        }

        configurarPresentaciones();
        configurarEventos();
        configurarValidaciones();
        configurarCalculoPrecios();
        configurarCamposLectura();
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnter();
        configurarSeleccionClaveAlternaPorDefecto();
        inicializarUbicacionesDinamicas();

        // Carga ubicaciones (BD) optimizada con token para evitar estados inconsistentes
        cargarUbicacionesDesdeBD();

        inicializado = true;

        if (itemParaEditar != null) cargarItemParaEditar();
        aplicarPrefill();

        Platform.runLater(() -> {
            if (cbClaveProducto != null) cbClaveProducto.requestFocus();
        });
    }

    // ==========================
    // SETTERS
    // ==========================

    public void setPresentacionPrefill(String presentacion) {
        this.prefillPresentacion = presentacion;
        if (inicializado) aplicarPrefillPresentacionFactor();
    }

    public void setFactorPrefill(String factor) {
        this.prefillFactor = factor;
        if (inicializado) aplicarPrefillPresentacionFactor();
    }

    public void setItemsCompra(ObservableList<compra> itemsCompra) {
        this.itemsCompra = itemsCompra;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setTituloFormulario(String tituloFormulario) {
        if (tituloFormulario == null || tituloFormulario.isBlank()) return;
        this.tituloFormulario = tituloFormulario;
        if (lblTitulo != null) lblTitulo.setText(tituloFormulario);
    }

    public void setProveedorSeleccionado(String proveedorId, String proveedorNombre) {
        this.proveedorId = proveedorId;
        this.proveedorNombre = proveedorNombre;

        if (inicializado && productoController != null) {
            productoController.recargarConProveedor(proveedorId);
        }
    }

    public void setItemParaEditar(compra item) {
        this.itemParaEditar = item;
        if (inicializado) cargarItemParaEditar();
    }

    public void setModoAjusteInventario(boolean modoAjusteInventario) {
        this.modoAjusteInventario = modoAjusteInventario;
    }

    public void setProductoPrefill(String idProducto, String nombreProducto, String descripcion) {
        this.prefillProductoId = idProducto;
        this.prefillProductoNombre = nombreProducto;
        this.prefillDescripcion = descripcion;
        if (inicializado) aplicarPrefill();
    }

    public void setUbicacionPrefill(String ubicacion) {
        this.prefillUbicacion = ubicacion;
        if (inicializado) aplicarPrefillUbicacion();
    }

    // ==========================
    // PREFILL
    // ==========================
    private void aplicarPrefillProducto() {
        if (prefillProductoId == null || prefillProductoNombre == null || productoController == null) return;
        intentarAplicarSeleccion(8);
    }

    private void intentarAplicarSeleccion(int intentosRestantes) {
        boolean seleccionado = productoController.setSeleccion(prefillProductoId, prefillProductoNombre);
        if (seleccionado) {
            if (txtDescripcion != null && prefillDescripcion != null && !prefillDescripcion.isBlank()) {
                txtDescripcion.setText(prefillDescripcion);
            }
            return;
        }
        if (intentosRestantes <= 0) {
            if (cbClaveProducto != null) cbClaveProducto.setValue(prefillProductoId);
            if (cbProductoNombre != null) cbProductoNombre.setValue(prefillProductoNombre);
            if (txtDescripcion != null && prefillDescripcion != null && !prefillDescripcion.isBlank()) {
                txtDescripcion.setText(prefillDescripcion);
            }
            return;
        }
        PauseTransition pausa = new PauseTransition(Duration.millis(200));
        pausa.setOnFinished(event -> intentarAplicarSeleccion(intentosRestantes - 1));
        pausa.play();
    }

    private void aplicarPrefillUbicacion() {
        if (prefillUbicacion == null || prefillUbicacion.isBlank()) return;
        if (filasUbicacion.isEmpty()) return;

        UbicacionRow fila = filasUbicacion.get(0);
        if (fila == null || fila.combo == null) return;

        fila.combo.setValue(prefillUbicacion);
        if (fila.combo.getEditor() != null) fila.combo.getEditor().setText(prefillUbicacion);
    }

    // ==========================
    // CONFIGURACIONES
    // ==========================

    private void configurarPresentaciones() {
        if (cbPresentacion != null) {
            cbPresentacion.setItems(presentaciones);
            cbPresentacion.setValue("pz");
        }
        if (txtFactor != null) txtFactor.setText("1");
    }

    private void cargarUbicacionesDesdeBD() {
        final long token = tokenUbicaciones.incrementAndGet();

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return modeloCompras.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                if (token != tokenUbicaciones.get()) return; // ignora resultados tardíos
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
                sincronizarCombosUbicacion();
            }

            @Override
            protected void failed() {
                if (token != tokenUbicaciones.get()) return;
                ubicaciones.clear();
                sincronizarCombosUbicacion();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void configurarEventos() {
        if (cbClaveProducto != null) {
            cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> onProductoCambio(newVal != null));
        }
        if (cbProductoNombre != null) {
            cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> onProductoCambio(newVal != null));
        }
        if (cbClaveAlterna != null) {
            cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (cargandoEdicion) return;
                if (newVal != null) actualizarDescripcionDesdeProducto();
                actualizarImagenProducto();
                if (modoAjusteInventario) solicitarPrecioAjusteDebounced();
                else cargarPrecioEntradaUltimoProducto();
            });
        }

        if (cbPresentacion != null) {
            cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (cargandoEdicion) return;
                if (newVal != null && newVal.equalsIgnoreCase("pz")) {
                    if (txtFactor != null) txtFactor.setText("1");
                    if (modoAjusteInventario) solicitarPrecioAjusteDebounced();
                    return;
                }
                if (oldVal != null && oldVal.equalsIgnoreCase("pz") && txtFactor != null && "1".equals(txtFactor.getText())) {
                    txtFactor.clear();
                }
                if (modoAjusteInventario) solicitarPrecioAjusteDebounced();
            });
        }

        if (txtLote != null) {
            txtLote.textProperty().addListener((obs, oldVal, newVal) -> {
                if (cargandoEdicion) return;
                if (modoAjusteInventario) solicitarPrecioAjusteDebounced();
                solicitarValidacionDuplicadoDebounced();
            });
        }

        if (txtFactor != null) {
            txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
                if (cargandoEdicion) return;
                if (modoAjusteInventario) solicitarPrecioAjusteDebounced();
            });
        }

        if (btnGuardar != null) btnGuardar.setOnAction(e -> guardarItem());
        if (btnLimpiar != null) btnLimpiar.setOnAction(e -> limpiarFormularioParaNuevo());
    }

    private void onProductoCambio(boolean nuevoNoNulo) {
        if (nuevoNoNulo) {
            actualizarDescripcionDesdeProducto();
            seleccionarClaveAlternaPendiente = !cargandoEdicion;
        }
        actualizarImagenProducto();

        if (modoAjusteInventario) {
            solicitarPrecioAjusteDebounced(); // debounce para no saturar BD
        } else {
            cargarPrecioEntradaUltimoProducto();
        }
    }

    private void solicitarPrecioAjusteDebounced() {
        if (debouncePrecioAjuste == null) {
            debouncePrecioAjuste = new PauseTransition(Duration.millis(250));
            debouncePrecioAjuste.setOnFinished(e -> cargarPrecioEntradaAjusteInventario());
        }
        debouncePrecioAjuste.stop();
        debouncePrecioAjuste.playFromStart();
    }

    private void solicitarValidacionDuplicadoDebounced() {
        if (debounceLoteDuplicado == null) {
            debounceLoteDuplicado = new PauseTransition(Duration.millis(250));
            debounceLoteDuplicado.setOnFinished(e -> validarProductoLoteDuplicadoEnTiempoReal());
        }
        debounceLoteDuplicado.stop();
        debounceLoteDuplicado.playFromStart();
    }

    private void configurarSeleccionClaveAlternaPorDefecto() {
        if (cbClaveAlterna == null) return;

        cbClaveAlterna.getItems().addListener((javafx.collections.ListChangeListener<String>) change -> {
            if (cbClaveAlterna.getItems().isEmpty()) return;
            if (!seleccionarClaveAlternaPendiente || cargandoEdicion) return;

            seleccionarClaveAlternaPendiente = false;
            Platform.runLater(this::seleccionarPrimerClaveAlternaDisponible);
        });
    }

    private void seleccionarPrimerClaveAlternaDisponible() {
        if (cbClaveAlterna == null || cbClaveAlterna.getItems().isEmpty()) return;

        String primeraClave = cbClaveAlterna.getItems().stream()
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse("");

        if (primeraClave.isBlank()) {
            cbClaveAlterna.setValue("");
            return;
        }
        cbClaveAlterna.setValue(primeraClave);
    }

    private void configurarLimpiezaPorCampoVacio() {
        configurarLimpiezaCombo(cbClaveProducto);
        configurarLimpiezaCombo(cbProductoNombre);
        configurarLimpiezaCombo(cbClaveAlterna);
    }

    private void configurarLimpiezaCombo(ComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getEditor() == null) return;

        comboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String texto = comboBox.getEditor().getText();
                if (texto == null || texto.isBlank()) limpiarSeleccionProducto();
            }
        });
    }

    private void limpiarSeleccionProducto() {
        if (productoController != null) productoController.limpiarSeleccion();
        if (txtDescripcion != null) txtDescripcion.clear();
        limpiarImagenProducto();
        // No se cambian validaciones ni flujo, solo limpieza.
    }

    private void limpiarImagenProducto() {
        if (previewImage != null) previewImage.setImage(null);
    }

    private void actualizarImagenProducto() {
        if (previewImage == null || productoController == null) return;

        previewImage.setImage(null);

        String urlImagen = productoController.getUrlImagenSeleccionada();
        if (urlImagen == null || urlImagen.isBlank()) return;

        // Cache primero (misma funcionalidad, más rápido)
        Image cached = cacheImagenes.get(urlImagen);
        if (cached != null) {
            previewImage.setImage(cached);
            return;
        }

        // Token para ignorar respuestas tardías si el usuario cambia de producto rápido
        final long token = tokenImagen.incrementAndGet();

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                conexionFTP ftp = new conexionFTP();
                return ftp.getImageFromFTP(urlImagen);
            }

            @Override
            protected void succeeded() {
                if (token != tokenImagen.get()) return;
                Image img = getValue();
                if (img != null) {
                    cacheImagenes.put(urlImagen, img);
                    previewImage.setImage(img);
                } else {
                    previewImage.setImage(null);
                }
            }

            @Override
            protected void failed() {
                if (token != tokenImagen.get()) return;
                previewImage.setImage(null);
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void configurarCalculoPrecios() {
        if (txtCantidad != null) txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        if (txtPrecioEntrada != null) txtPrecioEntrada.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        if (checkBoxIVA != null) checkBoxIVA.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
    }

    private void configurarCamposLectura() {
        if (txtPrecioIVA != null) txtPrecioIVA.setEditable(false);
        if (txtPrecioBruto != null) txtPrecioBruto.setEditable(false);
        if (txtPrecioTotal != null) txtPrecioTotal.setEditable(false);
    }

    private void configurarManejoEnter() {
        if (cbClaveProducto != null) cbClaveProducto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (cbProductoNombre != null) cbProductoNombre.requestFocus();
                event.consume();
            }
        });

        if (cbProductoNombre != null) cbProductoNombre.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (cbClaveAlterna != null) cbClaveAlterna.requestFocus();
                event.consume();
            }
        });

        if (cbClaveAlterna != null) cbClaveAlterna.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (txtCantidad != null) txtCantidad.requestFocus();
                event.consume();
            }
        });

        if (txtCantidad != null) txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (cbPresentacion != null) cbPresentacion.requestFocus();
                event.consume();
            }
        });

        if (cbPresentacion != null) cbPresentacion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (txtFactor != null) txtFactor.requestFocus();
                event.consume();
            }
        });

        if (txtFactor != null) txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });
    }

    private void configurarValidaciones() {
        // Se conservan exactamente los validadores existentes
        configurarValidadorCampo(txtCantidad, "entero");
        configurarValidadorCampo(txtFactor, "decimal");
        configurarValidadorCampo(txtPrecioEntrada, "decimal");
        configurarValidadorCampo(txtPrecioIVA, "decimal");
        configurarValidadorCampo(txtPrecioBruto, "decimal");
        configurarValidadorCampo(txtPrecioTotal, "decimal");
    }

    private void configurarValidadorCampo(TextField campo, String tipo) {
        if (campo == null) return;

        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            if ("entero".equals(tipo)) {
                campo.setText(helperCompraEmergente.getValidadorEnteros().apply(newVal));
            } else if ("decimal".equals(tipo)) {
                campo.setText(helperCompraEmergente.getValidadorDecimales().apply(newVal));
            }
        });
    }

    private void actualizarDescripcionDesdeProducto() {
        if (productoController == null || txtDescripcion == null) return;

        String idProducto = productoController.getIdSeleccionado();
        if (idProducto != null && idProducto.equals(ultimoIdProductoDescripcion)) return;

        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
        ultimoIdProductoDescripcion = idProducto != null ? idProducto : "";
    }

    // ==========================
    // PRECIOS (BD)
    // ==========================

    private void cargarPrecioEntradaUltimoProducto() {
        if (itemParaEditar != null) return; // mantener comportamiento

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank() || txtPrecioEntrada == null) return;

        // Cache para no repetir consulta (optimiza BD)
        BigDecimal cached = cachePrecioUltimoProducto.get(idProducto);
        if (cached != null) {
            txtPrecioEntrada.setText(formatearDecimal(cached));
            recalcularPrecios();
            return;
        }

        final long token = tokenPrecio.incrementAndGet();
        final String idSnapshot = idProducto;

        Task<Optional<BigDecimal>> task = new Task<>() {
            @Override
            protected Optional<BigDecimal> call() {
                return modeloCompras.obtenerPrecioEntradaUltimoProducto(idSnapshot);
            }

            @Override
            protected void succeeded() {
                if (token != tokenPrecio.get()) return; // ignora resultados tardíos
                Optional<BigDecimal> r = getValue();
                r.ifPresent(precio -> {
                    if (precio != null) {
                        cachePrecioUltimoProducto.put(idSnapshot, precio);
                        txtPrecioEntrada.setText(formatearDecimal(precio));
                        recalcularPrecios();
                    }
                });
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarPrecioEntradaAjusteInventario() {
        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        String lote = t(txtLote);
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        String factorTexto = t(txtFactor);

        // mismas reglas: si falta algo, no consultar
        if (idProducto == null || idProducto.isBlank()
                || lote.isBlank()
                || presentacion == null || presentacion.isBlank()
                || factorTexto.isBlank()) {
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto.trim());
        } catch (NumberFormatException e) {
            return;
        }

        if (txtPrecioEntrada == null) return;

        final long token = tokenPrecio.incrementAndGet();
        final String idSnapshot = idProducto;
        final String loteSnapshot = lote;
        final String presSnapshot = presentacion;
        final int factorSnapshot = factor;

        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modeloPreciosAjuste.obtenerPreciosProductoPorLotePresentacionFactor(
                        idSnapshot, loteSnapshot, presSnapshot, factorSnapshot
                );
            }

            @Override
            protected void succeeded() {
                if (token != tokenPrecio.get()) return;
                Optional<modelNuevoTraspasoSalida.PreciosProducto> r = getValue();
                if (r != null && r.isPresent()) {
                    BigDecimal precio = r.get().getPrecioUnitario();
                    txtPrecioEntrada.setText(formatearDecimal(precio));
                    recalcularPrecios();
                } else if (txtPrecioEntrada.getText() == null || txtPrecioEntrada.getText().isBlank()) {
                    recalcularPrecios();
                }
            }

            @Override
            protected void failed() {
                // No cambia validación; solo evita que un fallo deje UI inconsistente
                if (token != tokenPrecio.get()) return;
                if (txtPrecioEntrada.getText() == null || txtPrecioEntrada.getText().isBlank()) {
                    recalcularPrecios();
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // ==========================
    // CLAVE ALTERNA desde formulario
    // ==========================

    public void actualizarClaveAlternaCreada(String claveAlterna) {
        Platform.runLater(() -> {
            if (claveAlterna != null && !claveAlterna.isEmpty() && cbClaveAlterna != null) {
                cbClaveAlterna.setValue(claveAlterna);
                if (cbClaveAlterna.getEditor() != null) cbClaveAlterna.getEditor().setText(claveAlterna);

                actualizarDesdeClaveAlternaExterna(claveAlterna);

                if (txtCantidad != null) txtCantidad.requestFocus();
            }
        });
    }

    private void actualizarDesdeClaveAlternaExterna(String claveAlterna) {
        if (productoController != null) {
            productoController.setSeleccionPorClaveAlterna(claveAlterna);
            actualizarDescripcionDesdeProducto();
        }
    }

    // ==========================
    // GUARDAR (NO cambia validaciones)
    // ==========================

    private void guardarItem() {
        if (itemsCompra == null) {
            mostrarAlerta("Error", "No se pudo conectar con la tabla principal");
            return;
        }

        String clave = productoController != null ? productoController.getIdSeleccionado() : null;
        String nombre = productoController != null ? productoController.getNombreSeleccionado() : null;
        String claveAlterna = productoController != null ? productoController.getClaveAlternaSeleccionada() : null;

        String descripcion = txtDescripcion != null ? txtDescripcion.getText() : "";
        String lote = txtLote != null ? txtLote.getText() : "";
        String caducidad = obtenerCaducidadTexto();
        String cantidadTexto = txtCantidad != null ? txtCantidad.getText() : "";
        String nota = txtNota != null ? txtNota.getText() : "";
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        String factor = txtFactor != null ? txtFactor.getText() : "";
        String precioEntrada = txtPrecioEntrada != null ? txtPrecioEntrada.getText() : "";
        String precioIVA = txtPrecioIVA != null ? txtPrecioIVA.getText() : "";
        String precioBruto = txtPrecioBruto != null ? txtPrecioBruto.getText() : "";
        String precioTotal = txtPrecioTotal != null ? txtPrecioTotal.getText() : "";

        // 1) Validación producto seleccionado (MISMA)
        if (productoController == null || !productoController.validarSeleccion()) {
            mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                    "Por favor, verifique la selección.");
            return;
        }

        String mensajeCampos = validarCamposObligatorios(clave, nombre, claveAlterna, descripcion, lote,
                cantidadTexto, presentacion, factor, precioEntrada, precioIVA, precioBruto, precioTotal);
        if (mensajeCampos != null) {
            mostrarAlerta("Advertencia", mensajeCampos);
            return;
        }

        if (existeProductoLoteDuplicado(clave, lote)) {
            mostrarAlerta("Advertencia", "Este producto con el mismo lote ya está agregado a la compra.");
            return;
        }

        // 2) Validar formulario con helper (MISMA)
        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();

        helperCompraEmergente.ResultadoValidacion validacion =
                helperCompraEmergente.validarFormularioCompleto(
                        clave, nombre, cantidadTexto,
                        ubicacionesSeleccionadas, precioEntrada
                );

        if (!validacion.isValido()) {
            mostrarAlerta("Advertencia", validacion.getMensaje());
            return;
        }

        if (esProductoReactivo() && (caducidad == null || caducidad.isBlank())) {
            mostrarAlerta("Advertencia", "La caducidad es forzosa para los reactivos.");
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int cantidad = Integer.parseInt(cantidadTexto.trim());

        String mensajeUbicaciones = validarUbicacionesCapturadas(cantidad, ubicacionesSeleccionadas);
        if (mensajeUbicaciones != null) {
            mostrarAlerta("Advertencia", mensajeUbicaciones);
            return;
        }

        if (presentacion == null || presentacion.isBlank()) presentacion = "pz";

        // 3) Crear o actualizar (MISMO)
        boolean aplicaIva = checkBoxIVA != null && checkBoxIVA.isSelected();

        if (itemParaEditar != null) {
            itemParaEditar.setClaveProducto(clave);
            itemParaEditar.setProducto(nombre);
            itemParaEditar.setDescripcion(descripcion);
            itemParaEditar.setLote(lote);
            itemParaEditar.setCaducidad(caducidad);
            itemParaEditar.setCantidad(cantidad);
            itemParaEditar.setClaveAlterna(claveAlterna);
            itemParaEditar.setPresentacion(presentacion);
            itemParaEditar.setFactor(factor);
            itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
            itemParaEditar.setNota(nota);
            itemParaEditar.setPrecioEntrada(precioEntrada);
            itemParaEditar.setPrecioIva(precioIVA);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
            itemParaEditar.setAplicaIva(aplicaIva);
        } else {
            compra item = new compra(
                    clave, nombre, descripcion, lote, caducidad, cantidad, claveAlterna,
                    presentacion, factor, ubicacionesSeleccionadas, precioEntrada,
                    precioIVA, precioBruto, precioTotal,
                    aplicaIva,
                    proveedorId, proveedorNombre
            );
            item.setNota(nota);
            itemsCompra.add(item);
        }

        if (mainController != null) mainController.refrescarTabla();

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            if (modoAjusteInventario) mostrarAlertaSinEspera("Éxito", "Se agregó el artículo correctamente.");
            else mostrarAlertaSinEspera("Éxito", "Producto agregado a la compra");
            limpiarFormularioParaNuevo();
        }
    }

    private String validarCamposObligatorios(String clave, String nombre, String claveAlterna, String descripcion,
                                             String lote, String cantidad, String presentacion, String factor,
                                             String precioEntrada, String precioIva, String precioBruto,
                                             String precioTotal) {

        if (esVacio(clave) || esVacio(nombre) || esVacio(claveAlterna) || esVacio(descripcion)
                || esVacio(lote) || esVacio(cantidad) || esVacio(presentacion) || esVacio(factor)
                || esVacio(precioEntrada) || esVacio(precioIva) || esVacio(precioBruto)
                || esVacio(precioTotal)) {
            return "Debe completar todos los campos obligatorios (excepto caducidad y nota).";
        }

        // Se conserva la validación de revisar todas las ubicaciones/cantidades capturadas.
        // Optimización: usar filasUbicacion ya creada (evita recorrer nodos y streams).
        for (UbicacionRow fila : filasUbicacion) {
            if (fila == null) continue;

            String ubic = fila.combo != null ? fila.combo.getValue() : "";
            if ((ubic == null || ubic.isBlank()) && fila.combo != null && fila.combo.getEditor() != null) {
                ubic = fila.combo.getEditor().getText();
            }
            String cant = fila.cantidad != null ? fila.cantidad.getText() : "";

            if (esVacio(ubic) || esVacio(cant)) {
                return "Debe completar todas las ubicaciones y cantidades.";
            }
        }

        return null;
    }

    private void validarProductoLoteDuplicadoEnTiempoReal() {
        if (itemsCompra == null) return;
        String clave = productoController != null ? productoController.getIdSeleccionado() : null;
        String lote = t(txtLote);
        if (clave == null || clave.isBlank() || lote.isBlank()) {
            ultimoAvisoDuplicado = "";
            return;
        }

        if (existeProductoLoteDuplicado(clave, lote)) {
            String llave = clave.trim() + "|" + lote.trim().toLowerCase(Locale.ROOT);
            if (!llave.equals(ultimoAvisoDuplicado)) {
                ultimoAvisoDuplicado = llave;
                mostrarAlerta("Advertencia", "Este producto con el mismo lote ya está agregado a la compra.");
                if (txtLote != null) txtLote.clear();
            }
        } else {
            ultimoAvisoDuplicado = "";
        }
    }

    private boolean existeProductoLoteDuplicado(String clave, String lote) {
        if (itemsCompra == null) return false;
        String claveLimpia = clave == null ? "" : clave.trim();
        String loteLimpio = lote == null ? "" : lote.trim().toLowerCase(Locale.ROOT);
        if (claveLimpia.isBlank() || loteLimpio.isBlank()) return false;

        for (compra item : itemsCompra) {
            if (item == null) continue;
            if (itemParaEditar != null && item == itemParaEditar) continue;
            String claveItem = item.getClaveProducto() == null ? "" : item.getClaveProducto().trim();
            String loteItem = item.getLote() == null ? "" : item.getLote().trim().toLowerCase(Locale.ROOT);
            if (claveLimpia.equals(claveItem) && loteLimpio.equals(loteItem)) {
                return true;
            }
        }
        return false;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        Set<String> ubicacionesUnicas = new HashSet<>();
        for (UbicacionCompra ubicacionCompra : ubicacionesSeleccionadas) {
            if (ubicacionCompra == null || ubicacionCompra.getUbicacion() == null) continue;
            String ubicacion = ubicacionCompra.getUbicacion().trim();
            if (ubicacion.isBlank()) continue;
            if (!ubicacionesUnicas.add(ubicacion)) return true;
        }
        return false;
    }

    private void limpiarFormularioParaNuevo() {
        if (productoController != null) productoController.limpiarSeleccion();
        seleccionarClaveAlternaPendiente = false;
        ultimoAvisoDuplicado = "";

        limpiarCombosProducto();
        if (txtDescripcion != null) txtDescripcion.clear();

        limpiarImagenProducto();

        if (txtLote != null) txtLote.clear();
        if (dpCaducidad != null) dpCaducidad.setValue(null);
        if (txtCantidad != null) txtCantidad.clear();

        if (cbPresentacion != null) cbPresentacion.setValue("pz");
        if (txtFactor != null) txtFactor.setText("1");

        if (txtNota != null) txtNota.clear();

        if (txtPrecioEntrada != null) txtPrecioEntrada.clear();
        if (txtPrecioIVA != null) txtPrecioIVA.clear();
        if (txtPrecioBruto != null) txtPrecioBruto.clear();
        if (txtPrecioTotal != null) txtPrecioTotal.clear();

        if (checkBoxIVA != null) checkBoxIVA.setSelected(false);

        reiniciarUbicacionesDinamicas();

        // Se conserva el comportamiento: no forzamos focus aquí.
    }

    private void limpiarCombosProducto() {
        limpiarCombo(cbClaveProducto);
        limpiarCombo(cbProductoNombre);
        limpiarCombo(cbClaveAlterna);
    }

    private void limpiarCombo(ComboBox<String> comboBox) {
        if (comboBox == null) return;
        comboBox.setValue(null);
        comboBox.getSelectionModel().clearSelection();
        if (comboBox.getEditor() != null) comboBox.getEditor().clear();
    }

    // ==========================
    // ABRIR FORMULARIOS (se conserva)
    // ==========================

    @FXML
    private void agregarProducto() {
        if (proveedorId == null || proveedorId.isBlank() || proveedorNombre == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de agregar un producto.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo producto");
            stage.setScene(new Scene(root));
            stage.initOwner(btnGuardar.getScene().getWindow());

            stage.showAndWait();

            String productoId = ctrl.getProductoIdCreado();
            String productoNombre = ctrl.getProductoNombreCreado();

            if (productoId != null && !productoId.isEmpty() && productoNombre != null && !productoNombre.isEmpty()) {
                if (productoController != null) productoController.recargarConProveedor(null);

                mostrarAlertaSinEspera("Éxito", "Producto creado. Ahora vincule una clave alterna.");

                abrirFormularioClavesConProducto(null, false, proveedorId, proveedorNombre, productoId, productoNombre);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de producto.");
        }
    }

    @FXML
    private void vincularProducto() {
        if (proveedorId == null || proveedorId.isBlank() || proveedorNombre == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de vincular un producto.");
            return;
        }
        abrirFormularioClaves(null, false, proveedorId, proveedorNombre);
    }

    private void abrirFormularioClaves(String[] fila, boolean esEdicion, String proveedorId, String proveedorNombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();
            ctrl.setParentController(this);

            if (proveedorId != null && proveedorNombre != null) ctrl.setProveedorSeleccionado(proveedorId, proveedorNombre);
            if (esEdicion && fila != null) ctrl.cargarParaEdicion(fila);

            String titulo = esEdicion ? "Editar Clave" : "Nueva Clave";

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(btnGuardar.getScene().getWindow());
            stage.centerOnScreen();
            stage.showAndWait();

            if (productoController != null) productoController.recargarConProveedor(null);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de sincronización.");
        }
    }

    private void abrirFormularioClavesConProducto(String[] fila, boolean esEdicion,
                                                  String proveedorId, String proveedorNombre,
                                                  String productoId, String productoNombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();
            ctrl.setParentController(this);

            if (proveedorId != null && proveedorNombre != null) ctrl.setProveedorSeleccionado(proveedorId, proveedorNombre);
            if (productoId != null && productoNombre != null) ctrl.setProductoSeleccionado(productoId, productoNombre);
            if (esEdicion && fila != null) ctrl.cargarParaEdicion(fila);

            String titulo = "Vincular Clave - " + productoNombre;
            if (proveedorNombre != null) titulo += " (" + proveedorNombre + ")";

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(btnGuardar.getScene().getWindow());
            stage.centerOnScreen();
            stage.showAndWait();

            if (productoController != null) productoController.recargarConProveedor(null);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de sincronización.");
        }
    }

    // ==========================
    // EDICIÓN
    // ==========================

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) return;

        cargandoEdicion = true;
        seleccionarClaveAlternaPendiente = false;

        if (cbClaveProducto != null) cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        if (cbProductoNombre != null) cbProductoNombre.setValue(itemParaEditar.getProducto());
        if (cbClaveAlterna != null) cbClaveAlterna.setValue(itemParaEditar.getClaveAlterna());

        if (txtDescripcion != null) txtDescripcion.setText(itemParaEditar.getDescripcion());
        if (txtNota != null) txtNota.setText(itemParaEditar.getNota());

        if (txtLote != null) txtLote.setText(itemParaEditar.getLote());
        ultimoAvisoDuplicado = "";
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());

        if (txtCantidad != null) txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        if (cbPresentacion != null) cbPresentacion.setValue(itemParaEditar.getPresentacion());
        if (txtFactor != null) txtFactor.setText(itemParaEditar.getFactor());

        if (txtPrecioEntrada != null) txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());

        if (checkBoxIVA != null) checkBoxIVA.setSelected(itemParaEditar.isAplicaIva());

        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();

        Platform.runLater(() -> cargandoEdicion = false);
    }

    // ==========================
    // UBICACIONES DINÁMICAS
    // ==========================

    @FXML
    private void agregarUbicacion() {
        agregarFilaUbicacion(false);
    }

    private void inicializarUbicacionesDinamicas() {
        reiniciarUbicacionesDinamicas();
    }

    private void reiniciarUbicacionesDinamicas() {
        if (contenedorUbicaciones != null) contenedorUbicaciones.getChildren().clear();
        filasUbicacion.clear();
        contadorFilas = 0;
        agregarFilaUbicacion(true);
    }

    private void agregarFilaUbicacion(boolean esInicial) {
        if (contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Escribe o selecciona una ubicación");
        combo.setItems(ubicaciones);
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidadUb = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidadUb);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(esInicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (esInicial) boton.setOnAction(event -> agregarUbicacion());
        else boton.setOnAction(event -> eliminarFilaUbicacion(nuevaFila));

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        if (contenedorUbicaciones != null) contenedorUbicaciones.getChildren().add(nuevaFila);

        configurarComboUbicacion(combo);
        configurarValidadorCampo(txtCantidadUb, "entero");

        filasUbicacion.add(new UbicacionRow(nuevaFila, combo, txtCantidadUb));
        contadorFilas++;
    }

    private void eliminarFilaUbicacion(HBox fila) {
        UbicacionRow encontrada = null;
        for (UbicacionRow r : filasUbicacion) {
            if (r.contenedor == fila) { encontrada = r; break; }
        }
        if (encontrada != null) filasUbicacion.remove(encontrada);

        if (contenedorUbicaciones != null) contenedorUbicaciones.getChildren().remove(fila);
        contadorFilas = Math.max(0, contadorFilas - 1);
    }

    private void configurarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) return;

        boolean[] actualizando = {false};

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizando[0]) return;
            actualizando[0] = true;
            if (newVal == null || newVal.isBlank()) {
                if (comboBox.getEditor() != null) comboBox.getEditor().clear();
            } else if (comboBox.getEditor() != null) {
                comboBox.getEditor().setText(newVal);
            }
            actualizando[0] = false;
        });

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (actualizando[0]) return;
                if (newVal == null || newVal.isBlank()) {
                    actualizando[0] = true;
                    comboBox.setValue(null);
                    actualizando[0] = false;
                }
            });

            comboBox.getEditor().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    confirmarTextoCombo(comboBox, actualizando);
                    event.consume();
                }
            });
        }

        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) confirmarTextoCombo(comboBox, actualizando);
        });

        comboBox.setOnAction(event -> confirmarTextoCombo(comboBox, actualizando));
    }

    private void confirmarTextoCombo(ComboBox<String> comboBox, boolean[] actualizando) {
        if (comboBox == null || actualizando[0]) return;

        String texto = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        String seleccionado = comboBox.getSelectionModel().getSelectedItem();
        String valor = (seleccionado != null && !seleccionado.isBlank()) ? seleccionado : texto;

        if (valor == null || valor.isBlank()) return;

        actualizando[0] = true;
        comboBox.setValue(valor);
        if (comboBox.getEditor() != null) comboBox.getEditor().setText(valor);
        actualizando[0] = false;
    }

    private void sincronizarCombosUbicacion() {
        for (UbicacionRow filaUb : filasUbicacion) {
            if (filaUb != null && filaUb.combo != null) filaUb.combo.setItems(ubicaciones);
        }
    }

    private List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new ArrayList<>();
        for (UbicacionRow fila : filasUbicacion) {
            if (fila == null) continue;

            ComboBox<String> combo = fila.combo;
            TextField cantidadField = fila.cantidad;

            String ubicacion = combo != null ? combo.getValue() : null;
            if ((ubicacion == null || ubicacion.isBlank()) && combo != null && combo.getEditor() != null) {
                ubicacion = combo.getEditor().getText();
            }

            String cantidadTexto = cantidadField != null ? cantidadField.getText() : null;

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) continue;

            try {
                int cant = Integer.parseInt(cantidadTexto.trim());
                if (cant > 0) resultado.add(new UbicacionCompra(ubicacion.trim(), cant));
            } catch (NumberFormatException ignored) {
                // Se conserva: ignora cantidades inválidas aquí (ya se valida en helper/validaciones)
            }
        }
        return resultado;
    }

    private String validarUbicacionesCapturadas(int cantidadTotal, List<UbicacionCompra> ubicacionesSeleccionadas) {
        if (ubicacionesSeleccionadas == null || ubicacionesSeleccionadas.isEmpty()) {
            return "Debe capturar las ubicaciones con cantidad.";
        }
        int sumaUbicaciones = ubicacionesSeleccionadas.stream().mapToInt(UbicacionCompra::getCantidad).sum();
        if (sumaUbicaciones != cantidadTotal) {
            return "La suma de cantidades por ubicación debe ser igual a la cantidad total.";
        }
        return null;
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        reiniciarUbicacionesDinamicas();
        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) return;

        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra u = ubicacionesLista.get(i);
            if (u == null) continue;

            UbicacionRow fila = (i == 0) ? filasUbicacion.get(0) : crearFilaParaEdicion();
            if (fila == null) continue;

            fila.combo.setValue(u.getUbicacion());
            if (fila.combo.getEditor() != null) fila.combo.getEditor().setText(u.getUbicacion());
            fila.cantidad.setText(String.valueOf(u.getCantidad()));
        }
    }

    private UbicacionRow crearFilaParaEdicion() {
        agregarFilaUbicacion(false);
        return filasUbicacion.get(filasUbicacion.size() - 1);
    }

    // ==========================
    // ALERTAS / UI
    // ==========================

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private void mostrarAlertaSinEspera(String titulo, String mensaje) {
        // Optimización: sin crear hilos manuales; mismo comportamiento visible.
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.show();

            PauseTransition pt = new PauseTransition(Duration.millis(2000));
            pt.setOnFinished(e -> { if (alert.isShowing()) alert.close(); });
            pt.play();
        });
    }

    private void recalcularPrecios() {
        int cantidad = parseEntero(txtCantidad != null ? txtCantidad.getText() : "");
        BigDecimal precioEntrada = parseDecimal(txtPrecioEntrada != null ? txtPrecioEntrada.getText() : "");
        boolean aplicaIva = checkBoxIVA != null && checkBoxIVA.isSelected();

        if (cantidad <= 0) {
            if (txtPrecioIVA != null) txtPrecioIVA.clear();
            if (txtPrecioBruto != null) txtPrecioBruto.clear();
            if (txtPrecioTotal != null) txtPrecioTotal.clear();
            return;
        }

        BigDecimal precioConIva = precioEntrada;
        if (aplicaIva) {
            BigDecimal iva = precioEntrada.multiply(IvaConfigService.getIvaTasa());
            precioConIva = precioEntrada.add(iva);
        }

        BigDecimal cantidadDecimal = BigDecimal.valueOf(cantidad);
        BigDecimal precioBruto = precioEntrada.multiply(cantidadDecimal);
        BigDecimal precioTotal = precioConIva.multiply(cantidadDecimal);

        if (txtPrecioIVA != null) txtPrecioIVA.setText(formatearDecimal(precioConIva));
        if (txtPrecioBruto != null) txtPrecioBruto.setText(formatearDecimal(precioBruto));
        if (txtPrecioTotal != null) txtPrecioTotal.setText(formatearDecimal(precioTotal));
    }

    private String formatearDecimal(BigDecimal valor) {
        if (valor == null) return "0.00";
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private int parseEntero(String valor) {
        if (valor == null) {
            return 0;
        }
        String limpio = valor.trim();
        if (limpio.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        String limpio = valor.trim();
        if (limpio.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String obtenerCaducidadTexto() {
        if (dpCaducidad == null || dpCaducidad.getValue() == null) return "";
        return dpCaducidad.getValue().format(FECHA_FORMATO);
    }

    private boolean esProductoReactivo() {
        if (productoController == null) return false;
        String categoria = productoController.getCategoriaSeleccionada();
        if (categoria == null) return false;
        String categoriaNormalizada = categoria.trim().toLowerCase();
        return categoriaNormalizada.startsWith("reactiv");
    }

    private void configurarCaducidadDesdeTexto(String caducidad) {
        if (dpCaducidad == null) return;

        if (caducidad == null || caducidad.isBlank()) {
            dpCaducidad.setValue(null);
            return;
        }

        try {
            dpCaducidad.setValue(LocalDate.parse(caducidad.trim(), FECHA_FORMATO));
        } catch (Exception e) {
            dpCaducidad.setValue(null);
        }
    }

    private void cerrarFormulario() {
        if (btnGuardar == null || btnGuardar.getScene() == null) return;
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        if (stage != null) stage.close();
    }

    // ==========================
    // HELPERS
    // ==========================

    private static String t(TextInputControl c) {
        return (c == null || c.getText() == null) ? "" : c.getText().trim();
    }

    private static class UbicacionRow {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private UbicacionRow(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }

    private void aplicarPrefill() {
        aplicarPrefillProducto();
        aplicarPrefillUbicacion();
        aplicarPrefillPresentacionFactor(); // Nueva línea
    }

    private void aplicarPrefillPresentacionFactor() {
        if (prefillPresentacion != null && !prefillPresentacion.isBlank()) {
            if (cbPresentacion != null) {
                cbPresentacion.setValue(prefillPresentacion);
            }
        }

        if (prefillFactor != null && !prefillFactor.isBlank()) {
            if (txtFactor != null) {
                txtFactor.setText(prefillFactor);
            }
        }
    }
}
