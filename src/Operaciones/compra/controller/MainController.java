package Operaciones.compra.controller;

import Compartido.exportar.ReporteEntradaExporter;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.helper.BorradorService;
import Compartido.helper.OverlayCarga;
import Compartido.helper.RefrescoHelper;
import Operaciones.compra.model.CompraBorradorDTO;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.compra.model.model;
import VentanaPrincipal.controller.MovimientoType;
import VentanaPrincipal.controller.Pausable;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista, Pausable {

    @FXML private StackPane root;
    @FXML private Label labelUsuario;
    @FXML private VBox contenedor;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblProveedores;
    @FXML private Region expansor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<compra> contenidoTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private TextField factura;
    @FXML private TextField totalCompra;
    @FXML private CheckBox miCheckBox;
    @FXML private TableColumn<compra, Boolean> colSelect;
    @FXML private TableColumn<compra, String> colClaveProduct;
    @FXML private TableColumn<compra, String> colProducto;
    @FXML private TableColumn<compra, String> colDescripcionProducto;
    @FXML private TableColumn<compra, String> colLote;
    @FXML private TableColumn<compra, String> colCaducidad;
    @FXML private TableColumn<compra, String> colUbicacion;
    @FXML private TableColumn<compra, String> colNota;
    @FXML private TableColumn<compra, String> colPrecioUnitario;
    @FXML private TableColumn<compra, String> colPrecioIva;
    @FXML private TableColumn<compra, String> colPrecioBruto;
    @FXML private TableColumn<compra, String> colPrecioTotaal;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final model model = new model();

    private final ObservableList<String> proveedoresCache = FXCollections.observableArrayList();
    private final ObservableList<String> proveedoresFiltrados = FXCollections.observableArrayList();
    private final ObservableList<compra> itemsCompra = FXCollections.observableArrayList();

    private String proveedorSeleccionadoId;
    private boolean actualizandoSeleccionTodo = false;
    private boolean actualizandoFiltroProveedor = false;

    private OverlayCarga overlayCarga;
    private Pane overlayPaneCarga;
    private StackPane overlayRootActual;

    private final ExecutorService bg = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("compra-bg-" + t.getId());
                return t;
            }
    );

    private static final String CENTER = "-fx-alignment: CENTER;";

    @FXML
    public void initialize() {
        Platform.runLater(this::bindLayout);

        configurarAutocompleteProveedores();
        configurarTabla();
        configurarSeleccionTodo();
        configurarBloqueoProveedor();
        configurarTotalCompra();
        configurarAtajosTeclado();

        RefrescoHelper.setVistaActual("compra");
        RefrescoHelper.registrarRefresco("compra", this::actualizarCompra);
    }

    // =========================
    // Refresco
    // =========================
    private void actualizarCompra() {
        Platform.runLater(() -> {
            itemsCompra.clear();

            if (buscador != null) {
                buscador.setValue(null);
                buscador.getEditor().clear();
                buscador.setDisable(false);
            }

            if (factura != null) factura.clear();
            if (comentario != null) comentario.clear();
            if (totalCompra != null) totalCompra.setText("0.00");
            if (miCheckBox != null) miCheckBox.setSelected(false);

            proveedorSeleccionadoId = null;
        });

        refrescarProveedores();
    }

    private void bindLayout() {
        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

        HBox.setHgrow(expansor, Priority.ALWAYS);
        expansor.setMinWidth(10);

        buscador.prefWidthProperty().bind(rootHBox.widthProperty().multiply(0.2));
        lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
        lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
        lblProveedores.setMinWidth(Region.USE_PREF_SIZE);

        contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.7));
        contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

        contenedorComentario.maxWidthProperty().bind(contenedor.widthProperty());
        HBox.setHgrow(comentario, Priority.ALWAYS);
        comentario.setMaxWidth(Double.MAX_VALUE);

        contenedorBtnConfirmar.setMinWidth(Region.USE_PREF_SIZE);
        contenedorBtnConfirmar.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(contenedorBtnConfirmar, Priority.NEVER);
    }

    // =========================
    // Async helper
    // =========================
    private <T> void runAsync(Callable<T> work, Consumer<T> ok, Consumer<Throwable> fail) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> ok.accept(task.getValue()));
        task.setOnFailed(e -> fail.accept(task.getException()));
        bg.execute(task);
    }

    // =========================
    // Autocomplete proveedores
    // =========================
    private void configurarAutocompleteProveedores() {
        buscador.setItems(proveedoresFiltrados);
        cargarProveedoresInicial();

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoFiltroProveedor) return;

            String seleccionado = buscador.getValue();
            if (seleccionado != null && seleccionado.equals(newText)) return;

            actualizandoFiltroProveedor = true;
            try {
                String filtro = (newText == null) ? "" : newText.trim().toLowerCase();
                if (filtro.isEmpty()) {
                    proveedoresFiltrados.setAll(proveedoresCache);
                } else {
                    List<String> out = new ArrayList<>();
                    for (String n : proveedoresCache) {
                        if (n != null && n.toLowerCase().contains(filtro)) out.add(n);
                    }
                    proveedoresFiltrados.setAll(out);
                }

                if (!proveedoresFiltrados.isEmpty() && buscador.isFocused()) buscador.show();
            } finally {
                actualizandoFiltroProveedor = false;
            }
        });

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            proveedorSeleccionadoId = (newVal != null && !newVal.isBlank())
                    ? model.obtenerIdProveedorPorNombre(newVal)
                    : null;
        });
    }

    private void cargarProveedoresInicial() {
        runAsync(
                () -> model.obtenerNombresProveedores(),
                lista -> {
                    proveedoresCache.setAll(lista != null ? lista : Collections.emptyList());
                    proveedoresFiltrados.setAll(proveedoresCache);
                },
                ex -> {
                    proveedoresCache.clear();
                    proveedoresFiltrados.clear();
                }
        );
    }

    // =========================
    // Tabla
    // =========================
    private void configurarTabla() {
        contenidoTabla.setItems(itemsCompra);
        contenidoTabla.setEditable(true);

        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<compra, Boolean>, ObservableValue<Boolean>>() {
            @Override public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<compra, Boolean> param) {
                return param.getValue().seleccionadoProperty();
            }
        });
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionResumen"));
        colNota.setCellValueFactory(new PropertyValueFactory<>("nota"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        for (TableColumn<compra, ?> col : new TableColumn[]{
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colLote,
                colCaducidad, colUbicacion, colNota, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        }) col.setStyle(CENTER);

        contenidoTabla.setRowFactory(table -> {
            TableRow<compra> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) abrirFormularioCompra(row.getItem());
            });
            return row;
        });
    }

    // =========================
    // Seleccionar todo
    // =========================
    private void configurarSeleccionTodo() {
        if (miCheckBox == null) return;

        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizandoSeleccionTodo) return;
            for (compra item : itemsCompra) item.setSeleccionado(newVal);
            contenidoTabla.refresh();
        });

        itemsCompra.addListener((ListChangeListener<compra>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (compra item : change.getAddedSubList()) {
                        item.seleccionadoProperty().addListener((o, ov, nv) -> actualizarSeleccionTodo());
                    }
                }
            }
            actualizarSeleccionTodo();
        });
    }

    private void actualizarSeleccionTodo() {
        if (miCheckBox == null) return;
        try {
            actualizandoSeleccionTodo = true;
            miCheckBox.setSelected(!itemsCompra.isEmpty() && itemsCompra.stream().allMatch(compra::isSeleccionado));
        } finally {
            actualizandoSeleccionTodo = false;
        }
    }

    // =========================
    // Bloqueo proveedor
    // =========================
    private void configurarBloqueoProveedor() {
        buscador.setDisable(!itemsCompra.isEmpty());
        itemsCompra.addListener((ListChangeListener<compra>) c -> buscador.setDisable(!itemsCompra.isEmpty()));
    }

    // =========================
    // Total compra
    // =========================
    private void configurarTotalCompra() {
        if (totalCompra != null) {
            totalCompra.setEditable(false);
            totalCompra.setText("0.00");
        }

        itemsCompra.addListener((ListChangeListener<compra>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (compra item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalCompra());
                    }
                }
            }
            actualizarTotalCompra();
        });

        actualizarTotalCompra();
    }

    private void actualizarTotalCompra() {
        if (totalCompra == null) return;

        BigDecimal total = BigDecimal.ZERO;
        for (compra item : itemsCompra) total = total.add(parseTotal(item == null ? null : item.getPrecioTotal()));
        totalCompra.setText(total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private BigDecimal parseTotal(String valor) {
        if (valor == null || valor.isBlank()) return BigDecimal.ZERO;
        String limpio = valor.trim().replace(",", "").replaceAll("[^0-9.\\-]", "");
        if (limpio.isBlank() || "-".equals(limpio)) return BigDecimal.ZERO;
        try { return new BigDecimal(limpio); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    // =========================
    // Formularios
    // =========================
    @FXML
    public void formularioNuevaCompra() { abrirFormularioCompra(null); }

    private void abrirFormularioCompra(compra itemParaEditar) {
        String proveedorNombre = obtenerProveedorSeleccionado();
        if (proveedorNombre == null || proveedorNombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de agregar productos.");
            return;
        }

        String idProveedor = (proveedorSeleccionadoId != null) ? proveedorSeleccionadoId : model.obtenerIdProveedorPorNombre(proveedorNombre);
        if (idProveedor == null || idProveedor.isBlank()) {
            mostrarAlerta("Error", "No se encontró el proveedor seleccionado.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
            controlador.setItemsCompra(itemsCompra);
            controlador.setMainController(this);
            controlador.setProveedorSeleccionado(idProveedor, proveedorNombre);
            if (itemParaEditar != null) controlador.setItemParaEditar(itemParaEditar);
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Compra");
            stage.setScene(new Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de compra.");
        }
    }

    // =========================
    // Guardar compras
    // =========================
    @FXML
    public void guardarCompras() {
        if (itemsCompra.isEmpty()) { mostrarAlerta("Advertencia", "No hay compras para registrar."); return; }

        String proveedorNombre = obtenerProveedorSeleccionado();
        if (proveedorNombre == null || proveedorNombre.isBlank()) { mostrarAlerta("Advertencia", "Debe seleccionar un proveedor."); return; }

        String idProveedor = (proveedorSeleccionadoId != null) ? proveedorSeleccionadoId : model.obtenerIdProveedorPorNombre(proveedorNombre);
        if (idProveedor == null || idProveedor.isBlank()) { mostrarAlerta("Error", "No se encontró el proveedor seleccionado."); return; }

        String numeroFactura = factura != null ? factura.getText().trim() : "";
        if (numeroFactura.isEmpty()) { mostrarAlerta("Advertencia", "Debe capturar el número de factura antes de confirmar."); return; }

        String comentarioTexto = comentario != null ? comentario.getText().trim() : "";
        if (!confirmarRegistroCompra()) return;

        List<compra> snapshot = new ArrayList<>(itemsCompra);

        mostrarOverlayCarga("Cargando...");

        runAsync(
                () -> {
                    boolean registrado = model.registrarCompra(idProveedor, numeroFactura, comentarioTexto, snapshot);
                    if (!registrado) return null;
                    String clave = model.obtenerClaveCompraReciente(idProveedor, numeroFactura);
                    return (clave != null) ? clave : "";
                },
                claveCompra -> {
                    if (claveCompra == null) {
                        ocultarOverlayCarga();
                        mostrarAlerta("Error", "No se pudo registrar la compra.");
                        return;
                    }
                    if (claveCompra.isBlank()) claveCompra = "COMP_" + System.currentTimeMillis();

                    List<compra> copiaItems = new ArrayList<>(snapshot);

                    itemsCompra.clear();
                    if (factura != null) factura.clear();
                    if (comentario != null) comentario.clear();
                    actualizarTotalCompra();

                    proveedorSeleccionadoId = null;
                    buscador.setDisable(false);
                    buscador.setValue(null);

                    mostrarConfirmacionReporte(claveCompra, proveedorNombre, comentarioTexto, copiaItems);
                },
                ex -> {
                    ocultarOverlayCarga();
                    mostrarAlerta("Error", "No se pudo registrar la compra.");
                }
        );

        BorradorService.getInstance().eliminar(MovimientoType.COMPRA);
    }

    // =========================
    // Eliminar seleccionados
    // =========================
    @FXML
    public void eliminarSeleccionados() {
        if (itemsCompra.isEmpty()) {
            mostrarAlerta("Advertencia", "No hay registros para eliminar.");
            return;
        }
        if (itemsCompra.stream().noneMatch(compra::isSeleccionado)) {
            mostrarAlerta("Advertencia", "Seleccione al menos una fila para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro de que desea eliminar los registros seleccionados?");

        java.util.Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            itemsCompra.removeIf(compra::isSeleccionado);
            actualizarSeleccionTodo();
            contenidoTabla.refresh();
            actualizarTotalCompra();
        }
    }

    public void refrescarTabla() { contenidoTabla.refresh(); }

    private void mostrarOverlayCarga(String mensaje) {
        inicializarOverlayGlobal();
        if (overlayCarga == null) return;
        overlayCarga.setMensaje(mensaje);
        overlayCarga.mostrar();
    }

    private void ocultarOverlayCarga() {
        if (overlayCarga == null) return;
        overlayCarga.ocultar();
    }

    private void inicializarOverlayGlobal() {
        if (root == null || root.getScene() == null) {
            return;
        }

        StackPane rootEscena = (root.getScene().getRoot() instanceof StackPane stackPaneEscena)
                ? stackPaneEscena
                : root;

        if (overlayCarga != null && overlayPaneCarga != null && overlayRootActual == rootEscena
                && overlayPaneCarga.getParent() == rootEscena) {
            return;
        }

        if (overlayPaneCarga != null && overlayPaneCarga.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(overlayPaneCarga);
        }

        overlayPaneCarga = new Pane();
        overlayRootActual = rootEscena;
        overlayRootActual.getChildren().add(overlayPaneCarga);
        overlayCarga = new OverlayCarga(overlayRootActual, overlayPaneCarga);
    }

    private boolean confirmarRegistroCompra() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar compra");
        confirmacion.setHeaderText("¿Deseas confirmar el registro de esta compra?");
        confirmacion.setContentText("Si aceptas, se guardarán los registros en la base de datos.");

        ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(btnAceptar, btnCancelar);

        return confirmacion.showAndWait().filter(respuesta -> respuesta == btnAceptar).isPresent();
    }


    // =========================
    // Proveedores (refresco y selección)
    // =========================
    public void refrescarProveedores() {
        String seleccionActual = buscador.getValue();
        runAsync(
                () -> model.obtenerNombresProveedores(),
                lista -> {
                    proveedoresCache.setAll(lista != null ? lista : Collections.emptyList());
                    proveedoresFiltrados.setAll(proveedoresCache);
                    if (seleccionActual != null && proveedoresCache.contains(seleccionActual)) buscador.setValue(seleccionActual);
                },
                ex -> {
                    proveedoresCache.clear();
                    proveedoresFiltrados.clear();
                }
        );
    }

    public void agregarYSeleccionarProveedor(String nombreProveedor) {
        if (nombreProveedor == null || nombreProveedor.trim().isEmpty()) return;

        actualizandoFiltroProveedor = true;
        runAsync(
                () -> model.obtenerNombresProveedores(),
                lista -> Platform.runLater(() -> {
                    proveedoresCache.setAll(lista != null ? lista : Collections.emptyList());
                    proveedoresFiltrados.setAll(proveedoresCache);

                    buscador.setValue(nombreProveedor);
                    buscador.getEditor().setText(nombreProveedor);
                    buscador.getSelectionModel().select(nombreProveedor);

                    proveedorSeleccionadoId = model.obtenerIdProveedorPorNombre(nombreProveedor);

                    actualizandoFiltroProveedor = false;
                }),
                ex -> Platform.runLater(() -> {
                    actualizandoFiltroProveedor = false;
                })
        );
    }

    @FXML
    public void abrirNuevoProveedor() {
        Formularios.controller.controllerNuevoProveedor controlador = new Formularios.controller.controllerNuevoProveedor();
        controlador.configurarTextoNuevoProveedor("Agregar proveedor", "Agregar proveedor");

        controlador.setOnSaved(() -> {
            String nombreNuevoProveedor = controlador.getNombreProveedor(); // getter existente en tu controlador
            if (nombreNuevoProveedor != null) agregarYSeleccionarProveedor(nombreNuevoProveedor);
        });

        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoProveedor.fxml", controlador, "Proveedor");
    }

    // =========================
    // Utilidades UI
    // =========================
    private String obtenerProveedorSeleccionado() {
        String valor = buscador.getValue();
        if (valor == null || valor.isBlank()) valor = buscador.getEditor().getText();
        return valor != null ? valor.trim() : "";
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarConfirmacionReporte(String claveCompra,
                                            String nombreProveedor,
                                            String comentario,
                                            List<compra> itemsCompra) {
        ocultarOverlayCarga();

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Registro exitoso");
        dialogo.setHeaderText("Compra registrada correctamente");
        dialogo.setContentText("¿Deseas descargar el reporte de esta compra?");

        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                ReporteEntradaExporter.exportarReporteCompra(
                        claveCompra,
                        nombreProveedor,
                        comentario,
                        new ArrayList<>(itemsCompra),
                        contenidoTabla != null && contenidoTabla.getScene() != null
                                ? contenidoTabla.getScene().getWindow()
                                : null
                );
            }
        });
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
                miCheckBox.setSelected(true);
            } else if (event.getCode() == KeyCode.E) {
                eliminarSeleccionados();
            } else if (event.getCode() == KeyCode.N) {
                formularioNuevaCompra();
            } else if (event.getCode() == KeyCode.G) {
                guardarCompras();
            } else {
                return;
            }
            event.consume();
        });
    }

    /*---------------------------------------------------------------*/
    /*-------------------- INTERFAZ PAUSABLE ----------------------- */
    /*---------------------------------------------------------------*/

    @Override
    public MovimientoType getTipoMovimiento() {
        return MovimientoType.COMPRA;
    }

    @Override
    public Object guardarBorrador() {
        CompraBorradorDTO dto = new CompraBorradorDTO();

        dto.setProveedorNombre(obtenerProveedorSeleccionado());
        dto.setProveedorId(proveedorSeleccionadoId);
        dto.setNumeroFactura(factura != null ? factura.getText() : "");
        dto.setComentario(comentario != null ? comentario.getText() : "");

        List<CompraBorradorDTO.ItemCompraPlano> planos = new ArrayList<>();
        for (compra item : itemsCompra) {
            CompraBorradorDTO.ItemCompraPlano p = new CompraBorradorDTO.ItemCompraPlano();
            p.setClaveProducto(item.getClaveProducto());
            p.setProducto(item.getProducto());
            p.setDescripcion(item.getDescripcion());
            p.setLote(item.getLote());
            p.setCaducidad(item.getCaducidad());
            p.setCantidad(item.getCantidad());
            p.setClaveAlterna(item.getClaveAlterna());
            p.setPresentacion(item.getPresentacion());
            p.setFactor(item.getFactor());
            p.setNota(item.getNota());
            p.setPrecioEntrada(item.getPrecioEntrada());
            p.setPrecioIva(item.getPrecioIva());
            p.setPrecioBruto(item.getPrecioBruto());
            p.setPrecioTotal(item.getPrecioTotal());
            p.setAplicaIva(item.isAplicaIva());

            // Convertir ubicaciones
            List<CompraBorradorDTO.UbicacionPlano> ubicacionesPlano = new ArrayList<>();
            for (UbicacionCompra u : item.getUbicaciones()) {
                ubicacionesPlano.add(new CompraBorradorDTO.UbicacionPlano(u.getUbicacion(), u.getCantidad()));
            }
            p.setUbicaciones(ubicacionesPlano);

            planos.add(p);
        }
        dto.setItems(planos);

        return dto;
    }

    @Override
    public void cargarBorrador(Object borrador) {
        if (!(borrador instanceof CompraBorradorDTO)) return;
        CompraBorradorDTO dto = (CompraBorradorDTO) borrador;

        Platform.runLater(() -> {
            // Proveedor
            if (dto.getProveedorNombre() != null && !dto.getProveedorNombre().isEmpty()) {
                buscador.setValue(dto.getProveedorNombre());
                buscador.getEditor().setText(dto.getProveedorNombre());
                proveedorSeleccionadoId = dto.getProveedorId();
            }

            // Factura y comentario
            if (factura != null) {
                factura.setText(dto.getNumeroFactura() != null ? dto.getNumeroFactura() : "");
            }
            if (comentario != null) {
                comentario.setText(dto.getComentario() != null ? dto.getComentario() : "");
            }

            // Items
            if (dto.getItems() != null) {
                List<compra> nuevos = new ArrayList<>();
                for (CompraBorradorDTO.ItemCompraPlano p : dto.getItems()) {
                    compra item = new compra(); // Constructor vacío

                    item.setClaveProducto(p.getClaveProducto());
                    item.setProducto(p.getProducto());
                    item.setDescripcion(p.getDescripcion());
                    item.setLote(p.getLote());
                    item.setCaducidad(p.getCaducidad());
                    item.setCantidad(p.getCantidad());
                    item.setClaveAlterna(p.getClaveAlterna());
                    item.setPresentacion(p.getPresentacion());
                    item.setFactor(p.getFactor());
                    item.setNota(p.getNota());
                    item.setPrecioEntrada(p.getPrecioEntrada());
                    item.setPrecioIva(p.getPrecioIva());
                    item.setPrecioBruto(p.getPrecioBruto());
                    item.setPrecioTotal(p.getPrecioTotal());
                    item.setAplicaIva(p.isAplicaIva());

                    // Restaurar ubicaciones (esto actualizará ubicacionResumen automáticamente)
                    if (p.getUbicaciones() != null) {
                        List<UbicacionCompra> ubicaciones = new ArrayList<>();
                        for (CompraBorradorDTO.UbicacionPlano up : p.getUbicaciones()) {
                            // Si UbicacionCompra tiene constructor (String, int)
                            UbicacionCompra u = new UbicacionCompra(up.getUbicacion(), up.getCantidad());
                            ubicaciones.add(u);
                        }
                        item.setUbicaciones(ubicaciones);
                    }

                    nuevos.add(item);
                }
                itemsCompra.setAll(nuevos);
            }

            // Actualizar UI
            buscador.setDisable(!itemsCompra.isEmpty());
            actualizarSeleccionTodo();
            actualizarTotalCompra();
        });
    }

}
