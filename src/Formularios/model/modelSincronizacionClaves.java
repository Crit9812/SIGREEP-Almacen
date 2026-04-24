package Formularios.model;

import conexion.Conexion;

import java.sql.*;
import java.util.*;

/**
 * Model para sincronización de claves.
 * TABLAS:
 *  - proveedores (id INT, Nombre VARCHAR)
 *  - productos   (id VARCHAR(50), nombre, marca, etiqueta, material, unidadMedida, descripcion)
 *  - claves      (idAlterno VARCHAR(11) PK, idProveedor INT, idProducto VARCHAR)
 */
public class modelSincronizacionClaves {

    private static final String TABLA_PROVEEDORES = "proveedores";
    private static final String COL_PROVEEDOR_ID = "id";
    private static final String COL_PROVEEDOR_NOMBRE = "Nombre";

    private static final String TABLA_PRODUCTOS = "productos";
    private static final String COL_PRODUCTO_ID = "id";
    private static final String COL_PRODUCTO_NOMBRE = "nombre";
    private static final String COL_PRODUCTO_MARCA = "marca";
    private static final String COL_PRODUCTO_ETIQUETA = "etiqueta";
    private static final String COL_PRODUCTO_MATERIAL = "material";
    private static final String COL_PRODUCTO_UNIDAD = "unidadMedida";
    private static final String COL_PRODUCTO_DESC = "descripcion";

    private static final String TABLA_CLAVES = "claves";

    // nombres de columnas
    private static final String COL_CLAVE_CATALOGO = "idAlterno";
    private static final String COL_CLAVE_PROVEEDOR = "idProveedor";
    private static final String COL_CLAVE_GREEP = "idProducto";

