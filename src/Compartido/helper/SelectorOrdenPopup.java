package Compartido.helper;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SelectorOrdenPopup {

    public static class SeleccionOrden {
        private final String criterio;
        private final String direccion;

        public SeleccionOrden(String criterio, String direccion) {
            this.criterio = criterio;
            this.direccion = direccion;
        }

        public String getCriterio() {
            return criterio;
        }

        public String getDireccion() {
            return direccion;
        }
    }

    private static Popup popupActivo;

    public static void mostrar(Node owner, double screenX, double screenY, List<String> criterios,
                               String criterioActual, String direccionActual,
                               Consumer<SeleccionOrden> onConfirm) {
        if (popupActivo != null && popupActivo.isShowing()) {
            return;
        }

        Popup popup = new Popup();
        popupActivo = popup;
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setOnHidden(event -> popupActivo = null);

        VBox contenedor = new VBox(12);
        contenedor.setStyle("-fx-background-color: white; -fx-border-color: #333; -fx-border-width: 1px; "
                + "-fx-padding: 12; -fx-background-radius: 6; -fx-border-radius: 6;");

        Label titulo = new Label("Ordenar por");
        titulo.setStyle("-fx-font-size: 12pt; -fx-font-weight: bold; -fx-underline: true;");

        VBox listaCriterios = new VBox(8);
        Map<String, CheckBox> checks = new LinkedHashMap<>();
        for (String criterio : criterios) {
            String texto = textoCriterio(criterio);
            CheckBox checkBox = new CheckBox(texto);
            checkBox.setSelected(criterio.equalsIgnoreCase(criterioActual));
            checkBox.setStyle("-fx-font-size: 11pt; -fx-text-fill: black;");
            checks.put(criterio, checkBox);
            listaCriterios.getChildren().add(checkBox);
        }

        for (Map.Entry<String, CheckBox> entry : checks.entrySet()) {
            entry.getValue().selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    for (Map.Entry<String, CheckBox> other : checks.entrySet()) {
                        if (!other.getKey().equals(entry.getKey())) {
                            other.getValue().setSelected(false);
                        }
                    }
                } else if (checks.values().stream().noneMatch(CheckBox::isSelected)) {
                    entry.getValue().setSelected(true);
                }
            });
        }

        Label ordenLabel = new Label("Orden");
        ordenLabel.setStyle("-fx-font-size: 12pt; -fx-font-weight: bold; -fx-underline: true;");

        HBox ordenBox = new HBox(12);
        ordenBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox asc = new CheckBox("Ascendente");
        asc.setStyle("-fx-font-size: 11pt; -fx-text-fill: black;");
        CheckBox desc = new CheckBox("Descendente");
        desc.setStyle("-fx-font-size: 11pt; -fx-text-fill: black;");

        if ("desc".equalsIgnoreCase(direccionActual)) {
            desc.setSelected(true);
        } else {
            asc.setSelected(true);
        }

        asc.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                desc.setSelected(false);
            } else if (!desc.isSelected()) {
                asc.setSelected(true);
            }
        });
        desc.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                asc.setSelected(false);
            } else if (!asc.isSelected()) {
                desc.setSelected(true);
            }
        });

        ordenBox.getChildren().addAll(asc, desc);

        Button confirmar = new Button("Confirmar");
        confirmar.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 5 12;");
        confirmar.setOnAction(event -> {
            String criterio = checks.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(criterios.get(0));
            String direccion = desc.isSelected() ? "desc" : "asc";
            onConfirm.accept(new SeleccionOrden(criterio, direccion));
            popup.hide();
        });

        Region expansor = new Region();
        HBox.setHgrow(expansor, Priority.ALWAYS);
        HBox acciones = new HBox(expansor, confirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        contenedor.getChildren().addAll(titulo, listaCriterios, ordenLabel, ordenBox, acciones);
        popup.getContent().add(contenedor);
        popup.show(owner, screenX, screenY);
    }

    private static String textoCriterio(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            return "ID";
        }

        switch (criterio.toLowerCase()) {
            case "cantidad":
                return "Cantidad";
            case "producto":
                return "Producto";
            case "fecha":
                return "Fecha y hora";
            case "hora":
                return "Hora";
            case "clave":
                return "Clave";
            case "factura":
                return "Factura";
            case "facturaentrada":
                return "Factura entrada";
            case "facturasalida":
                return "Factura salida";
            case "tipo":
                return "Tipo";
            case "usuario":
                return "Usuario";
            case "externo":
                return "Externo";
            case "movimiento":
                return "Movimiento";
            case "antes":
                return "Antes";
            case "despues":
            case "después":
                return "Después";
            case "entradas":
                return "Entradas";
            case "salidas":
                return "Salidas";
            case "proveedor":
                return "Proveedor";
            case "cliente":
                return "Cliente";
            case "sucursal":
                return "Sucursal";
            case "ubicacion":
            case "ubicación":
                return "Ubicación";
            case "lote":
                return "Lote";
            case "caducidad":
                return "Caducidad";
            default:
                return "id".equalsIgnoreCase(criterio) ? "ID" : formatearCriterio(criterio);
        }
    }

    private static String formatearCriterio(String criterio) {
        String criterioNormalizado = criterio
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace("_", " ")
                .trim();
        if (criterioNormalizado.isBlank()) {
            return "ID";
        }

        String[] palabras = criterioNormalizado.split("\\s+");
        StringBuilder texto = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.isBlank()) {
                continue;
            }
            if (texto.length() > 0) {
                texto.append(' ');
            }
            String minuscula = palabra.toLowerCase();
            texto.append(Character.toUpperCase(minuscula.charAt(0)))
                    .append(minuscula.substring(1));
        }
        return texto.length() == 0 ? "ID" : texto.toString();
    }

}
