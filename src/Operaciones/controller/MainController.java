package Operaciones.controller;

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
    @FXML private Button btnAjusteInventario;
    @FXML private Button btnRegistrarUsuario;

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
                btn.prefWidthProperty().bind(
                        buttonGrid.maxWidthProperty().divide(4).subtract(buttonGrid.hgapProperty())
                );
                btn.prefHeightProperty().bind(
                        buttonGrid.maxHeightProperty().divide(2).subtract(buttonGrid.vgapProperty())
                );
                if (btn.getGraphic() instanceof ImageView iv) {
                    iv.fitWidthProperty().bind(btn.widthProperty().multiply(0.4));
                    iv.fitHeightProperty().bind(btn.heightProperty().multiply(0.43));
                }
            }
        }

        if (!PermisosRol.esAdministrador()) {
            ocultarBotonOperacion(btnRegistrarUsuario);
        }

        if (PermisosRol.esAuxiliar()) {
            ocultarBotonOperacion(btnAjusteInventario);
        }
    }

    private void ocultarBotonOperacion(Button boton) {
        if (boton == null) {
            return;
        }

        boton.setVisible(false);
        boton.setManaged(false);

        Integer rowIndex = GridPane.getRowIndex(boton);
        Integer colIndex = GridPane.getColumnIndex(boton);
        int filaBoton = rowIndex == null ? 0 : rowIndex;
        int colBoton = colIndex == null ? 0 : colIndex;

        for (Node node : buttonGrid.getChildren()) {
            if (!(node instanceof Button) || node == boton || !node.isManaged()) {
                continue;
            }

            Integer row = GridPane.getRowIndex(node);
            Integer col = GridPane.getColumnIndex(node);
            int fila = row == null ? 0 : row;
            int columna = col == null ? 0 : col;

            if (fila == filaBoton && columna > colBoton) {
                GridPane.setColumnIndex(node, columna - 1);
            }
        }
    }

    // Métodos de navegación
    @FXML
    public void ventanaAjusteInventario() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.AJUSTE_INVENTARIO);
        }
    }

    @FXML
    public void ventanaCompra() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.COMPRA);
        }
    }

    @FXML
    public void ventanaPedidos() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.PEDIDOS);
        }
    }

    @FXML
    public void ventanaRegistrarUsuario() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.REGISTRAR_USUARIO);
        }
    }

    @FXML
    public void ventanaTraspasoEntrada() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.TRASPASO_ENTRADA);
        }
    }

    @FXML
    public void ventanaTraspasoSalida() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.TRASPASO_SALIDA);
        }
    }

    @FXML
    public void ventanaVenta() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.VENTA);
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
