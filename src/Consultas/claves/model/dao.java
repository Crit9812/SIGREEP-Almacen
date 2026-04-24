package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class dao {

    public List<String[]> obtenerClavesCompletas() {
        List<String[]> resultados = new ArrayList<>();
        String sql = """
            SELECT ca.idAlterno,
                   ca.idProducto,
                   p.nombre AS producto,
                   ca.idProveedor,
                   pr.Nombre AS proveedor
            FROM claves ca
            LEFT JOIN productos p ON p.id = ca.idProducto
            LEFT JOIN proveedores pr ON pr.id = ca.idProveedor
            WHERE ca.estado = 'activo'
            ORDER BY ca.idAlterno
            """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String idProducto = rs.getString("idProducto");
                String descripcion = obtenerDescripcionProducto(idProducto);

                resultados.add(new String[]{
                        rs.getString("idAlterno"),
                        idProducto,
                        rs.getString("producto"),
                        rs.getString("idProveedor"),
                        rs.getString("proveedor"),
                        descripcion
                });
            }
        } catch (Exception e) {
            System.out.println("Error en obtenerClavesCompletas: " + e.getMessage());
        }
        return resultados;
    }

    public List<String[]> buscarClavesCompletas(String textoBusqueda) {
        List<String[]> resultados = new ArrayList<>();
        String sql = """
            SELECT ca.idAlterno,
                   ca.idProducto,
                   p.nombre AS producto,
                   ca.idProveedor,
                   pr.Nombre AS proveedor
            FROM claves ca
            LEFT JOIN productos p ON p.id = ca.idProducto
            LEFT JOIN proveedores pr ON pr.id = ca.idProveedor
            WHERE ca.estado = 'activo'
              AND (
                    ca.idAlterno LIKE ?
                    OR ca.idProducto LIKE ?
                    OR p.nombre LIKE ?
                    OR pr.Nombre LIKE ?
              )
            ORDER BY ca.idAlterno
            """;
        String busqueda = "%" + textoBusqueda + "%";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, busqueda);
            ps.setString(2, busqueda);
            ps.setString(3, busqueda);
            ps.setString(4, busqueda);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idProducto = rs.getString("idProducto");
                    String descripcion = obtenerDescripcionProducto(idProducto);

                    resultados.add(new String[]{
                            rs.getString("idAlterno"),
                            idProducto,
                            rs.getString("producto"),
                            rs.getString("idProveedor"),
                            rs.getString("proveedor"),
                            descripcion
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("Error en buscarClavesCompletas: " + e.getMessage());
        }
        return resultados;
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
            System.out.println("Error al obtener descripción del producto " + idProducto + ": " + e.getMessage());
            return "";
        }
    }

}
