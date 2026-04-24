package Compartido.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

// Clase para mostrar un mensaje de alerta
public class alertaController {

    @FXML private VBox capaAlerta;
    @FXML private Label alertaTitulo;
    @FXML private Label alertaMensaje;
    @FXML private VBox alertaModal;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            alertaModal.maxWidthProperty().bind(capaAlerta.widthProperty().multiply(0.30));
            alertaModal.maxHeightProperty().bind(capaAlerta.heightProperty().multiply(0.25));
            capaAlerta.setMouseTransparent(true);
        });
    }

    public void mostrarAlerta(String titulo, String mensaje) {

        alertaTitulo.setText(titulo);
        alertaMensaje.setText(mensaje);

        capaAlerta.setVisible(true);
        capaAlerta.setOpacity(1);
        capaAlerta.setMouseTransparent(false);
        capaAlerta.setPickOnBounds(true);

        capaAlerta.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cerrarAlerta();
            }
            event.consume();
        });

        alertaModal.requestFocus();

        FadeTransition ft = new FadeTransition(Duration.millis(200), capaAlerta);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void cerrarAlerta() {

        FadeTransition ft = new FadeTransition(Duration.millis(200), capaAlerta);
        ft.setFromValue(1);
        ft.setToValue(0);

        ft.setOnFinished(e -> {
            capaAlerta.setVisible(false);
            capaAlerta.setMouseTransparent(true);

            // liberar teclado
            capaAlerta.removeEventFilter(KeyEvent.KEY_PRESSED, event -> {});
        });

        ft.play();
    }
}
