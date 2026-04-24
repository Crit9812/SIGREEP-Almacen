package Formularios.controller;

import Compartido.controller.productoCboxController;
import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.model.DAO.GenericDAO;
import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import conexion.conexionFTP;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FormularioSalidaController {

    // === CAMPOS COMUNES PROTEGIDOS ===
    @FXML protected VBox contenedorUbicaciones;
    @FXML protected ComboBox<String> comboUbicacion;
    @FXML protected ComboBox<String> cbClaveProducto;
    @FXML protected ComboBox<String> cbClaveAlterna;
    @FXML protected ComboBox<String> cbProductoNombre;
    @FXML protected TextField txtDescripcion;
    @FXML protected ImageView previewImage;
    @FXML protected TextField txtLote;
    @FXML protected DatePicker dpCaducidad;
    @FXML protected TextField txtCantidad;
    @FXML protected ComboBox<String> cbPresentacion;
    @FXML protected TextField txtFactor;
    @FXML protected TextField txtCantidadUbicacion;
    @FXML protected TextField txtNota; // Campo común para ambos
    @FXML protected Button btnGuardar;
    @FXML protected Button btnLimpiar;
    @FXML protected TabPane tabPaneModo;
    @FXML protected Tab tabNormal;
    @FXML protected Tab tabRapido;
    @FXML protected TextField txtPrecioEntrada;
    @FXML protected TextField txtPrecioIVA;
    @FXML protected TextField txtPrecioBruto;
    @FXML protected TextField txtPrecioTotal;
    @FXML protected TextField txtPrecioEntradaRapida;
    @FXML protected TextField txtPrecioIVARapida;
    @FXML protected TextField txtPrecioBrutoRapida;
    @FXML protected TextField txtPrecioTotalRapida;

    // === CAMPOS PARA MODO RÁPIDO ===
    @FXML protected ComboBox<String> cbPresentacionRapida;
    @FXML protected TextField txtFactorRapido;
    @FXML protected TextField txtCantidadRapida;

    // === CONSTANTES COMUNES ===
    // En FormularioSalidaController.java debe tener:
    protected BigDecimal precioEntradaBase = BigDecimal.ZERO;
    protected BigDecimal precioIvaBase = BigDecimal.ZERO;
    protected static final int MAX_FILAS = 10;
    protected static final Duration DEBOUNCE_TIEMPO = Duration.millis(300);

    // === LISTAS Y COLECCIONES COMUNES ===
    protected final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    protected final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "pza", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    // === CONTROLADORES Y MODELOS ===
    protected productoCboxController productoController;
    protected final modelNuevoTraspasoSalida modelo = new modelNuevoTraspasoSalida();

    // === ESTADOS Y VALIDACIONES ===
    protected int contadorFilas = 1;
    protected boolean loteValidado = false;
    protected boolean caducidadValidada = false;
    protected boolean presentacionValida = false;
    protected boolean factorValido = false;
    protected boolean ubicacionValidada = false;
    protected boolean cantidadTotalValida = false;
    protected boolean cantidadRapidaValida = false;
    protected boolean seleccionarClaveAlternaPendiente = false;
    protected boolean inicializado = false;
    protected int cantidadDisponibleUbicacion = 0;

    // === CACHE Y DEBOUNCE ===
    protected final Map<String, Image> cacheImagenes = new ConcurrentHashMap<>();
    protected final Map<TextField, PauseTransition> debounceCantidadUbicacion = new HashMap<>();
    protected final Map<TextField, String> ultimaCantidadUbicacionValidada = new HashMap<>();
    protected final Map<ComboBox<String>, List<UbicacionCompra>> ubicacionesCapturadas = new HashMap<>();

    // === DEBOUNCE TRANSITIONS ===
    protected final PauseTransition loteDebounce = new PauseTransition(DEBOUNCE_TIEMPO);
    protected final PauseTransition factorDebounce = new PauseTransition(DEBOUNCE_TIEMPO);
    protected final PauseTransition cantidadRapidaDebounce = new PauseTransition(DEBOUNCE_TIEMPO);

    // === ULTIMOS VALORES VALIDADOS ===
    protected String ultimoLoteValidado = "";
    protected String ultimoFactorValidado = "";
    protected String ultimaPresentacionValidada = "";

    // === MÉTODOS ABSTRACTOS QUE CADA CLASE DEBE IMPLEMENTAR ===
    protected abstract void guardarItem();
    protected abstract void guardarItemRapido();
    protected abstract boolean existeProductoLoteEnLista(String clave, String lote, traspasoSalida itemExcluir);
    protected abstract List<UbicacionCompra> obtenerUbicacionesSeleccionadas();
    protected abstract void limpiarFormularioParaNuevo();
    protected abstract void mostrarAlerta(String titulo, String mensaje);
    protected abstract void mostrarAlertaSinEspera(String titulo, String mensaje);
    protected abstract void recalcularPrecios();
    protected abstract void recalcularPreciosRapido();
    protected abstract void limpiarPrecios();
    protected abstract void limpiarPreciosRapidos();
    protected abstract void cargarPreciosDesdeProducto();
    protected abstract void cargarPreciosRapidosDesdeUltimaEntrada();
    protected abstract boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas);

    // === MÉTODOS PARA CONFIGURACIÓN ESPECÍFICA ===
    protected abstract void configurarCamposLecturaEspecificos();
    protected abstract void configurarCalculoPreciosEspecifico();

    @FXML
    protected void initializeBase() {
        productoController = new productoCboxController();
        productoController.inicializarDisponibles(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

        configurarPresentaciones();
        configurarPresentacionesRapidas();
        configurarAutocompletadoUbicacion(comboUbicacion);
        configurarEventosBase();
        configurarValidacionesBase();
        configurarCalculoPreciosBase();
        configurarCamposLecturaBase();
        cargarUbicacionesDesdeBD();
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnterBase();
        configurarCascada();
        configurarModoRapido();
        configurarSeleccionClaveAlternaPorDefecto();
        configurarCampoCantidadUbicacion(txtCantidadUbicacion, comboUbicacion);
        configurarComboUbicacion(comboUbicacion, txtCantidadUbicacion);
        ubicacionesCapturadas.put(comboUbicacion, new ArrayList<>());

        // Configuración específica
        configurarCamposLecturaEspecificos();
        configurarCalculoPreciosEspecifico();

        inicializado = true;
        Platform.runLater(() -> {
            if (cbClaveProducto != null) {
                cbClaveProducto.requestFocus();
            }
        });
    }

    // === CONFIGURACIONES BASE COMUNES ===

    protected void configurarPresentaciones() {
        if (cbPresentacion != null) {
            cbPresentacion.setItems(presentaciones);
            cbPresentacion.setValue(null);

            cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Si es "pz", establecer factor 1 automáticamente
                    if (newVal.equalsIgnoreCase("pz")) {
                        if (txtFactor != null) {
                            txtFactor.setText("1");
                        }
                    }
                }

                // Limpiar factor si cambia de "pz" a otra presentación
                if (newVal != null && !newVal.equals(oldVal)) {
                    if (oldVal != null && oldVal.equalsIgnoreCase("pz") &&
                            (newVal == null || !newVal.equalsIgnoreCase("pz"))) {
                        if (txtFactor != null) {
                            txtFactor.clear();
                        }
                    }
                }

                presentacionValida = false;
                validarPresentacion();
                actualizarEstadoCascada();
            });
        }
    }

    protected void configurarPresentacionesRapidas() {
        if (cbPresentacionRapida != null) {
            cbPresentacionRapida.setItems(presentaciones);
            cbPresentacionRapida.setValue(null);

            // Listener para cuando se selecciona presentación en modo rápido
            cbPresentacionRapida.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Si es "pz", establecer factor 1 automáticamente
                    if (newVal.equalsIgnoreCase("pz")) {
                        if (txtFactorRapido != null) {
                            txtFactorRapido.setText("1");
                        }
                    }

                    // Validar presentación en modo rápido
                    validarPresentacionRapida();

                    // Recalcular disponibilidad si hay cantidad
                    if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                        programarValidacionCantidadRapida();
                    }
                }
            });
        }

        // Configurar validación de números enteros para factor rápido
        if (txtFactorRapido != null) {
            validarNumerosEnteros(txtFactorRapido);

            // Listener para cambios en factor rápido
            txtFactorRapido.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Validar factor en modo rápido
                    validarFactorRapidoCompleto(newVal);

                    // Si hay cantidad capturada, revalidarla
                    if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                        programarValidacionCantidadRapida();
                    }
                }
            });
        }

        // Configurar listener para cantidad rápida
        if (txtCantidadRapida != null) {
            txtCantidadRapida.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    programarValidacionCantidadRapida();
                }
            });
        }
    }

    protected void cargarUbicacionesDesdeBD() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return modelo.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
            }

            @Override
            protected void failed() {
                ubicaciones.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void configurarEventosBase() {
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
            }
            actualizarImagenProducto();
        });

        txtLote.textProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && !oldVal.equals(newVal)) {
                loteValidado = false;
                caducidadValidada = false;
                presentacionValida = false;
                factorValido = false;
                ubicacionValidada = false;
                cantidadDisponibleUbicacion = 0;
                cantidadTotalValida = false;
                dpCaducidad.setValue(null);
                cbPresentacion.setValue(null);
                if (txtFactor != null) txtFactor.clear();
                limpiarUbicacionPrimaria();
                limpiarPrecios();
                actualizarEstadoCascada();
            }
            programarValidacionLote(newVal);
        });

        if (btnGuardar != null) {
            btnGuardar.setOnAction(e -> guardarItem());
        }

        if (btnLimpiar != null) {
            btnLimpiar.setOnAction(e -> limpiarFormularioParaNuevo());
        }
    }

    protected void configurarLimpiezaPorCampoVacio() {
        configurarLimpiezaCombo(cbClaveProducto);
        configurarLimpiezaCombo(cbProductoNombre);
        configurarLimpiezaCombo(cbClaveAlterna);
    }

    protected void configurarLimpiezaCombo(ComboBox<String> comboBox) {
        if (comboBox == null) return;

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.isBlank()) {
                    comboBox.setValue(null);
                    limpiarValidacionesInventario();
                    limpiarFormularioDependiente();
                }
            });
        }
    }

    protected void limpiarFormularioDependiente() {
        if (txtDescripcion != null) txtDescripcion.clear();
        if (txtLote != null) txtLote.clear();
        if (dpCaducidad != null) dpCaducidad.setValue(null);
        if (txtCantidad != null) txtCantidad.clear();
        if (cbPresentacion != null) cbPresentacion.setValue(null);
        if (txtFactor != null) txtFactor.clear();
        if (txtNota != null) txtNota.clear();
        limpiarUbicacionPrimaria();
        limpiarPrecios();

        // Limpiar campos modo rápido
        if (txtCantidadRapida != null) txtCantidadRapida.clear();
        if (cbPresentacionRapida != null) cbPresentacionRapida.setValue(null);
        if (txtFactorRapido != null) txtFactorRapido.clear();

        // Limpiar precios rápidos específicos
        limpiarPreciosRapidos();
    }

    protected void actualizarImagenProducto() {
        if (previewImage == null || productoController == null) {
            return;
        }

        previewImage.setImage(null);
        String urlImagen = productoController.getUrlImagenSeleccionada();

        if (urlImagen == null || urlImagen.isBlank()) {
            return;
        }

        if (cacheImagenes.containsKey(urlImagen)) {
            previewImage.setImage(cacheImagenes.get(urlImagen));
            return;
        }

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                conexionFTP ftp = new conexionFTP();
                return ftp.getImageFromFTP(urlImagen);
            }
        };

        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            if (img != null) {
                cacheImagenes.put(urlImagen, img);
                previewImage.setImage(img);
            }
        });

        task.setOnFailed(e -> previewImage.setImage(null));
        new Thread(task).start();
    }

    protected void actualizarDescripcionDesdeProducto() {
        if (productoController != null && txtDescripcion != null) {
            String descripcion = productoController.getDescripcionSeleccionada();
            txtDescripcion.setText(descripcion);
        }
    }

    // === VALIDACIONES COMUNES ===

    protected void configurarValidacionesBase() {
        validarNumerosEnteros(txtCantidad);
        validarNumerosEnteros(txtFactor);
        validarNumerosEnteros(txtCantidadUbicacion);

        if (txtCantidadRapida != null) {
            validarNumerosEnteros(txtCantidadRapida);
        }

        if (txtFactorRapido != null) {
            validarNumerosEnteros(txtFactorRapido);
        }
    }

    protected void validarNumerosEnteros(TextField campo) {
        if (campo == null) return;

        campo.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("\\d*")) {
                campo.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    protected void validarLoteCompleto(String lote) {
        if (lote == null || lote.isBlank()) {
            loteValidado = false;
            actualizarEstadoCascada();
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            loteValidado = false;
            if (txtLote != null) txtLote.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de validar el lote.");
            actualizarEstadoCascada();
            return;
        }

        String loteSnapshot = lote;
        String productoSnapshot = idProducto;

        Task<ResultadoValidacionLote> task = new Task<>() {
            @Override
            protected ResultadoValidacionLote call() {
                // PRIMERO: Verificar si el producto con lote existe (con o sin caducidad)
                boolean productoExiste = modelo.verificarExistenciaProducto(productoSnapshot, loteSnapshot);

                if (!productoExiste) {
                    return ResultadoValidacionLote.loteInvalido();
                }

                // SEGUNDO: Verificar disponibilidad específica
                GenericDAO.ValidacionDisponibilidadSalida validacion =
                        modelo.validarEntradaYDisponibilidadLoteProducto(loteSnapshot, productoSnapshot);

                if (!validacion.isEntradaCompletada()) {
                    return ResultadoValidacionLote.entradaPendiente();
                }

                if (validacion.getDisponiblesSinSalida() <= 0) {
                    return ResultadoValidacionLote.salidaEnProceso();
                }

                // TERCERO: Obtener caducidad (puede ser null para productos sin caducidad)
                Optional<LocalDate> caducidad = modelo.obtenerCaducidadParaLoteProducto(
                        loteSnapshot, productoSnapshot);

                // IMPORTANTE: Permitir productos sin caducidad (caducidad puede ser Optional.empty())
                return ResultadoValidacionLote.ok(caducidad.orElse(null));
            }

            @Override
            protected void succeeded() {
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                String idActual = productoController != null ? productoController.getIdSeleccionado() : "";

                if (!loteSnapshot.equals(loteActual) || !productoSnapshot.equals(idActual)) {
                    return;
                }

                ResultadoValidacionLote resultado = getValue();

                if (resultado.estado == EstadoValidacionLote.ENTRADA_PENDIENTE) {
                    loteValidado = false;
                    if (txtLote != null) txtLote.clear();
                    if (dpCaducidad != null) dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia",
                            "El producto no está en stock, posiblemente esté en tus traspasos de entrada");
                } else if (resultado.estado == EstadoValidacionLote.SALIDA_EN_PROCESO) {
                    loteValidado = false;
                    if (txtLote != null) txtLote.clear();
                    if (dpCaducidad != null) dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia",
                            "El producto está en proceso de salida a una sucursal");
                } else if (resultado.estado == EstadoValidacionLote.LOTE_INVALIDO) {
                    loteValidado = false;
                    if (txtLote != null) txtLote.clear();
                    if (dpCaducidad != null) dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia", "El lote no corresponde al producto seleccionado.");
                } else {
                    loteValidado = true;
                    cargarPreciosDesdeProducto();
                    if (dpCaducidad != null) dpCaducidad.setValue(resultado.caducidad);
                    caducidadValidada = true;
                }

                ubicacionValidada = false;
                cantidadTotalValida = false;
                presentacionValida = false;
                factorValido = false;
                actualizarEstadoCascada();
            }

            @Override
            protected void failed() {
                loteValidado = false;
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void validarPresentacionRapida() {
        if (cbPresentacionRapida == null) return;

        String presentacion = cbPresentacionRapida.getValue();
        if (presentacion == null || presentacion.isBlank()) {
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de la presentación.");
            if (cbPresentacionRapida != null) {
                cbPresentacionRapida.setValue(null);
            }
            return;
        }

        String presentacionSnapshot = presentacion;
        String idProductoSnapshot = idProducto;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existePresentacionParaProducto(idProductoSnapshot, presentacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;

                if (!presentacionSnapshot.equals(presentacionActual) || !idProductoSnapshot.equals(idProductoActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    if (cbPresentacionRapida != null) {
                        cbPresentacionRapida.setValue(null);
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "La presentación no existe para el producto seleccionado.");
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void validarFactorRapidoCompleto(String factorTexto) {
        if (cbPresentacionRapida == null) return;

        String presentacion = cbPresentacionRapida.getValue();
        if (presentacion == null || presentacion.isBlank()) {
            if (!factorTexto.isBlank()) {
                mostrarAlertaSinEspera("Advertencia", "Debe capturar la presentación antes del factor.");
                if (txtFactorRapido != null) {
                    txtFactorRapido.clear();
                }
            }
            return;
        }

        if (factorTexto.isBlank()) {
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número mayor a 0.");
                if (txtFactorRapido != null) {
                    txtFactorRapido.clear();
                }
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número válido.");
            if (txtFactorRapido != null) {
                txtFactorRapido.clear();
            }
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes del factor.");
            if (txtFactorRapido != null) {
                txtFactorRapido.clear();
            }
            return;
        }

        String presentacionSnapshot = presentacion;
        String idProductoSnapshot = idProducto;
        int factorSnapshot = factor;
        String factorTextoSnapshot = factorTexto;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existeFactorParaProductoPresentacion(
                        idProductoSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String factorActual = txtFactorRapido != null ? txtFactorRapido.getText() : null;

                if (!presentacionSnapshot.equals(presentacionActual) ||
                        !idProductoSnapshot.equals(idProductoActual) ||
                        !factorTextoSnapshot.equals(factorActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    if (txtFactorRapido != null) {
                        txtFactorRapido.clear();
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "El factor no corresponde con la presentación seleccionada para este producto.");
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // === CÁLCULO DE PRECIOS BASE ===

    protected void configurarCalculoPreciosBase() {
        if (txtCantidad != null) {
            txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
                validarCantidadTotalDisponible();
                recalcularPrecios();
                actualizarEstadoCascada();
            });
        }

        if (txtCantidadRapida != null) {
            txtCantidadRapida.textProperty().addListener((obs, oldVal, newVal) -> {
                programarValidacionCantidadRapida();
                recalcularPreciosRapido();
            });
        }

        if (txtFactor != null) {
            txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
                programarValidacionFactor(newVal);
            });
        }
    }

    protected void configurarCamposLecturaBase() {
        // Campos comunes de solo lectura
        if (txtDescripcion != null) txtDescripcion.setEditable(false);
        if (dpCaducidad != null) {
            dpCaducidad.setEditable(false);
            dpCaducidad.setMouseTransparent(true);
            dpCaducidad.setFocusTraversable(false);
        }

        // Configuración específica
        configurarCamposLecturaEspecificos();
    }

    // === CONFIGURACIÓN DE UBICACIONES ===

    protected void configurarAutocompletadoUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) return;

        comboBox.setItems(ubicaciones);
        comboBox.setEditable(false);

        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            new AutoCompleteComboBoxListener<>(comboBox);
        }

        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarTextoUbicacion(comboBox);
                }
            });
        }

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (comboBox.isEditable() && newVal != null && !newVal.isBlank() && comboBox.getEditor() != null) {
                comboBox.getEditor().setText(newVal);
            }
            if (oldVal != null && !oldVal.equals(newVal)) {
                limpiarCapturasCombo(comboBox);
            }
        });
    }

    protected void validarTextoUbicacion(ComboBox<String> comboBox) {
        String valor = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!ubicaciones.contains(valor)) {
            comboBox.setValue(null);
            if (comboBox.getEditor() != null) {
                comboBox.getEditor().clear();
            }
            mostrarAlerta("Advertencia", "La ubicación no existe. Seleccione una válida.");
        }
    }

    protected void limpiarUbicacionPrimaria() {
        if (comboUbicacion != null) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
        }
        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.clear();
        }
        ultimaCantidadUbicacionValidada.clear();
        limpiarCapturasCombo(comboUbicacion);
    }

    protected void configurarCampoCantidadUbicacion(TextField campoCantidad, ComboBox<String> combo) {
        if (campoCantidad == null || combo == null) return;

        validarNumerosEnteros(campoCantidad);
        campoCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank() &&
                    (combo.getValue() == null || combo.getValue().isBlank())) {
                campoCantidad.clear();
                mostrarAlertaCascada("Debe capturar la ubicación antes de la cantidad en ubicación.");
                return;
            }
            programarValidacionCantidadUbicacion(campoCantidad, combo);
        });
    }

    protected void configurarComboUbicacion(ComboBox<String> combo, TextField campoCantidad) {
        if (combo == null) return;

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank() &&
                    (txtFactor.getText() == null || txtFactor.getText().isBlank())) {
                combo.setValue(null);
                if (combo.getEditor() != null) {
                    combo.getEditor().clear();
                }
                if (campoCantidad != null) {
                    campoCantidad.clear();
                }
                mostrarAlertaCascada("Debe capturar el factor antes de la ubicación.");
                return;
            }

            if (newVal != null && !newVal.isBlank() && ubicacionDuplicada(combo, newVal)) {
                combo.setValue(null);
                if (combo.getEditor() != null) {
                    combo.getEditor().clear();
                }
                mostrarAlertaCascada("No se puede seleccionar la misma ubicación más de una vez.");
                return;
            }

            limpiarCapturasCombo(combo);
            if (combo == comboUbicacion) {
                validarUbicacion();
            } else {
                validarUbicacionParaCombo(combo, campoCantidad);
            }
            actualizarEstadoCascada();
        });
    }

    protected void registrarCantidadUbicacion(ComboBox<String> combo, int cantidad) {
        String ubicacion = combo != null ? combo.getValue() : null;
        if (ubicacion == null || ubicacion.isBlank() || cantidad <= 0) {
            return;
        }
        List<UbicacionCompra> lista = ubicacionesCapturadas.computeIfAbsent(combo, key -> new ArrayList<>());
        lista.add(new UbicacionCompra(ubicacion, cantidad));
    }

    protected void limpiarCapturasCombo(ComboBox<String> combo) {
        if (combo == null) return;

        List<UbicacionCompra> lista = ubicacionesCapturadas.get(combo);
        if (lista != null) {
            lista.clear();
        }
    }

    protected boolean ubicacionDuplicada(ComboBox<String> comboActual, String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank() || contenedorUbicaciones == null) {
            return false;
        }

        String ubicacionNormalizada = ubicacion.trim();

        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) {
                continue;
            }
            HBox fila = (HBox) nodo;
            if (fila.getChildren().isEmpty()) {
                continue;
            }

            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            if (contenedorUbicacion.getChildren().size() < 2) {
                continue;
            }

            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            if (combo == null || combo == comboActual) {
                continue;
            }

            String valor = combo.getValue();
            if (valor != null && !valor.isBlank() && ubicacionNormalizada.equals(valor.trim())) {
                return true;
            }
        }

        return false;
    }

    // === MANEJO DE EVENTOS Y CASCADA ===

    protected void configurarManejoEnterBase() {
        if (cbClaveProducto != null) {
            cbClaveProducto.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && cbProductoNombre != null) {
                    cbProductoNombre.requestFocus();
                    event.consume();
                }
            });
        }

        if (cbProductoNombre != null) {
            cbProductoNombre.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && cbClaveAlterna != null) {
                    cbClaveAlterna.requestFocus();
                    event.consume();
                }
            });
        }

        if (cbClaveAlterna != null) {
            cbClaveAlterna.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && txtCantidad != null) {
                    txtCantidad.requestFocus();
                    event.consume();
                }
            });
        }

        if (txtCantidad != null) {
            txtCantidad.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && cbPresentacion != null) {
                    cbPresentacion.requestFocus();
                    event.consume();
                }
            });
        }

        if (cbPresentacion != null) {
            cbPresentacion.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && txtFactor != null) {
                    txtFactor.requestFocus();
                    event.consume();
                }
            });
        }

        if (txtFactor != null) {
            txtFactor.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && btnGuardar != null) {
                    guardarItem();
                    event.consume();
                }
            });
        }

        if (txtLote != null) {
            txtLote.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && dpCaducidad != null) {
                    dpCaducidad.requestFocus();
                    event.consume();
                }
            });
        }

        if (dpCaducidad != null) {
            dpCaducidad.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && txtCantidad != null) {
                    txtCantidad.requestFocus();
                    event.consume();
                }
            });
        }

        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && btnGuardar != null) {
                    guardarItem();
                    event.consume();
                }
            });
        }
    }

    protected void configurarCascada() {
        actualizarEstadoCascada();

        if (txtLote != null) {
            txtLote.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarCamposDesdeLote();
                }
            });
        }

        if (cbPresentacion != null) {
            cbPresentacion.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarCamposDesdePresentacion();
                }
            });
        }

        if (txtCantidad != null) {
            txtCantidad.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarCamposDesdeCantidad();
                }
            });
        }

        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarCamposDesdeCantidadUbicacion();
                }
            });
        }

        if (txtFactor != null) {
            txtFactor.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarCamposDesdeFactor();
                }
            });
        }
    }

    protected void configurarModoRapido() {
        if (tabPaneModo != null) {
            tabPaneModo.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == tabRapido) {
                    recalcularPreciosRapido();
                } else {
                    recalcularPrecios();
                }
            });
        }
    }

    protected boolean esModoRapido() {
        return tabPaneModo != null && tabRapido != null &&
                tabPaneModo.getSelectionModel().getSelectedItem() == tabRapido;
    }

    protected void configurarSeleccionClaveAlternaPorDefecto() {
        if (cbClaveAlterna == null) return;

        cbClaveAlterna.getItems().addListener((javafx.collections.ListChangeListener<String>) change -> {
            if (cbClaveAlterna.getItems().isEmpty()) {
                return;
            }
            if (!seleccionarClaveAlternaPendiente) {
                return;
            }
            seleccionarClaveAlternaPendiente = false;
            Platform.runLater(this::seleccionarPrimerClaveAlternaDisponible);
        });
    }

    protected void seleccionarPrimerClaveAlternaDisponible() {
        if (cbClaveAlterna == null || cbClaveAlterna.getItems().isEmpty()) {
            return;
        }

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

    protected void actualizarEstadoCascada() {
        if (txtLote != null) txtLote.setDisable(false);
        if (txtCantidad != null) txtCantidad.setDisable(false);
        if (cbPresentacion != null) cbPresentacion.setDisable(false);
        if (txtFactor != null) txtFactor.setDisable(false);
        if (comboUbicacion != null) comboUbicacion.setDisable(false);
        if (txtCantidadUbicacion != null) txtCantidadUbicacion.setDisable(false);
    }

    // === VALIDACIONES EN CASCADA ===

    protected void validarCamposDesdeLote() {
        String lote = txtLote != null ? txtLote.getText().trim() : "";
        if (!lote.isBlank()) {
            validarLoteCompleto(lote);
            ultimoLoteValidado = loteValidado ? lote : "";
        } else if (txtCantidad != null && !txtCantidad.getText().isBlank()) {
            txtCantidad.clear();
            mostrarAlertaCascada("Debe capturar el lote antes de la cantidad.");
            return;
        }
        if (!loteValidado) {
            return;
        }
        validarCantidadTotalDisponible();
    }

    protected void validarCamposDesdeCantidad() {
        validarCamposDesdeLote();
        if (!loteValidado) {
            return;
        }

        String cantidadTexto = txtCantidad != null ? txtCantidad.getText().trim() : "";
        if (cantidadTexto.isBlank()) {
            return;
        }

        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;

        // Verificar que haya factor para presentaciones que no sean "pz"
        if (!presentacion.equalsIgnoreCase("pz")) {
            if (txtFactor == null || txtFactor.getText() == null || txtFactor.getText().isBlank()) {
                txtCantidad.clear();
                mostrarAlertaCascada("Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                return;
            }
        }

        if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isBlank()) {
            if (txtFactor != null && txtFactor.getText() != null && !txtFactor.getText().isBlank()) {
                txtFactor.clear();
                mostrarAlertaCascada("Debe capturar la presentación antes del factor.");
                return;
            }
        }
    }

    protected void validarCamposDesdePresentacion() {
        validarCamposDesdeLote();
        if (!loteValidado) {
            return;
        }
        validarPresentacion();
    }

    protected void validarCamposDesdeFactor() {
        validarCamposDesdeCantidad();
        if (!cantidadTotalValida) {
            return;
        }

        String factorTexto = txtFactor != null ? txtFactor.getText().trim() : "";
        if (factorTexto.isBlank()) {
            return;
        }

        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            if (txtFactor != null) txtFactor.clear();
            mostrarAlertaCascada("Debe seleccionar una presentación antes del factor.");
            return;
        }

        validarPresentacion();
        if (txtFactor != null && !txtFactor.getText().isBlank()) {
            validarFactorCompleto(txtFactor.getText().trim());
        }

        if ((comboUbicacion != null && comboUbicacion.getValue() != null && !comboUbicacion.getValue().isBlank())
                && (txtFactor == null || txtFactor.getText() == null || txtFactor.getText().isBlank())) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
            mostrarAlertaCascada("Debe capturar el factor antes de la ubicación.");
            return;
        }
    }

    protected void validarCamposDesdeCantidadUbicacion() {
        validarCamposDesdeFactor();
        if (!factorValido) {
            return;
        }
        validarUbicacion();
        if (txtCantidadUbicacion != null && comboUbicacion != null) {
            validarCantidadDisponible(txtCantidadUbicacion, comboUbicacion);
        }
    }

    protected void mostrarAlertaCascada(String mensaje) {
        mostrarAlertaSinEspera("Advertencia", mensaje);
    }

    // === MÉTODOS DE VALIDACIÓN DE UBICACIONES ===

    protected void validarUbicacion() {
        if (!loteValidado || !caducidadValidada) {
            ubicacionValidada = false;
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        String lote = txtLote != null ? txtLote.getText().trim() : "";
        LocalDate caducidad = dpCaducidad != null ? dpCaducidad.getValue() : null;
        String ubicacion = comboUbicacion != null ? comboUbicacion.getValue() : null;

        if (idProducto == null || idProducto.isBlank() ||
                ubicacion == null || ubicacion.isBlank() ||
                lote.isBlank()) {
            ubicacionValidada = false;
            return;
        }

        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                boolean existe = modelo.existeLoteCaducidadUbicacionProducto(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                if (existe) {
                    cantidadDisponibleUbicacion = modelo.obtenerCantidadDisponibleProductoUbicacion(
                            idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                }
                return existe;
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                LocalDate caducidadActual = dpCaducidad != null ? dpCaducidad.getValue() : null;
                String ubicacionActual = comboUbicacion != null ? comboUbicacion.getValue() : null;

                if (!idProductoSnapshot.equals(idProductoActual) ||
                        !loteSnapshot.equals(loteActual) ||
                        !Objects.equals(caducidadSnapshot, caducidadActual) ||
                        !ubicacionSnapshot.equals(ubicacionActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    ubicacionValidada = false;
                    if (comboUbicacion != null) {
                        comboUbicacion.setValue(null);
                        if (comboUbicacion.getEditor() != null) {
                            comboUbicacion.getEditor().clear();
                        }
                    }
                    if (txtCantidadUbicacion != null) {
                        txtCantidadUbicacion.clear();
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "No hay productos en esa ubicación para el lote y caducidad indicados.");
                } else {
                    ubicacionValidada = true;
                }
                actualizarEstadoCascada();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void validarUbicacionParaCombo(ComboBox<String> combo, TextField campoCantidad) {
        if (combo == null) return;

        if (!loteValidado || !caducidadValidada) {
            if (combo == comboUbicacion) {
                ubicacionValidada = false;
            }
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        String lote = txtLote != null ? txtLote.getText().trim() : "";
        LocalDate caducidad = dpCaducidad != null ? dpCaducidad.getValue() : null;
        String ubicacion = combo.getValue() != null ? combo.getValue().trim() : "";

        if (idProducto == null || idProducto.isBlank() || ubicacion.isBlank()) {
            if (combo == comboUbicacion) {
                ubicacionValidada = false;
            }
            return;
        }

        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                boolean existe = modelo.existeLoteCaducidadUbicacionProducto(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                if (existe) {
                    cantidadDisponibleUbicacion = modelo.obtenerCantidadDisponibleProductoUbicacion(
                            idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                }
                return existe;
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                LocalDate caducidadActual = dpCaducidad != null ? dpCaducidad.getValue() : null;
                String ubicacionActual = combo.getValue() != null ? combo.getValue().trim() : "";

                if (!idProductoSnapshot.equals(idProductoActual) ||
                        !loteSnapshot.equals(loteActual) ||
                        !Objects.equals(caducidadSnapshot, caducidadActual) ||
                        !ubicacionSnapshot.equals(ubicacionActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    if (combo == comboUbicacion) {
                        ubicacionValidada = false;
                    }
                    combo.setValue(null);
                    if (combo.getEditor() != null) {
                        combo.getEditor().clear();
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "No hay productos en esa ubicación para el lote y caducidad indicados.");
                } else if (combo == comboUbicacion) {
                    ubicacionValidada = true;
                }
                actualizarEstadoCascada();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void validarCantidadDisponible(TextField campoCantidad, ComboBox<String> combo) {
        if (campoCantidad == null || combo == null) return;

        String texto = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";
        if (texto.isBlank()) {
            return;
        }

        if (combo.getValue() == null || combo.getValue().isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe capturar la ubicación antes de la cantidad en ubicación.");
            return;
        }

        if (!loteValidado || !caducidadValidada) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe capturar un lote y caducidad válidos antes de la cantidad.");
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        String lote = txtLote != null ? txtLote.getText().trim() : "";
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        String factorTexto = txtFactor != null ? txtFactor.getText() : null;
        LocalDate caducidad = dpCaducidad != null ? dpCaducidad.getValue() : null;
        String ubicacion = combo.getValue() != null ? combo.getValue().trim() : "";

        if (idProducto == null || idProducto.isBlank() ||
                lote.isBlank() ||
                presentacion == null || presentacion.isBlank() ||
                factorTexto == null || factorTexto.isBlank() ||
                ubicacion.isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe completar todas las características del producto antes de la cantidad.");
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                campoCantidad.clear();
                mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            campoCantidad.clear();
            mostrarAlerta("Error", "El factor debe ser un número válido.");
            return;
        }

        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;
        LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;
        String cantidadTextoSnapshot = texto;

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleDetalle(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, presentacionSnapshot, factorSnapshot,
                        ubicacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                String presentacionActual = cbPresentacion != null ? cbPresentacion.getValue() : null;
                String factorActualText = txtFactor != null ? txtFactor.getText() : null;
                int factorActual = 0;
                try {
                    factorActual = factorActualText != null ? Integer.parseInt(factorActualText) : 0;
                } catch (NumberFormatException e) {
                    factorActual = 0;
                }
                LocalDate caducidadActual = dpCaducidad != null ? dpCaducidad.getValue() : null;
                String ubicacionActual = combo.getValue() != null ? combo.getValue().trim() : "";
                String cantidadActual = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";

                if (!idProductoSnapshot.equals(idProductoActual) ||
                        !loteSnapshot.equals(loteActual) ||
                        !presentacionSnapshot.equals(presentacionActual) ||
                        factorSnapshot != factorActual ||
                        !Objects.equals(caducidadSnapshot, caducidadActual) ||
                        !ubicacionSnapshot.equals(ubicacionActual) ||
                        !cantidadTextoSnapshot.equals(cantidadActual)) {
                    return;
                }

                cantidadDisponibleUbicacion = getValue();
                if (cantidadDisponibleUbicacion <= 0) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia",
                            "No hay existencia en esa ubicación con las características indicadas.");
                    return;
                }

                int cantidad = parseEntero(cantidadActual);
                if (cantidad <= 0) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                    return;
                }
                if (cantidad > cantidadDisponibleUbicacion) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia", "La cantidad supera la disponible en esa ubicación.");
                    return;
                }
                registrarCantidadUbicacion(combo, cantidad);
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // === MÉTODOS DE DEBOUNCE ===

    protected void programarValidacionLote(String nuevoValor) {
        loteDebounce.stop();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimoLoteValidado = "";
            return;
        }

        loteDebounce.setOnFinished(event -> {
            String loteActual = txtLote != null ? txtLote.getText().trim() : "";
            if (loteActual.isBlank()) {
                return;
            }
            if (loteActual.equals(ultimoLoteValidado) && loteValidado) {
                return;
            }
            validarLoteCompleto(loteActual);
            if (loteValidado) {
                ultimoLoteValidado = loteActual;
            }
        });
        loteDebounce.playFromStart();
    }

    protected void programarValidacionCantidadRapida() {
        if (txtCantidadRapida == null) return;

        cantidadRapidaDebounce.stop();
        String nuevoValor = txtCantidadRapida.getText();

        if (nuevoValor == null || nuevoValor.isBlank()) {
            cantidadRapidaValida = false;
            return;
        }

        // Verificar que haya producto seleccionado
        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            txtCantidadRapida.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de capturar la cantidad.");
            return;
        }

        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            txtCantidadRapida.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione una presentación antes de capturar la cantidad.");
            return;
        }

        // Verificar que haya factor para presentaciones que no sean "pz"
        if (!presentacion.equalsIgnoreCase("pz")) {
            if (txtFactorRapido == null || txtFactorRapido.getText() == null || txtFactorRapido.getText().isBlank()) {
                txtCantidadRapida.clear();
                mostrarAlertaSinEspera("Advertencia",
                        "Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                return;
            }
        }

        cantidadRapidaDebounce.setOnFinished(event -> validarCantidadRapidaDisponible());
        cantidadRapidaDebounce.playFromStart();
    }

    protected void validarCantidadRapidaDisponible() {
        if (txtCantidadRapida == null) return;

        String cantidadTexto = txtCantidadRapida.getText() != null ? txtCantidadRapida.getText().trim() : "";
        if (cantidadTexto.isBlank()) {
            cantidadRapidaValida = false;
            return;
        }

        // Obtener presentación y factor del modo rápido
        String presentacionRapida = "pz"; // Valor por defecto
        int factorRapido = 1; // Valor por defecto

        if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null) {
            presentacionRapida = cbPresentacionRapida.getValue().trim();
        }

        if (txtFactorRapido != null && txtFactorRapido.getText() != null && !txtFactorRapido.getText().isBlank()) {
            try {
                factorRapido = Integer.parseInt(txtFactorRapido.getText().trim());
                if (factorRapido <= 0) {
                    cantidadRapidaValida = false;
                    mostrarAlertaSinEspera("Advertencia", "El factor debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                cantidadRapidaValida = false;
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número válido.");
                return;
            }
        } else if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().equalsIgnoreCase("pz")) {
            // Si no es "pz" y no tiene factor, mostrar error
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
        } catch (NumberFormatException e) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "La cantidad debe ser un número válido.");
            return;
        }

        if (cantidad <= 0) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "La cantidad debe ser mayor a 0.");
            return;
        }

        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de la cantidad.");
            return;
        }

        String idSnapshot = idProducto;
        int cantidadSnapshot = cantidad;
        String presentacionSnapshot = presentacionRapida;
        int factorSnapshot = factorRapido;

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                        idSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String textoActual = txtCantidadRapida.getText() != null ? txtCantidadRapida.getText().trim() : "";
                String presentacionActual = cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                        ? cbPresentacionRapida.getValue().trim() : "pz";
                String factorActual = txtFactorRapido != null && txtFactorRapido.getText() != null
                        ? txtFactorRapido.getText().trim() : "1";
                int factorNum = 1;
                try {
                    factorNum = Integer.parseInt(factorActual);
                } catch (NumberFormatException e) {
                    factorNum = 1;
                }

                if (!textoActual.equals(String.valueOf(cantidadSnapshot)) ||
                        !presentacionActual.equals(presentacionSnapshot) ||
                        factorNum != factorSnapshot) {
                    return;
                }

                int disponible = getValue() != null ? getValue() : 0;
                if (cantidadSnapshot > disponible) {
                    cantidadRapidaValida = false;
                    mostrarAlertaSinEspera("Advertencia",
                            "La cantidad supera la disponible para la presentación " + presentacionSnapshot
                                    + " con factor " + factorSnapshot + ".");
                } else {
                    cantidadRapidaValida = true;
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void programarValidacionCantidadUbicacion(TextField campoCantidad, ComboBox<String> combo) {
        PauseTransition debounce = debounceCantidadUbicacion.computeIfAbsent(campoCantidad,
                key -> new PauseTransition(DEBOUNCE_TIEMPO));
        debounce.stop();
        String nuevoValor = campoCantidad.getText();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimaCantidadUbicacionValidada.remove(campoCantidad);
            return;
        }

        if (combo.getValue() == null || combo.getValue().isBlank()) {
            campoCantidad.clear();
            mostrarAlertaCascada("Debe capturar la ubicación antes de la cantidad en ubicación.");
            return;
        }

        debounce.setOnFinished(event -> {
            String cantidadActual = campoCantidad.getText() != null
                    ? campoCantidad.getText().trim()
                    : "";
            if (cantidadActual.isBlank()) {
                ultimaCantidadUbicacionValidada.remove(campoCantidad);
                return;
            }
            if (cantidadActual.equals(ultimaCantidadUbicacionValidada.get(campoCantidad))) {
                return;
            }
            validarCantidadDisponible(campoCantidad, combo);
            actualizarEstadoCascada();
            ultimaCantidadUbicacionValidada.put(campoCantidad, cantidadActual);
        });
        debounce.playFromStart();
    }

    protected void programarValidacionFactor(String nuevoValor) {
        factorDebounce.stop();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimoFactorValidado = "";
            return;
        }

        if (cbPresentacion == null || cbPresentacion.getValue() == null || cbPresentacion.getValue().isBlank()) {
            if (txtFactor != null) txtFactor.clear();
            mostrarAlertaCascada("Debe capturar la presentación antes del factor.");
            return;
        }

        factorDebounce.setOnFinished(event -> {
            String factorActual = txtFactor != null ? txtFactor.getText().trim() : "";
            if (factorActual.isBlank()) {
                ultimoFactorValidado = "";
                return;
            }
            if (factorActual.equals(ultimoFactorValidado)) {
                return;
            }
            validarFactorCompleto(factorActual);
            if (factorValido) {
                ultimoFactorValidado = factorActual;
            }
        });
        factorDebounce.playFromStart();
    }

    // === VALIDACIONES DE CANTIDAD TOTAL ===

    protected void validarCantidadTotalDisponible() {
        if (!caducidadValidada) {
            cantidadTotalValida = false;
            return;
        }

        String texto = txtCantidad != null ? txtCantidad.getText().trim() : "";
        if (texto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }

        int cantidad = parseEntero(texto);
        if (cantidad <= 0) {
            cantidadTotalValida = false;
            return;
        }

        // Validar presentación y factor también
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        String factorTexto = txtFactor != null ? txtFactor.getText() : null;

        if (presentacion == null || presentacion.isBlank() ||
                factorTexto == null || factorTexto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                if (txtCantidad != null) txtCantidad.clear();
                cantidadTotalValida = false;
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            if (txtCantidad != null) txtCantidad.clear();
            cantidadTotalValida = false;
            mostrarAlertaSinEspera("Error", "El factor debe ser un número válido.");
            return;
        }

        String lote = txtLote != null ? txtLote.getText().trim() : "";
        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }

        String loteSnapshot = lote;
        String idSnapshot = idProducto;
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;
        int cantidadSnapshot = cantidad;

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                // Nueva consulta que incluya presentación y factor
                return modelo.obtenerCantidadDisponibleProductoLoteCaducidadPresentacionFactor(
                        idSnapshot, loteSnapshot, null, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                String idActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String presentacionActual = cbPresentacion != null ? cbPresentacion.getValue() : null;
                String factorActualText = txtFactor != null ? txtFactor.getText() : null;
                int factorActual = 0;
                try {
                    factorActual = factorActualText != null ? Integer.parseInt(factorActualText) : 0;
                } catch (NumberFormatException e) {
                    factorActual = 0;
                }
                int cantidadActual = parseEntero(txtCantidad != null ? txtCantidad.getText() : "");

                if (!loteSnapshot.equals(loteActual) ||
                        !idSnapshot.equals(idActual) ||
                        !presentacionSnapshot.equals(presentacionActual) ||
                        factorSnapshot != factorActual ||
                        cantidadActual != cantidadSnapshot) {
                    return;
                }

                int disponible = getValue();
                if (cantidadSnapshot > disponible) {
                    if (txtCantidad != null) txtCantidad.clear();
                    cantidadTotalValida = false;
                    mostrarAlertaSinEspera("Advertencia",
                            "La cantidad supera la disponible para el lote, presentación y factor seleccionados.");
                } else {
                    cantidadTotalValida = true;
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // === VALIDACIONES DE PRESENTACIÓN Y FACTOR ===

    protected void validarPresentacion() {
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        if (!loteValidado || !caducidadValidada) {
            presentacionValida = false;
            if (presentacion != null && !presentacion.isBlank()) {
                if (cbPresentacion != null) cbPresentacion.setValue(null);
                mostrarAlertaSinEspera("Advertencia", "Debe capturar un lote válido antes de la presentación.");
            }
            return;
        }

        String lote = txtLote != null ? txtLote.getText().trim() : "";
        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (presentacion == null || presentacion.isBlank() || idProducto == null || idProducto.isBlank()) {
            presentacionValida = false;
            return;
        }

        String presentacionSnapshot = presentacion;
        String loteSnapshot = lote;
        String idProductoSnapshot = idProducto;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existePresentacionParaProductoLote(idProductoSnapshot, loteSnapshot, presentacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacion != null ? cbPresentacion.getValue() : null;
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;

                if (!presentacionSnapshot.equals(presentacionActual) ||
                        !loteSnapshot.equals(loteActual) ||
                        !idProductoSnapshot.equals(idProductoActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    presentacionValida = false;
                    if (cbPresentacion != null) cbPresentacion.setValue(null);
                    mostrarAlertaSinEspera("Advertencia",
                            "La presentación no existe para el lote y producto seleccionados.");
                } else {
                    presentacionValida = true;
                    ultimaPresentacionValidada = presentacionSnapshot;
                }
                factorValido = false;
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    protected void validarFactorCompleto(String factorTexto) {
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            factorValido = false;
            if (!factorTexto.isBlank()) {
                if (txtFactor != null) txtFactor.clear();
                mostrarAlertaSinEspera("Advertencia", "Debe capturar la presentación antes del factor.");
            }
            return;
        }

        // Si es "pz" y tiene factor 1, validar automáticamente
        if (presentacion.equalsIgnoreCase("pz") && "1".equals(factorTexto)) {
            factorValido = true;
            ultimoFactorValidado = "1";
            return;
        }

        validarPresentacion();
        if (!presentacionValida) {
            factorValido = false;
            return;
        }

        if (!presentacion.equals(ultimaPresentacionValidada)) {
            factorValido = false;
            if (txtFactor != null) txtFactor.clear();
            mostrarAlerta("Advertencia", "Seleccione la presentación válida antes de capturar el factor.");
            return;
        }

        if (factorTexto.isBlank()) {
            factorValido = false;
            return;
        }

        int factor = parseEntero(factorTexto);
        if (factor <= 0) {
            factorValido = false;
            if (txtFactor != null) txtFactor.clear();
            mostrarAlerta("Advertencia", "El factor debe ser un número mayor a 0.");
            return;
        }

        String lote = txtLote != null ? txtLote.getText().trim() : "";
        String idProducto = productoController != null ? productoController.getIdSeleccionado() : null;
        if (idProducto == null || idProducto.isBlank() || presentacion == null || presentacion.isBlank()) {
            factorValido = false;
            return;
        }

        String presentacionSnapshot = presentacion;
        String loteSnapshot = lote;
        String idProductoSnapshot = idProducto;
        int factorSnapshot = factor;
        String factorTextoSnapshot = factorTexto;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existeFactorParaProductoLotePresentacion(
                        idProductoSnapshot, loteSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacion != null ? cbPresentacion.getValue() : null;
                String loteActual = txtLote != null ? txtLote.getText().trim() : "";
                String idProductoActual = productoController != null ? productoController.getIdSeleccionado() : null;
                String factorActual = txtFactor != null ? txtFactor.getText() : null;

                if (!presentacionSnapshot.equals(presentacionActual) ||
                        !loteSnapshot.equals(loteActual) ||
                        !idProductoSnapshot.equals(idProductoActual) ||
                        !factorTextoSnapshot.equals(factorActual)) {
                    return;
                }

                boolean existe = getValue();
                if (!existe) {
                    factorValido = false;
                    if (txtFactor != null) txtFactor.clear();
                    mostrarAlertaSinEspera("Advertencia",
                            "El factor no corresponde con la presentación y lote seleccionados.");
                } else {
                    factorValido = true;
                    ultimoFactorValidado = factorTextoSnapshot;
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // === MÉTODOS UTILITARIOS ===

    protected int parseEntero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (Exception e) {
            return 0;
        }
    }

    protected BigDecimal parseDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    protected String formatearDecimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected void limpiarValidacionesInventario() {
        loteValidado = false;
        caducidadValidada = false;
        presentacionValida = false;
        factorValido = false;
        ubicacionValidada = false;
        cantidadDisponibleUbicacion = 0;
        cantidadTotalValida = false;
        cantidadRapidaValida = false;
        ultimoLoteValidado = "";
        ultimoFactorValidado = "";
        ultimaPresentacionValidada = "";
        ultimaCantidadUbicacionValidada.clear();
    }

    // === MÉTODOS PARA AGREGAR UBICACIONES ===

    @FXML
    protected void agregarUbicacion() {
        agregarUbicacionCombo();
    }

    protected void agregarUbicacionCombo() {
        if (contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicaciones);
        nuevoCombo.setEditable(false);
        nuevoCombo.setPromptText("Selecciona una ubicación");
        vboxUbicacion.getChildren().addAll(new Label("Ubicación:"), nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(new Label("Cantidad en ubicación:"), txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button botonEliminar = new Button();
        String styleV = "-fx-background-color: #d3d3d3; -fx-border-color: #999; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;  -fx-max-width: 25; -fx-max-height: 25; -fx-background-radius: 5; -fx-text-fill: black;";
        botonEliminar.setStyle(styleV);
        botonEliminar.setText("-");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().addAll(botonEliminar);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);
        botonEliminar.setOnAction(this::manejarEliminar);

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        if (contenedorUbicaciones != null) {
            contenedorUbicaciones.getChildren().add(nuevaFila);
        }

        configurarCampoCantidadUbicacion(txtCantidad, nuevoCombo);
        configurarComboUbicacion(nuevoCombo, txtCantidad);
        ubicacionesCapturadas.put(nuevoCombo, new ArrayList<>());

        contadorFilas++;
    }

    protected void manejarEliminar(ActionEvent event) {
        if (!(event.getSource() instanceof Button)) return;

        Button botonPresionado = (Button) event.getSource();
        VBox contenedorBoton = (VBox) botonPresionado.getParent();
        HBox fila = (HBox) contenedorBoton.getParent();

        ComboBox<String> combo = null;
        TextField campoCantidad = null;

        if (fila.getChildren().size() >= 2) {
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);

            if (contenedorUbicacion.getChildren().size() > 1) {
                combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            }

            if (contenedorCantidad.getChildren().size() > 1) {
                campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);
            }
        }

        if (combo != null) {
            ubicacionesCapturadas.remove(combo);
        }

        if (campoCantidad != null) {
            ultimaCantidadUbicacionValidada.remove(campoCantidad);
        }

        if (contenedorUbicaciones != null) {
            contenedorUbicaciones.getChildren().remove(fila);
        }

        contadorFilas--;
    }

    protected void limpiarFilasAdicionales() {
        if (contenedorUbicaciones == null) return;

        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();

        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(contenedorUbicaciones.getChildren().size() - 1);
        }

        contadorFilas = 1;

        if (!contenedorUbicaciones.getChildren().isEmpty()) {
            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(0);
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            TextField campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);

            limpiarComboUbicacion(combo);
            campoCantidad.clear();
        }
    }

    protected void limpiarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setValue(null);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().clear();
        }
    }

    // === CLASES INTERNAS PARA VALIDACIONES ===

    protected enum EstadoValidacionLote {
        OK,
        ENTRADA_PENDIENTE,
        SALIDA_EN_PROCESO,
        LOTE_INVALIDO
    }

    protected static class ResultadoValidacionLote {
        private final EstadoValidacionLote estado;
        private final LocalDate caducidad;

        private ResultadoValidacionLote(EstadoValidacionLote estado, LocalDate caducidad) {
            this.estado = estado;
            this.caducidad = caducidad;
        }

        private static ResultadoValidacionLote ok(LocalDate caducidad) {
            return new ResultadoValidacionLote(EstadoValidacionLote.OK, caducidad);
        }

        private static ResultadoValidacionLote entradaPendiente() {
            return new ResultadoValidacionLote(EstadoValidacionLote.ENTRADA_PENDIENTE, null);
        }

        private static ResultadoValidacionLote salidaEnProceso() {
            return new ResultadoValidacionLote(EstadoValidacionLote.SALIDA_EN_PROCESO, null);
        }

        private static ResultadoValidacionLote loteInvalido() {
            return new ResultadoValidacionLote(EstadoValidacionLote.LOTE_INVALIDO, null);
        }
    }

    // === CLASES PARA ASIGNACIONES RÁPIDAS ===

    protected static class AsignacionRapida {
        protected final String ubicacion;
        protected final String lote;
        protected final LocalDate caducidad;
        protected final int cantidad;

        protected AsignacionRapida(String ubicacion, String lote, LocalDate caducidad, int cantidad) {
            this.ubicacion = ubicacion;
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.cantidad = cantidad;
        }
    }

    protected static class LoteCaducidadKey {
        protected final String lote;
        protected final LocalDate caducidad;

        protected LoteCaducidadKey(String lote, LocalDate caducidad) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LoteCaducidadKey other = (LoteCaducidadKey) obj;
            return java.util.Objects.equals(lote, other.lote)
                    && java.util.Objects.equals(caducidad, other.caducidad);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(lote, caducidad);
        }
    }

    // === MÉTODOS PARA CONSTRUCCIÓN DE ITEMS RÁPIDOS ===

    protected List<AsignacionRapida> construirAsignacionesRapidas(
            List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles,
            int cantidad
    ) {
        if (disponibles == null || disponibles.isEmpty()) {
            return List.of();
        }

        List<AsignacionRapida> resultado = new ArrayList<>();
        int restante = cantidad;

        for (modelNuevoTraspasoSalida.DisponibilidadRapida disp : disponibles) {
            if (restante <= 0) {
                break;
            }

            int asignar = Math.min(restante, disp.getTotal());
            if (asignar <= 0) {
                continue;
            }

            resultado.add(new AsignacionRapida(disp.getUbicacion(), disp.getLote(), disp.getCaducidad(), asignar));
            restante -= asignar;
        }

        if (restante > 0) {
            return List.of();
        }

        return resultado;
    }

    protected List<traspasoSalida> construirItemsRapidos(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones,
            String presentacion,
            int factor
    ) {
        List<traspasoSalida> resultado = new ArrayList<>();
        if (asignaciones == null || asignaciones.isEmpty()) {
            return resultado;
        }

        Map<LoteCaducidadKey, List<UbicacionCompra>> ubicacionesPorLote = new LinkedHashMap<>();
        Map<LoteCaducidadKey, Integer> cantidadesPorLote = new LinkedHashMap<>();

        for (AsignacionRapida asignacion : asignaciones) {
            if (asignacion == null || asignacion.cantidad <= 0) {
                continue;
            }

            LoteCaducidadKey key = new LoteCaducidadKey(asignacion.lote, asignacion.caducidad);
            ubicacionesPorLote.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new UbicacionCompra(asignacion.ubicacion, asignacion.cantidad));
            cantidadesPorLote.merge(key, asignacion.cantidad, Integer::sum);
        }

        for (Map.Entry<LoteCaducidadKey, List<UbicacionCompra>> entry : ubicacionesPorLote.entrySet()) {
            LoteCaducidadKey key = entry.getKey();
            int cantidadItem = cantidadesPorLote.getOrDefault(key, 0);

            if (cantidadItem <= 0) {
                continue;
            }

            String caducidad = key.caducidad != null ? key.caducidad.toString() : "";
            traspasoSalida item = new traspasoSalida(
                    clave,
                    nombre,
                    descripcion,
                    key.lote,
                    caducidad,
                    cantidadItem,
                    presentacion,
                    factor,
                    entry.getValue(),
                    "",
                    "",
                    "",
                    ""
            );
            item.setNota("");
            resultado.add(item);
        }

        return resultado;
    }

    // === MÉTODOS PARA CONFIRMACIÓN ===

    protected boolean confirmarRevisionUbicacionesRapidas() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Revisión de ubicaciones");
        confirmacion.setHeaderText("Revisa bien las ubicaciones de donde se sacan los productos.");
        confirmacion.setContentText("Presiona Aceptar para continuar o Cancelar para detener el guardado.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    protected boolean mostrarResumenUbicacionesRapidas(List<AsignacionRapida> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return false;
        }

        Map<String, Map<String, Integer>> lotesPorUbicacion = new LinkedHashMap<>();
        Map<String, Integer> totalesPorUbicacion = new LinkedHashMap<>();

        for (AsignacionRapida asignacion : asignaciones) {
            if (asignacion == null) {
                continue;
            }

            String ubicacion = asignacion.ubicacion != null ? asignacion.ubicacion : "";
            String lote = asignacion.lote != null && !asignacion.lote.isBlank() ? asignacion.lote : "Sin lote";

            lotesPorUbicacion.computeIfAbsent(ubicacion, k -> new LinkedHashMap<>())
                    .merge(lote, asignacion.cantidad, Integer::sum);
            totalesPorUbicacion.merge(ubicacion, asignacion.cantidad, Integer::sum);
        }

        VBox contenido = new VBox(10);
        contenido.setFillWidth(true);
        contenido.setAlignment(Pos.TOP_LEFT);

        for (Map.Entry<String, Map<String, Integer>> entry : lotesPorUbicacion.entrySet()) {
            String ubicacion = entry.getKey();
            int total = totalesPorUbicacion.getOrDefault(ubicacion, 0);

            HBox filaUbicacion = new HBox(10);
            filaUbicacion.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #000000;");
            filaUbicacion.setAlignment(Pos.CENTER_LEFT);

            Label ubicacionLabel = new Label("Ubicación " + ubicacion + ":");
            ubicacionLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");

            Label totalLabel = new Label(String.valueOf(total));
            totalLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");

            HBox.setHgrow(ubicacionLabel, Priority.ALWAYS);
            filaUbicacion.getChildren().addAll(ubicacionLabel, totalLabel);
            contenido.getChildren().add(filaUbicacion);

            for (Map.Entry<String, Integer> loteEntry : entry.getValue().entrySet()) {
                HBox filaLote = new HBox(10);
                filaLote.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #ffffff; "
                        + "-fx-border-color: #cccccc; -fx-border-width: 1;");
                filaLote.setAlignment(Pos.CENTER_LEFT);

                Label loteLabel = new Label("Lote " + loteEntry.getKey() + ":");
                Label cantidadLabel = new Label(String.valueOf(loteEntry.getValue()));

                HBox.setHgrow(loteLabel, Priority.ALWAYS);
                filaLote.getChildren().addAll(loteLabel, cantidadLabel);
                contenido.getChildren().add(filaLote);
            }
        }

        Alert resumenAlert = new Alert(Alert.AlertType.CONFIRMATION);
        resumenAlert.setTitle("Ubicaciones sugeridas");
        resumenAlert.setHeaderText("Primeras ubicaciones encontradas");
        resumenAlert.setContentText("Presiona Aceptar para continuar o Cancelar para volver al formulario.");
        resumenAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        resumenAlert.getDialogPane().setContent(contenido);

        Optional<ButtonType> respuesta = resumenAlert.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }
}