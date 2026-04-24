package Compartido.helper;

import conexion.conexionFTP;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SepomexCache {
    private static final int CP_LONGITUD = 5;
    private static final String SEPOMEX_FILE_NAME = "CPdescarga.txt";

    private static final String SEPOMEX_CP_HEADER = "d_codigo";
    private static final String SEPOMEX_COLONIA_HEADER = "d_asenta";
    private static final String SEPOMEX_MUNICIPIO_HEADER = "d_mnpio";
    private static final String SEPOMEX_ESTADO_HEADER = "d_estado";
    private static final String SEPOMEX_CIUDAD_HEADER = "d_ciudad";

    private static final Object SEPOMEX_LOCK = new Object();
    private static volatile boolean sepomexCargado = false;
    private static volatile String sepomexErrorCarga = null;
    private static Map<String, CpInfo> sepomexCache = new HashMap<>();

    private SepomexCache() {}

    public static CpInfo buscarCp(String cp) {
        try {
            cargarSepomexSiNecesario();
        } catch (IOException e) {
            sepomexErrorCarga = e.getMessage();
        }

        if (sepomexErrorCarga != null) {
            throw new IllegalStateException(sepomexErrorCarga);
        }

        return sepomexCache.get(cp);
    }

    private static void cargarSepomexSiNecesario() throws IOException {
        if (sepomexCargado || sepomexErrorCarga != null) return;

        synchronized (SEPOMEX_LOCK) {
            if (sepomexCargado || sepomexErrorCarga != null) return;

            conexionFTP ftp = new conexionFTP();
            byte[] contenido = ftp.getExtraFileBytes(SEPOMEX_FILE_NAME);
            if (contenido == null || contenido.length == 0) {
                sepomexErrorCarga = "No se pudo descargar el archivo SEPOMEX desde el FTP.";
                return;
            }

            Map<String, CpInfoBuilder> acumulado = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.ISO_8859_1))) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    sepomexErrorCarga = "El archivo SEPOMEX no contiene encabezados.";
                    return;
                }

                Map<String, Integer> headers = obtenerHeaders(headerLine);
                int idxCp = obtenerIndice(headers, SEPOMEX_CP_HEADER, "codigo", "cp");
                int idxColonia = obtenerIndice(headers, SEPOMEX_COLONIA_HEADER, "asentamiento", "colonia");
                int idxMunicipio = obtenerIndice(headers, SEPOMEX_MUNICIPIO_HEADER, "municipio");
                int idxEstado = obtenerIndice(headers, SEPOMEX_ESTADO_HEADER, "estado");
                int idxCiudad = obtenerIndice(headers, SEPOMEX_CIUDAD_HEADER, "ciudad");

                if (idxCp == -1) idxCp = 0;
                if (idxColonia == -1) idxColonia = 1;
                if (idxMunicipio == -1) idxMunicipio = 3;
                if (idxEstado == -1) idxEstado = 4;
                if (idxCiudad == -1) idxCiudad = 5;

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] campos = line.split("\\|", -1);

                    String cp = obtenerCampo(campos, idxCp);
                    cp = normalizarCp(cp);
                    if (cp.isBlank() || cp.length() != CP_LONGITUD) continue;

                    String colonia = limpiarTexto(obtenerCampo(campos, idxColonia));
                    String municipio = limpiarTexto(obtenerCampo(campos, idxMunicipio));
                    String estado = limpiarTexto(obtenerCampo(campos, idxEstado));
                    String ciudad = limpiarTexto(obtenerCampo(campos, idxCiudad));

                    CpInfoBuilder builder = acumulado.computeIfAbsent(cp, key -> new CpInfoBuilder());
                    builder.agregarColonia(colonia);
                    builder.setEstadoSiVacio(estado);
                    builder.setLocalidadSiVacio(municipio);
                    builder.setCiudadSiVacio(ciudad);
                }
            } catch (IOException e) {
                sepomexErrorCarga = "No se pudo leer el archivo SEPOMEX.";
                return;
            }

            Map<String, CpInfo> nuevoCache = new HashMap<>();
            for (Map.Entry<String, CpInfoBuilder> entry : acumulado.entrySet()) {
                nuevoCache.put(entry.getKey(), entry.getValue().build());
            }

            sepomexCache = nuevoCache;
            sepomexCargado = true;
        }
    }

    private static Map<String, Integer> obtenerHeaders(String headerLine) {
        Map<String, Integer> headers = new HashMap<>();
        String[] parts = headerLine.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            String header = parts[i];
            if (header == null) continue;
            String normalizado = header.trim().toLowerCase(Locale.ROOT);
            if (!normalizado.isBlank()) headers.put(normalizado, i);
        }
        return headers;
    }

    private static String obtenerCampo(String[] campos, int indice) {
        if (indice < 0 || indice >= campos.length) return "";
        return campos[indice];
    }

    private static int obtenerIndice(Map<String, Integer> headers, String... keys) {
        for (String key : keys) {
            if (key == null) continue;
            Integer idx = headers.get(key.toLowerCase(Locale.ROOT));
            if (idx != null) return idx;
        }
        return -1;
    }

    private static String normalizarCp(String valor) {
        if (valor == null) return "";
        String limpio = valor.trim();
        if (limpio.isEmpty()) return "";
        if (limpio.matches("\\d+")) {
            if (limpio.length() < CP_LONGITUD) {
                return String.format("%0" + CP_LONGITUD + "d", Long.parseLong(limpio));
            }
        }
        return limpio;
    }

    private static String limpiarTexto(String valor) {
        if (valor == null) return "";
        return valor.trim();
    }

    public static final class CpInfo {
        public final String pais;
        public final String estado;
        public final String localidad;
        public final String ciudad;
        public final List<String> colonias;

        private CpInfo(String pais, String estado, String localidad, String ciudad, List<String> colonias) {
            this.pais = pais;
            this.estado = estado;
            this.localidad = localidad;
            this.ciudad = ciudad;
            this.colonias = colonias;
        }
    }

    private static final class CpInfoBuilder {
        private String estado;
        private String localidad;
        private String ciudad;
        private final Set<String> colonias = new LinkedHashSet<>();

        private void agregarColonia(String colonia) {
            if (colonia != null && !colonia.isBlank()) {
                colonias.add(colonia.trim());
            }
        }

        private void setEstadoSiVacio(String valor) {
            if ((estado == null || estado.isBlank()) && valor != null && !valor.isBlank()) {
                estado = valor.trim();
            }
        }

        private void setLocalidadSiVacio(String valor) {
            if ((localidad == null || localidad.isBlank()) && valor != null && !valor.isBlank()) {
                localidad = valor.trim();
            }
        }

        private void setCiudadSiVacio(String valor) {
            if ((ciudad == null || ciudad.isBlank()) && valor != null && !valor.isBlank()) {
                ciudad = valor.trim();
            }
        }

        private CpInfo build() {
            return new CpInfo(
                    "México",
                    estado,
                    localidad,
                    ciudad,
                    new ArrayList<>(colonias)
            );
        }
    }
}
