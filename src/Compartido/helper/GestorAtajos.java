package Compartido.helper;

import VentanaPrincipal.controller.EnumVistas;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

public class GestorAtajos {

    private final Scene scene;
    private final AccionesAtajos acciones;
    private final StringBuilder secuencia = new StringBuilder();
    private final Map<KeyCode, String> mapaLetras = new HashMap<>();

    public GestorAtajos(Scene scene, AccionesAtajos acciones) {
        this.scene = scene;
        this.acciones = acciones;
        inicializarMapaLetras();
        instalarManejadores();
    }

    private void inicializarMapaLetras() {
        mapaLetras.put(KeyCode.O, "O");
        mapaLetras.put(KeyCode.R, "R");
        mapaLetras.put(KeyCode.C, "C");
        mapaLetras.put(KeyCode.A, "A");
        mapaLetras.put(KeyCode.E, "E");
        mapaLetras.put(KeyCode.S, "S");
        mapaLetras.put(KeyCode.M, "M");
        mapaLetras.put(KeyCode.V, "V");
        mapaLetras.put(KeyCode.P, "P");
        mapaLetras.put(KeyCode.I, "I");
        mapaLetras.put(KeyCode.H, "H");
        mapaLetras.put(KeyCode.F, "F");
        mapaLetras.put(KeyCode.U, "U");
        mapaLetras.put(KeyCode.L, "L");
    }

    private void instalarManejadores() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::manejarPresionada);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::manejarLiberada);
    }

    private void manejarPresionada(KeyEvent event) {
        // Atajos globales
        if (new KeyCodeCombination(KeyCode.F1).match(event)) {
            acciones.salir();
            event.consume();
            return;
        }
        if (new KeyCodeCombination(KeyCode.F2).match(event)) {
            acciones.actualizar();
            event.consume();
            return;
        }
        if (new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN).match(event)) {
            acciones.enfocarBusqueda();
            event.consume();
            return;
        }

        if (new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN).match(event)) {
            acciones.pausar();
            event.consume();
            return;
        }

        // Atajos de secuencia con Shift
        if (!event.isShiftDown()) {
            return;
        }

        String letra = mapaLetras.get(event.getCode());
        if (letra == null) {
            return;
        }

        secuencia.append(letra);
        event.consume();
    }

    private void manejarLiberada(KeyEvent event) {
        if (event.getCode() != KeyCode.SHIFT) {
            return;
        }

        ejecutarSecuencia();
        event.consume();
    }

    private void ejecutarSecuencia() {
        String sec = secuencia.toString();

        // Secuencias de dos letras
        switch (sec) {
            case "CO":
                acciones.navegarA("CONSULTAS");
                break;
            case "CM":
                acciones.navegarA(EnumVistas.COMPRA);
                break;
            case "PE":
                acciones.navegarA(EnumVistas.PEDIDOS);
                break;
            case "AI":
                acciones.navegarA(EnumVistas.AJUSTE_INVENTARIO);
                break;
            case "RU":
                acciones.navegarA(EnumVistas.REGISTRAR_USUARIO);
                break;
            case "PR":
                acciones.navegarA(EnumVistas.PROVEEDORES);
                break;
            case "CL":
                acciones.navegarA(EnumVistas.CLAVES);
                break;
            case "CS":
                acciones.navegarA(EnumVistas.CLASIFICACION);
                break;
            case "SU":
                acciones.navegarA(EnumVistas.SUCURSALES);
                break;
            default:
                // Secuencias de una letra
                switch (sec) {
                    case "O":
                        acciones.navegarA("OPERACIONES");
                        break;
                    case "R":
                        acciones.navegarA("REPORTES");
                        break;
                    case "A":
                        acciones.navegarA("CONFIGURACION");
                        break;
                    case "E":
                        acciones.navegarA(EnumVistas.TRASPASO_ENTRADA);
                        break;
                    case "S":
                        acciones.navegarA(EnumVistas.TRASPASO_SALIDA);
                        break;
                    case "V":
                        acciones.navegarA(EnumVistas.VENTA);
                        break;
                    case "I":
                        acciones.navegarA(EnumVistas.INVENTARIO);
                        break;
                    case "H":
                        acciones.navegarA(EnumVistas.HISTORIAL_ARTICULO);
                        break;
                    case "F":
                        acciones.navegarA(EnumVistas.HISTORIAL);
                        break;
                    case "U":
                        acciones.navegarA(EnumVistas.UTILIDADES);
                        break;
                    case "P":
                        acciones.navegarA(EnumVistas.PRODUCTO);
                        break;
                    case "C":
                        acciones.navegarA(EnumVistas.CLIENTES);
                        break;
                    default:
                        break;
                }
                break;
        }

        secuencia.setLength(0);
    }
}