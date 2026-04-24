package Compartido.helper;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.geometry.Pos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SelectorColumnasPopup {

    private static Popup popupActivo;

    public static <T> void mostrar(Node owner, double screenX, double screenY,
                                   List<TableColumn<T, ?>> columnas,
                                   Consumer<Map<TableColumn<T, ?>, Boolean>> onConfirm) {
        if (popupActivo != null && popupActivo.isShowing()) {
            return;
        }
        Popup popup = new Popup();
        popupActivo = popup;
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setOnHidden(event -> popupActivo = null);

        VBox contenedor = new VBox(10);
        contenedor.setStyle("-fx-background-color: white; -fx-border-color: #333; -fx-border-width: 1px; "
                + "-fx-padding: 12; -fx-background-radius: 6; -fx-border-radius: 6;");

        Label titulo = new Label("Modificar columnas");
        titulo.setStyle("-fx-font-size: 12pt; -fx-font-weight: bold; -fx-underline: true;");

        VBox lista = new VBox(8);
        Map<TableColumn<T, ?>, CheckBox> checks = new LinkedHashMap<>();
        for (TableColumn<T, ?> columna : columnas) {
            String nombre = columna.getText();
            if (nombre == null || nombre.isBlank()) {
                continue;
            }
            CheckBox checkBox = new CheckBox(nombre);
            checkBox.setSelected(columna.isVisible());
            checkBox.setStyle("-fx-font-size: 11pt; -fx-text-fill: black;");
            checks.put(columna, checkBox);
            lista.getChildren().add(checkBox);
        }

        CheckBox seleccionarTodo = new CheckBox("Seleccionar todo");
        seleccionarTodo.setStyle("-fx-font-size: 11pt; -fx-text-fill: black;");

        // Estado inicial
        boolean todosSeleccionados = checks.values().stream().allMatch(CheckBox::isSelected);
        seleccionarTodo.setSelected(todosSeleccionados);

        // 1. Listener SIMPLE para "Seleccionar todo"
        seleccionarTodo.setOnAction(event -> {
            boolean nuevoEstado = seleccionarTodo.isSelected();
            for (CheckBox checkBox : checks.values()) {
                checkBox.setSelected(nuevoEstado);
            }
        });

        // 2. Listener SIMPLE para cada checkbox individual
        for (CheckBox checkBox : checks.values()) {
            checkBox.setOnAction(event -> {
                // Contar cuántos están seleccionados
                long seleccionados = checks.values().stream()
                        .filter(CheckBox::isSelected)
                        .count();

                // Actualizar "Seleccionar todo" según corresponda
                if (seleccionados == checks.size()) {
                    seleccionarTodo.setSelected(true);
                } else if (seleccionados == 0) {
                    seleccionarTodo.setSelected(false);
                } else {
                    // Estado indeterminado - desmarcar "Seleccionar todo"
                    seleccionarTodo.setSelected(false);
                }
            });
        }

        Button confirmar = new Button("Confirmar");
        confirmar.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 5 12;");
        confirmar.setOnAction(event -> {
            Map<TableColumn<T, ?>, Boolean> seleccion = new LinkedHashMap<>();
            for (Map.Entry<TableColumn<T, ?>, CheckBox> entry : checks.entrySet()) {
                seleccion.put(entry.getKey(), entry.getValue().isSelected());
            }
            onConfirm.accept(seleccion);
            popup.hide();
        });

        Region expansor = new Region();
        HBox.setHgrow(expansor, Priority.ALWAYS);
        HBox acciones = new HBox(expansor, confirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        contenedor.getChildren().addAll(titulo, seleccionarTodo, lista, acciones);
        popup.getContent().add(contenedor);
        popup.show(owner, screenX, screenY);
    }
}