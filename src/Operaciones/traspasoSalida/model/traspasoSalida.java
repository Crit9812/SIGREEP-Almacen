package Operaciones.traspasoSalida.model;

import Operaciones.compra.model.UbicacionCompra;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class traspasoSalida {

    private final StringProperty claveProducto = new SimpleStringProperty("");
    private final StringProperty producto = new SimpleStringProperty("");
    private final StringProperty descripcion = new SimpleStringProperty("");
    private final StringProperty lote = new SimpleStringProperty("");
    private final StringProperty caducidad = new SimpleStringProperty("");
    private final IntegerProperty cantidad = new SimpleIntegerProperty(0);
    private final StringProperty presentacion = new SimpleStringProperty("");
    private final IntegerProperty factor = new SimpleIntegerProperty(0);
    private final StringProperty ubicacionResumen = new SimpleStringProperty("");
    private final StringProperty nota = new SimpleStringProperty("");
    private final StringProperty precioEntrada = new SimpleStringProperty("");
    private final StringProperty precioIva = new SimpleStringProperty("");
    private final StringProperty precioBruto = new SimpleStringProperty("");
    private final StringProperty precioTotal = new SimpleStringProperty("");
    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);

    private final List<UbicacionCompra> ubicaciones = new ArrayList<>();

    public traspasoSalida(String claveProducto, String producto, String descripcion, String lote, String caducidad,
                          int cantidad, String presentacion, int factor, List<UbicacionCompra> ubicaciones,
                          String precioEntrada, String precioIva, String precioBruto, String precioTotal) {
        this.claveProducto.set(claveProducto != null ? claveProducto : "");
        this.producto.set(producto != null ? producto : "");
        this.descripcion.set(descripcion != null ? descripcion : "");
        this.lote.set(lote != null ? lote : "");
        this.caducidad.set(caducidad != null ? caducidad : "");
        this.cantidad.set(cantidad);
        this.presentacion.set(presentacion != null ? presentacion : "");
        this.factor.set(factor);
        if (ubicaciones != null) {
            this.ubicaciones.addAll(ubicaciones);
        }
        this.ubicacionResumen.set(armarResumenUbicaciones());
        this.nota.set("");
        this.precioEntrada.set(precioEntrada != null ? precioEntrada : "");
        this.precioIva.set(precioIva != null ? precioIva : "");
        this.precioBruto.set(precioBruto != null ? precioBruto : "");
        this.precioTotal.set(precioTotal != null ? precioTotal : "");
    }

    // Constructor vacío
    public traspasoSalida() {
        this.claveProducto.set("");
        this.producto.set("");
        this.descripcion.set("");
        this.lote.set("");
        this.caducidad.set("");
        this.cantidad.set(0);
        this.presentacion.set("");
        this.factor.set(0);
        this.nota.set("");
        this.precioEntrada.set("");
        this.precioIva.set("");
        this.precioBruto.set("");
        this.precioTotal.set("");
    }

    private String armarResumenUbicaciones() {
        if (ubicaciones.isEmpty()) {
            return "";
        }
        return ubicaciones.stream()
                .map(u -> u.getUbicacion() + " (" + u.getCantidad() + ")")
                .collect(Collectors.joining(", "));
    }

    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public void setClaveProducto(String claveProducto) {
        this.claveProducto.set(claveProducto != null ? claveProducto : "");
    }

    public String getProducto() {
        return producto.get();
    }

    public StringProperty productoProperty() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto.set(producto != null ? producto : "");
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion != null ? descripcion : "");
    }

    public String getLote() {
        return lote.get();
    }

    public StringProperty loteProperty() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote.set(lote != null ? lote : "");
    }

    public String getCaducidad() {
        return caducidad.get();
    }

    public StringProperty caducidadProperty() {
        return caducidad;
    }

    public void setCaducidad(String caducidad) {
        this.caducidad.set(caducidad != null ? caducidad : "");
    }

    public int getCantidad() {
        return cantidad.get();
    }

    public IntegerProperty cantidadProperty() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad.set(cantidad);
    }

    public String getPresentacion() {
        return presentacion.get();
    }

    public StringProperty presentacionProperty() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion.set(presentacion != null ? presentacion : "");
    }

    public int getFactor() {
        return factor.get();
    }

    public IntegerProperty factorProperty() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor.set(factor);
    }

    public String getUbicacionResumen() {
        return ubicacionResumen.get();
    }

    public StringProperty ubicacionResumenProperty() {
        return ubicacionResumen;
    }

    public String getNota() {
        return nota.get();
    }

    public StringProperty notaProperty() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota.set(nota != null ? nota : "");
    }

    public String getPrecioEntrada() {
        return precioEntrada.get();
    }

    public StringProperty precioEntradaProperty() {
        return precioEntrada;
    }

    public void setPrecioEntrada(String precioEntrada) {
        this.precioEntrada.set(precioEntrada != null ? precioEntrada : "");
    }

    public String getPrecioIva() {
        return precioIva.get();
    }

    public StringProperty precioIvaProperty() {
        return precioIva;
    }

    public void setPrecioIva(String precioIva) {
        this.precioIva.set(precioIva != null ? precioIva : "");
    }

    public String getPrecioBruto() {
        return precioBruto.get();
    }

    public StringProperty precioBrutoProperty() {
        return precioBruto;
    }

    public void setPrecioBruto(String precioBruto) {
        this.precioBruto.set(precioBruto != null ? precioBruto : "");
    }

    public String getPrecioTotal() {
        return precioTotal.get();
    }

    public StringProperty precioTotalProperty() {
        return precioTotal;
    }

    public void setPrecioTotal(String precioTotal) {
        this.precioTotal.set(precioTotal != null ? precioTotal : "");
    }

    public boolean isSeleccionado() {
        return seleccionado.get();
    }

    public BooleanProperty seleccionadoProperty() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado.set(seleccionado);
    }

    public List<UbicacionCompra> getUbicaciones() {
        return new ArrayList<>(ubicaciones);
    }

    public void setUbicaciones(List<UbicacionCompra> nuevasUbicaciones) {
        ubicaciones.clear();
        if (nuevasUbicaciones != null) {
            ubicaciones.addAll(nuevasUbicaciones);
        }
        this.ubicacionResumen.set(armarResumenUbicaciones());
    }
}
