package Reportes.utilidades.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UtilidadItem {
    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty nombreProducto = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty descripcionProducto = new SimpleStringProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final StringProperty factor = new SimpleStringProperty();
    private final StringProperty cantidad = new SimpleStringProperty();
    private final StringProperty totalCompra = new SimpleStringProperty();
    private final StringProperty proveedor = new SimpleStringProperty();
    private final StringProperty totalVenta = new SimpleStringProperty();
    private final StringProperty cliente = new SimpleStringProperty();
    private final StringProperty porcentajeUtilidad = new SimpleStringProperty();
    private final StringProperty utilidadPesos = new SimpleStringProperty();
    private final StringProperty facturaCompra = new SimpleStringProperty();
    private final StringProperty facturaVenta = new SimpleStringProperty();
    private final StringProperty fechaSalida = new SimpleStringProperty();

    public UtilidadItem(String claveProducto,
                        String nombreProducto,
                        String categoria,
                        String descripcionProducto,
                        String presentacion,
                        String factor,
                        String cantidad,
                        String totalCompra,
                        String proveedor,
                        String totalVenta,
                        String cliente,
                        String porcentajeUtilidad,
                        String utilidadPesos,
                        String facturaCompra,
                        String facturaVenta,
                        String fechaSalida) {
        this.claveProducto.set(claveProducto);
        this.nombreProducto.set(nombreProducto);
        this.categoria.set(categoria);
        this.descripcionProducto.set(descripcionProducto);
        this.presentacion.set(presentacion);
        this.factor.set(factor);
        this.cantidad.set(cantidad);
        this.totalCompra.set(totalCompra);
        this.proveedor.set(proveedor);
        this.totalVenta.set(totalVenta);
        this.cliente.set(cliente);
        this.porcentajeUtilidad.set(porcentajeUtilidad);
        this.utilidadPesos.set(utilidadPesos);
        this.facturaCompra.set(facturaCompra);
        this.facturaVenta.set(facturaVenta);
        this.fechaSalida.set(fechaSalida);
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

    public String getNombreProducto() {
        return nombreProducto.get();
    }

    public StringProperty nombreProductoProperty() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto.set(nombreProducto);
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

    public String getDescripcionProducto() {
        return descripcionProducto.get();
    }

    public StringProperty descripcionProductoProperty() {
        return descripcionProducto;
    }

    public void setDescripcionProducto(String descripcionProducto) {
        this.descripcionProducto.set(descripcionProducto);
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

    public String getCantidad() {
        return cantidad.get();
    }

    public StringProperty cantidadProperty() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad.set(cantidad);
    }

    public String getTotalCompra() {
        return totalCompra.get();
    }

    public StringProperty totalCompraProperty() {
        return totalCompra;
    }

    public void setTotalCompra(String totalCompra) {
        this.totalCompra.set(totalCompra);
    }

    public String getProveedor() {
        return proveedor.get();
    }

    public StringProperty proveedorProperty() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor.set(proveedor);
    }

    public String getTotalVenta() {
        return totalVenta.get();
    }

    public StringProperty totalVentaProperty() {
        return totalVenta;
    }

    public void setTotalVenta(String totalVenta) {
        this.totalVenta.set(totalVenta);
    }

    public String getCliente() {
        return cliente.get();
    }

    public StringProperty clienteProperty() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente.set(cliente);
    }

    public String getPorcentajeUtilidad() {
        return porcentajeUtilidad.get();
    }

    public StringProperty porcentajeUtilidadProperty() {
        return porcentajeUtilidad;
    }

    public void setPorcentajeUtilidad(String porcentajeUtilidad) {
        this.porcentajeUtilidad.set(porcentajeUtilidad);
    }

    public String getUtilidadPesos() {
        return utilidadPesos.get();
    }

    public StringProperty utilidadPesosProperty() {
        return utilidadPesos;
    }

    public void setUtilidadPesos(String utilidadPesos) {
        this.utilidadPesos.set(utilidadPesos);
    }

    public String getFacturaCompra() {
        return facturaCompra.get();
    }

    public StringProperty facturaCompraProperty() {
        return facturaCompra;
    }

    public void setFacturaCompra(String facturaCompra) {
        this.facturaCompra.set(facturaCompra);
    }

    public String getFacturaVenta() {
        return facturaVenta.get();
    }

    public StringProperty facturaVentaProperty() {
        return facturaVenta;
    }

    public void setFacturaVenta(String facturaVenta) {
        this.facturaVenta.set(facturaVenta);
    }

    public String getFechaSalida() {
        return fechaSalida.get();
    }

    public StringProperty fechaSalidaProperty() {
        return fechaSalida;
    }

    public void setFechaSalida(String fechaSalida) {
        this.fechaSalida.set(fechaSalida);
    }
}
