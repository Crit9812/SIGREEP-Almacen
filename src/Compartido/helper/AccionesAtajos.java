package Compartido.helper;

import VentanaPrincipal.controller.EnumVistas;

public interface AccionesAtajos {
    void salir();
    void actualizar();
    void enfocarBusqueda();
    void navegarA(String vista);
    void navegarA(EnumVistas vista);
    void pausar();
}