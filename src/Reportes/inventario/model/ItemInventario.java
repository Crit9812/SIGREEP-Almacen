package Reportes.inventario.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemInventario {
    private final StringProperty idArticulo = new SimpleStringProperty();
    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty cantidad = new SimpleStringProperty();
    private final StringProperty producto = new SimpleStringProperty();
    private final StringProperty marca = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty material = new SimpleStringProperty();
    private final StringProperty unidadMedida = new SimpleStringProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final StringProperty factor = new SimpleStringProperty();
    private final StringProperty lote = new SimpleStringProperty();
    private final StringProperty caducidad = new SimpleStringProperty();
    private final StringProperty ubicacion = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty precioTotal = new SimpleStringProperty();
    private final StringProperty precioTotalIva = new SimpleStringProperty();
    private final StringProperty inventarioMinimo = new SimpleStringProperty();

    public ItemInventario(String idArticulo, String claveProducto, String cantidad, String producto, String marca, String categoria,
                          String material, String unidadMedida, String presentacion, String factor,
                          String lote, String caducidad, String ubicacion,
                          String descripcion, String precioTotal, String precioTotalIva, String inventarioMinimo) {
        this.idArticulo.set(idArticulo);
        this.claveProducto.set(claveProducto);
        this.cantidad.set(cantidad);
        this.producto.set(producto);
        this.marca.set(marca);
        this.categoria.set(categoria);
        this.material.set(material);
        this.unidadMedida.set(unidadMedida);
        this.presentacion.set(presentacion);
        this.factor.set(factor);
        this.lote.set(lote);
        this.caducidad.set(caducidad);
        this.ubicacion.set(ubicacion);
        this.descripcion.set(descripcion);
        this.precioTotal.set(precioTotal);
        this.precioTotalIva.set(precioTotalIva);
        this.inventarioMinimo.set(inventarioMinimo);
    }

    public String getIdArticulo() {
        return idArticulo.get();
    }

    public StringProperty idArticuloProperty() {
        return idArticulo;
    }

    public void setIdArticulo(String idArticulo) {
        this.idArticulo.set(idArticulo);
    }

    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public void setClaveProducto(String claveProducto) {
        this.claveProducto.set(claveProducto);
    }

    public String getCantidad() {
        return cantidad.get();
    }

    public StringProperty cantidadProperty() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad.set(cantidad);
    }

    public String getProducto() {
        return producto.get();
    }

    public StringProperty productoProperty() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto.set(producto);
    }

    public String getMarca() {
        return marca.get();
    }

    public StringProperty marcaProperty() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca.set(marca);
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria.set(categoria);
    }

    public String getMaterial() {
        return material.get();
    }

    public StringProperty materialProperty() {
        return material;
    }

    public void setMaterial(String material) {
        this.material.set(material);
    }

    public String getUnidadMedida() {
        return unidadMedida.get();
    }

    public StringProperty unidadMedidaProperty() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida.set(unidadMedida);
    }

    public String getPresentacion() {
        return presentacion.get();
    }

    public StringProperty presentacionProperty() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion.set(presentacion);
    }

    public String getFactor() {
        return factor.get();
    }

    public StringProperty factorProperty() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor.set(factor);
    }

    public String getLote() {
        return lote.get();
    }

    public StringProperty loteProperty() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote.set(lote);
    }

    public String getCaducidad() {
        return caducidad.get();
    }

    public StringProperty caducidadProperty() {
        return caducidad;
    }

    public void setCaducidad(String caducidad) {
        this.caducidad.set(caducidad);
    }

    public String getUbicacion() {
        return ubicacion.get();
    }

    public StringProperty ubicacionProperty() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion.set(ubicacion);
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }

    public String getPrecioTotal() {
        return precioTotal.get();
    }

    public StringProperty precioTotalProperty() {
        return precioTotal;
    }

    public void setPrecioTotal(String precioTotal) {
        this.precioTotal.set(precioTotal);
    }

    public String getPrecioTotalIva() {
        return precioTotalIva.get();
    }

    public StringProperty precioTotalIvaProperty() {
        return precioTotalIva;
    }

    public void setPrecioTotalIva(String precioTotalIva) {
        this.precioTotalIva.set(precioTotalIva);
    }

    public String getInventarioMinimo() {
        return inventarioMinimo.get();
    }

    public StringProperty inventarioMinimoProperty() {
        return inventarioMinimo;
    }

    public void setInventarioMinimo(String inventarioMinimo) {
        this.inventarioMinimo.set(inventarioMinimo);
    }
}
