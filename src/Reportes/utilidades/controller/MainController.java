package Reportes.utilidades.controller;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import Compartido.helper.RefrescoHelper;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Compartido.helper.AtajosTecladoHelper;
import Reportes.utilidades.model.UtilidadItem;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private DatePicker fechaInicio;
    @FXML private DatePicker fechaFin;
    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;
    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<UtilidadItem> contenidoTabla;
    @FXML private TableColumn<UtilidadItem, String> colClaveProducto;
    @FXML private TableColumn<UtilidadItem, String> colNombreProducto;
    @FXML private TableColumn<UtilidadItem, String> colCategoria;
    @FXML private TableColumn<UtilidadItem, String> colDescripcionProducto;
    @FXML private TableColumn<UtilidadItem, String> colPresentacion;
    @FXML private TableColumn<UtilidadItem, String> colFactor;
    @FXML private TableColumn<UtilidadItem, String> colCantidad;
    @FXML private TableColumn<UtilidadItem, String> colTotalCompra;
    @FXML private TableColumn<UtilidadItem, String> colProveedor;
    @FXML private TableColumn<UtilidadItem, String> colFacturaCompra;
    @FXML private TableColumn<UtilidadItem, String> colTotalVenta;
    @FXML private TableColumn<UtilidadItem, String> colCliente;
    @FXML private TableColumn<UtilidadItem, String> colFacturaVenta;
    @FXML private TableColumn<UtilidadItem, String> colPorcentajeUtilidad;
    @FXML private TableColumn<UtilidadItem, String> colUtilidadPesos;
    @FXML private TextField totalCompraGeneral;
    @FXML private TextField totalVentaGeneral;
    @FXML private TextField totalUtilidadGeneral;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;
    private final ObservableList<UtilidadItem> utilidades = FXCollections.observableArrayList();
    private final ObservableList<UtilidadItem> utilidadesOriginal = FXCollections.observableArrayList();
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;
    private String criterioOrden = "producto";
    private String direccionOrden = "asc";
    private static final DateTimeFormatter FORMATO_FECHA_ALT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

            fechaInicio.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
            fechaInicio.prefHeightProperty().bind(root.heightProperty().multiply(0.04));
            fechaFin.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
            fechaFin.prefHeightProperty().bind(root.heightProperty().multiply(0.04));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.78));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.72));

            configurarColumnas();
            configurarFiltros();
            configurarFiltroFechas();
            //agregarListenersRedimension();
            cargarUtilidades();
            Platform.runLater(() -> {
                /*Platform.runLater(() -> {
                    actualizarPoliticaRedimensionamiento();
                })*/;
            });
            RefrescoHelper.setVistaActual("utilidades");
            RefrescoHelper.registrarRefresco("utilidades", this::cargarUtilidades);
        });
    }

//    private void actualizarPoliticaRedimensionamiento() {
//        List<TableColumn<UtilidadItem, ?>> columnasVisibles = contenidoTabla.getColumns().stream()
//                .filter(TableColumn::isVisible)
//                .collect(Collectors.toList());
//
//        Platform.runLater(() -> {
//            double anchoDisponible = contenidoTabla.getWidth();
//            if (anchoDisponible <= 0) {
//                anchoDisponible = Math.max(100, contenedorTabla.getWidth());
//            }
//
//            double minWidthTotal = columnasVisibles.stream()
//                    .mapToDouble(TableColumn::getMinWidth)
//                    .sum();
//
//            // Margen del 5% para evitar problemas de redondeo
//            boolean columnasCaben = minWidthTotal <= (anchoDisponible * 1.05);
//
//            if (columnasCaben) {
//                contenidoTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
//                // Resetear prefWidth para distribución equitativa
//                for (TableColumn<UtilidadItem, ?> col : columnasVisibles) {
//                    col.setPrefWidth(-1);
//                }
//            } else {
//                contenidoTabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
//                // Si excede en más del 20%, ajustar proporcionalmente
//                if (minWidthTotal > anchoDisponible * 1.2) {
//                    double factor = (anchoDisponible * 0.9) / minWidthTotal;
//                    for (TableColumn<UtilidadItem, ?> col : columnasVisibles) {
//                        col.setPrefWidth(col.getMinWidth() * factor);
//                    }
//                }
//            }
//
//            contenidoTabla.requestLayout();
//        });
//    }

