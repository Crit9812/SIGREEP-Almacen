package Consultas.sucursales.controller;

import Compartido.exportar.exportador;
import Compartido.exportar.exportarPlantilla;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import Compartido.importar.importador;
import Consultas.sucursales.model.sucursal;
import Consultas.sucursales.model.model;
import Formularios.controller.controllerNuevaSucursal;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private Region expansor;
    @FXML private TextField buscador;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<sucursal> contenidoTabla;
    @FXML private TableColumn<sucursal, Void> colSelect;
    @FXML private TableColumn<sucursal, String> colId;
    @FXML private TableColumn<sucursal, String> colNombre;
    @FXML private TableColumn<sucursal, String> colCorreo;
    @FXML private TableColumn<sucursal, String> colTelefono;
    @FXML private TableColumn<sucursal, String> colDomicilio;
    @FXML private TableColumn<sucursal, String> colCP;
    @FXML private TableColumn<sucursal, String> colColonia;
    @FXML private TableColumn<sucursal, String> colNumeroExt;
    @FXML private TableColumn<sucursal, String> colNumeroInt;
    @FXML private TableColumn<sucursal, String> colCiudad;
    @FXML private TableColumn<sucursal, String> colEstado;
    @FXML private TableColumn<sucursal, String> colLocalidad;
    @FXML private TableColumn<sucursal, String> colPais;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();
    private  model sucursalModel;

    @FXML
    public void initialize() {
        sucursalModel = new model();
        Platform.runLater(() -> {

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(root.heightProperty().multiply(0.04));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            // CONFIGURACIÓN DE COLUMNAS (cell value factories)
            colId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getId())));
            colNombre.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNombre()));
            colDomicilio.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDomicilio()));
            colCP.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getCp())));
            colColonia.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getColonia()));
            colNumeroExt.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getNumeroExt())));
            colNumeroInt.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getNumeroInt())));
            colCiudad.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCiudad()));
            colEstado.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getEstado()));
            colLocalidad.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getLocalidad()));
            colPais.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPais()));
            colCorreo.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCorreo()));
            colTelefono.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getTelefono())));

            // ALINEACIÓN DE COLUMNAS
            TableColumn<sucursal, String>[] columnas = new TableColumn[]{
                    colSelect, colId, colNombre, colDomicilio, colCP, colColonia, colNumeroExt,
                    colNumeroInt, colCiudad, colEstado, colLocalidad, colPais, colCorreo, colTelefono
            };
            for (TableColumn<sucursal, String> col : columnas) {
                col.setStyle("-fx-alignment: CENTER;");
            }
            // BOTÓN ELIMINAR
            colSelect.setCellFactory(col -> new TableCell<sucursal, Void>() {
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
                            sucursal seleccionado = getTableView().getItems().get(getIndex());
                            eliminarSucursal(seleccionado);
                        });
                    }
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
            cargarSucursalesEnTabla();

            // Doble clic → abrir edición
            contenidoTabla.setRowFactory(tv -> {
                TableRow<sucursal> row = new TableRow<>();
                row.setOnMouseClicked(evt -> {
                    if (evt.getClickCount() == 2 && !row.isEmpty()) {
                        if (soloLectura) return;
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → abrir edición
            contenidoTabla.setOnKeyPressed(evt -> {
                if (evt.getCode().toString().equals("ENTER")) {
                    if (soloLectura) return;
                    sucursal sel = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (sel != null) abrirFormulario(sel);
                }
            });

            // Configurar listener para el buscador
            buscador.textProperty().addListener((observable, oldValue, newValue) -> {
                buscarSucursales(newValue);
            });
        });

        RefrescoHelper.setVistaActual("sucursales");
        RefrescoHelper.registrarRefresco("sucursales", this::actualizarSucursales);
        configurarAtajosTeclado();
    }

    private void actualizarSucursales() {
        sucursalModel = new model();
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            contenidoTabla.setItems(FXCollections.observableArrayList());
        });
        cargarSucursalesEnTabla();
    }

    private void cargarSucursalesEnTabla() {
        Task<ObservableList<sucursal>> task = new Task<>() {
            @Override
            protected ObservableList<sucursal> call() {
                return FXCollections.observableArrayList(sucursalModel.obtenerSucursales());
            }
            @Override
            protected void succeeded() {
                ObservableList<sucursal> sucursales = getValue();
                contenidoTabla.setItems(sucursales);
            }
        };
        new Thread(task).start();
    }

    private void buscarSucursales(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarSucursalesEnTabla();
        } else {
            ObservableList<sucursal> sucursales = FXCollections.observableArrayList(sucursalModel.buscarExacto(texto));
            contenidoTabla.setItems(sucursales);
        }
    }

    @FXML public void formularioNuevaSucursal() { if (soloLectura) { return; } abrirFormulario(null); }

    private void abrirFormulario(sucursal sucursalEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevaSucursal.fxml"));
            Parent view = loader.load();

            controllerNuevaSucursal ctrl = loader.getController();

            // Preparar stage modal
            Stage stage = new Stage();
            stage.setScene(new Scene(view));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            if (sucursalEditar == null) {
                // modo nuevo
                ctrl.prepararNuevaSucursal();
                stage.setTitle("Nueva Sucursal");
            } else {
                // modo edición
                ctrl.cargarSucursal(sucursalEditar);
                stage.setTitle("Editar Sucursal");
            }

            stage.showAndWait();
            cargarSucursalesEnTabla();

        } catch (Exception ex) {
            ex.printStackTrace();
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
                exportador.exportarTabla(contenidoTabla, "Sucursales", "pdf");
            } else if (res == btnExcel) {
                exportador.exportarTabla(contenidoTabla, "Sucursales", "excel");
            }
        });
    }

    public void importarDatos() {
        if (soloLectura) {
            return;
        }
        importador.importarExcel("sucursales", "id");
        cargarSucursalesEnTabla();
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("sucursales");
    }

    private void eliminarSucursal(sucursal seleccionado) {
        if (soloLectura || seleccionado == null) {
            return;
        }

        int enEntradas = sucursalModel.contarEntradasPorSucursal(seleccionado.getId());
        int enSalidas = sucursalModel.contarSalidasPorSucursal(seleccionado.getId());
        if (enEntradas > 0 || enSalidas > 0) {
            StringBuilder motivo = new StringBuilder(
                    "No se puede desactivar la sucursal porque tiene registros relacionados (activos, pendientes o disponibles):");
            if (enEntradas > 0) {
                motivo.append("\n- Entradas: ").append(enEntradas);
            }
            if (enSalidas > 0) {
                motivo.append("\n- Salidas: ").append(enSalidas);
            }
            Alert alertaAdvertencia = new Alert(Alert.AlertType.WARNING);
            alertaAdvertencia.setTitle("Advertencia");
            alertaAdvertencia.setHeaderText(null);
            Label contenido = new Label(motivo.toString());
            contenido.setWrapText(true);
            alertaAdvertencia.getDialogPane().setContent(contenido);
            alertaAdvertencia.showAndWait();
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea desactivar esta sucursal?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (sucursalModel.eliminarSucursal(seleccionado.getId())) {
                    contenidoTabla.getItems().remove(seleccionado);
                    new Alert(Alert.AlertType.INFORMATION, "Sucursal desactivada correctamente").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo desactivar la sucursal").showAndWait();
                }
            }
        });
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.N) formularioNuevaSucursal();
            else if (event.getCode() == KeyCode.E) {
                sucursal c = contenidoTabla.getSelectionModel().getSelectedItem();
                eliminarSucursal(c);
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
