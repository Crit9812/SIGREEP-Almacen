package Operaciones.traspasoEntrada.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class traspasoEntrada {

    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);
    private final StringProperty claveEntrada = new SimpleStringProperty("");
    private final StringProperty fecha = new SimpleStringProperty("");
    private final StringProperty hora = new SimpleStringProperty("");
    private final StringProperty total = new SimpleStringProperty("");
    private final StringProperty nombreSucursal = new SimpleStringProperty("");

    public traspasoEntrada(String claveEntrada, String fecha, String hora, String total, String nombreSucursal) {
        this.claveEntrada.set(claveEntrada != null ? claveEntrada : "");
        this.fecha.set(fecha != null ? fecha : "");
        this.hora.set(hora != null ? hora : "");
        this.total.set(total != null ? total : "");
        this.nombreSucursal.set(nombreSucursal != null ? nombreSucursal : "");
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

    public String getClaveEntrada() {
        return claveEntrada.get();
    }

    public StringProperty claveEntradaProperty() {
        return claveEntrada;
    }

    public void setClaveEntrada(String claveEntrada) {
        this.claveEntrada.set(claveEntrada != null ? claveEntrada : "");
    }

    public String getFecha() {
        return fecha.get();
    }

    public StringProperty fechaProperty() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha.set(fecha != null ? fecha : "");
    }

    public String getHora() {
        return hora.get();
    }

    public StringProperty horaProperty() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora.set(hora != null ? hora : "");
    }

    public String getTotal() {
        return total.get();
    }

    public StringProperty totalProperty() {
        return total;
    }

    public void setTotal(String total) {
        this.total.set(total != null ? total : "");
    }

    public String getNombreSucursal() {
        return nombreSucursal.get();
    }

    public StringProperty nombreSucursalProperty() {
        return nombreSucursal;
    }

    public void setNombreSucursal(String nombreSucursal) {
        this.nombreSucursal.set(nombreSucursal != null ? nombreSucursal : "");
    }
}
