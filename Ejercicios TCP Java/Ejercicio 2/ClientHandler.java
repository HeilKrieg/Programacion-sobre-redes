import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                Socket clientSocket = this.socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                if ("CHAO".equalsIgnoreCase(mensaje)) {
                    out.println("ADIOS");
                    break;
                }
                out.println("ECO [" + Thread.currentThread().getName() + "]: " + mensaje);
            }
        } catch (IOException e) {
            System.err.println("Error atendiendo cliente: " + e.getMessage());
        }
    }
}