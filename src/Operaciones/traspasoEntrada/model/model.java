package Operaciones.traspasoEntrada.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import Operaciones.compra.model.UbicacionCompra;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class model {

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    public String obtenerComentarioEntrada(String claveEntrada) {
        String sql = "SELECT nota FROM entradas WHERE idEntrada = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Convertir claveEntrada a int (idEntrada es numérico)
            ps.setInt(1, Integer.parseInt(claveEntrada));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String comentario = rs.getString("nota");
                    // Si es null o vacío, retornar string vacío
                    return (comentario != null) ? comentario.trim() : "";
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: claveEntrada debe ser numérica - " + claveEntrada);
            return "";
        } catch (SQLException e) {
            System.err.println("Error SQL al obtener comentario: " + e.getMessage());
            e.printStackTrace();
            return "";
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
        return ""; // Si no encuentra el registro
    }

    // Clase interna para representar los detalles de una entrada
    public static class DetalleEntrada {
        private String claveProducto;
        private String producto;
        private String cantidad;
        private String precioUnitario;
        private String precioTotal;
        private String nombreSucursal; 

        // Constructor modificado para incluir la sucursal
        public DetalleEntrada(String claveProducto, String producto, String cantidad,
                              String precioUnitario, String precioTotal, String nombreSucursal) {
            this.claveProducto = claveProducto;
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.precioTotal = precioTotal;
            this.nombreSucursal = nombreSucursal != null ? nombreSucursal : "";
        }

        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getCantidad() { return cantidad; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioTotal() { return precioTotal; }
        public String getNombreSucursal() { return nombreSucursal; } // NUEVO GETTER
    }

    public String obtenerNombreSucursalPorEntrada(String claveEntrada) {
        String nombreSucursal = "";

        if (claveEntrada == null || claveEntrada.trim().isEmpty()) {
            return "No especificado";
        }

        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, String> columnasSucursales = obtenerColumnas(conn, "sucursales");

            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colSucursal = resolverColumna(columnasEntradas, "idRemitente", "idSucursal", "sucursal",
                    "sucursal_id", "id_sucursal", "remitente");

            String colSucursalId = resolverColumna(columnasSucursales, "id", "idSucursal", "sucursal_id", "id_sucursal");
            String colSucursalNombre = resolverColumna(columnasSucursales, "nombre", "nombreSucursal", "sucursal");

            if (colId == null || colSucursal == null || colSucursalId == null || colSucursalNombre == null) {
                return "No especificado";
            }

            String sql = "SELECT s.`" + colSucursalNombre + "` AS sucursal " +
                    "FROM entradas e " +
                    "LEFT JOIN sucursales s ON e.`" + colSucursal + "` = s.`" + colSucursalId + "` " +
                    "WHERE e.`" + colId + "` = ? " +
                    "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveEntrada);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombreSucursal = formato(rs.getObject("sucursal"));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener nombre de sucursal para entrada " + claveEntrada + ": " + e.getMessage());
            e.printStackTrace();
        }

        return nombreSucursal != null && !nombreSucursal.trim().isEmpty() ? nombreSucursal : "No especificado";
    }

    public String obtenerNombreProducto(String claveProducto) {
        if (claveProducto == null || claveProducto.trim().isEmpty()) {
            return claveProducto;
        }

        try (Connection conn = new Conexion().conectar()) {
            // Obtener columnas de la tabla productos
            Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");

            String colClaveProducto = resolverColumna(columnasProductos, "id", "claveProducto", "clave_producto", "producto_id");
            String colNombre = resolverColumna(columnasProductos, "nombre", "nombreProducto", "producto_nombre", "descripcion");

            if (colClaveProducto == null || colNombre == null) {
                return claveProducto;
            }

            // Consulta para obtener el nombre del producto
            String sql = "SELECT `" + colNombre + "` AS nombre " +
                    "FROM `productos` " +
                    "WHERE `" + colClaveProducto + "` = ? " +
                    "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreProducto = formato(rs.getObject("nombre"));

                        // Obtener la descripción usando GenericDAO
                        GenericDAO<producto> dao = new GenericDAO<>(producto.class);
                        String descripcion = dao.obtenerResumenProducto(claveProducto);

                        // Construir el nombre compuesto
                        StringBuilder sb = new StringBuilder();
                        sb.append(nombreProducto != null ? nombreProducto : claveProducto);

                        if (descripcion != null && !descripcion.isEmpty()) {
                            sb.append(" - ").append(descripcion);
                        }

                        return sb.toString();
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener información del producto '" + claveProducto + "': " + e.getMessage());
            e.printStackTrace();
        }

        return claveProducto;
    }

    public ObservableList<traspasoEntrada> obtenerPendientes() {
        ObservableList<traspasoEntrada> lista = FXCollections.observableArrayList();

        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, String> columnasSucursales = obtenerColumnas(conn, "sucursales");

            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colFecha = resolverColumna(columnasEntradas, "fechaEntrada", "fecha", "fecha_entrada", "created_at");
            String colHora = resolverColumna(columnasEntradas, "horaEntrada", "hora", "hora_entrada");
            String colTotal = resolverColumna(columnasEntradas, "precioTotalEntrada", "precioTotal", "precio_total", "total", "totalEntrada");
            String colTipo = resolverColumna(columnasEntradas, "tipoEntrada", "tipo", "tipo_entrada");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");
            String colSucursal = resolverColumna(columnasEntradas, "idRemitente", "idSucursal", "sucursal", "sucursal_id", "id_sucursal", "remitente");

            if (colTipo == null || colEstado == null) {
                return lista;
            }

            String colSucursalId = resolverColumna(columnasSucursales, "id", "idSucursal", "sucursal_id", "id_sucursal");
            String colSucursalNombre = resolverColumna(columnasSucursales, "nombre", "nombreSucursal", "sucursal");
            boolean puedeUnirSucursal = colSucursal != null && colSucursalId != null && colSucursalNombre != null;

            String selectClave = colId != null ? "e.`" + colId + "` AS clave" : "NULL AS clave";
            String selectFecha = colFecha != null ? "e.`" + colFecha + "` AS fecha" : "NULL AS fecha";
            String selectHora = colHora != null ? "e.`" + colHora + "` AS hora" : "NULL AS hora";
            String selectTotal = colTotal != null ? "e.`" + colTotal + "` AS total" : "NULL AS total";
            String selectSucursal = puedeUnirSucursal
                    ? "s.`" + colSucursalNombre + "` AS sucursal"
                    : "NULL AS sucursal";

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                    .append(selectClave).append(", ")
                    .append(selectFecha).append(", ")
                    .append(selectHora).append(", ")
                    .append(selectTotal).append(", ")
                    .append(selectSucursal)
                    .append(" FROM entradas e ");

            if (puedeUnirSucursal) {
                sql.append("LEFT JOIN sucursales s ON e.`")
                        .append(colSucursal)
                        .append("` = s.`")
                        .append(colSucursalId)
                        .append("` ");
            }

            sql.append("WHERE LOWER(e.`")
                    .append(colTipo)
                    .append("`) = ? AND LOWER(e.`")
                    .append(colEstado)
                    .append("`) = ? ")
                    .append("ORDER BY ")
                    .append(colFecha != null ? "e.`" + colFecha + "`" : "clave")
                    .append(" DESC");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setString(1, "traspaso");
                ps.setString(2, "revision");

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String clave = formato(rs.getObject("clave"));
                        String fecha = formato(rs.getObject("fecha"));
                        String hora = formato(rs.getObject("hora"));
                        String total = formato(rs.getObject("total"));
                        String sucursal = formato(rs.getObject("sucursal"));

                        lista.add(new traspasoEntrada(clave, fecha, hora, total, sucursal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    // Método para obtener detalles de una entrada específica
    public List<DetalleEntrada> obtenerDetallesEntrada(String claveEntrada) {
        List<DetalleEntrada> detalles = new ArrayList<>();

        if (claveEntrada == null || claveEntrada.trim().isEmpty()) {
            return detalles;
        }

        try (Connection conn = new Conexion().conectar()) {
            // Obtener columnas de detalle_Entrada
            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");
            Map<String, String> columnasMarcas = obtenerColumnas(conn, "marcas");

            // Buscar las columnas necesarias
            String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
            String colClaveProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "producto_id", "clave_producto");
            String colCantidad = resolverColumna(columnasDetalle, "cantidad", "qty", "quantity");
            String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitario", "unitario", "precio_unitario", "unit_price");
            String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total", "total", "precio_final", "final_price");

            // Verificar que todas las columnas necesarias existen
            if (colClaveEntrada == null || colClaveProducto == null || colCantidad == null ||
                    colPrecioUnitario == null || colPrecioTotal == null) {
                System.err.println("No se pudieron encontrar todas las columnas necesarias en detalle_Entrada");
                return detalles;
            }

            String colProductoId = resolverColumna(columnasProductos, "id", "claveProducto", "clave_producto", "producto_id");
            String colProductoNombre = resolverColumna(columnasProductos, "nombre", "nombreProducto", "producto_nombre", "descripcion");
            String colProductoMarca = resolverColumna(columnasProductos, "marca", "idMarca", "marca_id");
            String colProductoPresentacion = resolverColumna(columnasProductos, "presentacion", "unidadMedida", "unidad_medida", "presentation");

            String colMarcaId = resolverColumna(columnasMarcas, "id", "idMarca", "marca_id");
            String colMarcaNombre = resolverColumna(columnasMarcas, "nombre", "marca", "descripcion", "nombreMarca");

            boolean puedeUnirProductos = colProductoId != null && colProductoNombre != null;
            boolean puedeUnirMarcas = colMarcaId != null && colMarcaNombre != null && colProductoMarca != null;

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT d.`").append(colClaveProducto).append("` AS claveProducto, ")
                    .append("d.`").append(colCantidad).append("` AS cantidad, ")
                    .append("d.`").append(colPrecioUnitario).append("` AS precioUnitario, ")
                    .append("d.`").append(colPrecioTotal).append("` AS precioTotal");

            if (puedeUnirProductos) {
                sql.append(", p.`").append(colProductoNombre).append("` AS nombre");
                if (colProductoMarca != null) {
                    sql.append(", p.`").append(colProductoMarca).append("` AS marca");
                }
                if (colProductoPresentacion != null) {
                    sql.append(", p.`").append(colProductoPresentacion).append("` AS presentacion");
                }
            }

            if (puedeUnirMarcas) {
                sql.append(", m.`").append(colMarcaNombre).append("` AS nombreMarca");
            }

            sql.append(" FROM `detalle_Entrada` d ");

            if (puedeUnirProductos) {
                sql.append("LEFT JOIN `productos` p ON d.`")
                        .append(colClaveProducto)
                        .append("` = p.`")
                        .append(colProductoId)
                        .append("` ");
            }

            if (puedeUnirMarcas) {
                sql.append("LEFT JOIN `marcas` m ON p.`")
                        .append(colProductoMarca)
                        .append("` = m.`")
                        .append(colMarcaId)
                        .append("` ");
            }

            sql.append("WHERE d.`")
                    .append(colClaveEntrada)
                    .append("` = ? ")
                    .append("ORDER BY d.`")
                    .append(colClaveProducto)
                    .append("`");

            String sqlProducto = null;
            if (!puedeUnirProductos && colProductoId != null && colProductoNombre != null) {
                sqlProducto = "SELECT `" + colProductoNombre + "` AS nombre, " +
                        (colProductoMarca != null ? "`" + colProductoMarca + "` AS marca, " : "NULL AS marca, ") +
                        (colProductoPresentacion != null ? "`" + colProductoPresentacion + "` AS presentacion " : "NULL AS presentacion ") +
                        "FROM `productos` " +
                        "WHERE `" + colProductoId + "` = ? " +
                        "LIMIT 1";
            }

            String sqlMarca = null;
            if (!puedeUnirMarcas && colMarcaId != null && colMarcaNombre != null) {
                sqlMarca = "SELECT `" + colMarcaNombre + "` AS nombre FROM marcas WHERE `" + colMarcaId + "` = ? LIMIT 1";
            }

            Map<String, String> cacheProductos = new HashMap<>();
            Map<String, String> cacheMarcas = new HashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(sql.toString());
                 PreparedStatement psProducto = sqlProducto != null ? conn.prepareStatement(sqlProducto) : null;
                 PreparedStatement psMarca = sqlMarca != null ? conn.prepareStatement(sqlMarca) : null) {
                ps.setString(1, claveEntrada);

                try (ResultSet rs = ps.executeQuery()) {
                    // Obtener el nombre de la sucursal del remitente (una sola vez para todos los detalles)
                    String nombreSucursal = obtenerNombreSucursalPorEntrada(claveEntrada);

                    while (rs.next()) {
                        String claveProd = formato(rs.getObject("claveProducto"));
                        String cantidad = formato(rs.getObject("cantidad"));
                        String precioUnitario = formato(rs.getObject("precioUnitario"));
                        String precioTotal = formato(rs.getObject("precioTotal"));

                        String nombreProducto = cacheProductos.get(claveProd);
                        if (nombreProducto == null) {
                            nombreProducto = claveProd;
                            String nombre = puedeUnirProductos ? formato(rs.getObject("nombre")) : "";
                            String marca = puedeUnirProductos && colProductoMarca != null
                                    ? formato(rs.getObject("marca"))
                                    : "";
                            String presentacion = puedeUnirProductos && colProductoPresentacion != null
                                    ? formato(rs.getObject("presentacion"))
                                    : "";
                            String nombreMarca = puedeUnirMarcas ? formato(rs.getObject("nombreMarca")) : "";

                            if (nombreMarca != null && !nombreMarca.isBlank()) {
                                marca = nombreMarca;
                            }

                            if (marca != null && !marca.isBlank() && psMarca != null && (nombreMarca == null || nombreMarca.isBlank())) {
                                String marcaNombre = cacheMarcas.get(marca);
                                if (marcaNombre == null) {
                                    psMarca.setString(1, marca);
                                    try (ResultSet rsMarca = psMarca.executeQuery()) {
                                        if (rsMarca.next()) {
                                            marcaNombre = formato(rsMarca.getObject("nombre"));
                                        }
                                    }
                                    cacheMarcas.put(marca, marcaNombre != null ? marcaNombre : marca);
                                }
                                marca = cacheMarcas.get(marca);
                            }

                            if ((nombre == null || nombre.isBlank()) && psProducto != null && claveProd != null && !claveProd.isBlank()) {
                                psProducto.setString(1, claveProd);
                                try (ResultSet rsProd = psProducto.executeQuery()) {
                                    if (rsProd.next()) {
                                        nombre = formato(rsProd.getObject("nombre"));
                                        if (marca == null || marca.isBlank()) {
                                            marca = formato(rsProd.getObject("marca"));
                                        }
                                        if (presentacion == null || presentacion.isBlank()) {
                                            presentacion = formato(rsProd.getObject("presentacion"));
                                        }
                                    }
                                }
                            }

                            StringBuilder compuesto = new StringBuilder();
                            if (nombre != null && !nombre.isBlank()) {
                                compuesto.append(nombre);
                            }
                            if (marca != null && !marca.isBlank()) {
                                if (compuesto.length() > 0) {
                                    compuesto.append(" - ");
                                }
                                compuesto.append(marca);
                            }
                            if (presentacion != null && !presentacion.isBlank()) {
                                compuesto.append(" (").append(presentacion).append(")");
                            }
                            if (compuesto.length() > 0) {
                                nombreProducto = compuesto.toString();
                            }
                            cacheProductos.put(claveProd, nombreProducto);
                        }

                        // Crear el DetalleEntrada con los 6 parámetros (añadiendo nombreSucursal)
                        detalles.add(new DetalleEntrada(claveProd, nombreProducto, cantidad,
                                precioUnitario, precioTotal, nombreSucursal));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener detalles de la entrada: " + e.getMessage());
            e.printStackTrace();
        }

        return detalles;
    }

    public boolean actualizarEstadoEntradas(List<String> clavesEntrada, String nuevoEstadoEntrada, String nuevoEstadoArticulos) {
        if (clavesEntrada == null || clavesEntrada.isEmpty()) {
            return false;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");

            if (colId == null || colEstado == null) {
                conn.rollback();
                return false;
            }

            boolean actualizadoArticulos = actualizarEstadoArticulosPorEntradas(
                    conn,
                    clavesEntrada,
                    nuevoEstadoArticulos
            );
            if (!actualizadoArticulos) {
                conn.rollback();
                return false;
            }

            if (!"rechazado".equalsIgnoreCase(nuevoEstadoEntrada)) {
                boolean actualizadoDetalles = actualizarEstadoDetallesPorEntradas(
                        conn,
                        clavesEntrada,
                        "activo"
                );
                if (!actualizadoDetalles) {
                    conn.rollback();
                    return false;
                }
            }

            boolean actualizado = actualizarEstado(conn, clavesEntrada, colId, colEstado, nuevoEstadoEntrada);
            if (!actualizado) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResultadoOperacion actualizarUbicacionesYEstados(String claveEntrada,
                                                            Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
                                                            String nuevoEstadoEntrada,
                                                            String nuevoEstadoArticulos) {
        if (claveEntrada == null || claveEntrada.isBlank()) {
            return ResultadoOperacion.error("La clave de entrada es inválida.");
        }
        if (ubicacionesPorProducto == null || ubicacionesPorProducto.isEmpty()) {
            return ResultadoOperacion.error("No se proporcionaron ubicaciones para actualizar.");
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

            String colDetalleId = resolverColumna(columnasDetalle, "id", "idDetalleEntrada", "id_detalle_entrada",
                    "detalle_entrada_id", "detalleEntrada");
            String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
            String colDetalleProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "id_producto",
                    "producto_id", "clave_producto");
            String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                    "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
            String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");

            if (colDetalleId == null || colDetalleEntrada == null || colDetalleProducto == null ||
                    colArticuloDetalle == null || colArticuloId == null || colArticuloUbicacion == null) {
                conn.rollback();
                return ResultadoOperacion.error("No se pudieron resolver las columnas necesarias para actualizar ubicaciones.");
            }

            String sqlArticulos = "SELECT a.`" + colArticuloId + "` AS idArticulo " +
                    "FROM articulo a " +
                    "JOIN detalle_Entrada d ON a.`" + colArticuloDetalle + "` = d.`" + colDetalleId + "` " +
                    "WHERE d.`" + colDetalleEntrada + "` = ? AND d.`" + colDetalleProducto + "` = ? " +
                    "ORDER BY a.`" + colArticuloId + "`";

            String sqlActualizar = "UPDATE articulo SET `" + colArticuloUbicacion + "` = ? WHERE `" + colArticuloId + "` = ?";

            try (PreparedStatement psArticulos = conn.prepareStatement(sqlArticulos);
                 PreparedStatement psActualizar = conn.prepareStatement(sqlActualizar)) {

                for (Map.Entry<String, List<UbicacionCompra>> entry : ubicacionesPorProducto.entrySet()) {
                    String claveProducto = entry.getKey();
                    List<UbicacionCompra> ubicaciones = entry.getValue();

                    List<Integer> idsArticulos = new ArrayList<>();
                    psArticulos.setString(1, claveEntrada);
                    psArticulos.setString(2, claveProducto);
                    try (ResultSet rs = psArticulos.executeQuery()) {
                        while (rs.next()) {
                            idsArticulos.add(rs.getInt("idArticulo"));
                        }
                    }

                    int indiceArticulo = 0;
                    for (UbicacionCompra ubicacion : ubicaciones) {
                        if (ubicacion == null) {
                            continue;
                        }
                        Integer idUbicacion = resolverUbicacionId(conn, ubicacion.getUbicacion());
                        if (idUbicacion == null) {
                            conn.rollback();
                            return ResultadoOperacion.error("No se pudo resolver la ubicación: " + ubicacion.getUbicacion());
                        }
                        int cantidad = Math.max(0, ubicacion.getCantidad());
                        for (int i = 0; i < cantidad && indiceArticulo < idsArticulos.size(); i++) {
                            psActualizar.setInt(1, idUbicacion);
                            psActualizar.setInt(2, idsArticulos.get(indiceArticulo++));
                            psActualizar.addBatch();
                        }
                    }

                    if (indiceArticulo != idsArticulos.size()) {
                        conn.rollback();
                        return ResultadoOperacion.error("La distribución de ubicaciones no coincide con los artículos para el producto: " + claveProducto);
                    }
                }

                psActualizar.executeBatch();
            }

            boolean actualizadoArticulos = actualizarEstadoArticulosPorEntradas(
                    conn,
                    java.util.List.of(claveEntrada),
                    nuevoEstadoArticulos
            );
            if (!actualizadoArticulos) {
                conn.rollback();
                return ResultadoOperacion.error("No se pudo actualizar el estado de los artículos.");
            }

            if (!"rechazado".equalsIgnoreCase(nuevoEstadoEntrada)) {
                boolean actualizadoDetalles = actualizarEstadoDetallesPorEntradas(
                        conn,
                        java.util.List.of(claveEntrada),
                        "activo"
                );
                if (!actualizadoDetalles) {
                    conn.rollback();
                    return ResultadoOperacion.error("No se pudo actualizar el estado de los detalles de la entrada.");
                }
            }

            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");
            if (colId == null || colEstado == null) {
                conn.rollback();
                return ResultadoOperacion.error("No se pudo actualizar el estado de la entrada.");
            }
            actualizarEstado(conn, java.util.List.of(claveEntrada), colId, colEstado, nuevoEstadoEntrada);

            conn.commit();
            return ResultadoOperacion.exito("Ubicaciones asignadas y estados actualizados correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultadoOperacion.error("Ocurrió un error al actualizar ubicaciones: " + e.getMessage());
        }
    }

    private boolean actualizarEstado(Connection conn, List<String> clavesEntrada, String colId, String colEstado, String nuevoEstado) throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "UPDATE entradas SET `" + colEstado + "` = ? WHERE `" + colId + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            int index = 2;
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private boolean eliminarArticulosPorEntradas(Connection conn, List<String> clavesEntrada, String colEntrada) throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "id", "idDetalleEntrada", "id_detalle_entrada", "detalle_entrada_id", "detalleEntrada");
        String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");

        if (colDetalleId == null || colDetalleEntrada == null || colArticuloDetalle == null) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "DELETE a FROM articulo a " +
                "JOIN detalle_Entrada d ON a.`" + colArticuloDetalle + "` = d.`" + colDetalleId + "` " +
                "WHERE d.`" + colDetalleEntrada + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private boolean actualizarEstadoArticulosPorEntradas(Connection conn, List<String> clavesEntrada, String nuevoEstado)
            throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "id", "idDetalleEntrada", "id_detalle_entrada",
                "detalle_entrada_id", "detalleEntrada");
        String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        if (colDetalleId == null || colDetalleEntrada == null || colArticuloDetalle == null || colArticuloEstado == null) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "UPDATE articulo a " +
                "JOIN detalle_Entrada d ON a.`" + colArticuloDetalle + "` = d.`" + colDetalleId + "` " +
                "SET a.`" + colArticuloEstado + "` = ? " +
                "WHERE d.`" + colDetalleEntrada + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setString(index++, nuevoEstado);
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private boolean actualizarEstadoDetallesPorEntradas(Connection conn, List<String> clavesEntrada, String nuevoEstado)
            throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");

        if (colDetalleEntrada == null || colDetalleEstado == null) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "UPDATE detalle_Entrada SET `" + colDetalleEstado + "` = ? WHERE `" + colDetalleEntrada + "` IN ("
                + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setString(index++, nuevoEstado);
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private List<String> filtrarClaves(List<String> clavesEntrada) {
        List<String> claves = new ArrayList<>();
        for (String clave : clavesEntrada) {
            if (clave != null && !clave.isBlank()) {
                claves.add(clave.trim());
            }
        }
        return claves;
    }

    private String resolverNombreMarca(Connection conn, String marcaRaw) throws SQLException {
        if (marcaRaw == null || marcaRaw.isBlank()) {
            return marcaRaw;
        }

        Map<String, String> columnasMarcas = obtenerColumnas(conn, "marcas");
        String colId = resolverColumna(columnasMarcas, "id", "idMarca", "marca_id");
        String colNombre = resolverColumna(columnasMarcas, "nombre", "marca", "descripcion", "nombreMarca");

        if (colId == null || colNombre == null) {
            return marcaRaw;
        }

        String sql = "SELECT `" + colNombre + "` AS nombre FROM marcas WHERE `" + colId + "` = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, marcaRaw);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombre = formato(rs.getObject("nombre"));
                    if (!nombre.isBlank()) {
                        return nombre;
                    }
                }
            }
        }

        return marcaRaw;
    }

    private String formato(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new HashMap<>();
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                columnas.put(limpio.toLowerCase(), limpio);
            }
        }
        return columnas;
    }

    private Integer resolverUbicacionId(Connection conn, String ubicacion) throws SQLException {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        String texto = ubicacion.trim();
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException ignored) {
        }

        String sql = "SELECT id FROM ubicaciones WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertar = "INSERT INTO ubicaciones (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, texto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    public static class ResultadoOperacion {
        private final boolean exito;
        private final String mensaje;

        private ResultadoOperacion(boolean exito, String mensaje) {
            this.exito = exito;
            this.mensaje = mensaje;
        }

        public static ResultadoOperacion exito(String mensaje) {
            return new ResultadoOperacion(true, mensaje);
        }

        public static ResultadoOperacion error(String mensaje) {
            return new ResultadoOperacion(false, mensaje);
        }

        public boolean isExito() {
            return exito;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) {
                continue;
            }
            String key = candidato.trim().toLowerCase();
            if (columnas.containsKey(key)) {
                return columnas.get(key);
            }
        }
        return null;
    }


}
