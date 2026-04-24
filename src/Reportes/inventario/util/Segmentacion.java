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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Segmentacion {

    public interface Callbacks {
        void onExito(String mensaje);
        void onError(String mensaje);
    }

    public static void mostrarDialogoSegmentacion(Window owner, String titulo,
                                                  String descripcionArticulo, int factor,
                                                  int idArticulo,
                                                  List<String> ubicacionesActivas,
                                                  Callbacks callbacks) {

        // ============ DIÁLOGO ============
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // ============ CONFIGURAR ESTILOS ============
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPadding(new Insets(0));
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: white;");
        try {
            dialogPane.getStylesheets().add(
                    Segmentacion.class.getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("CSS no encontrado, usando estilos por defecto");
        }

        // Ocultar botón CLOSE pero mantener ESC y X
        Button btnClose = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        if (btnClose != null) btnClose.setVisible(false);

        // ============ CONTENEDOR PRINCIPAL ============
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: white;");

        // ---------- TÍTULO ----------
        VBox topBox = new VBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setStyle("-fx-background-color: white;");
        topBox.setPadding(new Insets(15, 0, 10, 0));
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        topBox.getChildren().add(lblTitulo);
        borderPane.setTop(topBox);

        // ---------- CENTRO (CONTENIDO) ----------
        VBox centerBox = new VBox(12);
        centerBox.setPadding(new Insets(20, 25, 10, 25)); // padding inferior reducido
        centerBox.setStyle("-fx-background-color: white;");
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        // --- DESCRIPCIÓN DEL ARTÍCULO (con altura fija para evitar corte) ---
        Label lblDescripcionArticulo = new Label(descripcionArticulo);
        lblDescripcionArticulo.setWrapText(true);
        lblDescripcionArticulo.setMaxWidth(Double.MAX_VALUE);
        lblDescripcionArticulo.setMinHeight(50);        // Altura mínima para texto
        lblDescripcionArticulo.setPrefHeight(60);       // Altura preferida
        lblDescripcionArticulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-padding: 0 0 5 0;");

        // --- TÍTULO DE UBICACIONES ---
        Label lblUbicaciones = new Label("Ubicaciones para segmentar:");
        lblUbicaciones.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 0 5 0;");

        // --- CONTENEDOR DE FILAS DE UBICACIONES ---
        VBox contenedorUbicaciones = new VBox(8);
        contenedorUbicaciones.setStyle("-fx-background-color: white;");
        contenedorUbicaciones.setMinHeight(200);

        List<UbicacionFila> filas = new ArrayList<>();
        agregarFilaUbicacion(contenedorUbicaciones, filas, true, ubicacionesActivas, 10);

        // --- SCROLLPANE (FONDO BLANCO, SIN BORDES NI RELLENOS) ---
        ScrollPane scroll = new ScrollPane(contenedorUbicaciones);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: transparent;");
        scroll.setBorder(null);
        scroll.setPrefViewportHeight(280);
        scroll.setMinViewportHeight(200);

        // Agregar todo al centro
        centerBox.getChildren().addAll(lblDescripcionArticulo, lblUbicaciones, scroll);
        borderPane.setCenter(centerBox);

        // ---------- BOTONES INFERIORES (SIN ESPACIO SOBRANTE) ----------
        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.CENTER);
        // Padding reducido: solo superior (15) y laterales (20), inferior 0 para evitar espacio extra
        bottomBox.setPadding(new Insets(15, 20, -30, 20));
        bottomBox.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().addAll("boton-form", "detallado");
        btnCancelar.setPrefWidth(120);

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.getStyleClass().addAll("boton-form", "detallado");
        btnAceptar.setPrefWidth(120);
        btnAceptar.setDefaultButton(true);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        bottomBox.getChildren().addAll(btnCancelar, espaciador, btnAceptar);
        borderPane.setBottom(bottomBox);

        // ============ CONFIGURAR DIÁLOGO ============
        dialogPane.setContent(borderPane);
        dialogPane.setPrefWidth(500);
        dialogPane.setPrefHeight(520);

        // ============ EVENTOS ============
        btnCancelar.setOnAction(e -> {
            dialog.setResult(false);
            dialog.close();
        });

        btnAceptar.setOnAction(event -> {
            List<UbicacionCantidad> seleccionadas = obtenerUbicacionesSeleccionadas(filas);
            if (seleccionadas.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación",
                        "Debe capturar al menos una ubicación con cantidad.");
                event.consume();
                return;
            }

            int suma = seleccionadas.stream().mapToInt(UbicacionCantidad::getCantidad).sum();
            if (suma != factor) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación",
                        "La suma de las ubicaciones debe ser " + factor + " y actualmente es " + suma + ".");
                event.consume();
                return;
            }

            ejecutarSegmentacionEnBackground(idArticulo, seleccionadas, descripcionArticulo, factor, callbacks, dialog);
        });

        dialog.setOnCloseRequest(event -> dialog.setResult(false));
        dialog.showAndWait();
    }

    // ============ MÉTODOS PRIVADOS (sin cambios) ============


    private static boolean ejecutarSegmentacion(int idArticulo, List<UbicacionCantidad> ubicaciones) {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (!marcarComoSegmentado(conn, idArticulo)) { conn.rollback(); return false; }
                Map<String, Integer> ubicacionIds = obtenerIdsUbicaciones(conn, ubicaciones);
                if (ubicacionIds == null) { conn.rollback(); return false; }
                if (!insertarSegmentados(conn, idArticulo, ubicaciones, ubicacionIds)) { conn.rollback(); return false; }
                conn.commit();
                return true;
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    private static boolean marcarComoSegmentado(Connection conn, int idArticulo) throws SQLException {
        String sql = "UPDATE articulo SET segmentado = 1, Estado = 'segmentado' WHERE idArticulo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idArticulo);
            return ps.executeUpdate() > 0;
        }
    }

    private static Map<String, Integer> obtenerIdsUbicaciones(Connection conn, List<UbicacionCantidad> ubicaciones) throws SQLException {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (UbicacionCantidad ubicacion : ubicaciones) {
                ps.setString(1, ubicacion.getNombre());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    ids.put(ubicacion.getNombre(), rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private static boolean insertarSegmentados(Connection conn, int idArticulo,
                                               List<UbicacionCantidad> ubicaciones,
                                               Map<String, Integer> ubicacionIds) throws SQLException {
        int consecutivoDetalle = obtenerSiguienteConsecutivoDetalle(conn);
        String sql = "INSERT INTO detalleArticulo (idDetalle, idArticulo, idUbicacion, estado) VALUES (?, ?, ?, 'disponible')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (UbicacionCantidad ubicacion : ubicaciones) {
                Integer ubicacionId = ubicacionIds.get(ubicacion.getNombre());
                for (int i = 0; i < ubicacion.getCantidad(); i++) {
                    ps.setString(1, "S-" + consecutivoDetalle++);
                    ps.setString(2, String.valueOf(idArticulo));
                    ps.setInt(3, ubicacionId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            return true;
        }
    }

    private static int obtenerSiguienteConsecutivoDetalle(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(idDetalle, 3) AS UNSIGNED)), 0) FROM detalleArticulo WHERE idDetalle LIKE 'S-%'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1) + 1;
        }
        return 1;
    }

    private static void ejecutarSegmentacionEnBackground(int idArticulo, List<UbicacionCantidad> ubicaciones,
                                                         String descripcion, int factor,
                                                         Callbacks callbacks, Dialog<Boolean> dialog) {
        new Thread(() -> {
            try {
                boolean exito = ejecutarSegmentacion(idArticulo, ubicaciones);
                Platform.runLater(() -> {
                    if (exito) {
                        dialog.setResult(true);
                        dialog.close();
                        mostrarAlerta(Alert.AlertType.INFORMATION, "Segmentación",
                                "Se ha realizado la segmentación.");
                    } else {
                        mostrarAlerta(Alert.AlertType.INFORMATION, "Segmentación",
                                "No se ha podido completar la segmentación del articulo");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (callbacks != null) callbacks.onError("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    // ============ MÉTODOS AUXILIARES UI ============

    private static void agregarFilaUbicacion(VBox contenedor, List<UbicacionFila> filas, boolean inicial,
                                             List<String> ubicacionesActivas, int maxFilas) {
        if (filas.size() >= maxFilas) {
            mostrarAlerta(Alert.AlertType.WARNING, "Límite alcanzado",
                    "Solo se pueden agregar hasta " + maxFilas + " ubicaciones.");
            return;
        }

        HBox fila = new HBox(15);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox vboxUbicacion = new VBox(5);
        vboxUbicacion.setAlignment(Pos.CENTER_LEFT);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Selecciona ubicación");
        combo.setItems(FXCollections.observableArrayList(ubicacionesActivas));
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        vboxCantidad.setAlignment(Pos.CENTER_LEFT);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        Button boton = new Button(inicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (inicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(contenedor, filas, false, ubicacionesActivas, maxFilas));
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

    private static List<UbicacionCantidad> obtenerUbicacionesSeleccionadas(List<UbicacionFila> filas) {
        List<UbicacionCantidad> resultado = new ArrayList<>();
        for (UbicacionFila fila : filas) {
            String ubicacion = fila.combo.getValue();
            if ((ubicacion == null || ubicacion.isBlank()) && fila.combo.getEditor() != null) {
                ubicacion = fila.combo.getEditor().getText();
            }
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) continue;
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) resultado.add(new UbicacionCantidad(ubicacion.trim(), cantidad));
            } catch (NumberFormatException ignored) {}
        }
        return resultado;
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

    // ============ CLASES INTERNAS ============

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

    public static class UbicacionCantidad {
        private final String nombre;
        private final int cantidad;
        public UbicacionCantidad(String nombre, int cantidad) {
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
    }
}