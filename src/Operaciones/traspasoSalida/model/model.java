package Operaciones.traspasoSalida.model;

import Compartido.sesion.SesionUsuario;
import conexion.Conexion;
import conexion.ConexionExtra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class model {

    public List<String> obtenerNombresSucursales() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM sucursales WHERE status = 'activo' ORDER BY nombre";

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

    public String obtenerIdSucursalPorNombre(String nombreSucursal) {
        String sql = "SELECT id FROM sucursales WHERE nombre = ? AND status = 'activo' LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreSucursal);
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

    private String cancelarOperacion(Connection connOrigen, Connection connDestino, String mensaje, Exception e) {
        if (e != null) {
            e.printStackTrace();
        }

        try {
            if (connOrigen != null) {
                connOrigen.rollback();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (connDestino != null) {
                connDestino.rollback();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        javax.swing.JOptionPane.showMessageDialog(
                null,
                mensaje,
                "Operación cancelada",
                javax.swing.JOptionPane.ERROR_MESSAGE
        );

        return null;
    }


    public String registrarTraspasoSalida(String idDestinatario, String nombreDestinatario, String comentario, List<traspasoSalida> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        Connection connOrigen = null;
        Connection connDestino = null;

        try {
            connOrigen = new Conexion().conectar();
            connDestino = new ConexionExtra().conectar(nombreDestinatario);

            connOrigen.setAutoCommit(false);
            connDestino.setAutoCommit(false);

            long idSalida = registrarSalidaRemitente(connOrigen, idDestinatario, comentario, items);

            if (idSalida <= 0) {
                return cancelarOperacion(connOrigen, connDestino, "No se pudo registrar la salida. Se deshizo toda la operación.", null);
            }

            long idEntrada = registrarEntradaDestinatario(connDestino, idDestinatario, comentario, items, idSalida);

            if (idEntrada <= 0) {
                return cancelarOperacion(connOrigen, connDestino, "No se pudo registrar la entrada. Se deshizo toda la operación.", null);
            }

            Map<String, String> columnasSalida = obtenerColumnas(connOrigen, "salidas");

            String colIdSalida = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
            String colIdEntradaDestinatario = resolverColumna(columnasSalida, "idEntradaDestinatario");

            if (colIdSalida == null || colIdEntradaDestinatario == null) {
                return cancelarOperacion(connOrigen, connDestino, "No se encontraron las columnas necesarias. Se deshizo toda la operación.", null);
            }

            String sqlUpdateSalida = "UPDATE salidas SET " + colIdEntradaDestinatario + " = ? WHERE " + colIdSalida + " = ?";

            try (PreparedStatement ps = connOrigen.prepareStatement(sqlUpdateSalida)) {
                ps.setLong(1, idEntrada);
                ps.setLong(2, idSalida);

                if (ps.executeUpdate() <= 0) {
                    return cancelarOperacion(connOrigen, connDestino, "No se pudo actualizar la salida con la entrada del destinatario. Se deshizo toda la operación.", null);
                }
            }

            connDestino.commit();
            connOrigen.commit();

            return String.valueOf(idSalida);

        } catch (Exception e) {
            return cancelarOperacion(connOrigen, connDestino, "Ocurrió un error. Se deshizo toda la operación.", e);

        } finally {
            try {
                if (connOrigen != null) connOrigen.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (connDestino != null) connDestino.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private long registrarSalidaRemitente(Connection conn, String idDestinatario, String comentario, List<traspasoSalida> items) throws Exception {
        Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
        Map<String, Object> valoresSalida = new LinkedHashMap<>();

        String colDestinatario = resolverColumna(columnasSalida, "idDestinatario", "destinatario", "idSucursal",
                "id_sucursal", "sucursal", "sucursal_id");
        String colFactura = resolverColumna(columnasSalida, "noFatura", "noFactura", "factura",
                "numeroFactura", "numero_factura");
        String colComentario = resolverColumna(columnasSalida, "nota", "comentario", "observaciones");
        String colFecha = resolverColumna(columnasSalida, "fechaSalida", "fecha", "fecha_salida", "created_at");
        String colHora = resolverColumna(columnasSalida, "horaSalida", "hora", "hora_salida");
        String colTipo = resolverColumna(columnasSalida, "tipoSalida", "tipo", "tipo_salida");
        String colPrecioNeto = resolverColumna(columnasSalida, "precioNetoSalida", "precioNeto",
                "precio_neto", "precioNetoTotal");
        String colPrecioTotal = resolverColumna(columnasSalida, "precioTotalSalida", "precioTotal", "precio_total");
        String colUsuarioSalida = resolverColumna(columnasSalida, "claveUsuarioSalida", "idUsuarioSalida",
                "id_usuario_salida", "usuarioSalida", "usuario_salida");
        String colEstado = resolverColumna(columnasSalida, "Estado", "estado");
        String colEntradaDestinatario = resolverColumna(columnasSalida, "idEntradaDestinatario");

        if (colDestinatario != null) valoresSalida.put(colDestinatario, idDestinatario);
        if (colFactura != null) valoresSalida.put(colFactura, null);
        if (colComentario != null) valoresSalida.put(colComentario, comentario != null ? comentario : "");
        if (colFecha != null) valoresSalida.put(colFecha, Date.valueOf(LocalDate.now()));
        if (colHora != null) valoresSalida.put(colHora, Time.valueOf(LocalTime.now()));
        if (colTipo != null) valoresSalida.put(colTipo, "traspaso");
        if (colEstado != null) valoresSalida.put(colEstado, "pendiente");
        if (colEntradaDestinatario != null) valoresSalida.put(colEntradaDestinatario, null);

        Integer idUsuarioSalida = SesionUsuario.getIdUsuario();

        if (colUsuarioSalida != null && idUsuarioSalida != null) {
            valoresSalida.put(colUsuarioSalida, idUsuarioSalida);
        }

        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (traspasoSalida item : items) {
            totalNeto = totalNeto.add(parseDecimal(item.getPrecioBruto()));
            totalGeneral = totalGeneral.add(parseDecimal(item.getPrecioTotal()));
        }

        if (colPrecioNeto != null) {
            valoresSalida.put(colPrecioNeto, totalNeto.setScale(2, RoundingMode.HALF_UP));
        }

        if (colPrecioTotal != null) {
            valoresSalida.put(colPrecioTotal, totalGeneral.setScale(2, RoundingMode.HALF_UP));
        }

        long idSalida = insertarRegistro(conn, "salidas", columnasSalida, valoresSalida);

        if (idSalida <= 0) {
            throw new Exception("No se pudo insertar el registro en salidas.");
        }

        Map<String, String> columnasDetalleSalida = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
        Map<String, String> columnasAjustes = obtenerColumnas(conn, "ajuste_inventario");

        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");

        String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                "id_detalle_entrada");
        String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                "id_producto", "producto_id");
        String colDetalleEntradaClaveEntrada = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada",
                "entrada_id", "id_entrada");
        String colDetalleEntradaEstado = resolverColumna(columnasDetalleEntrada, "estado", "Estado");

        String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloLote = resolverColumna(columnasArticulo, "lote");
        String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
        String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
        String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        String colEntradaEstado = resolverColumna(columnasEntradas, "Estado", "estado");
        String colEntradaId = resolverColumna(columnasEntradas, "idEntrada", "id", "id_entrada");

        String colAjusteEstado = resolverColumna(columnasAjustes, "Estado", "estado");
        String colAjusteId = resolverColumna(columnasAjustes, "idAjuste", "id", "id_ajuste");

        String colDetalleArticuloId = resolverColumna(columnasDetalleArticulo, "idDetalle", "id");
        String colDetalleArticuloSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");
        String colDetalleArticuloUbicacion = resolverColumna(columnasDetalleArticulo, "idUbicacion",
                "id_ubicacion", "ubicacion_id");
        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo",
                "id_articulo", "articulo_id");

        if (colArticuloDetalleSalida == null || colArticuloId == null || colArticuloDetalleEntrada == null
                || colDetalleEntradaId == null || colDetalleEntradaClaveEntrada == null
                || colEntradaEstado == null || colEntradaId == null) {
            throw new Exception("Faltan columnas necesarias para registrar la salida.");
        }

        Set<Integer> detallesEntradaActualizados = new HashSet<>();

        for (traspasoSalida item : items) {
            Map<String, Object> valoresDetalle = new LinkedHashMap<>();

            String colSalida = resolverColumna(columnasDetalleSalida, "claveSalida", "idSalida", "id_salida", "salida_id");
            String colProducto = resolverColumna(columnasDetalleSalida, "claveProductoSalida", "claveProducto",
                    "idProducto", "id_producto", "producto_id");
            String colCantidad = resolverColumna(columnasDetalleSalida, "cantidad", "cantidadSalida", "cantidad_salida");
            String colPrecioSalida = resolverColumna(columnasDetalleSalida, "precioUnitarioSalida", "precioUnitario",
                    "precioSalida", "precio_salida", "precioSalidaUnitario");
            String colPrecioIva = resolverColumna(columnasDetalleSalida, "precioIVASalida", "precioIVA", "precioIva",
                    "precio_iva");
            String colPrecioBruto = resolverColumna(columnasDetalleSalida, "precioBrutoTotalSalida",
                    "precioBrutoTotal", "precioBruto", "precio_bruto");
            String colPrecioTotalDetalle = resolverColumna(columnasDetalleSalida, "precioTotalSalida", "precioTotal",
                    "precio_total");
            String colDetalleLote = resolverColumna(columnasDetalleSalida, "lote");
            String colDetalleCaducidad = resolverColumna(columnasDetalleSalida, "caducidad");
            String colDetallePresentacion = resolverColumna(columnasDetalleSalida, "presentacion");
            String colDetalleFactor = resolverColumna(columnasDetalleSalida, "factor");
            String colDetalleEstado = resolverColumna(columnasDetalleSalida, "estado", "Estado");

            if (colSalida != null) valoresDetalle.put(colSalida, idSalida);
            if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
            if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
            if (colPrecioSalida != null) valoresDetalle.put(colPrecioSalida, parseDecimal(item.getPrecioEntrada()));
            if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
            if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
            if (colPrecioTotalDetalle != null) valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));
            if (colDetalleLote != null) valoresDetalle.put(colDetalleLote, item.getLote());
            if (colDetalleCaducidad != null) valoresDetalle.put(colDetalleCaducidad, parseDate(item.getCaducidad()));
            if (colDetallePresentacion != null) valoresDetalle.put(colDetallePresentacion, item.getPresentacion());
            if (colDetalleFactor != null) valoresDetalle.put(colDetalleFactor, item.getFactor());
            if (colDetalleEstado != null) valoresDetalle.put(colDetalleEstado, "activo");

            long idDetalleSalida = insertarRegistro(conn, "detalle_Salida", columnasDetalleSalida, valoresDetalle);

            if (idDetalleSalida <= 0 || item.getUbicaciones() == null || item.getUbicaciones().isEmpty()) {
                throw new Exception("No se pudo registrar el detalle de salida o no hay ubicaciones.");
            }

            for (Operaciones.compra.model.UbicacionCompra ubicacion : item.getUbicaciones()) {
                if (ubicacion == null || ubicacion.getUbicacion() == null) {
                    continue;
                }

                int cantidad = Math.max(0, ubicacion.getCantidad());

                if (cantidad == 0) {
                    continue;
                }

                Integer ubicacionId = resolverUbicacionId(conn, ubicacion.getUbicacion().trim());

                if (ubicacionId == null) {
                    throw new Exception("No se pudo resolver la ubicación.");
                }

                List<Integer> articulosParaActualizar = new ArrayList<>();
                List<Integer> detallesEntrada = new ArrayList<>();
                List<String> detalleArticulosParaActualizar = new ArrayList<>();

                StringBuilder sqlSelect = new StringBuilder("SELECT a.")
                        .append(colArticuloId)
                        .append(", a.")
                        .append(colArticuloDetalleEntrada)
                        .append(" FROM articulo a JOIN detalle_Entrada de ON de.")
                        .append(colDetalleEntradaId)
                        .append(" = a.")
                        .append(colArticuloDetalleEntrada)
                        .append(" WHERE 1=1");

                sqlSelect.append(" AND (a.").append(colArticuloDetalleSalida).append(" IS NULL OR a.")
                        .append(colArticuloDetalleSalida).append(" = 0)");

                if (colArticuloLote != null) sqlSelect.append(" AND a.").append(colArticuloLote).append(" = ?");

                if (colArticuloCaducidad != null) {
                    Date caducidad = parseDate(item.getCaducidad());

                    if (caducidad != null) {
                        sqlSelect.append(" AND a.").append(colArticuloCaducidad).append(" = ?");
                    } else {
                        sqlSelect.append(" AND a.").append(colArticuloCaducidad).append(" IS NULL");
                    }
                }

                if (colArticuloUbicacion != null) sqlSelect.append(" AND a.").append(colArticuloUbicacion).append(" = ?");
                if (colArticuloPresentacion != null) sqlSelect.append(" AND a.").append(colArticuloPresentacion).append(" = ?");
                if (colArticuloFactor != null) sqlSelect.append(" AND a.").append(colArticuloFactor).append(" = ?");
                if (colArticuloEstado != null) sqlSelect.append(" AND LOWER(a.").append(colArticuloEstado).append(") = ?");
                if (colDetalleEntradaProducto != null) sqlSelect.append(" AND de.").append(colDetalleEntradaProducto).append(" = ?");

                sqlSelect.append(" LIMIT ?");

                try (PreparedStatement ps = conn.prepareStatement(sqlSelect.toString())) {
                    int index = 1;

                    if (colArticuloLote != null) ps.setString(index++, item.getLote());

                    if (colArticuloCaducidad != null) {
                        Date caducidad = parseDate(item.getCaducidad());

                        if (caducidad != null) {
                            ps.setDate(index++, caducidad);
                        }
                    }

                    if (colArticuloUbicacion != null) ps.setInt(index++, ubicacionId);
                    if (colArticuloPresentacion != null) ps.setString(index++, item.getPresentacion());
                    if (colArticuloFactor != null) ps.setInt(index++, item.getFactor());
                    if (colArticuloEstado != null) ps.setString(index++, "disponible");
                    if (colDetalleEntradaProducto != null) ps.setString(index++, item.getClaveProducto());

                    ps.setInt(index, cantidad);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            articulosParaActualizar.add(rs.getInt(colArticuloId));
                            detallesEntrada.add(rs.getInt(colArticuloDetalleEntrada));
                        }
                    }
                }

                if (articulosParaActualizar.size() < cantidad && esPresentacionDetalle(item.getPresentacion(), item.getFactor())) {
                    int restantes = cantidad - articulosParaActualizar.size();

                    String sqlDetalle = """
                SELECT da.%s AS idDetalle, a.%s AS detalleEntrada
                FROM detalleArticulo da
                JOIN articulo a ON a.%s = da.%s
                JOIN detalle_Entrada de ON de.%s = a.%s
                WHERE (da.%s IS NULL OR TRIM(CAST(da.%s AS CHAR)) = '' OR TRIM(CAST(da.%s AS CHAR)) = '0')
                  AND LOWER(da.%s) IN (?, ?)
                  AND LOWER(a.%s) = ?
                """.formatted(
                            colDetalleArticuloId,
                            colArticuloDetalleEntrada,
                            colArticuloId,
                            colDetalleArticuloArticulo,
                            colDetalleEntradaId,
                            colArticuloDetalleEntrada,
                            colDetalleArticuloSalida,
                            colDetalleArticuloSalida,
                            colDetalleArticuloSalida,
                            colDetalleArticuloEstado,
                            colArticuloEstado != null ? colArticuloEstado : "Estado"
                    );

                    StringBuilder sqlDetalleBuilder = new StringBuilder(sqlDetalle);

                    if (colArticuloLote != null) sqlDetalleBuilder.append(" AND a.").append(colArticuloLote).append(" = ?");

                    if (colArticuloCaducidad != null) {
                        Date caducidad = parseDate(item.getCaducidad());

                        if (caducidad != null) {
                            sqlDetalleBuilder.append(" AND a.").append(colArticuloCaducidad).append(" = ?");
                        } else {
                            sqlDetalleBuilder.append(" AND a.").append(colArticuloCaducidad).append(" IS NULL");
                        }
                    }

                    if (colDetalleArticuloUbicacion != null) sqlDetalleBuilder.append(" AND da.").append(colDetalleArticuloUbicacion).append(" = ?");
                    if (colDetalleEntradaProducto != null) sqlDetalleBuilder.append(" AND de.").append(colDetalleEntradaProducto).append(" = ?");

                    sqlDetalleBuilder.append(" LIMIT ?");

                    try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalleBuilder.toString())) {
                        int indexDetalle = 1;

                        psDetalle.setString(indexDetalle++, "activo");
                        psDetalle.setString(indexDetalle++, "disponible");
                        psDetalle.setString(indexDetalle++, "segmentado");

                        if (colArticuloLote != null) psDetalle.setString(indexDetalle++, item.getLote());

                        if (colArticuloCaducidad != null) {
                            Date caducidad = parseDate(item.getCaducidad());

                            if (caducidad != null) {
                                psDetalle.setDate(indexDetalle++, caducidad);
                            }
                        }

                        if (colDetalleArticuloUbicacion != null) psDetalle.setInt(indexDetalle++, ubicacionId);
                        if (colDetalleEntradaProducto != null) psDetalle.setString(indexDetalle++, item.getClaveProducto());

                        psDetalle.setInt(indexDetalle, restantes);

                        try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                            while (rsDetalle.next()) {
                                detalleArticulosParaActualizar.add(rsDetalle.getString("idDetalle"));
                                detallesEntrada.add(rsDetalle.getInt("detalleEntrada"));
                            }
                        }
                    }
                }

                if (articulosParaActualizar.size() + detalleArticulosParaActualizar.size() < cantidad) {
                    throw new Exception("No hay suficientes artículos disponibles para completar la salida.");
                }

                if (!articulosParaActualizar.isEmpty()) {
                    String placeholders = String.join(", ", Collections.nCopies(articulosParaActualizar.size(), "?"));

                    StringBuilder sqlUpdate = new StringBuilder("UPDATE articulo SET ")
                            .append(colArticuloDetalleSalida)
                            .append(" = ?");

                    if (colArticuloEstado != null) {
                        sqlUpdate.append(", ").append(colArticuloEstado).append(" = ?");
                    }

                    sqlUpdate.append(" WHERE ").append(colArticuloId).append(" IN (").append(placeholders).append(")");

                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate.toString())) {
                        int index = 1;

                        psUpdate.setLong(index++, idDetalleSalida);

                        if (colArticuloEstado != null) {
                            psUpdate.setString(index++, "pendiente");
                        }

                        for (Integer idArticulo : articulosParaActualizar) {
                            psUpdate.setInt(index++, idArticulo);
                        }

                        if (psUpdate.executeUpdate() < articulosParaActualizar.size()) {
                            throw new Exception("No se pudieron actualizar todos los artículos.");
                        }
                    }
                }

                if (!detalleArticulosParaActualizar.isEmpty()
                        && colDetalleArticuloSalida != null
                        && colDetalleArticuloEstado != null
                        && colDetalleArticuloId != null) {

                    String placeholders = String.join(", ", Collections.nCopies(detalleArticulosParaActualizar.size(), "?"));

                    String sqlUpdateDetalle = "UPDATE detalleArticulo SET "
                            + colDetalleArticuloSalida + " = ?, "
                            + colDetalleArticuloEstado + " = ? WHERE "
                            + colDetalleArticuloId + " IN (" + placeholders + ")";

                    try (PreparedStatement psDetalleUpdate = conn.prepareStatement(sqlUpdateDetalle)) {
                        int index = 1;

                        psDetalleUpdate.setLong(index++, idDetalleSalida);
                        psDetalleUpdate.setString(index++, "pendiente");

                        for (String idDetalle : detalleArticulosParaActualizar) {
                            psDetalleUpdate.setString(index++, idDetalle);
                        }

                        if (psDetalleUpdate.executeUpdate() < detalleArticulosParaActualizar.size()) {
                            throw new Exception("No se pudieron actualizar todos los detalles de artículo.");
                        }
                    }
                }

                detallesEntradaActualizados.addAll(detallesEntrada);
            }
        }

        for (Integer detalleEntradaId : detallesEntradaActualizados) {
            actualizarEstadoEntradaPorDetalle(
                    conn,
                    detalleEntradaId,
                    colEntradaId,
                    colEntradaEstado,
                    colDetalleEntradaId,
                    colDetalleEntradaClaveEntrada,
                    colDetalleEntradaEstado,
                    colArticuloDetalleEntrada,
                    colArticuloEstado,
                    colAjusteId,
                    colAjusteEstado,
                    colArticuloId,
                    colDetalleArticuloArticulo,
                    colDetalleArticuloEstado
            );
        }

        return idSalida;
    }


    private long registrarEntradaDestinatario(Connection conn, String idRemitente, String comentario,
                                              List<traspasoSalida> items, long idSalidaRemitente) throws Exception {

        Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
        Map<String, Object> valoresEntrada = new LinkedHashMap<>();

        String colFactura = resolverColumna(columnasEntradas, "noFactura", "noFatura", "factura", "numeroFactura", "numero_factura");
        String colFecha = resolverColumna(columnasEntradas, "fechaEntrada", "fecha", "fecha_entrada", "created_at");
        String colHora = resolverColumna(columnasEntradas, "horaEntrada", "hora", "hora_entrada");
        String colTipo = resolverColumna(columnasEntradas, "tipoEntrada", "tipo", "tipo_entrada");
        String colUsuario = resolverColumna(columnasEntradas, "claveUsuarioEntrada", "idUsuarioEntrada",
                "id_usuario_entrada", "usuarioEntrada", "usuario_entrada");
        String colRemitente = resolverColumna(columnasEntradas, "idRemitente", "remitente", "idSucursal",
                "id_sucursal", "sucursal", "sucursal_id");
        String colPrecioNeto = resolverColumna(columnasEntradas, "precioNetoEntrada", "precioNeto",
                "precio_neto", "precioNetoTotal");
        String colPrecioTotal = resolverColumna(columnasEntradas, "precioTotalEntrada", "precioTotal", "precio_total");
        String colNota = resolverColumna(columnasEntradas, "nota", "comentario", "observaciones");
        String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");
        String colSalidaRemitente = resolverColumna(columnasEntradas, "idSalidaRemitente");

        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (traspasoSalida item : items) {
            totalNeto = totalNeto.add(parseDecimal(item.getPrecioBruto()));
            totalGeneral = totalGeneral.add(parseDecimal(item.getPrecioTotal()));
        }

        if (colFactura != null) valoresEntrada.put(colFactura, 0);
        if (colFecha != null) valoresEntrada.put(colFecha, Date.valueOf(LocalDate.now()));
        if (colHora != null) valoresEntrada.put(colHora, Time.valueOf(LocalTime.now()));
        if (colTipo != null) valoresEntrada.put(colTipo, "traspaso");
        if (colUsuario != null && SesionUsuario.getIdUsuario() != null) valoresEntrada.put(colUsuario, SesionUsuario.getIdUsuario());
        if (colRemitente != null) valoresEntrada.put(colRemitente, idRemitente);
        if (colPrecioNeto != null) valoresEntrada.put(colPrecioNeto, totalNeto.setScale(2, RoundingMode.HALF_UP));
        if (colPrecioTotal != null) valoresEntrada.put(colPrecioTotal, totalGeneral.setScale(2, RoundingMode.HALF_UP));
        if (colNota != null) valoresEntrada.put(colNota, comentario != null ? comentario : "");
        if (colEstado != null) valoresEntrada.put(colEstado, "revision");
        if (colSalidaRemitente != null) valoresEntrada.put(colSalidaRemitente, idSalidaRemitente);

        long idEntrada = insertarRegistro(conn, "entradas", columnasEntradas, valoresEntrada);

        if (idEntrada <= 0) {
            throw new Exception("No se pudo insertar el registro en entradas.");
        }

        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        for (traspasoSalida item : items) {
            Map<String, Object> valoresDetalle = new LinkedHashMap<>();

            String colEntrada = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada", "entrada_id", "id_entrada");
            String colProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto", "id_producto", "producto_id");
            String colCantidad = resolverColumna(columnasDetalleEntrada, "cantidad", "cantidadEntrada", "cantidad_entrada");
            String colPrecioEntrada = resolverColumna(columnasDetalleEntrada, "precioUnitarioEntrada", "precioUnitario",
                    "precioEntrada", "precio_entrada", "precioEntradaUnitario");
            String colPrecioIva = resolverColumna(columnasDetalleEntrada, "precioIVAEntrada", "precioIVA", "precioIva", "precio_iva");
            String colPrecioBruto = resolverColumna(columnasDetalleEntrada, "precioBrutoTotalEntrada",
                    "precioBrutoTotal", "precioBruto", "precio_bruto");
            String colPrecioTotalDetalle = resolverColumna(columnasDetalleEntrada, "precioTotalEntrada", "precioTotal", "precio_total");
            String colLote = resolverColumna(columnasDetalleEntrada, "lote");
            String colCaducidad = resolverColumna(columnasDetalleEntrada, "caducidad");
            String colPresentacion = resolverColumna(columnasDetalleEntrada, "presentacion");
            String colFactor = resolverColumna(columnasDetalleEntrada, "factor");
            String colEstadoDetalle = resolverColumna(columnasDetalleEntrada, "estado", "Estado");

            if (colEntrada != null) valoresDetalle.put(colEntrada, idEntrada);
            if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
            if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
            if (colPrecioEntrada != null) valoresDetalle.put(colPrecioEntrada, parseDecimal(item.getPrecioEntrada()));
            if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
            if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
            if (colPrecioTotalDetalle != null) valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));
            if (colLote != null) valoresDetalle.put(colLote, item.getLote());
            if (colCaducidad != null) valoresDetalle.put(colCaducidad, parseDate(item.getCaducidad()));
            if (colPresentacion != null) valoresDetalle.put(colPresentacion, item.getPresentacion());
            if (colFactor != null) valoresDetalle.put(colFactor, item.getFactor());
            if (colEstadoDetalle != null) valoresDetalle.put(colEstadoDetalle, "revision");

            long idDetalleEntrada = insertarRegistro(conn, "detalle_Entrada", columnasDetalleEntrada, valoresDetalle);

            if (idDetalleEntrada <= 0) {
                throw new Exception("No se pudo insertar el detalle de entrada.");
            }

            int cantidadArticulos = Math.max(0, item.getCantidad());

            String presentacionArticulo = item.getPresentacion();
            int factorArticulo = item.getFactor();

            if (esPresentacionDetalle(item.getPresentacion(), item.getFactor())) {
                presentacionArticulo = "pz";
                factorArticulo = 1;
            }

            for (int i = 0; i < cantidadArticulos; i++) {
                Map<String, Object> valoresArticulo = new LinkedHashMap<>();

                String colDetalleEntradaArticulo = resolverColumna(columnasArticulo, "idDetalleEntrada",
                        "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                String colLoteArticulo = resolverColumna(columnasArticulo, "lote");
                String colCaducidadArticulo = resolverColumna(columnasArticulo, "caducidad");
                String colPresentacionArticulo = resolverColumna(columnasArticulo, "presentacion");
                String colFactorArticulo = resolverColumna(columnasArticulo, "factor");
                String colEstadoArticulo = resolverColumna(columnasArticulo, "Estado", "estado");
                String colSegmentadoArticulo = resolverColumna(columnasArticulo, "segmentado");

                if (colDetalleEntradaArticulo != null) valoresArticulo.put(colDetalleEntradaArticulo, idDetalleEntrada);
                if (colLoteArticulo != null) valoresArticulo.put(colLoteArticulo, item.getLote());
                if (colCaducidadArticulo != null) valoresArticulo.put(colCaducidadArticulo, parseDate(item.getCaducidad()));
                if (colPresentacionArticulo != null) valoresArticulo.put(colPresentacionArticulo, presentacionArticulo);
                if (colFactorArticulo != null) valoresArticulo.put(colFactorArticulo, factorArticulo);
                if (colEstadoArticulo != null) valoresArticulo.put(colEstadoArticulo, "revision");
                if (colSegmentadoArticulo != null) valoresArticulo.put(colSegmentadoArticulo, 0);

                long idArticulo = insertarRegistro(conn, "articulo", columnasArticulo, valoresArticulo);

                if (idArticulo <= 0) {
                    throw new Exception("No se pudo insertar el artículo de entrada.");
                }
            }
        }

        return idEntrada;
    }




    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new java.util.HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String nombreLimpio = nombre.trim();
                columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    if (nombre == null) {
                        continue;
                    }
                    String nombreLimpio = nombre.trim();
                    columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
                }
            }
        }

        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) continue;
            String match = columnas.get(candidato.toLowerCase());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private boolean esPresentacionDetalle(String presentacion, int factor) {
        if (presentacion == null) {
            return false;
        }
        String normalizada = presentacion.trim().toLowerCase();
        return ("pz".equals(normalizada) || "pieza".equals(normalizada)) && factor == 1;
    }

    private long insertarRegistro(Connection conn, String tabla, Map<String, String> columnas, Map<String, Object> valores)
            throws SQLException {
        if (valores.isEmpty()) {
            throw new SQLException("No hay valores para insertar en " + tabla);
        }

        String columnasSql = String.join(", ", valores.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(valores.size(), "?"));

        String sql = "INSERT INTO " + tabla + " (" + columnasSql + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            for (Object value : valores.values()) {
                ps.setObject(index++, value);
            }

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

        return 0;
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

    private Date parseDate(String fecha) {
        if (fecha == null || fecha.isBlank() ||
                fecha.equalsIgnoreCase("null") ||
                fecha.equalsIgnoreCase("n/a") ||
                fecha.trim().isEmpty()) {
            return null;
        }

        String fechaLimpia = fecha.trim();

        try {
            return Date.valueOf(fechaLimpia);
        } catch (Exception e) {
            return null;
        }
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

        return null;
    }

    private void actualizarEstadoEntradaPorDetalle(Connection conn,
                                                   int detalleEntradaId,
                                                   String colEntradaId,
                                                   String colEntradaEstado,
                                                   String colDetalleEntradaId,
                                                   String colDetalleEntradaClaveEntrada,
                                                   String colDetalleEntradaEstado,
                                                   String colArticuloDetalleEntrada,
                                                   String colArticuloEstado,
                                                   String colAjusteId,
                                                   String colAjusteEstado,
                                                   String colArticuloId,
                                                   String colDetalleArticuloArticulo,
                                                   String colDetalleArticuloEstado) throws SQLException {
        if (colDetalleEntradaId == null || colDetalleEntradaClaveEntrada == null || colArticuloDetalleEntrada == null
                || colArticuloEstado == null) {
            return;
        }

        String claveEntrada = obtenerClaveEntrada(conn, detalleEntradaId, colDetalleEntradaId, colDetalleEntradaClaveEntrada);
        if (claveEntrada == null) {
            return;
        }

        String filtroClaveEntrada = esClaveEntradaNumerica(claveEntrada)
                ? "de." + colDetalleEntradaClaveEntrada + " = ? "
                : "CAST(de." + colDetalleEntradaClaveEntrada + " AS CHAR) = ? ";
        String sqlConteo = "SELECT LOWER(a." + colArticuloEstado + ") AS estado, COUNT(*) AS total " +
                "FROM articulo a JOIN detalle_Entrada de ON de." + colDetalleEntradaId + " = a." +
                colArticuloDetalleEntrada + " WHERE " + filtroClaveEntrada +
                "GROUP BY LOWER(a." + colArticuloEstado + ")";

        String sqlConteoDetalle = "SELECT LOWER(da." + colDetalleArticuloEstado + ") AS estado, COUNT(*) AS total " +
                "FROM detalleArticulo da " +
                "JOIN articulo a ON a." + colArticuloId + " = da." + colDetalleArticuloArticulo + " " +
                "JOIN detalle_Entrada de ON de." + colDetalleEntradaId + " = a." + colArticuloDetalleEntrada + " " +
                "WHERE " + filtroClaveEntrada +
                "GROUP BY LOWER(da." + colDetalleArticuloEstado + ")";

        int disponibles = 0;
        int pendientes = 0;
        int finalizados = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            setClaveEntradaParametro(ps, 1, claveEntrada);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    int total = rs.getInt("total");
                    if ("disponible".equalsIgnoreCase(estado)) {
                        disponibles += total;
                    } else if ("pendiente".equalsIgnoreCase(estado)) {
                        pendientes += total;
                    }
                }
            }
        }

        if (colDetalleArticuloEstado != null && colArticuloId != null && colDetalleArticuloArticulo != null) {
            try (PreparedStatement ps = conn.prepareStatement(sqlConteoDetalle)) {
                setClaveEntradaParametro(ps, 1, claveEntrada);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String estado = rs.getString("estado");
                        int total = rs.getInt("total");
                        if ("activo".equalsIgnoreCase(estado) || "disponible".equalsIgnoreCase(estado)) {
                            disponibles += total;
                        } else if ("pendiente".equalsIgnoreCase(estado)) {
                            pendientes += total;
                        } else if ("vendido".equalsIgnoreCase(estado) || "ajustado".equalsIgnoreCase(estado)
                                || "eliminado".equalsIgnoreCase(estado)) {
                            finalizados += total;
                        }
                    }
                }
            }
        }

        String nuevoEstado;
        if (disponibles > 0) {
            nuevoEstado = "disponible";
        } else if (pendientes > 0) {
            nuevoEstado = "pendiente";
        } else {
            nuevoEstado = "finalizado";
        }

        if (disponibles == 0 && pendientes == 0 && finalizados > 0 && colDetalleEntradaEstado != null) {
            String sqlUpdateDetalle = "UPDATE detalle_Entrada SET " + colDetalleEntradaEstado + " = ? WHERE "
                    + colDetalleEntradaId + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateDetalle)) {
                ps.setString(1, "pendiente");
                ps.setInt(2, detalleEntradaId);
                ps.executeUpdate();
            }
        }

        if (esClaveEntradaNumerica(claveEntrada)) {
            if (colEntradaId == null || colEntradaEstado == null) {
                return;
            }
            String sqlUpdate = "UPDATE entradas SET " + colEntradaEstado + " = ? WHERE " + colEntradaId + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, nuevoEstado);
                ps.setInt(2, Integer.parseInt(claveEntrada));
                ps.executeUpdate();
            }
        } else {
            if (colAjusteId == null || colAjusteEstado == null) {
                return;
            }
            String sqlUpdate = "UPDATE ajuste_inventario SET " + colAjusteEstado + " = ? WHERE CAST("
                    + colAjusteId + " AS CHAR) = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, nuevoEstado);
                ps.setString(2, claveEntrada);
                ps.executeUpdate();
            }
        }
    }

    private String obtenerClaveEntrada(Connection conn, int detalleEntradaId, String colDetalleEntradaId,
                                       String colDetalleEntradaClaveEntrada) throws SQLException {
        String sql = "SELECT " + colDetalleEntradaClaveEntrada + " AS claveEntrada FROM detalle_Entrada WHERE "
                + colDetalleEntradaId + " = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String valor = rs.getString("claveEntrada");
                    if (valor != null) {
                        valor = valor.trim();
                    }
                    return (valor == null || valor.isBlank()) ? null : valor;
                }
            }
        }
        return null;
    }

    private boolean esClaveEntradaNumerica(String claveEntrada) {
        return claveEntrada != null && claveEntrada.matches("\\d+");
    }

    private void setClaveEntradaParametro(PreparedStatement ps, int index, String claveEntrada) throws SQLException {
        if (esClaveEntradaNumerica(claveEntrada)) {
            ps.setInt(index, Integer.parseInt(claveEntrada));
        } else {
            ps.setString(index, claveEntrada);
        }
    }
}
