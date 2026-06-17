package arqui.grupo5.vendiasender.sender;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FtpSender {
    private final String server;
    private final String user;
    private final String pass;

    public FtpSender(String server, String user, String pass) {
        this.server = server;
        this.user = user;
        this.pass = pass;
    }

    public void enviarViaFtp(File datFile) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, 21);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            try (FileInputStream fis = new FileInputStream(datFile)) {
                boolean done = ftpClient.storeFile("/datos_ventas/" + datFile.getName(), fis);
                if (!done) {
                    throw new IOException("Fallo al subir el archivo al FTP.");
                }
            }
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }
}
