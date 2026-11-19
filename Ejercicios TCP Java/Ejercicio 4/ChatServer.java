import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PUERTO = 8080;
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final RoomManager roomManager = new RoomManager();
    private static final Set<String> usuariosActivos = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {
        ProtocolParser parser = new ProtocolParser();
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("ChatServer con Broadcast iniciado en puerto " + PUERTO + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ChatClientHandler(clientSocket, parser, roomManager, usuariosActivos));
            }
        }
    }
}