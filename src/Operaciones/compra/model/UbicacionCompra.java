package Operaciones.compra.model;

public class UbicacionCompra {
    private final String ubicacion;
    private final int cantidad;

    public UbicacionCompra(String ubicacion, int cantidad) {
        this.ubicacion = ubicacion;
        this.cantidad = cantidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public int getCantidad() {
        return cantidad;
    }
}
