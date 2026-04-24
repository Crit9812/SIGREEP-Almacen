package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class modelEtiqueta {

    private static final String ESTADO_ACTIVO = "activo";

    private final GenericDAO<etiqueta> etiquetaDAO;

    public modelEtiqueta() {
        this.etiquetaDAO = new GenericDAO<>(etiqueta.class);
    }

    public ArrayList<etiqueta> obtenerTodas() {
        return filtrarActivos(etiquetaDAO.obtenerTodos());
    }

    public ArrayList<etiqueta> obtenerTodasIncluyendoInactivas() {
        return etiquetaDAO.obtenerTodos();
    }

    public Map<String, String> obtenerMapaEtiquetas() {
        Map<String, String> mapa = new HashMap<>();
        for (etiqueta etiqueta : obtenerTodas()) {
            mapa.put(etiqueta.getId(), etiqueta.getNombre());
        }

        return mapa;
    }

    public etiqueta buscarPorId(String id) {
        return etiquetaDAO.buscarExacto("id", id);
    }

    public boolean insertarEtiqueta(etiqueta etiqueta) {
        return etiquetaDAO.insertar(etiqueta);
    }

    public boolean actualizarEtiqueta(etiqueta etiqueta) {
        return etiquetaDAO.actualizar(etiqueta);
    }

    public boolean eliminarEtiqueta(String id) {
        etiqueta existente = buscarPorId(id);
        if (existente == null) {
            return false;
        }
        existente.setEstado("desactivado");
        return etiquetaDAO.actualizar(existente);
    }

    public etiqueta buscarPorNombre(String nombre) {
        for (etiqueta e : obtenerTodasIncluyendoInactivas()) {
            if (e != null && e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                return e;
            }
        }
        return null;
    }

    private ArrayList<etiqueta> filtrarActivos(ArrayList<etiqueta> todas) {
        ArrayList<etiqueta> activas = new ArrayList<>();
        for (etiqueta e : todas) {
            if (e != null && ESTADO_ACTIVO.equalsIgnoreCase(e.getEstado())) {
                activas.add(e);
            }
        }
        return activas;
    }
}
