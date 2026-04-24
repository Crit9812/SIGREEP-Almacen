package Consultas.clasificacion.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class model {

    // DAOs para cada entidad
    private final GenericDAO<marcas> marcaDAO = new GenericDAO<>(marcas.class);
    private final GenericDAO<etiquetas> etiquetaDAO = new GenericDAO<>(etiquetas.class);
    private final GenericDAO<ubicaciones> ubicacionDAO = new GenericDAO<>(ubicaciones.class);
    private final GenericDAO<unidades_Medida> umDAO = new GenericDAO<>(unidades_Medida.class);

    // ================= MARCAS =================

    public List<marcas> obtenerMarcas() {
        List<marcas> todas = marcaDAO.obtenerTodos();
        java.util.List<marcas> activas = new java.util.ArrayList<>();
        for (marcas m : todas) {
            if (m != null && "activo".equalsIgnoreCase(m.getEstado())) {
                activas.add(m);
            }
        }
        return activas;
    }

    public boolean insertarMarca(String nombre) {
        marcas marca = new marcas();
        marca.setNombre(nombre);
        marca.setEstado("activo");
        return marcaDAO.insertar(marca);
    }

    public boolean actualizarMarca(int id, String nombre) {
        marcas marca = buscarMarcaPorId(id);
        if (marca != null) {
            marca.setNombre(nombre);
            return marcaDAO.actualizar(marca);
        }
        return false;
    }

    private marcas buscarMarcaPorId(int id) {
        List<marcas> marcas = marcaDAO.obtenerTodos();
        for (marcas m : marcas) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    public boolean eliminarMarca(int id) {
        marcas marca = buscarMarcaPorId(id);
        if (marca == null) {
            return false;
        }
        marca.setEstado("desactivado");
        return marcaDAO.actualizar(marca);
    }

    public int contarProductosPorMarca(int marcaId) {
        String sql = "SELECT COUNT(*) FROM productos WHERE marca = ? AND estado = 'activo'";


        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, marcaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ================= ETIQUETAS =================

    public List<etiquetas> obtenerEtiquetas() {
        List<etiquetas> todas = etiquetaDAO.obtenerTodos();
        java.util.List<etiquetas> activas = new java.util.ArrayList<>();
        for (etiquetas e : todas) {
            if (e != null && "activo".equalsIgnoreCase(e.getEstado())) {
                activas.add(e);
            }
        }
        return activas;
    }

    public boolean insertarEtiqueta(String nombre) {
        etiquetas etiqueta = new etiquetas();
        etiqueta.setNombre(nombre);
        etiqueta.setEstado("activo");
        return etiquetaDAO.insertar(etiqueta);
    }

    public boolean actualizarEtiqueta(int id, String nombre) {
        etiquetas etiqueta = buscarEtiquetaPorId(id);
        if (etiqueta != null) {
            etiqueta.setNombre(nombre);
            return etiquetaDAO.actualizar(etiqueta);
        }
        return false;
    }

    private etiquetas buscarEtiquetaPorId(int id) {
        List<etiquetas> etiquetas = etiquetaDAO.obtenerTodos();
        for (etiquetas e : etiquetas) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public boolean eliminarEtiqueta(int id) {
        etiquetas etiqueta = buscarEtiquetaPorId(id);
        if (etiqueta == null) {
            return false;
        }
        etiqueta.setEstado("desactivado");
        return etiquetaDAO.actualizar(etiqueta);
    }

    public int contarProductosPorEtiqueta(int etiquetaId) {
        String sql = "SELECT COUNT(*) FROM productos WHERE etiqueta = ? AND estado = 'activo'";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, etiquetaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ================= UBICACIONES =================

    public List<ubicaciones> obtenerUbicaciones() {
        List<ubicaciones> todas = ubicacionDAO.obtenerTodos();
        java.util.List<ubicaciones> activas = new java.util.ArrayList<>();
        for (ubicaciones u : todas) {
            if (u != null && "activo".equalsIgnoreCase(u.getEstado())) {
                activas.add(u);
            }
        }
        return activas;
    }

    public boolean insertarUbicacion(String nombre) {
        ubicaciones ubicacion = new ubicaciones();
        ubicacion.setNombre(nombre);
        ubicacion.setEstado("activo");
        return ubicacionDAO.insertar(ubicacion);
    }

    public boolean actualizarUbicacion(int id, String nombre) {
        ubicaciones ubicacion = buscarUbicacionPorId(id);
        if (ubicacion != null) {
            ubicacion.setNombre(nombre);
            return ubicacionDAO.actualizar(ubicacion);
        }
        return false;
    }

    private ubicaciones buscarUbicacionPorId(int id) {
        List<ubicaciones> ubicaciones = ubicacionDAO.obtenerTodos();
        for (ubicaciones u : ubicaciones) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean eliminarUbicacion(int id) {
        ubicaciones ubicacion = buscarUbicacionPorId(id);
        if (ubicacion == null) {
            return false;
        }
        ubicacion.setEstado("desactivado");
        return ubicacionDAO.actualizar(ubicacion);
    }

    public int contarProductosPorUbicacion(int ubicacionId) {
        String sql = "SELECT COUNT(*) FROM articulo WHERE ubicacion = ? AND estado <> 'eliminado'";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ubicacionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> obtenerDetalleArticulosPorUbicacion(int ubicacionId) {
        List<String> detalles = new java.util.ArrayList<>();
        String sql = """
                SELECT idArticulo, Estado, lote, caducidad
                FROM articulo
                WHERE ubicacion = ?
                ORDER BY idArticulo
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ubicacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idArticulo = rs.getString("idArticulo");
                    String estado = rs.getString("Estado");
                    String lote = rs.getString("lote");
                    String caducidad = rs.getString("caducidad");
                    String texto = "Artículo " + idArticulo
                            + (estado != null ? " | Estado " + estado : "")
                            + (lote != null ? " | Lote " + lote : "")
                            + (caducidad != null ? " | Caducidad " + caducidad : "");
                    detalles.add(texto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return detalles;
    }

    // ================= UNIDADES DE MEDIDA =================

    public List<unidades_Medida> obtenerUM() {
        List<unidades_Medida> todas = umDAO.obtenerTodos();
        java.util.List<unidades_Medida> activas = new java.util.ArrayList<>();
        for (unidades_Medida u : todas) {
            if (u != null && "activo".equalsIgnoreCase(u.getEstado())) {
                activas.add(u);
            }
        }
        return activas;
    }

    public boolean insertarUM(String nombre) {
        unidades_Medida um = new unidades_Medida();
        um.setNombre(nombre);
        um.setEstado("activo");
        return umDAO.insertar(um);
    }

    public boolean existeMarca(String nombre) {
        return existePorNombre(marcaDAO.obtenerTodos(), nombre);
    }

    public boolean existeEtiqueta(String nombre) {
        return existePorNombre(etiquetaDAO.obtenerTodos(), nombre);
    }

    public boolean existeUbicacion(String nombre) {
        return existePorNombre(ubicacionDAO.obtenerTodos(), nombre);
    }

    public boolean existeUM(String nombre) {
        return existePorNombre(umDAO.obtenerTodos(), nombre);
    }

    private <T> boolean existePorNombre(List<T> items, String nombre) {
        if (nombre == null) {
            return false;
        }
        String objetivo = nombre.trim();
        if (objetivo.isEmpty()) {
            return false;
        }
        for (T item : items) {
            if (item instanceof marcas) {
                marcas m = (marcas) item;
                if (m.getNombre() != null && m.getNombre().equalsIgnoreCase(objetivo)) {
                    return true;
                }
            } else if (item instanceof etiquetas) {
                etiquetas e = (etiquetas) item;
                if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(objetivo)) {
                    return true;
                }
            } else if (item instanceof ubicaciones) {
                ubicaciones u = (ubicaciones) item;
                if (u.getNombre() != null && u.getNombre().equalsIgnoreCase(objetivo)) {
                    return true;
                }
            } else if (item instanceof unidades_Medida) {
                unidades_Medida um = (unidades_Medida) item;
                if (um.getNombre() != null && um.getNombre().equalsIgnoreCase(objetivo)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean actualizarUM(int id, String nombre) {
        unidades_Medida um = buscarUMPorId(id);
        if (um != null) {
            um.setNombre(nombre);
            return umDAO.actualizar(um);
        }
        return false;
    }

    private unidades_Medida buscarUMPorId(int id) {
        List<unidades_Medida> unidades = umDAO.obtenerTodos();
        for (unidades_Medida u : unidades) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean eliminarUM(int id) {
        unidades_Medida um = buscarUMPorId(id);
        if (um == null) {
            return false;
        }
        um.setEstado("desactivado");
        return umDAO.actualizar(um);
    }

    public int contarProductosPorUM(String nombreUM) {
        String sql = "SELECT COUNT(*) FROM productos WHERE unidadMedida LIKE ? AND estado = 'activo'";
        String patron = "%" + nombreUM + "%";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patron);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
