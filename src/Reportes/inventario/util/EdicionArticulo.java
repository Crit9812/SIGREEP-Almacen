package Reportes.inventario.util;

import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

public class EdicionArticulo {

    public interface Callbacks {
        void onExito();
        void onError(String mensaje);
    }

    public static void mostrarDialogoEdicion(Window owner, String titulo,
                                             String idArticulo, boolean esSegmentado,
                                             String ubicacionActual, String loteActual,
                                             String caducidadActual, String presentacionActual,
                                             String factorActual,
                                             List<String> ubicacionesActivas,
                                             List<String> presentacionesCompra,
                                             Consumer<Boolean> onEliminar,
                                             Consumer<Boolean> onSegmentar,
                                             Callbacks callbacks) {

        // Crear diálogo con resultado booleano para controlar el cierre
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        configurarDialogoModal(dialog, owner);

        // Configurar tipos de botones (solo Cancelar para la X y ESC)
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Configurar el DialogPane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPadding(new Insets(0));
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: white;");

        // Cargar la hoja de estilo del diálogo
        dialogPane.getStylesheets().add(
                EdicionArticulo.class.getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );

        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: white;");
        mainContainer.setPadding(new Insets(0));

        // Título superior
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 0 10 0;");
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblTitulo, new Insets(10, 0, 10, 0));

        // Mostrar ID del artículo con margen
        Label lblIdInfo = new Label("ID: " + idArticulo + (esSegmentado ? " (Segmentado)" : ""));
        lblIdInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 20;");
        lblIdInfo.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(lblIdInfo, new Insets(0, 0, 10, 20));

