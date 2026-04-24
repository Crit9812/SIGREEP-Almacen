package Operaciones.registrarUsuario.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.helper.OverlayCarga;
import Operaciones.registrarUsuario.model.usuario;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.application.Platform;
import Operaciones.registrarUsuario.model.model;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;
    @FXML private TableColumn<usuario, Void> colSelect;
    @FXML private TableColumn<usuario, String> colClaveUsuario;
    @FXML private TableColumn<usuario, String> colNombre;
    @FXML private TableColumn<usuario, String> colApellidoP;
    @FXML private TableColumn<usuario, String> colApellidoM;
    @FXML private TableColumn<usuario, String> colNombreUsuario;
    @FXML private TableColumn<usuario, String> colRol;
    @FXML private TableView<usuario> contenidoTabla;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private OverlayCarga overlayCarga;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            overlayCarga = new OverlayCarga(root, new Pane());

            colClaveUsuario.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIdUsuario()));
            colNombre.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreUsuario()));
            colApellidoP.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellidoPUsuario()));
            colApellidoM.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellidoMUsuario()));
            colNombreUsuario.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUserName()));
            colRol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRolUsuario()));

            colClaveUsuario.setStyle("-fx-alignment: CENTER;");
            colNombre.setStyle("-fx-alignment: CENTER;");
            colApellidoP.setStyle("-fx-alignment: CENTER;");
            colApellidoM.setStyle("-fx-alignment: CENTER;");
            colNombreUsuario.setStyle("-fx-alignment: CENTER;");
            colRol.setStyle("-fx-alignment: CENTER;");

            colSelect.setCellFactory(col -> new TableCell<usuario, Void>() {
                private final Button btn = new Button();
                private final HBox contenedor = new HBox();

                {
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    contenedor.setAlignment(javafx.geometry.Pos.CENTER);
                    contenedor.getChildren().add(btn);

                    btn.setOnAction(e -> {
                        usuario usuarioSeleccionado = getTableView().getItems().get(getIndex());
                        String idUsuario = usuarioSeleccionado.getIdUsuario();

                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setContentText("¿Está seguro que desea eliminar este usuario?");
                        alerta.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                model m = new model();
                                if (m.eliminarUsuario(idUsuario)) {
                                    getTableView().getItems().remove(usuarioSeleccionado);
                                } else {
                                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el usuario.").showAndWait();
                                }
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }

                    usuario usuarioSeleccionado = getTableView().getItems().get(getIndex());
                    boolean esAdmin = usuarioSeleccionado != null
                            && "1".equals(usuarioSeleccionado.getIdUsuario());
                    setGraphic(esAdmin ? null : contenedor);
                }
            });

            cargarUsuariosEnTabla();

            contenidoTabla.setRowFactory(tv -> {
                TableRow<usuario> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        intentarEditarUsuario(row.getItem());
                    }
                });
                return row;
            });

            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    usuario u = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (u != null) intentarEditarUsuario(u);
                }
            });

            configurarAtajosTeclado();
        });

        RefrescoHelper.setVistaActual("registrarUsuario");
        RefrescoHelper.registrarRefresco("registrarUsuario", this::actualizarUsuarios);
    }

    private void actualizarUsuarios() {

        // 1. Limpiar UI
        Platform.runLater(() -> {
            contenidoTabla.getSelectionModel().clearSelection();
            contenidoTabla.setItems(FXCollections.observableArrayList());
        });

        // 2. Recargar datos de forma asíncrona
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }
        Task<ArrayList<usuario>> task = new Task<>() {
            @Override
            protected java.util.ArrayList<usuario> call() {
                model model = new model();
                return model.obtenerUsuarios();
            }

            @Override
            protected void succeeded() {
                java.util.ArrayList<usuario> lista = getValue();
                contenidoTabla.getItems().setAll(lista);
                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }
            }

            @Override
            protected void failed() {
                System.err.println("✗ Error al cargar usuarios: " + getException().getMessage());
                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }
            }
        };
        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private boolean solicitarContrasena(usuario user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Verificación de identidad");
        dialog.setHeaderText("Ingrese la contraseña del usuario seleccionado");

        ButtonType botonConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(botonConfirmar, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        passwordField.setPrefWidth(250);  // Ancho moderado
        passwordField.setMaxWidth(250);

        VBox contenedor = new VBox(passwordField);
        contenedor.setSpacing(10);
        contenedor.setAlignment(javafx.geometry.Pos.CENTER); // Centrado en la ventana
        contenedor.setStyle("-fx-padding: 15;"); // Márgenes internos

        dialog.getDialogPane().setContent(contenedor);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == botonConfirmar) {
                return passwordField.getText();
            }
            return null;
        });

        var resultado = dialog.showAndWait();
        return resultado.isPresent() && resultado.get().equals(user.getContrasenaUsuario());
    }


    private void intentarEditarUsuario(usuario u) {
        if (solicitarContrasena(u)) {
            abrirNuevoUsuarioConDatos(u);
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "Contraseña incorrecta. No tiene autorización para editar este usuario.")
                    .showAndWait();
        }
    }

    @FXML
    public void abrirNuevoUsuario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoUsuario.fxml"));
            Parent vista = loader.load();
            Formularios.controller.controllerNuevoUsuario controller = loader.getController();
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(vista));
            stage.setTitle("Nuevo Usuario");

            // Ajustes de ventana
            stage.setResizable(false);
            stage.setWidth(450);
            stage.setHeight(580);
            stage.centerOnScreen();

            // Modal bloquea la principal
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            stage.showAndWait();
            cargarUsuariosEnTabla(); // También al cerrar normalmente

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir el formulario").showAndWait();
        }
    }

    public void abrirNuevoUsuarioConDatos(usuario usuarioSeleccionado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoUsuario.fxml"));
            Parent vista = loader.load();
            Formularios.controller.controllerNuevoUsuario controller = loader.getController();
            controller.setMainController(this);
            controller.cargarUsuario(usuarioSeleccionado);

            Stage stage = new Stage();
            stage.setScene(new Scene(vista));
            stage.setTitle("Editar Usuario");

            // Ajustes de ventana
            stage.setResizable(false);
            stage.setWidth(450);
            stage.setHeight(580);
            stage.centerOnScreen();

            // Modal bloquea la principal
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());


            stage.showAndWait();
            cargarUsuariosEnTabla(); // También al cerrar normalmente

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir el formulario").showAndWait();
        }
    }


    public void cargarUsuariosEnTabla() {
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }
        Task<ArrayList<usuario>> task = new Task<>() {
            @Override
            protected ArrayList<usuario> call() {
                model model = new model();
                return model.obtenerUsuarios();
            }

            @Override
            protected void succeeded() {
                if (contenidoTabla != null) {
                    contenidoTabla.getItems().setAll(getValue());
                }
                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }
            }

            @Override
            protected void failed() {
                if (overlayCarga != null) {
                    overlayCarga.ocultar();
                }
                mostrarError("No se pudieron cargar los usuarios.");
            }
        };
        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
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
            if (event.getCode() == KeyCode.N) {
                abrirNuevoUsuario();
            } else if (event.getCode() == KeyCode.E) {
                eliminarUsuarioSeleccionado();
            } else {
                return;
            }
            event.consume();
        });
    }

    private void eliminarUsuarioSeleccionado() {
        usuario seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null || "1".equals(seleccionado.getIdUsuario())) return;

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setContentText("¿Está seguro que desea eliminar este usuario?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                model m = new model();
                if (m.eliminarUsuario(seleccionado.getIdUsuario())) {
                    contenidoTabla.getItems().remove(seleccionado);
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el usuario.").showAndWait();
                }
            }
        });
    }

}
