package Configuracion.controller;

import Compartido.model.IvaConfigService;
import Compartido.sesion.SesionUsuario;
import Compartido.helper.RefrescoHelper;
import VentanaPrincipal.controller.ControladorVista;
import conexion.Conexion;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private TextField txtIva;
    @FXML private Label lblIvaActual;
    @FXML private Label lblSucursalNombre;
    @FXML private Label lblSucursalDomicilio;
    @FXML private Label lblSucursalCp;
    @FXML private Label lblSucursalColonia;
    @FXML private Label lblSucursalNumeroInt;
    @FXML private Label lblSucursalNumeroExt;
    @FXML private Label lblSucursalCiudad;
    @FXML private Label lblSucursalEstado;
    @FXML private Label lblSucursalLocalidad;
    @FXML private Label lblSucursalPais;
    @FXML private Label lblSucursalCorreo;
    @FXML private Label lblSucursalTelefono;
    @FXML private Label lblUsuarioId;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioApellidoP;
    @FXML private Label lblUsuarioApellidoM;
    @FXML private Label lblUsuarioUserName;
    @FXML private Label lblUsuarioRol;
    @FXML private Label lblUsuarioEstado;
    @FXML private PasswordField txtUsuarioContrasenaOculta;
    @FXML private TextField txtUsuarioContrasenaVisible;
    @FXML private Button btnToggleContrasenaUsuario;

    private boolean usuarioContrasenaVisible = false;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        cargarDatosSucursalActual();
        cargarDatosUsuarioSesion();
        configurarCampoContrasenaUsuario();
        configurarValidaciones();
        recargarIva();

        RefrescoHelper.setVistaActual("configuracion");
        RefrescoHelper.registrarRefresco("configuracion", this::recargarIva);
    }

    private void configurarCampoContrasenaUsuario() {
        txtUsuarioContrasenaVisible.setVisible(false);
        txtUsuarioContrasenaVisible.setManaged(false);
        txtUsuarioContrasenaOculta.setVisible(true);
        txtUsuarioContrasenaOculta.setManaged(true);
    }

    private void cargarDatosUsuarioSesion() {
        Integer idUsuario = SesionUsuario.getIdUsuario();
        if (idUsuario == null) {
            mostrarUsuarioNoDisponible();
            return;
        }

        String sql = """
                SELECT idUsuario, nombreUsuario, apellidoPUsuario, apellidoMUsuario,
                       userName, rolUsuario, contrasenaUsuario, estado
                FROM usuarios
                WHERE idUsuario = ?
                LIMIT 1
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblUsuarioId.setText(valorTexto(rs.getObject("idUsuario")));
                    lblUsuarioNombre.setText(valorTexto(rs.getString("nombreUsuario")));
                    lblUsuarioApellidoP.setText(valorTexto(rs.getString("apellidoPUsuario")));
                    lblUsuarioApellidoM.setText(valorTexto(rs.getString("apellidoMUsuario")));
                    lblUsuarioUserName.setText(valorTexto(rs.getString("userName")));
                    lblUsuarioRol.setText(valorTexto(rs.getString("rolUsuario")));
                    String contrasena = valorTexto(rs.getString("contrasenaUsuario"));
                    txtUsuarioContrasenaOculta.setText(contrasena);
                    txtUsuarioContrasenaVisible.setText(contrasena);
                    lblUsuarioEstado.setText(valorTexto(rs.getString("estado")));
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mostrarUsuarioNoDisponible();
    }

    private void cargarDatosSucursalActual() {
        String nombreSucursal = obtenerNombreSucursalDesdeConexion();
        if (nombreSucursal == null || nombreSucursal.isBlank()) {
            mostrarSucursalNoDisponible();
            return;
        }

        String sql = """
                SELECT nombre, domicilio, cp, colonia, numeroInt, numeroExt, ciudad, estado, localidad, pais, correo, telefono
                FROM sucursales
                WHERE LOWER(nombre) = LOWER(?)
                LIMIT 1
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreSucursal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblSucursalNombre.setText(valorTexto(rs.getString("nombre")));
                    lblSucursalDomicilio.setText(valorTexto(rs.getString("domicilio")));
                    lblSucursalCp.setText(valorTexto(rs.getObject("cp")));
                    lblSucursalColonia.setText(valorTexto(rs.getString("colonia")));
                    lblSucursalNumeroInt.setText(valorTexto(rs.getObject("numeroInt")));
                    lblSucursalNumeroExt.setText(valorTexto(rs.getObject("numeroExt")));
                    lblSucursalCiudad.setText(valorTexto(rs.getString("ciudad")));
                    lblSucursalEstado.setText(valorTexto(rs.getString("estado")));
                    lblSucursalLocalidad.setText(valorTexto(rs.getString("localidad")));
                    lblSucursalPais.setText(valorTexto(rs.getString("pais")));
                    lblSucursalCorreo.setText(valorTexto(rs.getString("correo")));
                    lblSucursalTelefono.setText(valorTexto(rs.getString("telefono")));
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mostrarSucursalNoDisponible();
    }

    private String obtenerNombreSucursalDesdeConexion() {
        try (Connection conn = new Conexion().conectar()) {
            String baseDatos = conn.getCatalog();
            if (baseDatos == null || baseDatos.isBlank()) {
                return null;
            }

            String prefijo = "distribu_";
            if (baseDatos.toLowerCase().startsWith(prefijo)) {
                return baseDatos.substring(prefijo.length());
            }
            return baseDatos;
        } catch (Exception e) {
            return null;
        }
    }

    private String valorTexto(Object value) {
        return value == null ? "--" : value.toString();
    }

    private void mostrarSucursalNoDisponible() {
        lblSucursalNombre.setText("--");
        lblSucursalDomicilio.setText("--");
        lblSucursalCp.setText("--");
        lblSucursalColonia.setText("--");
        lblSucursalNumeroInt.setText("--");
        lblSucursalNumeroExt.setText("--");
        lblSucursalCiudad.setText("--");
        lblSucursalEstado.setText("--");
        lblSucursalLocalidad.setText("--");
        lblSucursalPais.setText("--");
        lblSucursalCorreo.setText("--");
        lblSucursalTelefono.setText("--");
    }

    private void mostrarUsuarioNoDisponible() {
        lblUsuarioId.setText("--");
        lblUsuarioNombre.setText("--");
        lblUsuarioApellidoP.setText("--");
        lblUsuarioApellidoM.setText("--");
        lblUsuarioUserName.setText("--");
        lblUsuarioRol.setText("--");
        txtUsuarioContrasenaOculta.setText("--");
        txtUsuarioContrasenaVisible.setText("--");
        lblUsuarioEstado.setText("--");
    }

    @FXML
    private void toggleContrasenaUsuario() {
        usuarioContrasenaVisible = !usuarioContrasenaVisible;

        if (usuarioContrasenaVisible) {
            txtUsuarioContrasenaVisible.setText(txtUsuarioContrasenaOculta.getText());
            txtUsuarioContrasenaVisible.setVisible(true);
            txtUsuarioContrasenaVisible.setManaged(true);
            txtUsuarioContrasenaOculta.setVisible(false);
            txtUsuarioContrasenaOculta.setManaged(false);
        } else {
            txtUsuarioContrasenaOculta.setText(txtUsuarioContrasenaVisible.getText());
            txtUsuarioContrasenaOculta.setVisible(true);
            txtUsuarioContrasenaOculta.setManaged(true);
            txtUsuarioContrasenaVisible.setVisible(false);
            txtUsuarioContrasenaVisible.setManaged(false);
        }
    }

    @FXML
    private void guardarIva() {
        BigDecimal nuevoIva;
        try {
            nuevoIva = new BigDecimal(txtIva.getText().trim());
        } catch (Exception e) {
            mostrarAlerta("Advertencia", "El IVA debe ser numérico.");
            return;
        }

        if (nuevoIva.compareTo(BigDecimal.ZERO) < 0) {
            mostrarAlerta("Advertencia", "El IVA no puede ser negativo.");
            return;
        }

        boolean actualizado = IvaConfigService.actualizarEnBaseDatos(nuevoIva);
        if (!actualizado) {
            mostrarAlerta("Error", "No se pudo actualizar el IVA en base de datos.");
            return;
        }

        mostrarAlerta("Éxito", "IVA actualizado correctamente.");
        refrescarVistaIva();
    }

    @FXML
    private void recargarIva() {
        IvaConfigService.recargarDesdeBaseDatos();
        refrescarVistaIva();
    }

    private void refrescarVistaIva() {
        BigDecimal iva = IvaConfigService.getIvaPorcentaje();
        if (txtIva != null) {
            txtIva.setText(iva.stripTrailingZeros().toPlainString());
        }
        if (lblIvaActual != null) {
            lblIvaActual.setText("IVA actual: " + iva.stripTrailingZeros().toPlainString() + "%");
        }
    }

    private void configurarValidaciones() {
        if (txtIva == null) {
            return;
        }
        txtIva.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            String limpio = newVal.replace(',', '.');
            if (!limpio.matches("^\\d{0,3}(\\.\\d{0,2})?$")) {
                txtIva.setText(oldVal);
            } else if (!limpio.equals(newVal)) {
                txtIva.setText(limpio);
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}
