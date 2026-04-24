package Operaciones.traspasoSalida.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class detalleSalida {

    @FXML private TextField txtClave;
    @FXML private TextField txtProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioEntrada;
    @FXML private TextField txtPrecioSalida;
    @FXML private CheckBox chkIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    @FXML
    public void initialize() {

        btnGuardar.setOnAction(e -> {
            // Aquí puedes procesar los datos
            System.out.println("Producto: " + txtProducto.getText());
            ((Stage) btnGuardar.getScene().getWindow()).close();
        });
    }
}
