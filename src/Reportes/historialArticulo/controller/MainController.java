package Reportes.historialArticulo.controller;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import VentanaPrincipal.controller.ControladorVista;
import Compartido.helper.RefrescoHelper;
import VentanaPrincipal.controller.EnumVistas;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Compartido.helper.AtajosTecladoHelper;
import conexion.Conexion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.application.Platform;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.util.StringConverter;

public class MainController implements ControladorVista {

    private static final String PERIODO_EXPORT = "Periodo: 02/02/25-02/03/25";

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private ComboBox<ProductoOpcion> buscarProducto;
    @FXML private DatePicker fechaInicio;
    @FXML private DatePicker fechaFin;
    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;
    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<HistorialArticuloItem> contenidoTabla;
    @FXML private TableColumn<HistorialArticuloItem, String> colFecha;
    @FXML private TableColumn<HistorialArticuloItem, String> colHora;
    @FXML private TableColumn<HistorialArticuloItem, String> colTipoMovimiento;
    @FXML private TableColumn<HistorialArticuloItem, String> colTipoMovimientoDetalle;
    @FXML private TableColumn<HistorialArticuloItem, String> colAntes;
    @FXML private TableColumn<HistorialArticuloItem, String> colDespues;
    @FXML private TableColumn<HistorialArticuloItem, String> colEntradas;
    @FXML private TableColumn<HistorialArticuloItem, String> colSalidas;
    @FXML private TableColumn<HistorialArticuloItem, String> colProveedor;
    @FXML private TableColumn<HistorialArticuloItem, String> colFacturaEntrada;
    @FXML private TableColumn<HistorialArticuloItem, String> colCliente;
    @FXML private TableColumn<HistorialArticuloItem, String> colFacturaSalida;
    @FXML private TableColumn<HistorialArticuloItem, String> colUsuario;
    @FXML private TableColumn<HistorialArticuloItem, String> colEstado;
    @FXML private TableColumn<HistorialArticuloItem, String> colPrecioTotal;
    @FXML private Label lblClave;
    @FXML private Label lblDescripcion;
    @FXML private Label lblPresentacion;
    @FXML private Label lblFactor;
    @FXML private Label lblExistencias;
    @FXML private TextField totalEntradasGeneral;
    @FXML private TextField totalSalidasGeneral;
    @FXML private TextField diferenciaGeneral;

    private StackPane contentArea;
    private String productoSeleccionadoId;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final ObservableList<ProductoOpcion> productosCache = FXCollections.observableArrayList();
    private final ObservableList<ProductoOpcion> productosFiltrados = FXCollections.observableArrayList();
    private final ObservableList<HistorialArticuloItem> historialItems = FXCollections.observableArrayList();
    private final ObservableList<HistorialArticuloItem> historialItemsOriginal = FXCollections.observableArrayList();
    private final List<HistorialArticuloItem> historialCacheCompleto = new ArrayList<>();
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;
    private boolean actualizandoBusqueda = false;
    private boolean filtroPresentacionFactorActivoPrevio = false;
    private boolean forzarOrdenFechaHora = false;
    private String criterioOrden = "fecha";
    private String direccionOrden = "desc";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

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

            buscarProducto.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarProducto.prefHeightProperty().bind(root.heightProperty().multiply(0.04));

