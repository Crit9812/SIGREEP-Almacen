package Operaciones.pedidos.model;

import javafx.beans.property.*;

public class itemPedido {

    private final StringProperty claveProducto = new SimpleStringProperty("");
    private final StringProperty producto = new SimpleStringProperty("");
    private final StringProperty descripcion = new SimpleStringProperty("");
    private final IntegerProperty cantidad = new SimpleIntegerProperty(0);
    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);
    private final StringProperty claveAlterna = new SimpleStringProperty("");
    private final StringProperty presentacion = new SimpleStringProperty("");
    private final StringProperty factor = new SimpleStringProperty("");

    // Constructor base
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad,
                      String claveAlterna, String presentacion, String factor) {
        this.claveProducto.set(claveProducto);
        this.producto.set(producto);
        this.descripcion.set(descripcion);
        this.cantidad.set(cantidad);
        this.claveAlterna.set(claveAlterna != null ? claveAlterna : "");
        this.presentacion.set(presentacion != null ? presentacion : "");
        this.factor.set(factor != null ? factor : "");
    }

    // Constructor sin clave alterna, presentación ni factor
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad) {
        this(claveProducto, producto, descripcion, cantidad, "", "", "");
    }

    // Constructor con clave alterna
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad, String claveAlterna) {
        this(claveProducto, producto, descripcion, cantidad, claveAlterna, "", "");
    }

    // ===== Getters / Setters / Properties =====

    public String getClaveProducto() { return claveProducto.get(); }
    public void setClaveProducto(String claveProducto) { this.claveProducto.set(claveProducto); }
    public StringProperty claveProductoProperty() { return claveProducto; }

    public String getProducto() { return producto.get(); }
    public void setProducto(String producto) { this.producto.set(producto); }
    public StringProperty productoProperty() { return producto; }

    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String descripcion) { this.descripcion.set(descripcion); }
    public StringProperty descripcionProperty() { return descripcion; }

    public int getCantidad() { return cantidad.get(); }
    public void setCantidad(int cantidad) { this.cantidad.set(cantidad); }
    public IntegerProperty cantidadProperty() { return cantidad; }

    public boolean isSeleccionado() { return seleccionado.get(); }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado.set(seleccionado); }
    public BooleanProperty seleccionadoProperty() { return seleccionado; }

    public String getClaveAlterna() { return claveAlterna.get(); }
    public void setClaveAlterna(String claveAlterna) { this.claveAlterna.set(claveAlterna); }
    public StringProperty claveAlternaProperty() { return claveAlterna; }

    public String getPresentacion() { return presentacion.get(); }
    public void setPresentacion(String presentacion) { this.presentacion.set(presentacion); }
    public StringProperty presentacionProperty() { return presentacion; }

    public String getFactor() { return factor.get(); }
    public void setFactor(String factor) { this.factor.set(factor); }
    public StringProperty factorProperty() { return factor; }

    // ===== Utilidades =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        itemPedido that = (itemPedido) obj;

        return cantidad.get() == that.cantidad.get()
                && claveProducto.get().equals(that.claveProducto.get())
                && producto.get().equals(that.producto.get());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + claveProducto.get().hashCode();
        result = 31 * result + producto.get().hashCode();
        result = 31 * result + cantidad.get();
        return result;
    }

    public void copiarDe(itemPedido otro) {
        if (otro == null) return;
        setClaveProducto(otro.getClaveProducto());
        setProducto(otro.getProducto());
        setDescripcion(otro.getDescripcion());
        setCantidad(otro.getCantidad());
        setClaveAlterna(otro.getClaveAlterna());
        setPresentacion(otro.getPresentacion());
        setFactor(otro.getFactor());
        setSeleccionado(otro.isSeleccionado());
    }

    public itemPedido copiar() {
        return new itemPedido(
                getClaveProducto(),
                getProducto(),
                getDescripcion(),
                getCantidad(),
                getClaveAlterna(),
                getPresentacion(),
                getFactor()
        );
    }

    public boolean esValido() {
        return !claveProducto.get().isEmpty()
                && !producto.get().isEmpty()
                && cantidad.get() > 0;
    }

    public String getResumen() {
        return producto.get() + " x" + cantidad.get() + " "
                + (!presentacion.get().isEmpty() ? presentacion.get() : "unidades");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append(claveProducto.get())
                .append(" - ")
                .append(producto.get())
                .append(" (")
                .append(cantidad.get())
                .append(")");

        if (!claveAlterna.get().isEmpty()) {
            sb.append(" [").append(claveAlterna.get()).append("]");
        }
        if (!presentacion.get().isEmpty() && !"Pieza".equals(presentacion.get())) {
            sb.append(" | Pres: ").append(presentacion.get());
        }
        if (!factor.get().isEmpty() && !"1".equals(factor.get()) && !"1.0".equals(factor.get())) {
            sb.append(" | Factor: ").append(factor.get());
        }
        return sb.toString();
    }
}
