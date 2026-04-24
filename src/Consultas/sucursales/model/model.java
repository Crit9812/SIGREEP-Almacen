package Consultas.sucursales.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class model {

    private final GenericDAO<sucursal> dao = new GenericDAO<>(sucursal.class);

    public ObservableList<sucursal> obtenerSucursales() {
        ArrayList<sucursal> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    private ArrayList<sucursal> filtrarActivos(ArrayList<sucursal> lista) {
        ArrayList<sucursal> activos = new ArrayList<>();
        for (sucursal s : lista) {
            if (s != null && "activo".equalsIgnoreCase(s.getStatus())) {
                activos.add(s);
            }
        }
        return activos;
    }

    public boolean eliminarSucursal(int idSucursal) {
        sucursal sucursal = obtenerSucursalPorId(idSucursal);
        if (sucursal == null) {
            return false;
        }
        sucursal.setStatus("desactivado");
        return dao.actualizar(sucursal);
    }

    public sucursal obtenerSucursalPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<sucursal> buscarExacto(String nombre) {
        ArrayList<sucursal> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    // Metodos para verificar si las sucursales tienen registros anteriores para no borrarlas

    public int contarEntradasPorSucursal(int idSucursal) {
        String sql = """
                SELECT COUNT(*)
                FROM entradas
                WHERE idRemitente = ?
                  AND LOWER(Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idSucursal);
    }

    public int contarSalidasPorSucursal(int idSucursal) {
        String sql = """
                SELECT COUNT(*)
                FROM salidas
                WHERE idDestinatario = ?
                  AND LOWER(Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idSucursal);
    }

    private int contarRegistrosConEstados(String sql, int idSucursal) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSucursal);
            ps.setString(2, "activo");
            ps.setString(3, "pendiente");
            ps.setString(4, "disponible");
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

}
