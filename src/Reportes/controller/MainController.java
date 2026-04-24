package Reportes.controller;

import Compartido.sesion.PermisosRol;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private GridPane buttonGrid;
    @FXML private VBox contenedor;
    @FXML private Button btnUtilidades;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        // Bindings de tamaño para el contenedor y la grilla
        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.9));
        contenedor.prefWidthProperty().bind(root.widthProperty().multiply(1));

        buttonGrid.maxWidthProperty().bind(root.widthProperty().multiply(0.75));
        buttonGrid.maxHeightProperty().bind(root.heightProperty().multiply(0.75));

        buttonGrid.hgapProperty().bind(buttonGrid.maxWidthProperty().multiply(0.007));
        buttonGrid.vgapProperty().bind(buttonGrid.maxHeightProperty().multiply(0.02));

        buttonGrid.paddingProperty().bind(Bindings.createObjectBinding(() -> {
            double pad = buttonGrid.maxHeightProperty().get() * 0.12;
            return new Insets(pad, pad, pad, pad);
        }, buttonGrid.maxHeightProperty()));

        // Ajustar tamaño de cada botón y sus imágenes
        for (Node node : buttonGrid.getChildren()) {
            if (node instanceof Button btn) {
                // Dividimos el ancho máximo entre 4 columnas
                btn.prefWidthProperty().bind(
                        buttonGrid.maxWidthProperty().divide(4).subtract(buttonGrid.hgapProperty())
                );

                btn.prefHeightProperty().bind(
                        buttonGrid.maxHeightProperty().subtract(buttonGrid.vgapProperty())
                );
                if (btn.getGraphic() instanceof ImageView iv) {
                    iv.fitWidthProperty().bind(btn.widthProperty().multiply(0.4));
                    iv.fitHeightProperty().bind(btn.heightProperty().multiply(0.43));
                }
            }
        }

        if (PermisosRol.esUsuario()) {
            ocultarUtilidades();
        }
    }

    private void ocultarUtilidades() {
        if (btnUtilidades == null) {
            return;
        }

        btnUtilidades.setVisible(false);
        btnUtilidades.setManaged(false);

        Integer colIndex = GridPane.getColumnIndex(btnUtilidades);
        int colUtilidades = colIndex == null ? 0 : colIndex;

        for (Node node : buttonGrid.getChildren()) {
            if (!(node instanceof Button) || node == btnUtilidades || !node.isManaged()) {
                continue;
            }

            Integer col = GridPane.getColumnIndex(node);
            int columna = col == null ? 0 : col;
            if (columna > colUtilidades) {
                GridPane.setColumnIndex(node, columna - 1);
            }
        }
    }

    // Métodos de navegación (cambian la vista usando el controlador principal)
    @FXML
    public void ventanaHistorial() {
        if (controladorPrincipal != null) {
            // Ajusta el Enum según los valores que tengas definidos
            controladorPrincipal.cargarVista(EnumVistas.HISTORIAL); // ejemplo
        }
    }

    @FXML
    public void ventanaInventario() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.INVENTARIO); // ejemplo
        }
    }

    @FXML
    public void ventanaUtilidades() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.UTILIDADES); // ejemplo
        }
    }

    @FXML
    public void ventanaHistorialArticulo() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.HISTORIAL_ARTICULO); // ejemplo
        }
    }

    // Implementación de ControladorVista
    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}
