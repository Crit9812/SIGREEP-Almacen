package Consultas.sucursales.model;

import Compartido.model.DAO.Table;
import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;

@Table(name = "sucursales")
public class sucursal {

    @PrimaryKey
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "domicilio")
    private String domicilio;

    @Column(name = "cp")
    private Integer cp;

    @Column(name = "colonia")
    private String colonia;

    @Column(name = "numeroInt")
    private Integer numeroInt;

    @Column(name = "numeroExt")
    private Integer numeroExt;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "estado")
    private String estado;

    @Column(name = "localidad")
    private String localidad;

    @Column(name = "pais")
    private String pais;

    @Column(name = "correo")
    private String correo;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "status")
    private String status;

    public sucursal() {}

    public sucursal(Integer id, String nombre, String domicilio, Integer cp,
                    String colonia, Integer numeroInt, Integer numeroExt,
                    String ciudad, String estado, String localidad,
                    String pais, String correo, String telefono) {

        this.id = id;
        this.nombre = nombre;
        this.domicilio = domicilio;
        this.cp = cp;
        this.colonia = colonia;
        this.numeroInt = numeroInt;
        this.numeroExt = numeroExt;
        this.ciudad = ciudad;
        this.estado = estado;
        this.localidad = localidad;
        this.pais = pais;
        this.correo = correo;
        this.telefono = telefono;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public Integer getCp() { return cp; }
    public void setCp(Integer cp) { this.cp = cp; }

    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }

    public Integer getNumeroInt() { return numeroInt; }
    public void setNumeroInt(Integer numeroInt) { this.numeroInt = numeroInt; }

    public Integer getNumeroExt() { return numeroExt; }
    public void setNumeroExt(Integer numeroExt) { this.numeroExt = numeroExt; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
