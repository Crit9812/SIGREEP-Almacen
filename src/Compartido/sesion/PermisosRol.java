package Compartido.sesion;

public final class PermisosRol {

    private PermisosRol() {}

    public static boolean esAdministrador() {
        return tieneRol("Administrador");
    }

    public static boolean esAuxiliar() {
        return tieneRol("Auxiliar");
    }

    public static boolean esSupervisor() {
        return tieneRol("Supervisor");
    }

    public static boolean esUsuario() {
        return tieneRol("Usuario");
    }

    public static boolean esSupervisorOUsuario() {
        return esSupervisor() || esUsuario();
    }

    private static boolean tieneRol(String rolEsperado) {
        String rolActual = SesionUsuario.getRolUsuario();
        return rolActual != null && rolActual.trim().equalsIgnoreCase(rolEsperado);
    }
}
