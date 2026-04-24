package Operaciones.traspasoSalida.model;

import Operaciones.compra.model.UbicacionCompra;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TraspasoSalidaBorradorDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sucursalNombre;
    private String sucursalId;
    private String comentario;
    private List<ItemTraspasoPlano> items;

    // Getters y setters
    public String getSucursalNombre() { return sucursalNombre; }
    public void setSucursalNombre(String sucursalNombre) { this.sucursalNombre = sucursalNombre; }

    public String getSucursalId() { return sucursalId; }
    public void setSucursalId(String sucursalId) { this.sucursalId = sucursalId; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public List<ItemTraspasoPlano> getItems() { return items; }
    public void setItems(List<ItemTraspasoPlano> items) { this.items = items; }

    public static class ItemTraspasoPlano implements Serializable {
        private static final long serialVersionUID = 1L;

        private String claveProducto;
        private String producto;
        private String descripcion;
        private String lote;
        private String caducidad;
        private int cantidad;
        private String presentacion;
        private int factor;
        private String nota;
        private String precioEntrada;
        private String precioIva;
        private String precioBruto;
        private String precioTotal;
        private List<UbicacionPlano> ubicaciones;

        // Getters y setters
        public String getClaveProducto() { return claveProducto; }
        public void setClaveProducto(String claveProducto) { this.claveProducto = claveProducto; }

        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public String getLote() { return lote; }
        public void setLote(String lote) { this.lote = lote; }

        public String getCaducidad() { return caducidad; }
        public void setCaducidad(String caducidad) { this.caducidad = caducidad; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public String getPresentacion() { return presentacion; }
        public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

        public int getFactor() { return factor; }
        public void setFactor(int factor) { this.factor = factor; }

        public String getNota() { return nota; }
        public void setNota(String nota) { this.nota = nota; }

        public String getPrecioEntrada() { return precioEntrada; }
        public void setPrecioEntrada(String precioEntrada) { this.precioEntrada = precioEntrada; }

        public String getPrecioIva() { return precioIva; }
        public void setPrecioIva(String precioIva) { this.precioIva = precioIva; }

        public String getPrecioBruto() { return precioBruto; }
        public void setPrecioBruto(String precioBruto) { this.precioBruto = precioBruto; }

        public String getPrecioTotal() { return precioTotal; }
        public void setPrecioTotal(String precioTotal) { this.precioTotal = precioTotal; }

        public List<UbicacionPlano> getUbicaciones() { return ubicaciones; }
        public void setUbicaciones(List<UbicacionPlano> ubicaciones) { this.ubicaciones = ubicaciones; }
    }

    public static class UbicacionPlano implements Serializable {
        private String ubicacion;
        private int cantidad;

        public UbicacionPlano() {}
        public UbicacionPlano(String ubicacion, int cantidad) {
            this.ubicacion = ubicacion;
            this.cantidad = cantidad;
        }

        public String getUbicacion() { return ubicacion; }
        public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    }
}