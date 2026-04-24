package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.sucursales.model.sucursal;

public class modelNuevaSucursal {

    private final GenericDAO<sucursal> sucursalDAO;

    public modelNuevaSucursal() {
        sucursalDAO = new GenericDAO<>(sucursal.class);
    }

    public boolean guardarSucursal(sucursal s) {
        return sucursalDAO.insertar(s);
    }

    public boolean modificarSucursal(sucursal s) {
        return sucursalDAO.actualizar(s);
    }

}
