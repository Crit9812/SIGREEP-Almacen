package conexion;
//javac -encoding UTF-8 -cp ".;mysql.jar" prueba.java
//java -Dfile.encoding=UTF-8 -cp ".;mysql.jar" prueba

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexionExtra {
    public Connection conn;

    private static final String HOST = "jdbc:mysql://distribuidoragreep.com.mx:3306/distribu_";
    private static final String USER = "distribu_Admin";
    private static final String PASSWORD = "AdminGreep2025.";

    public static void main(String[] args) {
    }

    private static String construirURL(String nombreBD) {
        return HOST + nombreBD
                + "?useSSL=false"
                + "&serverTimezone=UTC"
                + "&characterEncoding=UTF-8"
                + "&connectTimeout=15000"
                + "&socketTimeout=120000"
                + "&tcpKeepAlive=true"
                + "&autoReconnect=true"
                + "&maxReconnects=3";
    }

    private static String construirURLMonitor(String nombreBD) {
        return HOST + nombreBD
                + "?useSSL=false"
                + "&serverTimezone=UTC"
                + "&characterEncoding=UTF-8"
                + "&connectTimeout=1500"
                + "&socketTimeout=1500"
                + "&tcpKeepAlive=true"
                + "&autoReconnect=true"
                + "&maxReconnects=1";
    }

    public Connection conectar(String nombreBD) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = construirURL(nombreBD);
            conn = DriverManager.getConnection(url, USER, PASSWORD);

            System.out.println("Conexión exitosa a la base de datos 2: " + nombreBD);
            ConexionMonitor.getInstance().notificarConexionRestablecidaInmediata();
            return conn;

        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC no encontrado: " + e.getMessage());
            throw new IllegalStateException("Driver JDBC no encontrado", e);
        } catch (SQLException e) {
            System.err.println("Error de conexión o SQL: " + e.getMessage());
            ConexionMonitor.getInstance().notificarDesconexionInmediata();
            throw new IllegalStateException("No se pudo establecer la conexión a la base de datos", e);
        }
    }

    public static boolean probarConexion(String nombreBD) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = construirURL(nombreBD);

            try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean probarConexionRapida(String nombreBD) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = construirURLMonitor(nombreBD);

            try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (Exception e) {
            return false;
        }
    }
}