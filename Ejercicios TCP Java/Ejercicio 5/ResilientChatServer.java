import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResilientChatServer {
    private static final int PUERTO = 8080;
    private static final int SO_TIMEOUT_MS = 30000;
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final RoomManager roomManager = new RoomManager();
    private static final Set<String> usuariosActivos = ConcurrentHashMap.newKeySet();
    private static final Set<ResilientClientHandler> handlersActivos = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        ProtocolParser parser = new ProtocolParser();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nApagando servidor de forma ordenada (Graceful Shutdown)...");
                for (ResilientClientHandler handler : handlersActivos) {
                    handler.enviarMensajeDirecto("SERVER_SHUTDOWN El servidor se cerrará en breve.");
                    handler.cerrarConexion();
                }
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
                pool.shutdown();
                try {
                    if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                        pool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                System.out.println("Servidor detenido limpiamente.");
            }));

            System.out.println("ResilientChatServer (JSMS) escuchando en puerto " + PUERTO + "...");
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(SO_TIMEOUT_MS);
                    clientSocket.setKeepAlive(true);
                    ResilientClientHandler handler = new ResilientClientHandler(
                            clientSocket, parser, roomManager, usuariosActivos, handlersActivos
                    );
                    handlersActivos.add(handler);
                    pool.execute(handler);
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        break;
                    }
                    System.err.println("Error aceptando conexión: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error fatal en ServerSocket: " + e.getMessage());
        }
    }
}