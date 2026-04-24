package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.clientes.model.cliente;

public class modelNuevoCliente {

    private final GenericDAO<cliente> clienteDAO;

    public modelNuevoCliente() {
        clienteDAO = new GenericDAO<>(cliente.class);
    }

    public boolean agregarCliente(cliente c) {
        return clienteDAO.insertar(c);
    }

    public boolean modificarCliente(cliente c) {
        return clienteDAO.actualizar(c);
    }
}
