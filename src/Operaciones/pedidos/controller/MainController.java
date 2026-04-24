package Operaciones.pedidos.controller;

import VentanaPrincipal.controller.Pausable;
import VentanaPrincipal.controller.MovimientoType;
import Compartido.helper.BorradorService;
import Operaciones.pedidos.model.PedidoBorradorDTO;
import Compartido.exportar.exportador;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.helper.OverlayCarga;
import Formularios.controller.controllerNuevoPedido;
import Operaciones.pedidos.model.itemPedido;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Callback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista, Pausable{

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<itemPedido> contenidoTabla;
    @FXML private Label lblEliminar;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;
    // Columnas
    @FXML private TableColumn<itemPedido, Boolean> colSelect;
    @FXML private TableColumn<itemPedido, String> colClaveProduct;
    @FXML private TableColumn<itemPedido, Integer> colCantidad;
    @FXML private TableColumn<itemPedido, String> colProducto;
    @FXML private TableColumn<itemPedido, String> colDescripcionProducto;
    @FXML private TableColumn<itemPedido, String> colPresentacion;
    @FXML private TableColumn<itemPedido, String> colFactor;
    @FXML private CheckBox miCheckBoxSeleccionarTodo;

    private final ObservableList<itemPedido> itemsPedido = FXCollections.observableArrayList();
    private Stage formularioStage;
    private itemPedido itemSeleccionadoParaEditar;
    private OverlayCarga overlayCarga;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));
            overlayCarga = new OverlayCarga(root, new Pane());

            // ===== CONFIGURACIONES =====
            configurarColumnasTabla();
            contenidoTabla.setItems(itemsPedido);
            configurarSeleccionTodo();
            configurarEventosEdicionTabla();
            configurarAtajosTeclado();
        });
    }

    private void configurarColumnasTabla() {
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<itemPedido, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<itemPedido, Boolean> param) {
                itemPedido item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                } else {
                    return new SimpleBooleanProperty(false);
                }
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));

        // Hacer que las columnas sean editables si es necesario
        contenidoTabla.setEditable(true);

        TableColumn<itemPedido, ?>[] columnas = new TableColumn[]{
                colSelect, colClaveProduct, colCantidad, colProducto,
                colDescripcionProducto, colPresentacion, colFactor
        };
        for (TableColumn<itemPedido, ?> col : columnas) col.setStyle("-fx-alignment: CENTER;");
    }

    private void configurarSeleccionTodo() {
        miCheckBoxSeleccionarTodo.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (itemPedido item : itemsPedido) {
                item.setSeleccionado(newVal);
            }
        });
    }

    private void configurarEventosEdicionTabla() {
        // Doble clic para editar
        contenidoTabla.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                itemPedido seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    abrirFormularioParaEditar(seleccionado);
                }
            }
        });

        // Tecla Enter para editar
        contenidoTabla.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                itemPedido seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    abrirFormularioParaEditar(seleccionado);
                }
            }
        });
    }

    @FXML
    public void abrirFormularioPedido() {
        abrirFormulario(null);
    }

    private void abrirFormularioParaEditar(itemPedido item) {
        abrirFormulario(item);
    }

    private void abrirFormulario(itemPedido itemParaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Formularios/view/nuevoPedido.fxml")
            );

            // Cargar el formulario
            Pane formulario = loader.load();

            // Obtener el controlador
            Formularios.controller.controllerNuevoPedido controlador = loader.getController();

            // PASAMOS LA LISTA DE LA TABLA AL FORMULARIO
            controlador.setItemsPedido(itemsPedido);
            // Pasamos la referencia al stage para poder cerrarlo
            controlador.setMainController(this);

            // 🔥 NUEVO: Si hay un item para editar, lo cargamos en el formulario
            if (itemParaEditar != null) {
                controlador.cargarItemParaEditar(itemParaEditar);
                this.itemSeleccionadoParaEditar = itemParaEditar; // Guardar referencia
            }

            // Crear nueva ventana modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(itemParaEditar != null ? "Editar Producto" : "Nuevo Pedido");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.setOnHidden(e -> {
                // Limpiar referencia cuando se cierra
                this.formularioStage = null;
                this.itemSeleccionadoParaEditar = null;
            });

            this.formularioStage = stage;
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error al abrir formulario: " + e.getMessage());
        }
    }

    @FXML
    public void descargar(){
        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Platform.runLater(() -> {
                        try {
                            boolean columnaVisible = colSelect.isVisible();

                            // Ocultar temporalmente la columna del checkbox
                            colSelect.setVisible(false);
                            exportador.exportarTabla(contenidoTabla, "pedidos", "pdf");

                            // Restaurar visibilidad de la columna
                            colSelect.setVisible(columnaVisible);

                            Platform.runLater(() -> {
                                if (overlayCarga != null) {
                                    overlayCarga.ocultar();
                                }
                                BorradorService.getInstance().eliminar(MovimientoType.PEDIDO);
                            });

                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                if (overlayCarga != null) {
                                    overlayCarga.ocultar();
                                }
                                // Asegurar que la columna se restaure incluso si hay error
                                colSelect.setVisible(true);
                                mostrarError("Error al exportar: " + e.getMessage());
                            });
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error al exportar: " + e.getMessage(), e);
                }
                return null;
            }
        };

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            if (exception != null) {
                Platform.runLater(() -> {
                    if (overlayCarga != null) {
                        overlayCarga.ocultar();
                    }
                    // Restaurar columna en caso de error
                    colSelect.setVisible(true);
                    mostrarError("Error en exportación: " + exception.getMessage());
                });
            }
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void eliminarProducto() {
        boolean haySeleccionados = false;
        for (itemPedido item : itemsPedido) {
            if (item.isSeleccionado()) {
                haySeleccionados = true;
                break;
            }
        }

        if (!haySeleccionados) {
            mostrarAlerta("Advertencia", "No hay productos seleccionados para eliminar.");
            return;
        }

        // Pedir confirmación al usuario
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("Eliminar productos seleccionados");
        confirmacion.setContentText("¿Está seguro que desea eliminar los productos seleccionados?");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                // Crear una copia de la lista para evitar ConcurrentModificationException
                ObservableList<itemPedido> itemsAEliminar = FXCollections.observableArrayList();

                // Identificar los items seleccionados
                for (itemPedido item : itemsPedido) {
                    if (item.isSeleccionado()) {
                        itemsAEliminar.add(item);
                    }
                }

                // Eliminar los items seleccionados
                itemsPedido.removeAll(itemsAEliminar);

                // Desmarcar "Seleccionar todo" si estaba marcado
                miCheckBoxSeleccionarTodo.setSelected(false);

                // Mostrar mensaje de éxito
                mostrarAlerta("Éxito", "Se eliminaron " + itemsAEliminar.size() + " producto(s) del pedido.");

                // Refrescar la tabla
                refrescarTabla();
            }
        });
    }

    public void actualizarItemEnLista(itemPedido itemViejo, itemPedido itemNuevo) {
        int indice = itemsPedido.indexOf(itemViejo);
        if (indice >= 0) {
            itemsPedido.set(indice, itemNuevo);
            refrescarTabla();
        }
    }

    public void eliminarItemDeLista(itemPedido item) {
        itemsPedido.remove(item);
        refrescarTabla();
    }

    public void cerrarFormulario() {
        if (formularioStage != null) {
            formularioStage.close();
            formularioStage = null;
            itemSeleccionadoParaEditar = null;
        }
    }

    public void refrescarTabla() {
        contenidoTabla.refresh();
    }

    public itemPedido getItemSeleccionadoParaEditar() {
        return itemSeleccionadoParaEditar;
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
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
            if (event.getCode() == KeyCode.A) miCheckBoxSeleccionarTodo.setSelected(true);
            else if (event.getCode() == KeyCode.E) eliminarProducto();
            else if (event.getCode() == KeyCode.N) abrirFormularioPedido();
            else if (event.getCode() == KeyCode.D) descargar();
            else return;
            event.consume();
        });
    }

    /*---------------------------------------------------------------*/
    /*-------------------- INTERFAZ PAUSABLE ----------------------- */
    /*---------------------------------------------------------------*/

    @Override
    public MovimientoType getTipoMovimiento() {
        return MovimientoType.PEDIDO;
    }

    @Override
    public Object guardarBorrador() {
        PedidoBorradorDTO dto = new PedidoBorradorDTO();
        List<PedidoBorradorDTO.ItemPedidoPlano> planos = new ArrayList<>();

        for (itemPedido item : itemsPedido) {
            PedidoBorradorDTO.ItemPedidoPlano p = new PedidoBorradorDTO.ItemPedidoPlano();
            p.setClaveProducto(item.getClaveProducto());
            p.setProducto(item.getProducto());
            p.setDescripcion(item.getDescripcion());
            p.setCantidad(item.getCantidad());
            p.setClaveAlterna(item.getClaveAlterna());
            p.setPresentacion(item.getPresentacion());
            p.setFactor(item.getFactor());
            planos.add(p);
        }

        dto.setItems(planos);
        return dto;
    }

    @Override
    public void cargarBorrador(Object borrador) {
        if (!(borrador instanceof PedidoBorradorDTO)) return;
        PedidoBorradorDTO dto = (PedidoBorradorDTO) borrador;

        Platform.runLater(() -> {
            if (dto.getItems() != null) {
                List<itemPedido> nuevos = new ArrayList<>();
                for (PedidoBorradorDTO.ItemPedidoPlano p : dto.getItems()) {
                    itemPedido item = new itemPedido(
                            p.getClaveProducto(),
                            p.getProducto(),
                            p.getDescripcion(),
                            p.getCantidad(),
                            p.getClaveAlterna(),
                            p.getPresentacion(),
                            p.getFactor()
                    );
                    nuevos.add(item);
                }
                itemsPedido.setAll(nuevos);
            }
            // Actualizar selección todo (siempre queda en false al cargar)
            miCheckBoxSeleccionarTodo.setSelected(false);
            refrescarTabla();
        });
    }

}
