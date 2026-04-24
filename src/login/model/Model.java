package login.model;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import conexion.Conexion;

public class Model {
    Conexion c = new Conexion();

    public Connection conexion;
    public boolean acceso;

    // Metodo para inyectar la conexión desde otra clase
    public void setConexion(Connection conexion) {
        this.conexion = conexion;
    }

    @FXML
    private void initialize() {
    }

    public boolean verificarUsuario(String username, String password) {
        setConexion(conexion = c.conectar());

        String sql = "SELECT * FROM usuarios WHERE userName = ? AND contrasenaUsuario = ? AND estado = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, "activo");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Acceso permitido");
                    acceso = true;
                } else {
                    System.out.println("Usuario o contraseña incorrectos");
                    acceso = false;
                }
            }

        } catch (SQLException e) {
            System.out.println("Ocurrió un error");
        }
        return acceso;
    }

    private void mostrarAlerta(String titulo, String mensaje, AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public Integer obtenerIdUsuario(String username) {

        setConexion(conexion = c.conectar());

        String sql = "SELECT idUsuario FROM usuarios WHERE userName = ? AND estado = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, "activo");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("idUsuario");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String obtenerRolUsuario(String username) {
        setConexion(conexion = c.conectar());
        String sql = "SELECT rolUsuario FROM usuarios WHERE userName = ? AND estado = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, "activo");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rolUsuario");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // o un valor por defecto
    }

}
