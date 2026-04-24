package Compartido.exportar;

public enum TipoOperacion {
    TRASPASO_SALIDA("TRASPASO DE SALIDA", "Reporte_Traspaso_Salida"),
    VENTA("VENTA", "Reporte_Venta"),
    COMPRA("COMPRA", "Reporte_Compra"),
    AJUSTE_INVENTARIO("AJUSTE DE INVENTARIO", "Reporte_Ajuste_Inventario"),
    TRASPASO_ENTRADA("TRASPASO DE ENTRADA", "Reporte_Traspaso_Entrada");

    private final String titulo;
    private final String prefijoArchivo;

    TipoOperacion(String titulo, String prefijoArchivo) {
        this.titulo = titulo;
        this.prefijoArchivo = prefijoArchivo;
    }

    public String getTitulo() { return titulo; }
    public String getPrefijoArchivo() { return prefijoArchivo; }
}