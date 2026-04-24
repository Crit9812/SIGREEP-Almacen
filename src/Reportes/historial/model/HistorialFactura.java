package Reportes.historial.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class HistorialFactura {
    private final StringProperty movimiento = new SimpleStringProperty();
    private final StringProperty claveMovimiento = new SimpleStringProperty();
    private final StringProperty factura = new SimpleStringProperty();
    private final StringProperty fecha = new SimpleStringProperty();
    private final StringProperty hora = new SimpleStringProperty();
    private final StringProperty tipoMovimiento = new SimpleStringProperty();
    private final StringProperty usuario = new SimpleStringProperty();
    private final StringProperty externo = new SimpleStringProperty();
    private final StringProperty precioNeto = new SimpleStringProperty();
    private final StringProperty precioTotal = new SimpleStringProperty();
    private final StringProperty nota = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();

    public HistorialFactura(String movimiento,
                            String claveMovimiento,
                            String factura,
                            String fecha,
                            String hora,
                            String tipoMovimiento,
                            String usuario,
                            String externo,
                            String precioNeto,
                            String precioTotal,
                            String nota,
                            String estado) {
        this.movimiento.set(movimiento);
        this.claveMovimiento.set(claveMovimiento);
        this.factura.set(factura);
        this.fecha.set(fecha);
        this.hora.set(hora);
        this.tipoMovimiento.set(tipoMovimiento);
        this.usuario.set(usuario);
        this.externo.set(externo);
        this.precioNeto.set(precioNeto);
        this.precioTotal.set(precioTotal);
        this.nota.set(nota);
        this.estado.set(estado);
    }

    public String getMovimiento() {
        return movimiento.get();
    }

    public StringProperty movimientoProperty() {
        return movimiento;
    }

    public void setMovimiento(String movimiento) {
        this.movimiento.set(movimiento);
    }

    public String getClaveMovimiento() {
        return claveMovimiento.get();
    }

    public StringProperty claveMovimientoProperty() {
        return claveMovimiento;
    }

    public void setClaveMovimiento(String claveMovimiento) {
        this.claveMovimiento.set(claveMovimiento);
    }

    public String getFactura() {
        return factura.get();
    }

    public StringProperty facturaProperty() {
        return factura;
    }

    public void setFactura(String factura) {
        this.factura.set(factura);
    }

    public String getFecha() {
        return fecha.get();
    }

    public StringProperty fechaProperty() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha.set(fecha);
    }

    public String getHora() {
        return hora.get();
    }

    public StringProperty horaProperty() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora.set(hora);
    }

    public String getTipoMovimiento() {
        return tipoMovimiento.get();
    }

    public StringProperty tipoMovimientoProperty() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento.set(tipoMovimiento);
    }

    public String getUsuario() {
        return usuario.get();
    }

    public StringProperty usuarioProperty() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario.set(usuario);
    }

    public String getExterno() {
        return externo.get();
    }

    public StringProperty externoProperty() {
        return externo;
    }

    public void setExterno(String externo) {
        this.externo.set(externo);
    }

    public String getPrecioNeto() {
        return precioNeto.get();
    }

    public StringProperty precioNetoProperty() {
        return precioNeto;
    }

    public void setPrecioNeto(String precioNeto) {
        this.precioNeto.set(precioNeto);
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

    public String getNota() {
        return nota.get();
    }

    public StringProperty notaProperty() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota.set(nota);
    }

    public String getEstado() {
        return estado.get();
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado.set(estado);
    }
}
