package Formularios.controller;

import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoSalida.controller.MainController;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline; // ✅ FIX LOTE
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class controllerNuevoTraspasoSalida extends FormularioSalidaController {

    // === CAMPOS ESPECÍFICOS PARA TRASPASO SALIDA ===
    @FXML private TextField txtPrecioSalida;
    @FXML private TextField txtPrecioEntradaIva;
    @FXML private TextField txtPrecioSalidaRapida;
    @FXML private TextField txtPrecioEntradaIvaRapida;
    @FXML private TextField txtPrecioIVARapida;
    @FXML private Label lblTitulo;
    @FXML private ScrollPane scrollPaneTraspaso;

    // === VARIABLES ESPECÍFICAS DE TRASPASO ===
    private ObservableList<traspasoSalida> itemsTraspaso;
    private MainController mainController;
    private traspasoSalida itemParaEditar;

    private String tituloFormulario = "Traspaso de Salida";
    private boolean modoSoloNormal = false;
    private boolean modoAjusteInventario = false;
    private boolean bloqueoAutoseleccionEdicion = false;
    private boolean permitirEdicionManualLote = false;

    // ✅ FIX LOTE: protección contra autolimpieza tardía (debounce/tareas/listeners del padre)
    private Timeline proteccionPrecargaLoteTimeline;
    private boolean usuarioModificoLote = false;
    private String lotePrecargado = null;
    private LocalDate caducidadPrecargada = null;

    @FXML
    public void initialize() {
        if (lblTitulo != null) lblTitulo.setText(tituloFormulario);

        initializeBase();
        aplicarModoSoloNormal();
        configurarBloqueoAutoseleccionEdicion();

        // ✅ FIX LOTE: si el usuario escribe en lote, desactivamos la "protección" para no pisar cambios manuales
        if (txtLote != null) {
            txtLote.addEventFilter(KeyEvent.KEY_TYPED, e -> usuarioModificoLote = true);
            txtLote.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                // solo marca "posible edición manual" si después teclean; aquí no hacemos nada más
            });
        }
        if (dpCaducidad != null) {
            dpCaducidad.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                // el usuario ya interactuó con caducidad; no bloqueamos, solo evita restauraciones agresivas
            });
        }

        if (itemParaEditar != null) {
            if (tabRapido != null) {
                tabRapido.setDisable(true);
                if (tabPaneModo != null && tabNormal != null) {
                    tabPaneModo.getSelectionModel().select(tabNormal);
                }
            }
            cargarItemParaEditar();
        }
    }

    // =========================
    // IMPLEMENTACIÓN ABSTRACTA
    // =========================

    @Override
    protected void guardarItem() {
        if (esModoRapido()) { guardarItemRapido(); return; }

        // Lectura de campos (menos repetición / mismas reglas)
        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();

        String descripcion = t(txtDescripcion);
        String lote = t(txtLote);
        LocalDate caducidad = dpCaducidad != null ? dpCaducidad.getValue() : null;
        String cantidadTexto = t(txtCantidad);
        String nota = txtNota != null ? t(txtNota) : "";

        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;
        String factorTexto = t(txtFactor);

        String precioEntrada = t(txtPrecioEntrada);
        String precioEntradaIva = t(txtPrecioEntradaIva);
        String precioSalida = t(txtPrecioSalida);
        String precioIva = t(txtPrecioIVA);
        String precioBruto = t(txtPrecioBruto);
        String precioTotal = t(txtPrecioTotal);

        // Validación: mismos campos requeridos
        if (isBlank(clave, nombre, descripcion, lote, cantidadTexto, presentacion, factorTexto,
                precioEntrada, precioEntradaIva, precioSalida, precioIva, precioBruto, precioTotal)) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar, excepto el comentario.");
            return;
        }

        Integer cantidad = parsePositivo(cantidadTexto, "cantidad");
        if (cantidad == null) return;

        Integer factor = parsePositivo(factorTexto, "factor");
        if (factor == null) return;

        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar las ubicaciones con cantidad.");
            return;
        }

        if (existeProductoLoteEnLista(clave, lote, itemParaEditar)) {
            mostrarAlerta("Advertencia",
                    "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int sumaUbicaciones = ubicacionesSeleccionadas.stream().mapToInt(UbicacionCompra::getCantidad).sum();
        if (sumaUbicaciones != cantidad) {
            mostrarAlerta("Advertencia", "La suma de cantidades por ubicación debe ser igual a la cantidad total.");
            return;
        }

        if (!validarCantidadPorUbicacion(clave, lote, caducidad, presentacion, factor, ubicacionesSeleccionadas)) {
            return;
        }

        // En traspaso salida, el precio de salida debe ser igual al de entrada
        BigDecimal precioEntradaDec = parseDecimal(precioEntrada);
        BigDecimal precioSalidaDec = parseDecimal(precioSalida);
        if (precioSalidaDec.compareTo(precioEntradaDec) != 0) {
            if (!confirmarAjustePrecioSalida(precioEntrada, false)) return;
            if (txtPrecioSalida != null) txtPrecioSalida.setText(precioEntrada);
            precioSalida = precioEntrada;
        }

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

        if (itemParaEditar != null) {
            itemParaEditar.setClaveProducto(clave);
            itemParaEditar.setProducto(nombre);
            itemParaEditar.setDescripcion(descripcion);
            itemParaEditar.setLote(lote);
            itemParaEditar.setCaducidad(caducidad != null ? caducidad.toString() : "");
            itemParaEditar.setCantidad(cantidad);
            itemParaEditar.setPresentacion(presentacion);
            itemParaEditar.setFactor(factor);
            itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
            itemParaEditar.setNota(nota);
            itemParaEditar.setPrecioEntrada(precioSalida);
            itemParaEditar.setPrecioIva(precioIva);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
        } else {
            traspasoSalida item = new traspasoSalida(
                    clave,
                    nombre,
                    descripcion,
                    lote,
                    caducidad != null ? caducidad.toString() : "",
                    cantidad,
                    presentacion,
                    factor,
                    ubicacionesSeleccionadas,
                    precioSalida,
                    precioIva,
                    precioBruto,
                    precioTotal
            );
            item.setNota(nota);
            itemsTraspaso.add(item);
        }

        if (mainController != null) mainController.refrescarTabla();

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            limpiarFormularioParaNuevo();
        }
    }

    private boolean validarCantidadPorUbicacion(String clave, String lote, java.time.LocalDate caducidad,
                                                String presentacion, int factor,
                                                List<UbicacionCompra> ubicacionesSeleccionadas) {
        for (UbicacionCompra ubicacion : ubicacionesSeleccionadas) {
            if (ubicacion == null || ubicacion.getUbicacion() == null || ubicacion.getUbicacion().isBlank()) {
                mostrarAlerta("Advertencia", "Debe seleccionar una ubicación válida.");
                return false;
            }

            int cantidadSolicitada = Math.max(0, ubicacion.getCantidad());
            if (cantidadSolicitada == 0) {
                continue;
            }

            int disponibleUbicacion = modelo.obtenerCantidadDisponibleDetalle(
                    clave,
                    lote,
                    caducidad,
                    presentacion,
                    factor,
                    ubicacion.getUbicacion().trim()
            );

            if (cantidadSolicitada > disponibleUbicacion) {
                mostrarAlerta(
                        "Advertencia",
                        "La cantidad solicitada para la ubicación '" + ubicacion.getUbicacion()
                                + "' excede la disponibilidad actual (" + disponibleUbicacion + ")."
                );
                return false;
            }
        }
        return true;
    }

    @Override
    protected void guardarItemRapido() {
        if (itemParaEditar != null) {
            mostrarAlerta("Advertencia", "La edición está disponible solo en el modo normal.");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = t(txtDescripcion);
        String cantidadTexto = t(txtCantidadRapida);

        // Presentación y factor
        String presentacionRapida = (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().isBlank())
                ? cbPresentacionRapida.getValue().trim()
                : "pz";

        int factorRapido = 1;
        String factorRapidoTxt = t(txtFactorRapido);
        if (!factorRapidoTxt.isBlank()) {
            Integer f = parsePositivo(factorRapidoTxt, "factor");
            if (f == null) return;
            factorRapido = f;
        } else if (!presentacionRapida.equalsIgnoreCase("pz")) {
            mostrarAlerta("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        // VALIDACIÓN 1: producto
        if (isBlank(clave, nombre)) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto.");
            return;
        }

        // VALIDACIÓN 2: presentación
        if (presentacionRapida.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar una presentación.");
            return;
        }

        // VALIDACIÓN 3: cantidad
        if (cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar la cantidad.");
            return;
        }

        // VALIDACIÓN 4: precios completos
        String precioSalida = t(txtPrecioSalidaRapida);
        String precioIva = t(txtPrecioIVARapida);
        String precioBruto = t(txtPrecioBrutoRapida);
        String precioTotal = t(txtPrecioTotalRapida);

        if (isBlank(descripcion, precioSalida, precioIva, precioBruto, precioTotal)) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
            return;
        }

        // VALIDACIÓN 5: cantidad numérica
        Integer cantidad = parsePositivo(cantidadTexto, "cantidad");
        if (cantidad == null) return;

        // VALIDACIÓN 6: precio salida == precio entrada (auto-ajuste)
        String precioEntradaTexto = t(txtPrecioEntradaRapida);
        if (!precioEntradaTexto.isBlank() && !precioSalida.isBlank()) {
            BigDecimal precioEntradaDec = parseDecimal(precioEntradaTexto);
            BigDecimal precioSalidaDec = parseDecimal(precioSalida);
            if (precioSalidaDec.compareTo(precioEntradaDec) != 0) {
                if (!confirmarAjustePrecioSalida(precioEntradaTexto, true)) return;
                if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.setText(precioEntradaTexto);
                precioSalida = precioEntradaTexto;
            }
        }

        // VALIDACIÓN 7: disponibilidad (se conserva)
        int disponible = modelo.obtenerCantidadDisponibleProductoPresentacionFactor(clave, presentacionRapida, factorRapido);
        if (cantidad > disponible) {
            mostrarAlerta("Advertencia",
                    "La cantidad supera la disponible para la presentación " + presentacionRapida
                            + " con factor " + factorRapido + ".");
            return;
        }

        // VALIDACIÓN 8: verificar combinación y disponibilidad en background (sin repetir consulta de disponibilidad)
        final String claveSnapshot = clave;
        final String nombreSnapshot = nombre;
        final String descripcionSnapshot = descripcion;
        final String presentacionSnapshot = presentacionRapida;
        final int factorSnapshot = factorRapido;
        final int cantidadSnapshot = cantidad;
        final int disponibleSnapshot = disponible;

        final String precioSalidaSnapshot = precioSalida;
        final String precioIvaSnapshot = precioIva;
        final String precioBrutoSnapshot = precioBruto;
        final String precioTotalSnapshot = precioTotal;

        Task<Boolean> validacionTask = new Task<>() {
            @Override
            protected Boolean call() {
                boolean combinacionExiste = modelo.existeCombinacionProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot);
                if (!combinacionExiste) return false;

                // Reusa la disponibilidad ya obtenida (evita un hit extra a BD)
                return disponibleSnapshot >= cantidadSnapshot;
            }

            @Override
            protected void succeeded() {
                if (!Boolean.TRUE.equals(getValue())) {
                    Platform.runLater(() ->
                            mostrarAlerta("Error",
                                    "La combinación de producto, presentación y factor no existe en inventario " +
                                            "o no hay suficiente cantidad disponible."));
                    return;
                }

                Platform.runLater(() -> continuarGuardadoRapido(
                        claveSnapshot, nombreSnapshot, descripcionSnapshot,
                        presentacionSnapshot, factorSnapshot, cantidadSnapshot,
                        precioSalidaSnapshot, precioIvaSnapshot, precioBrutoSnapshot, precioTotalSnapshot
                ));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> mostrarAlerta("Error", "Error al validar la disponibilidad del producto."));
            }
        };

        Thread hiloValidacion = new Thread(validacionTask);
        hiloValidacion.setDaemon(true);
        hiloValidacion.start();
    }

    @Override
    protected boolean existeProductoLoteEnLista(String clave, String lote, traspasoSalida itemExcluir) {
        if (isBlank(clave, lote)) return false;

        if (mainController != null && mainController.existeProductoLote(clave, lote, itemExcluir)) return true;
        if (itemsTraspaso == null) return false;

        String c = clave.trim();
        String l = lote.trim();

        return itemsTraspaso.stream().anyMatch(item ->
                item != null && item != itemExcluir &&
                        c.equals(item.getClaveProducto()) &&
                        l.equals(item.getLote()));
    }

    @Override
    protected List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new ArrayList<>();
        if (contenedorUbicaciones == null) return resultado;

        for (javafx.scene.Node n : contenedorUbicaciones.getChildren()) {
            if (!(n instanceof HBox fila)) continue;
            if (fila.getChildren().size() < 2) continue;

            ComboBox<String> combo = extraerComboUbicacion(fila);
            TextField campoCantidad = extraerCampoCantidadUbicacion(fila);
            if (combo == null || campoCantidad == null) continue;

            String ubicacion = combo.getValue();
            String cantidadTxt = t(campoCantidad);
            if (isBlank(ubicacion, cantidadTxt)) continue;

            Integer cant = parsePositivo(cantidadTxt, null); // aquí solo filtra inválidos (sin alertas)
            if (cant == null) continue;

            resultado.add(new UbicacionCompra(ubicacion.trim(), cant));
        }
        return resultado;
    }

    @Override
    protected boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        if (ubicacionesSeleccionadas == null) return false;

        HashSet<String> set = new HashSet<>();
        for (UbicacionCompra u : ubicacionesSeleccionadas) {
            if (u == null || u.getUbicacion() == null) continue;
            String key = u.getUbicacion().trim();
            if (key.isEmpty()) continue;
            if (!set.add(key)) return true;
        }
        return false;
    }

    @Override
    protected void limpiarFormularioParaNuevo() {
        limpiarValidacionesInventario();
        seleccionarClaveAlternaPendiente = false;

        if (productoController != null) productoController.limpiarSeleccion();

        limpiarFormularioDependiente();

        if (txtPrecioSalida != null) txtPrecioSalida.clear();

        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        limpiarFilasAdicionales();

        Platform.runLater(() -> { if (cbClaveProducto != null) cbClaveProducto.requestFocus(); });
    }

    @Override
    protected void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    @Override
    protected void mostrarAlertaSinEspera(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.show();

            PauseTransition pt = new PauseTransition(Duration.millis(2000));
            pt.setOnFinished(e -> { if (alert.isShowing()) alert.close(); });
            pt.play();
        });
    }

    @Override
    protected void recalcularPrecios() {
        int cantidad = parseEntero(txtCantidad != null ? txtCantidad.getText() : "");
        if (cantidad <= 0) {
            if (txtPrecioBruto != null) txtPrecioBruto.clear();
            if (txtPrecioTotal != null) txtPrecioTotal.clear();
            if (txtPrecioIVA != null) txtPrecioIVA.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalida != null ? txtPrecioSalida.getText() : "");
        BigDecimal precioConIva = precioSalida;

        // En traspaso no se aplica/quita IVA - viene de entrada con IVA
        if (txtPrecioEntradaIva != null && txtPrecioEntradaIva.getText() != null && !txtPrecioEntradaIva.getText().isBlank()) {
            precioConIva = parseDecimal(txtPrecioEntradaIva.getText());
        }

        BigDecimal cant = BigDecimal.valueOf(cantidad);
        BigDecimal bruto = precioSalida.multiply(cant);
        BigDecimal total = precioConIva.multiply(cant);

        if (txtPrecioIVA != null) txtPrecioIVA.setText(formatearDecimal(precioConIva));
        if (txtPrecioBruto != null) txtPrecioBruto.setText(formatearDecimal(bruto));
        if (txtPrecioTotal != null) txtPrecioTotal.setText(formatearDecimal(total));
    }

    @Override
    protected void recalcularPreciosRapido() {
        if (txtCantidadRapida == null || txtPrecioSalidaRapida == null) return;

        int cantidad = parseEntero(txtCantidadRapida.getText());
        if (cantidad <= 0) {
            if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.clear();
            if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.clear();
            if (txtPrecioIVARapida != null) txtPrecioIVARapida.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalidaRapida.getText());
        BigDecimal precioConIva = precioSalida;

        if (txtPrecioEntradaIvaRapida != null && txtPrecioEntradaIvaRapida.getText() != null
                && !txtPrecioEntradaIvaRapida.getText().isBlank()) {
            precioConIva = parseDecimal(txtPrecioEntradaIvaRapida.getText());
        }

        BigDecimal cant = BigDecimal.valueOf(cantidad);
        BigDecimal bruto = precioSalida.multiply(cant);
        BigDecimal total = precioConIva.multiply(cant);

        if (txtPrecioIVARapida != null) txtPrecioIVARapida.setText(formatearDecimal(precioConIva));
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.setText(formatearDecimal(bruto));
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.setText(formatearDecimal(total));
    }

    @Override
    protected void limpiarPrecios() {
        if (txtPrecioEntrada != null) txtPrecioEntrada.clear();
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.clear();
        if (txtPrecioSalida != null) txtPrecioSalida.clear();
        if (txtPrecioIVA != null) txtPrecioIVA.clear();
        if (txtPrecioBruto != null) txtPrecioBruto.clear();
        if (txtPrecioTotal != null) txtPrecioTotal.clear();

        precioEntradaBase = BigDecimal.ZERO;
        precioIvaBase = BigDecimal.ZERO;
    }

    @Override
    protected void limpiarPreciosRapidos() {
        if (txtPrecioEntradaRapida != null) txtPrecioEntradaRapida.clear();
        if (txtPrecioEntradaIvaRapida != null) txtPrecioEntradaIvaRapida.clear();
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.clear();
        if (txtPrecioIVARapida != null) txtPrecioIVARapida.clear();
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.clear();
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.clear();
    }

    @Override
    protected void cargarPreciosDesdeProducto() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank() || !datosCompletosParaPrecioEntrada()) {
            limpiarPrecios();
            return;
        }

        boolean precioSalidaVacio = txtPrecioSalida == null || txtPrecioSalida.getText() == null || txtPrecioSalida.getText().isBlank();
        if (!precioSalidaVacio) return;

        String lote = t(txtLote);
        String presentacion = cbPresentacion != null ? cbPresentacion.getValue() : null;

        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoPorLotePresentacion(idProducto, lote, presentacion);
            }

            @Override
            protected void succeeded() {
                Optional<modelNuevoTraspasoSalida.PreciosProducto> r = getValue();
                r.ifPresent(controllerNuevoTraspasoSalida.this::aplicarPrecioEntrada);
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    protected void cargarPreciosRapidosDesdeUltimaEntrada() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPreciosRapidos();
            return;
        }

        final String idSnapshot = idProducto;
        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoUltimaEntrada(idSnapshot);
            }

            @Override
            protected void succeeded() {
                String idActual = productoController.getIdSeleccionado();
                if (!idSnapshot.equals(idActual)) return;

                Optional<modelNuevoTraspasoSalida.PreciosProducto> r = getValue();
                if (r.isPresent()) aplicarPreciosRapidos(r.get());
                else limpiarPreciosRapidos();
            }

            @Override
            protected void failed() {
                limpiarPreciosRapidos();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    protected void configurarCamposLecturaEspecificos() {
        if (txtPrecioEntrada != null) txtPrecioEntrada.setEditable(false);
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.setEditable(false);
        if (txtPrecioEntradaRapida != null) txtPrecioEntradaRapida.setEditable(false);
        if (txtPrecioEntradaIvaRapida != null) txtPrecioEntradaIvaRapida.setEditable(false);
        if (txtPrecioIVA != null) txtPrecioIVA.setEditable(false);
        if (txtPrecioBruto != null) txtPrecioBruto.setEditable(false);
        if (txtPrecioTotal != null) txtPrecioTotal.setEditable(false);
        if (txtPrecioIVARapida != null) txtPrecioIVARapida.setEditable(false);
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.setEditable(false);
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.setEditable(false);
        if (txtPrecioSalida != null) txtPrecioSalida.setEditable(false);
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.setEditable(false);

        aplicarModoAjusteInventario();
    }

    @Override
    protected void configurarCalculoPreciosEspecifico() {
        if (txtPrecioSalida != null) txtPrecioSalida.textProperty().addListener((obs, o, n) -> recalcularPrecios());
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.textProperty().addListener((obs, o, n) -> recalcularPreciosRapido());
    }

    // =========================
    // MÉTODOS ESPECÍFICOS
    // =========================

    private void aplicarModoSoloNormal() {
        if (!modoSoloNormal || tabPaneModo == null) return;

        if (tabRapido != null) tabPaneModo.getTabs().remove(tabRapido);
        if (tabNormal != null) tabPaneModo.getSelectionModel().select(tabNormal);

        tabPaneModo.getStyleClass().add("modo-tabs-sin-header");
    }

    private void aplicarModoAjusteInventario() {
        if (!modoAjusteInventario) return;
        if (txtPrecioSalida != null) txtPrecioSalida.setEditable(false);
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.setEditable(false);
    }

    private boolean datosCompletosParaPrecioEntrada() {
        String id = productoController.getIdSeleccionado();
        return id != null && !id.isBlank()
                && loteValidado
                && cbPresentacion != null
                && cbPresentacion.getValue() != null
                && !cbPresentacion.getValue().isBlank();
    }

    private void aplicarPrecioEntrada(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) { limpiarPrecios(); return; }

        precioEntradaBase = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        precioIvaBase = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        if (txtPrecioEntrada != null) txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.setText(formatearDecimal(precioIvaBase));

        if (txtPrecioSalida != null && (txtPrecioSalida.getText() == null || txtPrecioSalida.getText().isBlank())) {
            txtPrecioSalida.setText(formatearDecimal(precioEntradaBase));
        }

        recalcularPrecios();
    }

    private void aplicarPreciosRapidos(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) { limpiarPreciosRapidos(); return; }

        BigDecimal pe = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        BigDecimal pi = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        if (txtPrecioEntradaRapida != null) txtPrecioEntradaRapida.setText(formatearDecimal(pe));
        if (txtPrecioEntradaIvaRapida != null) txtPrecioEntradaIvaRapida.setText(formatearDecimal(pi));
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.setText(formatearDecimal(pe));

        recalcularPreciosRapido();
    }

    private void continuarGuardadoRapido(String clave, String nombre, String descripcion,
                                         String presentacionRapida, int factorRapido, int cantidad,
                                         String precioSalida, String precioIva,
                                         String precioBruto, String precioTotal) {

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

        List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles =
                modelo.obtenerDisponibilidadesRapidas(clave, presentacionRapida, factorRapido);

        if (disponibles == null || disponibles.isEmpty()) {
            mostrarAlerta("Error", "No hay disponibilidad para el producto con las características especificadas.");
            return;
        }

        List<AsignacionRapida> asignaciones = construirAsignacionesRapidas(disponibles, cantidad);
        if (asignaciones.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        if (!confirmarRevisionUbicacionesRapidas()) return;
        if (!mostrarResumenUbicacionesRapidas(asignaciones)) return;

        List<traspasoSalida> itemsGenerados = construirItemsRapidosTraspaso(
                clave, nombre, descripcion, asignaciones, presentacionRapida, factorRapido);

        if (itemsGenerados.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        BigDecimal precioSalidaDec = parseDecimal(precioSalida);
        BigDecimal precioIvaDec = parseDecimal(precioIva);

        for (traspasoSalida item : itemsGenerados) {
            if (existeProductoLoteEnLista(clave, item.getLote(), null)) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
                return;
            }

            int cantidadItem = item.getCantidad();
            BigDecimal cant = BigDecimal.valueOf(cantidadItem);

            BigDecimal brutoItem = precioSalidaDec.multiply(cant);
            BigDecimal totalItem = precioIvaDec.multiply(cant);

            item.setPrecioEntrada(formatearDecimal(precioSalidaDec));
            item.setPrecioIva(formatearDecimal(precioIvaDec));
            item.setPrecioBruto(formatearDecimal(brutoItem));
            item.setPrecioTotal(formatearDecimal(totalItem));

            itemsTraspaso.add(item);
        }

        if (mainController != null) mainController.refrescarTabla();
        limpiarFormularioParaNuevo();
    }

    private List<traspasoSalida> construirItemsRapidosTraspaso(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones,
            String presentacion,
            int factor
    ) {
        return construirItemsRapidos(clave, nombre, descripcion, asignaciones, presentacion, factor);
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) return;

        // ✅ FIX LOTE: resetea estado de edición manual y toma snapshot de los valores que NO deben borrarse solos
        usuarioModificoLote = false;
        lotePrecargado = itemParaEditar.getLote() == null ? "" : itemParaEditar.getLote();
        caducidadPrecargada = null;
        String cadTxt = itemParaEditar.getCaducidad();
        if (cadTxt != null && !cadTxt.isBlank()) {
            try { caducidadPrecargada = LocalDate.parse(cadTxt); } catch (Exception ignored) { caducidadPrecargada = null; }
        }

        // ✅ FIX LOTE: detener cualquier protección anterior (por si reusan el mismo controller)
        detenerProteccionPrecargaLote();

        bloqueoAutoseleccionEdicion = true;
        permitirEdicionManualLote = false;

        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        txtDescripcion.setText(itemParaEditar.getDescripcion());

        if (txtNota != null) txtNota.setText(itemParaEditar.getNota());

        txtLote.setText(itemParaEditar.getLote());
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        loteValidado = true;
        caducidadValidada = true;

        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));
        presentacionValida = true;
        factorValido = true;

        if (txtPrecioEntrada != null) txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.setText(itemParaEditar.getPrecioIva());

        String precioSalida = itemParaEditar.getPrecioEntrada();
        if (txtPrecioSalida != null) txtPrecioSalida.setText(precioSalida);

        if (txtPrecioIVA != null) txtPrecioIVA.setText(itemParaEditar.getPrecioIva());
        if (txtPrecioBruto != null) txtPrecioBruto.setText(itemParaEditar.getPrecioBruto());
        if (txtPrecioTotal != null) txtPrecioTotal.setText(itemParaEditar.getPrecioTotal());

        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();

        // ✅ FIX LOTE: activa protección unos segundos para evitar que se borre solo al abrir (por listeners/tareas tardías)
        iniciarProteccionPrecargaLote();
    }

    // ✅ FIX LOTE
    private void iniciarProteccionPrecargaLote() {
        if (itemParaEditar == null) return;
        if (txtLote == null) return;

        final String esperadoLote = lotePrecargado == null ? "" : lotePrecargado;
        final LocalDate esperadaCad = caducidadPrecargada;

        // Checamos varias veces (porque el “borrado” suele venir de un debounce a 300ms-1500ms)
        List<Duration> checks = List.of(
                Duration.millis(150),
                Duration.millis(350),
                Duration.millis(700),
                Duration.millis(1200),
                Duration.millis(2000),
                Duration.millis(3200)
        );

        List<KeyFrame> frames = new ArrayList<>();
        for (Duration d : checks) {
            frames.add(new KeyFrame(d, e -> {
                if (itemParaEditar == null) return;
                if (usuarioModificoLote) return; // si el usuario ya tocó el lote, no restauramos nada

                // Si por algún proceso tardío lo limpian o cambian, lo devolvemos al precargado
                String actual = t(txtLote);
                if (!esperadoLote.isBlank() && actual.isBlank()) {
                    txtLote.setText(esperadoLote);
                }

                // Caducidad (por si también te la están limpiando)
                if (dpCaducidad != null && esperadaCad != null && dpCaducidad.getValue() == null) {
                    dpCaducidad.setValue(esperadaCad);
                }
            }));
        }

        proteccionPrecargaLoteTimeline = new Timeline();
        proteccionPrecargaLoteTimeline.getKeyFrames().addAll(frames);
        proteccionPrecargaLoteTimeline.setCycleCount(1);
        proteccionPrecargaLoteTimeline.playFromStart();
    }

    // ✅ FIX LOTE
    private void detenerProteccionPrecargaLote() {
        if (proteccionPrecargaLoteTimeline != null) {
            try { proteccionPrecargaLoteTimeline.stop(); } catch (Exception ignored) {}
            proteccionPrecargaLoteTimeline = null;
        }
    }

    private void configurarBloqueoAutoseleccionEdicion() {
        if (cbClaveProducto != null) {
            cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> protegerValorEnEdicion(cbClaveProducto));
            cbClaveProducto.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            cbClaveProducto.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }
        if (cbProductoNombre != null) {
            cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> protegerValorEnEdicion(cbProductoNombre));
            cbProductoNombre.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            cbProductoNombre.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }
        if (cbPresentacion != null) {
            cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> protegerValorEnEdicion(cbPresentacion));
            cbPresentacion.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            cbPresentacion.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }
        if (txtFactor != null) {
            txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
                if (bloqueoAutoseleccionEdicion && itemParaEditar != null && !String.valueOf(itemParaEditar.getFactor()).equals(newVal)) {
                    txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));
                }
            });
            txtFactor.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            txtFactor.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }

        if (txtDescripcion != null) {
            txtDescripcion.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!bloqueoAutoseleccionEdicion || itemParaEditar == null) return;

                String esperada = itemParaEditar.getDescripcion() == null ? "" : itemParaEditar.getDescripcion();
                String actual = newVal == null ? "" : newVal;
                if (!esperada.equals(actual)) {
                    txtDescripcion.setText(esperada);
                }
            });
            txtDescripcion.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            txtDescripcion.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }

        if (txtLote != null) {
            txtLote.textProperty().addListener((obs, oldVal, newVal) -> {
                if (itemParaEditar == null || permitirEdicionManualLote) return;

                String esperado = itemParaEditar.getLote() == null ? "" : itemParaEditar.getLote();
                String actual = newVal == null ? "" : newVal;

                // ✅ FIX LOTE: si algo lo intenta cambiar (incluido dejarlo en blanco), lo devolvemos
                if (!esperado.equals(actual)) {
                    txtLote.setText(esperado);
                }
            });
            txtLote.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                bloqueoAutoseleccionEdicion = false;
                permitirEdicionManualLote = true;
                usuarioModificoLote = true; // ✅ FIX LOTE: en cuanto el usuario teclea, ya no restauramos por protección
                detenerProteccionPrecargaLote();
            });
        }

        if (dpCaducidad != null) {
            dpCaducidad.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!bloqueoAutoseleccionEdicion || itemParaEditar == null) return;

                LocalDate esperada = null;
                String cad = itemParaEditar.getCaducidad();
                if (cad != null && !cad.isBlank()) {
                    try {
                        esperada = LocalDate.parse(cad);
                    } catch (java.time.format.DateTimeParseException ignored) {
                        esperada = null;
                    }
                }

                if ((esperada == null && newVal != null) || (esperada != null && !esperada.equals(newVal))) {
                    dpCaducidad.setValue(esperada);
                }
            });
            dpCaducidad.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
            dpCaducidad.addEventFilter(KeyEvent.KEY_PRESSED, e -> bloqueoAutoseleccionEdicion = false);
        }
    }

    private void protegerValorEnEdicion(ComboBox<String> combo) {
        if (!bloqueoAutoseleccionEdicion || itemParaEditar == null || combo == null) return;

        String esperado;
        if (combo == cbClaveProducto) esperado = itemParaEditar.getClaveProducto();
        else if (combo == cbProductoNombre) esperado = itemParaEditar.getProducto();
        else if (combo == cbPresentacion) esperado = itemParaEditar.getPresentacion();
        else return;

        String actual = combo.getValue();
        if (esperado != null && !esperado.equals(actual)) {
            combo.setValue(esperado);
            if (combo.getEditor() != null) combo.getEditor().setText(esperado);
        }
    }

    private void configurarCaducidadDesdeTexto(String caducidadTexto) {
        if (dpCaducidad == null) return;

        if (caducidadTexto == null || caducidadTexto.isBlank()) {
            dpCaducidad.setValue(null);
            return;
        }

        try {
            dpCaducidad.setValue(LocalDate.parse(caducidadTexto));
        } catch (java.time.format.DateTimeParseException e) {
            dpCaducidad.setValue(null);
        }
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        limpiarFilasAdicionales();
        ubicacionesCapturadas.clear();
        ultimaCantidadUbicacionValidada.clear();

        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) return;

        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra u = ubicacionesLista.get(i);
            if (i > 0) agregarUbicacionCombo();

            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            ComboBox<String> combo = extraerComboUbicacion(fila);
            TextField cantidad = extraerCampoCantidadUbicacion(fila);

            if (combo != null) {
                combo.setValue(u.getUbicacion());
                if (combo.getEditor() != null) combo.getEditor().setText(u.getUbicacion());
            }
            if (cantidad != null) cantidad.setText(String.valueOf(u.getCantidad()));
        }
    }

    private void cerrarFormulario() {
        // ✅ FIX LOTE: detener timeline para evitar fugas si cierras rápido
        detenerProteccionPrecargaLote();

        if (btnGuardar == null || btnGuardar.getScene() == null) return;
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        if (stage != null) stage.close();
    }

    // =========================
    // Helpers internos (sin cambiar validaciones)
    // =========================

    private static String t(TextInputControl c) {
        return (c == null || c.getText() == null) ? "" : c.getText().trim();
    }

    private static boolean isBlank(String... vals) {
        if (vals == null) return true;
        for (String v : vals) if (v == null || v.isBlank()) return true;
        return false;
    }

    private Integer parsePositivo(String texto, String campo) {
        try {
            int v = Integer.parseInt(texto.trim());
            if (v <= 0) {
                if ("cantidad".equalsIgnoreCase(campo)) mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                else if ("factor".equalsIgnoreCase(campo)) mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            if ("cantidad".equalsIgnoreCase(campo)) mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            else if ("factor".equalsIgnoreCase(campo)) mostrarAlerta("Error", "El factor debe ser un número válido.");
            return null;
        }
    }

    private boolean confirmarAjustePrecioSalida(String precioEntradaTexto, boolean modoRapido) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(modoRapido ? "Advertencia de precio" : "Advertencia");
        confirmacion.setHeaderText("El precio de salida no coincide con el precio de entrada.");
        confirmacion.setContentText("En traspaso de salida, el precio debe ser el mismo. ¿Deseas ajustarlo automáticamente?");
        confirmacion.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = confirmacion.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> extraerComboUbicacion(HBox fila) {
        try {
            VBox contU = (VBox) fila.getChildren().get(0);
            if (contU == null || contU.getChildren().size() < 2) return null;
            javafx.scene.Node n = contU.getChildren().get(1);
            return (n instanceof ComboBox) ? (ComboBox<String>) n : null;
        } catch (Exception e) {
            return null;
        }
    }

    private TextField extraerCampoCantidadUbicacion(HBox fila) {
        try {
            VBox contC = (VBox) fila.getChildren().get(1);
            if (contC == null || contC.getChildren().size() < 2) return null;
            javafx.scene.Node n = contC.getChildren().get(1);
            return (n instanceof TextField) ? (TextField) n : null;
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // GETTERS Y SETTERS
    // =========================

    public void setItemsTraspaso(ObservableList<traspasoSalida> itemsTraspaso) {
        this.itemsTraspaso = itemsTraspaso;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setItemParaEditar(traspasoSalida item) {
        this.itemParaEditar = item;
        if (itemParaEditar != null && inicializado) cargarItemParaEditar();
    }

    public void setTituloFormulario(String tituloFormulario) {
        if (tituloFormulario == null || tituloFormulario.isBlank()) return;
        this.tituloFormulario = tituloFormulario;
        if (lblTitulo != null) lblTitulo.setText(tituloFormulario);
    }

    public void setModoSoloNormal(boolean modoSoloNormal) {
        this.modoSoloNormal = modoSoloNormal;
        aplicarModoSoloNormal();
    }

    public void setModoAjusteInventario(boolean modoAjusteInventario) {
        this.modoAjusteInventario = modoAjusteInventario;
        if (inicializado) aplicarModoAjusteInventario();
    }
}