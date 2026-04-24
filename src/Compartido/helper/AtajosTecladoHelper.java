package Compartido.helper;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

public final class AtajosTecladoHelper {
    private AtajosTecladoHelper() {}

    public static void instalar(Node root, EventHandler<KeyEvent> handler) {
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
        });
        Scene scene = root.getScene();
        if (scene != null) scene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
    }
}
