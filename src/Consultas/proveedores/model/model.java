package Consultas.proveedores.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class model {

    private final GenericDAO<proveedores> dao = new GenericDAO<>(proveedores.class);

    public ObservableList<proveedores> obtener() {
        ArrayList<proveedores> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminar(int id) {
        proveedores proveedor = obtenerPorId(id);
        if (proveedor == null) {
            return false;
        }
        proveedor.setStatus("desactivado");
        return dao.actualizar(proveedor);
    }

    public proveedores obtenerPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<proveedores> buscarExacto(String nombre) {
        ArrayList<proveedores> lista = dao.buscarParcial("Nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public int contarEntradasNoCanceladas(int idProveedor) {
        String sql = """
                SELECT COUNT(*)
                FROM entradas
                WHERE idRemitente = ?
                  AND (Estado IS NULL OR LOWER(Estado) <> ?)
                """;
        return contarRegistrosConEstadoDiferente(sql, idProveedor, "cancelado");
    }

    public int contarClavesNoDesactivadas(int idProveedor) {
        String sql = """
                SELECT COUNT(*)
                FROM claves
                WHERE idProveedor = ?
                  AND (estado IS NULL OR LOWER(estado) <> ?)
                """;
        return contarRegistrosConEstadoDiferente(sql, idProveedor, "desactivado");
    }

    private int contarRegistrosConEstadoDiferente(String sql, int idProveedor, String estadoPermitido) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ps.setString(2, estadoPermitido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private ArrayList<proveedores> filtrarActivos(ArrayList<proveedores> lista) {
        ArrayList<proveedores> activos = new ArrayList<>();
        for (proveedores p : lista) {
            if (p != null && "activo".equalsIgnoreCase(p.getStatus())) {
                activos.add(p);
            }
        }
        return activos;
    }
}
