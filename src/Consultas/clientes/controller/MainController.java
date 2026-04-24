package Consultas.clientes.controller;

import Compartido.exportar.exportarPlantilla;
import Compartido.importar.importador;
import Consultas.clientes.model.cliente;
import Consultas.clientes.model.model;
import Formularios.controller.controllerNuevoCliente;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import Compartido.exportar.exportador;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import javafx.stage.Modality;
import javafx.stage.Stage;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;


import java.io.IOException;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private Region expansor;
    @FXML private TextField buscador;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<cliente> contenidoTabla;
    @FXML private TableColumn<cliente, Void> colSelect;
    @FXML private TableColumn<cliente, String> colID;
    @FXML private TableColumn<cliente, String> colNombre;
    @FXML private TableColumn<cliente, String> colRFC;
    @FXML private TableColumn<cliente, String> colCURP;
    @FXML private TableColumn<cliente, String> colRazonSocial;
    @FXML private TableColumn<cliente, String> colCorreo;
    @FXML private TableColumn<cliente, String> colTelefono;
    @FXML private TableColumn<cliente, String> colCP;
    @FXML private TableColumn<cliente, String> colPais;
    @FXML private TableColumn<cliente, String> colEstado;
    @FXML private TableColumn<cliente, String> colCiudad;
    @FXML private TableColumn<cliente, String> colLocalidad;
    @FXML private TableColumn<cliente, String> colColonia;
    @FXML private TableColumn<cliente, String> colDomicilio;
    @FXML private TableColumn<cliente, String> colNumeroExt;
    @FXML private TableColumn<cliente, String> colNumeroInt;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private model clienteModel;
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();

    @FXML
    public void initialize() {
        clienteModel = new model();
        Platform.runLater(() -> {

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(root.heightProperty().multiply(0.04));

            // Cell Value Factories - EXACTO igual estructura
            colID.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getId())));
            colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
            colRFC.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRfc()));
            colCURP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCurp()));
            colRazonSocial.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRazonSocial()));
            colCorreo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCorreo()));
            colTelefono.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getTelefono())));
            colCP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getCp())));
            colPais.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPais()));
            colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado()));
            colCiudad.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCiudad()));
            colLocalidad.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocalidad()));
            colColonia.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getColonia()));
            colDomicilio.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDomicilio()));
            colNumeroExt.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getNumeroExt())));
            colNumeroInt.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getNumeroInt())));

            // Centrado - EXACTO igual
            TableColumn<cliente, String>[] columnas = new TableColumn[]{
                    colSelect, colID,colNombre,colRFC,colCURP,colRazonSocial,colCorreo,colTelefono,colCP,
                    colPais,colEstado,colCiudad,colLocalidad,colColonia,colDomicilio,colNumeroExt,colNumeroInt
            };
            for (TableColumn<cliente, String> col : columnas) col.setStyle("-fx-alignment: CENTER;");

            // Botón eliminar - EXACTA misma estructura que productos
            colSelect.setCellFactory(col -> new TableCell<cliente, Void>() {
                private final Button btn;
                {
                    btn = new Button();
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    if (soloLectura) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    } else {
                        btn.setOnAction(e -> {
                        cliente seleccionado = getTableView().getItems().get(getIndex());
                        eliminarCliente(seleccionado);
                    });
                    }
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            contenidoTabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                // Puedes agregar algo aquí si necesitas, como en productos con imágenes
            });

            // Doble clic → editar - EXACTO igual
            contenidoTabla.setRowFactory(tv -> {
                TableRow<cliente> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        if (soloLectura) return;
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar - EXACTO igual
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    if (soloLectura) return;
                    cliente c = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (c != null) abrirFormulario(c);
                }
            });

            // Configurar listener para el buscador - EXACTAMENTE IGUAL que productos
            buscador.textProperty().addListener((observable, oldValue, newValue) -> {
                buscarClientes(newValue);
            });

            RefrescoHelper.setVistaActual("clientes");
            RefrescoHelper.registrarRefresco("clientes", this::actualizarClientes);
            configurarAtajosTeclado();

            // Carga Inicial en background como productos
            cargarClientesEnTabla();
        });
    }

    // ========== NUEVO MÉTODO DE ACTUALIZACIÓN ==========
    private void actualizarClientes() {
        // 1. Crear NUEVA instancia del modelo
        clienteModel = new model();

        // 2. Limpiar UI
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            contenidoTabla.setItems(FXCollections.observableArrayList());
            System.out.println("✓ UI limpiada");
        });

        // 3. Recargar datos
        cargarClientesEnTabla();

        System.out.println("========================================");
        System.out.println("ACTUALIZACIÓN DE CLIENTES COMPLETADA");
        System.out.println("========================================");
    }

    // MetODO EXACTAMENTE IGUAL que cargarProductosEnTabla() en productos
    private void cargarClientesEnTabla() {
        Task<ObservableList<cliente>> task = new Task<>() {
            @Override
            protected ObservableList<cliente> call() {
                return FXCollections.observableArrayList(clienteModel.obtenerClientes());
            }

            @Override
            protected void succeeded() {
                ObservableList<cliente> clientes = getValue();
                contenidoTabla.setItems(clientes);
            }
        };
        new Thread(task).start();
    }

    // MeTODO EXACTAMENTE IGUAL que buscarProductos() en productos
    private void buscarClientes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarClientesEnTabla();
        } else {
            // Si tu modelo de clientes tiene busquedaMultiple, úsalo como en productos
            // Si no, usa buscarExacto (pero deberías agregar busquedaMultiple a clientes también)
            ObservableList<cliente> clientes = FXCollections.observableArrayList(clienteModel.buscarExacto(texto));
            contenidoTabla.setItems(clientes);
        }
    }

    @FXML
    public void formularioNuevoCliente() {
        if (soloLectura) {
            return;
        }
        abrirFormulario(null);
    }

    private void abrirFormulario(cliente clienteEditar) {
        try {
            // 1. Cargar FXML con controlador manual
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoCliente.fxml"));

            // 2. Crear controlador manualmente
            controllerNuevoCliente ctrl = new controllerNuevoCliente();
            loader.setController(ctrl);

            // 3. Cargar el FXML - esto llama al initialize() del controlador
            Parent formularioRoot = loader.load();

            // 4. AHORA configurar el cliente (después de initialize())
            if (clienteEditar != null) {
                ctrl.cargarCliente(clienteEditar);
            } else {
                // Si tienes el método prepararNuevoCliente, llamarlo
                try {
                    ctrl.prepararNuevoCliente();
                } catch (Exception e) {
                    // Si no existe el método, no pasa nada
                }
            }

            // 5. Configurar callback para guardar
            ctrl.setOnSaved(() -> {
                cargarClientesEnTabla();
            });

            // 6. Mostrar ventana
            String titulo = clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente";
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(formularioRoot));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);

            stage.setOnHidden(e -> cargarClientesEnTabla());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void exportarDatos() {
        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Exportar");
        dialogo.setHeaderText("Seleccione el formato:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) exportador.exportarTabla(contenidoTabla, "Clientes", "pdf");
            else if (res == btnExcel) exportador.exportarTabla(contenidoTabla, "Clientes", "excel");
        });
    }

    public void importarDatos() {
        if (soloLectura) {
            return;
        }
        importador.importarExcel("clientes", "id");
        // Recargar como en productos
        cargarClientesEnTabla();
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("clientes");
    }

    private void eliminarCliente(cliente seleccionado) {
        if (soloLectura || seleccionado == null) {
            return;
        }

        int salidasNoCanceladas = clienteModel.contarSalidasNoCanceladasPorCliente(seleccionado.getId());
        if (salidasNoCanceladas > 0) {
            String mensaje = "No se puede eliminar el cliente porque tiene"
                    + " salidas habilitadas: (" + salidasNoCanceladas + ")";
            Alert alertaAdvertencia = new Alert(Alert.AlertType.WARNING);
            alertaAdvertencia.setTitle("Advertencia");
            alertaAdvertencia.setHeaderText(null);
            Label contenido = new Label(mensaje);
            contenido.setWrapText(true);
            alertaAdvertencia.getDialogPane().setContent(contenido);
            alertaAdvertencia.showAndWait();
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea eliminar este cliente?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (clienteModel.eliminarCliente(seleccionado.getId())) {
                    contenidoTabla.getItems().remove(seleccionado);
                    new Alert(Alert.AlertType.INFORMATION, "Cliente eliminado correctamente").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el cliente.").showAndWait();
                }
            }
        });
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.N) formularioNuevoCliente();
            else if (event.getCode() == KeyCode.E) {
                cliente c = contenidoTabla.getSelectionModel().getSelectedItem();
                eliminarCliente(c);
            } else if (event.getCode() == KeyCode.I) importarDatos();
            else if (event.getCode() == KeyCode.R) exportarDatos();
            else if (event.getCode() == KeyCode.D) exportarPlantilla();
            else return;
            event.consume();
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

}
