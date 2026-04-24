package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.helper.SepomexCache;
import Consultas.clientes.model.cliente;
import Formularios.model.modelNuevoCliente;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class controllerNuevoCliente {

    private static final int CP_LONGITUD = 5;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern RFC_PATTERN = Pattern.compile("^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$");
    private static final Pattern CURP_PATTERN = Pattern.compile("^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])"
            + "(0[1-9]|[12]\\d|3[01])[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\\d$");

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRFC;
    @FXML private TextField txtCURP;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtCP;
    @FXML private TextField txtPais;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtCiudad;
    @FXML private ComboBox<String> cmbColonia;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtNoExt;
    @FXML private TextField txtNoInt;
    @FXML private TextField txtCorreoElectronico;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idClienteEdicion = -1;

    private final AtomicLong solicitudCpId = new AtomicLong(0);
    private String coloniaPendiente = null;
    private String ultimoCpBuscado = null;

    private final modelNuevoCliente model = new modelNuevoCliente();
    private Runnable onSaved = null;

    // Para no spamear alertas
    private boolean avisoErrorMostrado = false;
    private boolean avisoInfoMostrado = false;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarCliente());
        configurarCamposAutocompletado();
        configurarAutocompletadoCp();
        configurarValidaciones();

        setEnterAction(txtNombre);
        setEnterAction(txtRFC);
        setEnterAction(txtCURP);
        setEnterAction(txtRazonSocial);
        setEnterAction(txtCP);
        setEnterAction(txtDomicilio);
        setEnterAction(txtNoExt);
        setEnterAction(txtNoInt);
        setEnterAction(txtCorreoElectronico);
        setEnterAction(txtTelefono);
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            setEnterAction(cmbColonia.getEditor());
        }
    }

    private void setEnterAction(TextField field) {
        if (field != null) field.setOnAction(e -> guardarCliente());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    // 👉 Ejecutar cuando es edición
    public void cargarCliente(cliente c) {
        if (c == null) return;

        modoEdicion = true;
        idClienteEdicion = c.getId();
        txtNombre.setText(c.getNombre());
        txtRFC.setText(c.getRfc());
        txtCURP.setText(c.getCurp());
        txtRazonSocial.setText(c.getRazonSocial());
        txtCP.setText(String.valueOf(c.getCp()));
        txtPais.setText(c.getPais());
        txtEstado.setText(c.getEstado());
        txtLocalidad.setText(c.getLocalidad());
        txtCiudad.setText(c.getCiudad());
        coloniaPendiente = c.getColonia();
        if (cmbColonia != null) cmbColonia.setValue(c.getColonia());
        txtDomicilio.setText(c.getDomicilio());
        txtNoExt.setText(String.valueOf(c.getNumeroExt()));
        txtNoInt.setText(String.valueOf(c.getNumeroInt()));
        txtCorreoElectronico.setText(c.getCorreo());
        txtTelefono.setText(c.getTelefono() == null ? "" : c.getTelefono());

        buscarCpSiValido(txtCP.getText());

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar cliente");
    }

    @FXML
    public void prepararNuevoCliente() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar cliente");
    }

    @FXML
    public void guardarCliente() {

        if (!validarFormulario()) return;

        try {
            cliente c = new cliente();
            if (modoEdicion) c.setId(idClienteEdicion);

            c.setNombre(txtNombre.getText().trim());
            c.setRfc(textoOpcional(txtRFC));
            c.setCurp(textoOpcional(txtCURP));
            c.setRazonSocial(textoOpcional(txtRazonSocial));
            c.setCp(obtenerNumeroOpcional(txtCP));
            c.setPais(textoOpcional(txtPais));
            c.setEstado(textoOpcional(txtEstado));
            c.setLocalidad(textoOpcional(txtLocalidad));
            c.setCiudad(textoOpcional(txtCiudad));
            c.setColonia(obtenerColoniaSeleccionada());
            c.setDomicilio(textoOpcional(txtDomicilio));
            c.setNumeroExt(obtenerNumeroOpcional(txtNoExt));
            c.setNumeroInt(obtenerNumeroOpcional(txtNoInt));
            c.setCorreo(textoOpcional(txtCorreoElectronico));
            c.setTelefono(txtTelefono.getText().trim());
            c.setStatus("activo");


            boolean exito = modoEdicion ?
                    model.modificarCliente(c) :
                    model.agregarCliente(c);

            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        modoEdicion ? "Cliente actualizado correctamente" :
                                "Cliente agregado correctamente"
                );

                if (onSaved != null) onSaved.run();

                Platform.runLater(() -> {
                    Stage stage = (Stage) btnGuardar.getScene().getWindow();
                    stage.close();
                });

            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar el cliente.");
            }

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Ocurrió un error inesperado al guardar el cliente.");
        }
    }

    private void configurarValidaciones() {
        aplicarFiltro(txtNombre, permitirTexto(100));
        aplicarFiltro(txtRFC, permitirAlfanumericoMayusculas(16));
        aplicarFiltro(txtCURP, permitirAlfanumericoMayusculas(18));
        aplicarFiltro(txtRazonSocial, permitirAlfanumericoConSimbolos(100));
        aplicarFiltro(txtCP, permitirNumeros(CP_LONGITUD));
        aplicarFiltro(txtDomicilio, permitirAlfanumericoConSimbolos(100));
        aplicarFiltro(txtNoExt, permitirNumeros(10));
        aplicarFiltro(txtNoInt, permitirNumeros(10));
        aplicarFiltro(txtCorreoElectronico, permitirEmail(100));
        aplicarFiltro(txtTelefono, permitirNumeros(10));

        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            aplicarFiltro(cmbColonia.getEditor(), permitirTexto(120));
        }
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

    private UnaryOperator<TextFormatter.Change> permitirAlfanumericoMayusculas(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!nuevo.matches("[A-Z0-9Ñ&]*")) return null;
            if (nuevo.length() > maxLength) return null;
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
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

        if (!validarCampoNumericoOpcional(txtCP, "Código Postal")) return false;
        if (!validarCampoNumericoOpcional(txtNoExt, "Número Exterior")) return false;
        if (!validarCampoNumericoOpcional(txtNoInt, "Número Interior")) return false;
        if (!validarCampoNumericoOpcional(txtTelefono, "Teléfono")) return false;

        String rfc = txtRFC.getText().trim().toUpperCase(Locale.ROOT);
        if (!rfc.isBlank() && !RFC_PATTERN.matcher(rfc).matches()) {
            mostrarAlerta(Alert.AlertType.ERROR, "El RFC no tiene un formato válido.");
            return false;
        }

        String curp = txtCURP.getText().trim().toUpperCase(Locale.ROOT);
        if (!curp.isBlank() && !CURP_PATTERN.matcher(curp).matches()) {
            mostrarAlerta(Alert.AlertType.ERROR, "La CURP no tiene un formato válido.");
            return false;
        }

        String cp = txtCP.getText().trim();
        if (!cp.isBlank() && cp.length() != CP_LONGITUD) {
            mostrarAlerta(Alert.AlertType.ERROR, "El código postal debe tener 5 dígitos.");
            return false;
        }

        String correo = txtCorreoElectronico.getText().trim();
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
        if (campo == null) return null;
        String valor = campo.getText();
        if (valor == null || valor.trim().isEmpty()) return null;
        return Integer.parseInt(valor.trim());
    }

    private String textoOpcional(TextField campo) {
        if (campo == null || campo.getText() == null) return "";
        return campo.getText().trim();
    }

    // Añade este método a la clase controllerNuevoCliente (después de setOnSaved)
    public String getNombreCliente() {
        return txtNombre.getText().trim();
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

        // Reset avisos cuando cambia CP
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

    // =========================
    // ✅ CARGA SEPOMEX (CPdescarga.txt)
    // =========================
    private SepomexCache.CpInfo buscarEnSepomex(String cp) {
        return SepomexCache.buscarCp(cp);
    }
}
