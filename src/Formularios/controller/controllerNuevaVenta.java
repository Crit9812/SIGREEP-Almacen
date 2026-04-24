package Formularios.controller;

import Compartido.model.IvaConfigService;
import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.venta.controller.MainController;
import Operaciones.traspasoSalida.model.traspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class controllerNuevaVenta extends FormularioSalidaController {

    // === CAMPOS ESPECÍFICOS DE VENTA ===
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioEntradaIva;
    @FXML private TextField txtPrecioSalida;
    @FXML private CheckBox checkBoxIVARapida;
    @FXML private TextField txtPrecioSalidaRapida;
    @FXML private TextField txtPrecioEntradaIvaRapida;
    @FXML private TextField txtPrecioIVARapida;
    @FXML private Label lblTitulo;
    @FXML private ScrollPane scrollPaneVenta;

    // === VARIABLES ESPECÍFICAS DE VENTA ===
    private ObservableList<traspasoSalida> itemsVenta;
    private MainController mainController;
    private traspasoSalida itemParaEditar;

    private String tituloFormulario = "Venta";
    private boolean modoSoloNormal = false;
    private boolean modoAjusteInventario = false;
    private boolean cargandoEdicion = false;
    private boolean bloqueoAutoseleccionEdicion = false;
    private boolean permitirEdicionManualLote = false;


    @FXML
    public void initialize() {
        // Configurar título
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }

        // Inicialización base
        initializeBase();

        // Configuración específica de venta
        aplicarModoSoloNormal();
        configurarBloqueoAutoseleccionEdicion();

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

    // === IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ===

    @Override
    protected void guardarItem() {
        if (esModoRapido()) {
            guardarItemRapido();
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        String nota = txtNota != null && txtNota.getText() != null ? txtNota.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        String precioEntrada = txtPrecioEntrada != null ? txtPrecioEntrada.getText().trim() : "";
        String precioEntradaIva = txtPrecioEntradaIva != null ? txtPrecioEntradaIva.getText().trim() : "";
        String precioSalida = txtPrecioSalida != null ? txtPrecioSalida.getText().trim() : "";
        String precioIva = txtPrecioIVA != null ? txtPrecioIVA.getText().trim() : "";
        String precioBruto = txtPrecioBruto != null ? txtPrecioBruto.getText().trim() : "";
        String precioTotal = txtPrecioTotal != null ? txtPrecioTotal.getText().trim() : "";

        if (clave == null || clave.isBlank()
                || nombre == null || nombre.isBlank()
                || descripcion.isBlank()
                || lote.isBlank()
                || cantidadTexto.isBlank()
                || presentacion == null || presentacion.isBlank()
                || factorTexto.isBlank()
                || precioEntrada.isBlank()
                || precioEntradaIva.isBlank()
                || precioSalida.isBlank()
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar, excepto el comentario.");
            return;
        }

        int cantidad;
        int factor;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El factor debe ser un número válido.");
            return;
        }

        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar las ubicaciones con cantidad.");
            return;
        }

        if (existeProductoLoteEnLista(clave, lote, itemParaEditar)) {
            mostrarAlerta("Advertencia",
                    "Ya se agregó este producto con el mismo lote. Finaliza la venta para poder repetirlo.");
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int sumaUbicaciones = ubicacionesSeleccionadas.stream()
                .mapToInt(UbicacionCompra::getCantidad)
                .sum();
        if (sumaUbicaciones != cantidad) {
            mostrarAlerta("Advertencia", "La suma de cantidades por ubicación debe ser igual a la cantidad total.");
            return;
        }

        if (!validarCantidadPorUbicacion(clave, lote, caducidad, presentacion, factor, ubicacionesSeleccionadas)) {
            return;
        }

        BigDecimal precioEntradaDecimal = parseDecimal(precioEntrada);
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        if (precioSalidaDecimal.compareTo(precioEntradaDecimal) < 0) {
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Advertencia");
            confirmacion.setHeaderText("El precio de salida es menor al precio de entrada.");
            confirmacion.setContentText("Esto representa una pérdida de dinero. ¿Deseas continuar?");
            Optional<ButtonType> respuesta = confirmacion.showAndWait();
            if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
                return;
            }
        }

        if (itemsVenta == null) {
            mostrarAlerta("Error", "No se pudo registrar la venta en la tabla.");
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
            itemsVenta.add(item);
        }

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            mostrarAlertaSinEspera("Éxito", "Producto agregado a la venta.");
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
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String cantidadTexto = txtCantidadRapida != null && txtCantidadRapida.getText() != null
                ? txtCantidadRapida.getText().trim()
                : "";

        // Obtener presentación y factor del modo rápido
        String presentacionRapida = "pz"; // Valor por defecto
        int factorRapido = 1; // Valor por defecto

        if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().isBlank()) {
            presentacionRapida = cbPresentacionRapida.getValue().trim();
        }

        if (txtFactorRapido != null && txtFactorRapido.getText() != null && !txtFactorRapido.getText().isBlank()) {
            try {
                factorRapido = Integer.parseInt(txtFactorRapido.getText().trim());
                if (factorRapido <= 0) {
                    mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "El factor debe ser un número válido.");
                return;
            }
        } else if (!presentacionRapida.equalsIgnoreCase("pz")) {
            // Si no es "pz" y no tiene factor, mostrar error
            mostrarAlerta("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        // VALIDACIÓN 1: Verificar que el producto esté seleccionado
        if (clave == null || clave.isBlank() || nombre == null || nombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto.");
            return;
        }

        // VALIDACIÓN 2: Verificar que la presentación esté seleccionada
        if (presentacionRapida.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar una presentación.");
            return;
        }

        // VALIDACIÓN 3: Verificar que la cantidad esté capturada
        if (cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar la cantidad.");
            return;
        }

        // VALIDACIÓN 4: Verificar que los precios estén completos
        String precioSalida = txtPrecioSalidaRapida != null && txtPrecioSalidaRapida.getText() != null
                ? txtPrecioSalidaRapida.getText().trim()
                : "";
        String precioIva = txtPrecioIVARapida != null && txtPrecioIVARapida.getText() != null
                ? txtPrecioIVARapida.getText().trim()
                : "";
        String precioBruto = txtPrecioBrutoRapida != null && txtPrecioBrutoRapida.getText() != null
                ? txtPrecioBrutoRapida.getText().trim()
                : "";
        String precioTotal = txtPrecioTotalRapida != null && txtPrecioTotalRapida.getText() != null
                ? txtPrecioTotalRapida.getText().trim()
                : "";

        if (descripcion.isBlank() || precioSalida.isBlank() || precioIva.isBlank()
                || precioBruto.isBlank() || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
            return;
        }

        // VALIDACIÓN 5: Validar cantidad numérica
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        // VALIDACIÓN 6: Confirmar pérdida si aplica
        if (!confirmarPerdidaRapidaSiAplica(cantidad)) {
            return;
        }

        // VALIDACIÓN 7: Verificar disponibilidad del producto con presentación y factor específicos
        int disponible = modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                clave, presentacionRapida, factorRapido);
        if (cantidad > disponible) {
            mostrarAlerta("Advertencia",
                    "La cantidad supera la disponible para la presentación " + presentacionRapida
                            + " con factor " + factorRapido + ".");
            return;
        }

        // VALIDACIÓN 8: Verificar que la combinación producto-presentación-factor exista en inventario
        String claveSnapshot = clave;
        String presentacionSnapshot = presentacionRapida;
        int factorSnapshot = factorRapido;
        int cantidadSnapshot = cantidad;

        Task<Boolean> validacionTask = new Task<>() {
            @Override
            protected Boolean call() {
                // Primero verificar si la combinación existe
                boolean combinacionExiste = modelo.existeCombinacionProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot);

                if (!combinacionExiste) {
                    return false;
                }

                // Luego verificar disponibilidad específica
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot) >= cantidadSnapshot;
            }

            @Override
            protected void succeeded() {
                boolean validacionExitosa = getValue();
                if (!validacionExitosa) {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error",
                                "La combinación de producto, presentación y factor no existe en inventario " +
                                        "o no hay suficiente cantidad disponible.");
                    });
                } else {
                    // Si la validación es exitosa, continuar con el resto del proceso
                    Platform.runLater(() -> continuarGuardadoRapido(claveSnapshot, nombre, descripcion,
                            presentacionSnapshot, factorSnapshot, cantidadSnapshot,
                            precioSalida, precioIva, precioBruto, precioTotal));
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    mostrarAlerta("Error", "Error al validar la disponibilidad del producto.");
                });
            }
        };

        // Ejecutar la validación en segundo plano
        Thread hiloValidacion = new Thread(validacionTask);
        hiloValidacion.setDaemon(true);
        hiloValidacion.start();
    }

    @Override
    protected boolean existeProductoLoteEnLista(String clave, String lote, traspasoSalida itemExcluir) {
        if (clave == null || clave.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }

        if (mainController != null && mainController.existeProductoLote(clave, lote, itemExcluir)) {
            return true;
        }

        if (itemsVenta == null) {
            return false;
        }

        String claveNormalizada = clave.trim();
        String loteNormalizado = lote.trim();

        return itemsVenta.stream()
                .anyMatch(item -> item != null
                        && item != itemExcluir
                        && claveNormalizada.equals(item.getClaveProducto())
                        && loteNormalizado.equals(item.getLote()));
    }

    @Override
    protected List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new java.util.ArrayList<>();

        if (contenedorUbicaciones == null) {
            return resultado;
        }

        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) {
                continue;
            }

            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 2) {
                continue;
            }

            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<String> combo = null;
            TextField campoCantidad = null;

            if (contenedorUbicacion != null && !contenedorUbicacion.getChildren().isEmpty()) {
                javafx.scene.Node nodoCombo = contenedorUbicacion.getChildren().get(1);
                if (nodoCombo instanceof ComboBox) {
                    combo = (ComboBox<String>) nodoCombo;
                }
            }

            if (contenedorCantidad != null && !contenedorCantidad.getChildren().isEmpty()) {
                javafx.scene.Node nodoCantidad = contenedorCantidad.getChildren().get(1);
                if (nodoCantidad instanceof TextField) {
                    campoCantidad = (TextField) nodoCantidad;
                }
            }

            if (combo == null || campoCantidad == null) {
                continue;
            }

            String ubicacion = combo.getValue();
            String cantidadTexto = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto.isBlank()) {
                continue;
            }

            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadTexto);
            } catch (NumberFormatException e) {
                continue;
            }

            if (cantidad <= 0) {
                continue;
            }

            resultado.add(new UbicacionCompra(ubicacion, cantidad));
        }

        return resultado;
    }

    @Override
    protected boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        if (ubicacionesSeleccionadas == null) {
            return false;
        }

        HashSet<String> ubicacionesUnicas = new HashSet<>();
        for (UbicacionCompra ubicacionCompra : ubicacionesSeleccionadas) {
            if (ubicacionCompra == null || ubicacionCompra.getUbicacion() == null) {
                continue;
            }
            String ubicacion = ubicacionCompra.getUbicacion().trim();
            if (ubicacion.isEmpty()) {
                continue;
            }
            if (!ubicacionesUnicas.add(ubicacion)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void limpiarFormularioParaNuevo() {
        limpiarValidacionesInventario();
        seleccionarClaveAlternaPendiente = false;

        if (productoController != null) {
            productoController.limpiarSeleccion();
        }

        limpiarFormularioDependiente();

        if (txtPrecioSalida != null) {
            txtPrecioSalida.clear();
        }

        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(false);
        }

        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        limpiarFilasAdicionales();

        Platform.runLater(() -> {
            if (cbClaveProducto != null) {
                cbClaveProducto.requestFocus();
            }
        });
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

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (alert.isShowing()) {
                        Platform.runLater(alert::close);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
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

        if (checkBoxIVA != null && checkBoxIVA.isSelected()) {
            BigDecimal iva = precioSalida.multiply(IvaConfigService.getIvaTasa());
            precioConIva = precioSalida.add(iva);
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVA != null) txtPrecioIVA.setText(formatearDecimal(precioConIva));
        if (txtPrecioBruto != null) txtPrecioBruto.setText(formatearDecimal(precioBruto));
        if (txtPrecioTotal != null) txtPrecioTotal.setText(formatearDecimal(precioTotal));
    }

    @Override
    protected void recalcularPreciosRapido() {
        if (txtCantidadRapida == null || txtPrecioSalidaRapida == null) {
            return;
        }

        int cantidad = parseEntero(txtCantidadRapida.getText());

        if (cantidad <= 0) {
            if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.clear();
            if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.clear();
            if (txtPrecioIVARapida != null) txtPrecioIVARapida.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalidaRapida.getText());
        BigDecimal precioConIva = precioSalida;

        if (checkBoxIVARapida != null && checkBoxIVARapida.isSelected()) {
            BigDecimal iva = precioSalida.multiply(IvaConfigService.getIvaTasa());
            precioConIva = precioSalida.add(iva);
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVARapida != null) txtPrecioIVARapida.setText(formatearDecimal(precioConIva));
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.setText(formatearDecimal(precioBruto));
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.setText(formatearDecimal(precioTotal));
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
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPrecios();
            return;
        }

        if (!datosCompletosParaPrecioEntrada()) {
            limpiarPrecios();
            return;
        }

        boolean precioSalidaVacio = txtPrecioSalida == null ||
                txtPrecioSalida.getText() == null ||
                txtPrecioSalida.getText().isBlank();

        if (!precioSalidaVacio) {
            return;
        }

        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();

        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                // Primero verificar si existe el producto con lote (con o sin caducidad)
                if (!modelo.existeLoteParaProducto(lote, idProducto)) {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error",
                                "El producto con lote " + lote + " no está en stock o no existe.");
                    });
                    return Optional.empty();
                }

                // Obtener precios manejando caducidad NULL
                return modelo.obtenerPreciosProductoPorLotePresentacion(idProducto, lote, presentacion);
            }

            @Override
            protected void succeeded() {
                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPrecioEntrada(resultado.get());
                } else {
                    // Si no encuentra precios, podría ser porque hay stock pero sin caducidad
                    // o hay algún otro problema
                    Platform.runLater(() -> {
                        mostrarAlerta("Información",
                                "Producto encontrado, pero no se pudieron cargar los precios. " +
                                        "Verifique la disponibilidad.");
                    });
                }
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

        String idSnapshot = idProducto;
        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoUltimaEntrada(idSnapshot);
            }

            @Override
            protected void succeeded() {
                String idActual = productoController.getIdSeleccionado();
                if (!idSnapshot.equals(idActual)) {
                    return;
                }

                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPreciosRapidos(resultado.get());
                } else {
                    limpiarPreciosRapidos();
                }
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
        // Campos específicos de venta de solo lectura
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

        aplicarModoAjusteInventario();
    }

    @Override
    protected void configurarCalculoPreciosEspecifico() {
        // Configuración específica de cálculo de precios para venta
        if (txtPrecioSalida != null) {
            txtPrecioSalida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }

        if (checkBoxIVA != null) {
            checkBoxIVA.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }

        if (checkBoxIVARapida != null) {
            checkBoxIVARapida.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }
    }

    // === MÉTODOS ESPECÍFICOS DE VENTA ===

    private void aplicarModoSoloNormal() {
        if (!modoSoloNormal || tabPaneModo == null) {
            return;
        }

        if (tabRapido != null) {
            tabPaneModo.getTabs().remove(tabRapido);
        }

        if (tabNormal != null) {
            tabPaneModo.getSelectionModel().select(tabNormal);
        }

        tabPaneModo.getStyleClass().add("modo-tabs-sin-header");
    }

    private void aplicarModoAjusteInventario() {
        if (!modoAjusteInventario) {
            return;
        }

        if (txtPrecioSalida != null) {
            txtPrecioSalida.setEditable(false);
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.setEditable(false);
        }
    }

    private boolean datosCompletosParaPrecioEntrada() {
        return productoController.getIdSeleccionado() != null
                && !productoController.getIdSeleccionado().isBlank()
                && loteValidado
                && cbPresentacion.getValue() != null
                && !cbPresentacion.getValue().isBlank();
    }

    private void aplicarPrecioEntrada(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPrecios();
            return;
        }

        precioEntradaBase = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        precioIvaBase = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        if (txtPrecioEntrada != null) txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.setText(formatearDecimal(precioIvaBase));
        if (txtPrecioSalida != null) txtPrecioSalida.setText(formatearDecimal(precioEntradaBase));

        // SOLO actualizar txtPrecioEntrada (solo lectura)
        if (txtPrecioEntrada != null) {
            txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        }

        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.setText(formatearDecimal(precioIvaBase));
        }

        // SOLO actualizar txtPrecioSalida si está vacío
        if (txtPrecioSalida != null &&
                (txtPrecioSalida.getText() == null || txtPrecioSalida.getText().isBlank())) {
            txtPrecioSalida.setText(formatearDecimal(precioEntradaBase));
        }

        recalcularPrecios();
    }

    private void aplicarPreciosRapidos(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPreciosRapidos();
            return;
        }

        BigDecimal precioEntradaRapida = precios.getPrecioUnitario() != null
                ? precios.getPrecioUnitario()
                : BigDecimal.ZERO;
        BigDecimal precioEntradaIvaRapida = precios.getPrecioIva() != null
                ? precios.getPrecioIva()
                : BigDecimal.ZERO;

        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setText(formatearDecimal(precioEntradaRapida));
        }

        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.setText(formatearDecimal(precioEntradaIvaRapida));
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.setText(formatearDecimal(precioEntradaRapida));
        }

        recalcularPreciosRapido();
    }

    private void continuarGuardadoRapido(String clave, String nombre, String descripcion,
                                         String presentacionRapida, int factorRapido, int cantidad,
                                         String precioSalida, String precioIva,
                                         String precioBruto, String precioTotal) {

        if (itemsVenta == null) {
            mostrarAlerta("Error", "No se pudo registrar la venta en la tabla.");
            return;
        }

        // Obtener disponibilidades rápidas
        List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles =
                modelo.obtenerDisponibilidadesRapidas(clave, presentacionRapida, factorRapido);

        if (disponibles == null || disponibles.isEmpty()) {
            mostrarAlerta("Error", "No hay disponibilidad para el producto con las características especificadas.");
            return;
        }

        // Construir asignaciones
        List<AsignacionRapida> asignaciones = construirAsignacionesRapidas(disponibles, cantidad);
        if (asignaciones.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Confirmaciones del usuario
        if (!confirmarRevisionUbicacionesRapidas()) {
            return;
        }

        if (!mostrarResumenUbicacionesRapidas(asignaciones)) {
            return;
        }

        // Construir items de venta
        List<traspasoSalida> itemsGenerados = construirItemsRapidosVenta(
                clave, nombre, descripcion, asignaciones, presentacionRapida, factorRapido);

        if (itemsGenerados.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Procesar cada item generado
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        BigDecimal precioIvaDecimal = parseDecimal(precioIva);

        for (traspasoSalida item : itemsGenerados) {
            // Verificar si ya existe en la venta
            if (existeProductoLoteEnLista(clave, item.getLote(), null)) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza la venta para poder repetirlo.");
                return;
            }

            // Calcular precios para este item
            int cantidadItem = item.getCantidad();
            BigDecimal brutoItem = precioSalidaDecimal.multiply(BigDecimal.valueOf(cantidadItem));
            BigDecimal totalItem = precioIvaDecimal.multiply(BigDecimal.valueOf(cantidadItem));

            // Establecer precios en el item
            item.setPrecioEntrada(formatearDecimal(precioSalidaDecimal));
            item.setPrecioIva(formatearDecimal(precioIvaDecimal));
            item.setPrecioBruto(formatearDecimal(brutoItem));
            item.setPrecioTotal(formatearDecimal(totalItem));

            // Agregar a la lista de ventas
            itemsVenta.add(item);
        }

        // Actualizar interfaz si hay controlador principal
        if (mainController != null) {
            mainController.refrescarTabla();
        }

        // Mostrar mensaje de éxito y limpiar formulario
        mostrarAlertaSinEspera("Éxito", "Producto agregado a la venta.");
        limpiarFormularioParaNuevo();
    }

    private boolean confirmarPerdidaRapidaSiAplica(int cantidad) {
        if (txtPrecioEntradaRapida == null || txtPrecioSalidaRapida == null) {
            return true;
        }

        String precioEntradaTexto = txtPrecioEntradaRapida.getText() != null
                ? txtPrecioEntradaRapida.getText().trim()
                : "";
        String precioSalidaTexto = txtPrecioSalidaRapida.getText() != null
                ? txtPrecioSalidaRapida.getText().trim()
                : "";

        if (precioEntradaTexto.isBlank() || precioSalidaTexto.isBlank()) {
            return true;
        }

        BigDecimal precioEntrada = parseDecimal(precioEntradaTexto);
        BigDecimal precioSalida = parseDecimal(precioSalidaTexto);

        if (precioSalida.compareTo(precioEntrada) >= 0) {
            return true;
        }

        BigDecimal perdidaUnit = precioEntrada.subtract(precioSalida);
        BigDecimal perdidaTotal = perdidaUnit.multiply(BigDecimal.valueOf(cantidad));

        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Advertencia de pérdida");
        alerta.setHeaderText("El precio de salida es menor al precio de entrada.");
        alerta.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(6);
        javafx.scene.control.Label mensaje = new javafx.scene.control.Label("Puede haber posibles pérdidas con este precio.");
        javafx.scene.control.Label perdidaLabel = new javafx.scene.control.Label("Pérdida estimada: " + formatearDecimal(perdidaTotal));
        perdidaLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
        contenido.getChildren().addAll(mensaje, perdidaLabel);
        alerta.getDialogPane().setContent(contenido);

        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    private List<traspasoSalida> construirItemsRapidosVenta(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones,
            String presentacion,
            int factor
    ) {
        return construirItemsRapidos(clave, nombre, descripcion, asignaciones, presentacion, factor);
    }

    private static final int MAX_INTENTOS_SELECCION_EDICION = 10;

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        cargandoEdicion = true;
        bloqueoAutoseleccionEdicion = true;
        permitirEdicionManualLote = false;
        seleccionarClaveAlternaPendiente = false;
        programarCargaItemParaEditar(0);
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
                if (!bloqueoAutoseleccionEdicion || itemParaEditar == null) {
                    return;
                }
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
                if (itemParaEditar == null || permitirEdicionManualLote) {
                    return;
                }
                String esperado = itemParaEditar.getLote() == null ? "" : itemParaEditar.getLote();
                String actual = newVal == null ? "" : newVal;
                if (!esperado.equals(actual)) {
                    txtLote.setText(esperado);
                }
            });
            txtLote.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                bloqueoAutoseleccionEdicion = false;
                permitirEdicionManualLote = true;
            });
        }

        if (dpCaducidad != null) {
            dpCaducidad.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!bloqueoAutoseleccionEdicion || itemParaEditar == null) {
                    return;
                }
                java.time.LocalDate esperada = null;
                String cad = itemParaEditar.getCaducidad();
                if (cad != null && !cad.isBlank()) {
                    try {
                        esperada = java.time.LocalDate.parse(cad);
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
        if (!bloqueoAutoseleccionEdicion || itemParaEditar == null || combo == null) {
            return;
        }

        String esperado;
        if (combo == cbClaveProducto) {
            esperado = itemParaEditar.getClaveProducto();
        } else if (combo == cbProductoNombre) {
            esperado = itemParaEditar.getProducto();
        } else if (combo == cbPresentacion) {
            esperado = itemParaEditar.getPresentacion();
        } else {
            return;
        }

        String actual = combo.getValue();
        if (esperado != null && !esperado.equals(actual)) {
            combo.setValue(esperado);
            if (combo.getEditor() != null) {
                combo.getEditor().setText(esperado);
            }
        }
    }

    private void programarCargaItemParaEditar(int intentos) {
        if (itemParaEditar == null) {
            return;
        }

        seleccionarClaveAlternaPendiente = false;
        boolean seleccionado = productoController != null
                && productoController.setSeleccion(itemParaEditar.getClaveProducto(), itemParaEditar.getProducto());

        if (!seleccionado && intentos < MAX_INTENTOS_SELECCION_EDICION) {
            Platform.runLater(() -> programarCargaItemParaEditar(intentos + 1));
            return;
        }

        aplicarSeleccionManualParaEdicion();
        desactivarAutorellenoEnEdicion();

        // Al sincronizar clave/producto/descripción se disparan listeners que pueden limpiar
        // campos dependientes. Reaplicamos la información en el siguiente ciclo de UI para
        // garantizar que toda la información del registro quede precargada en edición.
        Platform.runLater(() -> {
            desactivarAutorellenoEnEdicion();
            aplicarDatosItemEnEdicion();
        });
    }

    private void aplicarSeleccionManualParaEdicion() {
        if (itemParaEditar == null) {
            return;
        }
        if (cbClaveProducto != null) {
            cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
            if (cbClaveProducto.getEditor() != null) {
                cbClaveProducto.getEditor().setText(itemParaEditar.getClaveProducto());
            }
        }
        if (cbProductoNombre != null) {
            cbProductoNombre.setValue(itemParaEditar.getProducto());
            if (cbProductoNombre.getEditor() != null) {
                cbProductoNombre.getEditor().setText(itemParaEditar.getProducto());
            }
        }
    }

    private void desactivarAutorellenoEnEdicion() {
        if (!cargandoEdicion) {
            return;
        }

        seleccionarClaveAlternaPendiente = false;
    }

    private void aplicarDatosItemEnEdicion() {
        if (itemParaEditar == null) {
            return;
        }

        if (txtDescripcion != null) {
            txtDescripcion.setText(itemParaEditar.getDescripcion());
        }

        if (txtNota != null) {
            txtNota.setText(itemParaEditar.getNota());
        }

        if (txtLote != null) {
            txtLote.setText(itemParaEditar.getLote());
        }
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        loteValidado = true;
        caducidadValidada = true;
        if (txtCantidad != null) {
            txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        }
        if (cbPresentacion != null) {
            cbPresentacion.setValue(itemParaEditar.getPresentacion());
        }
        if (txtFactor != null) {
            txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));
        }
        presentacionValida = true;
        factorValido = true;

        if (txtPrecioEntrada != null) {
            txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());
        }

        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.setText(itemParaEditar.getPrecioIva());
        }

        String precioSalida = itemParaEditar.getPrecioEntrada();
        if (txtPrecioSalida != null) {
            txtPrecioSalida.setText(precioSalida);
        }
        if (txtPrecioIVA != null) {
            txtPrecioIVA.setText(itemParaEditar.getPrecioIva());
        }
        if (txtPrecioBruto != null) {
            txtPrecioBruto.setText(itemParaEditar.getPrecioBruto());
        }
        if (txtPrecioTotal != null) {
            txtPrecioTotal.setText(itemParaEditar.getPrecioTotal());
        }

        if (checkBoxIVA != null) {
            BigDecimal base = parseDecimal(precioSalida);
            BigDecimal conIva = parseDecimal(itemParaEditar.getPrecioIva());
            checkBoxIVA.setSelected(conIva.compareTo(base) > 0);
        }

        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();

        Platform.runLater(() -> {
            desactivarAutorellenoEnEdicion();
            cargandoEdicion = false;
        });
    }

    private void configurarCaducidadDesdeTexto(String caducidadTexto) {
        if (caducidadTexto == null || caducidadTexto.isBlank()) {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(null);
            }
            return;
        }

        try {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(java.time.LocalDate.parse(caducidadTexto));
            }
        } catch (java.time.format.DateTimeParseException e) {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(null);
            }
        }
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        limpiarFilasAdicionales();
        ubicacionesCapturadas.clear();
        ultimaCantidadUbicacionValidada.clear();

        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) {
            return;
        }

        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesLista.get(i);
            if (i > 0) {
                agregarUbicacionCombo();
            }

            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);
            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            TextField campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);

            combo.setValue(ubicacion.getUbicacion());
            if (combo.getEditor() != null) {
                combo.getEditor().setText(ubicacion.getUbicacion());
            }

            campoCantidad.setText(String.valueOf(ubicacion.getCantidad()));
        }
    }

    private void cerrarFormulario() {
        if (btnGuardar != null && btnGuardar.getScene() != null) {
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    // === GETTERS Y SETTERS ESPECÍFICOS ===

    public void setItemsVenta(ObservableList<traspasoSalida> itemsVenta) {
        this.itemsVenta = itemsVenta;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setItemParaEditar(traspasoSalida item) {
        this.itemParaEditar = item;
        if (itemParaEditar != null && inicializado) {
            cargarItemParaEditar();
        }
    }

    public void setTituloFormulario(String tituloFormulario) {
        if (tituloFormulario == null || tituloFormulario.isBlank()) {
            return;
        }

        this.tituloFormulario = tituloFormulario;
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }
    }

    public void setModoSoloNormal(boolean modoSoloNormal) {
        this.modoSoloNormal = modoSoloNormal;
        aplicarModoSoloNormal();
    }

    public void setModoAjusteInventario(boolean modoAjusteInventario) {
        this.modoAjusteInventario = modoAjusteInventario;
        if (inicializado) {
            aplicarModoAjusteInventario();
        }
    }

    private void setCaducidadValidada(boolean validada) {
        this.caducidadValidada = validada;
    }
}
