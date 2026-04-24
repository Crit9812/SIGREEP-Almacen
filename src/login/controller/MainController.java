package login.controller;

import controllerInterfaz.ControllerInterfaz;
import Compartido.helper.OverlayCarga;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import login.model.Model;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import Compartido.controller.alertaController;
import Compartido.model.NotificacionService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class MainController {

    @FXML private StackPane root;
    @FXML private ImageView backgroundImage;
    @FXML private HBox contenedor;
    @FXML private ImageView logoImage;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private Region expansor;
    @FXML private VBox formulario;
    @FXML private alertaController alertaController;
    @FXML private Button botonOcultarContrasena;

    private boolean botonActivo = false;
    private boolean contrasenaVisible = false;

    private volatile boolean hayNotificacionesActivasPendientes = false;
    private volatile boolean interfazListaParaAviso = false;
    private volatile boolean avisoNotificacionesMostrado = false;

    private OverlayCarga overlayCargaLogin;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            contenedor.maxHeightProperty().bind(root.heightProperty().multiply(0.65));
            contenedor.maxWidthProperty().bind(root.widthProperty().multiply(0.8));

            HBox.setHgrow(expansor, Priority.SOMETIMES);
            expansor.minWidthProperty().bind(root.widthProperty().multiply(0.05));
            expansor.maxWidthProperty().bind(root.widthProperty().multiply(0.06));

            usernameField.maxWidthProperty().bind(root.widthProperty().multiply(0.2));
            usernameField.prefHeightProperty().bind(root.heightProperty().multiply(0.05));

            passwordField.maxWidthProperty().bind(usernameField.widthProperty());
            passwordField.prefHeightProperty().bind(usernameField.heightProperty());

            passwordVisibleField.maxWidthProperty().bind(usernameField.widthProperty());
            passwordVisibleField.prefHeightProperty().bind(usernameField.heightProperty());

            botonOcultarContrasena.prefHeightProperty().bind(usernameField.heightProperty());


            logoImage.fitHeightProperty().bind(contenedor.heightProperty().multiply(0.65));
            logoImage.fitWidthProperty().bind(contenedor.widthProperty().multiply(0.28));

            Pane overlayPaneLogin = new Pane();
            root.getChildren().add(overlayPaneLogin);
            overlayCargaLogin = new OverlayCarga(root, overlayPaneLogin);

            animarInicio();
            usernameField.setOnAction(event -> iniciarSesion());
            passwordField.setOnAction(event -> iniciarSesion());
            passwordVisibleField.setOnAction(event -> iniciarSesion());

        });
    }

    @FXML
    public void togglePasswordVisibility() {
        // Alternar visibilidad de la contraseña
        if (contrasenaVisible) {
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        } else {
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
        }
        contrasenaVisible = !contrasenaVisible;

        // Cambiar color de fondo del botón usando el flag
        if (botonActivo) {
            botonOcultarContrasena.setStyle("-fx-background-color: #fff; -fx-border-color: #b2b2b2; -fx-border-width: 1px;");
        } else {
            botonOcultarContrasena.setStyle("-fx-background-color: #d2d2d2; -fx-border-color: #b2b2b2; -fx-border-width: 1px;");
        }
        botonActivo = !botonActivo;
    }

    private void animarInicio() {
        formulario.setOpacity(0);
        contenedor.setOpacity(0);

        logoImage.setVisible(true);
        contenedor.setOpacity(1);

        double originalTranslateX = logoImage.getTranslateX();
        double originalTranslateY = logoImage.getTranslateY();

        logoImage.setTranslateX(root.getWidth() / 2 - logoImage.getLayoutX() - logoImage.getFitWidth() / 2);
        logoImage.setTranslateY(root.getHeight() / 2 - logoImage.getLayoutY() - logoImage.getFitHeight() / 2);

        TranslateTransition moverLogo = new TranslateTransition(Duration.seconds(1.8), logoImage);
        moverLogo.setToX(originalTranslateX);
        moverLogo.setToY(originalTranslateY);
        moverLogo.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition aparecerFormulario = new FadeTransition(Duration.seconds(1.2), formulario);
        aparecerFormulario.setFromValue(0);
        aparecerFormulario.setToValue(1);
        aparecerFormulario.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition secuencia = new SequentialTransition(
                moverLogo,
                aparecerFormulario
        );

        secuencia.play();
    }

    @FXML
    public void iniciarSesion() {
        Model modelo = new Model();
        String username = usernameField.getText();
        String password = contrasenaVisible
                ? passwordVisibleField.getText()
                : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            alertaController.mostrarAlerta(
                    "Error",
                    "Por favor, ingresa usuario y contraseña.\n(Campos vacíos)."
            );
        } else {
            if (modelo.verificarUsuario(username, password)) {

                // Guardar usuario en sesión
                Compartido.sesion.SesionUsuario.setNombreUsuario(username);

                // Obtener y guardar ID
                Integer idUsuario = modelo.obtenerIdUsuario(username);
                Compartido.sesion.SesionUsuario.setIdUsuario(idUsuario);

                String rol = modelo.obtenerRolUsuario(username);
                Compartido.sesion.SesionUsuario.setRolUsuario(rol);
                String rolUsuario = Compartido.sesion.SesionUsuario.getRolUsuario();
                System.out.println("Rol desde la sesión: " + rolUsuario);

                iniciarTransicionConCargaContinua(rolUsuario, 1200, 2500);

            } else {
                alertaController.mostrarAlerta(
                        "Error",
                        "Usuario o contraseña incorrectos"
                );
            }
        }
    }

    private void iniciarTransicionConCargaContinua(String rolUsuario, int milisegundosAntesDeCerrarLogin, int milisegundosDespuesDeAbrirPrincipal) {
        if (overlayCargaLogin != null) {
            overlayCargaLogin.mostrar();
        }

        PauseTransition esperaAntesCambio = new PauseTransition(Duration.millis(milisegundosAntesDeCerrarLogin));
        esperaAntesCambio.setOnFinished(event -> iniciarCargaInterfazYNotificacionesSimultaneas(rolUsuario, milisegundosDespuesDeAbrirPrincipal));
        esperaAntesCambio.play();
    }

    private void iniciarCargaInterfazYNotificacionesSimultaneas(String rolUsuario, int milisegundosOverlayPrincipal) {

        hayNotificacionesActivasPendientes = false;
        interfazListaParaAviso = false;
        avisoNotificacionesMostrado = false;

        Thread hiloCargaInterfaz = new Thread(() -> Platform.runLater(() -> {
            VentanaPrincipal.controller.MainController controlador = new VentanaPrincipal.controller.MainController();
            String[] vista = obtenerVistaPrincipalPorRol(rolUsuario);
            if (overlayCargaLogin != null) {
                overlayCargaLogin.ocultar();
            }
            ControllerInterfaz.cambiarVistaConOverlayTemporal(vista[0], vista[1], controlador, milisegundosOverlayPrincipal);

            PauseTransition esperaFinCarga = new PauseTransition(Duration.millis(milisegundosOverlayPrincipal + 150));
            esperaFinCarga.setOnFinished(evento -> {
                interfazListaParaAviso = true;
                intentarMostrarAvisoSiCorresponde();
            });
            esperaFinCarga.play();
        }));
        hiloCargaInterfaz.setDaemon(true);

        Thread hiloRevisionNotificaciones = new Thread(() -> {
            hayNotificacionesActivasPendientes = new NotificacionService().hayNotificacionesActivas();
            Platform.runLater(this::intentarMostrarAvisoSiCorresponde);
        }, "hilo-revision-notificaciones-login");
        hiloRevisionNotificaciones.setDaemon(true);

        hiloCargaInterfaz.start();
        hiloRevisionNotificaciones.start();
    }

    private synchronized void intentarMostrarAvisoSiCorresponde() {
        if (avisoNotificacionesMostrado) {
            return;
        }
        if (!interfazListaParaAviso) {
            return;
        }
        if (!hayNotificacionesActivasPendientes) {
            return;
        }

        avisoNotificacionesMostrado = true;
        Platform.runLater(this::mostrarDialogoNotificaciones);
    }

    private String[] obtenerVistaPrincipalPorRol(String rolUsuario) {
        // Si en el futuro cada rol tiene una vista distinta, se configura aquí.
        if ("Administrador".equalsIgnoreCase(rolUsuario)
                || "Supervisor".equalsIgnoreCase(rolUsuario)
                || "Auxiliar".equalsIgnoreCase(rolUsuario)
                || "Usuario".equalsIgnoreCase(rolUsuario)) {
            return new String[]{"/VentanaPrincipal/view/main_view.fxml", "/VentanaPrincipal/style/estilos.css"};
        }

        return new String[]{"/VentanaPrincipal/view/main_view.fxml", "/VentanaPrincipal/style/estilos.css"};
    }

    private void mostrarDialogoNotificaciones() {
        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnVer = new ButtonType("Ver", ButtonBar.ButtonData.YES);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notificaciones");
        alert.setHeaderText("Existen notificaciones sin leer");
        alert.setContentText("¿Deseas verlas ahora?");
        alert.getButtonTypes().setAll(btnCerrar, btnVer);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == btnVer) {
                abrirVentanaNotificaciones();
            }
        });
    }

    private void abrirVentanaNotificaciones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/notificaciones.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Notificaciones");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la ventana de notificaciones: " + e.getMessage());
            alert.showAndWait();
        }
    }

}
