package Compartido.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class labelController {

    @FXML private Label labelOperaciones;
    @FXML private Label labelReportes;
    @FXML private Label labelConsultas;
    @FXML private Label labelConfiguracion;
    @FXML private VBox navbar;

    //----------------------------------------------------------
    @FXML private Button botonOperaciones;
    @FXML private Button botonReportes;
    @FXML private Button botonConsultas;
    @FXML private Button botonConfiguracion;

    private Pane overlayPane;

    public void setOverlayPane(Pane overlayPane) {
        this.overlayPane = overlayPane;

        labelOperaciones = (Label) overlayPane.lookup("#labelOperaciones");
        labelReportes = (Label) overlayPane.lookup("#labelReportes");
        labelConsultas = (Label) overlayPane.lookup("#labelConsultas");
        labelConfiguracion = (Label) overlayPane.lookup("#labelConfiguracion");
    }


    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            SplitPane.setResizableWithParent(navbar, false);

            navbar.setSpacing(10);
            navbar.setPadding(new Insets(7, 1, 0, 0));
            navbar.setAlignment(Pos.TOP_CENTER);

            // Botones del menú + labels
            for (Node nodo : navbar.getChildren()) {
                if (nodo instanceof HBox hbox) {
                    hbox.setAlignment(Pos.CENTER);

                    Button boton = null;
                    Label label = null;

                    for (Node subNodo : hbox.getChildren()) {
                        if (subNodo instanceof Button b) {
                            boton = b;
                            // Tamaño proporcional al navbar
                            boton.prefHeightProperty().bind(navbar.heightProperty().multiply(0.199));
                            boton.prefWidthProperty().bind(navbar.widthProperty().multiply(0.15));

                            // Ajustar imagen dentro del botón
                            if (b.getGraphic() instanceof ImageView iv) {
                                iv.fitHeightProperty().bind(b.heightProperty().multiply(0.72));
                                iv.fitWidthProperty().bind(b.widthProperty().multiply(0.62));
                            }
                        } else if (subNodo instanceof Label l) {
                            label = l;
                        }
                    }

                    // Configuración hover label con listener (optimización del punto 2)
                    if (boton != null && label != null) {
                        Button finalBoton = boton;
                        Label finalLabel = label;

                        // Ajustar font-size solo cuando cambia la altura del botón
                        finalBoton.heightProperty().addListener((obs, oldVal, newVal) -> {
                            double fontSize = newVal.doubleValue() * 0.3;
                            finalLabel.setStyle("-fx-font-size: " + fontSize + "px;");
                        });

                        // Tamaño mínimo del recuadro del label
                        finalLabel.minHeightProperty().bind(navbar.heightProperty().multiply(0.071));
                        finalLabel.minWidthProperty().bind(navbar.widthProperty().multiply(0.1));
                    }
                }
            }
        });
    }

    @FXML
    private void showLabelOperaciones() {
        showLabel(labelOperaciones, botonOperaciones);
    }

    @FXML
    private void hideLabelOperaciones() {
        labelOperaciones.setVisible(false);
        labelOperaciones.setManaged(false);
    }

    @FXML
    private void showLabelReportes() {
        showLabel(labelReportes, botonReportes);
    }

    @FXML
    private void hideLabelReportes() {
        labelReportes.setVisible(false);
        labelReportes.setManaged(false);
    }

    @FXML
    private void showLabelConsultas() {
        showLabel(labelConsultas, botonConsultas);

    }

    @FXML
    private void hideLabelConsultas() {
        labelConsultas.setVisible(false);
        labelConsultas.setManaged(false);
    }

    @FXML
    private void showLabelConfiguracion() {
        showLabel(labelConfiguracion, botonConfiguracion);
    }

    @FXML
    private void hideLabelConfiguracion() {
        labelConfiguracion.setVisible(false);
        labelConfiguracion.setManaged(false);
    }

    private void showLabel(Label label, Button boton) {
        label.setVisible(true);
        label.setManaged(true);

        // Ancho mínimo y preferido fijo proporcional al navbar
        label.minWidthProperty().bind(navbar.widthProperty().multiply(2));
        label.prefWidthProperty().bind(navbar.widthProperty().multiply(2));

        // Ajustar altura del label al botón
        label.prefHeightProperty().bind(boton.heightProperty().add(7));

        // Posicionar el label a la derecha del botón
        label.layoutXProperty().bind(
                boton.layoutXProperty()
                        .add(boton.widthProperty())
                        .add(2)
        );

        Bounds bounds = boton.localToScene(boton.getBoundsInLocal());
        Point2D puntoEnOverlay = overlayPane.sceneToLocal(bounds.getMinX(), bounds.getMinY());

        double botonY = puntoEnOverlay.getY();
        double botonAlto = bounds.getHeight();

        // Centrar verticalmente respecto al botón
        label.setLayoutY(botonY + botonAlto / 2 - label.prefHeight(-1) / 2);
    }

}
