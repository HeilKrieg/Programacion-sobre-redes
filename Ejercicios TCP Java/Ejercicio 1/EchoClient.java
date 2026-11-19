import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(HOST, PUERTO);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado a " + HOST + ":" + PUERTO + ". Escribe 'CHAO' para salir.");

            String lineaUsuario;
            while ((lineaUsuario = consola.readLine()) != null) {
                out.println(lineaUsuario);
                if ("CHAO".equalsIgnoreCase(lineaUsuario.trim())) {
                    break;
                }
                String respuestaServidor = in.readLine();
                if (respuestaServidor == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }
                System.out.println(respuestaServidor);
            }
        } catch (IOException e) {
            System.err.println("Error de conexión en EchoClient: " + e.getMessage());
        }
    }
}