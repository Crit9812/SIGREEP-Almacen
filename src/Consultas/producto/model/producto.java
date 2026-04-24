package Consultas.producto.model;

import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;
import Compartido.model.DAO.Table;

@Table(name = "productos")
public class producto {

    @PrimaryKey
    @Column(name = "id")
    private String idProducto;

    @Column(name = "nombre")
    private String nombreProducto;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "etiqueta")
    private String etiqueta;

    @Column(name = "marca")
    private String marca;

    @Column(name = "material")
    private String material;

    @Column(name = "unidadMedida")
    private String unidadMedida;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "inventarioMin")
    private int inventarioMin;

    @Column(name = "urlImagen")
    private String urlImagen;

    @Column(name = "estado")
    private String estado;

    public producto() {}

    public producto(String idProducto, String nombreProducto, String categoria, String etiqueta, String marca,
                    String material, String unidadMedida, String descripcion,
                    int inventarioMin, String urlImagen) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.categoria = categoria;
        this.etiqueta = etiqueta;
        this.marca = marca;
        this.material = material;
        this.unidadMedida = unidadMedida;
        this.descripcion = descripcion;
        this.inventarioMin = inventarioMin;
        this.urlImagen = urlImagen;
    }

    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getInventarioMin() { return inventarioMin; }
    public void setInventarioMin(int inventarioMin) { this.inventarioMin = inventarioMin; }

    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
