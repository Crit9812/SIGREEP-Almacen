package conexion;
//javac -encoding UTF-8 -cp ".;mysql.jar" prueba.java
//java -Dfile.encoding=UTF-8 -cp ".;mysql.jar" prueba

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Conexion {
    public Connection conn;
    private static final String URL = "jdbc:mysql://distribuidoragreep.com.mx:3306/distribu_Almacen"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&characterEncoding=UTF-8"
            + "&connectTimeout=15000"
            + "&socketTimeout=120000"
            + "&tcpKeepAlive=true"
            + "&autoReconnect=true"
            + "&maxReconnects=3";
    private static final String USER = "distribu_Admin";
    private static final String PASSWORD = "AdminGreep2025.";
    private static final String URL_MONITOR = "jdbc:mysql://distribuidoragreep.com.mx:3306/distribu_Almacen"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&characterEncoding=UTF-8"
            + "&connectTimeout=1500"
            + "&socketTimeout=1500"
            + "&tcpKeepAlive=true"
            + "&autoReconnect=true"
            + "&maxReconnects=1";

    public static void main(String[] args) {
    }

    public Connection conectar(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión exitosa a la base de datos.");
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

    public static boolean probarConexion() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean probarConexionRapida() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL_MONITOR, USER, PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
