package Compartido.controller;

import Compartido.model.NotificacionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;

public class notificacionesController {

    @FXML
    private ListView<NotificacionService.Notificacion> listaNotificaciones;

    @FXML
    private Label labelMensaje;

    private final NotificacionService notificacionService = new NotificacionService();

    @FXML
    public void initialize() {
        configurarLista();
        cargarNotificaciones();

        javafx.application.Platform.runLater(() -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) listaNotificaciones.getScene().getWindow();
            if (stage != null) {
                stage.setResizable(false);
                stage.setWidth(500);
                stage.setHeight(600);
                stage.setMinWidth(500);
                stage.setMaxWidth(500);
                stage.setMinHeight(600);
                stage.setMaxHeight(600);
            }
        });
    }

    private void configurarLista() {
        listaNotificaciones.setCellFactory(listView -> new ListCell<>() {
            private final HBox content = new HBox(12);
            private final Label badgeId = new Label();
            private final TextFlow textFlow = new TextFlow();
            private final Text textDescription = new Text();

            {
                // Configurar estilos
                content.setAlignment(Pos.CENTER_LEFT);
                content.setPrefHeight(50);

                // Badge para el ID
                badgeId.getStyleClass().add("badge-id");
                badgeId.setMinWidth(45);
                badgeId.setAlignment(Pos.CENTER);

                // Texto de la descripción
                textDescription.getStyleClass().add("texto-notificacion");
                textFlow.getChildren().add(textDescription);
                textFlow.setMaxWidth(360);

                content.getChildren().addAll(badgeId, textFlow);

                setGraphic(content);
            }

            @Override
            protected void updateItem(NotificacionService.Notificacion item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    getStyleClass().removeAll("notificacion-activa");
                    return;
                }

                // Actualizar contenido
                badgeId.setText(String.valueOf(item.getId()));

                String descripcion = item.getDescripcion() == null ? "" : item.getDescripcion();
                textDescription.setText(descripcion.length() > 90 ?
                        descripcion.substring(0, 90) + "..." : descripcion);

                setGraphic(content);

                // Aplicar estilo según estado
                if ("activo".equalsIgnoreCase(item.getEstado())) {
                    if (!getStyleClass().contains("notificacion-activa")) {
                        getStyleClass().add("notificacion-activa");
                    }
                } else {
                    getStyleClass().removeAll("notificacion-activa");
                }
            }
        });

        listaNotificaciones.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                abrirNotificacionSeleccionada();
            }
        });

        listaNotificaciones.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                abrirNotificacionSeleccionada();
            }
        });
    }

    private void cargarNotificaciones() {
        listaNotificaciones.setItems(FXCollections.observableArrayList(notificacionService.obtenerNotificaciones()));

        int activas = (int) listaNotificaciones.getItems().stream()
                .filter(n -> "activo".equalsIgnoreCase(n.getEstado()))
                .count();

        if (listaNotificaciones.getItems().isEmpty()) {
            labelMensaje.setText("📭 No hay notificaciones registradas.");
        } else {
            labelMensaje.setText(String.format("📬 Total: %d notificaciones (%d no leídas)",
                    listaNotificaciones.getItems().size(), activas));
        }
    }

    private void abrirNotificacionSeleccionada() {
        NotificacionService.Notificacion notificacion = listaNotificaciones.getSelectionModel().getSelectedItem();
        if (notificacion == null) {
            labelMensaje.setText(" Selecciona una notificación para abrirla.");
            return;
        }

        String detalle = notificacionService.obtenerDetalleSegunTipo(notificacion.getId());
        String mensaje = notificacion.getDescripcion();
        if (detalle != null && !detalle.isBlank()) {
            mensaje += "\n\n Detalles adicionales:\n" + detalle;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de notificación");
        alert.setHeaderText(" Notificación #" + notificacion.getId());

        // Personalizar el alert
        Label label = new Label(mensaje);
        label.setWrapText(true);
        label.setMaxWidth(400);
        label.getStyleClass().add("detalle-contenido");

        alert.getDialogPane().setContent(label);
        alert.getDialogPane().setPrefWidth(450);

        // Aplicar estilos CSS al diálogo
        alert.getDialogPane().getStyleClass().add("dialog-pane");

        // Cargar la misma hoja de estilos
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/Compartido/style/notificaciones.css").toExternalForm()
        );

        alert.showAndWait();

        if (notificacionService.marcarComoLeida(notificacion.getId())) {
            cargarNotificaciones();
            labelMensaje.setText("✅ Notificación marcada como leída.");
        }
    }
}