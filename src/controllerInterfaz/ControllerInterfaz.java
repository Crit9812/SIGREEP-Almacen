package controllerInterfaz;

import Compartido.helper.OverlayCarga;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import java.io.IOException;

public class ControllerInterfaz {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void cambiarVistaConOverlayTemporal(String rutaFXML, String rutaStyle, Object controlador, int milisegundosOverlay) {
        try {
            FXMLLoader loader = new FXMLLoader(ControllerInterfaz.class.getResource(rutaFXML));
            loader.setController(controlador);

            Parent root = loader.load();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            scene.getStylesheets().add(ControllerInterfaz.class.getResource(rutaStyle).toExternalForm());
            root.applyCss();
            root.layout();

            OverlayTemporal overlayTemporal = mostrarOverlayCargaTemporal(scene);

            primaryStage.getIcons().add(new Image(ControllerInterfaz.class.getResourceAsStream("/img/logo-GREEP.png")));
            primaryStage.setTitle("Gestor de inventario GREEP");
            primaryStage.setScene(scene);
            primaryStage.show();

            ocultarOverlayCargaTemporal(scene, overlayTemporal, milisegundosOverlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static OverlayTemporal mostrarOverlayCargaTemporal(Scene scene) {
        if (!(scene.getRoot() instanceof StackPane stackRoot)) {
            return null;
        }

        Pane overlayPane = new Pane();
        stackRoot.getChildren().add(overlayPane);

        OverlayCarga overlayCarga = new OverlayCarga(stackRoot, overlayPane);
        overlayCarga.mostrar();

        return new OverlayTemporal(overlayPane, overlayCarga);
    }

    private static void ocultarOverlayCargaTemporal(Scene scene, OverlayTemporal overlayTemporal, int milisegundosOverlay) {
        if (!(scene.getRoot() instanceof StackPane stackRoot) || overlayTemporal == null) {
            return;
        }

        PauseTransition pausa = new PauseTransition(Duration.millis(milisegundosOverlay));
        pausa.setOnFinished(event -> {
            overlayTemporal.overlayCarga.ocultar();
            stackRoot.getChildren().remove(overlayTemporal.overlayPane);
        });
        pausa.play();
    }

    private static class OverlayTemporal {
        private final Pane overlayPane;
        private final OverlayCarga overlayCarga;

        private OverlayTemporal(Pane overlayPane, OverlayCarga overlayCarga) {
            this.overlayPane = overlayPane;
            this.overlayCarga = overlayCarga;
        }
    }

    public static void cambiarVista(String rutaFXML, String rutaStyle, Object controlador) {
        try {
            FXMLLoader loader = new FXMLLoader(ControllerInterfaz.class.getResource(rutaFXML));
            loader.setController(controlador); // Asignamos el controlador antes de cargar

            Parent root = loader.load(); // carga FXML con el controlador asignado

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            scene.getStylesheets().add(ControllerInterfaz.class.getResource(rutaStyle).toExternalForm());
            root.applyCss();
            root.layout();

            primaryStage.getIcons().add(new Image(ControllerInterfaz.class.getResourceAsStream("/img/logo-GREEP.png")));
            primaryStage.setTitle("Gestor de inventario GREEP");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
