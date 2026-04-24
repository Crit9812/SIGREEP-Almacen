package Compartido.controller;

import Compartido.sesion.PermisosRol;
import VentanaPrincipal.controller.EnumVistas;
import VentanaPrincipal.controller.MainController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class navbarController {

    @FXML private Label labelOperaciones;
    @FXML private Label labelReportes;
    @FXML private Label labelConsultas;
    @FXML private Label labelConfiguracion;
    @FXML private VBox navbar;

    @FXML private Button botonOperaciones;
    @FXML private Button botonReportes;
    @FXML private Button botonConsultas;
    @FXML private Button botonConfiguracion;
    @FXML private Button botonPausa;

    private Pane overlayPane;
    private MainController controladorPrincipal;

    public void setOverlayPane(Pane overlayPane) {
        this.overlayPane = overlayPane;
        labelOperaciones = (Label) overlayPane.lookup("#labelOperaciones");
        labelReportes = (Label) overlayPane.lookup("#labelReportes");
        labelConsultas = (Label) overlayPane.lookup("#labelConsultas");
        labelConfiguracion = (Label) overlayPane.lookup("#labelConfiguracion");
    }

    public void setControladorPrincipal(MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;

        if (PermisosRol.esSupervisorOUsuario() && this.controladorPrincipal != null) {
            Platform.runLater(() -> this.controladorPrincipal.cambiarVista("REPORTES"));
        }
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            aplicarPermisosPorRol();

            navbar.setSpacing(10);
            navbar.setPadding(new Insets(7, 1, 0, 0));
            navbar.setAlignment(Pos.TOP_CENTER);

            for (Node nodo : navbar.getChildren()) {
                if (nodo instanceof HBox hbox) {
                    for (Node subNodo : hbox.getChildren()) {
                        if (subNodo instanceof Label) {
                            break;
                        }
                    }
                }
            }

            for (Node nodo : navbar.getChildren()) {
                if (nodo instanceof HBox hbox) {
                    hbox.setAlignment(Pos.CENTER);

                    Button boton = null;
                    Label label = null;

                    for (Node subNodo : hbox.getChildren()) {
                        if (subNodo instanceof Button b) {
                            boton = b;

                            if (label != null) {
                                // Solo establecer altura mínima para los botones de arriba
                                boton.prefHeightProperty().unbind(); // quitar bindings anteriores
                                boton.minHeightProperty().bind(navbar.heightProperty().multiply(0.1));
                            }

                            // Mantener ancho dinámico
                            boton.prefWidthProperty().bind(navbar.widthProperty().multiply(0.15));

                            if (b.getGraphic() instanceof ImageView iv) {
                                iv.fitHeightProperty().bind(b.heightProperty().multiply(0.72));
                                iv.fitWidthProperty().bind(b.widthProperty().multiply(0.62));
                            }

                        } else if (subNodo instanceof Label l) {
                            label = l;
                        }
                    }

                    if (boton != null && label != null) {
                        Button finalBoton = boton;
                        Label finalLabel = label;

                        // Ajuste dinámico de tamaño de fuente según altura del botón
                        finalBoton.heightProperty().addListener((obs, oldVal, newVal) -> {
                            double fontSize = newVal.doubleValue() * 0.3;
                            finalLabel.setStyle("-fx-font-size: " + fontSize + "px;");
                        });

                        finalLabel.minHeightProperty().bind(navbar.heightProperty().multiply(0.071));
                        finalLabel.minWidthProperty().bind(navbar.widthProperty().multiply(0.1));
                    }
                }
            }
        });
    }

    private void aplicarPermisosPorRol() {
        if (PermisosRol.esSupervisorOUsuario()) {
            ocultarElementoNavbar(botonOperaciones, labelOperaciones);
            ocultarElementoNavbar(botonConfiguracion, labelConfiguracion);
            ocultarElementoNavbar(botonPausa, null);
        }
        if (PermisosRol.esAuxiliar()) {
            ocultarElementoNavbar(botonPausa, null);
        }
    }

    private void ocultarElementoNavbar(Button boton, Label label) {
        if (boton != null) {
            boton.setVisible(false);
            boton.setManaged(false);
        }
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    @FXML
    private void showLabelOperaciones() { showLabel(labelOperaciones, botonOperaciones); }
    @FXML
    private void hideLabelOperaciones() { hideLabel(labelOperaciones); }
    @FXML
    private void showLabelReportes() { showLabel(labelReportes, botonReportes); }
    @FXML
    private void hideLabelReportes() { hideLabel(labelReportes); }
    @FXML
    private void showLabelConsultas() { showLabel(labelConsultas, botonConsultas); }
    @FXML
    private void hideLabelConsultas() { hideLabel(labelConsultas); }
    @FXML
    private void showLabelConfiguracion() { showLabel(labelConfiguracion, botonConfiguracion); }
    @FXML
    private void hideLabelConfiguracion() { hideLabel(labelConfiguracion); }

    private void showLabel(Label label, Button boton) {
        if (overlayPane == null || label == null || boton == null) return;
        label.setVisible(true);
        label.setManaged(true);
        label.minWidthProperty().bind(navbar.widthProperty().multiply(2.1));
        label.prefWidthProperty().bind(navbar.widthProperty().multiply(2.1));
        label.prefHeightProperty().bind(boton.heightProperty().add(7));

        label.layoutXProperty().bind(
                boton.layoutXProperty().add(boton.widthProperty()).add(2)
        );

        Bounds bounds = boton.localToScene(boton.getBoundsInLocal());
        Point2D puntoEnOverlay = overlayPane.sceneToLocal(bounds.getMinX(), bounds.getMinY());
        double botonY = puntoEnOverlay.getY();
        double botonAlto = bounds.getHeight();
        label.setLayoutY(botonY + botonAlto / 2 - label.prefHeight(-1) / 2);
    }

    private void hideLabel(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    // Métodos de navegación
    @FXML
    public void ventanaOperaciones() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cambiarVista("OPERACIONES");
        }
    }

    @FXML
    public void ventanaReportes() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cambiarVista("REPORTES");
        }
    }

    @FXML
    public void ventanaConsultas() {
        if (controladorPrincipal != null) {
            if (PermisosRol.esUsuario()) {
                controladorPrincipal.cargarVista(EnumVistas.CLAVES);
                return;
            }
            controladorPrincipal.cambiarVista("CONSULTAS");
        }
    }

    @FXML
    public void ventanaConfiguracion() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cambiarVista("CONFIGURACION");
        }
    }

    @FXML
    public void enEspera() {
        if (controladorPrincipal != null) {
            controladorPrincipal.pausarVistaActual();
        }
    }

}
