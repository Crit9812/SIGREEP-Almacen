package Operaciones.traspasoSalida.controller;

import Compartido.helper.OverlayCarga;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Operaciones.compra.model.UbicacionCompra;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import Operaciones.traspasoSalida.model.model;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.beans.value.ObservableValue;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import Compartido.exportar.ReporteSalidaExporter;
import javafx.scene.control.ButtonBar;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import VentanaPrincipal.controller.Pausable;
import VentanaPrincipal.controller.MovimientoType;
import Compartido.helper.BorradorService;
import Operaciones.traspasoSalida.model.TraspasoSalidaBorradorDTO;

public class MainController implements ControladorVista, Pausable {

    @FXML private StackPane root;
    @FXML private Label labelUsuario;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<traspasoSalida> contenidoTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private CheckBox miCheckBox;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblSucursal;
    @FXML private Button botonConfirmar;
    @FXML private Region expansor;
    @FXML private TextField totalTraspaso;
    @FXML private TableColumn<traspasoSalida, Boolean> colSelect;
    @FXML private TableColumn<traspasoSalida, String> colClaveProduct;
    @FXML private TableColumn<traspasoSalida, String> colProducto;
    @FXML private TableColumn<traspasoSalida, String> colDescripcionProducto;
    @FXML private TableColumn<traspasoSalida, String> colLote;
    @FXML private TableColumn<traspasoSalida, String> colCaducidad;
    @FXML private TableColumn<traspasoSalida, String> colUbicacion;
    @FXML private TableColumn<traspasoSalida, String> colPrecioUnitario;
    @FXML private TableColumn<traspasoSalida, String> colPrecioIva;
    @FXML private TableColumn<traspasoSalida, String> colPrecioBruto;
    @FXML private TableColumn<traspasoSalida, String> colPrecioTotaal;

