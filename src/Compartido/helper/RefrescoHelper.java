package Compartido.helper;

import java.util.HashMap;
import java.util.Map;

public class RefrescoHelper {

    // Mapa para almacenar múltiples métodos de refresco identificados por nombre de vista
    private static final Map<String, Runnable> refrescosRegistrados = new HashMap<>();

    // Nombre de la vista actual
    private static String vistaActual;

    private RefrescoHelper() {
        // Evitar instancias
    }

    /**
     * Registra el método que debe ejecutarse al presionar "Actualizar" para una vista específica
     */
    public static void registrarRefresco(String nombreVista, Runnable refresco) {
        refrescosRegistrados.put(nombreVista, refresco);
    }

    /**
     * Establece la vista actualmente activa
     */
    public static void setVistaActual(String nombreVista) {
        vistaActual = nombreVista;
    }

    /**
     * Ejecuta el refresco registrado para la vista actual (si existe)
     */
    public static void refrescar() {
        if (vistaActual != null && refrescosRegistrados.containsKey(vistaActual)) {
            refrescosRegistrados.get(vistaActual).run();
        }
    }

    /**
     * Limpia el registro de refrescos para una vista específica
     */
    public static void limpiarRefresco(String nombreVista) {
        refrescosRegistrados.remove(nombreVista);
    }
}