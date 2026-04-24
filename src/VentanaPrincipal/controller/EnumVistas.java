package VentanaPrincipal.controller;

public enum EnumVistas {
    // Módulos principales
    OPERACIONES("/Operaciones/view/main_view.fxml", "Operaciones", "#f0f0f0"),
    REPORTES("/Reportes/view/main_view.fxml", "Reportes", "#f0f0f0"),
    CONSULTAS("/Consultas/view/main_view.fxml", "Consultas", "#f0f0f0"),
    CONFIGURACION("/Configuracion/view/main_view.fxml", "Ajustes", "#f0f0f0"),

    // Subvistas de Operaciones
    AJUSTE_INVENTARIO("/Operaciones/ajusteInventario/view/main_view.fxml", "Ajuste de inventario", "#ffffff"),
    COMPRA("/Operaciones/compra/view/main_view.fxml", "Compra", "#ffffff"),
    PEDIDOS("/Operaciones/pedidos/view/main_view.fxml", "Pedidos", "#ffffff"),
    REGISTRAR_USUARIO("/Operaciones/registrarUsuario/view/main_view.fxml", "Registrar usuario", "#ffffff"),
    TRASPASO_ENTRADA("/Operaciones/traspasoEntrada/view/main_view.fxml", "Traspaso de entrada", "#ffffff"),
    TRASPASO_SALIDA("/Operaciones/traspasoSalida/view/main_view.fxml", "Traspaso de salida", "#ffffff"),
    VENTA("/Operaciones/venta/view/main_view.fxml", "Venta", "#ffffff"),

    // Subvistas de Reportes
    HISTORIAL("/Reportes/historial/view/main_view.fxml", "Historial por factura", "#ffffff"),
    HISTORIAL_ARTICULO("/Reportes/historialArticulo/view/main_view.fxml", "Historial por artículo", "#ffffff"),
    INVENTARIO("/Reportes/inventario/view/main_view.fxml", "Inventario", "#ffffff"),
    UTILIDADES("/Reportes/utilidades/view/main_view.fxml", "Utilidades", "#ffffff"),

    // Subvistas de Consultas
    CLASIFICACION("/Consultas/clasificacion/view/main_view.fxml", "Clasificación", "#ffffff"),
    CLAVES("/Consultas/claves/view/main_view.fxml", "Claves", "#ffffff"),
    CLIENTES("/Consultas/clientes/view/main_view.fxml", "Clientes", "#ffffff"),
    PRODUCTO("/Consultas/producto/view/main_view.fxml", "Productos", "#ffffff"),
    PROVEEDORES("/Consultas/proveedores/view/main_view.fxml", "Proveedores", "#ffffff"),
    SUCURSALES("/Consultas/sucursales/view/main_view.fxml", "Sucursales", "#ffffff"),

    // Vistas de ejemplo (puedes eliminarlas si ya no las usas)
    VISTA1("/Vista1/view/main_view.fxml", "VISTA 1 - Principal", "#4CAF50"),
    VISTA2("/Vista2/view/main_view.fxml", "VISTA 2 - Secundaria", "#2196F3");

    private final String rutaFxml;
    private final String titulo;
    private final String color;

    EnumVistas(String rutaFxml, String titulo, String color) {
        this.rutaFxml = rutaFxml;
        this.titulo = titulo;
        this.color = color;
    }

    public String getRutaFxml() { return rutaFxml; }
    public String getTitulo() { return titulo; }
    public String getColor() { return color; }
}