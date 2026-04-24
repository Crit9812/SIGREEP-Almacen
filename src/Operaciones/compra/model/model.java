package Operaciones.compra.model;

import Compartido.sesion.SesionUsuario;
import conexion.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class model {

    private static final String SQL_NOMBRES_PROVEEDORES =
            "SELECT Nombre AS nombre FROM proveedores WHERE status = 'activo' ORDER BY Nombre";
    private static final String SQL_NOMBRES_UBICACIONES =
            "SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre";
    private static final String SQL_ID_PROVEEDOR_POR_NOMBRE =
            "SELECT id FROM proveedores WHERE Nombre = ? AND status = 'activo' LIMIT 1";
    private static final String SQL_PRECIO_ENTRADA_ULTIMO_PRODUCTO =
            "SELECT d.precioUnitario AS precioEntrada "
                    + "FROM detalle_Entrada d "
                    + "JOIN entradas e ON e.idEntrada = d.claveEntrada "
                    + "WHERE d.claveProducto = ? "
                    + "ORDER BY e.fechaEntrada DESC, e.horaEntrada DESC, d.idDetalleEntrada DESC "
                    + "LIMIT 1";
    private static final String SQL_ENTRADA_RECIENTE =
            "SELECT idEntrada FROM entradas "
                    + "WHERE idRemitente = ? AND noFactura = ? "
                    + "ORDER BY fechaEntrada DESC, horaEntrada DESC "
                    + "LIMIT 1";
    private static final String SQL_UBICACION_POR_NOMBRE =
            "SELECT id FROM ubicaciones WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
    private static final String SQL_INSERTAR_UBICACION =
            "INSERT INTO ubicaciones (nombre, estado) VALUES (?, ?)";

    public List<String> obtenerNombresProveedores() {
        List<String> lista = new ArrayList<>();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_NOMBRES_PROVEEDORES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_NOMBRES_UBICACIONES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    public String obtenerIdProveedorPorNombre(String nombreProveedor) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_ID_PROVEEDOR_POR_NOMBRE)) {

            ps.setString(1, nombreProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.Optional<BigDecimal> obtenerPrecioEntradaUltimoProducto(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return java.util.Optional.empty();
        }

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_PRECIO_ENTRADA_ULTIMO_PRODUCTO)) {

            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal precioEntrada = rs.getBigDecimal("precioEntrada");
                    return java.util.Optional.of(precioEntrada != null ? precioEntrada : BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return java.util.Optional.empty();
    }

    public boolean registrarCompra(String idProveedor, String factura, String comentario, List<compra> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            try {
                long idEntrada = insertarEntrada(conn, idProveedor, factura, comentario, items);
                insertarDetalleYArticulos(conn, idEntrada, items);

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private long insertarEntrada(Connection conn, String idProveedor, String factura, String comentario, List<compra> items)
            throws SQLException {
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (compra item : items) {
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            BigDecimal precioUnitario = parseDecimal(item.getPrecioEntrada());
            BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
            BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());

            totalNeto = totalNeto.add(precioUnitario.multiply(cantidad));
            if (precioTotal.compareTo(BigDecimal.ZERO) > 0) {
                totalGeneral = totalGeneral.add(precioTotal);
            } else {
                totalGeneral = totalGeneral.add(precioBruto.multiply(cantidad));
            }
        }

        String sql = "INSERT INTO entradas (noFactura, fechaEntrada, horaEntrada, tipoEntrada, claveUsuarioEntrada, "
                + "idRemitente, precioNetoEntrada, precioTotalEntrada, nota, Estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            ps.setString(index++, factura);
            ps.setDate(index++, Date.valueOf(LocalDate.now()));
            ps.setTime(index++, Time.valueOf(LocalTime.now()));
            ps.setString(index++, "Compra");

            Integer idUsuarioEntrada = SesionUsuario.getIdUsuario();
            if (idUsuarioEntrada != null) {
                ps.setInt(index++, idUsuarioEntrada);
            } else {
                ps.setNull(index++, Types.INTEGER);
            }

            ps.setString(index++, idProveedor);
            ps.setBigDecimal(index++, totalNeto.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(index++, totalGeneral.setScale(2, RoundingMode.HALF_UP));
            ps.setString(index++, comentario);
            ps.setString(index, "disponible");

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("No se pudo obtener el ID de la entrada registrada");
    }

    private void insertarDetalleYArticulos(Connection conn, long idEntrada, List<compra> items) throws SQLException {
        String sqlDetalle = "INSERT INTO detalle_Entrada (claveEntrada, claveProducto, cantidad, precioUnitario, precioIVA, "
                + "precioBrutoTotal, precioTotal, Nota, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlArticulo = "INSERT INTO articulo (idDetalleEntrada, presentacion, factor, lote, caducidad, ubicacion, "
                + "segmentado, Estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Map<String, Integer> ubicacionCache = new LinkedHashMap<>();

        try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psArticulo = conn.prepareStatement(sqlArticulo)) {

            for (compra item : items) {
                long idDetalleEntrada = insertarDetalle(psDetalle, item, idEntrada);
                Map<String, Integer> cantidadesPorUbicacion = consolidarUbicaciones(item.getUbicaciones());

                for (Map.Entry<String, Integer> entry : cantidadesPorUbicacion.entrySet()) {
                    Integer ubicacionId = resolverUbicacionId(conn, entry.getKey(), ubicacionCache);
                    int cantidadUbicacion = entry.getValue();

                    for (int i = 0; i < cantidadUbicacion; i++) {
                        int index = 1;
                        psArticulo.setLong(index++, idDetalleEntrada);
                        psArticulo.setString(index++, item.getPresentacion());
                        Integer factor = parseInteger(item.getFactor());
                        if (factor != null) {
                            psArticulo.setInt(index++, factor);
                        } else {
                            psArticulo.setNull(index++, Types.INTEGER);
                        }
                        psArticulo.setString(index++, item.getLote());
                        Date caducidad = parseDate(item.getCaducidad());
                        if (caducidad != null) {
                            psArticulo.setDate(index++, caducidad);
                        } else {
                            psArticulo.setNull(index++, Types.DATE);
                        }
                        if (ubicacionId != null) {
                            psArticulo.setInt(index++, ubicacionId);
                        } else {
                            psArticulo.setNull(index++, Types.INTEGER);
                        }
                        psArticulo.setInt(index++, esSegmentado(item.getPresentacion()));
                        psArticulo.setString(index, "disponible");
                        psArticulo.addBatch();
                    }
                }
            }

            psArticulo.executeBatch();
        }
    }

    private long insertarDetalle(PreparedStatement psDetalle, compra item, long idEntrada) throws SQLException {
        int index = 1;
        psDetalle.setLong(index++, idEntrada);
        psDetalle.setString(index++, item.getClaveProducto());
        psDetalle.setInt(index++, item.getCantidad());
        psDetalle.setBigDecimal(index++, parseDecimal(item.getPrecioEntrada()));
        psDetalle.setBigDecimal(index++, parseDecimal(item.getPrecioIva()));
        psDetalle.setBigDecimal(index++, parseDecimal(item.getPrecioBruto()));
        psDetalle.setBigDecimal(index++, parseDecimal(item.getPrecioTotal()));
        psDetalle.setString(index++, item.getNota());
        psDetalle.setString(index, "activo");

        psDetalle.executeUpdate();

        try (ResultSet keys = psDetalle.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }

        try (PreparedStatement ps = psDetalle.getConnection().prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("No se pudo obtener el ID del detalle de entrada registrado");
    }

    private Map<String, Integer> consolidarUbicaciones(List<UbicacionCompra> ubicaciones) {
        Map<String, Integer> cantidadesPorUbicacion = new LinkedHashMap<>();
        if (ubicaciones == null) {
            return cantidadesPorUbicacion;
        }

        for (UbicacionCompra ubicacion : ubicaciones) {
            if (ubicacion == null || ubicacion.getUbicacion() == null) {
                continue;
            }
            int cantidadUbicacion = Math.max(0, ubicacion.getCantidad());
            if (cantidadUbicacion == 0) {
                continue;
            }
            cantidadesPorUbicacion.merge(ubicacion.getUbicacion().trim(), cantidadUbicacion, Integer::sum);
        }

        return cantidadesPorUbicacion;
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Date parseDate(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(fecha);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resolverUbicacionId(Connection conn, String ubicacion, Map<String, Integer> cache) throws SQLException {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        String texto = ubicacion.trim();
        Integer cached = cache.get(texto);
        if (cached != null) {
            return cached;
        }

        try {
            Integer id = Integer.valueOf(texto);
            cache.put(texto, id);
            return id;
        } catch (NumberFormatException ignored) {
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_UBICACION_POR_NOMBRE)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    cache.put(texto, id);
                    return id;
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR_UBICACION, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, texto);
            ps.setString(2, "activo");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    cache.put(texto, id);
                    return id;
                }
            }
        }

        return null;
    }

    public String obtenerClaveCompraReciente(String idProveedor, String factura) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(SQL_ENTRADA_RECIENTE)) {

            ps.setString(1, idProveedor);
            ps.setString(2, factura);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("idEntrada");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int esSegmentado(String presentacion) {
        return 0;
    }
}
