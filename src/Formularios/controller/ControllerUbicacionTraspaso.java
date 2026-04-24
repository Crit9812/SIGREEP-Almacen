package Formularios.controller;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ControllerUbicacionTraspaso {

    @FXML private VBox contenedorDetalles;
    @FXML private Label lblTitulo;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final List<UbicacionSection> seccionesUbicacion = new ArrayList<>();

    private final model modeloTraspaso = new model();

    private String claveEntrada;
    private Stage stage;
    private java.util.function.Consumer<java.util.Map<String, List<UbicacionCompra>>> onConfirmCallback;
    private Label lblCargando;

    @FXML
    public void initialize() {
        cargarUbicacionesDesdeBD();
        actualizarTitulo();
    }

    public void setClaveEntrada(String claveEntrada) {
        this.claveEntrada = claveEntrada;
        actualizarTitulo();
        cargarDetallesEntrada();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnConfirmCallback(java.util.function.Consumer<java.util.Map<String, List<UbicacionCompra>>> onConfirmCallback) {
        this.onConfirmCallback = onConfirmCallback;
    }

    private void actualizarTitulo() {
        if (lblTitulo == null) {
            return;
        }
        if (claveEntrada == null || claveEntrada.isBlank()) {
            lblTitulo.setText("Ubicaciones");
            return;
        }
        lblTitulo.setText("Ubicaciones - " + claveEntrada);
    }

    private void cargarUbicacionesDesdeBD() {
        javafx.concurrent.Task<List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<String> call() {
                return modeloTraspaso.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
                sincronizarCombosUbicacion();
            }

            @Override
            protected void failed() {
                ubicaciones.clear();
                sincronizarCombosUbicacion();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarDetallesEntrada() {
        if (contenedorDetalles == null) {
            return;
        }

        contenedorDetalles.getChildren().clear();
        seccionesUbicacion.clear();

        if (claveEntrada == null || claveEntrada.isBlank()) {
            return;
        }

        mostrarMensajeCargando();

        javafx.concurrent.Task<List<model.DetalleEntrada>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<model.DetalleEntrada> call() {
                return modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
            }

            @Override
            protected void succeeded() {
                ocultarMensajeCargando();
                List<model.DetalleEntrada> detalles = getValue();
                if (detalles == null || detalles.isEmpty()) {
                    return;
                }
                int contador = 1;
                for (model.DetalleEntrada detalle : detalles) {
                    agregarDetalleAlFormulario(detalle, contador++, detalles.size());
                }
            }

            @Override
            protected void failed() {
                ocultarMensajeCargando();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarMensajeCargando() {
        if (contenedorDetalles == null) {
            return;
        }
        contenedorDetalles.getChildren().clear();
        lblCargando = new Label("Cargando...");
        lblCargando.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
        contenedorDetalles.getChildren().add(lblCargando);
    }

    private void ocultarMensajeCargando() {
        if (contenedorDetalles == null || lblCargando == null) {
            return;
        }
        contenedorDetalles.getChildren().remove(lblCargando);
        lblCargando = null;
    }

    private void agregarDetalleAlFormulario(model.DetalleEntrada detalle, int numero, int totalDetalles) {
        VBox productoContainer = new VBox(8);
        productoContainer.setStyle("-fx-padding: 15; -fx-background-color: #f5f5f5; " +
                "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
        productoContainer.setPadding(new Insets(15));

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label lblNumero = new Label(numero + ".");
        lblNumero.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-min-width: 30;");

        Label lblProducto = new Label(detalle.getProducto());
        lblProducto.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50;");

        headerBox.getChildren().addAll(lblNumero, lblProducto);

        VBox detallesBox = new VBox(3);
        detallesBox.setStyle("-fx-padding: 0 0 0 40;");

        Label lblClave = new Label("Clave: " + detalle.getClaveProducto());
        Label lblCantidad = new Label("Cantidad: " + detalle.getCantidad());
        Label lblPrecioUnitario = new Label("Precio unitario: " + detalle.getPrecioUnitario());
        Label lblPrecioTotal = new Label("Precio total: " + detalle.getPrecioTotal());

        detallesBox.getChildren().addAll(lblClave, lblCantidad, lblPrecioUnitario, lblPrecioTotal);

        Label lblTituloUbicaciones = new Label("Ubicaciones para este producto:");
        lblTituloUbicaciones.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        VBox contenedorUbicaciones = new VBox(10);
        contenedorUbicaciones.setStyle("-fx-padding: 5 0 0 40;");

        UbicacionSection seccion = new UbicacionSection(
                detalle.getClaveProducto(),
                detalle.getProducto(),
                contenedorUbicaciones,
                obtenerCantidadEsperada(detalle.getCantidad())
        );
        seccionesUbicacion.add(seccion);
        agregarFilaUbicacion(seccion, true);

        productoContainer.getChildren().addAll(headerBox, detallesBox, lblTituloUbicaciones, contenedorUbicaciones);
        contenedorDetalles.getChildren().add(productoContainer);

        if (numero < totalDetalles) {
            Region separador = new Region();
            separador.setPrefHeight(10);
            contenedorDetalles.getChildren().add(separador);
        }
    }

    private void agregarFilaUbicacion(UbicacionSection seccion, boolean esInicial) {
        if (seccion.contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Escribe o selecciona una ubicación");
        combo.setItems(ubicaciones);
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(esInicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (esInicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(seccion, false));
        } else {
            boton.setOnAction(event -> eliminarFilaUbicacion(seccion, nuevaFila));
        }

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        seccion.contenedor.getChildren().add(nuevaFila);

        configurarComboUbicacion(combo);

        seccion.filasUbicacion.add(new UbicacionRow(nuevaFila, combo, txtCantidad));
        seccion.contadorFilas++;
    }

    private void eliminarFilaUbicacion(UbicacionSection seccion, HBox fila) {
        UbicacionRow filaEncontrada = null;
        for (UbicacionRow filaUbicacion : seccion.filasUbicacion) {
            if (filaUbicacion.contenedor == fila) {
                filaEncontrada = filaUbicacion;
                break;
            }
        }
        if (filaEncontrada != null) {
            seccion.filasUbicacion.remove(filaEncontrada);
        }
        seccion.contenedor.getChildren().remove(fila);
        seccion.contadorFilas = Math.max(0, seccion.contadorFilas - 1);
    }

    private void configurarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        boolean[] actualizando = {false};

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizando[0]) {
                return;
            }
            actualizando[0] = true;
            if (newVal == null || newVal.isBlank()) {
                if (comboBox.getEditor() != null) {
                    comboBox.getEditor().clear();
                }
            } else if (comboBox.getEditor() != null) {
                comboBox.getEditor().setText(newVal);
            }
            actualizando[0] = false;
        });

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (actualizando[0]) {
                    return;
                }
                if (newVal == null || newVal.isBlank()) {
                    actualizando[0] = true;
                    comboBox.setValue(null);
                    actualizando[0] = false;
                }
            });

            comboBox.getEditor().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    confirmarTextoCombo(comboBox, actualizando);
                    event.consume();
                }
            });
        }

        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                confirmarTextoCombo(comboBox, actualizando);
            }
        });

        comboBox.setOnAction(event -> confirmarTextoCombo(comboBox, actualizando));
    }

    private void confirmarTextoCombo(ComboBox<String> comboBox, boolean[] actualizando) {
        if (comboBox == null || actualizando[0]) {
            return;
        }
        String texto = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        String seleccionado = comboBox.getSelectionModel().getSelectedItem();
        String valor = (seleccionado != null && !seleccionado.isBlank()) ? seleccionado : texto;
        if (valor == null || valor.isBlank()) {
            return;
        }
        actualizando[0] = true;
        comboBox.setValue(valor);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().setText(valor);
        }
        actualizando[0] = false;
    }

    private void sincronizarCombosUbicacion() {
        for (UbicacionSection seccion : seccionesUbicacion) {
            for (UbicacionRow filaUbicacion : seccion.filasUbicacion) {
                filaUbicacion.combo.setItems(ubicaciones);
            }
        }
    }

    @FXML
    private void confirmarUbicaciones() {
        List<UbicacionCompra> ubicacionesSeleccionadas = new ArrayList<>();
        java.util.Map<String, List<UbicacionCompra>> ubicacionesPorProducto = new java.util.LinkedHashMap<>();
        for (UbicacionSection seccion : seccionesUbicacion) {
            List<UbicacionCompra> ubicacionesSeccion = obtenerUbicacionesSeleccionadas(seccion);
            if (ubicacionesSeccion.isEmpty()) {
                mostrarAlerta("Validación",
                        "Debe capturar al menos una ubicación con cantidad para: " + seccion.nombreProducto);
                return;
            }
            if (!validarSumaUbicaciones(seccion, ubicacionesSeccion)) {
                return;
            }
            ubicacionesSeleccionadas.addAll(ubicacionesSeccion);
            ubicacionesPorProducto.put(seccion.claveProducto, ubicacionesSeccion);
        }

        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Validación", "Debe capturar al menos una ubicación con cantidad.");
            return;
        }

        if (onConfirmCallback != null) {
            onConfirmCallback.accept(ubicacionesPorProducto);
        }
        cerrarFormulario();
    }

    @FXML
    private void cerrarFormulario() {
        Stage ventana = stage;
        if (ventana == null && btnCancelar != null && btnCancelar.getScene() != null) {
            ventana = (Stage) btnCancelar.getScene().getWindow();
        }
        if (ventana != null) {
            ventana.close();
        }
    }

    private List<UbicacionCompra> obtenerUbicacionesSeleccionadas(UbicacionSection seccion) {
        List<UbicacionCompra> resultado = new ArrayList<>();
        for (UbicacionRow filaUbicacion : seccion.filasUbicacion) {
            ComboBox<String> combo = filaUbicacion.combo;
            TextField cantidadField = filaUbicacion.cantidad;

            String ubicacion = combo.getValue();
            if ((ubicacion == null || ubicacion.isBlank()) && combo.getEditor() != null) {
                ubicacion = combo.getEditor().getText();
            }
            String cantidadTexto = cantidadField.getText();

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }

            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) {
                    resultado.add(new UbicacionCompra(ubicacion.trim(), cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar ubicaciones con cantidad inválida
            }
        }
        return resultado;
    }

    private int obtenerCantidadEsperada(String cantidadTexto) {
        if (cantidadTexto == null) {
            return 0;
        }
        try {
            return Integer.parseInt(cantidadTexto.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean validarSumaUbicaciones(UbicacionSection seccion, List<UbicacionCompra> ubicacionesSeccion) {
        int suma = 0;
        for (UbicacionCompra ubicacion : ubicacionesSeccion) {
            suma += ubicacion.getCantidad();
        }
        if (seccion.cantidadEsperada <= 0) {
            mostrarAlerta("Validación",
                    "No se pudo determinar la cantidad esperada para: " + seccion.nombreProducto);
            return false;
        }
        if (suma != seccion.cantidadEsperada) {
            mostrarAlerta("Validación",
                    "La suma de cantidades para " + seccion.nombreProducto +
                            " debe ser " + seccion.cantidadEsperada + " y actualmente es " + suma + ".");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private static class UbicacionRow {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private UbicacionRow(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }

    private static class UbicacionSection {
        private final String claveProducto;
        private final String nombreProducto;
        private final VBox contenedor;
        private final List<UbicacionRow> filasUbicacion;
        private int contadorFilas;
        private final int cantidadEsperada;

        private UbicacionSection(String claveProducto, String nombreProducto, VBox contenedor, int cantidadEsperada) {
            this.claveProducto = claveProducto;
            this.nombreProducto = nombreProducto;
            this.contenedor = contenedor;
            this.filasUbicacion = new ArrayList<>();
            this.contadorFilas = 0;
            this.cantidadEsperada = cantidadEsperada;
        }
    }
}
