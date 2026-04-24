package Compartido.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import conexion.Conexion;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class modelProductoCbox {

    /**
     * Obtiene todos los productos de la base de datos
     */
    public List<Map<String, String>> obtenerTodosProductos() throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");
            String colUrlImagen = resolverColumna(columnasProductos, "urlImagen");
            String urlImagenSelect = colUrlImagen != null
                    ? "p." + colUrlImagen + " AS urlImagen"
                    : "'' AS urlImagen";

            String sql = """
                SELECT 
                    p.id,
                    p.nombre,
                    COALESCE(p.categoria, '') AS categoria,
                    p.descripcion,
                    %s,
                    p.unidadMedida,
                    m.nombre AS marca,
                    e.nombre AS etiqueta
                FROM productos p
                LEFT JOIN marcas m ON m.id = p.marca
                LEFT JOIN etiquetas e ON e.id = p.etiqueta
                WHERE p.estado = 'activo'
                ORDER BY p.nombre
            """.formatted(urlImagenSelect);

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return procesarResultSetProductos(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo productos: " + e.getMessage());
            throw e;
        }
    }

    public List<Map<String, String>> obtenerProductosDisponibles() throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");

            String colUrlImagen = resolverColumna(columnasProductos, "urlImagen");
            String urlImagenSelect = colUrlImagen != null
                    ? "p." + colUrlImagen + " AS urlImagen"
                    : "'' AS urlImagen";
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");
            String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado");
            String colDetalleArticuloIdArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo",
                    "id_articulo", "articulo_id");
            String colDetalleArticuloDetalleSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                    "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");

            if (colArticuloEstado == null || colArticuloDetalleEntrada == null || colDetalleEntradaId == null
                    || colDetalleEntradaProducto == null || colArticuloDetalleSalida == null
                    || colDetalleArticuloEstado == null || colDetalleArticuloIdArticulo == null
                    || colDetalleArticuloDetalleSalida == null) {
                return obtenerTodosProductos();
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT p.id, p.nombre, COALESCE(p.categoria, '') AS categoria, p.descripcion, ")
                    .append(urlImagenSelect).append(", p.unidadMedida, ")
                    .append("m.nombre AS marca, e.nombre AS etiqueta ")
                    .append("FROM productos p ")
                    .append("LEFT JOIN marcas m ON m.id = p.marca ")
                    .append("LEFT JOIN etiquetas e ON e.id = p.etiqueta ")
                    .append("WHERE (p.estado = 'activo' OR p.estado IS NULL OR p.estado = '') ")
                    .append("AND ( ")
                    .append("EXISTS ( ")
                    .append("SELECT 1 FROM detalle_Entrada de ")
                    .append("JOIN articulo a ON a.`").append(colArticuloDetalleEntrada)
                    .append("` = de.`").append(colDetalleEntradaId).append("` ")
                    .append("WHERE de.`").append(colDetalleEntradaProducto).append("` = p.id ")
                    .append("AND LOWER(a.`").append(colArticuloEstado).append("`) = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida)
                    .append("` IS NULL OR a.`").append(colArticuloDetalleSalida).append("` = 0) ")
                    .append(") ")
                    .append("OR EXISTS ( ")
                    .append("SELECT 1 FROM detalle_Entrada de2 ")
                    .append("JOIN articulo a2 ON a2.`").append(colArticuloDetalleEntrada)
                    .append("` = de2.`").append(colDetalleEntradaId).append("` ")
                    .append("JOIN detalleArticulo da ON da.`").append(colDetalleArticuloIdArticulo)
                    .append("` = a2.idArticulo ")
                    .append("WHERE de2.`").append(colDetalleEntradaProducto).append("` = p.id ")
                    .append("AND LOWER(da.`").append(colDetalleArticuloEstado).append("`) = ? ")
                    .append("AND (da.`").append(colDetalleArticuloDetalleSalida)
                    .append("` IS NULL OR da.`").append(colDetalleArticuloDetalleSalida).append("` = 0) ")
                    .append(") ")
                    .append(") ")
                    .append("ORDER BY p.nombre");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setString(1, "disponible");
                ps.setString(2, "disponible");
                try (ResultSet rs = ps.executeQuery()) {
                    return procesarResultSetProductos(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo productos disponibles: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtiene todos los productos relacionados con un proveedor específico
     */
    public List<Map<String, String>> obtenerProductosPorProveedor(String idProveedor) throws SQLException {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(buildSqlProductosPorProveedor(conn))) {
            ps.setString(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                return procesarResultSetProductos(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo productos por proveedor: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Procesa el ResultSet de productos
     */
    private List<Map<String, String>> procesarResultSetProductos(ResultSet rs) throws SQLException {
        List<Map<String, String>> productos = new ArrayList<>();

        while (rs.next()) {
            productos.add(crearMapaProducto(rs));
        }

        return productos;
    }

    /**
     * Crea un mapa con los datos del producto
     */
    private Map<String, String> crearMapaProducto(ResultSet rs) throws SQLException {
        Map<String, String> producto = new HashMap<>();

        // Datos básicos
        String id = rs.getString("id");
        producto.put("id", id);
        producto.put("nombre", rs.getString("nombre"));
        producto.put("categoria", rs.getString("categoria"));

        // Datos individuales
        String marca = rs.getString("marca");
        String etiqueta = rs.getString("etiqueta");
        String unidadMedida = rs.getString("unidadMedida");
        String descripcionOriginal = rs.getString("descripcion");

        producto.put("marca", marca != null ? marca : "");
        producto.put("etiqueta", etiqueta != null ? etiqueta : "");
        producto.put("unidadMedida", unidadMedida != null ? unidadMedida : "");
        producto.put("descripcion", obtenerDescripcionProducto(id));
        producto.put("urlImagen", rs.getString("urlImagen"));

        return producto;
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


    /**
     * Obtiene las claves alternas para un producto específico
     */
    public List<Map<String, String>> obtenerClavesAlternasPorProducto(String idProducto) throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            WHERE ca.estado = 'activo'
              AND pr.estado = 'activo'
              AND ca.idProducto = ?
            ORDER BY pv.nombre
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                return procesarResultSetClaves(rs);
            }
        }
    }

    /**
     * Obtiene las claves alternas para un producto y proveedor específico
     */
    public List<Map<String, String>> obtenerClavesAlternasPorProducto(String idProducto, String idProveedor) throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            WHERE ca.estado = 'activo'
              AND pr.estado = 'activo'
              AND ca.idProducto = ? AND ca.idProveedor = ?
            ORDER BY pv.nombre
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                return procesarResultSetClaves(rs);
            }
        }
    }

    public List<Map<String, String>> obtenerClavesAlternasDisponibles() throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");

            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");
            String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado");
            String colDetalleArticuloIdArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo",
                    "id_articulo", "articulo_id");
            String colDetalleArticuloDetalleSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                    "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");

            if (colArticuloEstado == null || colArticuloDetalleEntrada == null || colDetalleEntradaId == null
                    || colDetalleEntradaProducto == null || colArticuloDetalleSalida == null
                    || colDetalleArticuloEstado == null || colDetalleArticuloIdArticulo == null
                    || colDetalleArticuloDetalleSalida == null) {
                return obtenerTodasClavesAlternas();
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT ca.idAlterno, ca.idProducto, ca.idProveedor, ")
                    .append("pv.nombre AS nombreProveedor, pr.nombre AS nombreProducto ")
                    .append("FROM claves ca ")
                    .append("LEFT JOIN proveedores pv ON pv.id = ca.idProveedor ")
                    .append("LEFT JOIN productos pr ON pr.id = ca.idProducto ")
                    .append("WHERE ca.estado = 'activo' ")
                    .append("AND (pr.estado = 'activo' OR pr.estado IS NULL OR pr.estado = '') ")
                    .append("AND ( ")
                    .append("EXISTS ( ")
                    .append("SELECT 1 FROM detalle_Entrada de ")
                    .append("JOIN articulo a ON a.`").append(colArticuloDetalleEntrada)
                    .append("` = de.`").append(colDetalleEntradaId).append("` ")
                    .append("WHERE de.`").append(colDetalleEntradaProducto).append("` = pr.id ")
                    .append("AND LOWER(a.`").append(colArticuloEstado).append("`) = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida)
                    .append("` IS NULL OR a.`").append(colArticuloDetalleSalida).append("` = 0) ")
                    .append(") ")
                    .append("OR EXISTS ( ")
                    .append("SELECT 1 FROM detalle_Entrada de2 ")
                    .append("JOIN articulo a2 ON a2.`").append(colArticuloDetalleEntrada)
                    .append("` = de2.`").append(colDetalleEntradaId).append("` ")
                    .append("JOIN detalleArticulo da ON da.`").append(colDetalleArticuloIdArticulo)
                    .append("` = a2.idArticulo ")
                    .append("WHERE de2.`").append(colDetalleEntradaProducto).append("` = pr.id ")
                    .append("AND LOWER(da.`").append(colDetalleArticuloEstado).append("`) = ? ")
                    .append("AND (da.`").append(colDetalleArticuloDetalleSalida)
                    .append("` IS NULL OR da.`").append(colDetalleArticuloDetalleSalida).append("` = 0) ")
                    .append(") ")
                    .append(") ")
                    .append("ORDER BY pv.nombre");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setString(1, "disponible");
                ps.setString(2, "disponible");
                try (ResultSet rs = ps.executeQuery()) {
                    return procesarResultSetClaves(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo claves alternas disponibles: " + e.getMessage());
            throw e;
        }
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                columnas.put(limpio.toLowerCase(), limpio);
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    if (nombre == null) {
                        continue;
                    }
                    String limpio = nombre.trim();
                    columnas.put(limpio.toLowerCase(), limpio);
                }
            }
        }

        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) {
                continue;
            }
            String match = columnas.get(candidato.toLowerCase());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Busca un producto por su clave alterna
     */
    public Optional<Map<String, String>> buscarPorClaveAlterna(String idAlterno) throws SQLException {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(buildSqlClaveAlterna(conn, false))) {

            ps.setString(1, idAlterno);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(crearMapaProductoConClave(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Busca un producto por su clave alterna y proveedor
     */
    public Optional<Map<String, String>> buscarPorClaveAlterna(String idAlterno, String idProveedor) throws SQLException {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(buildSqlClaveAlterna(conn, true))) {

            ps.setString(1, idAlterno);
            ps.setString(2, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(crearMapaProductoConClave(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Crea mapa de producto con información de clave alterna
     */
    private Map<String, String> crearMapaProductoConClave(ResultSet rs) throws SQLException {
        Map<String, String> producto = crearMapaProducto(rs);

        // Agregar datos específicos de clave alterna
        producto.put("idAlterno", rs.getString("idAlterno"));
        producto.put("idProveedor", rs.getString("idProveedor"));
        producto.put("nombreProveedor", rs.getString("nombreProveedor"));

        return producto;
    }

    private String buildSqlProductosPorProveedor(Connection conn) throws SQLException {
        String urlImagenSelect = obtenerSelectUrlImagen(conn, "p");
        return """
            SELECT DISTINCT
                p.id,
                p.nombre,
                COALESCE(p.categoria, '') AS categoria,
                p.descripcion,
                %s,
                p.unidadMedida,
                m.nombre AS marca,
                e.nombre AS etiqueta
            FROM productos p
            JOIN claves c ON c.idProducto = p.id
            LEFT JOIN marcas m ON m.id = p.marca
            LEFT JOIN etiquetas e ON e.id = p.etiqueta
            WHERE c.idProveedor = ?
              AND c.estado = 'activo'
              AND p.estado = 'activo'
            ORDER BY p.nombre
        """.formatted(urlImagenSelect);
    }

    private String buildSqlClaveAlterna(Connection conn, boolean filtrarProveedor) throws SQLException {
        String urlImagenSelect = obtenerSelectUrlImagen(conn, "p");
        String filtroProveedor = filtrarProveedor ? " AND ca.idProveedor = ?" : "";
        return """
            SELECT 
                p.id,
                p.nombre,
                COALESCE(p.categoria, '') AS categoria,
                p.descripcion,
                %s,
                p.unidadMedida,
                m.nombre AS marca,
                e.nombre AS etiqueta,
                ca.idAlterno,
                ca.idProveedor,
                pv.nombre AS nombreProveedor
            FROM claves ca
            JOIN productos p ON p.id = ca.idProducto
            LEFT JOIN marcas m ON m.id = p.marca
            LEFT JOIN etiquetas e ON e.id = p.etiqueta
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            WHERE ca.estado = 'activo'
              AND p.estado = 'activo'
              AND ca.idAlterno = ?%s
        """.formatted(urlImagenSelect, filtroProveedor);
    }

    private String obtenerSelectUrlImagen(Connection conn, String aliasTabla) throws SQLException {
        Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");
        String colUrlImagen = resolverColumna(columnasProductos, "urlImagen");
        if (colUrlImagen == null) {
            return "'' AS urlImagen";
        }
        return aliasTabla + "." + colUrlImagen + " AS urlImagen";
    }

    /**
     * Obtiene todas las claves alternas disponibles
     */
    public List<Map<String, String>> obtenerTodasClavesAlternas() throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            WHERE ca.estado = 'activo'
              AND pr.estado = 'activo'
            ORDER BY ca.idAlterno
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return procesarResultSetClaves(rs);
        }
    }

    /**
     * Obtiene todas las claves alternas de un proveedor
     */
    public List<Map<String, String>> obtenerClavesAlternasPorProveedor(String idProveedor) throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            WHERE ca.estado = 'activo'
              AND pr.estado = 'activo'
              AND ca.idProveedor = ?
            ORDER BY ca.idAlterno
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                return procesarResultSetClaves(rs);
            }
        }
    }

    /**
     * Procesa ResultSet de claves alternas
     */
    private List<Map<String, String>> procesarResultSetClaves(ResultSet rs) throws SQLException {
        List<Map<String, String>> claves = new ArrayList<>();

        while (rs.next()) {
            Map<String, String> clave = new HashMap<>();
            clave.put("idAlterno", rs.getString("idAlterno"));
            clave.put("idProducto", rs.getString("idProducto"));
            clave.put("idProveedor", rs.getString("idProveedor"));
            clave.put("nombreProveedor", rs.getString("nombreProveedor"));
            clave.put("nombreProducto", rs.getString("nombreProducto"));
            claves.add(clave);
        }

        return claves;
    }

    /**
     * Métodos auxiliares optimizados con Streams
     */
    public Optional<Map<String, String>> buscarPorId(String id, List<Map<String, String>> productos) {
        return productos != null ?
                productos.stream()
                        .filter(p -> id.equals(p.get("id")))
                        .findFirst() :
                Optional.empty();
    }

    public List<Map<String, String>> buscarPorNombre(String nombre, List<Map<String, String>> productos) {
        return productos != null ?
                productos.stream()
                        .filter(p -> nombre.equals(p.get("nombre")))
                        .collect(Collectors.toList()) :
                Collections.emptyList();
    }

    public boolean validarIdNombre(String id, String nombre, List<Map<String, String>> productos) {
        return productos != null &&
                productos.stream()
                        .anyMatch(p -> id.equals(p.get("id")) && nombre.equals(p.get("nombre")));
    }

    public String obtenerDescripcionPorId(String id, List<Map<String, String>> productos) {
        return buscarPorId(id, productos)
                .map(p -> p.get("descripcion"))
                .orElse("");
    }

    public String obtenerUnidadMedidaPorId(String id, List<Map<String, String>> productos) {
        return buscarPorId(id, productos)
                .map(p -> p.get("unidadMedida"))
                .orElse("");
    }

    public String obtenerCategoriaPorId(String id, List<Map<String, String>> productos) {
        return buscarPorId(id, productos)
                .map(p -> p.get("categoria"))
                .orElse("");
    }
}
