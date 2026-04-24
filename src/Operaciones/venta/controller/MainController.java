package Operaciones.venta.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.helper.OverlayCarga;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.exportar.ReporteSalidaExporter;
import javafx.scene.control.ButtonBar;
import Operaciones.venta.model.model;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import Operaciones.traspasoSalida.model.traspasoSalida;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import VentanaPrincipal.controller.Pausable;
import VentanaPrincipal.controller.MovimientoType;
import Compartido.helper.BorradorService;
import Operaciones.venta.model.VentaBorradorDTO;
import Operaciones.compra.model.UbicacionCompra;

public class MainController implements ControladorVista, Pausable {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView contenidoTabla;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private Button btnEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblCliente;
    @FXML private Region expansor;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private TextField factura;
    @FXML private TextField totalVenta;
    @FXML private Button botonConfirmar;
    @FXML private CheckBox miCheckBox;

    private final model model = new model();
    private ObservableList<String> clientesCache;
    private final ObservableList<String> clientesFiltrados = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsVenta = FXCollections.observableArrayList();
    private String clienteSeleccionadoId;
    private boolean actualizandoFiltroCliente = false;
    private boolean actualizandoSeleccion = false;
    private OverlayCarga overlayCarga;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML private TableColumn<traspasoSalida, Boolean> colSelect;
    @FXML private TableColumn<traspasoSalida, String> colClaveProduct;
    @FXML private TableColumn<traspasoSalida, String> colProducto;
    @FXML private TableColumn<traspasoSalida, String> colDescripcionProducto;
    @FXML private TableColumn<traspasoSalida, String> colCantidad;
    @FXML private TableColumn<traspasoSalida, String> colLote;
    @FXML private TableColumn<traspasoSalida, String> colCaducidad;
    @FXML private TableColumn<traspasoSalida, String> colUbicacion;
    @FXML private TableColumn<traspasoSalida, String> colNota;
    @FXML private TableColumn<traspasoSalida, String> colPrecioUnitario;
    @FXML private TableColumn<traspasoSalida, String> colPrecioIva;
    @FXML private TableColumn<traspasoSalida, String> colPrecioBruto;
    @FXML private TableColumn<traspasoSalida, String> colPrecioTotaal;


    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            buscador.prefWidthProperty().bind(rootHBox.widthProperty().multiply(0.2));
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
            lblCliente.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.73));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            // Comentario
            contenedorComentario.maxWidthProperty().bind(contenedor.widthProperty());
            HBox.setHgrow(comentario, Priority.ALWAYS);
            comentario.setMaxWidth(Double.MAX_VALUE);

            // Botón confirmar
            contenedorBtnConfirmar.setMinWidth(Region.USE_PREF_SIZE);
            contenedorBtnConfirmar.setMaxWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(contenedorBtnConfirmar, Priority.NEVER);

            overlayCarga = new OverlayCarga(root, new Pane());

        });
        configurarAutocompleteClientes();
        configurarTabla();
        configurarConfirmacion();
        configurarTotalVenta();
        configurarAtajosTeclado();
        RefrescoHelper.setVistaActual("venta");
        RefrescoHelper.registrarRefresco("venta", this::actualizarVenta);
    }
    private void actualizarVenta() {
        Platform.runLater(() -> {
            itemsVenta.clear();
            if (buscador != null) {
                buscador.setValue(null);
                buscador.getEditor().clear();
            }
            if (comentario != null) {
                comentario.clear();
            }
            if (factura != null) {
                factura.clear();
            }
            if (totalVenta != null) {
                totalVenta.setText("0.00");
            }
            if (miCheckBox != null) {
                miCheckBox.setSelected(false);
            }
            if (contenidoTabla != null) {
                contenidoTabla.getSelectionModel().clearSelection();
                contenidoTabla.refresh();
            }
            clienteSeleccionadoId = null;
        });
        refrescarClientes();
    }

    public void refrescarClientes() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return model.obtenerNombresClientes();
            }

            @Override
            protected void succeeded() {
                List<String> resultado = getValue();

                Platform.runLater(() -> {
                    // Actualizar las listas en el hilo de JavaFX
                    if (resultado != null && !resultado.isEmpty()) {
                        clientesCache.setAll(resultado);
                        clientesFiltrados.setAll(clientesCache);

                        // Mantener el cliente seleccionado si existe
                        String seleccionActual = buscador.getValue();
                        if (seleccionActual != null && clientesCache.contains(seleccionActual)) {
                            buscador.setValue(seleccionActual);
                            // Actualizar el ID del cliente
                            clienteSeleccionadoId = model.obtenerIdClientePorNombre(seleccionActual);
                        }
                    } else {
                        clientesCache.clear();
                        clientesFiltrados.clear();
                    }
                });
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                System.err.println("✗ Error al refrescar clientes: " + ex.getMessage());
                ex.printStackTrace();

                Platform.runLater(() -> {
                    clientesCache.clear();
                    clientesFiltrados.clear();
                });
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void configurarAutocompleteClientes() {
        clientesCache = FXCollections.observableArrayList();
        buscador.setItems(clientesFiltrados);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return model.obtenerNombresClientes();
            }

            @Override
            protected void succeeded() {
                List<String> resultado = getValue();
                clientesCache.setAll(resultado != null ? resultado : java.util.Collections.emptyList());
                clientesFiltrados.setAll(clientesCache);
            }

            @Override
            protected void failed() {
                clientesCache.clear();
                clientesFiltrados.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoFiltroCliente) {
                return;
            }
            String seleccionado = buscador.getValue();
            if (seleccionado != null && seleccionado.equals(newText)) {
                return;
            }
            actualizandoFiltroCliente = true;
            try {
                String filtro = newText == null ? "" : newText.trim().toLowerCase();
                ObservableList<String> filtrados = FXCollections.observableArrayList();
                if (filtro.isEmpty()) {
                    filtrados.setAll(clientesCache);
                } else {
                    for (String nombre : clientesCache) {
                        if (nombre.toLowerCase().contains(filtro)) {
                            filtrados.add(nombre);
                        }
                    }
                }
                Platform.runLater(() -> {
                    clientesFiltrados.setAll(filtrados);
                    if (!clientesFiltrados.isEmpty() && buscador.isFocused()) {
                        buscador.show();
                    }
                });
            } finally {
                actualizandoFiltroCliente = false;
            }
        });

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                clienteSeleccionadoId = model.obtenerIdClientePorNombre(newVal);
            } else {
                clienteSeleccionadoId = null;
            }
        });
    }

    @FXML public void abrirFormularioVenta() {
        abrirFormularioVenta(null);
    }

    private void abrirFormularioVenta(traspasoSalida itemParaEditar) {
        Formularios.controller.controllerNuevaVenta controlador = new Formularios.controller.controllerNuevaVenta();
        controlador.setItemsVenta(itemsVenta);
        controlador.setMainController(this);
        if (itemParaEditar != null) {
            controlador.setItemParaEditar(itemParaEditar);
        }
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevaVenta.fxml",controlador,"Venta");
    }

    private void configurarConfirmacion() {
        if (botonConfirmar != null) {
            botonConfirmar.setOnAction(event -> confirmarVenta());
        }
    }

    private void confirmarVenta() {
        if (itemsVenta.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe agregar al menos un producto para confirmar la venta.");
            return;
        }
        if (clienteSeleccionadoId == null || clienteSeleccionadoId.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un cliente.");
            return;
        }
        String facturaTexto = factura != null ? factura.getText().trim() : "";
        if (facturaTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar el número de factura.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("Se está realizando una venta y una salida de productos de tu inventario.");
        confirmacion.setContentText("¿Deseas continuar con el registro?");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String nota = comentario != null ? comentario.getText() : "";
                String nombreCliente = buscador != null ? buscador.getValue() : "";
                List<traspasoSalida> copiaItems = new ArrayList<>(itemsVenta);

                if (overlayCarga != null) {
                    overlayCarga.mostrar();
                }

                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return model.registrarVenta(
                                clienteSeleccionadoId,
                                nota,
                                facturaTexto,
                                new ArrayList<>(copiaItems)
                        );
                    }
                };

                task.setOnSucceeded(e -> {
                    boolean registrado = task.getValue();
                    if (registrado) {
                        itemsVenta.clear();
                        if (comentario != null) {
                            comentario.clear();
                        }
                        if (factura != null) {
                            factura.clear();
                        }
                        if (buscador != null) {
                            buscador.setValue(null);
                            if (buscador.getEditor() != null) {
                                buscador.getEditor().clear();
                            }
                            clienteSeleccionadoId = null;
                        }
                        if (overlayCarga != null) {
                            overlayCarga.ocultar();
                        }
                        mostrarConfirmacionReporteVenta(facturaTexto, nombreCliente, nota, copiaItems);
                        BorradorService.getInstance().eliminar(MovimientoType.VENTA);
                        itemsVenta.clear();
                    } else {
                        if (overlayCarga != null) {
                            overlayCarga.ocultar();
                        }
                        mostrarAlerta("Error", "No se pudo registrar la venta.");
                    }
                });

                task.setOnFailed(e -> {
                    if (overlayCarga != null) {
                        overlayCarga.ocultar();
                    }
                    mostrarAlerta("Error", "No se pudo registrar la venta.");
                });

                Thread hilo = new Thread(task);
                hilo.setDaemon(true);
                hilo.start();
            }
        });

    }

    private void mostrarConfirmacionReporteVenta(String factura,
                                                 String nombreCliente,
                                                 String comentario,
                                                 List<traspasoSalida> itemsVenta) {
        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Venta exitosa");
        dialogo.setHeaderText("Venta registrada correctamente");
        dialogo.setContentText("¿Deseas descargar el reporte de esta venta?");

        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                ReporteSalidaExporter.exportarReporteVenta(
                        factura,
                        nombreCliente,
                        comentario,
                        new ArrayList<>(itemsVenta),
                        contenidoTabla != null && contenidoTabla.getScene() != null
                                ? contenidoTabla.getScene().getWindow()
                                : null
                );
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void configurarTabla() {
        if (contenidoTabla == null) {
            return;
        }
        contenidoTabla.setItems(itemsVenta);

        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<traspasoSalida, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<traspasoSalida, Boolean> param) {
                traspasoSalida item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionResumen"));
        colNota.setCellValueFactory(new PropertyValueFactory<>("nota"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        TableColumn<traspasoSalida, ?>[] columnas = new TableColumn[] {
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colCantidad, colLote,
                colCaducidad, colUbicacion, colNota, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<traspasoSalida, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setRowFactory(table -> {
            javafx.scene.control.TableRow<traspasoSalida> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    abrirFormularioVenta(row.getItem());
                }
            });
            return row;
        });

        contenidoTabla.setEditable(true);
        if (miCheckBox != null) {
            miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (actualizandoSeleccion) {
                    return;
                }
                actualizandoSeleccion = true;
                try {
                    for (traspasoSalida item : itemsVenta) {
                        item.setSeleccionado(newVal);
                    }
                } finally {
                    actualizandoSeleccion = false;
                }
            });
        }

        itemsVenta.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
                        if (miCheckBox != null && miCheckBox.isSelected() && !item.isSeleccionado()) {
                            item.setSeleccionado(true);
                        }
                    }
                }
                if (change.wasRemoved()) {
                    actualizarSeleccionGeneral();
                }
            }
        });

        for (traspasoSalida item : itemsVenta) {
            item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
        }
    }

    private void configurarTotalVenta() {
        if (totalVenta != null) {
            totalVenta.setEditable(false);
            totalVenta.setText("0.00");
        }

        itemsVenta.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalVenta());
                    }
                    actualizarTotalVenta();
                }
                if (change.wasRemoved()) {
                    actualizarTotalVenta();
                }
            }
        });

        actualizarTotalVenta();
    }

    private void actualizarTotalVenta() {
        if (totalVenta == null) {
            return;
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.List<traspasoSalida> filas = contenidoTabla != null
                ? (java.util.List<traspasoSalida>) contenidoTabla.getItems()
                : itemsVenta;
        for (traspasoSalida item : filas) {
            total = total.add(parseTotal(item != null ? item.getPrecioTotal() : null));
        }
        totalVenta.setText(total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    private java.math.BigDecimal parseTotal(String valor) {
        if (valor == null || valor.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        String limpio = valor.trim().replace(",", "");
        limpio = limpio.replaceAll("[^0-9.\\-]", "");
        if (limpio.isBlank() || "-".equals(limpio)) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    public void refrescarTabla() {
        if (contenidoTabla != null) {
            contenidoTabla.refresh();
        }
    }

    public boolean existeProductoLote(String claveProducto, String lote) {
        return existeProductoLote(claveProducto, lote, null);
    }

    public boolean existeProductoLote(String claveProducto, String lote, traspasoSalida itemExcluir) {
        if (claveProducto == null || claveProducto.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }
        String claveNormalizada = claveProducto.trim();
        String loteNormalizado = lote.trim();
        return itemsVenta.stream()
                .anyMatch(item -> item != null
                        && item != itemExcluir
                        && claveNormalizada.equals(item.getClaveProducto())
                        && loteNormalizado.equals(item.getLote()));
    }

    @FXML
    public void eliminarSeleccionados() {
        List<traspasoSalida> seleccionados = itemsVenta.stream()
                .filter(traspasoSalida::isSeleccionado)
                .collect(Collectors.toList());
        if (seleccionados.isEmpty()) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas eliminar las ventas seleccionadas?");
        confirmacion.setContentText("Esta acción eliminará los elementos seleccionados de la venta.");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                itemsVenta.removeAll(seleccionados);
                actualizarSeleccionGeneral();
            }
        });
    }

    private void actualizarSeleccionGeneral() {
        if (miCheckBox == null || actualizandoSeleccion) {
            return;
        }
        actualizandoSeleccion = true;
        try {
            boolean seleccionarTodo = !itemsVenta.isEmpty()
                    && itemsVenta.stream().allMatch(traspasoSalida::isSeleccionado);
            miCheckBox.setSelected(seleccionarTodo);
        } finally {
            actualizandoSeleccion = false;
        }
    }

    public void agregarYSeleccionarCliente(String nombreCliente) {
        if (nombreCliente == null || nombreCliente.trim().isEmpty()) {
            return;
        }

        Platform.runLater(() -> {
            // 👉 PRIMERO, desactivar temporalmente el listener del filtro
            actualizandoFiltroCliente = true;

            try {
                // Refrescar la lista completa (COPIA EXACTA de proveedores)
                Task<List<String>> task = new Task<>() {
                    @Override
                    protected List<String> call() {
                        return model.obtenerNombresClientes();
                    }

                    @Override
                    protected void succeeded() {
                        List<String> resultado = getValue();
                        Platform.runLater(() -> {
                            // Limpiar y actualizar las listas
                            clientesCache.clear();
                            clientesFiltrados.clear();

                            if (resultado != null) {
                                clientesCache.addAll(resultado);
                                clientesFiltrados.addAll(clientesCache);
                            }

                            // 👉 CRÍTICO: Establecer el valor ANTES de reactivar el listener
                            buscador.setValue(nombreCliente);

                            // Obtener y establecer el ID
                            clienteSeleccionadoId = model.obtenerIdClientePorNombre(nombreCliente);

                            // 👉 También actualizar el texto del editor
                            buscador.getEditor().setText(nombreCliente);

                            // Forzar un refresh del combobox
                            buscador.getSelectionModel().select(nombreCliente);
                        });
                    }
                };

                Thread hilo = new Thread(task);
                hilo.setDaemon(true);
                hilo.start();

            } finally {
                // 👉 Reactivar el listener después de un breve retardo (IGUAL que proveedores)
                new Thread(() -> {
                    try {
                        Thread.sleep(100); // MISMO tiempo que proveedores
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(() -> {
                        actualizandoFiltroCliente = false;
                    });
                }).start();
            }
        });
    }

    @FXML
    public void abrirNuevoCliente() {
        Formularios.controller.controllerNuevoCliente controlador = new Formularios.controller.controllerNuevoCliente();

        // 👉 EXACTAMENTE IGUAL que en proveedores
        controlador.setOnSaved(() -> {
            // Después de guardar, refrescar y seleccionar el nuevo cliente
            String nombreNuevoCliente = controlador.getNombreCliente();
            if (nombreNuevoCliente != null) {
                agregarYSeleccionarCliente(nombreNuevoCliente);
            }
        });

        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoCliente.fxml", controlador, "Cliente");
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
            if (event.getCode() == KeyCode.A) miCheckBox.setSelected(true);
            else if (event.getCode() == KeyCode.E) eliminarSeleccionados();
            else if (event.getCode() == KeyCode.N) abrirFormularioVenta();
            else if (event.getCode() == KeyCode.G) confirmarVenta();
            else return;
            event.consume();
        });
    }

    /*---------------------------------------------------------------*/
    /*-------------------- INTERFAZ PAUSABLE ----------------------- */
    /*---------------------------------------------------------------*/

    @Override
    public MovimientoType getTipoMovimiento() {
        return MovimientoType.VENTA;
    }

    @Override
    public Object guardarBorrador() {
        VentaBorradorDTO dto = new VentaBorradorDTO();

        // Cliente
        dto.setClienteNombre(obtenerClienteSeleccionado());
        dto.setClienteId(clienteSeleccionadoId);
        dto.setNumeroFactura(factura != null ? factura.getText() : "");
        dto.setComentario(comentario != null ? comentario.getText() : "");

        // Items
        List<VentaBorradorDTO.ItemVentaPlano> planos = new ArrayList<>();
        for (traspasoSalida item : itemsVenta) {
            VentaBorradorDTO.ItemVentaPlano p = new VentaBorradorDTO.ItemVentaPlano();
            p.setClaveProducto(item.getClaveProducto());
            p.setProducto(item.getProducto());
            p.setDescripcion(item.getDescripcion());
            p.setLote(item.getLote());
            p.setCaducidad(item.getCaducidad());
            p.setCantidad(item.getCantidad());
            p.setPresentacion(item.getPresentacion());
            p.setFactor(item.getFactor());
            p.setNota(item.getNota());
            p.setPrecioEntrada(item.getPrecioEntrada());
            p.setPrecioIva(item.getPrecioIva());
            p.setPrecioBruto(item.getPrecioBruto());
            p.setPrecioTotal(item.getPrecioTotal());

            // Ubicaciones
            List<VentaBorradorDTO.UbicacionPlano> ubicacionesPlano = new ArrayList<>();
            for (UbicacionCompra u : item.getUbicaciones()) {
                ubicacionesPlano.add(new VentaBorradorDTO.UbicacionPlano(u.getUbicacion(), u.getCantidad()));
            }
            p.setUbicaciones(ubicacionesPlano);

            planos.add(p);
        }
        dto.setItems(planos);

        return dto;
    }

    @Override
    public void cargarBorrador(Object borrador) {
        if (!(borrador instanceof VentaBorradorDTO)) return;
        VentaBorradorDTO dto = (VentaBorradorDTO) borrador;

        Platform.runLater(() -> {
            // Cliente
            if (dto.getClienteNombre() != null && !dto.getClienteNombre().isEmpty()) {
                buscador.setValue(dto.getClienteNombre());
                buscador.getEditor().setText(dto.getClienteNombre());
                clienteSeleccionadoId = dto.getClienteId();
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
                List<traspasoSalida> nuevos = new ArrayList<>();
                for (VentaBorradorDTO.ItemVentaPlano p : dto.getItems()) {
                    traspasoSalida item = new traspasoSalida(); // Constructor vacío

                    item.setClaveProducto(p.getClaveProducto());
                    item.setProducto(p.getProducto());
                    item.setDescripcion(p.getDescripcion());
                    item.setLote(p.getLote());
                    item.setCaducidad(p.getCaducidad());
                    item.setCantidad(p.getCantidad());
                    item.setPresentacion(p.getPresentacion());
                    item.setFactor(p.getFactor());
                    item.setNota(p.getNota());
                    item.setPrecioEntrada(p.getPrecioEntrada());
                    item.setPrecioIva(p.getPrecioIva());
                    item.setPrecioBruto(p.getPrecioBruto());
                    item.setPrecioTotal(p.getPrecioTotal());

                    // Restaurar ubicaciones
                    if (p.getUbicaciones() != null) {
                        List<UbicacionCompra> ubicaciones = new ArrayList<>();
                        for (VentaBorradorDTO.UbicacionPlano up : p.getUbicaciones()) {
                            UbicacionCompra u = new UbicacionCompra(up.getUbicacion(), up.getCantidad());
                            ubicaciones.add(u);
                        }
                        item.setUbicaciones(ubicaciones);
                    }

                    nuevos.add(item);
                }
                itemsVenta.setAll(nuevos);
            }

            // Actualizar UI
            actualizarSeleccionGeneral();
            actualizarTotalVenta();
        });
    }

    // Método auxiliar para obtener el cliente seleccionado
    private String obtenerClienteSeleccionado() {
        String valor = buscador.getValue();
        if (valor == null || valor.isBlank()) {
            valor = buscador.getEditor().getText();
        }
        return valor != null ? valor.trim() : "";
    }

}
