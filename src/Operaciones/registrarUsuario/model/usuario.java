package Operaciones.registrarUsuario.model;

import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;
import Compartido.model.DAO.Table;

@Table(name = "usuarios")
public class usuario {

    @PrimaryKey
    @Column(name = "idUsuario")
    private String idUsuario;

    @Column(name = "nombreUsuario")
    private String nombreUsuario;

    @Column(name = "apellidoPUsuario")
    private String apellidoPUsuario;

    @Column(name = "apellidoMUsuario")
    private String apellidoMUsuario;

    @Column(name = "userName")
    private String userName;

    @Column(name = "rolUsuario")
    private String rolUsuario;

    @Column(name = "contrasenaUsuario")
    private String contrasenaUsuario;

    @Column(name = "estado")
    private String estado;

    public usuario() {}

    public usuario(String idUsuario, String nombreUsuario, String apellidoPUsuario,
                   String apellidoMUsuario, String userName, String rolUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.apellidoPUsuario = apellidoPUsuario;
        this.apellidoMUsuario = apellidoMUsuario;
        this.userName = userName;
        this.rolUsuario = rolUsuario;
    }

    // getters / setters
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getApellidoPUsuario() { return apellidoPUsuario; }
    public void setApellidoPUsuario(String apellidoPUsuario) { this.apellidoPUsuario = apellidoPUsuario; }

    public String getApellidoMUsuario() { return apellidoMUsuario; }
    public void setApellidoMUsuario(String apellidoMUsuario) { this.apellidoMUsuario = apellidoMUsuario; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(String rolUsuario) { this.rolUsuario = rolUsuario; }

    public String getContrasenaUsuario() { return contrasenaUsuario; }
    public void setContrasenaUsuario(String contrasenaUsuario) { this.contrasenaUsuario = contrasenaUsuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
