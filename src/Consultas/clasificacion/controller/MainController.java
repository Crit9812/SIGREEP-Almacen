package Consultas.clasificacion.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.sesion.PermisosRol;
import Consultas.clasificacion.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista {

    // ================== CONTENEDORES ==================
    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private VBox contenedorTabla;

    // ================== TABLAS ==================
    @FXML private TableView<marcas> contenidoTablaMarcas;
    @FXML private TableColumn<marcas, Void> colSelect;
    @FXML private TableColumn<marcas, Integer> colIDMarca;
    @FXML private TableColumn<marcas, String> colNombreMarca;

    @FXML private TableView<etiquetas> contenidoTablaEtiquetas;
    @FXML private TableColumn<etiquetas, Void> colSelectEtiqueta;
    @FXML private TableColumn<etiquetas, Integer> colIDEtiqueta;
    @FXML private TableColumn<etiquetas, String> colNombreEtiqueta;

    @FXML private TableView<ubicaciones> contenidoTablaUbicaciones;
    @FXML private TableColumn<ubicaciones, Void> colSelectUbicaciones;
    @FXML private TableColumn<ubicaciones, Integer> colIDUbicaciones;
    @FXML private TableColumn<ubicaciones, String> colNombreUbicaciones;

    @FXML private TableView<unidades_Medida> contenidoTablaUM;
    @FXML private TableColumn<unidades_Medida, Void> colSelectUM;
    @FXML private TableColumn<unidades_Medida, Integer> colIDUM;
    @FXML private TableColumn<unidades_Medida, String> colNombreUM;

    // ================== OTROS ==================
    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final boolean soloLectura = PermisosRol.esSupervisorOUsuario();
    private final model model = new model();

    // Servicios para carga asíncrona
    private DataLoadService dataLoadService;

    // Cache de datos
    private ObservableList<marcas> cacheMarcas = FXCollections.observableArrayList();
    private ObservableList<etiquetas> cacheEtiquetas = FXCollections.observableArrayList();
    private ObservableList<ubicaciones> cacheUbicaciones = FXCollections.observableArrayList();
    private ObservableList<unidades_Medida> cacheUM = FXCollections.observableArrayList();
    private boolean esperandoSubAtajoAgregar = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            configurarLayout();
            configurarTablas();
            configurarDobleClick();
            configurarEnter();

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));

            RefrescoHelper.setVistaActual("clasificacion");
            RefrescoHelper.registrarRefresco("clasificacion", this::cargarDatos);
            configurarAtajosTeclado();

            dataLoadService = new DataLoadService();
            configurarDataLoadService();
            cargarDatos();
        });
    }

    // ================== SERVICIO DE CARGA DE DATOS ==================
    private class DataLoadService extends Service<Void> {
        private ObservableList<marcas> marcasResult;
        private ObservableList<etiquetas> etiquetasResult;
        private ObservableList<ubicaciones> ubicacionesResult;
        private ObservableList<unidades_Medida> umResult;

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // Crear CountDownLatch para sincronizar las tareas
                    CountDownLatch latch = new CountDownLatch(4);

                    // Variables para resultados
                    marcasResult = FXCollections.observableArrayList();
                    etiquetasResult = FXCollections.observableArrayList();
                    ubicacionesResult = FXCollections.observableArrayList();
                    umResult = FXCollections.observableArrayList();

                    // Tarea para marcas
                    Thread marcaThread = new Thread(() -> {
                        try {
                            marcasResult.setAll(model.obtenerMarcas());
                        } catch (Exception e) {
                            System.err.println("Error cargando marcas: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });

                    // Tarea para etiquetas
                    Thread etiquetaThread = new Thread(() -> {
                        try {
                            etiquetasResult.setAll(model.obtenerEtiquetas());
                        } catch (Exception e) {
                            System.err.println("Error cargando etiquetas: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });

                    // Tarea para ubicaciones
                    Thread ubicacionThread = new Thread(() -> {
                        try {
                            ubicacionesResult.setAll(model.obtenerUbicaciones());
                        } catch (Exception e) {
                            System.err.println("Error cargando ubicaciones: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });

                    // Tarea para unidades de medida
                    Thread umThread = new Thread(() -> {
                        try {
                            umResult.setAll(model.obtenerUM());
                        } catch (Exception e) {
                            System.err.println("Error cargando unidades de medida: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });

                    // Iniciar todas las tareas
                    marcaThread.start();
                    etiquetaThread.start();
                    ubicacionThread.start();
                    umThread.start();
                    // Esperar a que todas terminen
                    latch.await();

                    return null;
                }
            };
        }
    }

    private void configurarDataLoadService() {
        dataLoadService.setOnSucceeded(e -> {
            // Actualizar cache con los resultados
            if (dataLoadService.marcasResult != null) {
                cacheMarcas = dataLoadService.marcasResult;
            }
            if (dataLoadService.etiquetasResult != null) {
                cacheEtiquetas = dataLoadService.etiquetasResult;
            }
            if (dataLoadService.ubicacionesResult != null) {
                cacheUbicaciones = dataLoadService.ubicacionesResult;
            }
            if (dataLoadService.umResult != null) {
                cacheUM = dataLoadService.umResult;
            }

            Platform.runLater(() -> {
                contenidoTablaMarcas.setItems(cacheMarcas);
                contenidoTablaEtiquetas.setItems(cacheEtiquetas);
                contenidoTablaUbicaciones.setItems(cacheUbicaciones);
                contenidoTablaUM.setItems(cacheUM);
            });
        });

        dataLoadService.setOnFailed(e -> {
            Platform.runLater(() -> {
                mostrarError("Error al cargar datos: " + dataLoadService.getException().getMessage());
                // Cargar datos vacíos para evitar excepciones
                contenidoTablaMarcas.setItems(FXCollections.observableArrayList());
                contenidoTablaEtiquetas.setItems(FXCollections.observableArrayList());
                contenidoTablaUbicaciones.setItems(FXCollections.observableArrayList());
                contenidoTablaUM.setItems(FXCollections.observableArrayList());
            });
        });
    }

    private void cargarDatos() {
        if (dataLoadService != null && dataLoadService.isRunning()) {
            dataLoadService.cancel();
        }
        dataLoadService.restart();
    }

    // ================== CONFIGURAR LAYOUT ==================
    private void configurarLayout() {
        contenidoTablaMarcas.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.86));
        contenidoTablaEtiquetas.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.86));
        contenidoTablaUbicaciones.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.86));
        contenidoTablaUM.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.86));
    }

    private void configurarTablas() {
        // MARCAS
        colIDMarca.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreMarca.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelect.setCellFactory(c -> crearBotonEliminarMarca());

        // ETIQUETAS
        colIDEtiqueta.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreEtiqueta.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelectEtiqueta.setCellFactory(c -> crearBotonEliminarEtiqueta());

        // UBICACIONES
        colIDUbicaciones.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreUbicaciones.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelectUbicaciones.setCellFactory(c -> crearBotonEliminarUbicacion());

        // UNIDADES DE MEDIDA
        colIDUM.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreUM.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelectUM.setCellFactory(c -> crearBotonEliminarUM());
    }

    // ================== DOBLE CLICK ==================
    private void configurarDobleClick() {
        dobleClick(contenidoTablaMarcas, this::editarMarca);
        dobleClick(contenidoTablaEtiquetas, this::editarEtiqueta);
        dobleClick(contenidoTablaUbicaciones, this::editarUbicacion);
        dobleClick(contenidoTablaUM, this::editarUM);
    }

    private <T> void dobleClick(TableView<T> tabla, Consumer<T> accion) {
        tabla.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    accion.accept(row.getItem());
                }
            });
            return row;
        });
    }

    // ================== ENTER PARA EDITAR ==================
    private void configurarEnter() {
        // Configurar ENTER para cada tabla
        configurarEnterEnTabla(contenidoTablaMarcas, this::editarMarca);
        configurarEnterEnTabla(contenidoTablaEtiquetas, this::editarEtiqueta);
        configurarEnterEnTabla(contenidoTablaUbicaciones, this::editarUbicacion);
        configurarEnterEnTabla(contenidoTablaUM, this::editarUM);
    }

    private <T> void configurarEnterEnTabla(TableView<T> tabla, Consumer<T> accion) {
        tabla.setOnKeyPressed(event -> {
            // Verificar si se presionó ENTER
            if (event.getCode().toString().equals("ENTER")) {
                    if (soloLectura) return;
                T seleccionado = tabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    accion.accept(seleccionado);
                }
            }
        });
    }

    // ================== EDITAR ==================
    private void editarMarca(marcas m) {
        editarGenerico("Editar marca", m.getNombre(),
                nombre -> {
                    if (model.actualizarMarca(m.getId(), nombre)) {
                        Platform.runLater(() -> {
                                m.setNombre(nombre);
                                contenidoTablaMarcas.refresh();
                        });
                    }
                },
                m::setNombre,
                contenidoTablaMarcas);
    }

    private void editarEtiqueta(etiquetas e) {
        editarGenerico("Editar etiqueta", e.getNombre(),
                nombre -> {
                    if (model.actualizarEtiqueta(e.getId(), nombre)) {
                        Platform.runLater(() -> {
                            e.setNombre(nombre);
                            contenidoTablaEtiquetas.refresh();
                        });
                    }
                },
                e::setNombre,
                contenidoTablaEtiquetas);
    }

    private void editarUbicacion(ubicaciones u) {
        editarGenerico("Editar ubicación", u.getNombre(),
                nombre -> {
                    if (model.actualizarUbicacion(u.getId(), nombre)) {
                        Platform.runLater(() -> {
                            u.setNombre(nombre);
                            contenidoTablaUbicaciones.refresh();
                        });
                    }
                },
                u::setNombre,
                contenidoTablaUbicaciones);
    }

    private void editarUM(unidades_Medida u) {
        editarGenerico("Editar unidad de medida", u.getNombre(),
                nombre -> {
                    if (model.actualizarUM(u.getId(), nombre)) {
                        Platform.runLater(() -> {
                            u.setNombre(nombre);
                            contenidoTablaUM.refresh();
                        });
                    }
                },
                u::setNombre,
                contenidoTablaUM);
    }

    private <T> void editarGenerico(
            String titulo,
            String valorActual,
            Consumer<String> actualizar,
            Consumer<String> setter,
            TableView<T> tabla
    ) {
        TextInputDialog dialog = new TextInputDialog(valorActual);
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);

        // Obtener el campo de texto del diálogo
        TextField inputField = dialog.getEditor();

        // Configurar ENTER para guardar
        inputField.setOnAction(e -> {
            String nombre = inputField.getText().trim();
            if (!nombre.isEmpty()) {
                // Ejecutar la actualización
                new Thread(() -> {
                    actualizar.accept(nombre);
                }).start();
                // Cerrar el diálogo
                dialog.getDialogPane().getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst()
                        .ifPresent(okButton -> dialog.setResult(nombre));
            }
        });

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                // Ejecutar la actualización en un hilo separado
                new Thread(() -> {
                    actualizar.accept(nombre.trim());
                }).start();
            }
        });
    }

    // ================== ELIMINAR ==================
    private TableCell<marcas, Void> crearBotonEliminarMarca() {
        return crearBotonEliminar(
                m -> model.contarProductosPorMarca(m.getId()),
                m -> {
                    if (model.eliminarMarca(m.getId())) {
                        Platform.runLater(() -> {
                            cacheMarcas.remove(m);
                            contenidoTablaMarcas.getItems().remove(m);
                        });
                    }
                },
                marcas::getNombre,
                "marca",
                contenidoTablaMarcas
        );
    }

    private TableCell<etiquetas, Void> crearBotonEliminarEtiqueta() {
        return crearBotonEliminar(
                e -> model.contarProductosPorEtiqueta(e.getId()),
                e -> {
                    if (model.eliminarEtiqueta(e.getId())) {
                        Platform.runLater(() -> {
                            cacheEtiquetas.remove(e);
                            contenidoTablaEtiquetas.getItems().remove(e);
                        });
                    }
                },
                etiquetas::getNombre,
                "etiqueta",
                contenidoTablaEtiquetas
        );
    }

    private TableCell<ubicaciones, Void> crearBotonEliminarUbicacion() {
        return crearBotonEliminar(
                u -> model.contarProductosPorUbicacion(u.getId()),
                u -> {
                    if (model.eliminarUbicacion(u.getId())) {
                        Platform.runLater(() -> {
                            cacheUbicaciones.remove(u);
                            contenidoTablaUbicaciones.getItems().remove(u);
                        });
                    }
                },
                ubicaciones::getNombre,
                "ubicación",
                contenidoTablaUbicaciones
        );
    }

    private TableCell<unidades_Medida, Void> crearBotonEliminarUM() {
        return crearBotonEliminar(
                u -> model.contarProductosPorUM(u.getNombre()),
                u -> {
                    if (model.eliminarUM(u.getId())) {
                        Platform.runLater(() -> {
                            cacheUM.remove(u);
                            contenidoTablaUM.getItems().remove(u);
                        });
                    }
                },
                unidades_Medida::getNombre,
                "unidad de medida",
                contenidoTablaUM
        );
    }

    private <T> TableCell<T, Void> crearBotonEliminar(
            Function<T, Integer> contar,
            Consumer<T> eliminar,
            Function<T, String> obtenerNombre,
            String tipo,
            TableView<T> tabla
    ) {
        return new TableCell<>() {

            private final Button btn = crearBoton();

            {
                if (soloLectura) {
                    btn.setVisible(false);
                    btn.setManaged(false);
                } else {
                    btn.setOnAction(e -> {
                    T item = getTableView().getItems().get(getIndex());
                    int vinculados = contar.apply(item);
                    String nombre = obtenerNombre.apply(item);

                    if (vinculados > 0) {
                        String tipoRelacion = "ubicación".equals(tipo) ? "artículo(s)" : "producto(s)";
                        mostrarAlertaWarning("No se puede eliminar",
                                "No se puede desactivar la " + tipo + " \"" + nombre + "\" porque tiene "
                                        + vinculados + " " + tipoRelacion + " relacionado(s).");
                        return;
                    }

                    if (!confirmar("Desactivar " + tipo, nombre)) return;

                    // Ejecutar la eliminación en un hilo separado
                    new Thread(() -> {
                        eliminar.accept(item);
                    }).start();
                    });
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private <T> void ejecutarEliminacionConValidaciones(
            T item,
            Function<T, Integer> contar,
            Consumer<T> eliminar,
            Function<T, String> obtenerNombre,
            String tipo
    ) {
        if (soloLectura || item == null) {
            return;
        }

        int vinculados = contar.apply(item);
        String nombre = obtenerNombre.apply(item);

        if (vinculados > 0) {
            String tipoRelacion = "ubicación".equals(tipo) ? "artículo(s)" : "producto(s)";
            mostrarAlertaWarning("No se puede eliminar",
                    "No se puede desactivar la " + tipo + " \"" + nombre + "\" porque tiene "
                            + vinculados + " " + tipoRelacion + " relacionado(s).");
            return;
        }

        if (!confirmar("Desactivar " + tipo, nombre)) {
            return;
        }

        new Thread(() -> eliminar.accept(item)).start();
    }

    private Button crearBoton() {
        ImageView img = new ImageView(new Image(
                getClass().getResourceAsStream("/img/eliminar.png")
        ));
        img.setFitWidth(18);
        img.setFitHeight(18);

        Button btn = new Button();
        btn.setGraphic(img);
        btn.setStyle("-fx-background-color: #333;");
        return btn;
    }

    // ================== CONFIRMACIONES ==================
    private boolean confirmar(String titulo, String nombre) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText("¿Desea eliminar: " + nombre + "?");
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private boolean confirmarConVinculos(String tipo, String nombre, int cantidad) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Elemento vinculado");
        a.setHeaderText(null);
        a.setContentText(
                "La " + tipo + " \"" + nombre + "\" está vinculada a "
                        + cantidad + " producto(s).\n\n¿Desea eliminarla?"
        );
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    // ================== AGREGAR ==================
    @FXML private void agregarMarca() {
        if (soloLectura) {
            return;
        }
        agregar("Agregar marca", model::insertarMarca, model::existeMarca, contenidoTablaMarcas);
    }

    @FXML private void agregarEtiqueta() {
        if (soloLectura) {
            return;
        }
        agregar("Agregar etiqueta", model::insertarEtiqueta, model::existeEtiqueta, contenidoTablaEtiquetas);
    }

    @FXML private void agregarUbicacion() {
        if (soloLectura) {
            return;
        }
        agregar("Agregar ubicación", model::insertarUbicacion, model::existeUbicacion, contenidoTablaUbicaciones);
    }

    @FXML private void agregarUM() {
        if (soloLectura) {
            return;
        }
        agregar("Agregar unidad de medida", model::insertarUM, model::existeUM, contenidoTablaUM);
    }

    private <T> void agregar(String titulo, Consumer<String> insertar, Function<String, Boolean> existe, TableView<T> tabla) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                if (existe.apply(nombre.trim())) {
                    mostrarError("El registro ya existe.");
                    return;
                }
                // Ejecutar la inserción en un hilo separado
                new Thread(() -> {
                    insertar.accept(nombre.trim());
                    // Recargar datos después de insertar
                    Platform.runLater(this::cargarDatos);
                }).start();
            }
        });
    }

    // ================== MENSAJES DE ERROR ==================
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAlertaWarning(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ================== CLEANUP ==================

    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (event.getCode() == KeyCode.N && event.isControlDown()) {
                esperandoSubAtajoAgregar = true;
                event.consume();
                return;
            }

            if (esperandoSubAtajoAgregar) {
                switch (event.getCode()) {
                    case M -> agregarMarca();
                    case E -> agregarEtiqueta();
                    case B -> agregarUbicacion();
                    case U -> agregarUM();
                    default -> {
                        esperandoSubAtajoAgregar = false;
                        return;
                    }
                }

                esperandoSubAtajoAgregar = false;
                event.consume();
                return;
            }

            if (event.getCode() == KeyCode.E && event.isControlDown()) {
                eliminarSeleccionClasificacion();
                event.consume();
            }
        });
    }

    private void eliminarSeleccionClasificacion() {
        if (contenidoTablaMarcas.isFocused()) {
            marcas m = contenidoTablaMarcas.getSelectionModel().getSelectedItem();
            ejecutarEliminacionConValidaciones(m,
                    x -> model.contarProductosPorMarca(x.getId()),
                    x -> {
                        if (model.eliminarMarca(x.getId())) {
                            Platform.runLater(() -> {
                                cacheMarcas.remove(x);
                                contenidoTablaMarcas.getItems().remove(x);
                            });
                        }
                    },
                    marcas::getNombre,
                    "marca");
        } else if (contenidoTablaEtiquetas.isFocused()) {
            etiquetas e = contenidoTablaEtiquetas.getSelectionModel().getSelectedItem();
            ejecutarEliminacionConValidaciones(e,
                    x -> model.contarProductosPorEtiqueta(x.getId()),
                    x -> {
                        if (model.eliminarEtiqueta(x.getId())) {
                            Platform.runLater(() -> {
                                cacheEtiquetas.remove(x);
                                contenidoTablaEtiquetas.getItems().remove(x);
                            });
                        }
                    },
                    etiquetas::getNombre,
                    "etiqueta");
        } else if (contenidoTablaUbicaciones.isFocused()) {
            ubicaciones u = contenidoTablaUbicaciones.getSelectionModel().getSelectedItem();
            ejecutarEliminacionConValidaciones(u,
                    x -> model.contarProductosPorUbicacion(x.getId()),
                    x -> {
                        if (model.eliminarUbicacion(x.getId())) {
                            Platform.runLater(() -> {
                                cacheUbicaciones.remove(x);
                                contenidoTablaUbicaciones.getItems().remove(x);
                            });
                        }
                    },
                    ubicaciones::getNombre,
                    "ubicación");
        } else {
            unidades_Medida u = contenidoTablaUM.getSelectionModel().getSelectedItem();
            ejecutarEliminacionConValidaciones(u,
                    x -> model.contarProductosPorUM(x.getNombre()),
                    x -> {
                        if (model.eliminarUM(x.getId())) {
                            Platform.runLater(() -> {
                                cacheUM.remove(x);
                                contenidoTablaUM.getItems().remove(x);
                            });
                        }
                    },
                    unidades_Medida::getNombre,
                    "unidad de medida");
        }
    }

    public void shutdown() {
        if (dataLoadService != null && dataLoadService.isRunning()) {
            dataLoadService.cancel();
        }
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
