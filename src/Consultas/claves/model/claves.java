package Consultas.claves.model;

import Compartido.model.DAO.Table;
import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;

@Table(name = "claves")
public class claves {

    @PrimaryKey
    @Column(name = "idAlterno")
    private String idClaveCatalogo;

    @Column(name = "idProveedor")
    private Integer idProveedor;

    @Column(name = "idProducto")
    private String idProducto;

    public claves() {}

    public claves(String idClaveCatalogo, String idProducto, Integer idProveedor) {
        this.idClaveCatalogo = idClaveCatalogo;
        this.idProducto = idProducto;
        this.idProveedor = idProveedor;
    }

    public String getIdClaveCatalogo() { return idClaveCatalogo; }
    public void setIdClaveCatalogo(String idClaveCatalogo) { this.idClaveCatalogo = idClaveCatalogo; }

    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }
}
