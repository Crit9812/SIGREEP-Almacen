package controllerFormularios;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class controllerFormulario {

    public static void llamarFormulario(String rutaFXML, Object controlador, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(controllerFormulario.class.getResource(rutaFXML));
            loader.setController(controlador); // Asignamos el controlador antes de cargar
            Parent root = loader.load(); // carga FXML con el controlador asignado

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.getIcons().add(new Image(controllerFormulario.class.getResourceAsStream("/img/logo-GREEP.png")));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana principal
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
