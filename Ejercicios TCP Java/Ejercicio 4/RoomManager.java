import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoomManager {
    // Mapa concurrente: NombreSala -> Lista thread-safe de Manejadores de Clientes
    private final Map<String, List<ChatClientHandler>> salas = new ConcurrentHashMap<>();

    public void unirseASala(String nombreSala, ChatClientHandler cliente) {
        salas.computeIfAbsent(nombreSala, k -> new CopyOnWriteArrayList<>()).addIfAbsent(cliente);
    }

    public void salirDeSala(String nombreSala, ChatClientHandler cliente) {
        if (nombreSala == null || cliente == null) {
            return;
        }
        List<ChatClientHandler> miembros = salas.get(nombreSala);
        if (miembros != null) {
            miembros.remove(cliente);
            if (miembros.isEmpty()) {
                salas.remove(nombreSala, miembros);
            }
        }
    }

    public void salirDeTodasLasSalas(ChatClientHandler cliente) {
        for (String sala : salas.keySet()) {
            salirDeSala(sala, cliente);
        }
    }

    public void broadcast(String nombreSala, String mensaje, ChatClientHandler emisor) {
        List<ChatClientHandler> miembros = salas.get(nombreSala);
        if (miembros != null) {
            for (ChatClientHandler miembro : miembros) {
                if (miembro != emisor) { // Evitar eco al propio emisor
                    miembro.enviarMensajeDirecto(mensaje);
                }
            }
        }
    }
}