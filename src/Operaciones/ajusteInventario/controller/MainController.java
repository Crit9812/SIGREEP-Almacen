package Operaciones.ajusteInventario.controller;

import Operaciones.compra.model.UbicacionCompra;
import VentanaPrincipal.controller.Pausable;
import VentanaPrincipal.controller.MovimientoType;
import Compartido.helper.BorradorService;
import Operaciones.ajusteInventario.model.AjusteBorradorDTO;
import Compartido.exportar.ReporteAjusteExporter;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.OverlayCarga;
import Compartido.helper.AtajosTecladoHelper;
import javafx.concurrent.Task;
import Operaciones.ajusteInventario.model.model;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista, Pausable {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TableView<Object> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;
    @FXML private TextField totalAjuste;
    @FXML private TextField comentario;
    @FXML private CheckBox miCheckBox;
    @FXML private TableColumn<Object, Boolean> colSelect;
    @FXML private TableColumn<Object, String> colTipo;
    @FXML private TableColumn<Object, String> colClaveProduct;
    @FXML private TableColumn<Object, String> colProducto;
    @FXML private TableColumn<Object, String> colDescripcionProducto;
    @FXML private TableColumn<Object, Number> colCantidad;
    @FXML private TableColumn<Object, String> colLote;
    @FXML private TableColumn<Object, String> colCaducidad;
    @FXML private TableColumn<Object, String> colUbicacion;
    @FXML private TableColumn<Object, String> colNota;
    @FXML private TableColumn<Object, String> colPrecioUnitario;
    @FXML private TableColumn<Object, String> colPrecioIva;
    @FXML private TableColumn<Object, String> colPrecioBruto;
    @FXML private TableColumn<Object, String> colPrecioTotaal;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final ObservableList<compra> itemsEntrada = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsSalida = FXCollections.observableArrayList();
    private final ObservableList<Object> itemsAjuste = FXCollections.observableArrayList();
    private boolean actualizandoSeleccionTodo = false;
    private final model ajusteModel = new model();
    private OverlayCarga overlayCarga;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {

            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.77));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            // Comentario
            if (contenedorComentario != null) {
                contenedorComentario.maxWidthProperty().bind(contenedor.widthProperty());
            }
            if (comentario != null) {
                HBox.setHgrow(comentario, Priority.ALWAYS);
                comentario.setMaxWidth(Double.MAX_VALUE);
            }

            // Botón confirmar
            contenedorBtnConfirmar.setMinWidth(Region.USE_PREF_SIZE);
            contenedorBtnConfirmar.setMaxWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(contenedorBtnConfirmar, Priority.NEVER);

            overlayCarga = new OverlayCarga(root, new Pane());

        });

        configurarTabla();
        configurarListeners();
        configurarSeleccionTodo();
        configurarTotalAjuste();
        configurarAtajosTeclado();

        RefrescoHelper.setVistaActual("ajusteInventario");
        RefrescoHelper.registrarRefresco("ajusteInventario", this::actualizarAjusteInventario);
    }

    private void actualizarAjusteInventario() {

        // 1. Limpiar todas las listas
        Platform.runLater(() -> {
            itemsEntrada.clear();
            itemsSalida.clear();
            itemsAjuste.clear();

            if (comentario != null) {
                comentario.clear();
            }

            if (miCheckBox != null) {
                miCheckBox.setSelected(false);
            }

            if (totalAjuste != null) {
                totalAjuste.setText("0.00");
                totalAjuste.setStyle("");
            }

            contenidoTabla.refresh();
        });

    }

    private void abrirFormularioEdicion(Object item) {
        try {
            if (item instanceof compra) {
                // Para items de entrada
                abrirFormularioEdicionEntrada((compra) item);
            } else if (item instanceof traspasoSalida) {
                // Para items de salida
                abrirFormularioEdicionSalida((traspasoSalida) item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de edición.");
        }
    }

    private void abrirFormularioEdicionEntrada(compra itemParaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
            controlador.setItemsCompra(itemsEntrada);
            controlador.setTituloFormulario("Editar Entrada");
            controlador.setModoAjusteInventario(true);
            controlador.setItemParaEditar(itemParaEditar); // Esto es importante para modo edición
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Editar Entrada");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de edición.");
        }
    }

    private void abrirFormularioEdicionSalida(traspasoSalida itemParaEditar) {
        try {
            Formularios.controller.controllerNuevaVenta controlador = new Formularios.controller.controllerNuevaVenta();
            controlador.setItemsVenta(itemsSalida);
            controlador.setTituloFormulario("Editar Salida");
            controlador.setModoSoloNormal(true);
            controlador.setModoAjusteInventario(true);
            controlador.setItemParaEditar(itemParaEditar); // Asegúrate de que este método exista en controllerNuevaVenta

            // Llamar al formulario de nueva venta en modo edición
            controllerFormularios.controllerFormulario.llamarFormulario(
                    "/Formularios/view/nuevaVenta.fxml",
                    controlador,
                    "Editar Salida"
            );
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de edición.");
        }
    }

    private void configurarTabla() {
        contenidoTabla.setItems(itemsAjuste);
        contenidoTabla.setEditable(true);

        // Configurar columna de selección (CheckBox)
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Object, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<Object, Boolean> param) {
                Object item = param.getValue();
                if (item instanceof compra) {
                    return ((compra) item).seleccionadoProperty();
                }
                if (item instanceof traspasoSalida) {
                    return ((traspasoSalida) item).seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        // Configurar columna Tipo
        colTipo.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return new SimpleStringProperty("Entrada");
            }
            if (item instanceof traspasoSalida) {
                return new SimpleStringProperty("Salida");
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Clave Producto
        colClaveProduct.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).claveProductoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).claveProductoProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Producto
        colProducto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).productoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).productoProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Descripción Producto
        colDescripcionProducto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).descripcionProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).descripcionProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Cantidad
        colCantidad.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).cantidadProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).cantidadProperty();
            }
            return new SimpleIntegerProperty(0);
        });

        // Configurar columna Lote
        colLote.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).loteProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).loteProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Caducidad
        colCaducidad.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).caducidadProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).caducidadProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Ubicación
        colUbicacion.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).ubicacionResumenProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).ubicacionResumenProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Nota
        colNota.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).notaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).notaProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Precio Unitario
        colPrecioUnitario.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioEntradaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioEntradaProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Precio IVA
        colPrecioIva.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioIvaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioIvaProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Precio Bruto
        colPrecioBruto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioBrutoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioBrutoProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar columna Precio Total
        colPrecioTotaal.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioTotalProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioTotalProperty();
            }
            return new SimpleStringProperty("");
        });

        // Configurar alineación central para todas las columnas
        TableColumn<Object, ?>[] columnas = new TableColumn[]{
                colSelect, colTipo, colClaveProduct, colProducto, colDescripcionProducto,
                colCantidad, colLote, colCaducidad, colUbicacion, colNota,
                colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<Object, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setRowFactory(table -> {
            TableRow<Object> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Object item = row.getItem();
                    abrirFormularioEdicion(item);
                }
            });
            return row;
        });
    }

    private void configurarListeners() {
        itemsEntrada.addListener((ListChangeListener<compra>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (compra item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalAjuste());
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionTodo());
                    }
                }
            }
            refrescarTabla();
        });

        itemsSalida.addListener((ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalAjuste());
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionTodo());
                    }
                }
            }
            refrescarTabla();
        });
    }

    private void configurarSeleccionTodo() {
        if (miCheckBox == null) {
            return;
        }
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizandoSeleccionTodo) {
                return;
            }
            for (compra item : itemsEntrada) {
                item.setSeleccionado(newVal);
            }
            for (traspasoSalida item : itemsSalida) {
                item.setSeleccionado(newVal);
            }
            contenidoTabla.refresh();
        });
        actualizarSeleccionTodo();
    }

    public void refrescarTabla() {
        itemsAjuste.setAll(itemsEntrada);
        itemsAjuste.addAll(itemsSalida);
        contenidoTabla.refresh();
        actualizarTotalAjuste();
    }

    private void configurarTotalAjuste() {
        if (totalAjuste != null) {
            totalAjuste.setEditable(false);
            totalAjuste.setText("0.00");
            totalAjuste.setStyle("");
        }
        actualizarTotalAjuste();
    }

    private void actualizarTotalAjuste() {
        if (totalAjuste == null) {
            return;
        }
        BigDecimal totalEntradas = BigDecimal.ZERO;
        for (compra item : itemsEntrada) {
            totalEntradas = totalEntradas.add(parseDecimal(item.getPrecioTotal()));
        }
        BigDecimal totalSalidas = BigDecimal.ZERO;
        for (traspasoSalida item : itemsSalida) {
            totalSalidas = totalSalidas.add(parseDecimal(item.getPrecioTotal()));
        }
        BigDecimal total = totalEntradas.subtract(totalSalidas);
        totalAjuste.setText(total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            totalAjuste.setStyle("-fx-text-fill: #d32f2f;");
        } else {
            totalAjuste.setStyle("");
        }
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        String limpio = valor.replace(",", "").trim();
        if (limpio.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @FXML
    public void abrirFormularioAgregar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
            controlador.setItemsCompra(itemsEntrada);
            controlador.setTituloFormulario("Agregar");
            controlador.setModoAjusteInventario(true);
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Agregar");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de agregar.");
        }
    }

    @FXML
    public void abrirFormularioQuitar() {
        Formularios.controller.controllerNuevaVenta controlador = new Formularios.controller.controllerNuevaVenta();
        controlador.setItemsVenta(itemsSalida);
        controlador.setTituloFormulario("Quitar");
        controlador.setModoSoloNormal(true);
        controlador.setModoAjusteInventario(true);
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevaVenta.fxml", controlador, "Quitar");
    }

    @FXML
    public void eliminarSeleccionados() {
        if (itemsEntrada.isEmpty() && itemsSalida.isEmpty()) {
            mostrarAlerta("Advertencia", "No hay registros para eliminar.");
            return;
        }

        boolean algunSeleccionado = itemsEntrada.stream().anyMatch(compra::isSeleccionado)
                || itemsSalida.stream().anyMatch(traspasoSalida::isSeleccionado);
        if (!algunSeleccionado) {
            mostrarAlerta("Advertencia", "Seleccione al menos una fila para eliminar.");
            return;
        }

        if (!confirmarEliminacion()) {
            return;
        }

        itemsEntrada.removeIf(compra::isSeleccionado);
        itemsSalida.removeIf(traspasoSalida::isSeleccionado);
        refrescarTabla();
        actualizarSeleccionTodo();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    public void guardarAjuste() {
        if (itemsEntrada.isEmpty() && itemsSalida.isEmpty()) {
            mostrarAlerta("Advertencia", "No hay ajustes para registrar.");
            return;
        }

        String comentarioTexto = comentario != null ? comentario.getText().trim() : "";
        List<Object> copiaItems = new ArrayList<>(itemsAjuste);
        List<compra> itemsEntradaSnapshot = new ArrayList<>(itemsEntrada);
        List<traspasoSalida> itemsSalidaSnapshot = new ArrayList<>(itemsSalida);

        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ajusteModel.registrarAjuste(itemsEntradaSnapshot, itemsSalidaSnapshot, comentarioTexto);
            }
        };

        task.setOnSucceeded(e -> {
            String idAjuste = task.getValue();
            if (idAjuste != null && !idAjuste.isEmpty()) {
                BorradorService.getInstance().eliminar(MovimientoType.AJUSTE);
                String claveAjuste = "" + idAjuste;

                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }

                // 3. Mostrar diálogo de confirmación y exportación
                mostrarConfirmacionReporte(claveAjuste, comentarioTexto, copiaItems);

                // 4. Limpiar la interfaz SOLO si se registró exitosamente
                itemsEntrada.clear();
                itemsSalida.clear();
                itemsAjuste.clear();
                if (comentario != null) {
                    comentario.clear();
                }
                actualizarTotalAjuste();
                actualizarSeleccionTodo();

            } else {
                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }
                mostrarAlerta("Error", "No se pudo registrar el ajuste.");
            }
        });

        task.setOnFailed(e -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            mostrarAlerta("Error", "No se pudo registrar el ajuste.");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarConfirmacionReporte(String claveAjuste, String comentario, List<Object> itemsAjuste) {
        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Registro exitoso");
        dialogo.setHeaderText("Ajuste de inventario registrado correctamente");
        dialogo.setContentText("ID del ajuste: " + claveAjuste + "\n\n¿Deseas descargar el reporte de este ajuste?");

        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                ReporteAjusteExporter.exportarReporteAjuste(
                        claveAjuste,
                        comentario,
                        new ArrayList<>(itemsAjuste),
                        contenidoTabla != null && contenidoTabla.getScene() != null
                                ? contenidoTabla.getScene().getWindow()
                                : null
                );
            }
        });
    }
    private boolean confirmarEliminacion() {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro de borrar los elementos seleccionados?");

        ButtonType botonAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alerta.getButtonTypes().setAll(botonAceptar, botonCancelar);

        return alerta.showAndWait().orElse(botonCancelar) == botonAceptar;
    }

    private void actualizarSeleccionTodo() {
        if (miCheckBox == null) {
            return;
        }
        try {
            actualizandoSeleccionTodo = true;
            boolean hayItems = !itemsEntrada.isEmpty() || !itemsSalida.isEmpty();
            boolean seleccionado = hayItems
                    && itemsEntrada.stream().allMatch(compra::isSeleccionado)
                    && itemsSalida.stream().allMatch(traspasoSalida::isSeleccionado);
            miCheckBox.setSelected(seleccionado);
        } finally {
            actualizandoSeleccionTodo = false;
        }
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
            else if (event.getCode() == KeyCode.N) abrirFormularioAgregar();
            else if (event.getCode() == KeyCode.G) guardarAjuste();
            else if (event.getCode() == KeyCode.Q) abrirFormularioQuitar();
            else return;
            event.consume();
        });
    }

    @Override
    public MovimientoType getTipoMovimiento() {
        return MovimientoType.AJUSTE;
    }

    @Override
    public Object guardarBorrador() {
        AjusteBorradorDTO dto = new AjusteBorradorDTO();
        dto.setComentario(comentario != null ? comentario.getText() : "");

        // Convertir entradas (compra)
        List<AjusteBorradorDTO.ItemEntradaPlano> entradasPlano = new ArrayList<>();
        for (compra item : itemsEntrada) {
            AjusteBorradorDTO.ItemEntradaPlano p = new AjusteBorradorDTO.ItemEntradaPlano();
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
            /*p.setProveedorId(item.getProveedorId());
            p.setProveedorNombre(item.getProveedorNombre());*/

            // Ubicaciones
            List<AjusteBorradorDTO.UbicacionPlano> ubicPlano = new ArrayList<>();
            for (UbicacionCompra u : item.getUbicaciones()) {
                ubicPlano.add(new AjusteBorradorDTO.UbicacionPlano(u.getUbicacion(), u.getCantidad()));
            }
            p.setUbicaciones(ubicPlano);

            entradasPlano.add(p);
        }
        dto.setEntradas(entradasPlano);

        // Convertir salidas (traspasoSalida)
        List<AjusteBorradorDTO.ItemSalidaPlano> salidasPlano = new ArrayList<>();
        for (traspasoSalida item : itemsSalida) {
            AjusteBorradorDTO.ItemSalidaPlano p = new AjusteBorradorDTO.ItemSalidaPlano();
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
            List<AjusteBorradorDTO.UbicacionPlano> ubicPlano = new ArrayList<>();
            for (UbicacionCompra u : item.getUbicaciones()) {
                ubicPlano.add(new AjusteBorradorDTO.UbicacionPlano(u.getUbicacion(), u.getCantidad()));
            }
            p.setUbicaciones(ubicPlano);

            salidasPlano.add(p);
        }
        dto.setSalidas(salidasPlano);

        return dto;
    }

    @Override
    public void cargarBorrador(Object borrador) {
        if (!(borrador instanceof AjusteBorradorDTO)) return;
        AjusteBorradorDTO dto = (AjusteBorradorDTO) borrador;

        Platform.runLater(() -> {
            // Comentario
            if (comentario != null) {
                comentario.setText(dto.getComentario() != null ? dto.getComentario() : "");
            }

            // Limpiar listas actuales
            itemsEntrada.clear();
            itemsSalida.clear();

            // Restaurar entradas
            if (dto.getEntradas() != null) {
                for (AjusteBorradorDTO.ItemEntradaPlano p : dto.getEntradas()) {
                    compra item = new compra(); // constructor vacío

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
                    /*item.setProveedorId(p.getProveedorId());
                    item.setProveedorNombre(p.getProveedorNombre());*/

                    // Restaurar ubicaciones
                    if (p.getUbicaciones() != null) {
                        List<UbicacionCompra> ubicaciones = new ArrayList<>();
                        for (AjusteBorradorDTO.UbicacionPlano up : p.getUbicaciones()) {
                            UbicacionCompra u = new UbicacionCompra(up.getUbicacion(), up.getCantidad());
                            ubicaciones.add(u);
                        }
                        item.setUbicaciones(ubicaciones);
                    }

                    itemsEntrada.add(item);
                }
            }

            // Restaurar salidas
            if (dto.getSalidas() != null) {
                for (AjusteBorradorDTO.ItemSalidaPlano p : dto.getSalidas()) {
                    traspasoSalida item = new traspasoSalida(); // constructor vacío

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
                        for (AjusteBorradorDTO.UbicacionPlano up : p.getUbicaciones()) {
                            UbicacionCompra u = new UbicacionCompra(up.getUbicacion(), up.getCantidad());
                            ubicaciones.add(u);
                        }
                        item.setUbicaciones(ubicaciones);
                    }

                    itemsSalida.add(item);
                }
            }

            // Refrescar la tabla unificada
            refrescarTabla();
            actualizarSeleccionTodo();
            actualizarTotalAjuste();
        });
    }
}
