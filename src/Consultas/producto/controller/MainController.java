package Consultas.producto.controller;

import Compartido.exportar.exportarPlantilla;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import Compartido.importar.importador;
import Compartido.exportar.exportador;
import Consultas.producto.model.producto;
import Consultas.producto.model.model;
import Formularios.controller.controllerNuevoProducto;
import conexion.conexionFTP;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private Region expansor;
    @FXML private TextField buscador;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<producto> contenidoTabla;
    @FXML private TableColumn<producto, Void> colSelect;
    @FXML private TableColumn<producto, String> colIdProducto;
    @FXML private TableColumn<producto, String> colNombre;
    @FXML private TableColumn<producto, String> colCategoria;
    @FXML private TableColumn<producto, String> colEtiqueta;
    @FXML private TableColumn<producto, String> colMarca;
    @FXML private TableColumn<producto, String> colMaterial;
    @FXML private TableColumn<producto, String> colUnidadMedida;
    @FXML private TableColumn<producto, String> colDescripcion;
    @FXML private TableColumn<producto, String> colInventarioMin;
    @FXML private TableColumn<producto, String> colImagen;
    @FXML private ImageView previewImage;
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private model productoModel;

    // Mapas concurrentes para alta velocidad y cache
    private Map<String, String> mapEtiquetas = new ConcurrentHashMap<>();
    private Map<String, String> mapMarcas = new ConcurrentHashMap<>();
    private final Map<String, Image> cacheImagenes = new ConcurrentHashMap<>();
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();

    @FXML
    public void initialize() {
        productoModel = new model();

        HBox.setHgrow(expansor, Priority.ALWAYS);
        expansor.setMinWidth(10);

        buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
        buscador.maxHeightProperty().bind(root.heightProperty().multiply(0.04));

        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
        contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.87));
        contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

        previewImage.fitWidthProperty().bind(root.widthProperty().multiply(0.07));
        previewImage.fitHeightProperty().bind(root.heightProperty().multiply(0.15));
        previewImage.setPreserveRatio(false);

        // Configuración columnas
        colIdProducto.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getIdProducto())));
        colNombre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreProducto()));
        colCategoria.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoria()));
        colMaterial.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMaterial()));
        colUnidadMedida.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUnidadMedida()));
        colDescripcion.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));
        colInventarioMin.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getInventarioMin())));
        colImagen.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUrlImagen()));

        TableColumn[] columnas = { colSelect, colIdProducto, colNombre, colCategoria, colEtiqueta, colMarca, colMaterial, colUnidadMedida, colDescripcion, colInventarioMin, colImagen};
        for (TableColumn col : columnas) col.setStyle("-fx-alignment: CENTER;");

        colSelect.setCellFactory(col -> new TableCell<>() {
            private final Button btn;
            {
                btn = new Button();
                ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                img.setFitWidth(18);
                img.setFitHeight(18);
                img.setPreserveRatio(true);
                btn.setGraphic(img);
                btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                if (soloLectura) {
                    btn.setVisible(false);
                    btn.setManaged(false);
                } else {
                    btn.setOnAction(e -> eliminarProducto(getTableView().getItems().get(getIndex())));
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        contenidoTabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> mostrarImagenProducto(newSel));

        contenidoTabla.setRowFactory(tv -> {
            TableRow<producto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    if (soloLectura) return;
                    editarProducto(row.getItem());
                }
            });
            return row;
        });

        contenidoTabla.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                if (soloLectura) return;
                producto seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    editarProducto(seleccionado);
                }
            }
        });

        // Configurar listener para el buscador (búsqueda en tiempo real)
        buscador.textProperty().addListener((observable, oldValue, newValue) -> {
            buscarProductos(newValue);
        });

        preloadDatosUltraRapido();
        RefrescoHelper.setVistaActual("productos");
        RefrescoHelper.registrarRefresco("productos", this::actualizarProductos);
        configurarAtajosTeclado();

    }

    private void actualizarProductos() {
        System.out.println("=== EJECUTANDO ACTUALIZACIÓN DE PRODUCTOS ===");

        // 1. Crear NUEVA instancia del modelo (esto forzará nueva conexión)
        productoModel = new model();
        System.out.println("Nuevo modelo creado");

        // 2. Limpiar todas las cachés
        cacheImagenes.clear();
        mapEtiquetas.clear();
        mapMarcas.clear();

        // 3. Limpiar la UI
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            previewImage.setImage(null);
            contenidoTabla.setItems(FXCollections.observableArrayList()); // Limpiar tabla temporalmente
        });

        // 4. Pequeña pausa para que se limpie la UI
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Recargar los datos con la nueva instancia
        preloadDatosUltraRapido();

        System.out.println("=== ACTUALIZACIÓN COMPLETADA ===");
    }

    private void preloadDatosUltraRapido() {
        Task<Void> preloadTask = new Task<>() {
            @Override
            protected Void call() {
                Map<String, String> etiquetasMap = productoModel.obtenerMapaEtiquetas();
                Map<String, String> marcasMap = productoModel.obtenerMapaMarcas();

                Platform.runLater(() -> {
                    // Actualizamos los mapas
                    mapEtiquetas.clear();
                    mapEtiquetas.putAll(etiquetasMap);

                    mapMarcas.clear();
                    mapMarcas.putAll(marcasMap);

                    // Cargamos productos
                    cargarProductosEnTabla();
                    configurarColumnasConMapas();
                });
                return null;
            }
        };
        new Thread(preloadTask).start();
    }

    private void cargarProductosEnTabla() {
        ObservableList<producto> productos = productoModel.obtenerProductos();
        contenidoTabla.setItems(productos);
    }

    private void configurarColumnasConMapas() {
        colEtiqueta.setCellValueFactory(cd -> {
            String id = cd.getValue().getEtiqueta();
            return new javafx.beans.property.SimpleStringProperty(mapEtiquetas.getOrDefault(id, ""));
        });
        colMarca.setCellValueFactory(cd -> {
            String id = cd.getValue().getMarca();
            return new javafx.beans.property.SimpleStringProperty(mapMarcas.getOrDefault(id, ""));
        });
    }

    private void eliminarProducto(producto p) {
        int entradas = productoModel.contarEntradasPorProducto(p.getIdProducto());
        int salidas = productoModel.contarSalidasPorProducto(p.getIdProducto());
        int detallesEntrada = productoModel.contarDetallesEntradaPorProducto(p.getIdProducto());
        int detallesSalida = productoModel.contarDetallesSalidaPorProducto(p.getIdProducto());
        int articulosEntrada = productoModel.contarArticulosEntradaPorProducto(p.getIdProducto());
        int articulosSalida = productoModel.contarArticulosSalidaPorProducto(p.getIdProducto());
        int clavesActivas = productoModel.contarClavesPorProducto(p.getIdProducto());

        if (entradas > 0 || salidas > 0 || detallesEntrada > 0 || detallesSalida > 0
                || articulosEntrada > 0 || articulosSalida > 0 || clavesActivas > 0) {
            StringBuilder motivo = new StringBuilder(
                    "No se puede desactivar el producto porque tiene registros relacionados (activos, pendientes o disponibles):");
            if (entradas > 0) {
                motivo.append("\n- Entradas: ").append(entradas);
            }
            if (detallesEntrada > 0) {
                motivo.append("\n- Detalles de entrada: ").append(detallesEntrada);
            }
            if (articulosEntrada > 0) {
                motivo.append("\n- Artículos de entradas: ").append(articulosEntrada);
            }
            if (salidas > 0) {
                motivo.append("\n- Salidas: ").append(salidas);
            }
            if (detallesSalida > 0) {
                motivo.append("\n- Detalles de salida: ").append(detallesSalida);
            }
            if (articulosSalida > 0) {
                motivo.append("\n- Artículos de salidas: ").append(articulosSalida);
            }
            if (clavesActivas > 0) {
                motivo.append("\n- Este producto tiene clave(s) alterna(s) vicnulada(s): ").append(clavesActivas);
            }
            new Alert(Alert.AlertType.WARNING, motivo.toString()).showAndWait();
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea desactivar este producto?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (productoModel.eliminarProducto(p.getIdProducto())) {
                    eliminarImagenProducto(p);
                    contenidoTabla.getItems().remove(p);
                    new Alert(Alert.AlertType.INFORMATION, "Producto desactivado correctamente").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo desactivar el producto.").showAndWait();
                }
            }
        });
    }

    private void eliminarImagenProducto(producto p) {
        String urlImagen = p.getUrlImagen();
        if (urlImagen == null || urlImagen.isBlank()) {
            return;
        }

        conexionFTP ftp = new conexionFTP();
        ftp.deleteImageFromFTP(urlImagen);
        cacheImagenes.remove(urlImagen);
        if (previewImage.getImage() != null && urlImagen.equals(p.getUrlImagen())) {
            previewImage.setImage(null);
        }
    }

    // Mtodo para buscar un producto por su ID
    private void mostrarImagenProducto(producto p) {
        previewImage.setImage(null);
        if (p == null || p.getUrlImagen() == null || p.getUrlImagen().isEmpty()) return;

        if (cacheImagenes.containsKey(p.getUrlImagen())) {
            previewImage.setImage(cacheImagenes.get(p.getUrlImagen()));
            return;
        }

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                conexionFTP ftp = new conexionFTP();
                return ftp.getImageFromFTP(p.getUrlImagen());
            }
        };
        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            cacheImagenes.put(p.getUrlImagen(), img);
            previewImage.setImage(img);
        });
        task.setOnFailed(e -> previewImage.setImage(null));
        new Thread(task).start();
    }

    private void editarProducto(producto p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();
            ctrl.cargarProducto(p);
            ctrl.setOnSaved(this::preloadDatosUltraRapido);
            Stage stage = new Stage();
            stage.initOwner(contenidoTabla.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Editar producto");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void formularioNuevoProducto() {
        if (soloLectura) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();
            ctrl.setOnSaved(this::preloadDatosUltraRapido);
            Stage stage = new Stage();
            stage.initOwner(contenidoTabla.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo producto");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            if (ctrl.isProductoCreado()) {
                preloadDatosUltraRapido();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportarDatos() {
        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Exportar");
        dialogo.setHeaderText("Seleccione el formato:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) exportador.exportarTabla(contenidoTabla, "Productos", "pdf");
            else if (res == btnExcel) exportador.exportarTabla(contenidoTabla, "Productos", "excel");
        });
    }

    @FXML
    private void importarDatos() {
        if (soloLectura) {
            return;
        }
        importador.importarProductosExcel();
        mapEtiquetas = new ConcurrentHashMap<>();
        mapMarcas = new ConcurrentHashMap<>();
        preloadDatosUltraRapido();
    }

    private void buscarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProductosEnTabla();
        } else {
            ObservableList<producto> productos = productoModel.busquedaMultipleProductos(texto);
            contenidoTabla.setItems(productos);
        }
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("productos");
    }

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.N) formularioNuevoProducto();
            else if (event.getCode() == KeyCode.E) {
                producto pSel = contenidoTabla.getSelectionModel().getSelectedItem();
                if (pSel != null) eliminarProducto(pSel);
            } else if (event.getCode() == KeyCode.I) importarDatos();
            else if (event.getCode() == KeyCode.R) exportarDatos();
            else if (event.getCode() == KeyCode.D) exportarPlantilla();
            else return;
            event.consume();
        });
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

}
