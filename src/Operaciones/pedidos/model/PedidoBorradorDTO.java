package Operaciones.pedidos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PedidoBorradorDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ItemPedidoPlano> items;

    public List<ItemPedidoPlano> getItems() {
        return items;
    }

    public void setItems(List<ItemPedidoPlano> items) {
        this.items = items;
    }

    public static class ItemPedidoPlano implements Serializable {
        private static final long serialVersionUID = 1L;

        private String claveProducto;
        private String producto;
        private String descripcion;
        private int cantidad;
        private String claveAlterna;
        private String presentacion;
        private String factor;

        // Getters y setters
        public String getClaveProducto() { return claveProducto; }
        public void setClaveProducto(String claveProducto) { this.claveProducto = claveProducto; }

        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public String getClaveAlterna() { return claveAlterna; }
        public void setClaveAlterna(String claveAlterna) { this.claveAlterna = claveAlterna; }

        public String getPresentacion() { return presentacion; }
        public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

        public String getFactor() { return factor; }
        public void setFactor(String factor) { this.factor = factor; }
    }
}