package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import Operaciones.registrarUsuario.model.usuario;

public class modelNuevoUsuario {

    private final GenericDAO<usuario> dao = new GenericDAO<>(usuario.class);

    /**
     * Verifica existencia por userName (nombre de usuario)
     */
    public boolean validarUsuarioExistente(String userName) {
        usuario u = dao.buscarExacto("userName", userName);
        return u != null;
    }

    /**
     * Inserta un usuario. Deja idUsuario en null para que la BD la genere si es AUTO_INCREMENT.
     */
    public boolean insertarUsuario(usuario u) {
        // no forzar idUsuario: aseguramos que esté null para evitar intento de insertar texto en int PK
        u.setIdUsuario(null);
        return dao.insertar(u);
    }

    public boolean actualizarUsuario(usuario u) {
        return dao.actualizar(u);
    }
}
