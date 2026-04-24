package Compartido.helper;

import VentanaPrincipal.controller.MovimientoType;
import java.util.HashMap;
import java.util.Map;

public class BorradorService {
    private static BorradorService instance;
    private final Map<MovimientoType, Object> borradores = new HashMap<>();

    private BorradorService() {}

    public static BorradorService getInstance() {
        if (instance == null) {
            instance = new BorradorService();
        }
        return instance;
    }

    public void guardar(MovimientoType tipo, Object borrador) {
        borradores.put(tipo, borrador);
    }

    public Object recuperar(MovimientoType tipo) {
        return borradores.get(tipo);
    }

    public void eliminar(MovimientoType tipo) {
        borradores.remove(tipo);
    }

    public boolean existe(MovimientoType tipo) {
        return borradores.containsKey(tipo);
    }
}