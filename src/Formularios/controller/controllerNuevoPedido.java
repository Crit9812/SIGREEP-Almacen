package Formularios.controller;

import Operaciones.pedidos.controller.MainController;
import Compartido.controller.productoCboxController;
import Operaciones.pedidos.model.itemPedido;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;

public class controllerNuevoPedido {

    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescripcion;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;

    // ESTA LISTA VIENE DESDE EL CONTROLLER DE LA TABLA
    private ObservableList<itemPedido> itemsPedido;
    private MainController mainController;
    private itemPedido itemEnEdicion; // 🔥 NUEVO: Para saber si estamos editando
    private boolean modoEdicion = false; // 🔥 NUEVO: Flag para modo edición

    // Controlador reutilizable para productos
    private productoCboxController productoController;

    // Lista de presentaciones disponibles
    private ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    @FXML
    public void initialize() {
        // Inicializar controlador de productos
        productoController = new productoCboxController();
        productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

        // Configurar ComboBox de presentación
        configurarPresentaciones();

        // Configurar eventos del formulario
        configurarEventos();
        validarCantidad();
        validarFactor();

        // Configurar tecla Enter en los campos
        configurarManejoEnter();

        // Enfocar el primer campo al iniciar
        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setEditable(false);
        cbPresentacion.setValue("pieza"); // Valor por defecto
        txtFactor.setText("1"); // Establecer factor inicial como 1

        // 🔥 NUEVO: Agregar listener para cambiar automáticamente el factor cuando se selecciona "Pieza"
        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                String presentacion = newVal.trim();

                // Si se selecciona "Pieza", establecer factor como 1
                if (presentacion.equalsIgnoreCase("pieza")) {
                    txtFactor.setText("1");
                }
                // Si se cambia a otra presentación que NO sea "Pieza"
                else {
                    // Verificar si el factor actual es "1" (valor por defecto de Pieza)
                    String factorActual = txtFactor.getText().trim();
                    if (factorActual.equals("1") || factorActual.equals("1.0")) {
                        txtFactor.clear();
                    }
                }
            }
            // Si se borra la presentación
            else if (newVal == null || newVal.trim().isEmpty()) {
                // Mantener el factor actual si existe
                if (txtFactor.getText().trim().isEmpty()) {
                    txtFactor.setText("1");
                }
            }
        });
    }

    public void setItemsPedido(ObservableList<itemPedido> itemsPedido) {
        this.itemsPedido = itemsPedido;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void configurarEventos() {
        // Evento para cuando cambia la selección (actualizar descripción)
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        btnGuardar.setOnAction(e -> guardarItem());
        btnCancelar.setOnAction(e -> cerrarFormulario());
    }

    private void configurarManejoEnter() {
        // Permitir usar Enter para guardar desde el campo de factor
        txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });

        cbClaveProducto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbProductoNombre.requestFocus();
                event.consume();
            }
        });

        cbProductoNombre.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbClaveAlterna.requestFocus();
                event.consume();
            }
        });

        cbClaveAlterna.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbPresentacion.requestFocus();
                event.consume();
            }
        });

        cbPresentacion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtFactor.requestFocus();
                event.consume();
            }
        });

        txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });

        // También permitir Enter desde el campo factor
        txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });
    }

    private void actualizarDescripcionDesdeProducto() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
    }

    private void guardarItem() {
        if (itemsPedido == null) {
            mostrarAlerta("Error", "No se pudo conectar con la tabla principal");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String claveAlterna = productoController.getClaveAlternaSeleccionada();
        String cantidadTexto = txtCantidad.getText();
        String presentacion = cbPresentacion.getValue();
        String factor = txtFactor.getText();

        if (clave == null || clave.isEmpty() ||
                nombre == null || nombre.isEmpty() ||
                cantidadTexto == null || cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Complete todos los campos obligatorios");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0");
                return;
            }

            // Validar factor si se ingresó
            if (factor != null && !factor.isEmpty()) {
                try {
                    double factorNum = Double.parseDouble(factor);
                    if (factorNum <= 0) {
                        mostrarAlerta("Advertencia", "El factor debe ser mayor a 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El factor debe ser un número válido");
                    return;
                }
            }

            if (!productoController.validarSeleccion()) {
                mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                        "Por favor, verifique la selección.");
                return;
            }

            String descripcionCorrecta = productoController.getDescripcionSeleccionada();

            // Si no se seleccionó presentación, usar valor por defecto
            if (presentacion == null || presentacion.isEmpty()) {
                presentacion = "Pieza";
            }

            // Crear item con todos los campos
            itemPedido item = new itemPedido(
                    clave,
                    nombre,
                    descripcionCorrecta,
                    cantidad,
                    claveAlterna != null ? claveAlterna : "",
                    presentacion,
                    factor != null ? factor : ""
            );

            // 🔥 MODIFICADO: Manejar modo edición vs modo nuevo
            if (modoEdicion && itemEnEdicion != null) {
                // Modo edición: reemplazar el item existente
                if (mainController != null) {
                    mainController.actualizarItemEnLista(itemEnEdicion, item);
                }
                mostrarAlertaSinEspera("Éxito", "Producto actualizado correctamente");

                // Cerrar formulario después de editar (opcional, puedes cambiarlo)
                cerrarFormulario();
            } else {
                // Modo nuevo: agregar nuevo item
                itemsPedido.add(item);
                mostrarAlertaSinEspera("Éxito", "Producto agregado al pedido");

                if (mainController != null) {
                    mainController.refrescarTabla();
                }

                // Limpiar formulario para agregar otro
                limpiarFormularioParaNuevo();
            }

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido");
        }
    }

    public void cargarItemParaEditar(itemPedido item) {
        if (item == null) return;

        this.itemEnEdicion = item;
        this.modoEdicion = true;

        // Cargar datos del item en los campos del formulario
        productoController.setSeleccion(item.getClaveProducto(), item.getProducto());
        txtCantidad.setText(String.valueOf(item.getCantidad()));
        txtDescripcion.setText(item.getDescripcion());

        // Cargar clave alterna si existe
        if (item.getClaveAlterna() != null && !item.getClaveAlterna().isEmpty()) {
            cbClaveAlterna.setValue(item.getClaveAlterna());
        }

        // Cargar presentación y factor
        if (item.getPresentacion() != null && !item.getPresentacion().isEmpty()) {
            cbPresentacion.setValue(item.getPresentacion());
        } else {
            cbPresentacion.setValue("Pieza");
        }

        if (item.getFactor() != null && !item.getFactor().isEmpty()) {
            txtFactor.setText(item.getFactor());
        } else {
            txtFactor.clear();
        }

        btnGuardar.setText("Actualizar");

    }

    private void limpiarFormularioParaNuevo() {
        // Resetear modo edición
        modoEdicion = false;
        itemEnEdicion = null;
        productoController.limpiarSeleccion();
        txtCantidad.clear();
        txtDescripcion.clear();
        cbPresentacion.setValue("Pieza");
        txtFactor.setText("1");
        btnGuardar.setText("Guardar");
        cbClaveProducto.requestFocus();
    }

    private void mostrarAlertaSinEspera(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.initOwner(btnGuardar.getScene().getWindow());

            // Mostrar y no esperar (non-modal)
            alert.show();

            // Cerrar automáticamente después de 2 segundos
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (alert.isShowing()) {
                        Platform.runLater(() -> alert.close());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    private void limpiarFormularioCompleto() {
        modoEdicion = false;
        itemEnEdicion = null;
        productoController.limpiarSeleccion();
        txtCantidad.clear();
        txtDescripcion.clear();
        cbPresentacion.setValue("Pieza");
        txtFactor.setText("1");
        cbClaveProducto.requestFocus();
        btnGuardar.setText("Guardar");
    }

    private void validarCantidad() {
        txtCantidad.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                txtCantidad.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void validarFactor() {
        txtFactor.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtFactor.setText(val.replaceAll("[^\\d.]", ""));

                // Evitar múltiples puntos
                if (val.chars().filter(ch -> ch == '.').count() > 1) {
                    int firstDot = val.indexOf('.');
                    txtFactor.setText(val.substring(0, firstDot + 1) +
                            val.substring(firstDot + 1).replace(".", ""));
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    @FXML
    private void cerrarFormulario() {
        if (mainController != null) {
            mainController.cerrarFormulario();
        }
    }

    // Metodo para cargar un producto específico (para edición)
    public void cargarProducto(String id, String nombre) {
        if (productoController != null) {
            productoController.setSeleccion(id, nombre);
            actualizarDescripcionDesdeProducto();
        }
    }

    // Cargar por clave alterna
    public void cargarPorClaveAlterna(String idAlterno) {
        if (productoController != null) {
            productoController.setSeleccionPorClaveAlterna(idAlterno);
            actualizarDescripcionDesdeProducto();
        }
    }
    public void resetearFormulario() {
        limpiarFormularioCompleto();
    }
}
