package Compartido.helper;

public class BusquedaProductoHelper {

    private static SolicitudBusqueda solicitudPendiente;

    private BusquedaProductoHelper() {
    }

    public static synchronized void guardarSolicitud(String idProducto, String nombreProducto, String terminoBusqueda,
                                                     String presentacion, String factor) {
        solicitudPendiente = new SolicitudBusqueda(idProducto, nombreProducto, terminoBusqueda, presentacion, factor);
    }

    public static synchronized SolicitudBusqueda consumirSolicitud() {
        SolicitudBusqueda solicitud = solicitudPendiente;
        solicitudPendiente = null;
        return solicitud;
    }

    public static class SolicitudBusqueda {
        private final String idProducto;
        private final String nombreProducto;
        private final String terminoBusqueda;
        private final String presentacion;
        private final String factor;

        public SolicitudBusqueda(String idProducto, String nombreProducto, String terminoBusqueda,
                                 String presentacion, String factor) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
            this.terminoBusqueda = terminoBusqueda;
            this.presentacion = presentacion;
            this.factor = factor;
        }

        public String getIdProducto() {
            return idProducto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public String getTerminoBusqueda() {
            return terminoBusqueda;
        }

        public String getPresentacion() {
            return presentacion;
        }

        public String getFactor() {
            return factor;
        }
    }
}
