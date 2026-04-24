package Reportes.inventario.controller;

import Compartido.sesion.PermisosRol;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import Reportes.inventario.util.EdicionArticulo;
import javafx.geometry.Insets;
import Formularios.controller.controllerCompraEmergente;
import Operaciones.ajusteInventario.model.model;
import Operaciones.compra.model.compra;
import Reportes.inventario.model.ItemInventario;
import Compartido.helper.OverlayCarga;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DetalleInventarioController {

    @FXML private Label lblNombre;
    @FXML private Label lblMarca;
    @FXML private Label lblMaterial;
    @FXML private Label lblUnidad;
    @FXML private Label lblClasificacion;
    @FXML private Label lblDescripcion;
    @FXML private VBox contenedorDetalles;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnCerrar;
    @FXML private StackPane root;
    @FXML private Pane overlayPane;
    @FXML private Button btnAgregarArticulo;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;
    @FXML private Label lblPresentacion;
    @FXML private Label lblFactor;
    private String presentacionFiltro;
    private String factorFiltro;
    private ItemInventario itemInventario;
    private Stage stage;
    private Runnable onRefresh;
    private OverlayCarga overlayCarga;
    private final boolean soloLecturaReportes = !PermisosRol.esAdministrador();
    private static final List<String> PRESENTACIONES_COMPRA = List.of(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );
    private static final int MAX_FILAS = 10;

    @FXML
    public void initialize() {
        if (root != null && overlayPane != null) {
            overlayCarga = new OverlayCarga(root, overlayPane);
        }
        HBox.setHgrow(expansor, Priority.ALWAYS);
        expansor.setMinWidth(10);

        if (soloLecturaReportes && btnAgregarArticulo != null) {
            btnAgregarArticulo.setVisible(false);
            btnAgregarArticulo.setManaged(false);
        }
        if (soloLecturaReportes && lblAgregar != null) {
            lblAgregar.setVisible(false);
            lblAgregar.setManaged(false);
        }

        actualizarDatosProducto();
    }

    public void setItemInventario(ItemInventario itemInventario) {
        this.itemInventario = itemInventario;
        this.presentacionFiltro = itemInventario.getPresentacion();
        this.factorFiltro = itemInventario.getFactor();
        if (root != null) {
            root.getStyleClass().add("detalle-inventario");
        }
        if (scrollPane != null) {
            scrollPane.getStyleClass().add("detalle-inventario");
        }
        if (btnCerrar != null) {
            btnCerrar.getStyleClass().add("boton-cerrar");
        }
        if (contenedorDetalles != null) {
            contenedorDetalles.getStyleClass().add("contenedor-detalles");
        }

        actualizarDatosProducto();
        cargarDetalles();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != null) {
            stage.setResizable(false);
            stage.setMinWidth(500);
            stage.setMinHeight(700);
            stage.setWidth(500);
            stage.setHeight(700);
            stage.setMaxWidth(500);
            stage.setMaxHeight(700);
        }
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    @FXML
    private void cerrarVentana() {
        if (stage != null) {
            stage.close();
            return;
        }
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            btnCerrar.getScene().getWindow().hide();
        }
    }

    private void actualizarDatosProducto() {
        if (itemInventario == null) {
            return;
        }
        if (lblNombre != null) {
            lblNombre.setText(valorTexto(itemInventario.getProducto()));
        }
        if (lblMarca != null) {
            lblMarca.setText(valorTexto(itemInventario.getMarca()));
        }
        if (lblMaterial != null) {
            lblMaterial.setText(valorTexto(itemInventario.getMaterial()));
        }
        if (lblUnidad != null) {
            lblUnidad.setText(valorTexto(itemInventario.getUnidadMedida()));
        }
        if (lblClasificacion != null) {
            lblClasificacion.setText(valorTexto(itemInventario.getCategoria()));
        }
        if (lblDescripcion != null) {
            lblDescripcion.setText(valorTexto(itemInventario.getDescripcion()));
        }
        if (lblPresentacion != null) {
            lblPresentacion.setText(valorTexto(itemInventario.getPresentacion()));
        }
        if (lblFactor != null) {
            lblFactor.setText(valorTexto(itemInventario.getFactor()));
        }
    }

    private void cargarDetalles() {
        if (itemInventario == null) {
            mostrarSinDetalles();
            return;
        }

        Task<List<UbicacionDetalle>> task = new Task<>() {
            @Override
            protected List<UbicacionDetalle> call() {
                return obtenerUbicaciones();
            }
        };

        task.setOnSucceeded(event -> renderizarDetalles(task.getValue()));
        task.setOnFailed(event -> mostrarSinDetalles());

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void abrirFormularioCompraDesdeBoton() {
        if (soloLecturaReportes) {
            return;
        }
        abrirFormularioCompra(null);
    }

    private List<UbicacionDetalle> obtenerUbicaciones() {
        if (itemInventario == null || itemInventario.getClaveProducto() == null) {
            return List.of();
        }

        String presentacion = valorTexto(presentacionFiltro);
        String factor = valorTexto(factorFiltro);

        Map<String, UbicacionDetalle> ubicaciones = new LinkedHashMap<>();

        try (Connection conn = new Conexion().conectar()) {

            // 1. Obtener artículos NO segmentados según la presentación/filtro actual
            String sqlBase = """
            SELECT a.idArticulo,
                   a.lote,
                   a.caducidad,
                   a.presentacion,
                   a.factor,
                   u.nombre AS ubicacion,
                   'no_segmentado' AS tipo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN ubicaciones u ON a.ubicacion = u.id
            WHERE a.Estado = 'disponible'
              AND p.id = ?
              AND a.presentacion = ?
              AND a.factor = ?
            ORDER BY u.nombre, a.idArticulo
            """;

            try (PreparedStatement ps = conn.prepareStatement(sqlBase)) {
                ps.setString(1, itemInventario.getClaveProducto());
                ps.setString(2, presentacion);
                ps.setString(3, factor);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        agregarArticuloDesdeResultSet(rs, ubicaciones);
                    }
                }
            }

            // 2. Si el filtro actual es para "pz" factor "1", incluir también los segmentados
            if ("pz".equalsIgnoreCase(presentacion) && "1".equals(factor)) {
                // Buscar artículos segmentados del mismo producto
                String sqlSegmentados = """
                SELECT da.idDetalle,
                       a.lote,
                       a.caducidad,
                       'pz' AS presentacion,
                       '1' AS factor,
                       u.nombre AS ubicacion,
                       'segmentado' AS tipo,
                       a.idArticulo AS idArticuloPadre
                FROM detalleArticulo da
                INNER JOIN articulo a ON da.idArticulo = a.idArticulo
                INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                INNER JOIN productos p ON de.claveProducto = p.id
                LEFT JOIN ubicaciones u ON da.idUbicacion = u.id
                WHERE da.estado = 'disponible'
                  AND a.Estado = 'segmentado'
                  AND p.id = ?
                ORDER BY u.nombre, da.idDetalle
                """;

                try (PreparedStatement ps = conn.prepareStatement(sqlSegmentados)) {
                    ps.setString(1, itemInventario.getClaveProducto());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            agregarArticuloDesdeResultSet(rs, ubicaciones);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(ubicaciones.values());
    }

    private void agregarArticuloDesdeResultSet(ResultSet rs, Map<String, UbicacionDetalle> ubicaciones)
            throws SQLException {

        String ubicacion = valorTexto(rs.getString("ubicacion"));
        if (ubicacion.isBlank()) {
            ubicacion = "Sin ubicación";
        }

        UbicacionDetalle detalle = ubicaciones.computeIfAbsent(ubicacion, UbicacionDetalle::new);

        String tipo = rs.getString("tipo");
        boolean esSegmentado = "segmentado".equals(tipo);

        ArticuloDetalle articulo;
        if (esSegmentado) {
            // Para segmentados: usar idDetalle, mostrar como pz factor 1
            articulo = new ArticuloDetalle(
                    rs.getString("idDetalle"),           // idDetalle (S-1, S-2, etc.)
                    rs.getInt("idArticuloPadre"),        // id del artículo padre
                    ubicacion,
                    valorTexto(rs.getString("lote")),
                    valorTexto(rs.getString("caducidad"))
            );
        } else {
            articulo = new ArticuloDetalle(
                    rs.getInt("idArticulo"),
                    ubicacion,
                    valorTexto(rs.getString("lote")),
                    valorTexto(rs.getString("caducidad")),
                    valorTexto(rs.getString("presentacion")),
                    valorTexto(rs.getString("factor"))
            );
        }

        detalle.articulos.add(articulo);
    }

    private void renderizarDetalles(List<UbicacionDetalle> ubicaciones) {
        if (contenedorDetalles == null) {
            return;
        }
        contenedorDetalles.getChildren().clear();

        if (ubicaciones == null || ubicaciones.isEmpty()) {
            mostrarSinDetalles();
            return;
        }

        for (UbicacionDetalle ubicacion : ubicaciones) {
            VBox card = new VBox(8);
            card.getStyleClass().add("tarjeta-ubicacion");
            card.maxWidthProperty().bind(contenedorDetalles.widthProperty());
            //card.setPrefWidth(150);
            //card.setMaxWidth(150);

            HBox header = new HBox(10);
            Label titulo = new Label(String.format("Ubicación: %s (%d artículos)",
                    valorTexto(ubicacion.nombre), ubicacion.articulos.size()));
            titulo.getStyleClass().add("titulo-ubicacion");
            CheckBox chkDetalles = new CheckBox("Mostrar detalles");
            chkDetalles.getStyleClass().add("custom-check");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(titulo, spacer, chkDetalles);

            VBox listaArticulos = new VBox(6);
            listaArticulos.setStyle("-fx-padding: 4 0 0 0;");
            listaArticulos.setVisible(false);
            listaArticulos.setManaged(false);

            if (ubicacion.articulos.isEmpty()) {
                listaArticulos.getChildren().add(new Label("Sin artículos disponibles."));
            } else {
                int index = 1;
                for (ArticuloDetalle articulo : ubicacion.articulos) {
                    HBox fila = new HBox(8);

                    // Construir descripción según tipo
                    StringBuilder descripcion = new StringBuilder();
                    descripcion.append(index++).append(") ");

                    if (articulo.isEsSegmentado()) {
                        // Mostrar información para artículos segmentados
                        descripcion.append("[SEGMENTADO] ");
                        descripcion.append("ID Pieza: ").append(valorTexto(articulo.getIdDetalle()));

                        // Solo mostrar lote si tiene valor
                        String lote = valorTexto(articulo.getLote());
                        if (!lote.isBlank() && !"N/A".equalsIgnoreCase(lote)) {
                            descripcion.append(" | Lote: ").append(lote);
                        }

                        // Solo mostrar caducidad si tiene valor
                        String caducidad = valorTexto(articulo.getCaducidad());
                        if (!caducidad.isBlank() && !"N/A".equalsIgnoreCase(caducidad)) {
                            descripcion.append(" | Caducidad: ").append(caducidad);
                        }
                    } else {
                        // Mostrar información para artículos no segmentados
                        descripcion.append("ID: ").append(articulo.getIdArticulo());

                        // Solo mostrar lote si tiene valor
                        String lote = valorTexto(articulo.getLote());
                        if (!lote.isBlank() && !"N/A".equalsIgnoreCase(lote)) {
                            descripcion.append(" | Lote: ").append(lote);
                        }

                        // Solo mostrar caducidad si tiene valor
                        String caducidad = valorTexto(articulo.getCaducidad());
                        if (!caducidad.isBlank() && !"N/A".equalsIgnoreCase(caducidad)) {
                            descripcion.append(" | Caducidad: ").append(caducidad);
                        }

                    }

                    Label texto = new Label(descripcion.toString());

                    // Configurar botones según tipo de artículo
                    HBox contenedorBotones = new HBox(5);
                    contenedorBotones.setAlignment(Pos.CENTER_RIGHT);

                    // TODOS los artículos pueden editarse (tanto segmentados como no segmentados)
                    if (!soloLecturaReportes) {
                        Button btnEditar = new Button("Editar");
                        configurarBotonIcono(btnEditar, "/img/editar.png", "Editar");
                        btnEditar.getStyleClass().add("boton-detalle");
                        btnEditar.setOnAction(event -> editarArticulo(articulo));
                        contenedorBotones.getChildren().add(btnEditar);

                        // Todos los artículos pueden eliminarse
                        Button btnEliminar = new Button("Eliminar");
                        configurarBotonIcono(btnEliminar, "/img/eliminar.png", "Eliminar");
                        btnEliminar.getStyleClass().add("boton-detalle");
                        btnEliminar.setOnAction(event -> eliminarArticulo(articulo));
                        contenedorBotones.getChildren().add(btnEliminar);
                    }

                    // Configurar layout de la fila
                    Region spacerFila = new Region();
                    HBox.setHgrow(spacerFila, Priority.ALWAYS);

                    fila.getChildren().addAll(texto, spacerFila, contenedorBotones);
                    listaArticulos.getChildren().add(fila);

                    // Agregar separador visual entre filas (excepto en la última)
                    if (index <= ubicacion.articulos.size()) {
                        Separator separador = new Separator();
                        separador.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
                        listaArticulos.getChildren().add(separador);
                    }
                }
            }

            chkDetalles.selectedProperty().addListener((obs, oldVal, newVal) -> {
                listaArticulos.setVisible(newVal);
                listaArticulos.setManaged(newVal);
            });

            card.getChildren().addAll(header, listaArticulos);
            contenedorDetalles.getChildren().add(card);
        }
    }

    private void mostrarSinDetalles() {
        Platform.runLater(() -> {
            if (contenedorDetalles == null) {
                return;
            }
            contenedorDetalles.getChildren().setAll(new Label("Sin detalles disponibles."));
        });
    }

    private void abrirFormularioCompra(String ubicacion) {
        if (itemInventario == null) {
            return;
        }

        String ubicacionPrefill = ubicacion; // Mantener el parámetro por compatibilidad
        if ("Sin ubicación".equalsIgnoreCase(ubicacionPrefill) || ubicacionPrefill == null) {
            ubicacionPrefill = null;
        }

        ObservableList<compra> itemsEntrada = FXCollections.observableArrayList();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            controllerCompraEmergente controlador = new controllerCompraEmergente();
            controlador.setItemsCompra(itemsEntrada);
            controlador.setTituloFormulario("Agregar artículo");
            controlador.setModoAjusteInventario(true);
            controlador.setProductoPrefill(itemInventario.getClaveProducto(), itemInventario.getProducto(),
                    itemInventario.getDescripcion());
            controlador.setUbicacionPrefill(ubicacionPrefill); // Ahora puede ser null

            controlador.setPresentacionPrefill(presentacionFiltro);
            controlador.setFactorPrefill(factorFiltro);

            try {
                Method metodo = controlador.getClass().getMethod("setProductoSoloLectura", boolean.class);
                metodo.invoke(controlador, true);
            } catch (Exception e) {
                System.out.println("Nota: No se pudo hacer el producto de solo lectura");
            }
            // ==========================================================

            loader.setController(controlador);

            Pane formulario = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Agregar artículo");
            modal.setScene(new Scene(formulario));
            if (btnCerrar != null && btnCerrar.getScene() != null) {
                modal.initOwner(btnCerrar.getScene().getWindow());
            }
            modal.setResizable(false);
            modal.showAndWait();
            if (!itemsEntrada.isEmpty()) {
                model ajusteModel = new model();
                String ajusteId = ajusteModel.registrarAjuste(itemsEntrada, List.of(), "");
                if (ajusteId != null && !ajusteId.isBlank()) {
                    notificarActualizacion();
                    cargarDetalles();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editarArticulo(ArticuloDetalle articulo) {
        if (soloLecturaReportes) {
            return;
        }
        // Determinar si es segmentado
        boolean esSegmentado = articulo.isEsSegmentado();
        String idTexto = esSegmentado ? articulo.getIdDetalle() : String.valueOf(articulo.getIdArticulo());

        // Obtener ubicaciones activas
        List<String> ubicacionesActivas = obtenerUbicacionesActivas();

        // Obtener presentaciones disponibles
        List<String> presentacionesCompra = Arrays.asList(
                "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
        );

        EdicionArticulo.mostrarDialogoEdicion(
                btnCerrar.getScene().getWindow(),
                esSegmentado ? "Editar artículo segmentado" : "Editar artículo",
                idTexto,
                esSegmentado,
                valorTexto(articulo.getUbicacion()),
                valorTexto(articulo.getLote()),
                valorTexto(articulo.getCaducidad()),
                valorTexto(articulo.getPresentacion()),
                valorTexto(articulo.getFactor()),
                ubicacionesActivas,
                presentacionesCompra,
                // Callback para Eliminar
                (confirmado) -> eliminarArticulo(articulo),
                // Callback para Segmentar (solo si no es segmentado)
                (confirmado) -> {
                    if (!esSegmentado) {
                        iniciarSegmentacion(articulo, null);
                    }
                },
                // Callbacks para éxito/error
                new EdicionArticulo.Callbacks() {
                    @Override
                    public void onExito() {
                        // Notificar actualización y recargar detalles
                        notificarActualizacion();
                        cargarDetalles();
                    }

                    @Override
                    public void onError(String mensaje) {
                        mostrarAdvertencia("Error", mensaje);
                    }
                }
        );
    }

    private void eliminarArticulo(ArticuloDetalle articulo) {
        if (soloLecturaReportes) {
            return;
        }
        if (articulo == null) {
            return;
        }

        String mensaje;
        String titulo;

        if (articulo.isEsSegmentado()) {
            titulo = "Eliminar pieza segmentada";
            mensaje = "¿Deseas eliminar esta pieza segmentada (ID: " + articulo.getIdDetalle() + ")?\n\n" +
                    "NOTA: Solo se eliminará esta pieza específica, no todas las del grupo.";
        } else {
            titulo = "Eliminar artículo";
            mensaje = "¿Deseas eliminar el artículo (ID: " + articulo.getIdArticulo() + ")?";
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(mensaje);

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }

                if (articulo.isEsSegmentado()) {
                    // Para segmentados: inactivar solo esta pieza en detalleArticulo
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE detalleArticulo SET estado = 'inactivo' WHERE idDetalle = ?")) {
                        ps.setString(1, articulo.getIdDetalle());
                        int afectados = ps.executeUpdate();

                        if (afectados > 0) {
                            // Verificar si quedan segmentados activos para este artículo padre
                            try (PreparedStatement psVerificar = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM detalleArticulo WHERE idArticulo = ? AND estado = 'activo'")) {
                                psVerificar.setInt(1, articulo.getIdArticulo());
                                try (ResultSet rs = psVerificar.executeQuery()) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // Si no quedan segmentados activos, cambiar estado del artículo padre
                                        try (PreparedStatement psActualizarPadre = conn.prepareStatement(
                                                "UPDATE articulo SET Estado = 'disponible', segmentado = 0 WHERE idArticulo = ?")) {
                                            psActualizarPadre.setInt(1, articulo.getIdArticulo());
                                            psActualizarPadre.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Para no segmentados: marcar como eliminado en articulo
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET Estado = 'eliminado' WHERE idArticulo = ?")) {
                        ps.setInt(1, articulo.getIdArticulo());
                        ps.executeUpdate();
                    }
                }

                notificarActualizacion();
                cargarDetalles();

            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo eliminar el artículo: " + e.getMessage());
            }
        });
    }

    private void notificarActualizacion() {
        if (onRefresh != null) {
            Platform.runLater(onRefresh);
        }
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private LocalDate obtenerCaducidadSeleccionada(javafx.scene.control.DatePicker dpCaducidad) {
        if (dpCaducidad == null) {
            return null;
        }
        String texto = dpCaducidad.getEditor() != null ? dpCaducidad.getEditor().getText() : "";
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        LocalDate valor = dpCaducidad.getValue();
        if (valor != null) {
            return valor;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String valorTexto(String texto) {
        return texto == null ? "" : texto;
    }

    private List<String> obtenerUbicacionesActivas() {
        return new Operaciones.compra.model.model().obtenerNombresUbicaciones();
    }

    private void configurarBotonIcono(Button boton, String rutaIcono, String textoFallback) {
        boton.getStyleClass().add("boton-icono");
        try {
            ImageView icono = new ImageView(new Image(getClass().getResourceAsStream(rutaIcono)));
            icono.setFitWidth(16);
            icono.setFitHeight(16);
            boton.setGraphic(icono);
            boton.setText("");
        } catch (Exception e) {
            boton.setText(textoFallback);
        }
    }

    private void agregarEstilosDialogo(Dialog<ButtonType> dialog) {
        DialogPane pane = dialog.getDialogPane();

        pane.getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );

        Button btnOk = (Button) pane.lookupButton(ButtonType.OK);
        Button btnCancel = (Button) pane.lookupButton(ButtonType.CANCEL);

        if (btnOk != null) btnOk.getStyleClass().add("boton-formulario");
        if (btnCancel != null) btnCancel.getStyleClass().add("boton-formulario");

        if (btnCancel != null) ButtonBar.setButtonData(btnCancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        if (btnOk != null) ButtonBar.setButtonData(btnOk, ButtonBar.ButtonData.OK_DONE);

        // 🔥 Obtiene el ButtonBar real por lookup y define el orden manual
        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        }
    }

    private void iniciarSegmentacion(ArticuloDetalle articulo, Dialog<ButtonType> dialogPadre) {
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        int factor = parseFactor(articulo.factor);
        if (factor <= 0) {
            mostrarAdvertencia("Factor inválido", "El factor debe ser un número mayor a cero.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Segmentar artículo");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Al segmentar este artículo se dividirá el producto en " +
                factor + " piezas.\n¿Deseas continuar?");

        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        confirmacion.getButtonTypes().setAll(btnCancelar, btnAceptar);
        configurarOrdenBotones(confirmacion.getDialogPane(), btnCancelar, btnAceptar);

        confirmacion.initModality(Modality.APPLICATION_MODAL);
        if (dialogPadre != null) {
            confirmacion.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != btnAceptar) {
                return;
            }
            abrirFormularioSegmentacion(articulo, factor, dialogPadre);
        });
    }

    private void abrirFormularioSegmentacion(ArticuloDetalle articulo, int factor, Dialog<ButtonType> dialogPadre) {
        if (articulo == null || factor <= 0) {
            return;
        }

        // Usar la clase utilitaria Segmentacion CORRECTAMENTE
        Reportes.inventario.util.Segmentacion.mostrarDialogoSegmentacion(
                btnCerrar.getScene().getWindow(),
                "Segmentar artículo",
                obtenerDescripcionArticulo(articulo, factor),
                factor,
                articulo.idArticulo, // <- Agregar el idArticulo (parámetro nuevo)
                obtenerUbicacionesActivas(),
                new Reportes.inventario.util.Segmentacion.Callbacks() {
                    @Override
                    public void onExito(String mensaje) {
                        // Cerrar diálogo padre si existe
                        if (dialogPadre != null) {
                            dialogPadre.close();
                        }

                        // Mostrar mensaje de éxito
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Segmentación completada");
                        alert.setHeaderText(null);
                        alert.setContentText(mensaje);
                        alert.showAndWait();

                        // Refrescar la vista
                        notificarActualizacion();
                        cargarDetalles();

                        // Cerrar diálogo padre si aún está abierto
                        if (dialogPadre != null) {
                            dialogPadre.close();
                        }
                    }

                    @Override
                    public void onError(String mensaje) {
                        mostrarAdvertencia("Error", mensaje);
                    }
                }
        );
    }

    private void ejecutarSegmentacion(ArticuloDetalle articulo, int factor,
                                      List<Reportes.inventario.util.Segmentacion.UbicacionCantidad> ubicaciones,
                                      Dialog<ButtonType> dialogPadre) {
        if (articulo == null || ubicaciones == null || ubicaciones.isEmpty()) {
            return;
        }

        if (dialogPadre != null) {
            dialogPadre.close();
        }
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn == null) {
                        throw new SQLException("Sin conexión a la base de datos");
                    }
                    conn.setAutoCommit(false);

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET segmentado = 1, Estado = 'segmentado' WHERE idArticulo = ?")) {
                        ps.setInt(1, articulo.idArticulo);
                        ps.executeUpdate();
                    }

                    // Obtener IDs de ubicaciones
                    Map<String, Integer> ubicacionIds = new HashMap<>();
                    try (PreparedStatement psUbicacion = conn.prepareStatement(
                            "SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'")) {
                        for (Reportes.inventario.util.Segmentacion.UbicacionCantidad ubicacion : ubicaciones) {
                            psUbicacion.setString(1, ubicacion.getNombre());
                            try (ResultSet rs = psUbicacion.executeQuery()) {
                                if (!rs.next()) {
                                    conn.rollback();
                                    throw new SQLException("Ubicación inválida: " + ubicacion.getNombre());
                                }
                                ubicacionIds.put(ubicacion.getNombre(), rs.getInt(1));
                            }
                        }
                    }

                    int consecutivoDetalle = obtenerSiguienteConsecutivoDetalle(conn);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO detalleArticulo (idDetalle, idArticulo, idUbicacion, estado) VALUES (?, ?, ?, 'disponible')")) {
                        for (Reportes.inventario.util.Segmentacion.UbicacionCantidad ubicacion : ubicaciones) {
                            Integer ubicacionId = ubicacionIds.get(ubicacion.getNombre());
                            for (int i = 0; i < ubicacion.getCantidad(); i++) {
                                ps.setString(1, "S-" + consecutivoDetalle++);
                                ps.setString(2, String.valueOf(articulo.idArticulo));
                                ps.setInt(3, ubicacionId);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                    return construirMensajeSegmentacion(articulo, factor, ubicaciones);
                }
            }
        };

        task.setOnSucceeded(event -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            String mensaje = task.getValue();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Segmentación completada");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
            notificarActualizacion();
            cargarDetalles();
        });

        task.setOnFailed(event -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            Throwable ex = task.getException();
            mostrarAdvertencia("Error", ex != null ? ex.getMessage() : "No se pudo segmentar el artículo.");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private String obtenerDescripcionArticulo(ArticuloDetalle articulo, int factor) {
        String producto = itemInventario != null ? valorTexto(itemInventario.getProducto()) : "";
        String descripcion = itemInventario != null ? valorTexto(itemInventario.getDescripcion()) : "";
        String presentacion = valorTexto(articulo.presentacion);
        return "Producto: " + producto + " | Presentación: " + presentacion +
                " | Factor: " + factor + "\nDescripción: " + descripcion;
    }

    private int obtenerSiguienteConsecutivoDetalle(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(idDetalle, 3) AS UNSIGNED)), 0) " +
                "FROM detalleArticulo WHERE idDetalle LIKE 'S-%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        }
        return 1;
    }

    private void agregarFilaUbicacion(VBox contenedor, List<UbicacionFila> filas, boolean inicial) {
        if (filas.size() >= MAX_FILAS) {
            mostrarAdvertencia("Límite alcanzado",
                    "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }
        HBox fila = new HBox(15);

        VBox vboxUbicacion = new VBox(5);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Selecciona ubicación");
        combo.setItems(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(inicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (inicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(contenedor, filas, false));
        } else {
            boton.setOnAction(event -> {
                contenedor.getChildren().remove(fila);
                filas.removeIf(item -> item.contenedor == fila);
            });
        }

        fila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedor.getChildren().add(fila);
        filas.add(new UbicacionFila(fila, combo, txtCantidad));
    }

    private List<UbicacionCantidad> obtenerUbicacionesSeleccionadas(List<UbicacionFila> filas) {
        List<UbicacionCantidad> resultado = new ArrayList<>();
        for (UbicacionFila fila : filas) {
            String ubicacion = fila.combo.getValue();
            if ((ubicacion == null || ubicacion.isBlank()) && fila.combo.getEditor() != null) {
                ubicacion = fila.combo.getEditor().getText();
            }
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) {
                    resultado.add(new UbicacionCantidad(ubicacion.trim(), cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar cantidades inválidas
            }
        }
        return resultado;
    }

    private int parseFactor(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void configurarOrdenBotones(DialogPane pane, ButtonType cancelar, ButtonType aceptar) {
        if (pane == null) {
            return;
        }
        Button btnCancelar = (Button) pane.lookupButton(cancelar);
        Button btnAceptar = (Button) pane.lookupButton(aceptar);

        if (btnCancelar != null) btnCancelar.getStyleClass().add("boton-formulario");
        if (btnAceptar != null) btnAceptar.getStyleClass().add("boton-formulario");

        if (btnCancelar != null) ButtonBar.setButtonData(btnCancelar, ButtonBar.ButtonData.CANCEL_CLOSE);
        if (btnAceptar != null) ButtonBar.setButtonData(btnAceptar, ButtonBar.ButtonData.OK_DONE);

        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        }
    }

    private void configurarDialogoAcciones(Dialog<ButtonType> dialog, ButtonType cancelar, ButtonType aceptar) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );
        configurarOrdenBotones(pane, cancelar, aceptar);
    }

    private void configurarDialogoModal(Dialog<ButtonType> dialog) {
        if (dialog == null) {
            return;
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            dialog.initOwner(btnCerrar.getScene().getWindow());
        }
    }

    private String construirMensajeSegmentacion(ArticuloDetalle articulo, int factor,
                                                List<Reportes.inventario.util.Segmentacion.UbicacionCantidad> ubicaciones) {
        String producto = itemInventario != null ? valorTexto(itemInventario.getProducto()) : "";
        String presentacion = valorTexto(articulo.presentacion);
        StringBuilder detalle = new StringBuilder();
        for (Reportes.inventario.util.Segmentacion.UbicacionCantidad ubicacion : ubicaciones) {
            if (detalle.length() > 0) {
                detalle.append(", ");
            }
            detalle.append(ubicacion.getNombre()).append(" (").append(ubicacion.getCantidad()).append(" piezas)");
        }
        return "El artículo " + producto + " con presentación " + presentacion + " y factor " + factor +
                " se segmentó en: " + detalle + ".";
    }

    private void actualizarVisibilidadSegmentar(Button botonSegmentar, String presentacion) {
        if (botonSegmentar == null) {
            return;
        }
        String valor = valorTexto(presentacion).trim().toLowerCase();
        boolean mostrar = !"pz".equals(valor) && !"pieza".equals(valor);
        botonSegmentar.setVisible(mostrar);
        botonSegmentar.setManaged(mostrar);
    }

    private static class UbicacionDetalle {
        private final String nombre;
        private final List<ArticuloDetalle> articulos = new ArrayList<>();

        private UbicacionDetalle(String nombre) {
            this.nombre = nombre;
        }
    }

    private static class ArticuloDetalle {
        private int idArticulo; // Para no segmentados, o id padre para segmentados
        private String idDetalle; // Solo para segmentados (ej: "S-1", "S-2")
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;
        private final boolean esSegmentado;

        // Constructor para NO segmentados
        private ArticuloDetalle(int idArticulo, String ubicacion, String lote,
                                String caducidad, String presentacion, String factor) {
            this.idArticulo = idArticulo;
            this.idDetalle = null;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.esSegmentado = false;
        }

        // Constructor para SEGMENTADOS (siempre pz factor 1)
        private ArticuloDetalle(String idDetalle, int idArticuloPadre, String ubicacion,
                                String lote, String caducidad) {
            this.idArticulo = idArticuloPadre;
            this.idDetalle = idDetalle;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = "pz";
            this.factor = "1";
            this.esSegmentado = true;
        }

        // Getters (mantener públicos)
        public int getIdArticulo() { return idArticulo; }
        public String getIdDetalle() { return idDetalle; }
        public String getUbicacion() { return ubicacion; }
        public String getLote() { return lote; }
        public String getCaducidad() { return caducidad; }
        public String getPresentacion() { return presentacion; }
        public String getFactor() { return factor; }
        public boolean isEsSegmentado() { return esSegmentado; }

        public String getIdentificadorUnico() {
            return esSegmentado ? idDetalle : String.valueOf(idArticulo);
        }

        public Object getIdentificadorBD() {
            if (esSegmentado) {
                return idDetalle;
            } else {
                return idArticulo;
            }
        }

        public String getTipoDescripcion() {
            return esSegmentado ? "Pieza segmentada" : "Artículo completo";
        }
    }

    private static class UbicacionFila {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private UbicacionFila(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }

    private static class UbicacionCantidad {
        private final String nombre;
        private final int cantidad;
        private int id;

        private UbicacionCantidad(String nombre, int cantidad) {
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }
}
