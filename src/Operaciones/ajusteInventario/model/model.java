package Operaciones.ajusteInventario.model;

import Compartido.sesion.SesionUsuario;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
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

    public String registrarAjuste(List<compra> entradas, List<traspasoSalida> salidas, String comentario) {
        boolean sinEntradas = entradas == null || entradas.isEmpty();
        boolean sinSalidas = salidas == null || salidas.isEmpty();
        if (sinEntradas && sinSalidas) {
            return null;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            BigDecimal totalNeto = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;

            // Calcular totales
            if (!sinEntradas) {
                for (compra item : entradas) {
                    BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
                    BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());

                    totalNeto = totalNeto.add(precioBruto);
                    totalGeneral = totalGeneral.add(precioTotal);

                }
            }

            if (!sinSalidas) {
                for (traspasoSalida item : salidas) {
                    BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());
                    BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());

                    totalNeto = totalNeto.subtract(precioBruto);
                    totalGeneral = totalGeneral.subtract(precioTotal);
                }
            }

            // Insertar ajuste principal
            String idAjuste = insertarAjuste(conn, totalNeto, totalGeneral, comentario);

            if (!sinEntradas) {
                registrarDetallesEntrada(conn, idAjuste, entradas);
            }

            if (!sinSalidas) {
                registrarDetallesSalida(conn, idAjuste, salidas);
            }

            conn.commit();
            return idAjuste;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String insertarAjuste(Connection conn, BigDecimal totalNeto, BigDecimal totalGeneral, String comentario)
            throws SQLException {
        // Obtener el último ID de ajuste
        String ultimoId = obtenerUltimoIdAjuste(conn);

        // Generar nuevo ID con "A" al final
        String nuevoId = generarNuevoIdAjuste(ultimoId);

        String sql = "INSERT INTO ajuste_inventario (idAjuste, idUsuario, fechaAjuste, horaAjuste, precioNeto, precioTotal, Nota, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoId);
            ps.setInt(2, SesionUsuario.getIdUsuario());
            ps.setString(3, LocalDate.now().toString());
            ps.setTime(4, Time.valueOf(LocalTime.now()));
            ps.setBigDecimal(5, totalNeto.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(6, totalGeneral.setScale(2, RoundingMode.HALF_UP));
            ps.setString(7, comentario != null ? comentario : "");
            ps.setString(8, "activo");
            ps.executeUpdate();

            return nuevoId;
        }
    }

    private void registrarDetallesEntrada(Connection conn, String idAjuste, List<compra> entradas) throws SQLException {
        for (compra item : entradas) {
            // Insertar detalle de entrada
            long idDetalleEntrada = insertarDetalleEntrada(conn, idAjuste, item);

            // Procesar ubicaciones
            Map<String, Integer> cantidadesPorUbicacion = new LinkedHashMap<>();
            for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                if (ubicacion == null || ubicacion.getUbicacion() == null) {
                    continue;
                }
                int cantidadUbicacion = Math.max(0, ubicacion.getCantidad());
                if (cantidadUbicacion == 0) {
                    continue;
                }
                cantidadesPorUbicacion.merge(ubicacion.getUbicacion().trim(), cantidadUbicacion, Integer::sum);
            }

            // Crear artículos por cada ubicación
            for (Map.Entry<String, Integer> entry : cantidadesPorUbicacion.entrySet()) {
                Integer ubicacionId = resolverUbicacionId(conn, entry.getKey());
                int cantidadUbicacion = entry.getValue();
                for (int i = 0; i < cantidadUbicacion; i++) {
                    insertarArticulo(conn, idDetalleEntrada, item, ubicacionId);
                }
            }
        }
    }

    private long insertarDetalleEntrada(Connection conn, String idAjuste, compra item) throws SQLException {
        // Primero obtener el valor numérico del ID de ajuste (remover la "A")
        long idAjusteNumerico = extraerNumeroDeIdAjuste(idAjuste);

        String sql = "INSERT INTO detalle_Entrada (claveEntrada, claveProducto, cantidad, precioUnitario, " +
                "precioIVA, precioBrutoTotal, precioTotal, Nota, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, idAjuste);
            ps.setString(2, item.getClaveProducto());
            ps.setInt(3, item.getCantidad());
            ps.setBigDecimal(4, parseDecimal(item.getPrecioEntrada()));
            ps.setBigDecimal(5, parseDecimal(item.getPrecioIva()));
            ps.setBigDecimal(6, parseDecimal(item.getPrecioBruto()));
            ps.setBigDecimal(7, parseDecimal(item.getPrecioTotal()));
            ps.setString(8, item.getNota());
            ps.setString(9, "activo");

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        return 0;
    }

    private void insertarArticulo(Connection conn, long idDetalleEntrada, compra item, Integer ubicacionId)
            throws SQLException {
        String sql = "INSERT INTO articulo (idDetalleEntrada, presentacion, factor, lote, caducidad, " +
                "ubicacion, segmentado, Estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idDetalleEntrada);
            ps.setString(2, item.getPresentacion());
            ps.setInt(3, parseInteger(item.getFactor()));
            ps.setString(4, item.getLote());
            ps.setDate(5, parseDate(item.getCaducidad()));
            ps.setInt(6, ubicacionId);
            ps.setInt(7, esSegmentado(item.getPresentacion()));
            ps.setString(8, "disponible");

            ps.executeUpdate();
        }
    }

    private void registrarDetallesSalida(Connection conn, String idAjuste, List<traspasoSalida> salidas) throws SQLException {
        for (traspasoSalida item : salidas) {
            // Insertar detalle de salida
            long idDetalleSalida = insertarDetalleSalida(conn, idAjuste, item);

            if (item.getUbicaciones().isEmpty()) {
                throw new SQLException("No hay ubicaciones para la salida.");
            }

            // Procesar cada ubicación
            for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                if (ubicacion == null || ubicacion.getUbicacion() == null) {
                    continue;
                }
                int cantidad = Math.max(0, ubicacion.getCantidad());
                if (cantidad == 0) {
                    continue;
                }

                Integer ubicacionId = resolverUbicacionId(conn, ubicacion.getUbicacion().trim());
                if (ubicacionId == null) {
                    throw new SQLException("No se encontró la ubicación.");
                }

                // Obtener detalle de entrada relacionado
                Integer detalleEntradaId = obtenerDetalleEntradaId(conn, item, ubicacionId);

                // Seleccionar artículos para actualizar/eliminar
                List<RegistroSalida> registrosParaActualizar = obtenerRegistrosParaSalida(
                        conn, item, ubicacionId, cantidad, detalleEntradaId
                );

                if (registrosParaActualizar.size() < cantidad) {
                    throw new SQLException("No hay suficientes artículos para la salida.");
                }

                // Actualizar o eliminar artículos
                actualizarRegistrosParaSalida(conn, idDetalleSalida, registrosParaActualizar);

                // Actualizar estado de la entrada si corresponde
                if (detalleEntradaId != null) {
                    actualizarEstadoEntradaPorDetalle(conn, detalleEntradaId);
                }
            }
        }
    }

    private long insertarDetalleSalida(Connection conn, String idAjuste, traspasoSalida item) throws SQLException {
        // Primero obtener el valor numérico del ID de ajuste (remover la "A")
        long idAjusteNumerico = extraerNumeroDeIdAjuste(idAjuste);

        // SOLO las columnas que existen en la tabla detalle_Salida
        String sql = "INSERT INTO detalle_Salida (claveSalida, claveProductoSalida, cantidad, precioUnitarioSalida, " +
                "precioIVASalida, precioBrutoTotalSalida, precioTotalSalida, Nota, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, idAjuste);
            ps.setString(2, item.getClaveProducto());
            ps.setInt(3, item.getCantidad());
            ps.setBigDecimal(4, parseDecimal(item.getPrecioEntrada()));
            ps.setBigDecimal(5, parseDecimal(item.getPrecioIva()));
            ps.setBigDecimal(6, parseDecimal(item.getPrecioBruto()));
            ps.setBigDecimal(7, parseDecimal(item.getPrecioTotal()));
            ps.setString(8, item.getNota());
            ps.setString(9, "activo");

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        return 0;
    }

    private Integer obtenerDetalleEntradaId(Connection conn, traspasoSalida item, int ubicacionId) throws SQLException {
        String sql = "SELECT a.idDetalleEntrada AS detalleEntrada " +
                "FROM articulo a " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE de.claveProducto = ? " +
                "AND a.lote = ? " +
                "AND a.presentacion = ? " +
                "AND a.factor = ? " +
                "AND a.ubicacion = ? " +
                "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getClaveProducto());
            ps.setString(2, item.getLote());
            ps.setString(3, item.getPresentacion());
            ps.setInt(4, item.getFactor());
            ps.setInt(5, ubicacionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("detalleEntrada");
                }
            }
        }

        if (!esPresentacionDetalle(item.getPresentacion(), item.getFactor())) {
            return null;
        }

        String sqlDetalle = "SELECT a.idDetalleEntrada AS detalleEntrada " +
                "FROM detalleArticulo da " +
                "JOIN articulo a ON a.idArticulo = da.idArticulo " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE de.claveProducto = ? " +
                "AND a.lote = ? " +
                "AND da.idUbicacion = ? " +
                "AND LOWER(da.estado) IN (?, ?) " +
                "AND LOWER(a.Estado) = ? " +
                "AND (da.idDetalleSalida IS NULL OR TRIM(CAST(da.idDetalleSalida AS CHAR)) = '' OR TRIM(CAST(da.idDetalleSalida AS CHAR)) = '0') " +
                "LIMIT 1";

        try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
            int index = 1;
            psDetalle.setString(index++, item.getClaveProducto());
            psDetalle.setString(index++, item.getLote());
            psDetalle.setInt(index++, ubicacionId);
            psDetalle.setString(index++, "activo");
            psDetalle.setString(index++, "disponible");
            psDetalle.setString(index++, "segmentado");
            try (ResultSet rs = psDetalle.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("detalleEntrada");
                }
            }
        }

        return null;
    }

    private List<RegistroSalida> obtenerRegistrosParaSalida(Connection conn, traspasoSalida item,
                                                            int ubicacionId, int cantidad, Integer detalleEntradaId)
            throws SQLException {
        List<RegistroSalida> registros = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.idArticulo AS idRegistro, 'ARTICULO' AS tipo " +
                        "FROM articulo a"
        );

        if (detalleEntradaId != null) {
            sql.append(" JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada");
        }

        sql.append(" WHERE 1=1");
        sql.append(" AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)");
        sql.append(" AND a.lote = ?");
        sql.append(" AND a.ubicacion = ?");
        sql.append(" AND a.presentacion = ?");
        sql.append(" AND a.factor = ?");
        sql.append(" AND LOWER(a.Estado) = ?");

        if (detalleEntradaId != null) {
            sql.append(" AND de.claveProducto = ?");
        }

        sql.append(" LIMIT ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, item.getLote());
            ps.setInt(index++, ubicacionId);
            ps.setString(index++, item.getPresentacion());
            ps.setInt(index++, item.getFactor());
            ps.setString(index++, "disponible");

            if (detalleEntradaId != null) {
                ps.setString(index++, item.getClaveProducto());
            }

            ps.setInt(index, cantidad);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroSalida(rs.getString("idRegistro"), TipoRegistro.ARTICULO));
                }
            }
        }

        if (!esPresentacionDetalle(item.getPresentacion(), item.getFactor()) || registros.size() >= cantidad) {
            return registros;
        }

        int restantes = cantidad - registros.size();
        StringBuilder sqlDetalle = new StringBuilder(
                "SELECT da.idDetalle AS idRegistro, 'DETALLE' AS tipo " +
                        "FROM detalleArticulo da " +
                        "JOIN articulo a ON a.idArticulo = da.idArticulo"
        );
        if (detalleEntradaId != null) {
            sqlDetalle.append(" JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada");
        }
        sqlDetalle.append(" WHERE 1=1");
        sqlDetalle.append(" AND (da.idDetalleSalida IS NULL OR TRIM(CAST(da.idDetalleSalida AS CHAR)) = '' OR TRIM(CAST(da.idDetalleSalida AS CHAR)) = '0')");
        sqlDetalle.append(" AND da.idUbicacion = ?");
        sqlDetalle.append(" AND a.lote = ?");
        sqlDetalle.append(" AND LOWER(da.estado) IN (?, ?)");
        sqlDetalle.append(" AND LOWER(a.Estado) = ?");
        if (detalleEntradaId != null) {
            sqlDetalle.append(" AND de.claveProducto = ?");
        }
        sqlDetalle.append(" LIMIT ?");

        try (PreparedStatement ps = conn.prepareStatement(sqlDetalle.toString())) {
            int index = 1;
            ps.setInt(index++, ubicacionId);
            ps.setString(index++, item.getLote());
            ps.setString(index++, "activo");
            ps.setString(index++, "disponible");
            ps.setString(index++, "segmentado");
            if (detalleEntradaId != null) {
                ps.setString(index++, item.getClaveProducto());
            }
            ps.setInt(index, restantes);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroSalida(rs.getString("idRegistro"), TipoRegistro.DETALLE));
                }
            }
        }

        return registros;
    }

    private void actualizarRegistrosParaSalida(Connection conn, long idDetalleSalida, List<RegistroSalida> registros)
            throws SQLException {
        if (registros.isEmpty()) {
            return;
        }

        List<String> articulosIds = new ArrayList<>();
        List<String> detalleIds = new ArrayList<>();
        for (RegistroSalida registro : registros) {
            if (registro.tipo == TipoRegistro.ARTICULO) {
                articulosIds.add(registro.id);
            } else {
                detalleIds.add(registro.id);
            }
        }

        if (!articulosIds.isEmpty()) {
            String placeholders = String.join(", ", java.util.Collections.nCopies(articulosIds.size(), "?"));
            String sql = "UPDATE articulo SET idDetalleSalida = ?, Estado = ? WHERE idArticulo IN (" + placeholders + ")";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setLong(index++, idDetalleSalida);
                ps.setString(index++, "ajustado");

                for (String idArticulo : articulosIds) {
                    ps.setInt(index++, Integer.parseInt(idArticulo));
                }

                int actualizadas = ps.executeUpdate();
                if (actualizadas < articulosIds.size()) {
                    throw new SQLException("No se actualizaron todos los artículos.");
                }
            }
        }

        if (!detalleIds.isEmpty()) {
            String placeholders = String.join(", ", java.util.Collections.nCopies(detalleIds.size(), "?"));
            String sql = "UPDATE detalleArticulo SET idDetalleSalida = ?, estado = ? WHERE idDetalle IN ("
                    + placeholders + ")";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setLong(index++, idDetalleSalida);
                ps.setString(index++, "eliminado");

                for (String idDetalle : detalleIds) {
                    ps.setString(index++, idDetalle);
                }

                int actualizadas = ps.executeUpdate();
                if (actualizadas < detalleIds.size()) {
                    throw new SQLException("No se actualizaron todos los detalles.");
                }
            }
        }
    }

    private boolean esPresentacionDetalle(String presentacion, int factor) {
        if (presentacion == null) {
            return false;
        }
        String normalizada = presentacion.trim().toLowerCase();
        return ("pz".equals(normalizada) || "pieza".equals(normalizada)) && factor == 1;
    }

    private enum TipoRegistro {
        ARTICULO,
        DETALLE
    }

    private static class RegistroSalida {
        private final String id;
        private final TipoRegistro tipo;

        private RegistroSalida(String id, TipoRegistro tipo) {
            this.id = id;
            this.tipo = tipo;
        }
    }

    private void actualizarEstadoEntradaPorDetalle(Connection conn, int detalleEntradaId) throws SQLException {
        // 1. Obtener claveEntrada como STRING
        String sqlClaveEntrada = "SELECT claveEntrada FROM detalle_Entrada WHERE idDetalleEntrada = ? LIMIT 1";
        String claveEntradaStr = null;

        try (PreparedStatement ps = conn.prepareStatement(sqlClaveEntrada)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveEntradaStr = rs.getString("claveEntrada");
                }
            }
        }

        if (claveEntradaStr == null) {
            return;
        }

        // 2. Determinar si es ajuste (tiene "A") o compra (numérico)
        boolean esAjuste = claveEntradaStr.toUpperCase().endsWith("A");

        // 3. Contar artículos y detalles segmentados por estado de toda la entrada
        String sqlConteo = "SELECT LOWER(a.Estado) AS estado, COUNT(*) AS total " +
                "FROM articulo a " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE de.claveEntrada = ? " +
                "GROUP BY LOWER(a.Estado)";

        String sqlConteoDetalle = "SELECT LOWER(da.estado) AS estado, COUNT(*) AS total " +
                "FROM detalleArticulo da " +
                "JOIN articulo a ON a.idArticulo = da.idArticulo " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE de.claveEntrada = ? " +
                "GROUP BY LOWER(da.estado)";

        int disponibles = 0;
        int pendientes = 0;
        int vendidos = 0;
        int ajustados = 0;

        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            ps.setString(1, claveEntradaStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    int total = rs.getInt("total");
                    if ("disponible".equalsIgnoreCase(estado)) {
                        disponibles += total;
                    } else if ("pendiente".equalsIgnoreCase(estado)) {
                        pendientes += total;
                    } else if ("vendido".equalsIgnoreCase(estado)) {
                        vendidos += total;
                    } else if ("ajustado".equalsIgnoreCase(estado)) {
                        ajustados += total;
                    }
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlConteoDetalle)) {
            ps.setString(1, claveEntradaStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    int total = rs.getInt("total");
                    if ("activo".equalsIgnoreCase(estado) || "disponible".equalsIgnoreCase(estado)) {
                        disponibles += total;
                    } else if ("pendiente".equalsIgnoreCase(estado)) {
                        pendientes += total;
                    } else if ("vendido".equalsIgnoreCase(estado)) {
                        vendidos += total;
                    } else if ("ajustado".equalsIgnoreCase(estado) || "eliminado".equalsIgnoreCase(estado)) {
                        ajustados += total;
                    }
                }
            }
        }

        // 4. Determinar nuevo estado
        String nuevoEstado;
        if (disponibles > 0) {
            nuevoEstado = "disponible";
        } else if (pendientes > 0) {
            nuevoEstado = "pendiente";
        } else if (vendidos > 0) {
            nuevoEstado = "finalizado";
        } else if (ajustados > 0) {
            nuevoEstado = "finalizado";
        } else {
            nuevoEstado = "finalizado";
        }

        // 5. Actualizar tabla correspondiente
        if (esAjuste) {
            // Actualizar ajuste_inventario
            String sqlUpdate = "UPDATE ajuste_inventario SET estado = ? WHERE idAjuste = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, nuevoEstado);
                ps.setString(2, claveEntradaStr);
                ps.executeUpdate();
            }
        } else {
            // Actualizar entradas (compra/traspaso)
            try {
                int claveEntradaNum = Integer.parseInt(claveEntradaStr);
                String sqlUpdate = "UPDATE entradas SET Estado = ? WHERE idEntrada = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, nuevoEstado);
                    ps.setInt(2, claveEntradaNum);
                    ps.executeUpdate();
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: claveEntrada no es numérica ni ajuste: " + claveEntradaStr);
            }
        }

        // 6. Actualizar estado del detalle_Entrada específico
        String estadoDetalleEntrada = "activo"; // Estado por defecto
        if (disponibles == 0 && pendientes == 0) {
            // Si no hay artículos disponibles ni pendientes
            estadoDetalleEntrada = "desactivado";
        }

        // Actualizar detalle_Entrada
        String sqlUpdateDetalle = "UPDATE detalle_Entrada SET estado = ? WHERE idDetalleEntrada = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdateDetalle)) {
            ps.setString(1, estadoDetalleEntrada);
            ps.setInt(2, detalleEntradaId);
            ps.executeUpdate();
        }
    }

    // Métodos auxiliares para manejar IDs de ajuste con "A" - CORREGIDOS
    private String obtenerUltimoIdAjuste(Connection conn) throws SQLException {
        // Obtener TODOS los IDs y encontrar el máximo NUMÉRICO
        String sql = "SELECT idAjuste FROM ajuste_inventario";

        int maxNumero = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("idAjuste");
                if (id != null && !id.isEmpty()) {
                    // Extraer el número del ID
                    String numeroStr = id.replaceAll("[^0-9]", "");
                    if (!numeroStr.isEmpty()) {
                        try {
                            int numero = Integer.parseInt(numeroStr);
                            if (numero > maxNumero) {
                                maxNumero = numero;
                            }
                        } catch (NumberFormatException e) {
                            // Ignorar IDs mal formados
                        }
                    }
                }
            }
        }

        if (maxNumero > 0) {
            return maxNumero + "A";
        }
        return null;
    }

    private String generarNuevoIdAjuste(String ultimoId) {
        if (ultimoId == null || ultimoId.isEmpty()) {
            return "1A"; // Primer ID
        }

        try {
            // Extraer solo los dígitos numéricos
            String numeroStr = ultimoId.replaceAll("[^0-9]", "");
            if (numeroStr.isEmpty()) {
                return "1A";
            }
            int numero = Integer.parseInt(numeroStr);
            int nuevoNumero = numero + 1;
            return nuevoNumero + "A";
        } catch (NumberFormatException e) {
            return "1A"; // En caso de error, empezar desde 1A
        }
    }

    private long extraerNumeroDeIdAjuste(String idAjuste) {
        if (idAjuste == null || idAjuste.isEmpty()) {
            return 0;
        }

        try {
            // Extraer solo los dígitos numéricos (remover la "A")
            String numeroStr = idAjuste.replaceAll("[^0-9]", "");
            if (numeroStr.isEmpty()) {
                return 0;
            }
            return Long.parseLong(numeroStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Métodos auxiliares originales (se mantienen igual)
    private BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpio = valor.trim().replace(",", "");
        try {
            return new BigDecimal(limpio);
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

    private int esSegmentado(String presentacion) {
        return 0;
    }

}
