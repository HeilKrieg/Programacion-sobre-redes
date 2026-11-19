import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class JsmsCliClient {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);

        try (
                Socket socket = new Socket(HOST, PUERTO);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))
        ) {
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while (running.get() && (serverMsg = in.readLine()) != null) {
                        System.out.println("\n<-- " + serverMsg);
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("\nConexión perdida con el servidor: " + e.getMessage());
                    }
                } finally {
                    running.set(false);
                }
            }, "JSMS-Listener");

            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("Conectado a JSMS (" + HOST + ":" + PUERTO + ").");
            System.out.println("Comandos: CONNECT <user> | JOIN <sala> | MSG <texto> | QUIT");

            String userInput;
            while (running.get()) {
                System.out.print("> ");
                userInput = consola.readLine();
                if (userInput == null) {
                    break;
                }
                out.println(userInput);
                if ("QUIT".equalsIgnoreCase(userInput.trim())) {
                    running.set(false);
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error en cliente JSMS: " + e.getMessage());
        } finally {
            running.set(false);
        }
    }
}