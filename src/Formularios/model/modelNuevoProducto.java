package Formularios.model;

import Consultas.clasificacion.model.unidades_Medida;
import Consultas.producto.model.producto;
import Consultas.producto.model.etiqueta;
import Consultas.producto.model.marca;
import Consultas.producto.model.modelEtiqueta;
import Consultas.producto.model.modelMarca;
import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class modelNuevoProducto {

    private GenericDAO<producto> productoDAO;
    private modelEtiqueta modelEtiqueta;
    private modelMarca modelMarca;
    private GenericDAO<unidades_Medida> unidadDAO;

    public modelNuevoProducto() {
        this.productoDAO = new GenericDAO<>(producto.class);
        this.modelEtiqueta = new modelEtiqueta();
        this.modelMarca = new modelMarca();
        this.unidadDAO = new GenericDAO<>(unidades_Medida.class);
    }

    // Metodo para guardar producto (nuevo)
    public boolean guardarProducto(producto p) {
        return productoDAO.insertar(p);
    }

    // Metodo para modificar producto (existente)
    public boolean modificarProducto(producto p) {
        return productoDAO.actualizar(p);
    }

    public boolean eliminarProducto(String id) {
        return productoDAO.eliminar(id);
    }

    // Metodo para buscar producto por ID
    public producto buscarProductoPorId(String id) {
        return productoDAO.buscarExacto("id", id);
    }

    public producto buscarProductoDuplicado(String idIgnorar, String nombre, String etiquetaId,
                                            String marcaId, String categoria, String material,
                                            String unidadMedida) {
        java.util.ArrayList<producto> productos = productoDAO.obtenerTodos();
        String nombreNormalizado = normalizarTexto(nombre);
        String etiquetaNormalizada = normalizarTexto(etiquetaId);
        String marcaNormalizada = normalizarTexto(marcaId);
        String categoriaNormalizada = normalizarTexto(categoria);
        String materialNormalizado = normalizarTexto(material);
        String unidadNormalizada = normalizarUnidadMedida(unidadMedida);
        String idIgnorarNormalizado = normalizarTexto(idIgnorar);

        for (producto productoActual : productos) {
            if (productoActual == null) {
                continue;
            }
            String idProducto = normalizarTexto(productoActual.getIdProducto());
            if (!idIgnorarNormalizado.isEmpty() && idProducto.equals(idIgnorarNormalizado)) {
                continue;
            }
            if (nombreNormalizado.equals(normalizarTexto(productoActual.getNombreProducto()))
                    && etiquetaNormalizada.equals(normalizarTexto(productoActual.getEtiqueta()))
                    && marcaNormalizada.equals(normalizarTexto(productoActual.getMarca()))
                    && categoriaNormalizada.equals(normalizarTexto(productoActual.getCategoria()))
                    && materialNormalizado.equals(normalizarTexto(productoActual.getMaterial()))
                    && unidadNormalizada.equals(normalizarUnidadMedida(productoActual.getUnidadMedida()))) {
                return productoActual;
            }
        }
        return null;
    }

    // Métodos para manejar etiquetas
    public etiqueta buscarEtiquetaPorNombre(String nombre) {
        return modelEtiqueta.buscarPorNombre(nombre);
    }

    public String crearOActualizarEtiqueta(String nombre) {
        // Primero buscamos si ya existe (insensible a mayúsculas/minúsculas)
        etiqueta etiquetaExistente = buscarEtiquetaPorNombreInsensible(nombre);

        if (etiquetaExistente != null) {
            // Si existe, actualizamos el nombre por si hay cambios de formato
            if (!etiquetaExistente.getNombre().equals(nombre)) {
                etiquetaExistente.setNombre(nombre);
                modelEtiqueta.actualizarEtiqueta(etiquetaExistente);
            }
            return etiquetaExistente.getId();
        } else {
            // Si no existe, creamos una nueva
            etiqueta nuevaEtiqueta = new etiqueta();
            nuevaEtiqueta.setNombre(nombre);
            nuevaEtiqueta.setEstado("activo");
            if (modelEtiqueta.insertarEtiqueta(nuevaEtiqueta)) {
                return nuevaEtiqueta.getId();
            }
        }
        return null;
    }

    private etiqueta buscarEtiquetaPorNombreInsensible(String nombre) {
        // Necesitamos implementar un método en ModelEtiqueta para búsqueda insensible
        // Por ahora, vamos a obtener todas y buscar manualmente
        java.util.ArrayList<etiqueta> todas = modelEtiqueta.obtenerTodasIncluyendoInactivas();
        for (etiqueta e : todas) {
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                return e;
            }
        }
        return null;
    }

    // Métodos para manejar marcas
    public marca buscarMarcaPorNombre(String nombre) {
        return modelMarca.buscarPorNombre(nombre);
    }

    public String crearOActualizarMarca(String nombre) {
        // Primero buscamos si ya existe (insensible a mayúsculas/minúsculas)
        marca marcaExistente = buscarMarcaPorNombreInsensible(nombre);

        if (marcaExistente != null) {
            // Si existe, actualizamos el nombre por si hay cambios de formato
            if (!marcaExistente.getNombre().equals(nombre)) {
                marcaExistente.setNombre(nombre);
                modelMarca.actualizarMarca(marcaExistente);
            }
            return marcaExistente.getId();
        } else {
            // Si no existe, creamos una nueva
            marca nuevaMarca = new marca();
            nuevaMarca.setNombre(nombre);
            nuevaMarca.setEstado("activo");
            if (modelMarca.insertarMarca(nuevaMarca)) {
                return nuevaMarca.getId();
            }
        }
        return null;
    }

    private marca buscarMarcaPorNombreInsensible(String nombre) {
        // Necesitamos implementar un metodo en ModelMarca para búsqueda insensible
        // Por ahora, vamos a obtener todas y buscar manualmente
        java.util.ArrayList<marca> todas = modelMarca.obtenerTodasIncluyendoInactivas();
        for (marca m : todas) {
            if (m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre)) {
                return m;
            }
        }
        return null;
    }

    public ObservableList<String> obtenerUnidadesActivas() {
        java.util.ArrayList<unidades_Medida> unidades = unidadDAO.obtenerTodos();
        java.util.List<String> nombres = new java.util.ArrayList<>();
        for (unidades_Medida unidad : unidades) {
            if (unidad != null && "activo".equalsIgnoreCase(unidad.getEstado())) {
                if (unidad.getNombre() != null) {
                    nombres.add(unidad.getNombre());
                }
            }
        }
        return FXCollections.observableArrayList(nombres);
    }

    public String crearOActualizarUnidad(String nombre) {
        unidades_Medida existente = buscarUnidadPorNombreInsensible(nombre);
        if (existente != null) {
            if (!nombre.equals(existente.getNombre())) {
                existente.setNombre(nombre);
                unidadDAO.actualizar(existente);
            }
            return existente.getNombre();
        }

        unidades_Medida nueva = new unidades_Medida();
        nueva.setNombre(nombre);
        nueva.setEstado("activo");
        if (unidadDAO.insertar(nueva)) {
            return nueva.getNombre();
        }
        return null;
    }

    private unidades_Medida buscarUnidadPorNombreInsensible(String nombre) {
        java.util.ArrayList<unidades_Medida> unidades = unidadDAO.obtenerTodos();
        for (unidades_Medida unidad : unidades) {
            if (unidad.getNombre() != null && unidad.getNombre().equalsIgnoreCase(nombre)) {
                return unidad;
            }
        }
        return null;
    }

    // Metodo para obtener etiqueta por ID
    public etiqueta obtenerEtiquetaPorId(String id) {
        return modelEtiqueta.buscarPorId(id);
    }

    // Metodo para obtener marca por ID
    public marca obtenerMarcaPorId(String id) {
        return modelMarca.buscarPorId(id);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().toLowerCase();
    }

    private String normalizarUnidadMedida(String unidadMedida) {
        if (unidadMedida == null) {
            return "";
        }
        String[] partes = unidadMedida.split(",");
        java.util.List<String> normalizadas = new java.util.ArrayList<>();
        for (String parte : partes) {
            String normalizada = parte.trim().replaceAll("\\s+", "");
            if (!normalizada.isEmpty()) {
                normalizadas.add(normalizada.toLowerCase());
            }
        }
        return String.join(",", normalizadas);
    }
}
