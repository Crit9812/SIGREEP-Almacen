package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class modelNuevoTraspasoSalida {

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM ubicaciones WHERE LOWER(estado) = 'activo' ORDER BY nombre";

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

    public Optional<PreciosProducto> obtenerPreciosProducto(String idProducto) {
        return obtenerPreciosProducto(idProducto, null, null, null);
    }

    public Optional<PreciosProducto> obtenerPreciosProducto(String idProducto, String lote,
                                                            java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN ubicaciones u ON u.id = a.ubicacion
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
                      AND u.nombre = ?
                    %s
                    UNION ALL
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    JOIN ubicaciones u ON u.id = da.idUbicacion
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
                      AND u.nombre = ?
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                ) precios
                ORDER BY idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, filtroDetalle);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote != null ? lote : "");
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                } else {
                    ps.setDate(index++, null);
                    ps.setDate(index++, null);
                }
                ps.setString(index++, ubicacionNombre != null ? ubicacionNombre : "");
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                ps.setString(index++, idProducto);
                ps.setString(index++, lote != null ? lote : "");
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                } else {
                    ps.setDate(index++, null);
                    ps.setDate(index++, null);
                }
                ps.setString(index++, ubicacionNombre != null ? ubicacionNombre : "");
                ps.setString(index++, "disponible");
                if (colEstado != null) {
                    ps.setString(index, "segmentado");
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<PreciosProducto> obtenerPreciosProductoPorLoteCaducidad(String idProducto, String lote,
                                                                            java.time.LocalDate caducidad) {
        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
                    %s
                    UNION ALL
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                ) precios
                ORDER BY idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, filtroDetalle);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote != null ? lote : "");
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                } else {
                    ps.setDate(index++, null);
                    ps.setDate(index++, null);
                }
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                ps.setString(index++, idProducto);
                ps.setString(index++, lote != null ? lote : "");
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                } else {
                    ps.setDate(index++, null);
                    ps.setDate(index++, null);
                }
                ps.setString(index++, "disponible");
                if (colEstado != null) {
                    ps.setString(index, "segmentado");
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<PreciosProducto> obtenerPreciosProductoPorLotePresentacion(String idProducto, String lote,
                                                                               String presentacion) {
        String normalizada = presentacion != null ? presentacion.trim().toLowerCase() : "";
        boolean incluirDetalle = "pz".equals(normalizada) || "pieza".equals(normalizada);

        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada,
                           a.caducidad AS caducidadOrden
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND a.presentacion = ?
                      AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                    %s
                    %s
                ) precios
                ORDER BY
                    CASE WHEN caducidadOrden IS NOT NULL THEN 0 ELSE 1 END,
                    idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, incluirDetalle ? """
                    UNION ALL
                    SELECT de.precioUnitario / NULLIF((
                               SELECT COUNT(*)
                               FROM detalleArticulo daConteo
                               WHERE daConteo.idArticulo = a.idArticulo
                           ), 0) AS precioUnitario,
                           de.precioIVA / NULLIF((
                               SELECT COUNT(*)
                               FROM detalleArticulo daConteo
                               WHERE daConteo.idArticulo = a.idArticulo
                           ), 0) AS precioIVA,
                           de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada,
                           a.caducidad AS caducidadOrden
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                """.formatted(filtroDetalle) : "");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote);
                ps.setString(index++, presentacion);
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                if (incluirDetalle) {
                    ps.setString(index++, idProducto);
                    ps.setString(index++, lote);
                    ps.setString(index++, "disponible");
                    if (colEstado != null) {
                        ps.setString(index++, "segmentado");
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<PreciosProducto> obtenerPreciosProductoPorLotePresentacionFactor(String idProducto, String lote,
                                                                                     String presentacion, int factor) {
        String normalizada = presentacion != null ? presentacion.trim().toLowerCase() : "";
        boolean incluirDetalle = ("pz".equals(normalizada) || "pieza".equals(normalizada)) && factor == 1;

        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND a.presentacion = ?
                      AND a.factor = ?
                    %s
                    %s
                ) precios
                ORDER BY idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, incluirDetalle ? """
                    UNION ALL
                    SELECT de.precioUnitario / NULLIF((
                               SELECT COUNT(*)
                               FROM detalleArticulo daConteo
                               WHERE daConteo.idArticulo = a.idArticulo
                           ), 0) AS precioUnitario,
                           de.precioIVA / NULLIF((
                               SELECT COUNT(*)
                               FROM detalleArticulo daConteo
                               WHERE daConteo.idArticulo = a.idArticulo
                           ), 0) AS precioIVA,
                           de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    WHERE de.claveProducto = ?
                      AND a.lote = ?
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                """.formatted(filtroDetalle) : "");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote != null ? lote : "");
                ps.setString(index++, presentacion != null ? presentacion : "");
                ps.setInt(index++, factor);
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                if (incluirDetalle) {
                    ps.setString(index++, idProducto);
                    ps.setString(index++, lote != null ? lote : "");
                    ps.setString(index++, "disponible");
                    if (colEstado != null) {
                        ps.setString(index, "segmentado");
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<PreciosProducto> obtenerPreciosProductoUltimaEntrada(String idProducto) {
        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE de.claveProducto = ?
                    %s
                    UNION ALL
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    WHERE de.claveProducto = ?
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                ) precios
                ORDER BY idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, filtroDetalle);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                ps.setString(index++, idProducto);
                ps.setString(index++, "disponible");
                if (colEstado != null) {
                    ps.setString(index, "segmentado");
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean existeLote(String lote) {
        String sql = "SELECT 1 FROM articulo WHERE lote = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, lote);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteConCaducidad(String lote, java.time.LocalDate caducidad) {
        String sql = "SELECT 1 FROM articulo WHERE lote = ? AND caducidad = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, lote);
            ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteCaducidadUbicacion(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            int disponibles = GenericDAO.contarDisponiblesSinSalidaPorLoteCaducidadUbicacion(
                    conn, lote, caducidad, ubicacionNombre);
            return disponibles > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponible(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteCaducidadUbicacion(
                    conn, lote, caducidad, ubicacionNombre);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean existeLoteCaducidadUbicacionProducto(String idProducto, String lote,
                                                        java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            int disponibles = GenericDAO.contarDisponiblesSinSalidaPorProductoLoteCaducidadUbicacion(
                    conn, idProducto, lote, caducidad, ubicacionNombre);
            return disponibles > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponibleProductoUbicacion(String idProducto, String lote,
                                                          java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorProductoLoteCaducidadUbicacion(
                    conn, idProducto, lote, caducidad, ubicacionNombre);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int obtenerCantidadDisponibleDetalle(String idProducto, String lote, java.time.LocalDate caducidad,
                                                String presentacion, int factor, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            int disponibles = GenericDAO.contarDisponiblesSinSalidaDetalle(
                    conn, idProducto, lote, caducidad, presentacion, factor, ubicacionNombre);
            if (disponibles > 0 || esPresentacionDetalle(presentacion, factor)) {
                return disponibles;
            }

            return contarDetalleArticuloPorUbicacion(
                    conn,
                    idProducto,
                    lote,
                    caducidad,
                    ubicacionNombre
            );
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int contarDetalleArticuloPorUbicacion(Connection conn, String idProducto, String lote,
                                                  java.time.LocalDate caducidad, String ubicacionNombre) {
        if (conn == null || idProducto == null || lote == null || ubicacionNombre == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(*) AS total
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = da.idUbicacion
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(da.estado) = 'disponible'
              AND LOWER(a.Estado) = 'segmentado'
              AND u.nombre = ?
        """;

        if (caducidad != null) {
            sql += " AND a.caducidad = ? ";
        } else {
            sql += " AND a.caducidad IS NULL ";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, ubicacionNombre);
            if (caducidad != null) {
                ps.setDate(index, java.sql.Date.valueOf(caducidad));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean existeLoteParaProducto(String lote, String idProducto) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProducto(conn, lote, idProducto) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<java.time.LocalDate> obtenerCaducidadParaLoteProducto(String lote, String idProducto) {
        String sql = """
            SELECT caducidad
            FROM (
                SELECT a.caducidad AS caducidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE a.lote = ? AND de.claveProducto = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                UNION ALL
                SELECT a.caducidad AS caducidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE a.lote = ? AND de.claveProducto = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
            ) caducidades
            ORDER BY
                CASE
                    WHEN caducidad IS NOT NULL THEN 0
                    ELSE 1
                END,
                caducidad ASC
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setString(2, idProducto);
            ps.setString(3, lote);
            ps.setString(4, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    if (caducidad != null && !rs.wasNull()) {
                        return Optional.of(caducidad.toLocalDate());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean existeProductoLoteSinCaducidad(String idProducto, String lote) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProductoCaducidad(conn, lote, idProducto, null) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponibleProductoLoteCaducidad(String idProducto, String lote,
                                                              java.time.LocalDate caducidad) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProductoCaducidad(conn, lote, idProducto, caducidad);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public GenericDAO.ValidacionDisponibilidadSalida validarEntradaYDisponibilidadLoteProducto(String lote,
                                                                                               String idProducto) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.validarEntradaYDisponibilidadLoteProducto(conn, lote, idProducto);
        } catch (Exception e) {
            e.printStackTrace();
            return new GenericDAO.ValidacionDisponibilidadSalida(false, 0);
        }
    }

    public boolean existePresentacionParaProductoLote(String idProducto, String lote, String presentacion) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            if (!esPresentacionDetalle(presentacion)) {
                return false;
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND a.lote = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                psDetalle.setString(2, lote);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeFactorParaProductoLotePresentacion(String idProducto, String lote,
                                                            String presentacion, int factor) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ? AND a.factor = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            if (!esPresentacionDetalle(presentacion, factor)) {
                return false;
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND a.lote = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                psDetalle.setString(2, lote);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponibleProductoPresentacionFactor(String idProducto, String presentacion, int factor) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    if (!esPresentacionDetalle(presentacion, factor)) {
                        return total;
                    }
                    String sqlDetalle = """
                        SELECT COUNT(*) AS total
                        FROM detalleArticulo da
                        JOIN articulo a ON a.idArticulo = da.idArticulo
                        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                        WHERE de.claveProducto = ?
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(da.estado) = 'disponible'
                          AND LOWER(a.Estado) = 'segmentado'
                    """;
                    try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                        psDetalle.setString(1, idProducto);
                        try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                            if (rsDetalle.next()) {
                                total += rsDetalle.getInt("total");
                            }
                        }
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return 0;
    }

    public List<DisponibilidadRapida> obtenerDisponibilidadesRapidas(String idProducto, String presentacion, int factor) {
        List<DisponibilidadRapida> resultado = new ArrayList<>();
        boolean incluirDetalle = esPresentacionDetalle(presentacion, factor);
        String sql = """
            SELECT lote, caducidad, ubicacion, SUM(total) AS total
            FROM (
                SELECT a.lote AS lote, a.caducidad AS caducidad, u.nombre AS ubicacion, COUNT(*) AS total
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                JOIN ubicaciones u ON u.id = a.ubicacion
                WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                GROUP BY a.lote, a.caducidad, u.nombre
                %s
            ) disponibilidades
            GROUP BY lote, caducidad, ubicacion
            ORDER BY
                CASE
                    WHEN caducidad IS NOT NULL THEN 0
                    ELSE 1
                END,
                caducidad ASC,
                lote ASC
        """.formatted(incluirDetalle ? """
                UNION ALL
                SELECT a.lote AS lote, a.caducidad AS caducidad, u.nombre AS ubicacion, COUNT(*) AS total
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                JOIN ubicaciones u ON u.id = da.idUbicacion
                WHERE de.claveProducto = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                GROUP BY a.lote, a.caducidad, u.nombre
            """ : "");

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            if (incluirDetalle) {
                ps.setString(index, idProducto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lote = rs.getString("lote");
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    String ubicacion = rs.getString("ubicacion");
                    int total = rs.getInt("total");
                    java.time.LocalDate caducidadLocal = caducidad != null ? caducidad.toLocalDate() : null;
                    resultado.add(new DisponibilidadRapida(lote, caducidadLocal, ubicacion, total));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    public static class DisponibilidadRapida {
        private final String lote;
        private final java.time.LocalDate caducidad;
        private final String ubicacion;
        private final int total;

        public DisponibilidadRapida(String lote, java.time.LocalDate caducidad, String ubicacion, int total) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.ubicacion = ubicacion != null ? ubicacion : "";
            this.total = total;
        }

        public String getLote() {
            return lote;
        }

        public java.time.LocalDate getCaducidad() {
            return caducidad;
        }

        public String getUbicacion() {
            return ubicacion;
        }

        public int getTotal() {
            return total;
        }
    }


    private BigDecimal obtenerDecimal(ResultSet rs, String columna) {
        try {
            BigDecimal valor = rs.getBigDecimal(columna);
            return valor != null ? valor : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean esPresentacionDetalle(String presentacion) {
        if (presentacion == null) {
            return false;
        }
        String normalizada = presentacion.trim().toLowerCase();
        return "pz".equals(normalizada) || "pieza".equals(normalizada);
    }

    private boolean esPresentacionDetalle(String presentacion, int factor) {
        return esPresentacionDetalle(presentacion) && factor == 1;
    }

    public static class PreciosProducto {
        private final BigDecimal precioUnitario;
        private final BigDecimal precioIva;
        private final BigDecimal precioBruto;
        private final BigDecimal precioTotal;

        public PreciosProducto(BigDecimal precioUnitario, BigDecimal precioIva,
                               BigDecimal precioBruto, BigDecimal precioTotal) {
            this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
            this.precioIva = precioIva != null ? precioIva : BigDecimal.ZERO;
            this.precioBruto = precioBruto != null ? precioBruto : BigDecimal.ZERO;
            this.precioTotal = precioTotal != null ? precioTotal : BigDecimal.ZERO;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public BigDecimal getPrecioIva() {
            return precioIva;
        }

        public BigDecimal getPrecioBruto() {
            return precioBruto;
        }

        public BigDecimal getPrecioTotal() {
            return precioTotal;
        }
    }

    private String agregarFiltroEstado(String sqlBase, Connection conn) throws Exception {
        String colEstado = obtenerColumnaEstadoArticulo(conn);
        StringBuilder sql = new StringBuilder(sqlBase);
        if (colEstado != null) {
            String filtro = "LOWER(a." + colEstado + ") = ?";
            String sqlLower = sqlBase.toLowerCase();
            int insertPos = sql.length();
            int groupPos = sqlLower.indexOf(" group by ");
            int orderPos = sqlLower.indexOf(" order by ");
            if (groupPos >= 0 && orderPos >= 0) {
                insertPos = Math.min(groupPos, orderPos);
            } else if (groupPos >= 0) {
                insertPos = groupPos;
            } else if (orderPos >= 0) {
                insertPos = orderPos;
            }
            boolean tieneWhere = sqlLower.contains(" where ");
            String condicion = (tieneWhere ? " AND " : " WHERE ") + filtro + " ";
            if (insertPos < sql.length()) {
                sql.insert(insertPos, condicion);
            } else {
                sql.append(condicion);
            }
        }
        return sql.toString();
    }

    private int agregarParametroEstado(PreparedStatement ps, Connection conn, int index) throws Exception {
        String colEstado = obtenerColumnaEstadoArticulo(conn);
        if (colEstado != null) {
            ps.setString(index++, "disponible");
        }
        return index;
    }

    private String obtenerColumnaEstadoArticulo(Connection conn) throws Exception {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "articulo", null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                String lower = limpio.toLowerCase();
                if ("estado".equals(lower)) {
                    return limpio;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene la cantidad disponible para un producto con lote, caducidad, presentación y factor específicos
     */
    public int obtenerCantidadDisponibleProductoLoteCaducidadPresentacionFactor(
            String idProducto, String lote, java.time.LocalDate caducidad,
            String presentacion, int factor) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProductoPresentacionFactor(
                    conn, lote, idProducto, presentacion, factor);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int obtenerCantidadDisponibleProductoLote(String idProducto, String lote) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProducto(conn, lote, idProducto);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }



    /**
     * Verifica si una presentación existe para un producto (sin lote específico)
     * Para uso en modo rápido
     */
    public boolean existePresentacionParaProducto(String idProducto, String presentacion) {
        String sql = """
        SELECT 1
        FROM articulo a
        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
        WHERE de.claveProducto = ? AND a.presentacion = ?
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            if (!esPresentacionDetalle(presentacion)) {
                return false;
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un factor existe para una combinación producto-presentación (sin lote específico)
     * Para uso en modo rápido
     */
    public boolean existeFactorParaProductoPresentacion(String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT 1
        FROM articulo a
        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
        WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            if (!esPresentacionDetalle(presentacion, factor)) {
                return false;
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una combinación producto-presentación-factor existe en inventario
     * Para uso en modo rápido
     */
    public boolean existeCombinacionProductoPresentacionFactor(String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT 1
        FROM articulo a
        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
        WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            if (!esPresentacionDetalle(presentacion, factor)) {
                return false;
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene los precios de la última entrada de un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public Optional<PreciosProducto> obtenerPreciosProductoUltimaEntradaConPresentacion(
            String idProducto, String presentacion, int factor) {
        String normalizada = presentacion != null ? presentacion.trim().toLowerCase() : "";
        boolean incluirDetalle = ("pz".equals(normalizada) || "pieza".equals(normalizada)) && factor == 1;

        try (Connection conn = new Conexion().conectar()) {
            String colEstado = obtenerColumnaEstadoArticulo(conn);
            String filtroArticulo = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";
            String filtroDetalle = colEstado != null ? " AND LOWER(a." + colEstado + ") = ?" : "";

            String sql = """
                SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
                FROM (
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                    %s
                    %s
                ) precios
                ORDER BY idDetalleEntrada DESC
                LIMIT 1
            """.formatted(filtroArticulo, incluirDetalle ? """
                    UNION ALL
                    SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal,
                           de.idDetalleEntrada AS idDetalleEntrada
                    FROM detalle_Entrada de
                    JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
                    JOIN detalleArticulo da ON da.idArticulo = a.idArticulo
                    WHERE de.claveProducto = ?
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(da.estado) = ?
                    %s
                """.formatted(filtroDetalle) : "");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, presentacion);
                ps.setInt(index++, factor);
                if (colEstado != null) {
                    ps.setString(index++, "disponible");
                }
                if (incluirDetalle) {
                    ps.setString(index++, idProducto);
                    ps.setString(index++, "disponible");
                    if (colEstado != null) {
                        ps.setString(index, "segmentado");
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                        BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                        BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                        BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                        return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Si no encuentra con presentación y factor específicos, intentar con el método genérico
        return obtenerPreciosProductoUltimaEntrada(idProducto);
    }

    /**
     * Obtiene la cantidad disponible total de un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public int obtenerCantidadTotalDisponibleProductoPresentacionFactor(
            String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT COUNT(*) AS total
        FROM articulo a
        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
        WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    if (!esPresentacionDetalle(presentacion, factor)) {
                        return total;
                    }
                    String sqlDetalle = """
                        SELECT COUNT(*) AS total
                        FROM detalleArticulo da
                        JOIN articulo a ON a.idArticulo = da.idArticulo
                        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                        WHERE de.claveProducto = ?
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(da.estado) = 'disponible'
                          AND LOWER(a.Estado) = 'segmentado'
                    """;
                    try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                        psDetalle.setString(1, idProducto);
                        try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                            if (rsDetalle.next()) {
                                total += rsDetalle.getInt("total");
                            }
                        }
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return 0;
    }

    /**
     * Verifica si hay suficiente cantidad disponible para un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public boolean verificarDisponibilidadSuficiente(String idProducto, String presentacion,
                                                     int factor, int cantidadRequerida) {
        int disponible = obtenerCantidadDisponibleProductoPresentacionFactor(idProducto, presentacion, factor);
        return disponible >= cantidadRequerida;
    }

    /**
     * Obtiene las disponibilidades por lote para un producto con presentación y factor específicos
     * Ordenado por fecha de caducidad (más cercana primero)
     * Para uso en modo rápido
     */
    public List<DisponibilidadPorLote> obtenerDisponibilidadesPorLote(String idProducto,
                                                                      String presentacion, int factor) {
        List<DisponibilidadPorLote> resultado = new ArrayList<>();
        boolean incluirDetalle = esPresentacionDetalle(presentacion, factor);
        String sql = """
        SELECT lote, caducidad, SUM(cantidad) AS cantidad
        FROM (
            SELECT a.lote AS lote, a.caducidad AS caducidad, COUNT(*) AS cantidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
            GROUP BY a.lote, a.caducidad
            %s
        ) lotes
        GROUP BY lote, caducidad
        ORDER BY
            CASE
                WHEN caducidad IS NOT NULL THEN 0
                ELSE 1
            END,
            caducidad ASC,
            lote ASC
    """.formatted(incluirDetalle ? """
            UNION ALL
            SELECT a.lote AS lote, a.caducidad AS caducidad, COUNT(*) AS cantidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(da.estado) = 'disponible'
              AND LOWER(a.Estado) = 'segmentado'
            GROUP BY a.lote, a.caducidad
        """ : "");

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            if (incluirDetalle) {
                ps.setString(index, idProducto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lote = rs.getString("lote");
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    int cantidad = rs.getInt("cantidad");
                    java.time.LocalDate caducidadLocal = caducidad != null ? caducidad.toLocalDate() : null;
                    resultado.add(new DisponibilidadPorLote(lote, caducidadLocal, cantidad));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    /**
     * Clase interna para representar disponibilidad por lote
     */
    public static class DisponibilidadPorLote {
        private final String lote;
        private final java.time.LocalDate caducidad;
        private final int cantidad;

        public DisponibilidadPorLote(String lote, java.time.LocalDate caducidad, int cantidad) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.cantidad = cantidad;
        }

        public String getLote() {
            return lote;
        }

        public java.time.LocalDate getCaducidad() {
            return caducidad;
        }

        public int getCantidad() {
            return cantidad;
        }
    }

    /**
     * Obtiene las ubicaciones disponibles para un producto con presentación y factor específicos
     * Ordenadas por cantidad disponible (mayor a menor)
     * Para uso en modo rápido
     */
    public List<UbicacionDisponible> obtenerUbicacionesDisponibles(String idProducto,
                                                                   String presentacion, int factor) {
        List<UbicacionDisponible> resultado = new ArrayList<>();
        boolean incluirDetalle = esPresentacionDetalle(presentacion, factor);
        String sql = """
        SELECT ubicacion, SUM(cantidad) AS cantidad
        FROM (
            SELECT u.nombre AS ubicacion, COUNT(*) AS cantidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
            GROUP BY u.nombre
            %s
        ) ubicaciones
        GROUP BY ubicacion
        ORDER BY cantidad DESC, ubicacion ASC
    """.formatted(incluirDetalle ? """
            UNION ALL
            SELECT u.nombre AS ubicacion, COUNT(*) AS cantidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = da.idUbicacion
            WHERE de.claveProducto = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(da.estado) = 'disponible'
              AND LOWER(a.Estado) = 'segmentado'
            GROUP BY u.nombre
        """ : "");

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            if (incluirDetalle) {
                ps.setString(index, idProducto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ubicacion = rs.getString("ubicacion");
                    int cantidad = rs.getInt("cantidad");
                    resultado.add(new UbicacionDisponible(ubicacion, cantidad));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    /**
     * Clase interna para representar ubicaciones disponibles
     */
    public static class UbicacionDisponible {
        private final String ubicacion;
        private final int cantidad;

        public UbicacionDisponible(String ubicacion, int cantidad) {
            this.ubicacion = ubicacion != null ? ubicacion : "";
            this.cantidad = cantidad;
        }

        public String getUbicacion() {
            return ubicacion;
        }

        public int getCantidad() {
            return cantidad;
        }
    }

    public boolean verificarExistenciaProducto(String idProducto, String lote) {
        String sql = """
        SELECT 1
        FROM articulo a
        JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
        WHERE de.claveProducto = ? 
          AND a.lote = ?
          AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
          AND LOWER(a.estado) = 'disponible'
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            String sqlDetalle = """
                SELECT 1
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ?
                  AND a.lote = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(da.estado) = 'disponible'
                  AND LOWER(a.Estado) = 'segmentado'
                LIMIT 1
            """;
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                psDetalle.setString(1, idProducto);
                psDetalle.setString(2, lote);
                try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                    return rsDetalle.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
