package Formularios.controller;

import Consultas.producto.model.producto;
import Consultas.producto.model.etiqueta;
import Consultas.producto.model.marca;
import Consultas.producto.model.model;
import Formularios.model.modelNuevoProducto;
import conexion.conexionFTP;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class controllerNuevoProducto {

    @FXML private TextField txtIdProducto;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<marca> cmbMarca;
    @FXML private TextField txtMaterial;
    @FXML private VBox contenedorUnidadMedida;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtInventarioMin;
    @FXML private Button btnGuardar;
    @FXML private ImageView imageView;
    @FXML private Button btnSeleccionarImagen;
    @FXML private ComboBox<etiqueta> cmbEtiqueta;

    private File imagenSeleccionada;
    private boolean modoEdicion = false;
    private String idEdicion = "";
    private String nombreImagenActual = "";
    private Runnable onSaved = null;
    private boolean reactivarProducto = false;
    private String idReactivacionPendiente = null;
    private boolean permitirNuevoDuplicado = false;
    private boolean reemplazarDuplicadoConNuevoId = false;
    private String idDuplicadoParaReemplazar = null;
    private boolean omitirConfirmacionReactivacion = false;

    private modelNuevoProducto modeloFormulario;
    private model modeloConsulta;

    private String productoIdCreado = "";
    private String productoNombreCreado = "";

    private static final int MAX_UNIDADES_MEDIDA = 5;
    private final List<UnidadMedidaRow> filasUnidadMedida = new ArrayList<>();
    private final ObservableList<String> medidasDisponibles = FXCollections.observableArrayList();
    private int contadorUnidades = 0;

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @FXML
    public void initialize() {
        // Inicializar modelos
        modeloFormulario = new modelNuevoProducto();
        modeloConsulta = new model();

        // Configurar ImageView
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");

        // Inicializar ComboBoxes
        cmbCategoria.setEditable(false); // solo elegir
        cmbMarca.setEditable(true);      // puede escribir
        cmbEtiqueta.setEditable(true);   // puede escribir

        // Cargar datos
        cargarCategorias();
        cargarMarcas();
        cargarEtiquetas();
        cargarUnidadesMedida();

        // Configurar cómo mostrar las etiquetas y marcas en los ComboBox
        configurarComboBoxes();

        inicializarUnidadesMedidaDinamicas();

        // Configurar atajo de teclado ENTER
        btnGuardar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ENTER -> guardarProducto();
                    }
                });
            }
        });
    }

    private void configurarComboBoxes() {
        // Configurar ComboBox de Marca
        cmbMarca.setConverter(new StringConverter<marca>() {
            @Override
            public String toString(marca marca) {
                return marca == null ? "" : marca.getNombre();
            }

            @Override
            public marca fromString(String string) {
                return cmbMarca.getItems().stream()
                        .filter(m -> m.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Configurar ComboBox de Etiqueta
        cmbEtiqueta.setConverter(new StringConverter<etiqueta>() {
            @Override
            public String toString(etiqueta etiqueta) {
                return etiqueta == null ? "" : etiqueta.getNombre();
            }

            @Override
            public etiqueta fromString(String string) {
                return cmbEtiqueta.getItems().stream()
                        .filter(e -> e.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void cargarCategorias() {
        ObservableList<String> items = FXCollections.observableArrayList(
                "cristaleria",
                "consumibles",
                "equipo",
                "reactivos"
        );
        cmbCategoria.setItems(items);
    }

    private void cargarMarcas() {
        ObservableList<marca> marcas = modeloConsulta.obtenerListaMarcas();
        cmbMarca.setItems(marcas);
    }

    private void cargarEtiquetas() {
        ObservableList<etiqueta> etiquetas = modeloConsulta.obtenerListaEtiquetas();
        cmbEtiqueta.setItems(etiquetas);
    }

    private void cargarUnidadesMedida() {
        ObservableList<String> unidades = modeloFormulario.obtenerUnidadesActivas();
        medidasDisponibles.setAll(unidades);
        actualizarMedidasEnFilas();
    }

    @FXML
    public void seleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen del producto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.jpg","*.jpeg","*.png","*.gif","*.bmp")
        );

        File archivo = fileChooser.showOpenDialog(null);
        if (archivo != null) {
            if (archivo.length() > 5*1024*1024) {
                mostrarError("La imagen no debe superar los 5MB");
                return;
            }
            imagenSeleccionada = archivo;
            imageView.setImage(new Image(archivo.toURI().toString()));
        }
    }

    public void cargarProducto(producto p) {
        if (p == null) return;
        modoEdicion = true;
        idEdicion = p.getIdProducto();
        nombreImagenActual = p.getUrlImagen();

        txtIdProducto.setText(p.getIdProducto());
        txtIdProducto.setDisable(true); // No permitir editar ID en modo edición

        txtNombre.setText(p.getNombreProducto());
        cmbCategoria.getSelectionModel().select(p.getCategoria());
        txtMaterial.setText(p.getMaterial());
        cargarUnidadesMedidaDesdeProducto(p.getUnidadMedida());
        txtDescripcion.setText(p.getDescripcion());
        txtInventarioMin.setText(String.valueOf(p.getInventarioMin()));

        // Cargar etiqueta
        if (p.getEtiqueta() != null && !p.getEtiqueta().isEmpty()) {
            etiqueta etiqueta = modeloFormulario.obtenerEtiquetaPorId(p.getEtiqueta());
            if (etiqueta != null) {
                cmbEtiqueta.getSelectionModel().select(etiqueta);
            }
        }

        // Cargar marca
        if (p.getMarca() != null && !p.getMarca().isEmpty()) {
            marca marca = modeloFormulario.obtenerMarcaPorId(p.getMarca());
            if (marca != null) {
                cmbMarca.getSelectionModel().select(marca);
            }
        }

        // Cargar imagen si existe
        if (nombreImagenActual != null && !nombreImagenActual.isEmpty()) {
            try {
                conexionFTP ftp = new conexionFTP();
                Image imagen = ftp.getImageFromFTP(nombreImagenActual);
                imageView.setImage(imagen);
            } catch (Exception e) {
                System.out.println("Error cargando imagen desde servidor: " + e.getMessage());
            }
        }

        btnGuardar.setText("Actualizar");
    }

    @FXML
    public void guardarProducto() {
        try {
            if (!modoEdicion) {
                reactivarProducto = false;
                idReactivacionPendiente = null;
                permitirNuevoDuplicado = false;
                reemplazarDuplicadoConNuevoId = false;
                idDuplicadoParaReemplazar = null;
                omitirConfirmacionReactivacion = false;
            } else if (idReactivacionPendiente == null) {
                reactivarProducto = false;
            }
            // Validar campos obligatorios
            if (modoEdicion) {
                // En modo edición, el ID ya está establecido
                if (idEdicion == null || idEdicion.isEmpty()) {
                    mostrarError("Error: No se encontró ID del producto a editar");
                    return;
                }
            } else {
                // En modo nuevo, validar ID
                String idProducto = txtIdProducto.getText().trim();
                if (idProducto.isEmpty()) {
                    mostrarError("El ID es obligatorio");
                    return;
                }

                // Verificar si el ID ya existe
                producto existente = modeloFormulario.buscarProductoPorId(idProducto);
                if (existente != null) {
                    if (esProductoDesactivado(existente)) {
                        prepararSobrescritura(existente);
                    } else {
                        mostrarError("Ese ID ya está registrado en otro producto.");
                        return;
                    }
                }
            }

            if (txtNombre.getText().trim().isEmpty()) {
                mostrarError("El nombre es requerido");
                return;
            }

            // Obtener valores de los campos
            String nombre = txtNombre.getText().trim();
            String categoria = cmbCategoria.getSelectionModel().getSelectedItem();
            if (categoria == null) categoria = "";

            String material = txtMaterial.getText().trim();
            String descripcion = txtDescripcion.getText().trim();
            String unidadMedida = construirUnidadMedida();
            if (unidadMedida == null) {
                return;
            }

            int inventarioMin = 0;
            if (!txtInventarioMin.getText().isEmpty()) {
                try {
                    inventarioMin = Integer.parseInt(txtInventarioMin.getText());
                } catch (NumberFormatException e) {
                    mostrarError("El inventario mínimo debe ser un número válido");
                    return;
                }
            }

            // Manejar etiqueta
            String etiquetaId = "";
            String etiquetaTexto = cmbEtiqueta.getEditor().getText().trim();
            if (!etiquetaTexto.isEmpty()) {
                etiquetaId = modeloFormulario.crearOActualizarEtiqueta(etiquetaTexto);
                if (etiquetaId == null) {
                    mostrarError("Error al procesar la etiqueta");
                    return;
                }
            }

            // Manejar marca
            String marcaId = "";
            String marcaTexto = cmbMarca.getEditor().getText().trim();
            if (!marcaTexto.isEmpty()) {
                marcaId = modeloFormulario.crearOActualizarMarca(marcaTexto);
                if (marcaId == null) {
                    mostrarError("Error al procesar la marca");
                    return;
                }
            }

            if (!modoEdicion) {
                producto duplicado = modeloFormulario.buscarProductoDuplicado(
                        txtIdProducto.getText().trim(),
                        nombre,
                        etiquetaId,
                        marcaId,
                        categoria,
                        material,
                        unidadMedida
                );
                if (duplicado != null && !permitirNuevoDuplicado) {
                    String idNuevo = txtIdProducto.getText().trim();
                    DecisionDuplicado decision = confirmarDuplicado(duplicado.getIdProducto(), idNuevo);
                    if (decision == DecisionDuplicado.CANCELAR) {
                        return;
                    }
                    if (decision == DecisionDuplicado.SOBRESCRIBIR) {
                        prepararSobrescritura(duplicado);
                        omitirConfirmacionReactivacion = true;
                    } else if (decision == DecisionDuplicado.NUEVO) {
                        permitirNuevoDuplicado = true;
                        reemplazarDuplicadoConNuevoId = true;
                        idDuplicadoParaReemplazar = duplicado.getIdProducto();
                    }
                }
            }

            if (!omitirConfirmacionReactivacion && reactivarProducto && idReactivacionPendiente != null) {
                if (!confirmarReactivacion(idReactivacionPendiente)) {
                    return;
                }
            }

            if (!modoEdicion && reemplazarDuplicadoConNuevoId && idDuplicadoParaReemplazar != null) {
                if (!modeloFormulario.eliminarProducto(idDuplicadoParaReemplazar)) {
                    mostrarError("No se pudo sobrescribir el producto existente.");
                    return;
                }
                reemplazarDuplicadoConNuevoId = false;
                idDuplicadoParaReemplazar = null;
            }

            // Manejar imagen
            String nombreImagen = nombreImagenActual;
            if (imagenSeleccionada != null) {
                String extension = getFileExtension(imagenSeleccionada.getName());
                String nuevoNombre = (modoEdicion ? idEdicion : txtIdProducto.getText().trim()) + extension;

                conexionFTP ftp = new conexionFTP();
                if (ftp.uploadFile(imagenSeleccionada, nuevoNombre)) {
                    nombreImagen = nuevoNombre;
                } else {
                    mostrarError("Error al subir la imagen al servidor");
                    return;
                }
            }

            // Crear objeto producto
            producto p = new producto();

            if (modoEdicion) {
                p.setIdProducto(idEdicion);
            } else {
                p.setIdProducto(txtIdProducto.getText().trim());
            }

            p.setNombreProducto(nombre);
            p.setCategoria(categoria);
            p.setEtiqueta(etiquetaId);
            p.setMarca(marcaId);
            p.setMaterial(material);
            p.setUnidadMedida(unidadMedida);
            p.setDescripcion(descripcion);
            p.setInventarioMin(inventarioMin);
            p.setUrlImagen(nombreImagen);
            if (modoEdicion) {
                producto existente = modeloFormulario.buscarProductoPorId(p.getIdProducto());
                if (existente != null) {
                    if (reactivarProducto) {
                        p.setEstado("activo");
                    } else {
                        p.setEstado(existente.getEstado());
                    }
                }
            } else {
                p.setEstado("activo");
            }

            boolean resultado;
            if (modoEdicion) {
                resultado = modeloFormulario.modificarProducto(p);
            } else {
                resultado = modeloFormulario.guardarProducto(p);
            }

            /*if (resultado) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText(modoEdicion ? "Producto actualizado" : "Producto guardado");
                alert.showAndWait();

                // Refrescar comboboxes si se agregaron nuevas etiquetas/marcas
                cargarMarcas();
                cargarEtiquetas();
                cargarUnidadesMedida();
                cargarUnidadesMedida();

                // Ejecutar callback para refrescar la tabla principal
                if (onSaved != null) onSaved.run();

                // Cerrar ventana
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            } else {
                mostrarError("No se pudo guardar el producto");
            }*/
            if (resultado) {
                // ALMACENAR DATOS DEL PRODUCTO CREADO
                if (!modoEdicion) {  // Solo para nuevos productos, no para ediciones
                    productoIdCreado = p.getIdProducto();
                    productoNombreCreado = p.getNombreProducto();
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText(modoEdicion ? "Producto actualizado" : "Producto guardado");
                alert.showAndWait();

                // Refrescar comboboxes si se agregaron nuevas etiquetas/marcas
                cargarMarcas();
                cargarEtiquetas();

                // Ejecutar callback para refrescar la tabla principal
                if (onSaved != null) onSaved.run();

                // Cerrar ventana
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            }

        } catch (Exception e) {
            mostrarError("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf(".");
        return idx == -1 ? ".jpg" : fileName.substring(idx).toLowerCase();
    }

    private void inicializarUnidadesMedidaDinamicas() {
        reiniciarUnidadesMedidaDinamicas();
    }

    private void reiniciarUnidadesMedidaDinamicas() {
        contenedorUnidadMedida.getChildren().clear();
        filasUnidadMedida.clear();
        contadorUnidades = 0;
        agregarFilaUnidadMedida(true);
    }

    private void agregarFilaUnidadMedida(boolean esInicial) {
        if (contadorUnidades >= MAX_UNIDADES_MEDIDA) {
            mostrarError("Solo se pueden agregar hasta " + MAX_UNIDADES_MEDIDA + " unidades de medida.");
            return;
        }

        HBox nuevaFila = new HBox(10);

        VBox vboxUnidad = new VBox(5);
        Label labelUnidad = new Label("Unidad");
        TextField txtUnidad = new TextField();
        txtUnidad.setTextFormatter(new TextFormatter<>(change -> {
            String nuevoTexto = change.getControlNewText();
            return nuevoTexto.matches("\\d*(\\.\\d*)?") ? change : null;
        }));
        vboxUnidad.getChildren().addAll(labelUnidad, txtUnidad);
        HBox.setHgrow(vboxUnidad, Priority.ALWAYS);

        VBox vboxMedida = new VBox(5);
        Label labelMedida = new Label("Medida");
        ComboBox<String> comboMedida = new ComboBox<>();
        comboMedida.setEditable(true);
        comboMedida.setItems(medidasDisponibles);
        vboxMedida.getChildren().addAll(labelMedida, comboMedida);
        HBox.setHgrow(vboxMedida, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(esInicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.getChildren().add(boton);
        vboxBoton.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (esInicial) {
            boton.setOnAction(event -> agregarFilaUnidadMedida(false));
        } else {
            boton.setOnAction(event -> eliminarFilaUnidadMedida(nuevaFila));
        }

        nuevaFila.getChildren().addAll(vboxUnidad, vboxMedida, vboxBoton);
        contenedorUnidadMedida.getChildren().add(nuevaFila);

        filasUnidadMedida.add(new UnidadMedidaRow(nuevaFila, txtUnidad, comboMedida));
        contadorUnidades++;
    }

    private void eliminarFilaUnidadMedida(HBox fila) {
        UnidadMedidaRow filaEncontrada = null;
        for (UnidadMedidaRow filaUnidad : filasUnidadMedida) {
            if (filaUnidad.contenedor == fila) {
                filaEncontrada = filaUnidad;
                break;
            }
        }
        if (filaEncontrada != null) {
            filasUnidadMedida.remove(filaEncontrada);
        }
        contenedorUnidadMedida.getChildren().remove(fila);
        contadorUnidades = Math.max(0, contadorUnidades - 1);
    }

    private void actualizarMedidasEnFilas() {
        for (UnidadMedidaRow fila : filasUnidadMedida) {
            fila.comboMedida.setItems(medidasDisponibles);
        }
    }

    private String construirUnidadMedida() {
        List<String> unidades = new ArrayList<>();
        for (UnidadMedidaRow fila : filasUnidadMedida) {
            String unidadTexto = fila.txtUnidad.getText().trim();
            String medidaTexto = fila.comboMedida.getEditor().getText().trim();

            if (unidadTexto.isEmpty() && medidaTexto.isEmpty()) {
                continue;
            }

            if (unidadTexto.isEmpty() || medidaTexto.isEmpty()) {
                mostrarError("Debe completar la unidad y la medida en cada fila.");
                return null;
            }

            String medidaGuardada = modeloFormulario.crearOActualizarUnidad(medidaTexto);
            if (medidaGuardada == null) {
                mostrarError("Error al procesar la unidad de medida");
                return null;
            }
            unidades.add(unidadTexto + medidaGuardada);
        }

        return String.join(", ", unidades);
    }

    private void cargarUnidadesMedidaDesdeProducto(String unidadMedida) {
        reiniciarUnidadesMedidaDinamicas();
        if (unidadMedida == null || unidadMedida.isBlank()) {
            return;
        }

        String[] partes = unidadMedida.split(",");
        int index = 0;
        for (String parte : partes) {
            if (index >= MAX_UNIDADES_MEDIDA) {
                break;
            }
            String texto = parte.trim();
            if (texto.isEmpty()) {
                continue;
            }

            if (index > 0) {
                agregarFilaUnidadMedida(false);
            }
            UnidadMedidaRow fila = filasUnidadMedida.get(index);
            String unidadTexto = extraerUnidad(texto);
            String medidaTexto = extraerMedida(texto);
            fila.txtUnidad.setText(unidadTexto);
            fila.comboMedida.getEditor().setText(medidaTexto);
            fila.comboMedida.setValue(medidaTexto);
            index++;
        }
    }

    private String extraerUnidad(String texto) {
        if (texto == null) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(.*)$")
                .matcher(texto);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extraerMedida(String texto) {
        if (texto == null) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(.*)$")
                .matcher(texto);
        if (matcher.matches()) {
            return matcher.group(2).trim();
        }
        return texto.trim();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public String getProductoIdCreado() {
        return productoIdCreado;
    }

    public String getProductoNombreCreado() {
        return productoNombreCreado;
    }

    // AÑADIR este método para verificar si se creó un producto
    public boolean isProductoCreado() {
        return !productoIdCreado.isEmpty() && !productoNombreCreado.isEmpty();
    }

    private boolean esProductoDesactivado(producto productoExistente) {
        if (productoExistente == null) {
            return false;
        }
        String estado = productoExistente.getEstado();
        if (estado == null) {
            return false;
        }
        String estadoNormalizado = estado.trim().toLowerCase();
        return !estadoNormalizado.equals("activo");
    }

    private void prepararSobrescritura(producto productoExistente) {
        modoEdicion = true;
        idEdicion = productoExistente.getIdProducto();
        reactivarProducto = esProductoDesactivado(productoExistente);
        idReactivacionPendiente = reactivarProducto ? idEdicion : null;
        txtIdProducto.setText(idEdicion);
        txtIdProducto.setDisable(true);
    }

    private boolean confirmarReactivacion(String idProducto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Producto desactivado");
        alert.setHeaderText("Este producto ya está registrado pero fue desactivado.");
        alert.setContentText("¿Deseas reactivarlo con el ID " + idProducto + "?");
        ButtonType botonAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(botonAceptar, botonCancelar);
        return alert.showAndWait().filter(botonAceptar::equals).isPresent();
    }

    private DecisionDuplicado confirmarDuplicado(String idExistente, String idNuevo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Producto duplicado");
        alert.setHeaderText("Ya existe un producto con los mismos datos.");
        alert.setContentText("ID existente: " + idExistente + "\nID nuevo: " + idNuevo
                + "\n\n¿Qué deseas hacer?");

        ButtonType botonSobrescribir = new ButtonType("Sobrescribir", ButtonBar.ButtonData.OK_DONE);
        ButtonType botonNuevo = new ButtonType("Nuevo", ButtonBar.ButtonData.OTHER);
        ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(botonSobrescribir, botonNuevo, botonCancelar);

        return alert.showAndWait()
                .map(respuesta -> {
                    if (respuesta == botonSobrescribir) {
                        return DecisionDuplicado.SOBRESCRIBIR;
                    }
                    if (respuesta == botonNuevo) {
                        return DecisionDuplicado.NUEVO;
                    }
                    return DecisionDuplicado.CANCELAR;
                })
                .orElse(DecisionDuplicado.CANCELAR);
    }

    private enum DecisionDuplicado {
        CANCELAR,
        SOBRESCRIBIR,
        NUEVO
    }

    private static class UnidadMedidaRow {
        private final HBox contenedor;
        private final TextField txtUnidad;
        private final ComboBox<String> comboMedida;

        private UnidadMedidaRow(HBox contenedor, TextField txtUnidad, ComboBox<String> comboMedida) {
            this.contenedor = contenedor;
            this.txtUnidad = txtUnidad;
            this.comboMedida = comboMedida;
        }
    }
}
