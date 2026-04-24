package Reportes.historial.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Compartido.helper.AtajosTecladoHelper;
import Compartido.helper.OverlayCarga;
import Reportes.historial.model.HistorialFactura;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private TextField buscarFactura;

    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<HistorialFactura> contenidoTabla;
    @FXML private TableColumn<HistorialFactura, String> colMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colClaveMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colFactura;
    @FXML private TableColumn<HistorialFactura, String> colFecha;
    @FXML private TableColumn<HistorialFactura, String> colHora;
    @FXML private TableColumn<HistorialFactura, String> colTipoMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colUsuario;
    @FXML private TableColumn<HistorialFactura, String> colExterno;
    @FXML private TableColumn<HistorialFactura, String> colPrecioNeto;
    @FXML private TableColumn<HistorialFactura, String> colPrecioTotal;
    @FXML private TableColumn<HistorialFactura, String> colNota;
    @FXML private TableColumn<HistorialFactura, String> colEstado;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final ObservableList<HistorialFactura> itemsHistorial = FXCollections.observableArrayList();
    private final ObservableList<HistorialFactura> itemsHistorialOriginal = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_ALT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private String criterioOrden = "fecha";
    private String direccionOrden = "desc";
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;
    private OverlayCarga overlayCargaGlobal;

    @FXML
    public void initialize() {
        configurarAtajosTeclado();
        Platform.runLater(() -> {
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);

            HBox.setHgrow(expansorBusqueda, Priority.ALWAYS);
            expansorBusqueda.setMinWidth(10);

            buscarFactura.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarFactura.prefHeightProperty().bind(root.heightProperty().multiply(0.04));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.78));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            overlayCargaGlobal = new OverlayCarga(root, new Pane());

            configurarColumnas();
            configurarFiltros();
            configurarBusquedaFactura();
            cargarHistorial();
            configurarDobleClick();

            RefrescoHelper.setVistaActual("historial");
            RefrescoHelper.registrarRefresco("historial", this::cargarHistorial);
        });
    }

    private void configurarDobleClick() {
        if (contenidoTabla == null) {
            return;
        }

        contenidoTabla.setRowFactory(table -> {
            TableRow<HistorialFactura> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    abrirDetalleHistorial(row.getItem());
                }
            });
            return row;
        });
    }

    private void abrirDetalleHistorial(HistorialFactura item) {
        if (item == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reportes/historial/view/detalle_view.fxml"));
            Pane rootDetalle = loader.load();
            DetalleFacturaController controller = loader.getController();
            controller.setHistorial(item);
            controller.setOnRefresh(this::cargarHistorial);
            controller.setOverlayCargaGlobal(overlayCargaGlobal);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Detalles de " + item.getMovimiento());
            stage.setScene(new javafx.scene.Scene(rootDetalle));
            controller.setStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarColumnas() {
        colMovimiento.setCellValueFactory(new PropertyValueFactory<>("movimiento"));
        colClaveMovimiento.setCellValueFactory(new PropertyValueFactory<>("claveMovimiento"));
        colFactura.setCellValueFactory(new PropertyValueFactory<>("factura"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTipoMovimiento.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colExterno.setCellValueFactory(new PropertyValueFactory<>("externo"));
        colPrecioNeto.setCellValueFactory(new PropertyValueFactory<>("precioNeto"));
        colPrecioTotal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));
        colNota.setCellValueFactory(new PropertyValueFactory<>("nota"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<HistorialFactura, ?>[] columnas = new TableColumn[] {
                colMovimiento,
                colClaveMovimiento,
                colFactura,
                colFecha,
                colHora,
                colTipoMovimiento,
                colUsuario,
                colExterno,
                colPrecioNeto,
                colPrecioTotal,
                colNota,
                colEstado
        };

        for (TableColumn<HistorialFactura, ?> columna : columnas) {
            columna.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(itemsHistorial);
    }

    private void cargarHistorial() {
        itemsHistorial.clear();
        List<HistorialFactura> registros = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }

            registros.addAll(obtenerEntradas(conn));
            registros.addAll(obtenerSalidas(conn));
            registros.addAll(obtenerAjustes(conn));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        registros.sort(Comparator.comparing(this::obtenerFechaHoraOrden,
                Comparator.nullsLast(Comparator.reverseOrder())));
        itemsHistorialOriginal.setAll(registros);
        actualizarValoresFiltro(comboFiltro.getValue());
        aplicarFiltrosYBusqueda();
    }

    private List<HistorialFactura> obtenerEntradas(Connection conn) throws SQLException {
        String query = "SELECT e.idEntrada, e.noFactura, e.fechaEntrada, e.horaEntrada, e.tipoEntrada, "
                + "e.claveUsuarioEntrada, e.idRemitente, e.precioNetoEntrada, e.precioTotalEntrada, e.nota, e.Estado, "
                + "u.userName AS usuarioNombre, "
                + "CASE "
                + "WHEN LOWER(e.tipoEntrada) = 'compra' THEN p.Nombre "
                + "WHEN LOWER(e.tipoEntrada) = 'traspaso' THEN s.nombre "
                + "ELSE e.idRemitente "
                + "END AS externoNombre "
                + "FROM entradas e "
                + "LEFT JOIN usuarios u ON e.claveUsuarioEntrada = u.idUsuario "
                + "LEFT JOIN proveedores p ON e.idRemitente = p.id "
                + "LEFT JOIN sucursales s ON e.idRemitente = s.id "
                + "WHERE LOWER(e.Estado) IN ('pendiente', 'finalizado', 'disponible', 'cancelado')";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                registros.add(new HistorialFactura(
                        "Entrada",
                        String.valueOf(rs.getInt("idEntrada")),
                        valorTexto(rs.getObject("noFactura")),
                        valorTexto(rs.getObject("fechaEntrada")),
                        valorTexto(rs.getObject("horaEntrada")),
                        valorTexto(rs.getObject("tipoEntrada")),
                        valorTexto(rs.getObject("usuarioNombre")),
                        valorTexto(rs.getObject("externoNombre")),
                        valorTexto(rs.getObject("precioNetoEntrada")),
                        valorTexto(rs.getObject("precioTotalEntrada")),
                        valorTexto(rs.getObject("nota")),
                        valorTexto(rs.getObject("Estado"))
                ));
            }
        }

        return registros;
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<HistorialFactura, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<HistorialFactura, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                });
    }

    private void configurarFiltros() {
        comboFiltro.getItems().setAll(
                "Movimiento",
                "Tipo",
                "Usuario",
                "Externo",
                "Estado"
        );
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (restaurandoFiltros) {
                return;
            }
            actualizarValoresFiltro(newVal);
        });
    }

    private void actualizarValoresFiltro(String campo) {
        if (comboValor == null) {
            return;
        }
        comboValor.getItems().clear();
        if (!restaurandoFiltros) {
            comboValor.setValue(null);
        }
        if (campo == null || campo.isBlank()) {
            return;
        }
        List<String> valores = new ArrayList<>();
        for (HistorialFactura item : itemsHistorialOriginal) {
            String valor = obtenerValorCampo(item, campo);
            if (valor != null && !valor.isBlank() && !valores.contains(valor)) {
                valores.add(valor);
            }
        }
        comboValor.getItems().setAll(valores);
    }

    @FXML
    private void agregarFiltro() {
        String campo = comboFiltro.getValue();
        String valor = comboValor.getValue();
        if (campo == null || valor == null) {
            mostrarAdvertencia(
                    "Filtro incompleto",
                    "Debes seleccionar un valor para el campo \"" + campo + "\"."
            );
            return;
        }
        if (filtrosActivos.size() >= 3) {
            mostrarAdvertencia(
                    "Límite de filtros",
                    "Solo puedes aplicar hasta 3 filtros al mismo tiempo.\n" +
                            "Elimina uno para agregar otro."
            );
            return;
        }
        for (Filtro filtro : filtrosActivos) {
            if (filtro.campo.equals(campo)) {
                mostrarAdvertencia(
                        "Filtro duplicado",
                        "Ya existe un filtro aplicado para el campo \"" + campo + "\".\n" +
                                "Elimina el filtro actual si deseas cambiar su valor."
                );
                return;
            }
        }
        Filtro filtro = new Filtro(campo, valor);
        filtrosActivos.add(filtro);
        contenedorFiltros.getChildren().add(crearChipFiltro(filtro));
        aplicarFiltrosYBusqueda();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(6);
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.getStyleClass().add("chip");

        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.getStyleClass().add("chip-label");

        Button quitar = new Button("x");
        quitar.getStyleClass().add("chip-close");
        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().remove(chip);
            aplicarFiltrosYBusqueda();
        });

        chip.getChildren().addAll(texto, quitar);
        if (contenedorFiltros.getChildren().isEmpty()) {
            HBox.setMargin(chip, new Insets(0, 0, 0, 25));
        }

        return chip;
    }

    private void aplicarFiltrosYBusqueda() {
        String filtroFactura = buscarFactura != null ? buscarFactura.getText() : "";
        String criterioFactura = filtroFactura == null ? "" : filtroFactura.trim().toLowerCase();

        List<HistorialFactura> filtrados = new ArrayList<>();
        for (HistorialFactura item : itemsHistorialOriginal) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);
                if (valor == null || !valor.equals(filtro.valor)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide && !criterioFactura.isBlank()) {
                String factura = valorTexto(item.getFactura()).toLowerCase();
                coincide = factura.contains(criterioFactura);
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        itemsHistorial.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(HistorialFactura item, String campo) {
        switch (campo) {
            case "Movimiento":
                return item.getMovimiento();
            case "Factura":
                return item.getFactura();
            case "Fecha":
                return item.getFecha();
            case "Tipo":
                return item.getTipoMovimiento();
            case "Usuario":
                return item.getUsuario();
            case "Externo":
                return item.getExterno();
            case "Estado":
                return item.getEstado();
            case "Precio neto":
                return item.getPrecioNeto();
            case "Precio total":
                return item.getPrecioTotal();
            default:
                return "";
        }
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Historial por factura", "excel",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Historial por factura", "pdf",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.previsualizarPDF(contenidoTabla, "Historial por factura",
                obtenerFiltrosAplicados());
    }

    private List<String> obtenerFiltrosAplicados() {
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }
        String filtroFactura = buscarFactura != null ? buscarFactura.getText() : "";
        if (filtroFactura != null && !filtroFactura.isBlank()) {
            filtrosAplicados.add("Factura contiene: " + filtroFactura.trim());
        }
        return filtrosAplicados;
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = List.of(
                "movimiento",
                "fecha",
                "clave",
                "factura",
                "tipo",
                "usuario",
                "externo"
        );
        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<HistorialFactura> comparator;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "movimiento":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getMovimiento()));
                break;
            case "clave":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveMovimiento()));
                break;
            case "factura":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getFactura()));
                break;
            case "tipo":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getTipoMovimiento()));
                break;
            case "usuario":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getUsuario()));
                break;
            case "externo":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getExterno()));
                break;
            case "fecha":
            default:
                comparator = Comparator.comparing(this::obtenerFechaHoraOrden,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(itemsHistorial, comparator);
    }

    private void configurarBusquedaFactura() {
        if (buscarFactura == null) {
            return;
        }
        buscarFactura.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosYBusqueda());
    }

    private List<HistorialFactura> obtenerSalidas(Connection conn) throws SQLException {
        String query = "SELECT s.idSalida, s.noFactura, s.fechaSalida, s.horaSalida, s.tipoSalida, "
                + "s.claveUsuarioSalida, s.idDestinatario, s.precioNetoSalida, s.precioTotalSalida, s.nota, s.Estado, "
                + "u.userName AS usuarioNombre, "
                + "CASE "
                + "WHEN LOWER(s.tipoSalida) = 'venta' THEN c.Nombre "
                + "WHEN LOWER(s.tipoSalida) = 'traspaso' THEN su.nombre "
                + "ELSE s.idDestinatario "
                + "END AS externoNombre "
                + "FROM salidas s "
                + "LEFT JOIN usuarios u ON s.claveUsuarioSalida = u.idUsuario "
                + "LEFT JOIN clientes c ON s.idDestinatario = c.id "
                + "LEFT JOIN sucursales su ON s.idDestinatario = su.id "
                + "WHERE LOWER(s.Estado) IN ('pendiente', 'finalizado', 'disponible', 'cancelado')";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                registros.add(new HistorialFactura(
                        "Salida",
                        String.valueOf(rs.getInt("idSalida")),
                        valorTexto(rs.getObject("noFactura")),
                        valorTexto(rs.getObject("fechaSalida")),
                        valorTexto(rs.getObject("horaSalida")),
                        valorTexto(rs.getObject("tipoSalida")),
                        valorTexto(rs.getObject("usuarioNombre")),
                        valorTexto(rs.getObject("externoNombre")),
                        valorTexto(rs.getObject("precioNetoSalida")),
                        valorTexto(rs.getObject("precioTotalSalida")),
                        valorTexto(rs.getObject("nota")),
                        valorTexto(rs.getObject("Estado"))
                ));
            }
        }

        return registros;
    }

    private List<HistorialFactura> obtenerAjustes(Connection conn) throws SQLException {
        String query = "SELECT a.idAjuste, a.idUsuario, a.fechaAjuste, a.horaAjuste, a.precioNeto, a.precioTotal, a.Nota, "
                + "a.Estado, "
                + "u.userName AS usuarioNombre "
                + "FROM ajuste_inventario a "
                + "LEFT JOIN usuarios u ON a.idUsuario = u.idUsuario";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String horaAjuste = formatearHoraAjuste(rs.getObject("horaAjuste"));
                registros.add(new HistorialFactura(
                        "Ajuste",
                        valorTexto(rs.getObject("idAjuste")),
                        "-",
                        valorTexto(rs.getObject("fechaAjuste")),
                        horaAjuste,
                        "Ajuste de inventario",
                        valorTexto(rs.getObject("usuarioNombre")),
                        "-",
                        valorTexto(rs.getObject("precioNeto")),
                        valorTexto(rs.getObject("precioTotal")),
                        valorTexto(rs.getObject("Nota")),
                        valorTexto(rs.getObject("Estado"))
                ));
            }
        }

        return registros;
    }

    private String formatearHoraAjuste(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number) {
            long segundos = ((Number) valor).longValue();
            if (segundos < 0) {
                return "";
            }
            return LocalTime.ofSecondOfDay(segundos).format(FORMATO_HORA);
        }
        return valor.toString();
    }

    private String valorTexto(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        return texto.isBlank() ? "" : texto;
    }

    private LocalDateTime obtenerFechaHoraOrden(HistorialFactura item) {
        LocalDate fecha = parseFecha(item.getFecha());
        if (fecha == null) {
            return null;
        }
        LocalTime hora = parseHora(item.getHora());
        if (hora == null) {
            hora = LocalTime.MIDNIGHT;
        }
        return LocalDateTime.of(fecha, hora);
    }

    private LocalDate parseFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatos = List.of(DateTimeFormatter.ISO_LOCAL_DATE, FORMATO_FECHA_ALT);
        for (DateTimeFormatter formatter : formatos) {
            try {
                return LocalDate.parse(fechaTexto, formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar con el siguiente formato
            }
        }
        return null;
    }

    private LocalTime parseHora(String horaTexto) {
        if (horaTexto == null || horaTexto.isBlank()) {
            return null;
        }
        String texto = horaTexto.trim();
        if (texto.chars().allMatch(Character::isDigit)) {
            try {
                long segundos = Long.parseLong(texto);
                if (segundos >= 0) {
                    return LocalTime.ofSecondOfDay(segundos);
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(texto, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static class Filtro {
        private final String campo;
        private final String valor;

        private Filtro(String campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }
    }


    private void configurarAtajosTeclado() {
        AtajosTecladoHelper.instalar(root, event -> {
            if (!event.isControlDown()) return;
            if (event.getCode() == KeyCode.R) exportarExcel();
            else if (event.getCode() == KeyCode.P) vistaPreviaPdf();
            else if (event.getCode() == KeyCode.D) descargarPdf();
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
