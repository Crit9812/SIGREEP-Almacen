package Formularios.utilities;

import Operaciones.compra.model.UbicacionCompra;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class UbicacionManager {

    // Componentes UI
    private VBox contenedor;
    private ComboBox<String> comboUbicacionPrincipal;
    private TextField txtCantidadPrincipal;
    private ObservableList<String> ubicacionesCache; // Caché de todas las ubicaciones
    private ObservableList<String> ubicacionesFiltradas; // Lista filtrada actual

    // Estado
    private int contadorFilas = 1;
    private int maxFilas = 10;
    private boolean actualizandoFiltro = false; // Para evitar recursión

    // Constructor
    public UbicacionManager(VBox contenedor, ComboBox<String> comboUbicacion,
                            TextField txtCantidad, ObservableList<String> ubicaciones) {
        this.contenedor = contenedor;
        this.comboUbicacionPrincipal = comboUbicacion;
        this.txtCantidadPrincipal = txtCantidad;

        // Inicializar las listas
        this.ubicacionesCache = FXCollections.observableArrayList();
        this.ubicacionesFiltradas = FXCollections.observableArrayList();

        // Cargar ubicaciones iniciales
        if (ubicaciones != null) {
            this.ubicacionesCache.setAll(ubicaciones);
            this.ubicacionesFiltradas.setAll(ubicacionesCache);
        }

        // Configurar autocomplete en el combo principal si existe
        if (comboUbicacionPrincipal != null) {
            configurarAutocompleteComboBox(comboUbicacionPrincipal);
        }
    }

    // ============ SISTEMA DE AUTOCOMPLETADO ============

    /**
     * Configura el sistema de autocompletado para un ComboBox específico
     */
    private void configurarAutocompleteComboBox(ComboBox<String> comboBox) {
        if (comboBox == null) return;

        // Configurar como editable
        comboBox.setEditable(true);

        // Usar la lista filtrada como fuente de datos
        comboBox.setItems(ubicacionesFiltradas);

        // Listener para filtrado en tiempo real
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoFiltro) {
                return;
            }

            String seleccionado = comboBox.getValue();
            if (seleccionado != null && seleccionado.equals(newText)) {
                return;
            }

            actualizandoFiltro = true;
            try {
                String filtro = newText == null ? "" : newText.trim().toLowerCase();
                ObservableList<String> filtrados = FXCollections.observableArrayList();

                if (filtro.isEmpty()) {
                    // Si no hay filtro, mostrar todas las ubicaciones
                    filtrados.setAll(ubicacionesCache);
                } else {
                    // Filtrar según texto ingresado
                    for (String ubicacion : ubicacionesCache) {
                        if (ubicacion.toLowerCase().contains(filtro)) {
                            filtrados.add(ubicacion);
                        }
                    }
                }

                // Actualizar en el hilo de JavaFX
                Platform.runLater(() -> {
                    ubicacionesFiltradas.setAll(filtrados);

                    // Mostrar dropdown si hay resultados y el combo tiene foco
                    if (!ubicacionesFiltradas.isEmpty() && comboBox.isFocused()) {
                        comboBox.show();
                    }

                    // Mantener el texto del editor
                    comboBox.getEditor().setText(newText);
                    comboBox.getEditor().positionCaret(newText.length());
                });
            } finally {
                actualizandoFiltro = false;
            }
        });

        // Listener para cuando se selecciona un valor
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                // Actualizar el editor con el valor seleccionado
                Platform.runLater(() -> {
                    if (comboBox.getEditor() != null) {
                        comboBox.getEditor().setText(newVal);
                    }
                });
            }
        });

        // Comportamiento al perder el foco
        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Cuando pierde el foco
                Platform.runLater(() -> {
                    String texto = comboBox.getEditor().getText();
                    if (texto != null && !texto.trim().isEmpty()) {
                        // Verificar si el texto existe en la lista
                        boolean existe = ubicacionesCache.stream()
                                .anyMatch(u -> u.equalsIgnoreCase(texto.trim()));

                        if (!existe) {
                            // Si no existe, agregarlo a la lista
                            agregarUbicacionPersonalizada(texto.trim());
                        }

                        // Asegurar que el valor se mantenga
                        comboBox.setValue(texto.trim());
                    }
                });
            }
        });
    }

    /**
     * Agrega una ubicación personalizada a la lista
     */
    private void agregarUbicacionPersonalizada(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            return;
        }

        String ubicacionTrim = ubicacion.trim();

        // Verificar que no exista ya
        boolean yaExiste = ubicacionesCache.stream()
                .anyMatch(u -> u.equalsIgnoreCase(ubicacionTrim));

        if (!yaExiste) {
            ubicacionesCache.add(ubicacionTrim);
            ubicacionesFiltradas.add(ubicacionTrim);

            // Actualizar todos los combobox existentes
            actualizarCombosSecundarios();
        }
    }

    /**
     * Actualiza la lista de ubicaciones y refresca todos los combobox
     */
    public void actualizarListaUbicaciones(ObservableList<String> nuevasUbicaciones) {
        if (nuevasUbicaciones == null) return;

        // Actualizar caché
        this.ubicacionesCache.setAll(nuevasUbicaciones);

        // Actualizar lista filtrada
        this.ubicacionesFiltradas.setAll(ubicacionesCache);

        // Actualizar combo principal si existe
        if (comboUbicacionPrincipal != null) {
            Platform.runLater(() -> {
                // Mantener la selección actual
                String seleccionActual = comboUbicacionPrincipal.getValue();
                comboUbicacionPrincipal.setItems(FXCollections.observableArrayList(ubicacionesFiltradas));

                // Restaurar selección si aún existe
                if (seleccionActual != null && ubicacionesCache.contains(seleccionActual)) {
                    comboUbicacionPrincipal.setValue(seleccionActual);
                }
            });
        }

        // Actualizar todos los combos secundarios
        actualizarCombosSecundarios();
    }

    // ============ MÉTODOS PÚBLICOS PRINCIPALES ============

    /**
     * Agrega una nueva fila de ubicación dinámica
     */
    public void agregarFilaUbicacion() {
        if (contadorFilas >= maxFilas) {
            mostrarAlertaLimite();
            return;
        }

        HBox nuevaFila = crearFilaUbicacion();
        contenedor.getChildren().add(nuevaFila);
        contadorFilas++;
    }

    /**
     * Extrae todas las ubicaciones del UI (incluyendo la principal)
     */
    public List<UbicacionCompra> obtenerUbicaciones() {
        List<UbicacionCompra> resultado = new ArrayList<>();

        // Agregar ubicación principal si tiene datos
        if (comboUbicacionPrincipal != null && txtCantidadPrincipal != null) {
            String ubicacion = comboUbicacionPrincipal.getValue();
            String cantidadTexto = txtCantidadPrincipal.getText();

            if (ubicacion != null && !ubicacion.isBlank() &&
                    cantidadTexto != null && !cantidadTexto.isBlank()) {
                try {
                    int cantidad = Integer.parseInt(cantidadTexto);
                    if (cantidad > 0) {
                        resultado.add(new UbicacionCompra(ubicacion, cantidad));
                    }
                } catch (NumberFormatException ignored) {
                    // Ignorar si la cantidad no es válida
                }
            }
        }

        // Agregar ubicaciones de filas dinámicas
        resultado.addAll(extraerUbicacionesDinamicas());

        return resultado;
    }

    /**
     * Carga ubicaciones existentes en el UI
     */
    public void cargarUbicaciones(List<UbicacionCompra> ubicacionesExistentes) {
        limpiarUbicacionesDinamicas();
        contadorFilas = 1;

        if (ubicacionesExistentes == null || ubicacionesExistentes.isEmpty()) {
            if (comboUbicacionPrincipal != null) {
                comboUbicacionPrincipal.setValue(null);
            }
            if (txtCantidadPrincipal != null) {
                txtCantidadPrincipal.clear();
            }
            return;
        }

        // Cargar primera ubicación en campos principales
        UbicacionCompra primera = ubicacionesExistentes.get(0);
        if (comboUbicacionPrincipal != null) {
            comboUbicacionPrincipal.setValue(primera.getUbicacion());
        }
        if (txtCantidadPrincipal != null) {
            txtCantidadPrincipal.setText(String.valueOf(primera.getCantidad()));
        }

        // Cargar el resto en filas dinámicas
        for (int i = 1; i < ubicacionesExistentes.size(); i++) {
            agregarFilaUbicacion();
            UbicacionCompra ubicacion = ubicacionesExistentes.get(i);
            HBox fila = (HBox) contenedor.getChildren().get(i);
            configurarFilaUbicacion(fila, ubicacion);
        }
    }

    /**
     * Limpia todas las ubicaciones del UI
     */
    public void limpiar() {
        // Limpiar campos principales
        if (comboUbicacionPrincipal != null) {
            comboUbicacionPrincipal.setValue(null);
            if (comboUbicacionPrincipal.getEditor() != null) {
                comboUbicacionPrincipal.getEditor().clear();
            }
        }

        if (txtCantidadPrincipal != null) {
            txtCantidadPrincipal.clear();
        }

        // Limpiar ubicaciones dinámicas
        limpiarUbicacionesDinamicas();

        // Reiniciar contador
        contadorFilas = 1;
    }

    /**
     * Valida que las ubicaciones sumen la cantidad total especificada
     */
    public helperCompraEmergente.ResultadoValidacion validarUbicaciones(int cantidadTotal) {
        List<UbicacionCompra> ubicaciones = obtenerUbicaciones();

        if (ubicaciones.isEmpty()) {
            return new helperCompraEmergente.ResultadoValidacion(
                    false, "Debe capturar al menos una ubicación con cantidad."
            );
        }

        int sumaUbicaciones = ubicaciones.stream()
                .mapToInt(UbicacionCompra::getCantidad)
                .sum();

        if (sumaUbicaciones != cantidadTotal) {
            return new helperCompraEmergente.ResultadoValidacion(
                    false,
                    "La suma de cantidades por ubicación (" + sumaUbicaciones +
                            ") debe ser igual a la cantidad total (" + cantidadTotal + ")."
            );
        }

        return new helperCompraEmergente.ResultadoValidacion(true, "");
    }

    // ============ MÉTODOS PRIVADOS DE UI ============

    private HBox crearFilaUbicacion() {
        HBox nuevaFila = new HBox(20);

        // Columna 1: ComboBox de ubicación
        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");

        // Crear ComboBox con autocompletado
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicacionesFiltradas);
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");
        configurarAutocompleteComboBox(nuevoCombo); // Aplicar el mismo sistema
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        // Columna 2: TextField de cantidad
        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        configurarValidadorEnteros(txtCantidad);
        vboxCantidad.getChildren().addAll(lblCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        // Columna 3: Botón eliminar
        VBox vboxBoton = new VBox(5);
        Button botonEliminar = new Button();
        String styleV = "-fx-background-color: #d3d3d3; -fx-border-color: #999; -fx-font-weight: bold; " +
                "-fx-cursor: hand; -fx-border-radius: 5; -fx-max-width: 25; -fx-max-height: 25; " +
                "-fx-background-radius: 5; -fx-text-fill: black;";
        botonEliminar.setStyle(styleV);
        botonEliminar.setText("-");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().addAll(botonEliminar);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        // Configurar evento eliminar
        botonEliminar.setOnAction(this::manejarEliminarFila);

        // Ensamblar la fila
        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        nuevaFila.setUserData(contadorFilas); // Guardar índice para referencia

        return nuevaFila;
    }

    /**
     * Actualiza todos los combobox secundarios con la lista actual
     */
    private void actualizarCombosSecundarios() {
        for (int i = 1; i < contenedor.getChildren().size(); i++) {
            javafx.scene.Node nodo = contenedor.getChildren().get(i);
            if (!(nodo instanceof HBox)) continue;

            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 1) continue;

            VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
            for (javafx.scene.Node child : vboxUbicacion.getChildren()) {
                if (child instanceof ComboBox) {
                    @SuppressWarnings("unchecked")
                    ComboBox<String> comboBox = (ComboBox<String>) child;

                    Platform.runLater(() -> {
                        // Mantener selección actual
                        String seleccionActual = comboBox.getValue();
                        comboBox.setItems(FXCollections.observableArrayList(ubicacionesFiltradas));

                        // Restaurar selección si existe
                        if (seleccionActual != null && ubicacionesCache.contains(seleccionActual)) {
                            comboBox.setValue(seleccionActual);
                        }
                    });
                    break;
                }
            }
        }
    }

    private void configurarFilaUbicacion(HBox fila, UbicacionCompra ubicacion) {
        if (fila.getChildren().size() < 2) {
            return;
        }

        VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
        VBox vboxCantidad = (VBox) fila.getChildren().get(1);

        ComboBox<String> combo = null;
        TextField cantidadField = null;

        // Buscar ComboBox en la primera columna
        for (javafx.scene.Node child : vboxUbicacion.getChildren()) {
            if (child instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> comboBox = (ComboBox<String>) child;
                combo = comboBox;
                break;
            }
        }

        // Buscar TextField en la segunda columna
        for (javafx.scene.Node child : vboxCantidad.getChildren()) {
            if (child instanceof TextField) {
                cantidadField = (TextField) child;
                break;
            }
        }

        if (combo != null) {
            combo.setValue(ubicacion.getUbicacion());
        }
        if (cantidadField != null) {
            cantidadField.setText(String.valueOf(ubicacion.getCantidad()));
        }
    }

    private List<UbicacionCompra> extraerUbicacionesDinamicas() {
        List<UbicacionCompra> resultado = new ArrayList<>();

        // Empezar desde 1 para saltar la fila principal (si existe)
        for (int i = 1; i < contenedor.getChildren().size(); i++) {
            javafx.scene.Node nodo = contenedor.getChildren().get(i);
            if (!(nodo instanceof HBox)) continue;

            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 2) continue;

            VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
            VBox vboxCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<?> combo = null;
            TextField cantidadField = null;

            // Extraer ComboBox
            for (javafx.scene.Node child : vboxUbicacion.getChildren()) {
                if (child instanceof ComboBox) {
                    combo = (ComboBox<?>) child;
                    break;
                }
            }

            // Extraer TextField
            for (javafx.scene.Node child : vboxCantidad.getChildren()) {
                if (child instanceof TextField) {
                    cantidadField = (TextField) child;
                    break;
                }
            }

            if (combo == null || cantidadField == null) continue;

            String ubicacion = combo.getValue() != null ? combo.getValue().toString() : "";
            String cantidadTexto = cantidadField.getText();

            if (ubicacion == null || ubicacion.isBlank() ||
                    cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }

            try {
                int cantidad = Integer.parseInt(cantidadTexto);
                if (cantidad > 0) {
                    resultado.add(new UbicacionCompra(ubicacion, cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar ubicaciones con cantidad inválida
            }
        }

        return resultado;
    }

    private void limpiarUbicacionesDinamicas() {
        // Eliminar todas las filas dinámicas (manteniendo solo la primera fila principal si existe)
        if (contenedor != null) {
            // Crear una lista de los índices a eliminar (desde 1 hasta el final)
            List<javafx.scene.Node> nodosAEliminar = new ArrayList<>();

            for (int i = 1; i < contenedor.getChildren().size(); i++) {
                javafx.scene.Node nodo = contenedor.getChildren().get(i);
                if (nodo instanceof HBox) {
                    nodosAEliminar.add(nodo);
                }
            }

            // Eliminar todos los nodos
            contenedor.getChildren().removeAll(nodosAEliminar);
        }
    }

    private void manejarEliminarFila(ActionEvent event) {
        Button botonPresionado = (Button) event.getSource();
        VBox contenedorBoton = (VBox) botonPresionado.getParent();
        HBox fila = (HBox) contenedorBoton.getParent();

        contenedor.getChildren().remove(fila);
        contadorFilas--;

        // Reindexar filas restantes
        for (int i = 1; i < contenedor.getChildren().size(); i++) {
            HBox filaActual = (HBox) contenedor.getChildren().get(i);
            filaActual.setUserData(i);
        }
    }

    // ============ MÉTODOS DE UTILIDAD ============

    private void configurarValidadorEnteros(TextField campo) {
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                campo.setText(helperCompraEmergente.getValidadorEnteros().apply(newVal));
            }
        });
    }

    private void mostrarAlertaLimite() {
        Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Límite alcanzado");
            alerta.setHeaderText(null);
            alerta.setContentText("Solo se pueden agregar hasta " + maxFilas + " ubicaciones.");
            alerta.showAndWait();
        });
    }

    // ============ GETTERS Y SETTERS ============

    public void setMaxFilas(int maxFilas) {
        this.maxFilas = maxFilas;
    }

    public int getContadorFilas() {
        return contadorFilas;
    }

    public ObservableList<String> getUbicacionesCache() {
        return FXCollections.observableArrayList(ubicacionesCache);
    }
}