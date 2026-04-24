package Compartido.helper;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class OverlayCarga {

    private final StackPane root;
    private final Pane overlayPane;
    private StackPane overlayCarga;
    private Label labelCarga;
    private String mensaje = "Cargando...";

    public OverlayCarga(StackPane root, Pane overlayPane) {
        this.root = root;
        this.overlayPane = overlayPane;
        configurarOverlayCarga();
    }

    public OverlayCarga(StackPane root, Pane overlayPane, String mensaje) {
        this.root = root;
        this.overlayPane = overlayPane;
        this.mensaje = (mensaje == null || mensaje.trim().isEmpty()) ? "Cargando..." : mensaje;
        configurarOverlayCarga();
    }

    private void configurarOverlayCarga() {
        if (overlayPane == null || root == null || overlayCarga != null) {
            return;
        }

        labelCarga = new Label(mensaje);
        labelCarga.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        overlayCarga = new StackPane(labelCarga);
        overlayCarga.setVisible(false);
        overlayCarga.setManaged(false);
        overlayCarga.setMouseTransparent(true);
        overlayCarga.setPickOnBounds(true);
        overlayCarga.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlayCarga.setAlignment(Pos.CENTER);

        overlayCarga.prefWidthProperty().bind(root.widthProperty());
        overlayCarga.prefHeightProperty().bind(root.heightProperty());
        overlayCarga.setMinWidth(Region.USE_PREF_SIZE);
        overlayCarga.setMinHeight(Region.USE_PREF_SIZE);

        overlayPane.setPickOnBounds(false);
        overlayPane.setMouseTransparent(true);
        overlayPane.getChildren().add(overlayCarga);
    }

    public void setMensaje(String mensaje) {
        String nuevoMensaje = (mensaje == null || mensaje.trim().isEmpty()) ? "Cargando..." : mensaje;
        this.mensaje = nuevoMensaje;

        Runnable actualizarTexto = () -> {
            if (labelCarga != null) {
                labelCarga.setText(nuevoMensaje);
            }
        };

        if (Platform.isFxApplicationThread()) {
            actualizarTexto.run();
        } else {
            Platform.runLater(actualizarTexto);
        }
    }

    public void mostrar() {
        if (overlayCarga == null) {
            configurarOverlayCarga();
        }
        if (overlayCarga == null) {
            return;
        }

        Runnable mostrarOverlay = () -> {
            overlayCarga.setManaged(true);
            overlayCarga.setVisible(true);
            overlayPane.setPickOnBounds(true);
            overlayPane.setMouseTransparent(false);
            overlayCarga.setMouseTransparent(false);
            if (overlayPane.getParent() == root) {
                int ultimoIndice = root.getChildren().size() - 1;
                if (ultimoIndice >= 0 && root.getChildren().get(ultimoIndice) != overlayPane) {
                    root.getChildren().remove(overlayPane);
                    root.getChildren().add(overlayPane);
                }
            }
            if (overlayCarga.getParent() == overlayPane) {
                int ultimoIndiceOverlay = overlayPane.getChildren().size() - 1;
                if (ultimoIndiceOverlay >= 0 && overlayPane.getChildren().get(ultimoIndiceOverlay) != overlayCarga) {
                    overlayPane.getChildren().remove(overlayCarga);
                    overlayPane.getChildren().add(overlayCarga);
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            mostrarOverlay.run();
        } else {
            Platform.runLater(mostrarOverlay);
        }
    }

    public void ocultar() {
        if (overlayCarga == null) {
            return;
        }

        Runnable ocultarOverlay = () -> {
            overlayCarga.setVisible(false);
            overlayCarga.setManaged(false);
            overlayCarga.setMouseTransparent(true);
            overlayPane.setMouseTransparent(true);
            overlayPane.setPickOnBounds(false);
        };

        if (Platform.isFxApplicationThread()) {
            ocultarOverlay.run();
        } else {
            Platform.runLater(ocultarOverlay);
        }
    }
}
