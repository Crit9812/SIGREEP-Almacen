package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class modelMarca {

    private static final String ESTADO_ACTIVO = "activo";

    private final GenericDAO<marca> marcaDAO;

    public modelMarca() {
        this.marcaDAO = new GenericDAO<>(marca.class);
    }

    public ArrayList<marca> obtenerTodas() {
        return filtrarActivos(marcaDAO.obtenerTodos());
    }

    public ArrayList<marca> obtenerTodasIncluyendoInactivas() {
        return marcaDAO.obtenerTodos();
    }

    public Map<String, String> obtenerMapaMarcas() {
        Map<String, String> mapa = new HashMap<>();
        for (marca marca : obtenerTodas()) {
            mapa.put(marca.getId(), marca.getNombre());
        }

        return mapa;
    }

    public marca buscarPorId(String id) {
        return marcaDAO.buscarExacto("id", id);
    }

    public boolean insertarMarca(marca marca) {
        return marcaDAO.insertar(marca);
    }

    public boolean actualizarMarca(marca marca) {
        return marcaDAO.actualizar(marca);
    }

    public boolean eliminarMarca(String id) {
        marca existente = buscarPorId(id);
        if (existente == null) {
            return false;
        }
        existente.setEstado("desactivado");
        return marcaDAO.actualizar(existente);
    }

    public marca buscarPorNombre(String nombre) {
        for (marca m : obtenerTodasIncluyendoInactivas()) {
            if (m != null && m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre)) {
                return m;
            }
        }
        return null;
    }

    private ArrayList<marca> filtrarActivos(ArrayList<marca> todas) {
        ArrayList<marca> activas = new ArrayList<>();
        for (marca m : todas) {
            if (m != null && ESTADO_ACTIVO.equalsIgnoreCase(m.getEstado())) {
                activas.add(m);
            }
        }
        return activas;
    }
}