    private final model model = new model();
    private ObservableList<String> sucursalesCache;
    private final ObservableList<String> sucursalesFiltradas = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsTraspaso = FXCollections.observableArrayList();
    private String sucursalSeleccionadaId;
    private boolean actualizandoSucursal = false;
    private boolean actualizandoSeleccion = false;
    private OverlayCarga overlayCarga;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

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
            lblSucursal.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.77));
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
            configurarAutocompleteSucursales();
            configurarTabla();
            configurarTotalTraspaso();
            configurarConfirmacion();
            configurarAtajosTeclado();
        RefrescoHelper.setVistaActual("traspasoSalida");
        RefrescoHelper.registrarRefresco("traspasoSalida", this::actualizarTraspasoSalida);
    }

    // Añade estos métodos en la clase MainController (traspasoSalida)
    public boolean existeProductoLote(String claveProducto, String lote) {
        return existeProductoLote(claveProducto, lote, null);
    }


    public boolean existeProductoLote(String claveProducto, String lote, traspasoSalida itemExcluir) {
        if (claveProducto == null || claveProducto.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }

        String claveNormalizada = claveProducto.trim();
        String loteNormalizado = lote.trim();

        return itemsTraspaso.stream()
                .anyMatch(item -> item != null
                        && item != itemExcluir
                        && claveNormalizada.equals(item.getClaveProducto())
                        && loteNormalizado.equals(item.getLote()));
    }

    private void actualizarTraspasoSalida() {
        Platform.runLater(() -> {
            itemsTraspaso.clear();
            if (buscador != null) {
                buscador.setValue(null);
                buscador.getEditor().clear();
            }
            if (comentario != null) {
                comentario.clear();
            }
            if (totalTraspaso != null) {
                totalTraspaso.setText("0.00");
            }
            if (miCheckBox != null) {
                miCheckBox.setSelected(false);
            }
            if (contenidoTabla != null) {
                contenidoTabla.getSelectionModel().clearSelection();
                contenidoTabla.refresh();
            }
            sucursalSeleccionadaId = null;
        });
        refrescarSucursales();
    }

    private void refrescarSucursales() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                System.out.println("✓ Refrescando lista de sucursales en segundo plano...");
                return model.obtenerNombresSucursales();
            }

            @Override
            protected void succeeded() {
                List<String> resultado = getValue();

                Platform.runLater(() -> {
                    // Actualizar las listas en el hilo de JavaFX
                    if (resultado != null && !resultado.isEmpty()) {
                        sucursalesCache.setAll(resultado);
                        sucursalesFiltradas.setAll(sucursalesCache);
                        System.out.println("✓ " + resultado.size() + " sucursales cargadas");
                    } else {
                        sucursalesCache.clear();
                        sucursalesFiltradas.clear();
                        System.out.println("✓ No hay sucursales disponibles");
                    }
                });
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                System.err.println("✗ Error al refrescar sucursales: " + ex.getMessage());
                ex.printStackTrace();

                Platform.runLater(() -> {
                    sucursalesCache.clear();
                    sucursalesFiltradas.clear();
                });
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    public void refrescarYSeleccionarSucursal(String nombreSucursal) {
        if (nombreSucursal == null || nombreSucursal.trim().isEmpty()) {
            return;
        }

        Platform.runLater(() -> {
            refrescarSucursales();
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(500));
            pause.setOnFinished(e -> {
                buscador.setValue(nombreSucursal);
                sucursalSeleccionadaId = model.obtenerIdSucursalPorNombre(nombreSucursal);
            });
            pause.play();
        });
    }


    private void configurarAutocompleteSucursales() {
        sucursalesCache = FXCollections.observableArrayList();
        buscador.setItems(sucursalesFiltradas);

        javafx.concurrent.Task<java.util.List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<String> call() {
                return model.obtenerNombresSucursales();
            }

            @Override
            protected void succeeded() {
                java.util.List<String> resultado = getValue();
                sucursalesCache.setAll(resultado != null ? resultado : java.util.Collections.emptyList());
                sucursalesFiltradas.setAll(sucursalesCache);
            }

            @Override
            protected void failed() {
                sucursalesCache.clear();
                sucursalesFiltradas.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoSucursal) {
                return;
            }
            actualizandoSucursal = true;
            try {
                String seleccionado = buscador.getValue();
                if (seleccionado != null && seleccionado.equals(newText)) {
                    return;
                }
                if (newText == null || newText.isBlank()) {
                    sucursalesFiltradas.setAll(sucursalesCache);
                    return;
                }

                ObservableList<String> filtrados = FXCollections.observableArrayList();
                for (String nombre : sucursalesCache) {
                    if (nombre.toLowerCase().contains(newText.toLowerCase())) {
                        filtrados.add(nombre);
                    }
                }

                java.util.List<String> nuevos = new java.util.ArrayList<>(filtrados);
                javafx.application.Platform.runLater(() -> {
                    sucursalesFiltradas.setAll(nuevos);
                    if (!nuevos.isEmpty() && buscador.isFocused()) {
                        buscador.show();
                    }
                });
            } finally {
                actualizandoSucursal = false;
            }
        });

        buscador.setOnShowing(event -> sucursalesFiltradas.setAll(sucursalesCache));

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                sucursalSeleccionadaId = model.obtenerIdSucursalPorNombre(newVal);
            } else {
                sucursalSeleccionadaId = null;
            }
        });
    }

    @FXML
    public void abrirTraspasoSalida() {
        abrirFormularioTraspaso(null);
    }

    private void abrirFormularioTraspaso(traspasoSalida itemParaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoTraspasoSalida.fxml"));
            Formularios.controller.controllerNuevoTraspasoSalida controlador =
                    new Formularios.controller.controllerNuevoTraspasoSalida();
            controlador.setItemsTraspaso(itemsTraspaso);
            controlador.setMainController(this);

            // Pasar el item para editar si existe
            if (itemParaEditar != null) {
                controlador.setItemParaEditar(itemParaEditar);
            }

            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(itemParaEditar != null ? "Editar Traspaso" : "Traspaso de salida");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        contenidoTabla.setItems(itemsTraspaso);

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
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionResumen"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        TableColumn<traspasoSalida, ?>[] columnas = new TableColumn[] {
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colLote,
                colCaducidad, colUbicacion, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<traspasoSalida, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        // AGREGAR ESTE CÓDIGO PARA EL DOBLE CLIC (EXACTAMENTE IGUAL AL DE VENTA)
        contenidoTabla.setRowFactory(table -> {
            javafx.scene.control.TableRow<traspasoSalida> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    abrirFormularioTraspaso(row.getItem());
                }
            });
            return row;
        });

        contenidoTabla.setEditable(true);
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizandoSeleccion) {
                return;
            }
            actualizandoSeleccion = true;
            try {
                for (traspasoSalida item : itemsTraspaso) {
                    item.setSeleccionado(newVal);
                }
            } finally {
                actualizandoSeleccion = false;
            }
        });

        itemsTraspaso.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
                        if (miCheckBox.isSelected() && !item.isSeleccionado()) {
                            item.setSeleccionado(true);
                        }
                    }
                }
                if (change.wasRemoved()) {
                    actualizarSeleccionGeneral();
                }
            }
        });

        for (traspasoSalida item : itemsTraspaso) {
            item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
        }
    }

    @FXML
    public void eliminarSeleccionados() {
        List<traspasoSalida> seleccionados = itemsTraspaso.stream()
                .filter(traspasoSalida::isSeleccionado)
                .collect(Collectors.toList());
        if (seleccionados.isEmpty()) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas eliminar los productos seleccionados?");
        confirmacion.setContentText("Esta acción eliminará los elementos seleccionados del traspaso.");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                itemsTraspaso.removeAll(seleccionados);
                actualizarSeleccionGeneral();
                actualizarTotalTraspaso();
            }
        });
    }

    private void actualizarSeleccionGeneral() {
        if (actualizandoSeleccion) {
            return;
        }
        actualizandoSeleccion = true;
        try {
            boolean seleccionarTodo = !itemsTraspaso.isEmpty()
                    && itemsTraspaso.stream().allMatch(traspasoSalida::isSeleccionado);
            miCheckBox.setSelected(seleccionarTodo);
        } finally {
            actualizandoSeleccion = false;
        }
    }

    private void configurarTotalTraspaso() {
        if (totalTraspaso != null) {
            totalTraspaso.setEditable(false);
            totalTraspaso.setText("0.00");
        }

        itemsTraspaso.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalTraspaso());
                    }
                }
            }
            actualizarTotalTraspaso();
        });

        actualizarTotalTraspaso();
    }

    private void configurarConfirmacion() {
        if (botonConfirmar != null) {
            botonConfirmar.setOnAction(event -> confirmarTraspasoSalida());
        }
    }

    private void confirmarTraspasoSalida() {
        if (itemsTraspaso.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe agregar al menos un producto para confirmar el traspaso.");
            return;
        }
        if (sucursalSeleccionadaId == null || sucursalSeleccionadaId.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar una sucursal de destino.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas confirmar el traspaso de salida?");
        confirmacion.setContentText("Esta acción registrará el traspaso y marcará los artículos correspondientes.");

        confirmacion.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (overlayCarga != null) {
                        overlayCarga.mostrar();
                    }
                    String nota = comentario != null ? comentario.getText() : "";

                // Obtener nombre de la sucursal destino
                String nombreSucursalDestino = buscador.getValue();

                List<traspasoSalida> itemsSnapshot = new ArrayList<>(itemsTraspaso);

                Task<String> task = new Task<>() {
                    @Override
                    protected String call() {
                        return model.registrarTraspasoSalida(
                                sucursalSeleccionadaId,
                                nombreSucursalDestino,
                                nota,
                                itemsSnapshot
                        );
                    }
                };

                task.setOnSucceeded(e -> {
                    String idSalida = task.getValue();
                    if (idSalida != null && !idSalida.isEmpty()) {
                        String claveSalida = " " + idSalida;  // ID REAL de la tabla salidas

                        // Limpiar la tabla
                        itemsTraspaso.clear();
                        actualizarTotalTraspaso();
                        if (comentario != null) {
                            comentario.clear();
                        }

                        if (overlayCarga != null) {
                            overlayCarga.ocultar();
                        }
                        mostrarConfirmacionReporte(claveSalida, nombreSucursalDestino, nota, itemsSnapshot);
                    } else {
                        if (overlayCarga != null) {
                            overlayCarga.ocultar();
                        }
                    }
                });

                task.setOnFailed(e -> {
                    if (overlayCarga != null) {
                        overlayCarga.ocultar();
                    }
                });

                javafx.animation.PauseTransition pausaCarga = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(80)
                );
                pausaCarga.setOnFinished(evt -> {
                    Thread hilo = new Thread(task);
                    hilo.setDaemon(true);
                    hilo.start();
                });
                pausaCarga.play();
            }
        });

        BorradorService.getInstance().eliminar(MovimientoType.TRASPASO);

    }

    private void actualizarTotalTraspaso() {
        if (totalTraspaso == null) {
            return;
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (traspasoSalida item : itemsTraspaso) {
            total = total.add(parseTotal(item != null ? item.getPrecioTotal() : null));
        }
        totalTraspaso.setText(total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
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
        contenidoTabla.refresh();
    }

    // En MainController.java (traspasoSalida)
    private void mostrarConfirmacionReporte(String claveSalida,
                                            String nombreSucursalDestino,
                                            String comentario,
                                            List<traspasoSalida> itemsTraspaso) {
        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Registro exitoso");
        dialogo.setHeaderText("Traspaso de salida registrado correctamente");
        dialogo.setContentText("¿Deseas descargar el reporte de este traspaso?");

        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                // CAMBIA ESTA LÍNEA: usa exportarReporteTraspasoSalida en lugar de exportarReporte
                ReporteSalidaExporter.exportarReporteTraspasoSalida(
                        claveSalida,
                        nombreSucursalDestino,
                        comentario,
                        new ArrayList<>(itemsTraspaso),
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
            else if (event.getCode() == KeyCode.N) abrirTraspasoSalida();
            else if (event.getCode() == KeyCode.G) confirmarTraspasoSalida();
            else return;
            event.consume();
        });
    }

    /*---------------------------------------------------------------*/
    /*-------------------- INTERFAZ PAUSABLE ----------------------- */
    /*---------------------------------------------------------------*/

    @Override
    public MovimientoType getTipoMovimiento() {
        return MovimientoType.TRASPASO;
    }

    @Override
    public Object guardarBorrador() {
        TraspasoSalidaBorradorDTO dto = new TraspasoSalidaBorradorDTO();

        // Sucursal destino
        dto.setSucursalNombre(buscador.getValue() != null ? buscador.getValue() : "");
        dto.setSucursalId(sucursalSeleccionadaId);

        // Comentario
        dto.setComentario(comentario != null ? comentario.getText() : "");

        // Items
        List<TraspasoSalidaBorradorDTO.ItemTraspasoPlano> planos = new ArrayList<>();
        for (traspasoSalida item : itemsTraspaso) {
            TraspasoSalidaBorradorDTO.ItemTraspasoPlano p = new TraspasoSalidaBorradorDTO.ItemTraspasoPlano();

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

            // Convertir ubicaciones
            List<TraspasoSalidaBorradorDTO.UbicacionPlano> ubicacionesPlano = new ArrayList<>();
            for (UbicacionCompra u : item.getUbicaciones()) {
                ubicacionesPlano.add(new TraspasoSalidaBorradorDTO.UbicacionPlano(u.getUbicacion(), u.getCantidad()));
            }
            p.setUbicaciones(ubicacionesPlano);

            planos.add(p);
        }
        dto.setItems(planos);

        return dto;
    }

    @Override
    public void cargarBorrador(Object borrador) {
        if (!(borrador instanceof TraspasoSalidaBorradorDTO)) return;
        TraspasoSalidaBorradorDTO dto = (TraspasoSalidaBorradorDTO) borrador;

        Platform.runLater(() -> {
            // Sucursal
            if (dto.getSucursalNombre() != null && !dto.getSucursalNombre().isEmpty()) {
                buscador.setValue(dto.getSucursalNombre());
                buscador.getEditor().setText(dto.getSucursalNombre());
                sucursalSeleccionadaId = dto.getSucursalId();
            }

            // Comentario
            if (comentario != null) {
                comentario.setText(dto.getComentario() != null ? dto.getComentario() : "");
            }

            // Items
            if (dto.getItems() != null) {
                List<traspasoSalida> nuevos = new ArrayList<>();
                for (TraspasoSalidaBorradorDTO.ItemTraspasoPlano p : dto.getItems()) {
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
                        for (TraspasoSalidaBorradorDTO.UbicacionPlano up : p.getUbicaciones()) {
                            UbicacionCompra u = new UbicacionCompra(up.getUbicacion(), up.getCantidad());
                            ubicaciones.add(u);
                        }
                        item.setUbicaciones(ubicaciones); // Esto actualizará ubicacionResumen automáticamente
                    }

                    nuevos.add(item);
                }
                itemsTraspaso.setAll(nuevos);
            }

            // Actualizar UI
            actualizarSeleccionGeneral();
            actualizarTotalTraspaso();
        });
    }
}
