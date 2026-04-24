package Operaciones.registrarUsuario.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;

public class model {

    private GenericDAO<usuario> dao = new GenericDAO<>(usuario.class);

    public ArrayList<usuario> obtenerUsuarios() {
        ArrayList<usuario> lista = dao.obtenerTodos();
        ArrayList<usuario> activos = new ArrayList<>();
        for (usuario u : lista) {
            if (u != null && "activo".equalsIgnoreCase(u.getEstado())) {
                activos.add(u);
            }
        }
        return activos;
    }

    public boolean eliminarUsuario(String id) {
        usuario user = dao.buscarExacto("idUsuario", id);
        if (user == null) {
            return false;
        }
        user.setEstado("desactivado");
        return dao.actualizar(user);
    }

    public boolean insertarUsuario(usuario u) {
        return dao.insertar(u);
    }

    public boolean actualizarUsuario(usuario u) {
        return dao.actualizar(u);
    }
}
