package conexion;

import Compartido.helper.OverlayCarga;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConexionMonitor {
    private static final ConexionMonitor INSTANCE = new ConexionMonitor();
    private static final long INTERVALO_VERIFICACION_MILLIS = 400;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "conexion-monitor");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean monitorIniciado = new AtomicBoolean(false);
    private final AtomicBoolean verificando = new AtomicBoolean(false);
    private final Map<Stage, OverlayState> overlaysPorStage = new HashMap<>();
    private boolean listenerVentanasRegistrado = false;

    private volatile boolean conexionDisponible = true;
    private Stage stagePrincipal;

    private ConexionMonitor() {
    }

    public static ConexionMonitor getInstance() {
        return INSTANCE;
    }

    public void iniciar(Stage stagePrincipal) {
        this.stagePrincipal = stagePrincipal;

        stagePrincipal.sceneProperty().addListener((obs, oldScene, newScene) -> Platform.runLater(this::actualizarOverlaysEnTodasLasVentanas));
        registrarListenerVentanas();
        Platform.runLater(this::actualizarOverlaysEnTodasLasVentanas);

        if (monitorIniciado.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::verificarYRecuperarConexion, 0,
                    INTERVALO_VERIFICACION_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    public void notificarDesconexionInmediata() {
        conexionDisponible = false;
        Platform.runLater(this::mostrarOverlayBloqueanteEnTodasLasVentanas);
    }

    public void notificarConexionRestablecidaInmediata() {
        conexionDisponible = true;
        Platform.runLater(this::ocultarOverlayBloqueanteEnTodasLasVentanas);
    }

    private void registrarListenerVentanas() {
        if (listenerVentanasRegistrado) {
            return;
        }
        listenerVentanasRegistrado = true;
        Window.getWindows().addListener((ListChangeListener<Window>) change ->
                Platform.runLater(this::actualizarOverlaysEnTodasLasVentanas));
    }

    private void actualizarOverlaysEnTodasLasVentanas() {
        overlaysPorStage.entrySet().removeIf(entry -> !entry.getKey().isShowing());

        if (stagePrincipal != null && stagePrincipal.isShowing()) {
            asegurarOverlayParaStage(stagePrincipal);
        }

        for (Window window : Window.getWindows()) {
            if (!(window instanceof Stage stage) || !stage.isShowing()) {
                continue;
            }
            asegurarOverlayParaStage(stage);
        }

        for (OverlayState state : overlaysPorStage.values()) {
            if (conexionDisponible) {
                state.overlayCarga.ocultar();
            } else {
                state.overlayCarga.setMensaje("Conectando...");
                state.overlayCarga.mostrar();
            }
        }
    }

    private void asegurarOverlayParaStage(Stage stage) {
        Scene scene = stage.getScene();
        if (scene == null) {
            return;
        }

        OverlayState existente = overlaysPorStage.get(stage);
        if (existente != null && existente.scene == scene && existente.overlayPane.getParent() == existente.rootContenedor) {
            return;
        }

        StackPane rootContenedor;
        if (scene.getRoot() instanceof StackPane stackRoot) {
            rootContenedor = stackRoot;
        } else {
            Parent rootOriginal = scene.getRoot();
            rootContenedor = new StackPane();
            rootContenedor.getChildren().add(rootOriginal);
            scene.setRoot(rootContenedor);
        }

        Pane overlayPane = new Pane();
        rootContenedor.getChildren().add(overlayPane);

        OverlayCarga overlayCarga = new OverlayCarga(rootContenedor, overlayPane, "Conectando...");
        overlayCarga.ocultar();

        OverlayState state = new OverlayState(scene, rootContenedor, overlayPane, overlayCarga);
        overlaysPorStage.put(stage, state);
    }

    private void mostrarOverlayBloqueanteEnTodasLasVentanas() {
        actualizarOverlaysEnTodasLasVentanas();
        for (OverlayState state : overlaysPorStage.values()) {
            state.overlayCarga.setMensaje("Conectando...");
            state.overlayCarga.mostrar();
        }
    }

    private void ocultarOverlayBloqueanteEnTodasLasVentanas() {
        for (OverlayState state : overlaysPorStage.values()) {
            state.overlayCarga.ocultar();
        }
    }

    private static class OverlayState {
        private final Scene scene;
        private final StackPane rootContenedor;
        private final Pane overlayPane;
        private final OverlayCarga overlayCarga;

        private OverlayState(Scene scene, StackPane rootContenedor, Pane overlayPane, OverlayCarga overlayCarga) {
            this.scene = scene;
            this.rootContenedor = rootContenedor;
            this.overlayPane = overlayPane;
            this.overlayCarga = overlayCarga;
        }
    }

    private void verificarYRecuperarConexion() {
        if (!verificando.compareAndSet(false, true)) {
            return;
        }

        try {
            boolean disponible = Conexion.probarConexionRapida();

            if (disponible != conexionDisponible) {
                conexionDisponible = disponible;
                if (disponible) {
                    Platform.runLater(this::ocultarOverlayBloqueanteEnTodasLasVentanas);
                } else {
                    Platform.runLater(this::mostrarOverlayBloqueanteEnTodasLasVentanas);
                }
            }
        } finally {
            verificando.set(false);
        }
    }
}