            if (fechaInicio != null && fechaFin != null) {
                fechaInicio.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
                fechaInicio.prefHeightProperty().bind(root.heightProperty().multiply(0.04));
                fechaFin.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
                fechaFin.prefHeightProperty().bind(root.heightProperty().multiply(0.04));
            }

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.71));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.72));

            configurarColumnas();
            configurarBuscadorProducto();
            configurarFiltros();
            configurarFiltroFechas();
            actualizarTotales();
            RefrescoHelper.setVistaActual("historialArticulo");
            RefrescoHelper.registrarRefresco("historialArticulo", this::refrescarVista);
        });
    }

    private void refrescarVista() {
        // Si hay un producto seleccionado, recargar su historial
        if (productoSeleccionadoId != null && !productoSeleccionadoId.isBlank()) {
            cargarHistorialArticulo(productoSeleccionadoId);
        }
        // Opcional: recargar la lista de productos en el buscador (para reflejar altas/bajas)
        // Se puede hacer en segundo plano sin bloquear la UI
        Task<List<ProductoOpcion>> task = new Task<>() {
            @Override
            protected List<ProductoOpcion> call() {
                return cargarProductosActivos();
            }
            @Override
            protected void succeeded() {
                List<ProductoOpcion> resultado = getValue();
                productosCache.setAll(resultado != null ? resultado : List.of());
                // Mantener la selección actual si sigue existiendo
                String textoActual = buscarProducto.getEditor().getText();
                ProductoOpcion seleccionado = buscarProducto.getValue();
                if (seleccionado != null) {
                    // Verificar que el producto aún esté en la lista actualizada
                    boolean existe = productosCache.stream()
                            .anyMatch(p -> p.getId().equals(seleccionado.getId()));
                    if (!existe) {
                        // Si ya no existe, limpiar selección
                        buscarProducto.setValue(null);
                        productoSeleccionadoId = null;
                        limpiarDetalleProducto();
                        historialItems.clear();
                        actualizarTotales();
                    } else {
                        // Forzar actualización del texto visible (por si cambió nombre/descripción)
                        buscarProducto.getEditor().setText(seleccionado.getTextoVisible());
                    }
                }
                // Refiltrar la lista desplegable según el texto actual
                filtrarProductos(textoActual);
            }
        };
        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTipoMovimiento.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colTipoMovimientoDetalle.setCellValueFactory(new PropertyValueFactory<>("tipoMovimientoDetalle"));
        colAntes.setCellValueFactory(new PropertyValueFactory<>("antes"));
        colDespues.setCellValueFactory(new PropertyValueFactory<>("despues"));
        colEntradas.setCellValueFactory(new PropertyValueFactory<>("entradas"));
        colSalidas.setCellValueFactory(new PropertyValueFactory<>("salidas"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colFacturaEntrada.setCellValueFactory(new PropertyValueFactory<>("facturaEntrada"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colFacturaSalida.setCellValueFactory(new PropertyValueFactory<>("facturaSalida"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colPrecioTotal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        contenidoTabla.setItems(historialItems);
    }

    private void configurarBuscadorProducto() {
        buscarProducto.setItems(productosFiltrados);
        buscarProducto.setEditable(true);
        buscarProducto.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductoOpcion producto) {
                return producto == null ? "" : producto.getTextoVisible();
            }

            @Override
            public ProductoOpcion fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                String normalizado = texto.trim();
                for (ProductoOpcion producto : productosCache) {
                    if (producto.getTextoVisible().equalsIgnoreCase(normalizado)) {
                        return producto;
                    }
                }
                return null;
            }
        });

        Task<List<ProductoOpcion>> task = new Task<>() {
            @Override
            protected List<ProductoOpcion> call() {
                return cargarProductosActivos();
            }

            @Override
            protected void succeeded() {
                List<ProductoOpcion> resultado = getValue();
                productosCache.setAll(resultado != null ? resultado : List.of());
                productosFiltrados.setAll(productosCache);
            }

            @Override
            protected void failed() {
                productosCache.clear();
                productosFiltrados.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscarProducto.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoBusqueda) {
                return;
            }
            ProductoOpcion seleccionado = buscarProducto.getValue();
            if (seleccionado != null && seleccionado.getTextoVisible().equals(newText)) {
                return;
            }
            actualizandoBusqueda = true;
            try {
                filtrarProductos(newText);
            } finally {
                actualizandoBusqueda = false;
            }
        });

        buscarProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizandoBusqueda = true;
                buscarProducto.getEditor().setText(newVal.getTextoVisible());
                actualizandoBusqueda = false;
                productoSeleccionadoId = newVal.getId();
                cargarHistorialArticulo(newVal.getId());
            } else {
                productoSeleccionadoId = null;
                limpiarDetalleProducto();
            }
        });
    }

    private void configurarFiltros() {
        if (comboFiltro == null || comboValor == null) {
            return;
        }
        comboFiltro.getItems().setAll(
                "Fecha",
                "Hora",
                "Movimiento",
                "Tipo de movimiento",
                "Origen",
                "Factura entrada",
                "Destino",
                "Factura salida",
                "Usuario",
                "Estado",
                "Presentación",
                "Factor"
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

        if ("Factor".equals(campo) && obtenerValorFiltroActivo("Presentación") == null) {
            return;
        }

        List<String> valores = new ArrayList<>();
        for (HistorialArticuloItem item : historialCacheCompleto) {
            if ("Factor".equals(campo)) {
                String presentacionActiva = obtenerValorFiltroActivo("Presentación");
                if (presentacionActiva != null
                        && contieneValor(item.getPresentacion(), presentacionActiva)) {
                    for (String factor : separarValores(item.getFactor())) {
                        if (!valores.contains(factor)) {
                            valores.add(factor);
                        }
                    }
                }
                continue;
            }

            String valor = obtenerValorCampo(item, campo);
            if ("Presentación".equals(campo)) {
                for (String presentacion : separarValores(valor)) {
                    if (!valores.contains(presentacion)) {
                        valores.add(presentacion);
                    }
                }
            } else if (valor != null && !valor.isBlank() && !valores.contains(valor)) {
                valores.add(valor);
            }
        }
        comboValor.getItems().setAll(valores);
    }

    @FXML
    private void agregarFiltro() {
        String campo = comboFiltro.getValue();
        String valor = comboValor.getValue();

        if ("Factor".equals(campo) && obtenerValorFiltroActivo("Presentación") == null) {
            mostrarAdvertencia(
                    "Filtro incompleto",
                    "Debes seleccionar primero una Presentación para poder filtrar por Factor."
            );
            return;
        }

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
        aplicarFiltros();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(5);
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.getStyleClass().add("chip");

        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.getStyleClass().add("chip-text");

        Button quitar = new Button("✕");
        quitar.getStyleClass().add("chip-close");

        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            if ("Presentación".equals(filtro.campo)) {
                filtrosActivos.removeIf(f -> "Factor".equals(f.campo));
                reconstruirChipsFiltros();
            } else {
                contenedorFiltros.getChildren().remove(chip);
            }
            aplicarFiltros();
        });

        chip.getChildren().addAll(texto, quitar);

        if (contenedorFiltros.getChildren().isEmpty()) {
            HBox.setMargin(chip, new Insets(0, 0, 0, 25));
        }

        return chip;
    }

    private void reconstruirChipsFiltros() {
        if (contenedorFiltros == null) {
            return;
        }
        contenedorFiltros.getChildren().clear();
        for (Filtro filtroActivo : filtrosActivos) {
            contenedorFiltros.getChildren().add(crearChipFiltro(filtroActivo));
        }
    }

    private void aplicarFiltros() {
        LocalDate fechaInicioSeleccionada = fechaInicio != null ? fechaInicio.getValue() : null;
        LocalDate fechaFinSeleccionada = fechaFin != null ? fechaFin.getValue() : null;
        boolean filtroPresentacionFactorActivo = tieneFiltroPresentacionFactor();
        boolean cambioFiltroPresentacionFactor = filtroPresentacionFactorActivoPrevio != filtroPresentacionFactorActivo;
        List<HistorialArticuloItem> filtrados = new ArrayList<>();
        for (HistorialArticuloItem item : historialCacheCompleto) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                if (!coincideFiltro(item, filtro)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide && (fechaInicioSeleccionada != null || fechaFinSeleccionada != null)) {
                LocalDate fechaItem = parseFechaItem(item.getFecha());
                if (fechaItem == null) {
                    coincide = false;
                } else {
                    if (fechaInicioSeleccionada != null && fechaItem.isBefore(fechaInicioSeleccionada)) {
                        coincide = false;
                    }
                    if (coincide && fechaFinSeleccionada != null && fechaItem.isAfter(fechaFinSeleccionada)) {
                        coincide = false;
                    }
                }
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        List<HistorialArticuloItem> resultado = filtrados;
        if (filtroPresentacionFactorActivo) {
            resultado = recalcularExistencias(filtrados);
        }

        if (forzarOrdenFechaHora || filtroPresentacionFactorActivo || cambioFiltroPresentacionFactor) {
            resultado = ordenarPorFechaHora(resultado);
            historialItems.setAll(resultado);
            contenidoTabla.getSortOrder().clear();
        } else {
            historialItems.setAll(resultado);
            aplicarOrdenamiento();
        }

        filtroPresentacionFactorActivoPrevio = filtroPresentacionFactorActivo;
        forzarOrdenFechaHora = false;
        actualizarExistenciasSegunTabla(resultado);
        actualizarTotales();
        actualizarPresentacionFactorDesdeFiltros();
    }

    private boolean coincideFiltro(HistorialArticuloItem item, Filtro filtro) {
        String valor = obtenerValorCampo(item, filtro.campo);
        if ("Presentación".equals(filtro.campo) || "Factor".equals(filtro.campo)) {
            return contieneValor(valor, filtro.valor);
        }
        return valor != null && valor.equals(filtro.valor);
    }

    private String obtenerValorFiltroActivo(String campo) {
        for (Filtro filtro : filtrosActivos) {
            if (campo.equals(filtro.campo)) {
                return filtro.valor;
            }
        }
        return null;
    }

    private boolean contieneValor(String texto, String buscado) {
        if (buscado == null || buscado.isBlank()) {
            return false;
        }
        for (String valor : separarValores(texto)) {
            if (valor.equalsIgnoreCase(buscado.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> separarValores(String texto) {
        List<String> valores = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return valores;
        }
        String[] partes = texto.split(",");
        for (String parte : partes) {
            String limpio = parte == null ? "" : parte.trim();
            if (!limpio.isBlank() && !valores.contains(limpio)) {
                valores.add(limpio);
            }
        }
        return valores;
    }


    private void actualizarExistenciasSegunTabla(List<HistorialArticuloItem> items) {
        if (lblExistencias == null) {
            return;
        }
        if (items == null || items.isEmpty()) {
            lblExistencias.setText("Existencias: 0");
            return;
        }

        HistorialArticuloItem ultimo = null;
        LocalDateTime ultimaFechaHora = null;
        for (HistorialArticuloItem item : items) {
            LocalDateTime fechaHora = obtenerFechaHora(item.getFecha(), item.getHora());
            if (ultimo == null) {
                ultimo = item;
                ultimaFechaHora = fechaHora;
                continue;
            }
            if (ultimaFechaHora == null) {
                if (fechaHora != null) {
                    ultimo = item;
                    ultimaFechaHora = fechaHora;
                }
                continue;
            }
            if (fechaHora != null && fechaHora.isAfter(ultimaFechaHora)) {
                ultimo = item;
                ultimaFechaHora = fechaHora;
            }
        }

        int existencias = ultimo != null ? obtenerEnteroSeguro(ultimo.getDespues()) : 0;
        lblExistencias.setText("Existencias: " + existencias);
    }

    private String obtenerValorCampo(HistorialArticuloItem item, String campo) {
        switch (campo) {
            case "Fecha":
                return item.getFecha();
            case "Hora":
                return item.getHora();
            case "Movimiento":
                return item.getTipoMovimiento();
            case "Tipo de movimiento":
                return item.getTipoMovimientoDetalle();
            case "Origen":
                return item.getProveedor();
            case "Factura entrada":
                return item.getFacturaEntrada();
            case "Destino":
                return item.getCliente();
            case "Factura salida":
                return item.getFacturaSalida();
            case "Usuario":
                return item.getUsuario();
            case "Estado":
                return item.getEstado();
            case "Presentación":
                return item.getPresentacion();
            case "Factor":
                return item.getFactor();
            default:
                return "";
        }
    }



    private List<HistorialArticuloItem> ordenarPorFechaHora(List<HistorialArticuloItem> items) {
        List<HistorialArticuloItem> ordenados = new ArrayList<>(items);
        ordenados.sort(Comparator.comparing(
                item -> obtenerFechaHora(item.getFecha(), item.getHora()),
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return ordenados;
    }

    private boolean tieneFiltroPresentacionFactor() {
        for (Filtro filtro : filtrosActivos) {
            if ("Presentación".equals(filtro.campo) || "Factor".equals(filtro.campo)) {
                return true;
            }
        }
        return false;
    }

    private List<HistorialArticuloItem> recalcularExistencias(List<HistorialArticuloItem> items) {
        List<HistorialArticuloItem> recalculados = new ArrayList<>();
        int existencias = 0;
        for (HistorialArticuloItem item : items) {
            int entradas = obtenerEnteroSeguro(item.getEntradas());
            int salidas = obtenerEnteroSeguro(item.getSalidas());
            int antes = existencias;
            int despues = existencias + entradas - salidas;

            recalculados.add(new HistorialArticuloItem(
                    item.getFecha(),
                    item.getHora(),
                    item.getTipoMovimiento(),
                    item.getTipoMovimientoDetalle(),
                    String.valueOf(antes),
                    String.valueOf(despues),
                    item.getEntradas(),
                    item.getSalidas(),
                    item.getProveedor(),
                    item.getFacturaEntrada(),
                    item.getCliente(),
                    item.getFacturaSalida(),
                    item.getUsuario(),
                    item.getEstado(),
                    item.getPrecioTotal(),
                    item.getTotalEntradaMonto(),
                    item.getTotalSalidaMonto(),
                    item.getPresentacion(),
                    item.getFactor()
            ));
            existencias = despues;
        }
        return recalculados;
    }

    private int obtenerEnteroSeguro(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void actualizarPresentacionFactorDesdeFiltros() {
        if (lblPresentacion == null || lblFactor == null) {
            return;
        }
        String presentacion = "";
        String factor = "";
        for (Filtro filtro : filtrosActivos) {
            if ("Presentación".equals(filtro.campo)) {
                presentacion = filtro.valor;
            } else if ("Factor".equals(filtro.campo)) {
                factor = filtro.valor;
            }
        }
        lblPresentacion.setText(presentacion.isBlank() ? "Presentación:" : "Presentación: " + presentacion);
        lblFactor.setText(factor.isBlank() ? "Factor:" : "Factor: " + factor);
    }

    private void actualizarTotales() {
        if (totalEntradasGeneral == null || totalSalidasGeneral == null || diferenciaGeneral == null) {
            return;
        }

        double totalEntradas = 0;
        double totalSalidas = 0;

        for (HistorialArticuloItem item : historialItems) {
            totalEntradas += item.getTotalEntradaMonto();
            totalSalidas += item.getTotalSalidaMonto();
        }

        double diferencia = totalSalidas - totalEntradas;
        totalEntradasGeneral.setText(formatearImporte(totalEntradas));
        totalSalidasGeneral.setText(formatearImporte(totalSalidas));
        diferenciaGeneral.setText(formatearImporte(diferencia));
    }

    private String formatearImporte(Double valor) {
        if (valor == null) {
            return "";
        }
        return "$" + String.format(Locale.US, "%.2f", valor);
    }

    private double valorSeguroPrecio(Double valor) {
        return valor == null ? 0d : valor;
    }

    private Double obtenerNumeroDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Double.parseDouble(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void configurarFiltroFechas() {
        if (fechaInicio == null || fechaFin == null) {
            return;
        }
        fechaInicio.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        fechaFin.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
    }

    private LocalDate parseFechaItem(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaTexto, FORMATO_FECHA);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void filtrarProductos(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        ObservableList<ProductoOpcion> filtrados = FXCollections.observableArrayList();
        if (filtro.isEmpty()) {
            filtrados.setAll(productosCache);
        } else {
            for (ProductoOpcion producto : productosCache) {
                if (producto.coincide(filtro)) {
                    filtrados.add(producto);
                }
            }
        }
        String textoActual = buscarProducto.getEditor().getText();
        productosFiltrados.setAll(filtrados);
        buscarProducto.getEditor().setText(textoActual);
        buscarProducto.getEditor().positionCaret(textoActual != null ? textoActual.length() : 0);
        if (!filtrados.isEmpty() && buscarProducto.isFocused()) {
            buscarProducto.show();
        }
    }

    private List<ProductoOpcion> cargarProductosActivos() {
        Map<String, ProductoOpcion> productos = new LinkedHashMap<>();
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return List.of();
            }
            String sql = "SELECT p.id AS pid, p.nombre AS pname "
                    + "FROM productos p "
                    + "WHERE p.estado = 'activo' "
                    + "ORDER BY p.nombre";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("pid");
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    String nombre = rs.getString("pname");

                    // Obtener descripción usando GenericDAO
                    String descripcion = obtenerDescripcionProducto(id);

                    productos.put(id, new ProductoOpcion(id, nombre, descripcion));
                }
            }

            String sqlClaves = "SELECT idProducto, idAlterno FROM claves WHERE estado = 'activo'";
            try (PreparedStatement ps = conn.prepareStatement(sqlClaves);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idProducto = rs.getString("idProducto");
                    String idAlterno = rs.getString("idAlterno");
                    if (idProducto == null || idAlterno == null) {
                        continue;
                    }
                    ProductoOpcion producto = productos.get(idProducto);
                    if (producto != null) {
                        producto.agregarClaveAlterna(idAlterno);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(productos.values());
    }

    private String obtenerDescripcionProducto(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return "";
        }
        try {
            GenericDAO<producto> dao = new GenericDAO<>(producto.class);
            String resumen = dao.obtenerResumenProducto(idProducto);
            return resumen != null ? resumen : "";
        } catch (Exception e) {
            System.err.println("Error al obtener descripción del producto " + idProducto + ": " + e.getMessage());
            return "";
        }
    }

    private String construirDescripcion(String marca, String etiqueta, String clasificacion,
                                        String unidad, String material) {
        List<String> partes = new ArrayList<>();
        agregarParte(partes, marca);
        agregarParte(partes, etiqueta);
        agregarParte(partes, clasificacion);
        agregarParte(partes, unidad);
        agregarParte(partes, material);
        return String.join(", ", partes);
    }

    private String construirDescripcion(String marca, String etiqueta, String clasificacion,
                                        String unidad, String material, String descripcion) {
        List<String> partes = new ArrayList<>();
        agregarParte(partes, marca);
        agregarParte(partes, etiqueta);
        agregarParte(partes, clasificacion);
        agregarParte(partes, unidad);
        agregarParte(partes, material);
        agregarParte(partes, descripcion);
        return String.join(", ", partes);
    }

    private void agregarParte(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<HistorialArticuloItem, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<HistorialArticuloItem, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                });
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = List.of(
                "fecha",
                "hora",
                "movimiento",
                "tipoMovimientoDetalle",
                "antes",
                "despues",
                "entradas",
                "salidas",
                "proveedor",
                "facturaEntrada",
                "cliente",
                "facturaSalida",
                "usuario",
                "estado",
                "precioTotal"
        );
        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        TableColumn<HistorialArticuloItem, ?> columna = obtenerColumnaOrden();
        if (columna == null) {
            return;
        }
        columna.setSortType("asc".equalsIgnoreCase(direccionOrden)
                ? TableColumn.SortType.ASCENDING
                : TableColumn.SortType.DESCENDING);
        contenidoTabla.getSortOrder().setAll(columna);
        contenidoTabla.sort();
    }

    private TableColumn<HistorialArticuloItem, ?> obtenerColumnaOrden() {
        Map<String, TableColumn<HistorialArticuloItem, ?>> columnas = new HashMap<>();
        columnas.put("fecha", colFecha);
        columnas.put("hora", colHora);
        columnas.put("movimiento", colTipoMovimiento);
        columnas.put("tipoMovimientoDetalle", colTipoMovimientoDetalle);
        columnas.put("antes", colAntes);
        columnas.put("despues", colDespues);
        columnas.put("entradas", colEntradas);
        columnas.put("salidas", colSalidas);
        columnas.put("proveedor", colProveedor);
        columnas.put("facturaEntrada", colFacturaEntrada);
        columnas.put("cliente", colCliente);
        columnas.put("facturaSalida", colFacturaSalida);
        columnas.put("usuario", colUsuario);
        columnas.put("estado", colEstado);
        columnas.put("precioTotal", colPrecioTotal);
        return columnas.get(criterioOrden);
    }

    private void cargarHistorialArticulo(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            historialItems.clear();
            limpiarDetalleProducto();
            filtroPresentacionFactorActivoPrevio = false;
            forzarOrdenFechaHora = false;
            actualizarTotales();
            return;
        }

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                historialItems.clear();
                limpiarDetalleProducto();
                actualizarTotales();
                return;
            }

            cargarDetalleProducto(conn, idProducto);
            movimientos.addAll(obtenerEntradasArticulo(conn, idProducto));
            movimientos.addAll(obtenerSalidasArticulo(conn, idProducto));
            movimientos.addAll(obtenerAjustesArticulo(conn, idProducto));
            movimientos = consolidarMovimientosPorReferencia(movimientos);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        movimientos.sort(Comparator.comparing(MovimientoArticulo::getFechaHora,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<HistorialArticuloItem> nuevos = new ArrayList<>();
        int existencias = 0;
        for (MovimientoArticulo mov : movimientos) {
            int antes = existencias;
            int despues = existencias;
            String entradas = "0";
            String salidas = "0";
            boolean esCancelado = "cancelado".equalsIgnoreCase(valorTexto(mov.getEstado()).trim());

            if (!esCancelado) {
                if (mov.getCantidad() >= 0) {
                    despues = existencias + mov.getCantidad();
                    entradas = String.valueOf(mov.getCantidad());
                } else {
                    despues = existencias - Math.abs(mov.getCantidad());
                    salidas = String.valueOf(Math.abs(mov.getCantidad()));
                }
            }

            nuevos.add(new HistorialArticuloItem(
                    textoGuionSiVacio(mov.getFecha()),
                    textoGuionSiVacio(mov.getHora()),
                    textoGuionSiVacio(mov.getTipoMovimiento()),
                    textoGuionSiVacio(mov.getTipoMovimientoDetalle()),
                    String.valueOf(antes),
                    String.valueOf(despues),
                    entradas,
                    salidas,
                    textoGuionSiVacio(mov.getProveedor()),
                    textoGuionSiVacio(mov.getFacturaEntrada()),
                    textoGuionSiVacio(mov.getCliente()),
                    textoGuionSiVacio(mov.getFacturaSalida()),
                    textoGuionSiVacio(mov.getUsuario()),
                    textoGuionSiVacio(mov.getEstado()),
                    textoGuionSiVacio(formatearImporte(mov.getPrecioTotal())),
                    esCancelado ? 0d : (mov.getCantidad() >= 0 ? valorSeguroPrecio(mov.getPrecioTotal()) : 0d),
                    esCancelado ? 0d : (mov.getCantidad() < 0 ? valorSeguroPrecio(mov.getPrecioTotal()) : 0d),
                    textoGuionSiVacio(mov.getPresentacion()),
                    textoGuionSiVacio(mov.getFactor())
            ));
            existencias = despues;
        }

        historialCacheCompleto.clear();
        historialCacheCompleto.addAll(nuevos);
        historialItemsOriginal.setAll(historialCacheCompleto);
        filtroPresentacionFactorActivoPrevio = false;
        forzarOrdenFechaHora = true;
        reiniciarFiltros();
        aplicarFiltros();
    }

    private List<MovimientoArticulo> consolidarMovimientosPorReferencia(List<MovimientoArticulo> movimientos) {
        Map<String, MovimientoArticulo> consolidados = new LinkedHashMap<>();

        for (MovimientoArticulo actual : movimientos) {
            String llave = String.join("|",
                    valorTexto(actual.getTipoMovimiento()),
                    valorTexto(actual.getReferenciaMovimiento()),
                    valorTexto(actual.getTipoMovimientoDetalle()));

            MovimientoArticulo previo = consolidados.get(llave);
            if (previo == null) {
                consolidados.put(llave, actual);
                continue;
            }

            int cantidad = previo.getCantidad() + actual.getCantidad();
            Double totalPrevio = valorSeguroPrecio(previo.getPrecioTotal());
            Double totalActual = valorSeguroPrecio(actual.getPrecioTotal());
            Double total = totalPrevio + totalActual;

            String estado = estadoPrioritario(previo.getEstado(), actual.getEstado());

            consolidados.put(llave, new MovimientoArticulo(
                    previo.getFecha(),
                    previo.getHora(),
                    previo.getTipoMovimiento(),
                    previo.getTipoMovimientoDetalle(),
                    cantidad,
                    previo.getProveedor(),
                    previo.getFacturaEntrada(),
                    previo.getCliente(),
                    previo.getFacturaSalida(),
                    previo.getUsuario(),
                    estado,
                    total,
                    previo.getReferenciaMovimiento(),
                    !valorTexto(previo.getPresentacion()).isBlank() ? previo.getPresentacion() : actual.getPresentacion(),
                    !valorTexto(previo.getFactor()).isBlank() ? previo.getFactor() : actual.getFactor()
            ));
        }

        return new ArrayList<>(consolidados.values());
    }

    private String estadoPrioritario(String estadoA, String estadoB) {
        if ("cancelado".equalsIgnoreCase(valorTexto(estadoA).trim()) || "cancelado".equalsIgnoreCase(valorTexto(estadoB).trim())) {
            return "cancelado";
        }
        return !valorTexto(estadoA).isBlank() ? estadoA : estadoB;
    }

    private void reiniciarFiltros() {
        restaurandoFiltros = true;
        filtrosActivos.clear();
        if (contenedorFiltros != null) {
            contenedorFiltros.getChildren().clear();
        }
        if (comboFiltro != null) {
            comboFiltro.setValue(null);
        }
        if (comboValor != null) {
            comboValor.getItems().clear();
            comboValor.setValue(null);
        }
        restaurandoFiltros = false;
        actualizarValoresFiltro(comboFiltro != null ? comboFiltro.getValue() : null);
    }

    private List<MovimientoArticulo> obtenerEntradasArticulo(Connection conn, String idProducto) throws SQLException {
        String sql = "SELECT e.idEntrada, e.fechaEntrada, e.horaEntrada, e.tipoEntrada, e.noFactura, "
                + "e.claveUsuarioEntrada, u.userName AS usuarioNombre, "
                + "e.idRemitente, p.Nombre AS proveedorNombre, s.nombre AS sucursalNombre, "
                + "de.cantidad, e.Estado, CASE WHEN LOWER(e.Estado) = 'cancelado' THEN e.precioTotalEntrada ELSE de.precioTotal END AS precioTotalMovimiento, "
                + "pa.presentaciones AS presentacionMovimiento, pa.factores AS factorMovimiento "
                + "FROM detalle_Entrada de "
                + "JOIN entradas e ON e.idEntrada = de.claveEntrada "
                + "LEFT JOIN usuarios u ON u.idUsuario = e.claveUsuarioEntrada "
                + "LEFT JOIN proveedores p ON p.id = e.idRemitente "
                + "LEFT JOIN sucursales s ON s.id = e.idRemitente "
                + "LEFT JOIN (SELECT idDetalleEntrada, GROUP_CONCAT(DISTINCT presentacion ORDER BY presentacion SEPARATOR \", \") AS presentaciones, "
                + "GROUP_CONCAT(DISTINCT factor ORDER BY factor SEPARATOR \", \") AS factores FROM articulo GROUP BY idDetalleEntrada) pa "
                + "ON pa.idDetalleEntrada = de.idDetalleEntrada "
                + "WHERE de.claveProducto = ?";

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoEntrada = valorTexto(rs.getObject("tipoEntrada"));
                    String origen = resolverOrigenEntrada(
                            tipoEntrada,
                            valorTexto(rs.getObject("proveedorNombre")),
                            valorTexto(rs.getObject("sucursalNombre"))
                    );
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaEntrada")),
                            valorTexto(rs.getObject("horaEntrada")),
                            "Entrada",
                            tipoEntrada,
                            obtenerCantidad(rs.getObject("cantidad")),
                            origen,
                            valorTexto(rs.getObject("noFactura")),
                            "",
                            "",
                            valorTexto(rs.getObject("usuarioNombre")),
                            valorTexto(rs.getObject("Estado")),
                            obtenerNumeroDecimal(rs.getObject("precioTotalMovimiento")),
                            valorTexto(rs.getObject("idEntrada")),
                            valorTexto(rs.getObject("presentacionMovimiento")),
                            valorTexto(rs.getObject("factorMovimiento"))
                    ));
                }
            }
        }

        return movimientos;
    }

    private List<MovimientoArticulo> obtenerSalidasArticulo(Connection conn, String idProducto) throws SQLException {
        String sql = "SELECT s.idSalida, s.fechaSalida, s.horaSalida, s.tipoSalida, s.noFactura, "
                + "s.claveUsuarioSalida, u.userName AS usuarioNombre, "
                + "s.idDestinatario, c.Nombre AS clienteNombre, su.nombre AS sucursalNombre, "
                + "ds.cantidad, s.Estado, CASE WHEN LOWER(s.Estado) = 'cancelado' THEN s.precioTotalSalida ELSE ds.precioTotalSalida END AS precioTotalMovimiento, "
                + "pa.presentaciones AS presentacionMovimiento, pa.factores AS factorMovimiento "
                + "FROM detalle_Salida ds "
                + "JOIN salidas s ON s.idSalida = ds.claveSalida "
                + "LEFT JOIN usuarios u ON u.idUsuario = s.claveUsuarioSalida "
                + "LEFT JOIN clientes c ON c.id = s.idDestinatario "
                + "LEFT JOIN sucursales su ON su.id = s.idDestinatario "
                + "LEFT JOIN (SELECT idDetalleSalida, GROUP_CONCAT(DISTINCT presentacion ORDER BY presentacion SEPARATOR \", \") AS presentaciones, "
                + "GROUP_CONCAT(DISTINCT factor ORDER BY factor SEPARATOR \", \") AS factores FROM articulo WHERE idDetalleSalida IS NOT NULL GROUP BY idDetalleSalida) pa "
                + "ON pa.idDetalleSalida = ds.idDetalleSalida "
                + "WHERE ds.claveProductoSalida = ?";

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoSalida = valorTexto(rs.getObject("tipoSalida"));
                    String destino = resolverDestinoSalida(
                            tipoSalida,
                            valorTexto(rs.getObject("clienteNombre")),
                            valorTexto(rs.getObject("sucursalNombre"))
                    );
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaSalida")),
                            valorTexto(rs.getObject("horaSalida")),
                            "Salida",
                            tipoSalida,
                            -obtenerCantidad(rs.getObject("cantidad")),
                            "",
                            "",
                            destino,
                            valorTexto(rs.getObject("noFactura")),
                            valorTexto(rs.getObject("usuarioNombre")),
                            valorTexto(rs.getObject("Estado")),
                            obtenerNumeroDecimal(rs.getObject("precioTotalMovimiento")),
                            valorTexto(rs.getObject("idSalida")),
                            valorTexto(rs.getObject("presentacionMovimiento")),
                            valorTexto(rs.getObject("factorMovimiento"))
                    ));
                }
            }
        }

        return movimientos;
    }

    private List<MovimientoArticulo> obtenerAjustesArticulo(Connection conn, String idProducto) throws SQLException {
        List<MovimientoArticulo> movimientos = new ArrayList<>();

        String sqlAjustesEntrada = "SELECT ai.idAjuste, ai.fechaAjuste, ai.horaAjuste, ai.estado, ai.idUsuario, u.userName AS usuarioNombre, "
                + "de.cantidad, de.precioTotal AS precioTotalMovimiento "
                + "FROM ajuste_inventario ai "
                + "JOIN detalle_Entrada de ON de.claveEntrada = ai.idAjuste "
                + "LEFT JOIN usuarios u ON u.idUsuario = ai.idUsuario "
                + "WHERE de.claveProducto = ? AND ai.idAjuste LIKE ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlAjustesEntrada)) {
            ps.setString(1, idProducto);
            ps.setString(2, "%A");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaAjuste")),
                            valorTexto(rs.getObject("horaAjuste")),
                            "Ajuste",
                            "Entrada",
                            obtenerCantidad(rs.getObject("cantidad")),
                            "",
                            "",
                            "",
                            "",
                            valorTexto(rs.getObject("usuarioNombre")),
                            valorTexto(rs.getObject("estado")),
                            obtenerNumeroDecimal(rs.getObject("precioTotalMovimiento")),
                            valorTexto(rs.getObject("idAjuste")),
                            "",
                            ""
                    ));
                }
            }
        }

        String sqlAjustesSalida = "SELECT ai.idAjuste, ai.fechaAjuste, ai.horaAjuste, ai.estado, ai.idUsuario, u.userName AS usuarioNombre, "
                + "ds.cantidad, ds.precioTotalSalida AS precioTotalMovimiento "
                + "FROM ajuste_inventario ai "
                + "JOIN detalle_Salida ds ON ds.claveSalida = ai.idAjuste "
                + "LEFT JOIN usuarios u ON u.idUsuario = ai.idUsuario "
                + "WHERE ds.claveProductoSalida = ? AND ai.idAjuste LIKE ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlAjustesSalida)) {
            ps.setString(1, idProducto);
            ps.setString(2, "%A");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaAjuste")),
                            valorTexto(rs.getObject("horaAjuste")),
                            "Ajuste",
                            "Salida",
                            -obtenerCantidad(rs.getObject("cantidad")),
                            "",
                            "",
                            "",
                            "",
                            valorTexto(rs.getObject("usuarioNombre")),
                            valorTexto(rs.getObject("estado")),
                            obtenerNumeroDecimal(rs.getObject("precioTotalMovimiento")),
                            valorTexto(rs.getObject("idAjuste")),
                            "",
                            ""
                    ));
                }
            }
        }

        return movimientos;
    }

    private int obtenerCantidad(Object valor) {
        if (valor == null) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private String resolverOrigenEntrada(String tipoEntrada, String proveedorNombre, String sucursalNombre) {
        if (esTraspaso(tipoEntrada)) {
            return !sucursalNombre.isBlank() ? sucursalNombre : proveedorNombre;
        }
        return !proveedorNombre.isBlank() ? proveedorNombre : sucursalNombre;
    }

    private String resolverDestinoSalida(String tipoSalida, String clienteNombre, String sucursalNombre) {
        if (esTraspaso(tipoSalida)) {
            return !sucursalNombre.isBlank() ? sucursalNombre : clienteNombre;
        }
        return !clienteNombre.isBlank() ? clienteNombre : sucursalNombre;
    }

    private boolean esTraspaso(String tipoMovimiento) {
        return tipoMovimiento != null && tipoMovimiento.toLowerCase(Locale.ROOT).contains("traspaso");
    }

    private String textoGuionSiVacio(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    private LocalDateTime obtenerFechaHora(String fechaTexto, String horaTexto) {
        if ((fechaTexto == null || fechaTexto.isBlank()) && (horaTexto == null || horaTexto.isBlank())) {
            return null;
        }
        LocalDate fecha = null;
        LocalTime hora = null;
        if (fechaTexto != null && !fechaTexto.isBlank()) {
            try {
                fecha = LocalDate.parse(fechaTexto, FORMATO_FECHA);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (horaTexto != null && !horaTexto.isBlank()) {
            try {
                hora = LocalTime.parse(horaTexto, FORMATO_HORA);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (fecha == null && hora == null) {
            return null;
        }
        if (fecha == null) {
            fecha = LocalDate.MIN;
        }
        if (hora == null) {
            hora = LocalTime.MIN;
        }
        return LocalDateTime.of(fecha, hora);
    }

    private void cargarDetalleProducto(Connection conn, String idProducto) throws SQLException {
        String sqlProducto = "SELECT p.id, p.categoria, p.material, p.unidadMedida, p.descripcion, "
                + "m.nombre AS marca, e.nombre AS etiqueta "
                + "FROM productos p "
                + "LEFT JOIN marcas m ON m.id = p.marca "
                + "LEFT JOIN etiquetas e ON e.id = p.etiqueta "
                + "WHERE p.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlProducto)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String descripcion = obtenerDescripcionProducto(idProducto);
                    lblClave.setText("Clave: " + idProducto);
                    lblDescripcion.setText("Descripción: " + descripcion);
                }
            }
        }

        lblPresentacion.setText("Presentación:");
        lblFactor.setText("Factor:");

        String sqlExistencias = "SELECT COUNT(*) AS total "
                + "FROM articulo a "
                + "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada "
                + "WHERE de.claveProducto = ? AND LOWER(a.Estado) = 'disponible'";
        try (PreparedStatement ps = conn.prepareStatement(sqlExistencias)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblExistencias.setText("Existencias: " + rs.getInt("total"));
                }
            }
        }
    }

    private void limpiarDetalleProducto() {
        lblClave.setText("Clave:");
        lblDescripcion.setText("Descripción:");
        lblPresentacion.setText("Presentación:");
        lblFactor.setText("Factor:");
        lblExistencias.setText("Existencias:");
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Historial por artículo", "excel",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        double entradas = parseImporteTexto(totalEntradasGeneral.getText());
        double salidas = parseImporteTexto(totalSalidasGeneral.getText());
        double diferencia = parseImporteTexto(diferenciaGeneral.getText());

        exportador.TotalesReporte totales = new exportador.TotalesReporte(
                "Total entradas", entradas,
                "Total salidas", salidas,
                "Diferencia", diferencia
        );

        exportador.exportarTabla(contenidoTabla, "Historial por artículo", "pdf",
                obtenerFiltrosAplicados(), totales);
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        double entradas = parseImporteTexto(totalEntradasGeneral.getText());
        double salidas = parseImporteTexto(totalSalidasGeneral.getText());
        double diferencia = parseImporteTexto(diferenciaGeneral.getText());

        exportador.TotalesReporte totales = new exportador.TotalesReporte(
                "Total entradas", entradas,
                "Total salidas", salidas,
                "Diferencia", diferencia
        );

        exportador.previsualizarPDF(contenidoTabla, "Historial por artículo",
                obtenerFiltrosAplicados(), totales);
    }


    private double parseImporteTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0d;
        }
        String normalizado = texto.replace("$", "").replace(",", "").trim();
        if (normalizado.isBlank()) {
            return 0d;
        }
        try {
            return Double.parseDouble(normalizado);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private List<String> obtenerFiltrosAplicados() {
        List<String> filtrosAplicados = new ArrayList<>();
        ProductoOpcion seleccionado = buscarProducto.getValue();
        String texto = buscarProducto.getEditor().getText();
        if (seleccionado != null) {
            filtrosAplicados.add("Producto: " + seleccionado.getTextoVisible());
        } else if (texto != null && !texto.isBlank()) {
            filtrosAplicados.add("Producto contiene: " + texto.trim());
        }
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }
        filtrosAplicados.add(obtenerPeriodoExport());
        return filtrosAplicados;
    }

    private String obtenerPeriodoExport() {
        LocalDate inicio = fechaInicio != null ? fechaInicio.getValue() : null;
        LocalDate fin = fechaFin != null ? fechaFin.getValue() : null;
        if (inicio == null && fin == null) {
            return PERIODO_EXPORT;
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yy");
        String textoInicio = inicio != null ? inicio.format(formato) : "...";
        String textoFin = fin != null ? fin.format(formato) : "...";
        return "Periodo: " + textoInicio + "-" + textoFin;
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private static class ProductoOpcion {
        private final String id;
        private final String nombre;
        private final String descripcion;
        private final Set<String> clavesAlternas = new LinkedHashSet<>();

        private ProductoOpcion(String id, String nombre, String descripcion) {
            this.id = id;
            this.nombre = nombre == null ? "" : nombre;
            this.descripcion = descripcion == null ? "" : descripcion;
        }

        private void agregarClaveAlterna(String clave) {
            if (clave == null || clave.isBlank()) {
                return;
            }
            clavesAlternas.add(clave.trim());
        }

        private String getId() {
            return id;
        }

        private String getTextoVisible() {
            String base = id + " - " + nombre;
            if (descripcion.isBlank()) {
                return base;
            }
            return base + " - " + descripcion;
        }

        private boolean coincide(String filtro) {
            String normalizado = filtro.toLowerCase(Locale.ROOT);
            if (id != null && id.toLowerCase(Locale.ROOT).contains(normalizado)) {
                return true;
            }
            if (nombre != null && nombre.toLowerCase(Locale.ROOT).contains(normalizado)) {
                return true;
            }
            for (String clave : clavesAlternas) {
                if (clave.toLowerCase(Locale.ROOT).contains(normalizado)) {
                    return true;
                }
            }
            return getTextoVisible().toLowerCase(Locale.ROOT).contains(normalizado);
        }
    }

    private class MovimientoArticulo {
        private final String fecha;
        private final String hora;
        private final String tipoMovimiento;
        private final String tipoMovimientoDetalle;
        private final int cantidad;
        private final String proveedor;
        private final String facturaEntrada;
        private final String cliente;
        private final String facturaSalida;
        private final String usuario;
        private final String estado;
        private final Double precioTotal;
        private final String referenciaMovimiento;
        private final String presentacion;
        private final String factor;

        private MovimientoArticulo(String fecha, String hora, String tipoMovimiento, String tipoMovimientoDetalle, int cantidad,
                                   String proveedor, String facturaEntrada, String cliente,
                                   String facturaSalida, String usuario, String estado, Double precioTotal, String referenciaMovimiento,
                                   String presentacion, String factor) {
            this.fecha = fecha;
            this.hora = hora;
            this.tipoMovimiento = tipoMovimiento;
            this.tipoMovimientoDetalle = tipoMovimientoDetalle;
            this.cantidad = cantidad;
            this.proveedor = proveedor;
            this.facturaEntrada = facturaEntrada;
            this.cliente = cliente;
            this.facturaSalida = facturaSalida;
            this.usuario = usuario;
            this.estado = estado;
            this.precioTotal = precioTotal;
            this.referenciaMovimiento = referenciaMovimiento;
            this.presentacion = presentacion;
            this.factor = factor;
        }

        private LocalDateTime getFechaHora() {
            return obtenerFechaHora(fecha, hora);
        }

        private String getFecha() {
            return fecha;
        }

        private String getHora() {
            return hora;
        }

        private String getTipoMovimiento() {
            return tipoMovimiento;
        }

        private String getTipoMovimientoDetalle() {
            return tipoMovimientoDetalle;
        }

        private int getCantidad() {
            return cantidad;
        }

        private String getProveedor() {
            return proveedor;
        }

        private String getFacturaEntrada() {
            return facturaEntrada;
        }

        private String getCliente() {
            return cliente;
        }

        private String getFacturaSalida() {
            return facturaSalida;
        }

        private String getUsuario() {
            return usuario;
        }

        private String getEstado() {
            return estado;
        }

        private Double getPrecioTotal() {
            return precioTotal;
        }

        private String getReferenciaMovimiento() {
            return referenciaMovimiento;
        }

        private String getPresentacion() {
            return presentacion;
        }

        private String getFactor() {
            return factor;
        }
    }

    public static class HistorialArticuloItem {
        private final String fecha;
        private final String hora;
        private final String tipoMovimiento;
        private final String tipoMovimientoDetalle;
        private final String antes;
        private final String despues;
        private final String entradas;
        private final String salidas;
        private final String proveedor;
        private final String facturaEntrada;
        private final String cliente;
        private final String facturaSalida;
        private final String usuario;
        private final String estado;
        private final String precioTotal;
        private final double totalEntradaMonto;
        private final double totalSalidaMonto;
        private final String presentacion;
        private final String factor;

        public HistorialArticuloItem(String fecha, String hora, String tipoMovimiento, String tipoMovimientoDetalle, String antes, String despues,
                                     String entradas, String salidas, String proveedor, String facturaEntrada,
                                     String cliente, String facturaSalida, String usuario, String estado,
                                     String precioTotal, double totalEntradaMonto, double totalSalidaMonto,
                                     String presentacion, String factor) {
            this.fecha = fecha;
            this.hora = hora;
            this.tipoMovimiento = tipoMovimiento;
            this.tipoMovimientoDetalle = tipoMovimientoDetalle;
            this.antes = antes;
            this.despues = despues;
            this.entradas = entradas;
            this.salidas = salidas;
            this.proveedor = proveedor;
            this.facturaEntrada = facturaEntrada;
            this.cliente = cliente;
            this.facturaSalida = facturaSalida;
            this.usuario = usuario;
            this.estado = estado;
            this.precioTotal = precioTotal;
            this.totalEntradaMonto = totalEntradaMonto;
            this.totalSalidaMonto = totalSalidaMonto;
            this.presentacion = presentacion;
            this.factor = factor;
        }

        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getTipoMovimiento() { return tipoMovimiento; }
        public String getTipoMovimientoDetalle() { return tipoMovimientoDetalle; }
        public String getAntes() { return antes; }
        public String getDespues() { return despues; }
        public String getEntradas() { return entradas; }
        public String getSalidas() { return salidas; }
        public String getProveedor() { return proveedor; }
        public String getFacturaEntrada() { return facturaEntrada; }
        public String getCliente() { return cliente; }
        public String getFacturaSalida() { return facturaSalida; }
        public String getUsuario() { return usuario; }
        public String getEstado() { return estado; }
        public String getPrecioTotal() { return precioTotal; }
        public double getTotalEntradaMonto() { return totalEntradaMonto; }
        public double getTotalSalidaMonto() { return totalSalidaMonto; }
        public String getPresentacion() { return presentacion; }
        public String getFactor() { return factor; }
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
