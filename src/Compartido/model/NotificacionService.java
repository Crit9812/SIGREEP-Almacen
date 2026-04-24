package Compartido.model;

import conexion.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificacionService {

    public static class Notificacion {
        private final String id;
        private final Date fecha;
        private final String descripcion;
        private final String estado;

        public Notificacion(String id, Date fecha, String descripcion, String estado) {
            this.id = id;
            this.fecha = fecha;
            this.descripcion = descripcion;
            this.estado = estado;
        }

        public String getId() {
            return id;
        }

        public Date getFecha() {
            return fecha;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getEstado() {
            return estado;
        }
    }

    public void generarNotificacionesIniciales() {
        if (!esViernesEnMexico()) {
            return;
        }

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }
            generarNotificacionesCaducidad(conn);
            generarNotificacionesInventarioMinimo(conn);
        } catch (SQLException e) {
            System.err.println("Error al generar notificaciones iniciales: " + e.getMessage());
        }
    }

    public List<Notificacion> obtenerNotificaciones() {
        List<Notificacion> notificaciones = new ArrayList<>();
        String sql = "SELECT id, fecha, descripcion, estado FROM Notificaciones " +
                "ORDER BY (CASE WHEN LOWER(estado) = 'activo' THEN 0 ELSE 1 END), fecha DESC, id DESC";

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return notificaciones;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    notificaciones.add(new Notificacion(
                            rs.getString("id"),
                            rs.getDate("fecha"),
                            rs.getString("descripcion"),
                            rs.getString("estado")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener notificaciones: " + e.getMessage());
        }

        return notificaciones;
    }



    public String obtenerDetalleSegunTipo(String idNotificacion) {
        if (idNotificacion == null || idNotificacion.isBlank()) {
            return "";
        }

        String idLimpio = idNotificacion.trim();
        if (idLimpio.endsWith("C")) {
            return obtenerDetalleCaducidad(idLimpio.substring(0, idLimpio.length() - 1));
        }
        if (idLimpio.endsWith("M")) {
            return obtenerDetalleInventarioMinimo(idLimpio.substring(0, idLimpio.length() - 1));
        }
        return "";
    }

    private String obtenerDetalleCaducidad(String idArticuloTexto) {
        String sql = "SELECT a.lote, COALESCE(u.nombre, 'Sin ubicación') AS ubicacion " +
                "FROM articulo a " +
                "LEFT JOIN ubicaciones u ON u.id = a.ubicacion " +
                "WHERE a.idArticulo = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return "";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(idArticuloTexto));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String lote = rs.getString("lote");
                        String ubicacion = rs.getString("ubicacion");

                        return "Lote: " + (lote == null || lote.isBlank() ? "Sin lote" : lote) +
                                "\nUbicación: " + (ubicacion == null || ubicacion.isBlank() ? "Sin ubicación" : ubicacion);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return "";
        } catch (SQLException e) {
            System.err.println("Error al obtener detalle de caducidad: " + e.getMessage());
        }

        return "";
    }

    private String obtenerDetalleInventarioMinimo(String idProducto) {
        String sql = "SELECT p.nombre, p.unidadMedida, COALESCE(p.categoria, '') AS categoria, " +
                "COALESCE(p.descripcion, '') AS descripcion, COALESCE(m.nombre, '') AS marca " +
                "FROM productos p " +
                "LEFT JOIN marcas m ON m.id = p.marca " +
                "WHERE p.id = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return "";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String marca = rs.getString("marca");
                        String nombre = rs.getString("nombre");
                        String unidadMedida = rs.getString("unidadMedida");
                        String categoria = rs.getString("categoria");
                        String descripcion = rs.getString("descripcion");

                        return "Marca: " + (marca == null || marca.isBlank() ? "Sin marca" : marca) +
                                "\nNombre: " + (nombre == null || nombre.isBlank() ? "Sin nombre" : nombre) +
                                "\nUnidad de medida: " + (unidadMedida == null || unidadMedida.isBlank() ? "Sin unidad" : unidadMedida) +
                                "\nCategoría: " + (categoria == null || categoria.isBlank() ? "Sin categoría" : categoria) +
                                "\nDescripción: " + (descripcion == null || descripcion.isBlank() ? "Sin descripción" : descripcion);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalle de inventario mínimo: " + e.getMessage());
        }

        return "";
    }

    public boolean hayNotificacionesActivas() {
        String sql = "SELECT 1 FROM Notificaciones WHERE LOWER(estado) = 'activo' LIMIT 1";

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar notificaciones activas: " + e.getMessage());
            return false;
        }
    }

    public boolean marcarComoLeida(String idNotificacion) {
        String sql = "UPDATE Notificaciones SET estado = 'leido' WHERE id = ?";

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, idNotificacion);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al marcar notificación como leída: " + e.getMessage());
            return false;
        }
    }

    private boolean esViernesEnMexico() {
        ZoneId zonaMexico = ZoneId.of("America/Mexico_City");
        DayOfWeek diaActual = ZonedDateTime.now(zonaMexico).getDayOfWeek();
        return diaActual == DayOfWeek.FRIDAY;
    }

    private void generarNotificacionesCaducidad(Connection conn) throws SQLException {
        String sql = "SELECT a.idArticulo, a.caducidad " +
                "FROM articulo a " +
                "WHERE LOWER(a.Estado) = 'disponible' " +
                "AND a.caducidad IS NOT NULL " +
                "AND a.caducidad >= CURDATE() " +
                "AND a.caducidad <= DATE_ADD(CURDATE(), INTERVAL 2 MONTH)";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idArticulo = rs.getInt("idArticulo");
                Date caducidad = rs.getDate("caducidad");
                String idNotificacion = idArticulo + "C";
                String descripcion = "El artículo con id " + idArticulo +
                        " está próximo a caducar el " +
                        new java.text.SimpleDateFormat("dd/MM/yy").format(caducidad) + ".";

                upsertNotificacion(conn, idNotificacion, descripcion);
            }
        }
    }

    private void generarNotificacionesInventarioMinimo(Connection conn) throws SQLException {
        String sql = "SELECT p.id AS idProducto, p.inventarioMin, COUNT(a.idArticulo) AS disponibles " +
                "FROM productos p " +
                "LEFT JOIN detalle_Entrada de ON de.claveProducto = p.id " +
                "LEFT JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada AND LOWER(a.Estado) = 'disponible' " +
                "WHERE LOWER(p.estado) = 'activo' AND p.inventarioMin IS NOT NULL " +
                "GROUP BY p.id, p.inventarioMin " +
                "HAVING disponibles <= p.inventarioMin";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String idProducto = rs.getString("idProducto");
                int disponibles = rs.getInt("disponibles");
                int minimo = rs.getInt("inventarioMin");

                String idNotificacion = idProducto + "M";
                String descripcion = "El producto con id " + idProducto +
                        " tiene pocos artículos en stock: " + disponibles + "/" + minimo + ".";

                upsertNotificacion(conn, idNotificacion, descripcion);
            }
        }
    }

    private void upsertNotificacion(Connection conn, String idNotificacion, String descripcion) throws SQLException {
        String existeSql = "SELECT id FROM Notificaciones WHERE id = ?";
        try (PreparedStatement psExiste = conn.prepareStatement(existeSql)) {
            psExiste.setString(1, idNotificacion);
            try (ResultSet rs = psExiste.executeQuery()) {
                if (rs.next()) {
                    String update = "UPDATE Notificaciones SET estado = 'activo', descripcion = ?, fecha = CURDATE() WHERE id = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(update)) {
                        psUpdate.setString(1, descripcion);
                        psUpdate.setString(2, idNotificacion);
                        psUpdate.executeUpdate();
                    }
                } else {
                    String insert = "INSERT INTO Notificaciones (id, fecha, descripcion, estado) VALUES (?, CURDATE(), ?, 'activo')";
                    try (PreparedStatement psInsert = conn.prepareStatement(insert)) {
                        psInsert.setString(1, idNotificacion);
                        psInsert.setString(2, descripcion);
                        psInsert.executeUpdate();
                    }
                }
            }
        }
    }
}