    /**
     * Devuelve lista de proveedores: cada Map contiene keys "id" (Integer) y "nombre" (String)
     */
    public List<Map<String, Object>> obtenerProveedores() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT `" + COL_PROVEEDOR_ID + "`, `" + COL_PROVEEDOR_NOMBRE + "` FROM " + TABLA_PROVEEDORES
                + " WHERE status = 'activo' ORDER BY `" + COL_PROVEEDOR_NOMBRE + "`";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt(COL_PROVEEDOR_ID));
                m.put("nombre", rs.getString(COL_PROVEEDOR_NOMBRE));
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Devuelve productos con los nombres de marca y etiqueta (cuando existan).
     * Cada Map incluye keys: "id","nombre","marca","etiqueta","material","unidad","descripcion"
     */
    public List<Map<String, Object>> obtenerProductos() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();

        String sql = "SELECT p.`" + COL_PRODUCTO_ID + "` AS pid, p.`" + COL_PRODUCTO_NOMBRE + "` AS pname, " +
                "m.nombre AS marca_name, e.nombre AS etiqueta_name, p.`" + COL_PRODUCTO_MATERIAL + "` AS material, " +
                "p.`" + COL_PRODUCTO_UNIDAD + "` AS unidad, p.`" + COL_PRODUCTO_DESC + "` AS descripcion, " +
                "p.`" + COL_PRODUCTO_MARCA + "` AS raw_marca, p.`" + COL_PRODUCTO_ETIQUETA + "` AS raw_etiqueta " +
                "FROM " + TABLA_PRODUCTOS + " p " +
                "LEFT JOIN marcas m ON m.id = p." + COL_PRODUCTO_MARCA + " " +
                "LEFT JOIN etiquetas e ON e.id = p." + COL_PRODUCTO_ETIQUETA + " " +
                "WHERE p.estado = 'activo'";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                String id = rs.getString("pid");
                String nombre = rs.getString("pname");
                String marcaName = rs.getString("marca_name");
                String etiquetaName = rs.getString("etiqueta_name");
                String material = rs.getString("material");
                String unidad = rs.getString("unidad");
                String descripcion = rs.getString("descripcion");

                String rawMarca = rs.getString("raw_marca");
                String rawEtiqueta = rs.getString("raw_etiqueta");

                if ((marcaName == null || marcaName.isEmpty()) && rawMarca != null && !rawMarca.isBlank()) {
                    marcaName = rawMarca;
                }
                if ((etiquetaName == null || etiquetaName.isEmpty()) && rawEtiqueta != null && !rawEtiqueta.isBlank()) {
                    etiquetaName = rawEtiqueta;
                }

                m.put("id", id);
                m.put("nombre", nombre == null ? "" : nombre);
                m.put("marca", marcaName == null ? "" : marcaName);
                m.put("etiqueta", etiquetaName == null ? "" : etiquetaName);
                m.put("material", material == null ? "" : material);
                m.put("unidad", unidad == null ? "" : unidad);
                m.put("descripcion", descripcion == null ? "" : descripcion);
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Inserta o actualiza un registro en la tabla `claves`.
     *
     * Si originalId != null:
     *   - si originalId equals idAlterno => comportamiento normal de UPDATE sobre ese registro
     *   - si originalId != idAlterno => intentamos renombrar la PK:
     *         * si idAlterno ya existe -> devolvemos false (conflicto)
     *         * si no existe -> hacemos UPDATE SET idAlterno=?, idProveedor=?, idProducto=? WHERE idAlterno=originalId
     *
     * Si originalId == null:
     *   - si existe un registro con idAlterno -> UPDATE
     *   - si no existe -> INSERT
     *
     * Devuelve true si la operación tuvo efecto (INSERT/UPDATE), false en caso de conflicto o fallo.
     */
    public boolean guardarClave(String originalId, String idAlterno, Integer idProveedor, String idProducto) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            // Normalizamos valores (permitir idProveedor null)
            if (idProducto == null) idProducto = "";

            if (originalId != null && !originalId.equals(idAlterno)) {
                // El usuario cambió la clave primaria: comprobar conflicto y actualizar la PK
                String checkNew = "SELECT COUNT(1) FROM " + TABLA_CLAVES + " WHERE `" + COL_CLAVE_CATALOGO + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkNew)) {
                    ps.setString(1, idAlterno);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // El nuevo idAlterno ya existe -> conflicto
                            return false;
                        }
                    }
                }

                String upd = "UPDATE " + TABLA_CLAVES +
                        " SET `" + COL_CLAVE_CATALOGO + "` = ?, `" + COL_CLAVE_PROVEEDOR + "` = ?, `" + COL_CLAVE_GREEP + "` = ?" +
                        " WHERE `" + COL_CLAVE_CATALOGO + "` = ?";

                try (PreparedStatement ups = conn.prepareStatement(upd)) {
                    ups.setString(1, idAlterno);
                    if (idProveedor != null)
                        ups.setObject(2, idProveedor);
                    else
                        ups.setNull(2, Types.INTEGER);
                    ups.setString(3, idProducto);
                    ups.setString(4, originalId);

                    return ups.executeUpdate() > 0;
                }
            } else {
                // originalId == null OR originalId equals idAlterno -> comportamiento UPDATE/INSERT según existencia del idAlterno
                String check = "SELECT COUNT(1) FROM " + TABLA_CLAVES + " WHERE `" + COL_CLAVE_CATALOGO + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setString(1, idAlterno);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // UPDATE
                            String upd = "UPDATE " + TABLA_CLAVES +
                                    " SET `" + COL_CLAVE_PROVEEDOR + "` = ?, `" + COL_CLAVE_GREEP + "` = ?" +
                                    " WHERE `" + COL_CLAVE_CATALOGO + "` = ?";

                            try (PreparedStatement ups = conn.prepareStatement(upd)) {
                                if (idProveedor != null)
                                    ups.setObject(1, idProveedor);
                                else
                                    ups.setNull(1, Types.INTEGER);

                                ups.setString(2, idProducto);
                                ups.setString(3, idAlterno);

                                return ups.executeUpdate() > 0;
                            }
                        } else {
                            // INSERT
                            String ins = "INSERT INTO " + TABLA_CLAVES +
                                    " (`" + COL_CLAVE_CATALOGO + "`, `" + COL_CLAVE_PROVEEDOR + "`, `" + COL_CLAVE_GREEP + "`, `estado`) VALUES (?, ?, ?, ?)";

                            try (PreparedStatement insP = conn.prepareStatement(ins)) {
                                insP.setString(1, idAlterno);

                                if (idProveedor != null)
                                    insP.setObject(2, idProveedor);
                                else
                                    insP.setNull(2, Types.INTEGER);

                                insP.setString(3, idProducto);
                                insP.setString(4, "activo");

                                return insP.executeUpdate() > 0;
                            }
                        }
                    }
                }
            }
        }
    }

    public Map<String, String> obtenerDetalleClave(String idAlterno) throws SQLException {
        String sql = "SELECT ca.`" + COL_CLAVE_CATALOGO + "` AS clave, " +
                "p.`" + COL_PRODUCTO_NOMBRE + "` AS producto, " +
                "pr.`" + COL_PROVEEDOR_NOMBRE + "` AS proveedor " +
                "FROM " + TABLA_CLAVES + " ca " +
                "LEFT JOIN " + TABLA_PRODUCTOS + " p ON p.`" + COL_PRODUCTO_ID + "` = ca.`" + COL_CLAVE_GREEP + "` " +
                "LEFT JOIN " + TABLA_PROVEEDORES + " pr ON pr.`" + COL_PROVEEDOR_ID + "` = ca.`" + COL_CLAVE_PROVEEDOR + "` " +
                "WHERE ca.`" + COL_CLAVE_CATALOGO + "` = ? LIMIT 1";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAlterno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> out = new HashMap<>();
                    out.put("clave", rs.getString("clave"));
                    out.put("producto", rs.getString("producto"));
                    out.put("proveedor", rs.getString("proveedor"));
                    return out;
                }
            }
        }
        return null;
    }
}
