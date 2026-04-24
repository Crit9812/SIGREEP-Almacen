package Compartido.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.model.modelProductoCbox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class productoCboxController {

    // Referencias a los ComboBox
    private ComboBox<String> cbProductoId;
    private ComboBox<String> cbProductoNombre;
    private ComboBox<String> cbClaveAlterna;

    // Datos y servicios
    private List<Map<String, String>> productos;
    private List<Map<String, String>> clavesAlternas;
    private final modelProductoCbox modelProductoCbox;

    // Executor para manejar hilos
    private final ExecutorService executorService;

    // Control de sincronización
    private String ultimoIdSeleccionado;
    private String ultimoNombreSeleccionado;
    private String ultimaClaveAlternaSeleccionada;
    private boolean actualizandoDesdeId = false;
    private boolean actualizandoDesdeNombre = false;
    private boolean actualizandoDesdeClaveAlterna = false;

    // Mapa para almacenar las descripciones de las claves alternas
    private final Map<String, String> mapaDescripcionesClaves = new HashMap<>();
    private String idProveedorFiltro;
    private boolean soloDisponibles;

    public productoCboxController() {
        this.modelProductoCbox = new modelProductoCbox();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Inicializa el controlador con los ComboBox
     */
    public void inicializar(ComboBox<String> cbId, ComboBox<String> cbNombre, ComboBox<String> cbClaveAlterna) {
        this.idProveedorFiltro = null;
        this.soloDisponibles = false;
        inicializarBase(cbId, cbNombre, cbClaveAlterna);
    }

    public void inicializarDisponibles(ComboBox<String> cbId, ComboBox<String> cbNombre, ComboBox<String> cbClaveAlterna) {
        this.idProveedorFiltro = null;
        this.soloDisponibles = true;
        inicializarBase(cbId, cbNombre, cbClaveAlterna);
    }

    public void inicializarConProveedor(ComboBox<String> cbId, ComboBox<String> cbNombre, ComboBox<String> cbClaveAlterna,
                                        String idProveedor) {
        this.idProveedorFiltro = idProveedor;
        this.soloDisponibles = false;
        inicializarBase(cbId, cbNombre, cbClaveAlterna);
    }

    public String getUrlImagenSeleccionada() {
        String id = getIdSeleccionado();
        if (id == null || id.isBlank() || productos == null) {
            return null;
        }

        for (Map<String, String> producto : productos) {
            if (id.equals(producto.get("id"))) {
                return producto.get("urlImagen");
            }
        }

        return null;
    }

    private void inicializarBase(ComboBox<String> cbId, ComboBox<String> cbNombre, ComboBox<String> cbClaveAlterna) {
        this.cbProductoId = cbId;
        this.cbProductoNombre = cbNombre;
        this.cbClaveAlterna = cbClaveAlterna;

        // Configurar autocompletado para todos los ComboBox
        configurarAutocompletado();

        // Configurar el ComboBox de clave alterna
        configurarComboBoxClaveAlterna();

        cargarDatosEnSegundoPlano();
        configurarEventos();
    }

    /**
     * Configura autocompletado para todos los ComboBox
     */
    private void configurarAutocompletado() {
        // Hacer todos los ComboBox editables
        cbProductoId.setEditable(true);
        cbProductoNombre.setEditable(true);
        cbClaveAlterna.setEditable(true);

        // Agregar listeners de autocompletado (se agregarán cuando se carguen los datos)
        Platform.runLater(() -> {
            // Los listeners se crearán después de cargar los datos
            // Esto se hará en cargarProductosEnUI() y actualizarComboBoxClavesAlternas()
        });
    }

    /**
     * Configura el ComboBox de clave alterna con CellFactory personalizado
     */
    private void configurarComboBoxClaveAlterna() {
        if (cbClaveAlterna == null) return;

        // Configurar StringConverter
        cbClaveAlterna.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String clave) {
                // Siempre mostrar solo la clave en el campo
                return clave == null ? "" : clave;
            }

            @Override
            public String fromString(String texto) {
                if (texto == null || texto.isEmpty()) {
                    return "";
                }

                // Si el usuario escribió "clave - proveedor", extraer solo la clave
                if (texto.contains(" - ")) {
                    String[] partes = texto.split(" - ");
                    if (partes.length > 0) {
                        String posibleClave = partes[0].trim();
                        // Verificar si esta clave existe en nuestro mapa
                        if (mapaDescripcionesClaves.containsKey(posibleClave)) {
                            return posibleClave;
                        }
                    }
                }

                return texto.trim();
            }
        });

        // Configurar CellFactory para mostrar descripciones en la lista
        cbClaveAlterna.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String clave, boolean empty) {
                        super.updateItem(clave, empty);

                        if (empty || clave == null) {
                            setText(null);
                        } else if (clave.isEmpty()) {
                            setText("(vacío)");
                        } else {
                            // Mostrar la descripción completa en la lista
                            String descripcion = mapaDescripcionesClaves.get(clave);
                            setText(descripcion != null ? descripcion : clave);
                        }
                    }
                };
            }
        });

        // Manejar la tecla Enter en el ComboBox de clave alterna
        cbClaveAlterna.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Cuando se presiona Enter, forzar la actualización
                String texto = cbClaveAlterna.getEditor().getText();
                if (texto != null && !texto.isEmpty()) {
                    // Llamar al método de actualización
                    if (!actualizandoDesdeId && !actualizandoDesdeNombre && !actualizandoDesdeClaveAlterna) {
                        String idAlterno = cbClaveAlterna.getConverter().fromString(texto);
                        if (idAlterno != null && !idAlterno.isEmpty()) {
                            actualizarDesdeClaveAlterna(idAlterno);
                        }
                    }
                }
                event.consume(); // Prevenir comportamiento por defecto
            }
        });
    }

    /**
     * Carga todos los datos necesarios en segundo plano
     */
    private void cargarDatosEnSegundoPlano() {
        // Cargar productos
        Task<List<Map<String, String>>> taskProductos = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                if (idProveedorFiltro != null && !idProveedorFiltro.isBlank()) {
                    return modelProductoCbox.obtenerProductosPorProveedor(idProveedorFiltro);
                }
                if (soloDisponibles) {
                    return modelProductoCbox.obtenerProductosDisponibles();
                }
                return modelProductoCbox.obtenerTodosProductos();
            }

            @Override
            protected void succeeded() {
                productos = getValue();
                Platform.runLater(() -> cargarProductosEnUI());
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> mostrarAlerta("Error",
                        "No se pudieron cargar los productos: " + getException().getMessage()));
            }
        };

        // Cargar claves alternas
        Task<List<Map<String, String>>> taskClaves = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                if (idProveedorFiltro != null && !idProveedorFiltro.isBlank()) {
                    return modelProductoCbox.obtenerClavesAlternasPorProveedor(idProveedorFiltro);
                }
                if (soloDisponibles) {
                    return modelProductoCbox.obtenerClavesAlternasDisponibles();
                }
                return modelProductoCbox.obtenerTodasClavesAlternas();
            }

            @Override
            protected void succeeded() {
                clavesAlternas = getValue();
                Platform.runLater(() -> cargarClavesAlternasIniciales());
            }

            @Override
            protected void failed() {
                clavesAlternas = Collections.emptyList();
            }
        };

        executorService.submit(taskProductos);
        executorService.submit(taskClaves);
    }

    private void cargarClavesAlternasIniciales() {
        if (cbClaveAlterna == null) {
            return;
        }

        cbClaveAlterna.getItems().clear();
        mapaDescripcionesClaves.clear();
        cbClaveAlterna.getItems().add("");

        if (clavesAlternas == null || clavesAlternas.isEmpty()) {
            cbClaveAlterna.setValue("");
            ultimaClaveAlternaSeleccionada = null;
            return;
        }

        java.util.Set<String> alternasUnicas = new java.util.LinkedHashSet<>();
        for (Map<String, String> clave : clavesAlternas) {
            String idAlterno = clave.get("idAlterno");
            if (idAlterno == null || idAlterno.isBlank()) {
                continue;
            }
            if (!alternasUnicas.add(idAlterno)) {
                continue;
            }
            String nombreProveedor = clave.get("nombreProveedor");
            String textoDescriptivo = nombreProveedor != null && !nombreProveedor.isEmpty()
                    ? idAlterno + " - " + nombreProveedor
                    : idAlterno;
            mapaDescripcionesClaves.put(idAlterno, textoDescriptivo);
            cbClaveAlterna.getItems().add(idAlterno);
        }

        new AutoCompleteComboBoxListener<>(cbClaveAlterna);
    }

    /**
     * Carga productos en la UI
     */
    private void cargarProductosEnUI() {
        if (productos == null || cbProductoId == null || cbProductoNombre == null) return;

        List<String> ids = new ArrayList<>(productos.size());
        java.util.Set<String> nombresUnicos = new java.util.LinkedHashSet<>();

        productos.forEach(p -> {
            ids.add(p.get("id"));
            String nombre = p.get("nombre");
            if (nombre != null && !nombre.isBlank()) {
                nombresUnicos.add(nombre);
            }
        });

        cbProductoId.getItems().setAll(ids);
        cbProductoNombre.getItems().setAll(nombresUnicos);

        // 🔥 AGREGAR AUTOCOMPLETADO después de cargar los datos
        new AutoCompleteComboBoxListener<>(cbProductoId);
        new AutoCompleteComboBoxListener<>(cbProductoNombre);

        // Configurar manejo de Enter para los ComboBox de productos
        configurarManejoEnter();

        // Inicializar el ComboBox de claves alternas vacío
        cbClaveAlterna.getItems().clear();
        cbClaveAlterna.setValue("");
        if (clavesAlternas != null && !clavesAlternas.isEmpty()) {
            cargarClavesAlternasIniciales();
        }
    }

    /**
     * Configura el manejo de la tecla Enter en los ComboBox
     */
    private void configurarManejoEnter() {
        // Manejar Enter en cbProductoId
        cbProductoId.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String id = cbProductoId.getValue();
                if (id != null && !actualizandoDesdeNombre && !actualizandoDesdeClaveAlterna) {
                    actualizarDesdeId(id);
                }
                event.consume();
            }
        });

        // Manejar Enter en cbProductoNombre
        cbProductoNombre.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String nombre = cbProductoNombre.getValue();
                if (nombre != null && !actualizandoDesdeId && !actualizandoDesdeClaveAlterna) {
                    actualizarDesdeNombre(nombre);
                }
                event.consume();
            }
        });
    }

    /**
     * Configura los eventos de los ComboBox
     */
    private void configurarEventos() {
        // Listener para cambios en cbProductoId
        cbProductoId.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !actualizandoDesdeNombre && !actualizandoDesdeClaveAlterna) {
                actualizarDesdeId(newVal);
            }
        });

        // Listener para cambios en cbProductoNombre
        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !actualizandoDesdeId && !actualizandoDesdeClaveAlterna) {
                actualizarDesdeNombre(newVal);
            }
        });

        // Listener para cambios en cbClaveAlterna
        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            if (!actualizandoDesdeId && !actualizandoDesdeNombre && !actualizandoDesdeClaveAlterna) {
                String idAlterno = cbClaveAlterna.getConverter().fromString(newVal);
                if (idAlterno != null && !idAlterno.isEmpty()) {
                    actualizarDesdeClaveAlterna(idAlterno);
                } else {
                    ultimaClaveAlternaSeleccionada = null;
                }
            }
        });
    }

    /**
     * Actualiza desde ID
     */
    private void actualizarDesdeId(String id) {
        actualizandoDesdeId = true;
        try {
            modelProductoCbox.buscarPorId(id, productos).ifPresent(producto -> {
                cbProductoNombre.setValue(producto.get("nombre"));
                ultimoIdSeleccionado = id;
                ultimoNombreSeleccionado = producto.get("nombre");
                mostrarClavesAlternasParaProducto(id);
            });
        } finally {
            actualizandoDesdeId = false;
        }
    }

    /**
     * Actualiza desde nombre
     */
    private void actualizarDesdeNombre(String nombre) {
        actualizandoDesdeNombre = true;
        try {
            List<Map<String, String>> productosConNombre = modelProductoCbox.buscarPorNombre(nombre, productos);

            if (productosConNombre.isEmpty()) return;

            if (productosConNombre.size() == 1) {
                Map<String, String> producto = productosConNombre.get(0);
                cbProductoId.setValue(producto.get("id"));
                ultimoIdSeleccionado = producto.get("id");
                ultimoNombreSeleccionado = nombre;
                mostrarClavesAlternasParaProducto(producto.get("id"));
            } else {
                manejarNombreDuplicado(nombre, productosConNombre);
            }
        } finally {
            actualizandoDesdeNombre = false;
        }
    }

    /**
     * Actualiza desde clave alterna
     */
    private void actualizarDesdeClaveAlterna(String idAlterno) {
        actualizandoDesdeClaveAlterna = true;
        try {
            try {
                if (idProveedorFiltro != null && !idProveedorFiltro.isBlank()) {
                    modelProductoCbox.buscarPorClaveAlterna(idAlterno, idProveedorFiltro).ifPresent(producto -> {
                        cbProductoId.setValue(producto.get("id"));
                        cbProductoNombre.setValue(producto.get("nombre"));
                        ultimoIdSeleccionado = producto.get("id");
                        ultimoNombreSeleccionado = producto.get("nombre");
                        ultimaClaveAlternaSeleccionada = idAlterno;

                        if (ultimoIdSeleccionado == null || !producto.get("id").equals(ultimoIdSeleccionado)) {
                            mostrarClavesAlternasParaProducto(producto.get("id"));
                        }
                    });
                } else {
                    modelProductoCbox.buscarPorClaveAlterna(idAlterno).ifPresent(producto -> {
                        cbProductoId.setValue(producto.get("id"));
                        cbProductoNombre.setValue(producto.get("nombre"));
                        ultimoIdSeleccionado = producto.get("id");
                        ultimoNombreSeleccionado = producto.get("nombre");
                        ultimaClaveAlternaSeleccionada = idAlterno;

                        // Solo recargar claves si el producto ha cambiado
                        if (ultimoIdSeleccionado == null || !producto.get("id").equals(ultimoIdSeleccionado)) {
                            mostrarClavesAlternasParaProducto(producto.get("id"));
                        }
                    });
                }
            } catch (SQLException e) {
                mostrarAlerta("Error", "No se pudo obtener información de la clave alterna: " + e.getMessage());
            }
        } finally {
            actualizandoDesdeClaveAlterna = false;
        }
    }

    /**
     * Muestra claves alternas para un producto
     */
    private void mostrarClavesAlternasParaProducto(String idProducto) {
        if (actualizandoDesdeClaveAlterna) {
            return;
        }

        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                if (idProveedorFiltro != null && !idProveedorFiltro.isBlank()) {
                    return modelProductoCbox.obtenerClavesAlternasPorProducto(idProducto, idProveedorFiltro);
                }
                return modelProductoCbox.obtenerClavesAlternasPorProducto(idProducto);
            }

            @Override
            protected void succeeded() {
                List<Map<String, String>> clavesParaProducto = getValue();
                Platform.runLater(() -> actualizarComboBoxClavesAlternas(idProducto, clavesParaProducto));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    cbClaveAlterna.setValue("");
                    cbClaveAlterna.getItems().clear();
                    mapaDescripcionesClaves.clear();
                    ultimaClaveAlternaSeleccionada = null;
                });
            }
        };

        executorService.submit(task);
    }

    /**
     * Actualiza el ComboBox de claves alternas con las opciones del producto
     */
    private void actualizarComboBoxClavesAlternas(String idProducto, List<Map<String, String>> clavesParaProducto) {
        // Guardar la clave seleccionada actual
        String claveSeleccionadaActual = getClaveAlternaSeleccionada();

        // Limpiar
        cbClaveAlterna.getItems().clear();
        mapaDescripcionesClaves.clear();

        if (clavesParaProducto == null || clavesParaProducto.isEmpty()) {
            cbClaveAlterna.setValue("");
            ultimaClaveAlternaSeleccionada = null;
            return;
        }

        // Agregar opción vacía
        cbClaveAlterna.getItems().add("");

        boolean claveSeleccionadaEncontrada = false;

        for (Map<String, String> clave : clavesParaProducto) {
            String idAlterno = clave.get("idAlterno");
            String nombreProveedor = clave.get("nombreProveedor");

            String textoDescriptivo = nombreProveedor != null && !nombreProveedor.isEmpty()
                    ? idAlterno + " - " + nombreProveedor
                    : idAlterno;

            mapaDescripcionesClaves.put(idAlterno, textoDescriptivo);
            cbClaveAlterna.getItems().add(idAlterno);

            if (idAlterno.equals(claveSeleccionadaActual)) {
                claveSeleccionadaEncontrada = true;
            }
        }

        // 🔥 AGREGAR AUTOCOMPLETADO para el ComboBox de claves alternas
        new AutoCompleteComboBoxListener<>(cbClaveAlterna);

        // Restaurar selección
        if (claveSeleccionadaActual != null && claveSeleccionadaEncontrada) {
            cbClaveAlterna.setValue(claveSeleccionadaActual);
            ultimaClaveAlternaSeleccionada = claveSeleccionadaActual;
        } else if (clavesParaProducto.size() == 1) {
            String idAlterno = clavesParaProducto.get(0).get("idAlterno");
            cbClaveAlterna.setValue(idAlterno);
            ultimaClaveAlternaSeleccionada = idAlterno;
        } else {
            cbClaveAlterna.setValue("");
            ultimaClaveAlternaSeleccionada = null;
        }
    }

    /**
     * Maneja nombres duplicados
     */
    private void manejarNombreDuplicado(String nombre, List<Map<String, String>> productosConNombre) {
        Platform.runLater(() -> {
            Dialog<Map<String, String>> dialog = crearDialogoSeleccion(
                    "Seleccionar Producto",
                    "Hay " + productosConNombre.size() + " productos con el nombre '" + nombre + "'",
                    productosConNombre,
                    producto -> "ID: " + producto.get("id") + " - Descripción: " + producto.get("descripcion")
            );

            dialog.showAndWait().ifPresent(producto -> {
                cbProductoId.setValue(producto.get("id"));
                ultimoIdSeleccionado = producto.get("id");
                ultimoNombreSeleccionado = producto.get("nombre");
                mostrarClavesAlternasParaProducto(producto.get("id"));
            });
        });
    }

    /**
     * Crea un diálogo de selección genérico
     */
    private <T> Dialog<T> crearDialogoSeleccion(String titulo, String header, List<T> items,
                                                java.util.function.Function<T, String> formatter) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(header);
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType btnSeleccionar = new ButtonType("Seleccionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSeleccionar, ButtonType.CANCEL);

        ListView<String> listaOpciones = new ListView<>();
        items.forEach(item -> listaOpciones.getItems().add(formatter.apply(item)));
        listaOpciones.setPrefSize(500, 200);
        dialog.getDialogPane().setContent(listaOpciones);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSeleccionar) {
                int indice = listaOpciones.getSelectionModel().getSelectedIndex();
                if (indice >= 0 && indice < items.size()) {
                    return items.get(indice);
                }
            }
            return null;
        });

        return dialog;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public String getIdSeleccionado() {
        return cbProductoId != null ? cbProductoId.getValue() : null;
    }

    public String getNombreSeleccionado() {
        return cbProductoNombre != null ? cbProductoNombre.getValue() : null;
    }

    public String getClaveAlternaSeleccionada() {
        return cbClaveAlterna != null ? cbClaveAlterna.getValue() : null;
    }

    public String getDescripcionSeleccionada() {
        String id = getIdSeleccionado();
        return (id != null && productos != null) ?
                modelProductoCbox.obtenerDescripcionPorId(id, productos) : "";
    }

    public String getUnidadMedidaSeleccionada() {
        String id = getIdSeleccionado();
        return (id != null && productos != null) ?
                modelProductoCbox.obtenerUnidadMedidaPorId(id, productos) : "";
    }

    public String getCategoriaSeleccionada() {
        String id = getIdSeleccionado();
        return (id != null && productos != null) ?
                modelProductoCbox.obtenerCategoriaPorId(id, productos) : "";
    }

    public boolean validarSeleccion() {
        String id = getIdSeleccionado();
        String nombre = getNombreSeleccionado();
        return (id != null && nombre != null && productos != null) &&
                modelProductoCbox.validarIdNombre(id, nombre, productos);
    }

    public Optional<Map<String, String>> getProductoSeleccionado() {
        String id = getIdSeleccionado();
        return (id != null && productos != null) ?
                modelProductoCbox.buscarPorId(id, productos) : Optional.empty();
    }

    public void limpiarSeleccion() {
        actualizandoDesdeId = true;
        actualizandoDesdeNombre = true;
        actualizandoDesdeClaveAlterna = true;

        try {
            Optional.ofNullable(cbProductoId).ifPresent(cb -> cb.setValue(null));
            Optional.ofNullable(cbProductoNombre).ifPresent(cb -> cb.setValue(null));
            Optional.ofNullable(cbClaveAlterna).ifPresent(cb -> {
                cb.setValue("");
                cb.getItems().clear();
            });

            mapaDescripcionesClaves.clear();

            if (cbClaveAlterna != null && clavesAlternas != null && !clavesAlternas.isEmpty()) {
                cargarClavesAlternasIniciales();
            }

            ultimoIdSeleccionado = null;
            ultimoNombreSeleccionado = null;
            ultimaClaveAlternaSeleccionada = null;
        } finally {
            actualizandoDesdeId = false;
            actualizandoDesdeNombre = false;
            actualizandoDesdeClaveAlterna = false;
        }
    }

    public boolean setSeleccion(String id, String nombre) {
        if (productos == null || !modelProductoCbox.validarIdNombre(id, nombre, productos)) {
            return false;
        }

        Optional.ofNullable(cbProductoId).ifPresent(cb -> cb.setValue(id));
        Optional.ofNullable(cbProductoNombre).ifPresent(cb -> cb.setValue(nombre));

        ultimoIdSeleccionado = id;
        ultimoNombreSeleccionado = nombre;
        mostrarClavesAlternasParaProducto(id);

        return true;
    }

    public boolean setSeleccionPorClaveAlterna(String idAlterno) {
        if (cbClaveAlterna != null) {
            actualizandoDesdeClaveAlterna = true;
            try {
                if (cbClaveAlterna.getItems().contains(idAlterno)) {
                    cbClaveAlterna.setValue(idAlterno);
                    ultimaClaveAlternaSeleccionada = idAlterno;
                    return true;
                }
                return false;
            } finally {
                actualizandoDesdeClaveAlterna = false;
            }
        }
        return false;
    }

    public List<Map<String, String>> getTodosProductos() {
        return productos != null ? new ArrayList<>(productos) : new ArrayList<>();
    }

    public void recargarConProveedor(String idProveedor) {
        this.idProveedorFiltro = idProveedor;
        cargarDatosEnSegundoPlano();
    }

    /**
     * Cierra recursos
     */
    public void cerrar() {
        executorService.shutdown();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
