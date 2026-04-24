package Consultas.claves.controller;

import Compartido.exportar.exportador;
import Compartido.exportar.exportarPlantilla;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import Compartido.importar.importador;
import Consultas.claves.model.model;
import Formularios.controller.controllerSincronizacionClaves;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

import java.io.IOException;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<String[]> contenidoTabla;
    @FXML private TableColumn<String[], Void> colSelect;
    @FXML private TableColumn<String[], String> colClaveProducto;
    @FXML private TableColumn<String[], String> colProducto;
    @FXML private TableColumn<String[], String> colIDProvedor;
    @FXML private TableColumn<String[], String> colProveedor;
    @FXML private TableColumn<String[], String> colClaveAlterna;
    @FXML private TableColumn<String[], String> colDescripcion;
    @FXML private TextField buscador;
    @FXML private Region expansor;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private model modeloClaves;
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();

    @FXML
    public void initialize() {
        modeloClaves = new model();
        Platform.runLater(() -> {

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(root.heightProperty().multiply(0.04));
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            // Configuración de columnas
            colClaveAlterna.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[0]));
            colClaveProducto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[1]));
            colProducto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[2]));
            colIDProvedor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[3]));
            colProveedor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[4]));
            colDescripcion.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[5]));

            TableColumn[] columnas = { colSelect, colClaveAlterna, colClaveProducto, colProducto, colIDProvedor, colProveedor, colDescripcion };
            for (TableColumn col : columnas) col.setStyle("-fx-alignment: CENTER;");

            // Botón eliminar...
            colSelect.setCellFactory(col -> new TableCell<>() {
                private final Button btn;
                {
                    btn = new Button();
                    javafx.scene.image.ImageView img = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(getClass().getResourceAsStream("/img/eliminar.png"))
                    );
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    img.setPreserveRatio(true);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    if (soloLectura) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    } else {
                        btn.setOnAction(e -> eliminarClave(getTableView().getItems().get(getIndex())));
                    }
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            // Listener de búsqueda en tiempo real
            buscador.textProperty().addListener((o, oldVal, newVal) -> {
                buscarClaves(newVal);
            });

            // Doble clic para editar
            contenidoTabla.setRowFactory(tv -> {
                TableRow<String[]> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty() && event.getButton()== MouseButton.PRIMARY && event.getClickCount() == 2) {
                        if (soloLectura) return;
                        String[] fila = row.getItem();
                        abrirFormulario(fila, true);
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if (soloLectura) return;
                    String[] filaSeleccionada = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (filaSeleccionada != null) {
                        abrirFormulario(filaSeleccionada, true);
                    }
                }
            });

            RefrescoHelper.setVistaActual("claves");
            RefrescoHelper.registrarRefresco("claves", this::actualizarClaves);
            configurarAtajosTeclado();

            cargarTabla();
        });
    }

    private void actualizarClaves() {
        modeloClaves = new model();
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            contenidoTabla.setItems(FXCollections.observableArrayList());
        });
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cargarTabla();
    }

    private void cargarTabla() {
        try {
            // Crear tarea asíncrona para cargar datos
            javafx.concurrent.Task<javafx.collections.ObservableList<String[]>> task =
                    new javafx.concurrent.Task<>() {

                        @Override
                        protected javafx.collections.ObservableList<String[]> call() {
                            return modeloClaves.obtenerParaTabla();
                        }

                        @Override
                        protected void succeeded() {
                            javafx.collections.ObservableList<String[]> datos = getValue();
                            Platform.runLater(() -> {
                                contenidoTabla.setItems(datos);
                            });
                        }

                        @Override
                        protected void failed() {
                            System.err.println("✗ Error al cargar tabla: " + getException().getMessage());
                            getException().printStackTrace();
                            Platform.runLater(() -> {
                                mostrarAlertaError("Error", "No se pudieron cargar los datos: " + getException().getMessage());
                            });
                        }
                    };

            new Thread(task).start();

        } catch (Exception e) {
            System.err.println("✗ Error en cargarTabla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirFormulario(String[] fila, boolean esEdicion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();

            if (esEdicion && fila != null) {
                ctrl.cargarParaEdicion(fila);
            }
            // Si no es edición, el controlador se inicializará para nuevo registro

            Stage stage = new Stage();
            // Cambiar título según si es edición o nuevo
            stage.setTitle(esEdicion ? "Editar Clave" : "Nueva Clave");
            stage.setScene(new Scene(vista));
            stage.setResizable(false);

            // Configurar como modal para bloquear la pantalla principal
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            // Centrar la ventana
            stage.centerOnScreen();

            stage.showAndWait();

            // Recargar la tabla después de cerrar el formulario
            cargarTabla();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlertaError("Error", "No se pudo abrir el formulario.");
        }
    }

    private void eliminarClave(String[] fila) {
        if (soloLectura) {
            return;
        }
        String idAlterno = fila[0];
        int enEntradas = modeloClaves.contarEntradasPorClave(idAlterno);
        int enSalidas = modeloClaves.contarSalidasPorClave(idAlterno);
        if (enEntradas > 0 || enSalidas > 0) {
            StringBuilder motivo = new StringBuilder(
                    "No se puede desactivar la clave porque tiene registros relacionados (activos, pendientes o disponibles):");
            if (enEntradas > 0) {
                motivo.append("\n- Entradas: ").append(enEntradas);
            }
            if (enSalidas > 0) {
                motivo.append("\n- Salidas: ").append(enSalidas);
            }
            mostrarAlertaWarning("Advertencia", motivo.toString());
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea desactivar esta clave?");

        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (modeloClaves.eliminar(idAlterno)) {
                    contenidoTabla.getItems().remove(fila);
                    mostrarAlertaInfo("Éxito", "Clave desactivada correctamente.");
                } else {
                    mostrarAlertaError("Error", "No se pudo desactivar la clave.");
                }
            }
        });
    }

    private void buscarClaves(String textoBusqueda) {
        String texto = textoBusqueda.trim();

        if (texto.isEmpty()) {
            cargarTabla();
        } else {
            contenidoTabla.setItems(modeloClaves.buscarEnTabla(texto));
        }
    }

    @FXML public void formularioNuevaSincronizacionClaves() {
        if (soloLectura) {
            return;
        }
        abrirFormulario(null, false);
    }

    @FXML private void exportarDatos() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAlertaWarning("Advertencia", "No hay datos para exportar.");
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
            if (res == btnPDF)
                exportador.exportarTabla(contenidoTabla, "Claves", "pdf");
            else if (res == btnExcel)
                exportador.exportarTabla(contenidoTabla, "Claves", "excel");
        });
    }

    @FXML private void importarDatos() {
        if (soloLectura) {
            return;
        }
        importador.importarClavesExcel();
        cargarTabla();
    }

    @FXML public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("claves");
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarAlertaInfo(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarAlertaWarning(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        Label contenido = new Label(mensaje);
        contenido.setWrapText(true);
        alerta.getDialogPane().setContent(contenido);
        alerta.showAndWait();
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.N) formularioNuevaSincronizacionClaves();
            else if (event.getCode() == KeyCode.E) {
                String[] fila = contenidoTabla.getSelectionModel().getSelectedItem();
                if (fila != null) eliminarClave(fila);
            } else if (event.getCode() == KeyCode.I) importarDatos();
            else if (event.getCode() == KeyCode.R) exportarDatos();
            else if (event.getCode() == KeyCode.D) exportarPlantilla();
            else return;
            event.consume();
        });
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}
