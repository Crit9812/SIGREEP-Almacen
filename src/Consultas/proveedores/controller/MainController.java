package Consultas.proveedores.controller;

import Compartido.exportar.exportador;
import Compartido.exportar.exportarPlantilla;
import Compartido.importar.importador;
import Consultas.proveedores.model.proveedores;
import Consultas.proveedores.model.model;
import Formularios.controller.controllerNuevoProveedor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import javafx.scene.input.KeyCode;

import java.io.IOException;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TextField buscador;
    @FXML private Region expansor;
    @FXML private TableView<proveedores> contenidoTabla;
    @FXML private TableColumn<proveedores, Void> colSelect;
    @FXML private TableColumn<proveedores, String> colID;
    @FXML private TableColumn<proveedores, String> colNombre;
    @FXML private TableColumn<proveedores, String> colRepresentante;
    @FXML private TableColumn<proveedores, String> colRFC;
    @FXML private TableColumn<proveedores, String> colCURP;
    @FXML private TableColumn<proveedores, String> colRazonSocial;
    @FXML private TableColumn<proveedores, String> colCorreo;
    @FXML private TableColumn<proveedores, String> colTelefono;
    @FXML private TableColumn<proveedores, String> colCP;
    @FXML private TableColumn<proveedores, String> colPais;
    @FXML private TableColumn<proveedores, String> colEstado;
    @FXML private TableColumn<proveedores, String> colCiudad;
    @FXML private TableColumn<proveedores, String> colLocalidad;
    @FXML private TableColumn<proveedores, String> colColonia;
    @FXML private TableColumn<proveedores, String> colDomicilio;
    @FXML private TableColumn<proveedores, String> colNumeroExt;
    @FXML private TableColumn<proveedores, String> colNumeroInt;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private model proveedorModel;
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();

    @FXML
    public void initialize() {
        proveedorModel = new model();
        Platform.runLater(() -> {

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(root.heightProperty().multiply(0.05));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.9));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.87));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            configurarColumnas();

            // Centrado - estructura más limpia
            TableColumn<proveedores, ?>[] columnas = new TableColumn[]{
                    colSelect, colID, colNombre, colRepresentante, colRFC, colCURP, colRazonSocial, colCorreo,
                    colTelefono, colCP, colPais, colEstado, colCiudad, colLocalidad, colColonia, colDomicilio,
                    colNumeroExt, colNumeroInt
            };
            for (TableColumn<proveedores, ?> col : columnas) {
                col.setStyle("-fx-alignment: CENTER;");
            }

            // Botón eliminar
            colSelect.setCellFactory(col -> new TableCell<proveedores, Void>() {
                private final Button btn;
                {
                    btn = new Button();
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    img.setPreserveRatio(true);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    if (soloLectura) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    } else {
                        btn.setOnAction(e -> {
                        proveedores seleccionado = getTableView().getItems().get(getIndex());
                        eliminarProveedor(seleccionado);
                    });
                    }
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            // Carga Inicial en background como productos
            cargarProveedoresEnTabla();

            // Doble clic → editar - EXACTO igual
            contenidoTabla.setRowFactory(tv -> {
                TableRow<proveedores> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        if (soloLectura) return;
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar - EXACTO igual (mantener esto)
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    if (soloLectura) return;
                    proveedores p = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (p != null) abrirFormulario(p);
                }
            });

            RefrescoHelper.setVistaActual("proveedores");
            RefrescoHelper.registrarRefresco("proveedores", this::actualizarProveedores);
            configurarAtajosTeclado();

            buscador.textProperty().addListener((observable, oldValue, newValue) -> buscarProveedores(newValue));
        });
    }

    private void actualizarProveedores() {

        // 1. Crear NUEVA instancia del modelo
        proveedorModel = new model();

        // 2. Limpiar campo de búsqueda
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            // Opcional: limpiar tabla temporalmente
            contenidoTabla.setItems(FXCollections.observableArrayList());
        });
        cargarProveedoresEnTabla();

    }

    private void cargarProveedoresEnTabla() {
        Task<ObservableList<proveedores>> task = new Task<>() {
            @Override
            protected ObservableList<proveedores> call() {
                // En productos es: productoModel.obtenerProductos()
                // Aquí es exactamente igual pero con proveedorModel
                return FXCollections.observableArrayList(proveedorModel.obtener());
            }

            @Override
            protected void succeeded() {
                ObservableList<proveedores> proveedores = getValue();
                contenidoTabla.setItems(proveedores);
            }
        };
        new Thread(task).start();
    }

    private void buscarProveedores(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProveedoresEnTabla();
        } else {
            // Si tu modelo de proveedores tiene busquedaMultiple, úsalo como en productos
            // Si no, usa buscarExacto (pero deberías agregar busquedaMultiple a proveedores también)
            ObservableList<proveedores> proveedores = FXCollections.observableArrayList(proveedorModel.buscarExacto(texto));
            contenidoTabla.setItems(proveedores);
            // ELIMINADO: El mensaje de "No se encontraron registros"
        }
    }

    private void configurarColumnas() {
        colID.setCellValueFactory(cellData -> asString(cellData.getValue().getId()));
        colNombre.setCellValueFactory(cellData -> asString(cellData.getValue().getNombre()));
        colRepresentante.setCellValueFactory(cellData -> asString(cellData.getValue().getRepresentante()));
        colRFC.setCellValueFactory(cellData -> asString(cellData.getValue().getRfc()));
        colCURP.setCellValueFactory(cellData -> asString(cellData.getValue().getCurp()));
        colRazonSocial.setCellValueFactory(cellData -> asString(cellData.getValue().getRazonSocial()));
        colCorreo.setCellValueFactory(cellData -> asString(cellData.getValue().getCorreo()));
        colTelefono.setCellValueFactory(cellData -> asString(cellData.getValue().getTelefono()));
        colCP.setCellValueFactory(cellData -> asString(cellData.getValue().getCp()));
        colPais.setCellValueFactory(cellData -> asString(cellData.getValue().getPais()));
        colEstado.setCellValueFactory(cellData -> asString(cellData.getValue().getEstado()));
        colCiudad.setCellValueFactory(cellData -> asString(cellData.getValue().getCiudad()));
        colLocalidad.setCellValueFactory(cellData -> asString(cellData.getValue().getLocalidad()));
        colColonia.setCellValueFactory(cellData -> asString(cellData.getValue().getColonia()));
        colDomicilio.setCellValueFactory(cellData -> asString(cellData.getValue().getDomicilio()));
        colNumeroExt.setCellValueFactory(cellData -> asString(cellData.getValue().getNumeroExt()));
        colNumeroInt.setCellValueFactory(cellData -> asString(cellData.getValue().getNumeroInt()));
    }

    private SimpleStringProperty asString(Object value) {
        return new SimpleStringProperty(String.valueOf(value));
    }

    @FXML
    public void formularioNuevoProveedor() {
        if (soloLectura) {
            return;
        }
        abrirFormulario(null);
    }

    private void abrirFormulario(proveedores editar) {
        try {
            // === CREAR CONTROLLER MANUALMENTE ===
            controllerNuevoProveedor controlador = new controllerNuevoProveedor();

            // === CREAR LOADER Y ASIGNAR CONTROLLER ===
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Formularios/view/nuevoProveedor.fxml")
            );
            loader.setController(controlador);

            Parent vista = loader.load();

            // === USAR EL CONTROLLER YA ASIGNADO ===
            if (editar != null) {
                controlador.cargarProveedor(editar);
            } else {
                controlador.prepararNuevoProveedor();
            }

            // === STAGE ===
            Stage stage = new Stage();
            stage.setTitle(editar == null ? "Nuevo Registro" : "Editar Registro");
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.setWidth(600);
            stage.setHeight(640);
            stage.centerOnScreen();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            stage.showAndWait();

            // === RECARGAR TABLA ===
            cargarProveedoresEnTabla();

        } catch (Exception e) {
            e.printStackTrace();
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
        dialogo.setHeaderText("Seleccione el formato para exportar:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) {
                exportador.exportarTabla(contenidoTabla, "proveedores", "pdf");
            } else if (res == btnExcel) {
                exportador.exportarTabla(contenidoTabla, "proveedores", "excel");
            }
        });
    }

    public void importarDatos() {
        if (soloLectura) {
            return;
        }
        importador.importarExcel("proveedores", "id");
        // Recargar como en productos
        cargarProveedoresEnTabla();
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("proveedores");
    }

    private void eliminarProveedor(proveedores seleccionado) {
        if (soloLectura || seleccionado == null) {
            return;
        }

        int enEntradas = proveedorModel.contarEntradasNoCanceladas(seleccionado.getId());
        int enClaves = proveedorModel.contarClavesNoDesactivadas(seleccionado.getId());
        if (enEntradas > 0 || enClaves > 0) {
            StringBuilder motivo = new StringBuilder("No se puede desactivar el proveedor porque tiene");
            if (enEntradas > 0 && enClaves <= 0) {
                motivo.append(" entradas habilitadas: (").append(enEntradas).append(")");
            }
            if (enClaves > 0 && enEntradas <= 0) {
                motivo.append(" claves habilitadas: (").append(enClaves).append(")");
            }
            if (enClaves > 0 && enEntradas > 0) {
                motivo.append(" claves habilitadas: (").append(enClaves)
                        .append(") y entradas habilitadas: (").append(enEntradas).append(")");
            }
            Alert alertaMotivo = new Alert(Alert.AlertType.WARNING);
            alertaMotivo.setTitle("No se puede desactivar");
            alertaMotivo.setHeaderText(null);
            alertaMotivo.setContentText(motivo.toString());
            alertaMotivo.getDialogPane().setPrefWidth(420);
            alertaMotivo.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alertaMotivo.showAndWait();
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea desactivar este proveedor?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (proveedorModel.eliminar(seleccionado.getId())) {
                    contenidoTabla.getItems().remove(seleccionado);
                    new Alert(Alert.AlertType.INFORMATION, "Proveedor desactivado correctamente").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo desactivar el proveedor.").showAndWait();
                }
            }
        });
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.N) formularioNuevoProveedor();
            else if (event.getCode() == KeyCode.E) {
                proveedores pSel = contenidoTabla.getSelectionModel().getSelectedItem();
                eliminarProveedor(pSel);
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
