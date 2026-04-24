package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> claveDAO;
    private final dao clavesDAO;

    public model() {
        this.claveDAO = new GenericDAO<>(claves.class);
        this.clavesDAO = new dao();
    }

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = claveDAO.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        String sql = "UPDATE claves SET estado = 'desactivado' WHERE idAlterno = ?";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int contarEntradasPorClave(String idAlterno) {
        String sql = """
                SELECT COUNT(*)
                FROM detalle_Entrada de
                INNER JOIN entradas e ON e.idEntrada = de.claveEntrada
                WHERE de.claveProducto = ?
                  AND LOWER(e.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idAlterno);
    }

    public int contarSalidasPorClave(String idAlterno) {
        String sql = """
                SELECT COUNT(*)
                FROM detalle_Salida ds
                INNER JOIN salidas s ON s.idSalida = ds.claveSalida
                WHERE ds.claveProductoSalida = ?
                  AND LOWER(s.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idAlterno);
    }

    private int contarRegistrosConEstados(String sql, String idAlterno) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAlterno);
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

    // --- USANDO DAO ESPECÍFICO ---

    public ObservableList<String[]> obtenerParaTabla() {
        return FXCollections.observableArrayList(
                clavesDAO.obtenerClavesCompletas()
        );
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }

        return FXCollections.observableArrayList(
                clavesDAO.buscarClavesCompletas(textoBusqueda.trim())
        );
    }
}