        // Contenido central - Campos del formulario
        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(0, 20, 0, 20));
        contenido.setStyle("-fx-background-color: white;");

        // Mostrar nota para segmentados
        if (esSegmentado) {
            Label lblInfo = new Label("NOTA: Los cambios de lote y caducidad se aplicarán a TODAS las piezas segmentadas. La ubicación se cambia solo para esta pieza.");
            lblInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-wrap-text: true; -fx-padding: 0 0 10 0;");
            contenido.getChildren().add(lblInfo);
        }

        // Campos del formulario
        TextField txtLote = new TextField(loteActual != null ? loteActual : "");
        txtLote.setMaxWidth(180);

        DatePicker dpCaducidad = new DatePicker();
        dpCaducidad.setPromptText("Opcional");
        if (caducidadActual != null && !caducidadActual.trim().isEmpty()) {
            try {
                dpCaducidad.setValue(LocalDate.parse(caducidadActual.trim()));
            } catch (DateTimeParseException ignored) {
                dpCaducidad.setValue(null);
            }
        }
        dpCaducidad.setMaxWidth(180);

        ComboBox<String> cbPresentacion = new ComboBox<>();
        cbPresentacion.setItems(FXCollections.observableArrayList(presentacionesCompra));
        cbPresentacion.setPromptText("Selecciona presentación");
        if (presentacionActual != null && !presentacionActual.isEmpty()) {
            cbPresentacion.setValue(presentacionActual);
        }
        cbPresentacion.setMaxWidth(180);
        cbPresentacion.setDisable(esSegmentado);

        TextField txtFactor = new TextField(factorActual != null ? factorActual : "");
        txtFactor.setMaxWidth(180);
        txtFactor.setDisable(esSegmentado);

        ComboBox<String> cbUbicacion = new ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(ubicacionesActivas));
        cbUbicacion.setPromptText("Selecciona ubicación");
        if (ubicacionActual != null && !ubicacionActual.isEmpty() && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)) {
            cbUbicacion.setValue(ubicacionActual);
        }
        cbUbicacion.setMaxWidth(180);

        // Organización en filas
        HBox fila1 = new HBox(15);
        fila1.setAlignment(Pos.CENTER_LEFT);
        VBox vboxLote = new VBox(5);
        vboxLote.getChildren().addAll(new Label("Lote:"), txtLote);
        HBox.setHgrow(vboxLote, Priority.ALWAYS);
        VBox vboxCaducidad = new VBox(5);
        vboxCaducidad.getChildren().addAll(new Label("Caducidad (Opcional):"), dpCaducidad);
        HBox.setHgrow(vboxCaducidad, Priority.ALWAYS);
        fila1.getChildren().addAll(vboxLote, vboxCaducidad);

        HBox fila2 = new HBox(15);
        fila2.setAlignment(Pos.CENTER_LEFT);
        VBox vboxPresentacion = new VBox(5);
        vboxPresentacion.getChildren().addAll(new Label("Presentación:"), cbPresentacion);
        HBox.setHgrow(vboxPresentacion, Priority.ALWAYS);
        VBox vboxFactor = new VBox(5);
        vboxFactor.getChildren().addAll(new Label("Factor:"), txtFactor);
        HBox.setHgrow(vboxFactor, Priority.ALWAYS);
        fila2.getChildren().addAll(vboxPresentacion, vboxFactor);

        HBox fila3 = new HBox();
        fila3.setAlignment(Pos.CENTER_LEFT);
        VBox vboxUbicacion = new VBox(5);
        vboxUbicacion.getChildren().addAll(new Label("Ubicación:"), cbUbicacion);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);
        fila3.getChildren().addAll(vboxUbicacion, new Region());

        contenido.getChildren().addAll(fila1, fila2, fila3);

        // Botones personalizados
        HBox contenedorBotones = new HBox(15);
        contenedorBotones.setAlignment(Pos.CENTER);
        contenedorBotones.setPadding(new Insets(20, 20, -30, 20));
        contenedorBotones.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.getStyleClass().add("botonCancelar");
        btnEliminar.setPrefWidth(120);

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.getStyleClass().addAll("boton-form", "detallado");
        btnAceptar.setPrefWidth(120);
        btnAceptar.setDefaultButton(true); // Hacer que sea el botón por defecto (enter)

        Button btnSegmentar = new Button("Segmentar");
        btnSegmentar.getStyleClass().addAll("boton-form", "detallado");
        btnSegmentar.setPrefWidth(120);

        if (esSegmentado) {
            btnSegmentar.setVisible(false);
            btnSegmentar.setManaged(false);
        } else {
            actualizarVisibilidadSegmentar(btnSegmentar, cbPresentacion.getValue());
            cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) ->
                    actualizarVisibilidadSegmentar(btnSegmentar, newVal));
        }

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        contenedorBotones.getChildren().addAll(btnEliminar, espaciador, btnSegmentar, btnAceptar);

        mainContainer.getChildren().addAll(lblTitulo, lblIdInfo, contenido, contenedorBotones);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(esSegmentado ? 500 : 520);

        // Ocultar el botón por defecto de ButtonType.CLOSE (que es la X)
        Button btnClose = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        btnClose.setVisible(false);

        // ============ MANEJAR EVENTOS ============

        btnAceptar.setOnAction(e -> {
            if (!validarCampos(cbUbicacion, cbPresentacion, txtFactor, esSegmentado)) {
                return;
            }

            LocalDate fechaCaducidad = obtenerFechaDatePicker(dpCaducidad);

            // Ejecutar guardado en BD
            boolean exito = guardarEnBD(
                    idArticulo,
                    esSegmentado,
                    cbUbicacion.getValue(),
                    txtLote.getText(),
                    fechaCaducidad,
                    cbPresentacion.getValue(),
                    txtFactor.getText()
            );

            if (exito) {
                // Cerrar diálogo con resultado true
                dialog.setResult(true);
                dialog.close();
                if (callbacks != null) {
                    callbacks.onExito();
                }
            } else {
                if (callbacks != null) {
                    callbacks.onError("No se pudo guardar los cambios");
                }
            }
        });

        // Botón Eliminar
        // Botón Eliminar
        btnEliminar.setOnAction(e -> {
            // Primero mostrar confirmación
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.setHeaderText(null);
            confirmacion.setContentText("¿Estás seguro de que deseas eliminar este artículo?");

            confirmacion.initOwner(dialog.getDialogPane().getScene().getWindow());
            confirmacion.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.OK) {
                    // Solo después de confirmar, ejecutar el callback y cerrar
                    if (onEliminar != null) {
                        onEliminar.accept(true);
                    }
                    dialog.setResult(false);
                    dialog.close();
                }
            });
        });

        // Botón Segmentar
        // Botón Segmentar
        btnSegmentar.setOnAction(e -> {
            // Primero verificar que no sea segmentado (por seguridad)
            if (esSegmentado) {
                mostrarAlerta(Alert.AlertType.WARNING, "No permitido",
                        "Los artículos segmentados no se pueden volver a segmentar.");
                return;
            }

            // Ejecutar callback y cerrar
            if (onSegmentar != null) {
                onSegmentar.accept(true);
            }
            dialog.setResult(false);
            dialog.close();
        });

        // Configurar cierre del diálogo
        dialog.setOnCloseRequest(event -> {
            dialog.setResult(false);
        });

        // Mostrar y esperar
        dialog.showAndWait();
    }

    private static boolean guardarEnBD(String idArticulo, boolean esSegmentado,
                                       String ubicacion, String lote, LocalDate caducidad,
                                       String presentacion, String factor) {

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return false;
            }

            // Obtener ID de ubicación
            Integer ubicacionId = obtenerIdUbicacion(conn, ubicacion);
            if (ubicacionId == null && ubicacion != null && !ubicacion.isEmpty()) {
                return false; // Ubicación no encontrada
            }

            if (esSegmentado) {
                return actualizarArticuloSegmentado(conn, idArticulo, ubicacionId, lote, caducidad);
            } else {
                return actualizarArticuloNormal(conn, idArticulo, ubicacionId, lote, caducidad, presentacion, factor);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean actualizarArticuloNormal(Connection conn, String idArticuloStr,
                                                    Integer ubicacionId, String lote, LocalDate caducidad,
                                                    String presentacion, String factor) throws SQLException {
        int idArticulo = Integer.parseInt(idArticuloStr);

        String sql = "UPDATE articulo SET ubicacion = ?, lote = ?, caducidad = ?, " +
                "presentacion = ?, factor = ? WHERE idArticulo = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ubicacionId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, ubicacionId);
            }
            ps.setString(2, lote != null ? lote.trim() : "");

            // **CORRECCIÓN CRÍTICA: Usar java.sql.Date o NULL de tipo DATE**
            if (caducidad != null) {
                ps.setDate(3, java.sql.Date.valueOf(caducidad));  // Convertir LocalDate a java.sql.Date
            } else {
                ps.setNull(3, java.sql.Types.DATE);  // ← NULL de tipo DATE, no VARCHAR
            }

            ps.setString(4, presentacion != null ? presentacion : "");
            ps.setString(5, factor != null ? factor : "");
            ps.setInt(6, idArticulo);

            return ps.executeUpdate() > 0;
        }
    }

    private static LocalDate obtenerFechaDatePicker(DatePicker datePicker) {
        if (datePicker == null) {
            return null;
        }
        // 1. Primero verificar el texto del editor
        String texto = datePicker.getEditor() != null ? datePicker.getEditor().getText() : "";
        if (texto == null || texto.trim().isEmpty()) {
            return null;  // Editor vacío = fecha vacía
        }
        LocalDate valor = datePicker.getValue();
        if (valor != null) {
            return valor;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ignored) {
            return null;  // Texto inválido = fecha vacía
        }
    }

    private static boolean actualizarArticuloSegmentado(Connection conn, String idDetalle,
                                                        Integer ubicacionId, String lote,
                                                        LocalDate caducidad) throws SQLException {
        // Primero actualizar lote y caducidad en el artículo padre
        boolean padreActualizado = actualizarPadreSegmentado(conn, idDetalle, lote, caducidad);
        if (!padreActualizado) {
            return false;
        }

        // Luego actualizar ubicación del segmentado específico
        String sql = "UPDATE detalleArticulo SET idUbicacion = ? WHERE idDetalle = ? AND estado = 'activo'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ubicacionId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, ubicacionId);
            }
            ps.setString(2, idDetalle);

            int filas = ps.executeUpdate();
            if (filas == 0) {
                // Intentar insertar como nuevo
                return insertarNuevoSegmentado(conn, idDetalle, ubicacionId);
            }
            return true;
        }
    }

    private static boolean actualizarPadreSegmentado(Connection conn, String idDetalle,
                                                     String lote, LocalDate caducidad) throws SQLException {
        // Obtener idArticulo del padre
        String sqlPadre = "SELECT idArticulo FROM detalleArticulo WHERE idDetalle = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlPadre)) {
            ps.setString(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idArticuloPadre = rs.getInt("idArticulo");

                    // Actualizar el padre
                    String sqlUpdate = "UPDATE articulo SET lote = ?, caducidad = ? WHERE idArticulo = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                        psUpdate.setString(1, lote != null ? lote.trim() : "");

                        if (caducidad != null) {
                            psUpdate.setDate(2, java.sql.Date.valueOf(caducidad));  // ← java.sql.Date
                        } else {
                            psUpdate.setNull(2, java.sql.Types.DATE);  // ← NULL de tipo DATE
                        }

                        psUpdate.setInt(3, idArticuloPadre);
                        return psUpdate.executeUpdate() > 0;
                    }
                }
            }
        }
        return false;
    }

    private static boolean insertarNuevoSegmentado(Connection conn, String idDetalle,
                                                   Integer ubicacionId) throws SQLException {
        // Obtener idArticulo del padre
        String sqlPadre = "SELECT idArticulo FROM detalleArticulo WHERE idDetalle LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlPadre)) {
            ps.setString(1, idDetalle.substring(0, idDetalle.length() - 1) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idArticuloPadre = rs.getInt("idArticulo");

                    String sqlInsert = "INSERT INTO detalleArticulo (idDetalle, idArticulo, idUbicacion, estado) VALUES (?, ?, ?, 'activo')";
                    try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                        psInsert.setString(1, idDetalle);
                        psInsert.setInt(2, idArticuloPadre);
                        if (ubicacionId == null) {
                            psInsert.setNull(3, java.sql.Types.INTEGER);
                        } else {
                            psInsert.setInt(3, ubicacionId);
                        }
                        return psInsert.executeUpdate() > 0;
                    }
                }
            }
        }
        return false;
    }

    private static Integer obtenerIdUbicacion(Connection conn, String nombreUbicacion) throws SQLException {
        if (nombreUbicacion == null || nombreUbicacion.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUbicacion.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private static boolean validarCampos(ComboBox<String> cbUbicacion, ComboBox<String> cbPresentacion,
                                         TextField txtFactor, boolean esSegmentado) {
        if (cbUbicacion.getValue() == null || cbUbicacion.getValue().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "La ubicación es requerida.");
            return false;
        }

        if (!esSegmentado) {
            if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "La presentación es requerida.");
                return false;
            }

            if (txtFactor.getText() == null || txtFactor.getText().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "El factor es requerido.");
                return false;
            }
        }

        return true;
    }

    private static void actualizarVisibilidadSegmentar(Button botonSegmentar, String presentacion) {
        if (botonSegmentar == null) return;
        String valor = presentacion == null ? "" : presentacion.trim().toLowerCase();
        boolean mostrar = !"pz".equals(valor) && !"pieza".equals(valor);
        botonSegmentar.setVisible(mostrar);
        botonSegmentar.setManaged(mostrar);
    }

    private static void configurarDialogoModal(Dialog<?> dialog, Window owner) {
        if (dialog == null) return;
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
    }

    private static void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}