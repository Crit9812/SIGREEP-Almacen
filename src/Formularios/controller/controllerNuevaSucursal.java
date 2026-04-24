package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.helper.SepomexCache;
import Consultas.sucursales.model.sucursal;
import Formularios.model.modelNuevaSucursal;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class controllerNuevaSucursal {

    private static final int CP_LONGITUD = 5;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtCP;
    @FXML private ComboBox<String> cmbColonia;
    @FXML private TextField txtNumeroExt;
    @FXML private TextField txtNumeroInt;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtPais;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idSucursalEdicion = -1;
    private final modelNuevaSucursal model = new modelNuevaSucursal();
    private Runnable onSaved = null;
    private final AtomicLong solicitudCpId = new AtomicLong(0);
    private String coloniaPendiente = null;
    private String ultimoCpBuscado = null;
    private boolean avisoErrorMostrado = false;
    private boolean avisoInfoMostrado = false;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarSucursal());
        configurarValidaciones();
        configurarCamposAutocompletado();
        configurarAutocompletadoCp();

        // Activar guardar con ENTER
        setEnterAction(txtNombre);
        setEnterAction(txtDomicilio);
        setEnterAction(txtCP);
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            setEnterAction(cmbColonia.getEditor());
        }
        setEnterAction(txtNumeroExt);
        setEnterAction(txtNumeroInt);
        setEnterAction(txtCiudad);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtPais);
        setEnterAction(txtCorreo);
        setEnterAction(txtTelefono);
    }

    private void setEnterAction(TextField field) {
        if (field != null) field.setOnAction(e -> guardarSucursal());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void cargarSucursal(sucursal s) {
        if (s == null) return;

        modoEdicion = true;
        idSucursalEdicion = s.getId();

        txtNombre.setText(s.getNombre());
        txtDomicilio.setText(s.getDomicilio());
        txtCP.setText(String.valueOf(s.getCp()));
        if (cmbColonia != null) cmbColonia.setValue(s.getColonia());
        txtNumeroExt.setText(String.valueOf(s.getNumeroExt()));
        txtNumeroInt.setText(String.valueOf(s.getNumeroInt()));
        txtCiudad.setText(s.getCiudad());
        txtEstado.setText(s.getEstado());
        txtLocalidad.setText(s.getLocalidad());
        txtPais.setText(s.getPais());
        txtCorreo.setText(s.getCorreo());
        txtTelefono.setText(String.valueOf(s.getTelefono()));

        coloniaPendiente = s.getColonia();
        buscarCpSiValido(txtCP.getText());

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar sucursal");
    }

    @FXML public void prepararNuevaSucursal() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar sucursal");
    }

    @FXML public void guardarSucursal() {
        if (!validarFormulario()) return;

        sucursal s = new sucursal();
        if (modoEdicion) s.setId(idSucursalEdicion);

        s.setNombre(txtNombre.getText().trim());
        s.setDomicilio(txtDomicilio.getText().trim());
        s.setCp(Integer.parseInt(txtCP.getText().trim()));
        s.setColonia(obtenerColoniaSeleccionada());
        s.setNumeroExt(Integer.parseInt(txtNumeroExt.getText().trim()));
        s.setNumeroInt(obtenerNumeroOpcional(txtNumeroInt));
        s.setCiudad(txtCiudad.getText().trim());
        s.setEstado(txtEstado.getText().trim());
        s.setLocalidad(txtLocalidad.getText().trim());
        s.setPais(txtPais.getText().trim());
        s.setCorreo(txtCorreo.getText().trim());
        s.setTelefono(txtTelefono.getText().trim());
        s.setStatus("activo");

        boolean exito = modoEdicion ?
                model.modificarSucursal(s) :
                model.guardarSucursal(s);

        if (exito) {
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    modoEdicion ? "Sucursal actualizada correctamente"
                            : "Sucursal agregada correctamente"
            );

            if (onSaved != null) onSaved.run();
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();

        } else {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error al guardar la sucursal en la BD");
        }
    }

    private void configurarValidaciones() {
        aplicarFiltro(txtNombre, permitirAlfanumericoConSimbolos(100));
        aplicarFiltro(txtDomicilio, permitirAlfanumericoConSimbolos(180));
        aplicarFiltro(txtCP, permitirNumeros(CP_LONGITUD));
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            aplicarFiltro(cmbColonia.getEditor(), permitirTexto(120));
        }
        aplicarFiltro(txtNumeroExt, permitirNumeros(10));
        aplicarFiltro(txtNumeroInt, permitirNumeros(10));
        aplicarFiltro(txtCorreo, permitirEmail(120));
        aplicarFiltro(txtTelefono, permitirNumeros(10));
    }

    private void aplicarFiltro(TextField field, UnaryOperator<TextFormatter.Change> filter) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private UnaryOperator<TextFormatter.Change> permitirNumeros(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("\\d*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirTexto(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s'.-]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirAlfanumericoConSimbolos(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ\\s#.,'/-]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirEmail(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-z0-9._%+\\-@]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private boolean validarFormulario() {
        if (!validarCampoObligatorio(txtNombre, "Nombre")) return false;
        if (!validarCampoObligatorio(txtDomicilio, "Domicilio")) return false;
        if (!validarCampoObligatorio(txtCP, "Código Postal")) return false;
        if (!validarCampoObligatorio(txtNumeroExt, "Número Exterior")) return false;
        if (!validarCampoObligatorio(txtCiudad, "Ciudad")) return false;
        if (!validarCampoObligatorio(txtEstado, "Estado")) return false;
        if (!validarCampoObligatorio(txtLocalidad, "Localidad")) return false;
        if (!validarCampoObligatorio(txtPais, "País")) return false;

        if (!validarCampoNumerico(txtCP, "Código Postal")) return false;
        if (!validarCampoNumerico(txtNumeroExt, "Número Exterior")) return false;
        if (!validarCampoNumericoOpcional(txtNumeroInt, "Número Interior")) return false;
        if (!validarCampoNumericoOpcional(txtTelefono, "Teléfono")) return false;

        String cp = txtCP.getText().trim();
        if (cp.length() != CP_LONGITUD) {
            mostrarAlerta(Alert.AlertType.ERROR, "El código postal debe tener 5 dígitos.");
            return false;
        }

        String correo = txtCorreo.getText().trim();
        if (!correo.isBlank() && !EMAIL_PATTERN.matcher(correo).matches()) {
            mostrarAlerta(Alert.AlertType.ERROR, "El correo electrónico no tiene un formato válido.");
            return false;
        }

        String telefono = txtTelefono.getText().trim();
        if (!telefono.isBlank() && telefono.length() != 10) {
            mostrarAlerta(Alert.AlertType.ERROR, "El teléfono debe tener 10 dígitos.");
            return false;
        }

        return true;
    }

    private boolean validarCampoObligatorio(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        if (campo.getText() == null || campo.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' es obligatorio.");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validarCampoNumerico(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        if (!campo.getText().matches("\\d+")) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' debe contener solo números.");
            campo.requestFocus();
            campo.selectAll();
            return false;
        }
        return true;
    }

    private boolean validarCampoNumericoOpcional(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        String valor = campo.getText();
        if (valor == null || valor.trim().isEmpty()) return true;
        return validarCampoNumerico(campo, nombreCampo);
    }

    private Integer obtenerNumeroOpcional(TextField campo) {
        if (campo == null) return 0;
        String valor = campo.getText();
        if (valor == null || valor.trim().isEmpty()) return 0;
        return Integer.parseInt(valor.trim());
    }

    private void configurarCamposAutocompletado() {
        bloquearCampo(txtPais);
        bloquearCampo(txtEstado);
        bloquearCampo(txtLocalidad);
        bloquearCampo(txtCiudad);

        if (cmbColonia != null) {
            cmbColonia.setEditable(true);
            new AutoCompleteComboBoxListener<>(cmbColonia);
        }
    }

    private void bloquearCampo(TextField campo) {
        if (campo == null) return;
        campo.setEditable(false);
        campo.setFocusTraversable(false);
    }

    private void configurarAutocompletadoCp() {
        if (txtCP == null) return;
        txtCP.textProperty().addListener((obs, oldVal, newVal) -> buscarCpSiValido(newVal));
    }

    private void buscarCpSiValido(String cpTexto) {
        if (cpTexto == null) {
            limpiarAutocompletado();
            return;
        }

        String cp = cpTexto.trim();

        if (ultimoCpBuscado == null || !ultimoCpBuscado.equals(cp)) {
            avisoErrorMostrado = false;
            avisoInfoMostrado = false;
        }

        if (!cp.matches("\\d{" + CP_LONGITUD + "}")) {
            limpiarAutocompletado();
            return;
        }
        if (cp.equals(ultimoCpBuscado)) return;

        ultimoCpBuscado = cp;
        buscarDatosPorCp(cp, coloniaPendiente);
    }

    private void buscarDatosPorCp(String cp, String coloniaPreferida) {
        long solicitudActual = solicitudCpId.incrementAndGet();

        CompletableFuture
                .supplyAsync(() -> buscarEnSepomex(cp))
                .thenAccept(info -> {
                    if (solicitudCpId.get() != solicitudActual) return;
                    Platform.runLater(() -> aplicarAutocompletado(info, coloniaPreferida));
                })
                .exceptionally(ex -> {
                    if (solicitudCpId.get() == solicitudActual) {
                        Platform.runLater(() -> {
                            limpiarAutocompletado();
                            mostrarErrorUnaVez("Error consultando SEPOMEX", ex.getMessage());
                        });
                    }
                    return null;
                });
    }

    private void mostrarErrorUnaVez(String titulo, String detalle) {
        if (avisoErrorMostrado) return;
        avisoErrorMostrado = true;

        String msg = (detalle == null || detalle.isBlank()) ? "Sin detalle." : detalle;
        mostrarAlerta(Alert.AlertType.ERROR, titulo + "\n\n" + msg);
    }

    private void mostrarInfoUnaVez(String titulo, String detalle) {
        if (avisoInfoMostrado) return;
        avisoInfoMostrado = true;

        String msg = (detalle == null || detalle.isBlank()) ? "" : ("\n\n" + detalle);
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo + msg);
    }

    private void mostrarAlerta(Alert.AlertType type, String mensaje) {
        Alert alert = new Alert(type, mensaje);
        Stage stage = btnGuardar != null && btnGuardar.getScene() != null
                ? (Stage) btnGuardar.getScene().getWindow()
                : null;
        if (stage != null) {
            alert.initOwner(stage);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.showAndWait();
    }

    private void aplicarAutocompletado(SepomexCache.CpInfo info, String coloniaPreferida) {
        if (info == null) {
            limpiarAutocompletado();
            mostrarInfoUnaVez("CP no encontrado", "No se encontraron datos para ese código postal.");
            return;
        }

        if (info.pais != null) txtPais.setText(info.pais);
        if (info.estado != null) txtEstado.setText(info.estado);
        if (info.localidad != null) txtLocalidad.setText(info.localidad);
        if (info.ciudad != null) txtCiudad.setText(info.ciudad);

        if (cmbColonia != null) {
            cmbColonia.setItems(FXCollections.observableArrayList(info.colonias));

            String seleccion = coloniaPreferida;
            if (seleccion == null || seleccion.isBlank()) {
                seleccion = info.colonias.isEmpty() ? null : info.colonias.get(0);
            }

            if (seleccion != null && !seleccion.isBlank()) {
                if (!info.colonias.contains(seleccion)) {
                    cmbColonia.getItems().add(seleccion);
                }
                cmbColonia.setValue(seleccion);
                if (cmbColonia.getEditor() != null) {
                    cmbColonia.getEditor().setText(seleccion);
                }
            } else {
                cmbColonia.setValue(null);
                if (cmbColonia.getEditor() != null) cmbColonia.getEditor().clear();
            }
        }

        coloniaPendiente = null;
    }

    private void limpiarAutocompletado() {
        if (txtPais != null) txtPais.clear();
        if (txtEstado != null) txtEstado.clear();
        if (txtLocalidad != null) txtLocalidad.clear();
        if (txtCiudad != null) txtCiudad.clear();

        if (cmbColonia != null) {
            cmbColonia.setItems(FXCollections.observableArrayList());
            cmbColonia.setValue(null);
            if (cmbColonia.getEditor() != null) cmbColonia.getEditor().clear();
        }
    }

    private String obtenerColoniaSeleccionada() {
        if (cmbColonia == null) return "";
        String valor = cmbColonia.getValue();
        if ((valor == null || valor.isBlank()) && cmbColonia.getEditor() != null) {
            valor = cmbColonia.getEditor().getText();
        }
        return valor != null ? valor.trim() : "";
    }

    private SepomexCache.CpInfo buscarEnSepomex(String cp) {
        return SepomexCache.buscarCp(cp);
    }

}
