package Formularios.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class controllerNuevoAjuste {

    @FXML
    private Button btnAgregar;

    @FXML
    private Button btnQuitar;

    // estilos base
    private final String estiloNormal = "-fx-background-color: #fcfcfc; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: .5; -fx-border-radius: 5; -fx-background-radius: 5;";
    private final String estiloActivo = "-fx-background-color: #333; -fx-text-fill: white;";

    @FXML
    private void initialize() {
        // Estado inicial
        btnAgregar.setStyle(estiloNormal);
        btnQuitar.setStyle(estiloNormal);

        // Acción al presionar "Agregar"
        btnAgregar.setOnAction(event -> {
            btnAgregar.setStyle(estiloActivo);
            btnQuitar.setStyle(estiloNormal);
        });

        // Acción al presionar "Quitar"
        btnQuitar.setOnAction(event -> {
            btnQuitar.setStyle(estiloActivo);
            btnAgregar.setStyle(estiloNormal);
        });
    }

}
