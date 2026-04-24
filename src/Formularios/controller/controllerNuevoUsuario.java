package Formularios.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import Formularios.model.modelNuevoUsuario;
import Operaciones.registrarUsuario.controller.MainController;
import Operaciones.registrarUsuario.model.usuario;
import javafx.stage.Stage;

public class controllerNuevoUsuario {

    @FXML private Label titulo;
    @FXML private ComboBox<String> miComboBox;
    @FXML private TextField txtNombreDeUsuario;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;

    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtContrasenaVisible;
    @FXML private Button btnVerContrasena;

    @FXML private PasswordField txtConfirmarContrasena;
    @FXML private TextField txtConfirmarContrasenaVisible;
    @FXML private Button btnVerConfirmar;

    @FXML private Button btnGuardar;

    private MainController mainController;
    private usuario usuarioEditando = null;

    private final modelNuevoUsuario modelo = new modelNuevoUsuario();

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        // Texto por defecto = Agregar
        titulo.setText("Nuevo Usuario");
        btnGuardar.setText("Registrar");

        miComboBox.setItems(FXCollections.observableArrayList(
                "Administrador", "Auxiliar", "Supervisor", "Usuario"
        ));
        miComboBox.getSelectionModel().selectFirst();

        sincronizarPasswordFields();
        configurarBotonesVerOcultar();

        btnGuardar.setOnAction(e -> guardarUsuario());

        javafx.application.Platform.runLater(() -> {
            btnGuardar.getScene().setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    guardarUsuario();
                }
            });
        });
    }

    private void sincronizarPasswordFields() {
        txtContrasena.textProperty().addListener((obs, oldV, newV) -> txtContrasenaVisible.setText(newV));
        txtContrasenaVisible.textProperty().addListener((obs, oldV, newV) -> txtContrasena.setText(newV));

        txtConfirmarContrasena.textProperty().addListener((obs, oldV, newV) -> txtConfirmarContrasenaVisible.setText(newV));
        txtConfirmarContrasenaVisible.textProperty().addListener((obs, oldV, newV) -> txtConfirmarContrasena.setText(newV));
    }

    private void configurarBotonesVerOcultar() {
        btnVerContrasena.setOnAction(e -> togglePassword(txtContrasena, txtContrasenaVisible));
        btnVerConfirmar.setOnAction(e -> togglePassword(txtConfirmarContrasena, txtConfirmarContrasenaVisible));
    }

    private void togglePassword(PasswordField pf, TextField tf) {
        boolean mostrar = !tf.isVisible();
        tf.setVisible(mostrar);
        tf.setManaged(mostrar);
        pf.setVisible(!mostrar);
        pf.setManaged(!mostrar);
    }

    private void guardarUsuario() {

        if (!validarCampos()) return;

        usuario u = new usuario();
        u.setNombreUsuario(txtNombre.getText());
        u.setApellidoPUsuario(txtApellidoPaterno.getText());
        u.setApellidoMUsuario(txtApellidoMaterno.getText());
        u.setUserName(txtNombreDeUsuario.getText());
        u.setRolUsuario(miComboBox.getSelectionModel().getSelectedItem());
        u.setContrasenaUsuario(txtContrasena.getText());
        if (usuarioEditando != null) {
            u.setEstado(usuarioEditando.getEstado());
        } else {
            u.setEstado("activo");
        }

        // MODO EDICIÓN
        if (usuarioEditando != null) {
            u.setIdUsuario(usuarioEditando.getIdUsuario());

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Confirmar actualización del usuario?");
            confirmacion.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (modelo.actualizarUsuario(u)) {
                        mostrarInfo("Usuario actualizado correctamente.");
                        actualizarTablaYCerrar();
                    } else mostrarError("No se pudo actualizar.");
                }
            });
            return;
        }

        // MODO AGREGAR
        if (modelo.validarUsuarioExistente(u.getUserName())) {
            mostrarError("El nombre de usuario ya existe.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar registro del nuevo usuario?");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (modelo.insertarUsuario(u)) {
                    mostrarInfo("Usuario agregado correctamente.");
                    actualizarTablaYCerrar();
                } else mostrarError("No se pudo agregar.");
            }
        });
    }

    private boolean validarCampos() {

        if (txtNombre.getText().isBlank() ||
                txtApellidoPaterno.getText().isBlank() ||
                txtApellidoMaterno.getText().isBlank() ||
                txtNombreDeUsuario.getText().isBlank() ||
                txtContrasena.getText().isBlank() ||
                txtConfirmarContrasena.getText().isBlank()) {
            mostrarError("Todos los campos son obligatorios.");
            return false;
        }

        if (!txtContrasena.getText().equals(txtConfirmarContrasena.getText())) {
            mostrarError("Las contraseñas no coinciden.");
            return false;
        }

        return true;
    }

    public void cargarUsuario(usuario usuario) {
        this.usuarioEditando = usuario;

        // Cambios de visual en modo edición
        titulo.setText("Editar Usuario");
        btnGuardar.setText("Guardar cambios");

        txtNombre.setText(usuario.getNombreUsuario());
        txtApellidoPaterno.setText(usuario.getApellidoPUsuario());
        txtApellidoMaterno.setText(usuario.getApellidoMUsuario());
        txtNombreDeUsuario.setText(usuario.getUserName());
        miComboBox.getSelectionModel().select(usuario.getRolUsuario());

        txtContrasena.setText(usuario.getContrasenaUsuario());
        txtConfirmarContrasena.setText(usuario.getContrasenaUsuario());
    }

    private void actualizarTablaYCerrar() {
        if (mainController != null) mainController.cargarUsuariosEnTabla();
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void mostrarInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
