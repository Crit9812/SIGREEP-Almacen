package Compartido.exportar;

public class ConfiguracionReporte {
    private final String[] headers;
    private final float[] widths;

    public ConfiguracionReporte(String[] headers, float[] widths) {
        this.headers = headers;
        this.widths = widths;
    }

    public String[] getHeaders() { return headers; }
    public float[] getWidths() { return widths; }

    public static ConfiguracionReporte getConfiguracion(TipoOperacion tipo) {
        switch (tipo) {
            case TRASPASO_SALIDA:
                return new ConfiguracionReporte(
                        new String[]{"#", "Clave", "Producto", "Descripción", "Cant.", "Present.", "Factor", "P. Unit.", "P. Total"},
                        new float[]{30, 80, 150, 120, 60, 60, 80, 80, 80}
                );
            case VENTA:
                return new ConfiguracionReporte(
                        new String[]{"#", "Clave", "Producto", "Descripción", "Cant.", "Present.", "Lote", "P. Unit.", "P. Total"},
                        new float[]{30, 80, 150, 120, 60, 60, 80, 80, 80}
                );
            case COMPRA:
                return new ConfiguracionReporte(
                        new String[]{"#", "Clave", "Producto", "Descripción", "Cant.", "Present.", "Factor", "Lote", "Caducidad", "P. Unit.", "P. Total"},
                        new float[]{30, 80, 150, 120, 60, 60, 60, 80, 80, 80, 80}
                );
            case AJUSTE_INVENTARIO:
                return new ConfiguracionReporte(
                        new String[]{"#", "Tipo", "Clave", "Producto", "Descripción", "Cant.", "Lote", "P. Unit.", "P. Total"},
                        new float[]{30, 30, 80, 150, 120, 60, 80, 80, 80}
                );
            case TRASPASO_ENTRADA:
                return new ConfiguracionReporte(
                        new String[]{"#", "Clave", "Producto", "Cantidad", "Precio Unit.", "Precio Total"},
                        new float[]{40, 90, 180, 70, 90, 90}
                );
            default:
                return getConfiguracion(TipoOperacion.TRASPASO_SALIDA);
        }
    }
}