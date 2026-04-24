package conexion;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class conexionFTP {

    private String server = "ftp.distribuidoragreep.com.mx";
    private int port = 21;
    private String user = "Inventario@distribuidoragreep.com.mx";
    private String pass = "Greepsa2025.";
    private String remoteDirImagenes = "/imagenesInventario/"; // Ya estamos directamente en la carpeta de imágenes
    private String remoteDirExtra = "/ExtraGestorInventario/"; //Archivos extra para el sistema

    public boolean uploadFile(File file, String newFileName) {
        FTPClient ftpClient = new FTPClient();
        FileInputStream inputStream = null;

        try {
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) return false;

            boolean loggedIn = ftpClient.login(user, pass);
            if (!loggedIn) return false;

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Cambiar al directorio remoto (ya existe)
            if (!remoteDirImagenes.isEmpty()) ftpClient.changeWorkingDirectory(remoteDirImagenes);

            inputStream = new FileInputStream(file);
            boolean uploaded = ftpClient.storeFile(newFileName, inputStream);
            return uploaded;

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean testConnection() {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) return false;

            boolean loggedIn = ftpClient.login(user, pass);
            ftpClient.logout();
            return loggedIn;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) ftpClient.disconnect();
            } catch (IOException ignored) {}
        }
    }

    public Image getImageFromFTP(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        FTPClient ftpClient = new FTPClient();
        try {
            // Conectar al FTP
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("No se pudo conectar al FTP. Código: " + replyCode);
                return null;
            }

            // Login
            boolean loggedIn = ftpClient.login(user, pass);
            if (!loggedIn) {
                System.out.println("Error en login FTP");
                return null;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            // Cambiar al directorio remoto ya existente
            if (!remoteDirImagenes.isEmpty()) ftpClient.changeWorkingDirectory(remoteDirImagenes);

            // Descargar archivo en memoria
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(fileName, outputStream);

            if (success) {
                byte[] bytes = outputStream.toByteArray();
                return new Image(new ByteArrayInputStream(bytes));
            } else {
                System.out.println("No se pudo obtener la imagen desde FTP: " + fileName);
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean deleteImageFromFTP(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("No se pudo conectar al FTP. Código: " + replyCode);
                return false;
            }

            boolean loggedIn = ftpClient.login(user, pass);
            if (!loggedIn) {
                System.out.println("Error en login FTP");
                return false;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            if (!remoteDirImagenes.isEmpty()) ftpClient.changeWorkingDirectory(remoteDirImagenes);

            return ftpClient.deleteFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public byte[] getExtraFileBytes(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("No se pudo conectar al FTP. Código: " + replyCode);
                return null;
            }

            boolean loggedIn = ftpClient.login(user, pass);
            if (!loggedIn) {
                System.out.println("Error en login FTP");
                return null;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            if (!remoteDirExtra.isEmpty()) ftpClient.changeWorkingDirectory(remoteDirExtra);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(fileName, outputStream);

            if (success) {
                return outputStream.toByteArray();
            }

            System.out.println("No se pudo obtener el archivo desde FTP: " + fileName);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
