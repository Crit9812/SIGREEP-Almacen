package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.proveedores.model.proveedores;

public class modelNuevoProveedor {

    private final GenericDAO<proveedores> proveedorDAO;

    public modelNuevoProveedor() {
        proveedorDAO = new GenericDAO<>(proveedores.class);
    }

    public boolean agregarProveedor(proveedores p) {
        return proveedorDAO.insertar(p);
    }

    public boolean modificarProveedor(proveedores p) {
        return proveedorDAO.actualizar(p);
    }
}
