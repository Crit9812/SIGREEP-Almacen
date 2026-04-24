package Formularios.utilities;

import javafx.scene.control.TextField;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class helperCompraEmergente {

    // Constantes
    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");
    private static final Pattern ENTERO_PATTERN = Pattern.compile("\\d*");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");

    // ============ CLASES PARA RESULTADOS ============

    // Clase para resultados de cálculo
    public static class ResultadoCalculo {
        private final BigDecimal precioConIva;
        private final BigDecimal precioBruto;
        private final BigDecimal precioTotal;

        public ResultadoCalculo(BigDecimal precioConIva, BigDecimal precioBruto, BigDecimal precioTotal) {
            this.precioConIva = precioConIva;
            this.precioBruto = precioBruto;
            this.precioTotal = precioTotal;
        }

        public BigDecimal getPrecioConIva() { return precioConIva; }
        public BigDecimal getPrecioBruto() { return precioBruto; }
        public BigDecimal getPrecioTotal() { return precioTotal; }

        public String getPrecioConIvaFormateado() { return formatearDecimal(precioConIva); }
        public String getPrecioBrutoFormateado() { return formatearDecimal(precioBruto); }
        public String getPrecioTotalFormateado() { return formatearDecimal(precioTotal); }
    }

    // Clase para resultados de validación
    public static class ResultadoValidacion {
        private final boolean valido;
        private final String mensaje;

        public ResultadoValidacion(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        public boolean isValido() { return valido; }
        public String getMensaje() { return mensaje; }
    }

    // ============ SECCIÓN DE VALIDACIONES ============

    // Validador para números enteros
    public static UnaryOperator<String> getValidadorEnteros() {
        return cambio -> {
            if (cambio == null || cambio.isEmpty()) {
                return cambio;
            }
            // Permite solo dígitos
            return cambio.replaceAll("[^\\d]", "");
        };
    }

    // Validador para números decimales
    public static UnaryOperator<String> getValidadorDecimales() {
        return cambio -> {
            if (cambio == null || cambio.isEmpty()) {
                return cambio;
            }

            // Elimina caracteres no permitidos (excepto punto)
            String limpio = cambio.replaceAll("[^\\d.]", "");

            // Verifica múltiples puntos
            int puntoCount = 0;
            for (char c : limpio.toCharArray()) {
                if (c == '.') puntoCount++;
            }

            // Si hay más de un punto, mantiene solo el primero
            if (puntoCount > 1) {
                int primerPunto = limpio.indexOf('.');
                return limpio.substring(0, primerPunto + 1) +
                        limpio.substring(primerPunto + 1).replace(".", "");
            }

            return limpio;
        };
    }

    // Validación de cantidad
    public static ResultadoValidacion validarCantidad(String cantidadStr) {
        if (cantidadStr == null || cantidadStr.trim().isEmpty()) {
            return new ResultadoValidacion(false, "La cantidad es requerida");
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr.trim());
            if (cantidad <= 0) {
                return new ResultadoValidacion(false, "La cantidad debe ser mayor a 0");
            }
            return new ResultadoValidacion(true, "");
        } catch (NumberFormatException e) {
            return new ResultadoValidacion(false, "La cantidad debe ser un número válido");
        }
    }

    // Validación de ubicaciones
    public static ResultadoValidacion validarUbicaciones(List<?> ubicaciones, int cantidadTotal) {
        if (ubicaciones == null || ubicaciones.isEmpty()) {
            return new ResultadoValidacion(false, "Debe capturar al menos una ubicación con cantidad");
        }

        // Suma de cantidades en ubicaciones
        int sumaUbicaciones = 0;
        for (Object ubicacion : ubicaciones) {
            if (ubicacion instanceof Operaciones.compra.model.UbicacionCompra) {
                sumaUbicaciones += ((Operaciones.compra.model.UbicacionCompra) ubicacion).getCantidad();
            }
        }

        if (sumaUbicaciones != cantidadTotal) {
            return new ResultadoValidacion(false,
                    "La suma de cantidades por ubicación (" + sumaUbicaciones +
                            ") debe ser igual a la cantidad total (" + cantidadTotal + ")");
        }

        return new ResultadoValidacion(true, "");
    }

    // Validación de producto seleccionado
    public static ResultadoValidacion validarProducto(String productoId, String productoNombre) {
        if (productoId == null || productoId.trim().isEmpty() ||
                productoNombre == null || productoNombre.trim().isEmpty()) {
            return new ResultadoValidacion(false, "Complete los campos obligatorios de producto");
        }
        return new ResultadoValidacion(true, "");
    }

    // Validación de precio
    public static ResultadoValidacion validarPrecio(String precioStr) {
        if (precioStr == null || precioStr.trim().isEmpty()) {
            return new ResultadoValidacion(false, "El precio es requerido");
        }

        try {
            BigDecimal precio = new BigDecimal(precioStr.trim());
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                return new ResultadoValidacion(false, "El precio debe ser mayor a 0");
            }
            return new ResultadoValidacion(true, "");
        } catch (NumberFormatException e) {
            return new ResultadoValidacion(false, "El precio debe ser un número válido");
        }
    }

    // Validación completa del formulario
    public static ResultadoValidacion validarFormularioCompleto(
            String productoId, String productoNombre, String cantidadStr,
            List<?> ubicaciones, String precioStr) {

        // Validar producto
        ResultadoValidacion validacionProducto = validarProducto(productoId, productoNombre);
        if (!validacionProducto.isValido()) {
            return validacionProducto;
        }

        // Validar cantidad
        ResultadoValidacion validacionCantidad = validarCantidad(cantidadStr);
        if (!validacionCantidad.isValido()) {
            return validacionCantidad;
        }

        int cantidad = Integer.parseInt(cantidadStr.trim());

        // Validar ubicaciones
        ResultadoValidacion validacionUbicaciones = validarUbicaciones(ubicaciones, cantidad);
        if (!validacionUbicaciones.isValido()) {
            return validacionUbicaciones;
        }

        // Validar precio
        ResultadoValidacion validacionPrecio = validarPrecio(precioStr);
        if (!validacionPrecio.isValido()) {
            return validacionPrecio;
        }

        return new ResultadoValidacion(true, "Validación exitosa");
    }

    // ============ SECCIÓN DE CÁLCULOS ============

    // Metodo principal para calcular precios
    public static ResultadoCalculo calcularPrecios(String cantidadStr, String precioEntradaStr, boolean aplicaIva) {
        int cantidad = parseEntero(cantidadStr);
        BigDecimal precioEntrada = parseDecimal(precioEntradaStr);

        BigDecimal precioConIva = precioEntrada;
        if (aplicaIva) {
            BigDecimal iva = precioEntrada.multiply(IVA_TASA);
            precioConIva = precioEntrada.add(iva);
        }

        BigDecimal precioBruto = precioEntrada.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        return new ResultadoCalculo(precioConIva, precioBruto, precioTotal);
    }

    // Metodo específico para calcular solo el precio con IVA
    public static BigDecimal calcularPrecioConIVA(BigDecimal precioBase, boolean aplicaIva) {
        if (!aplicaIva) {
            return precioBase;
        }
        BigDecimal iva = precioBase.multiply(IVA_TASA);
        return precioBase.add(iva);
    }

    // Metodo para calcular total
    public static BigDecimal calcularPrecioTotal(int cantidad, BigDecimal precioUnitario, boolean conIva) {
        BigDecimal precioFinal = conIva ?
                calcularPrecioConIVA(precioUnitario, true) : precioUnitario;
        return precioFinal.multiply(BigDecimal.valueOf(cantidad));
    }

    // ============ MÉTODOS DE UTILIDAD ============

    // Métodos de parseo seguros
    public static int parseEntero(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static BigDecimal parseDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // Metodo de formateo
    public static String formatearDecimal(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // Metodo para formatear cualquier string decimal
    public static String formatearDecimalString(String valorStr) {
        BigDecimal valor = parseDecimal(valorStr);
        return formatearDecimal(valor);
    }
}