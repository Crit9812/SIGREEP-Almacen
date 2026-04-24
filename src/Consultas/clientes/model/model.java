package Consultas.clientes.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class model {

    private final GenericDAO<cliente> dao = new GenericDAO<>(cliente.class);
    private static final String ESTADO_CANCELADO = "cancelado";

    public ObservableList<cliente> obtenerClientes() {
        ArrayList<cliente> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminarCliente(int idCliente) {
        cliente cliente = obtenerClientePorId(idCliente);
        if (cliente == null) {
            return false;
        }
        cliente.setStatus("desactivado");
        return dao.actualizar(cliente);
    }

    public cliente obtenerClientePorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<cliente> buscarExacto(String nombre) {
        ArrayList<cliente> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public int contarSalidasNoCanceladasPorCliente(int idCliente) {
        String sql = """
                SELECT COUNT(*)
                FROM salidas s
                WHERE s.idDestinatario = ?
                  AND LOWER(s.tipoSalida) = 'venta'
                  AND COALESCE(LOWER(s.Estado), '') <> ?
                """;
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, ESTADO_CANCELADO);
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

    private ArrayList<cliente> filtrarActivos(ArrayList<cliente> lista) {
        ArrayList<cliente> activos = new ArrayList<>();
        for (cliente c : lista) {
            if (c != null && "activo".equalsIgnoreCase(c.getStatus())) {
                activos.add(c);
            }
        }
        return activos;
    }
}
