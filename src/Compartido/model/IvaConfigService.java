package Compartido.model;

import conexion.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class IvaConfigService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal IVA_PORCENTAJE_DEFAULT = new BigDecimal("16");

    private static volatile BigDecimal ivaPorcentajeGlobal = IVA_PORCENTAJE_DEFAULT;

    private IvaConfigService() {
    }

    public static BigDecimal getIvaPorcentaje() {
        return ivaPorcentajeGlobal;
    }

    public static BigDecimal getIvaTasa() {
        return ivaPorcentajeGlobal.divide(CIEN, 6, RoundingMode.HALF_UP);
    }

    public static synchronized void recargarDesdeBaseDatos() {
        String sql = "SELECT IVA FROM configuracion WHERE id = 1 LIMIT 1";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            if (rs.next()) {
                BigDecimal valor = rs.getBigDecimal("IVA");
                ivaPorcentajeGlobal = normalizarPorcentaje(valor);
            }
        } catch (Exception e) {
            ivaPorcentajeGlobal = IVA_PORCENTAJE_DEFAULT;
        }
    }

    public static synchronized boolean actualizarEnBaseDatos(BigDecimal nuevoPorcentaje) {
        BigDecimal normalizado = normalizarPorcentaje(nuevoPorcentaje);
        String sql = "UPDATE configuracion SET IVA = ?, fecha = CURRENT_DATE WHERE id = 1";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setBigDecimal(1, normalizado);
            int actualizados = st.executeUpdate();
            if (actualizados > 0) {
                ivaPorcentajeGlobal = normalizado;
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static BigDecimal normalizarPorcentaje(BigDecimal valor) {
        if (valor == null) {
            return IVA_PORCENTAJE_DEFAULT;
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}

