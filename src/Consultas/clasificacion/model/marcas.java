package Consultas.clasificacion.model;

import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;
import Compartido.model.DAO.Table;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Table(name = "marcas")
public class marcas {

    @PrimaryKey
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "estado")
    private String estado;

    // Properties para JavaFX
    private transient IntegerProperty idProperty;
    private transient StringProperty nombreProperty;

    public marcas() {}

    public marcas(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) {
        this.id = id;
        if (idProperty != null) {  // ← AGREGAR
            idProperty.set(id);    // ← AGREGAR
        }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = nombre;
        if (nombreProperty != null) {
            nombreProperty.set(nombre);
        }
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // Métodos property para JavaFX
    public IntegerProperty idProperty() {
        if (idProperty == null) {
            idProperty = new SimpleIntegerProperty(id);
        }
        return idProperty;
    }

    public StringProperty nombreProperty() {
        if (nombreProperty == null) {
            nombreProperty = new SimpleStringProperty(nombre);
        }
        return nombreProperty;
    }
}
