package VentanaPrincipal.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.helper.BorradorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private encabezadoController paneNavbarController;
    @FXML private VBox navbar;
    @FXML private StackPane contentArea;
    @FXML private Pane overlayPane;

    // Guarda el controlador de la vista actual para poder pausarlo
    private Object controladorActual;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                if (paneNavbarController != null) {
                    paneNavbarController.setControladorPrincipal(this);
                } else {
                    System.out.println("paneNavbarController es null (revisar fx:include y tipos).");
                }
                // Cargar navbar lateral
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();
                navbarController navbarCtrl = loader.getController();

                // Pasar referencia de este MainController al navbar
                navbarCtrl.setControladorPrincipal(this);

                if (overlayPane != null) {
                    navbarCtrl.setOverlayPane(overlayPane);
                }

                navbar.getChildren().setAll(navbarLoaded);

                // Bindings de tamaño
                paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
                paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

                // Vista inicial
                cargarVista(EnumVistas.OPERACIONES);

                // Manejo de cierre de ventana principal
                Stage stage = (Stage) root.getScene().getWindow();
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    salirConConfirmacion(stage);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void cargarVista(EnumVistas vista) {
        String ruta = vista.getRutaFxml();
        String titulo = vista.getTitulo();
        String color = vista.getColor();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Node vistaNodo = loader.load();

            // Actualizar header
            if (paneNavbarController != null) {
                paneNavbarController.setTitulo(titulo, color);
            }

            // Reemplazar contenido central
            contentArea.getChildren().setAll(vistaNodo);

            // Obtener el controlador de la nueva vista y guardarlo
            Object controller = loader.getController();
            this.controladorActual = controller;

            // Inyectar dependencias al controlador de la vista hija
            if (controller instanceof ContenidoController) {
                ((ContenidoController) controller).setContentArea(contentArea);
            }
            if (controller instanceof ControladorVista) {
                ((ControladorVista) controller).setControladorPrincipal(this);
            }

            // Si la vista implementa Pausable, verificar si hay un borrador guardado
            if (controller instanceof Pausable) {
                Pausable p = (Pausable) controller;
                MovimientoType tipo = p.getTipoMovimiento();

                // Si existe un borrador para este tipo de movimiento, cargarlo automáticamente
                if (BorradorService.getInstance().existe(tipo)) {
                    Object borrador = BorradorService.getInstance().recuperar(tipo);
                    p.cargarBorrador(borrador);
                    // Nota: No eliminamos el borrador aquí porque podría ser útil si el usuario
                    // quiere pausar nuevamente sin finalizar. Se eliminará al completar la operación.
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cambia a una vista usando el nombre del enum (mayúsculas)
     */
    public void cambiarVista(String nombreVista) {
        try {
            EnumVistas vista = EnumVistas.valueOf(nombreVista.toUpperCase());
            cargarVista(vista);
        } catch (IllegalArgumentException e) {
            System.err.println("Vista no encontrada: " + nombreVista);
            cargarVista(EnumVistas.OPERACIONES); // Vista por defecto
        }
    }

    public void pausarVistaActual() {
        Alert alert;

        if (controladorActual instanceof Pausable) {
            Pausable p = (Pausable) controladorActual;
            Object borrador = p.guardarBorrador();
            BorradorService.getInstance().guardar(p.getTipoMovimiento(), borrador);
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("Borrador guardado");
            alert.setContentText("Borrador guardado para: " + p.getTipoMovimiento());

        } else {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setTitle("No disponible");
            alert.setContentText("Esta pantalla no soporta la funcionalidad de pausa");
        }
        alert.showAndWait();
    }

    private void salirConConfirmacion(Stage stage) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que quieres salir?");
        alert.setHeaderText("Confirmar salida");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                stage.close();
            }
        });
    }

    // Getters útiles (opcionales)
    public Object getControladorActual() {
        return controladorActual;
    }

    public StackPane getContentArea() {
        return contentArea;
    }
}