package VentanaPrincipal.controller;

public interface Pausable {
    /**
     * @return el tipo de movimiento asociado a esta pantalla
     */
    MovimientoType getTipoMovimiento();

    /**
     * Guarda el estado actual y devuelve un objeto (DTO) con los datos.
     */
    Object guardarBorrador();

    /**
     * Restaura el estado a partir del borrador.
     */
    void cargarBorrador(Object borrador);
}