//    private void agregarListenersRedimension() {
//        // Listener del contenedor de la tabla (doble runLater, igual que Inventario)
//        contenedorTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal.doubleValue() > 0) {
//                Platform.runLater(() -> {
//                    Platform.runLater(this::actualizarPoliticaRedimensionamiento);
//                });
//            }
//        });
//
//        // Listener de la tabla misma
//        contenidoTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal.doubleValue() > 0 && newVal.doubleValue() != oldVal.doubleValue()) {
//                Platform.runLater(this::actualizarPoliticaRedimensionamiento);
//            }
//        });
//    }

    private void configurarColumnas() {
        colClaveProducto.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colNombreProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcionProducto"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colTotalCompra.setCellValueFactory(new PropertyValueFactory<>("totalCompra"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colFacturaCompra.setCellValueFactory(new PropertyValueFactory<>("facturaCompra"));
        colTotalVenta.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colFacturaVenta.setCellValueFactory(new PropertyValueFactory<>("facturaVenta"));
        colPorcentajeUtilidad.setCellValueFactory(new PropertyValueFactory<>("porcentajeUtilidad"));
        colUtilidadPesos.setCellValueFactory(new PropertyValueFactory<>("utilidadPesos"));

        TableColumn<UtilidadItem, ?>[] columnas = new TableColumn[] {
                colClaveProducto,
                colNombreProducto,
                colCategoria,
                colDescripcionProducto,
                colPresentacion,
                colFactor,
                colCantidad,
                colTotalCompra,
                colProveedor,
                colFacturaCompra,
                colTotalVenta,
                colCliente,
                colFacturaVenta,
                colPorcentajeUtilidad,
                colUtilidadPesos
        };

        for (TableColumn<UtilidadItem, ?> columna : columnas) {
            columna.setStyle("-fx-alignment: CENTER;");
        }

        colClaveProducto.setMinWidth(80);
        colNombreProducto.setMinWidth(150);
        colCategoria.setMinWidth(100);
        colDescripcionProducto.setMinWidth(200);
        colPresentacion.setMinWidth(80);
        colFactor.setMinWidth(60);
        colCantidad.setMinWidth(60);
        colTotalCompra.setMinWidth(90);
        colProveedor.setMinWidth(120);
        colFacturaCompra.setMinWidth(100);
        colTotalVenta.setMinWidth(90);
        colCliente.setMinWidth(120);
        colFacturaVenta.setMinWidth(100);
        colPorcentajeUtilidad.setMinWidth(90);
        colUtilidadPesos.setMinWidth(90);

        contenidoTabla.setItems(utilidades);
    }

    private void cargarUtilidades() {
        utilidades.clear();
        List<UtilidadItem> registros = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }
            registros.addAll(obtenerUtilidades(conn));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        utilidadesOriginal.setAll(registros);
        actualizarValoresFiltro(comboFiltro.getValue());
        aplicarFiltrosYBusqueda();
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

    private List<UtilidadItem> obtenerUtilidades(Connection conn) throws SQLException {
        String query = "SELECT s.idSalida, s.noFactura AS facturaVenta, s.fechaSalida AS fechaSalida, s.idDestinatario, "
                + "c.Nombre AS cliente, ds.idDetalleSalida, ds.claveProductoSalida AS claveProducto, "
                + "ds.precioUnitarioSalida, ds.precioIVASalida, ds.precioTotalSalida, ds.cantidad AS cantidadSalida, "
                + "a.idDetalleEntrada, a.presentacion, a.factor, "
                + "de.idDetalleEntrada AS detalleEntradaId, de.claveEntrada, de.precioUnitario AS precioUnitarioEntrada, "
                + "de.precioIVA AS precioIVAEntrada, de.precioTotal AS precioTotalEntrada, de.cantidad AS cantidadEntrada, "
                + "e.noFactura AS facturaCompra, e.idRemitente, p.Nombre AS proveedor, "
                + "pr.nombre AS nombreProducto, pr.categoria AS categoria, pr.material AS material, "
                + "pr.unidadMedida AS unidadMedida, pr.descripcion AS descripcionProducto, et.nombre AS etiquetaNombre "
                + "FROM articulo a "
                + "JOIN detalle_Salida ds ON ds.idDetalleSalida = a.idDetalleSalida "
                + "JOIN salidas s ON s.idSalida = ds.claveSalida "
                + "LEFT JOIN clientes c ON s.idDestinatario = c.id "
                + "LEFT JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada "
                + "LEFT JOIN entradas e ON e.idEntrada = de.claveEntrada "
                + "LEFT JOIN proveedores p ON e.idRemitente = p.id "
                + "LEFT JOIN productos pr ON pr.id = ds.claveProductoSalida "
                + "LEFT JOIN etiquetas et ON et.id = pr.etiqueta "
                + "WHERE LOWER(TRIM(s.tipoSalida)) = 'venta' "
                + "AND LOWER(TRIM(s.Estado)) = 'finalizado'";
        Map<String, UtilidadAcumulado> acumulados = new LinkedHashMap<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int salidaId = rs.getInt("idSalida");
                Integer detalleEntradaId = obtenerEntero(rs.getObject("detalleEntradaId"));
                String claveProducto = valorTexto(rs.getObject("claveProducto"));
                String facturaVenta = valorTexto(rs.getObject("facturaVenta"));
                String facturaCompra = valorTexto(rs.getObject("facturaCompra"));
                String proveedor = valorTexto(rs.getObject("proveedor"));
                String cliente = valorTexto(rs.getObject("cliente"));
                String fechaSalida = valorTexto(rs.getObject("fechaSalida"));
                String nombreProducto = valorTexto(rs.getObject("nombreProducto"));
                String categoria = valorTexto(rs.getObject("categoria"));
                String presentacion = valorTexto(rs.getObject("presentacion"));
                String factor = valorTexto(rs.getObject("factor"));
                String material = valorTexto(rs.getObject("material"));
                String unidad = valorTexto(rs.getObject("unidadMedida"));
                String etiqueta = valorTexto(rs.getObject("etiquetaNombre"));
                String descripcionProducto = valorTexto(rs.getObject("descripcionProducto"));
                String descripcion = obtenerDescripcionProducto(claveProducto);

                double costoUnitario = obtenerCostoUnitario(rs);
                double ventaUnitario = obtenerVentaUnitario(rs);

                String key = salidaId + "|" + Objects.toString(detalleEntradaId, "-") + "|" + claveProducto;
                UtilidadAcumulado acumulado = acumulados.computeIfAbsent(key, k -> new UtilidadAcumulado(
                        salidaId,
                        detalleEntradaId,
                        claveProducto,
                        nombreProducto,
                        categoria,
                        descripcion,
                        presentacion,
                        factor,
                        facturaCompra,
                        proveedor,
                        facturaVenta,
                        cliente,
                        fechaSalida
                ));
                acumulado.cantidad++;
                acumulado.totalCompra += costoUnitario;
                acumulado.totalVenta += ventaUnitario;
            }
        }

        List<UtilidadItem> resultado = new ArrayList<>();
        for (UtilidadAcumulado acumulado : acumulados.values()) {
            double utilidadPesos = acumulado.totalVenta - acumulado.totalCompra;
            double porcentaje = acumulado.totalCompra > 0 ? (utilidadPesos / acumulado.totalCompra) * 100 : 0;

            resultado.add(new UtilidadItem(
                    acumulado.claveProducto,
                    acumulado.nombreProducto,
                    acumulado.categoria,
                    acumulado.descripcionProducto,
                    acumulado.presentacion,
                    acumulado.factor,
                    String.valueOf(acumulado.cantidad),
                    formatoNumero(acumulado.totalCompra),
                    acumulado.proveedor,
                    formatoNumero(acumulado.totalVenta),
                    acumulado.cliente,
                    formatoPorcentaje(porcentaje),
                    formatoNumero(utilidadPesos),
                    acumulado.facturaCompra,
                    acumulado.facturaVenta,
                    acumulado.fechaSalida
            ));
        }

        return resultado;
    }

    private double obtenerCostoUnitario(ResultSet rs) throws SQLException {
        Double precioIVA = obtenerNumero(rs.getObject("precioIVAEntrada"));
        Double precioUnitario = obtenerNumero(rs.getObject("precioUnitarioEntrada"));
        Double precioTotal = obtenerNumero(rs.getObject("precioTotalEntrada"));
        Double cantidad = obtenerNumero(rs.getObject("cantidadEntrada"));
        if (precioIVA != null) {
            return precioIVA;
        }
        if (precioTotal != null && cantidad != null && cantidad != 0) {
            return precioTotal / cantidad;
        }
        if (precioUnitario != null) {
            return precioUnitario;
        }
        return 0;
    }

    private double obtenerVentaUnitario(ResultSet rs) throws SQLException {
        Double precioIVA = obtenerNumero(rs.getObject("precioIVASalida"));
        Double precioUnitario = obtenerNumero(rs.getObject("precioUnitarioSalida"));
        Double precioTotal = obtenerNumero(rs.getObject("precioTotalSalida"));
        Double cantidad = obtenerNumero(rs.getObject("cantidadSalida"));
        if (precioIVA != null) {
            return precioIVA;
        }
        if (precioTotal != null && cantidad != null && cantidad != 0) {
            return precioTotal / cantidad;
        }
        if (precioUnitario != null) {
            return precioUnitario;
        }
        return 0;
    }

    private void configurarFiltros() {
        comboFiltro.getItems().setAll(
                "Clave",
                "Producto",
                "Categoría",
                "Presentación",
                "Factor",
                "Proveedor",
                "Cliente",
                "Factura compra",
                "Factura venta",
                "Cantidad",
                "Porcentaje utilidad"
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
        if ("Porcentaje utilidad".equals(campo)) {
            comboValor.getItems().setAll(
                    "0 - 5%",
                    "5 - 10%",
                    "10 - 15%",
                    "15 - 20%",
                    "20 - 25%",
                    "25% +"
            );
        } else {
            List<String> valores = new ArrayList<>();
            for (UtilidadItem item : utilidadesOriginal) {
                String valor = obtenerValorCampo(item, campo);
                if (valor != null && !valor.isBlank() && !valores.contains(valor)) {
                    valores.add(valor);
                }
            }
            comboValor.getItems().setAll(valores);
        }
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
        HBox chip = new HBox(5); // Mismo spacing que en Inventario
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.getStyleClass().add("chip"); // ← Usa la clase CSS .chip

        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.getStyleClass().add("chip-text"); // ← Agrega clase para texto

        Button quitar = new Button("✕"); // Usa el mismo símbolo que Inventario (✕ en lugar de x)
        quitar.getStyleClass().add("chip-close"); // ← Usa la clase CSS .chip-close

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
        LocalDate fechaInicioSeleccionada = fechaInicio != null ? fechaInicio.getValue() : null;
        LocalDate fechaFinSeleccionada = fechaFin != null ? fechaFin.getValue() : null;

        List<UtilidadItem> filtrados = new ArrayList<>();
        for (UtilidadItem item : utilidadesOriginal) {
            boolean coincide = true;

            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);

                if ("Porcentaje utilidad".equals(filtro.campo)) {
                    double porcentajeItem = parseNumero(valor);
                    if (!estaEnRango(porcentajeItem, filtro.valor)) {
                        coincide = false;
                        break;
                    }
                } else {
                    if (valor == null || !valor.equals(filtro.valor)) {
                        coincide = false;
                        break;
                    }
                }
            }

            // Filtro de fechas
            if (coincide && (fechaInicioSeleccionada != null || fechaFinSeleccionada != null)) {
                LocalDate fechaItem = parseFecha(item.getFechaSalida());
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
        utilidades.setAll(filtrados);
        aplicarOrdenamiento();
        actualizarTotalesGenerales();
    }

    private boolean estaEnRango(double valor, String rango) {
        System.out.println("Valor a filtrar: " + valor + ", Rango original: '" + rango + "'");

        if (rango == null) return false;

        // Limpiar el rango: eliminar % y espacios
        String rangoLimpio = rango.replace("%", "").replace(" ", "").trim();
        System.out.println("Rango limpio: '" + rangoLimpio + "'");

        if (rangoLimpio.endsWith("+")) {
            double min = Double.parseDouble(rangoLimpio.replace("+", ""));
            boolean resultado = valor >= min;
            System.out.println("Comparación 25+: " + valor + " >= " + min + " = " + resultado);
            return resultado;
        } else {
            String[] partes = rangoLimpio.split("-");
            if (partes.length == 2) {
                try {
                    double min = Double.parseDouble(partes[0]);
                    double max = Double.parseDouble(partes[1]);
                    boolean resultado = valor >= min && valor < max;
                    System.out.println("Comparación rango: " + valor + " entre " + min + " y " + max + " = " + resultado);
                    return resultado;
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando rango: " + rangoLimpio);
                    return false;
                }
            }
        }
        return false;
    }

    private void actualizarTotalesGenerales() {
        if (totalCompraGeneral == null || totalVentaGeneral == null || totalUtilidadGeneral == null) {
            return;
        }

        double sumaTotalCompra = 0;
        double sumaTotalVenta = 0;
        double sumaTotalUtilidad = 0;

        for (UtilidadItem item : utilidades) {
            sumaTotalCompra += parseNumero(item.getTotalCompra());
            sumaTotalVenta += parseNumero(item.getTotalVenta());
            sumaTotalUtilidad += parseNumero(item.getUtilidadPesos());
        }

        totalCompraGeneral.setText(formatoNumero(sumaTotalCompra));
        totalVentaGeneral.setText(formatoNumero(sumaTotalVenta));
        totalUtilidadGeneral.setText(formatoNumero(sumaTotalUtilidad));
    }

    private String obtenerValorCampo(UtilidadItem item, String campo) {
        switch (campo) {
            case "Clave":
                return item.getClaveProducto();
            case "Producto":
                return item.getNombreProducto();
            case "Categoría":
                return item.getCategoria();
            case "Presentación":
                return item.getPresentacion();
            case "Factor":
                return item.getFactor();
            case "Proveedor":
                return item.getProveedor();
            case "Cliente":
                return item.getCliente();
            case "Factura compra":
                return item.getFacturaCompra();
            case "Factura venta":
                return item.getFacturaVenta();
            case "Cantidad":
                return item.getCantidad();
            case "Total compra":
                return item.getTotalCompra();
            case "Total venta":
                return item.getTotalVenta();
            case "Porcentaje utilidad":
                return item.getPorcentajeUtilidad();
            case "Utilidad":
                return item.getUtilidadPesos();
            default:
                return "";
        }
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<UtilidadItem, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<UtilidadItem, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                });
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = List.of(
                "producto",
                "categoria",
                "clave",
                "cantidad",
                "totalCompra",
                "totalVenta",
                "proveedor",
                "cliente",
                "utilidad",
                "porcentaje",
                "facturaVenta",
                "facturaCompra"
        );
        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<UtilidadItem> comparator;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase(Locale.ROOT);

        switch (criterioOrden) {
            case "clave":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
            case "categoria":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getCategoria()));
                break;
            case "cantidad":
                comparator = Comparator.comparing(item -> parseNumero(item.getCantidad()));
                break;
            case "totalCompra":
                comparator = Comparator.comparing(item -> parseNumero(item.getTotalCompra()));
                break;
            case "totalVenta":
                comparator = Comparator.comparing(item -> parseNumero(item.getTotalVenta()));
                break;
            case "proveedor":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getProveedor()));
                break;
            case "cliente":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getCliente()));
                break;
            case "utilidad":
                comparator = Comparator.comparing(item -> parseNumero(item.getUtilidadPesos()));
                break;
            case "porcentaje":
                comparator = Comparator.comparing(item -> parseNumero(item.getPorcentajeUtilidad()));
                break;
            case "facturaVenta":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getFacturaVenta()));
                break;
            case "facturaCompra":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getFacturaCompra()));
                break;
            case "producto":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getNombreProducto()));
                break;
        }

        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(utilidades, comparator);
    }

    private void configurarFiltroFechas() {
        if (fechaInicio == null || fechaFin == null) {
            return;
        }
        fechaInicio.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosYBusqueda());
        fechaFin.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosYBusqueda());
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Utilidades", "excel",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        double totalCompra = parseNumero(totalCompraGeneral.getText());
        double totalVenta = parseNumero(totalVentaGeneral.getText());
        double totalUtilidad = parseNumero(totalUtilidadGeneral.getText());

        exportador.TotalesReporte totales = new exportador.TotalesReporte(
                "Total compra", totalCompra,
                "Total venta", totalVenta,
                "Utilidad", totalUtilidad
        );

        exportador.exportarTabla(contenidoTabla, "Utilidades", "pdf",
                obtenerFiltrosAplicados(), totales);
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        double totalCompra = parseNumero(totalCompraGeneral.getText());
        double totalVenta = parseNumero(totalVentaGeneral.getText());
        double totalUtilidad = parseNumero(totalUtilidadGeneral.getText());

        exportador.TotalesReporte totales = new exportador.TotalesReporte(
                "Total compra", totalCompra,
                "Total venta", totalVenta,
                "Utilidad", totalUtilidad
        );

        exportador.previsualizarPDF(contenidoTabla, "Utilidades",
                obtenerFiltrosAplicados(), totales);
    }

    private List<String> obtenerFiltrosAplicados() {
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }
        if (fechaInicio != null && fechaInicio.getValue() != null) {
            filtrosAplicados.add("Fecha desde: " + fechaInicio.getValue());
        }
        if (fechaFin != null && fechaFin.getValue() != null) {
            filtrosAplicados.add("Fecha hasta: " + fechaFin.getValue());
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

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private Double obtenerNumero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }
        try {
            return Double.parseDouble(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer obtenerEntero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatoNumero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private String formatoPorcentaje(double valor) {
        return String.format(Locale.US, "%.2f%%", valor);
    }

    private double parseNumero(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        // Elimina %, espacios, comas y cualquier otro carácter no numérico excepto el punto decimal
        String limpio = valor.replaceAll("[^0-9.]", "").trim();
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            System.out.println("Error parseando: '" + valor + "' -> '" + limpio + "'");
            return 0;
        }
    }

    private LocalDate parseFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatos = List.of(DateTimeFormatter.ISO_LOCAL_DATE, FORMATO_FECHA_ALT);
        for (DateTimeFormatter formatter : formatos) {
            try {
                return LocalDate.parse(fechaTexto.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar con el siguiente formato
            }
        }
        return null;
    }

    private static class Filtro {
        private final String campo;
        private final String valor;

        private Filtro(String campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }
    }

    private static class UtilidadAcumulado {
        private final int salidaId;
        private final Integer entradaId;
        private final String claveProducto;
        private final String nombreProducto;
        private final String categoria;
        private final String descripcionProducto;
        private final String presentacion;
        private final String factor;
        private final String facturaCompra;
        private final String proveedor;
        private final String facturaVenta;
        private final String cliente;
        private final String fechaSalida;
        private int cantidad;
        private double totalCompra;
        private double totalVenta;

        private UtilidadAcumulado(int salidaId,
                                  Integer entradaId,
                                  String claveProducto,
                                  String nombreProducto,
                                  String categoria,
                                  String descripcionProducto,
                                  String presentacion,
                                  String factor,
                                  String facturaCompra,
                                  String proveedor,
                                  String facturaVenta,
                                  String cliente,
                                  String fechaSalida) {
            this.salidaId = salidaId;
            this.entradaId = entradaId;
            this.claveProducto = claveProducto;
            this.nombreProducto = nombreProducto;
            this.categoria = categoria;
            this.descripcionProducto = descripcionProducto;
            this.presentacion = presentacion;
            this.factor = factor;
            this.facturaCompra = facturaCompra;
            this.proveedor = proveedor;
            this.facturaVenta = facturaVenta;
            this.cliente = cliente;
            this.fechaSalida = fechaSalida;
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
