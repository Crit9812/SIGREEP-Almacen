package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Map;

public class model {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_PENDIENTE = "pendiente";
    private static final String ESTADO_DISPONIBLE = "disponible";

    private final GenericDAO<producto> productoDAO;
    private final modelEtiqueta modelEtiqueta;
    private final modelMarca modelMarca;

    public model() {
        this.productoDAO = new GenericDAO<>(producto.class);
        this.modelEtiqueta = new modelEtiqueta();
        this.modelMarca = new modelMarca();
    }

    public ObservableList<producto> obtenerProductos() {
        ArrayList<producto> lista = productoDAO.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminarProducto(String id) {
        producto producto = buscarProductoPorId(id);
        if (producto == null) {
            return false;
        }
        producto.setEstado("desactivado");
        return productoDAO.actualizar(producto);
    }

    public ObservableList<producto> buscarProductos(String campo, String valor) {
        ArrayList<producto> lista = productoDAO.buscarParcial(campo, valor);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public ObservableList<producto> busquedaMultipleProductos(String textoBusqueda) {
        ArrayList<producto> lista = productoDAO.buscarMultiple("id", "nombre", textoBusqueda);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public producto buscarProductoPorId(String id) {
        return productoDAO.buscarExacto("id", id);
    }

    public int contarEntradasPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM entradas e
                INNER JOIN detalle_Entrada de ON de.claveEntrada = e.idEntrada
                WHERE de.claveProducto = ?
                  AND LOWER(e.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto);
    }

    public int contarSalidasPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM salidas s
                INNER JOIN detalle_Salida ds ON ds.claveSalida = s.idSalida
                WHERE ds.claveProductoSalida = ?
                  AND LOWER(s.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto);
    }

    public int contarClavesPorProducto(String idProducto) {
        return contarRegistros("SELECT COUNT(*) FROM claves WHERE idProducto = ? AND estado = 'activo'", idProducto);
    }

    public int contarDetallesEntradaPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM detalle_Entrada de
                INNER JOIN entradas e ON e.idEntrada = de.claveEntrada
                WHERE de.claveProducto = ?
                  AND LOWER(e.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto);
    }

    public int contarDetallesSalidaPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM detalle_Salida ds
                INNER JOIN salidas s ON s.idSalida = ds.claveSalida
                WHERE ds.claveProductoSalida = ?
                  AND LOWER(s.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto);
    }

    public int contarArticulosEntradaPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM articulo a
                INNER JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                INNER JOIN entradas e ON e.idEntrada = de.claveEntrada
                WHERE de.claveProducto = ?
                  AND LOWER(e.Estado) IN (?, ?, ?)
                  AND LOWER(a.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto, true);
    }

    public int contarArticulosSalidaPorProducto(String idProducto) {
        String sql = """
                SELECT COUNT(*)
                FROM articulo a
                INNER JOIN detalle_Salida ds ON ds.idDetalleSalida = a.idDetalleSalida
                INNER JOIN salidas s ON s.idSalida = ds.claveSalida
                WHERE ds.claveProductoSalida = ?
                  AND LOWER(s.Estado) IN (?, ?, ?)
                  AND LOWER(a.Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProducto, true);
    }

    private int contarRegistros(String sql, String valor) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int contarRegistrosConEstados(String sql, String valor) {
        return contarRegistrosConEstados(sql, valor, false);
    }

    private int contarRegistrosConEstados(String sql, String valor, boolean incluirEstadoArticulo) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, ESTADO_ACTIVO);
            ps.setString(3, ESTADO_PENDIENTE);
            ps.setString(4, ESTADO_DISPONIBLE);
            if (incluirEstadoArticulo) {
                ps.setString(5, ESTADO_ACTIVO);
                ps.setString(6, ESTADO_PENDIENTE);
                ps.setString(7, ESTADO_DISPONIBLE);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean actualizarUrlImagen(String id, String nuevaUrl) {
        producto productoActual = buscarProductoPorId(id);
        if (productoActual == null) {
            return false;
        }

        productoActual.setUrlImagen(nuevaUrl);
        return productoDAO.actualizar(productoActual);
    }

    public Map<String, String> obtenerMapaEtiquetas() {
        return modelEtiqueta.obtenerMapaEtiquetas();
    }

    public Map<String, String> obtenerMapaMarcas() {
        return modelMarca.obtenerMapaMarcas();
    }

    public ObservableList<etiqueta> obtenerListaEtiquetas() {
        ArrayList<etiqueta> lista = modelEtiqueta.obtenerTodas();
        return FXCollections.observableArrayList(lista);
    }

    public ObservableList<marca> obtenerListaMarcas() {
        ArrayList<marca> lista = modelMarca.obtenerTodas();
        return FXCollections.observableArrayList(lista);
    }

    public etiqueta buscarEtiquetaPorId(String id) {
        return modelEtiqueta.buscarPorId(id);
    }

    public marca buscarMarcaPorId(String id) {
        return modelMarca.buscarPorId(id);
    }

    private ArrayList<producto> filtrarActivos(ArrayList<producto> lista) {
        ArrayList<producto> activos = new ArrayList<>();
        for (producto p : lista) {
            if (p != null && ESTADO_ACTIVO.equalsIgnoreCase(p.getEstado())) {
                activos.add(p);
            }
        }
        return activos;
    }
}
