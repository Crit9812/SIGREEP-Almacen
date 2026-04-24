package Compartido.sesion;

public class SesionUsuario {

    private static String nombreUsuario;
    private static Integer idUsuario;
    private static String rolUsuario;

    public static void setNombreUsuario(String nombre){
        nombreUsuario = nombre;
    }
    public static String getNombreUsuario(){
        return nombreUsuario;
    }

    public static void setIdUsuario(Integer id){
        idUsuario = id;
    }
    public static Integer getIdUsuario(){
        return idUsuario;
    }

    public static void setRolUsuario(String rol) { rolUsuario = rol; }
    public static String getRolUsuario() { return rolUsuario; }

    public static void cerrarSesion(){
        nombreUsuario = null;
        idUsuario = null;
        rolUsuario = null;
    }
}
