import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ChatClientHandler implements Runnable {
    private final Socket socket;
    private final ProtocolParser parser;
    private final RoomManager roomManager;
    private final Set<String> usuariosActivos;

    private PrintWriter out;
    private String username;
    private String currentRoom;

    public ChatClientHandler(Socket socket, ProtocolParser parser, RoomManager roomManager, Set<String> usuariosActivos) {
        this.socket = socket;
        this.parser = parser;
        this.roomManager = roomManager;
        this.usuariosActivos = usuariosActivos;
    }

    public void enviarMensajeDirecto(String mensaje) {
        if (out != null) {
            out.println(mensaje);
        }
    }

    @Override
    public void run() {
        try (
                Socket clientSocket = this.socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            this.out = writer;
            String line;
            while ((line = in.readLine()) != null) {
                Command cmd = parser.parse(line);
                if (cmd.getType() == Command.Type.INVALID) {
                    out.println(cmd.getErrorMessage());
                    continue;
                }

                switch (cmd.getType()) {
                    case CONNECT:
                        if (this.username != null) {
                            out.println("401 ERROR Ya registrado como " + this.username);
                        } else if (usuariosActivos.add(cmd.getArgument())) {
                            this.username = cmd.getArgument();
                            out.println("200 OK");
                        } else {
                            out.println("401 ERROR Usuario ya existe");
                        }
                        break;

                    case JOIN:
                        if (this.username == null) {
                            out.println("401 ERROR Debes hacer CONNECT primero");
                        } else {
                            if (this.currentRoom != null) {
                                roomManager.salirDeSala(this.currentRoom, this);
                            }
                            this.currentRoom = cmd.getArgument();
                            roomManager.unirseASala(this.currentRoom, this);
                            out.println("JOINED " + this.currentRoom);
                        }
                        break;

                    case MSG:
                        if (this.username == null) {
                            out.println("401 ERROR Debes hacer CONNECT primero");
                        } else if (this.currentRoom == null) {
                            out.println("400 ERROR Debes hacer JOIN <sala> primero");
                        } else {
                            String payload = "MSG " + this.username + ": " + cmd.getArgument();
                            roomManager.broadcast(this.currentRoom, payload, this);
                            out.println(payload);
                        }
                        break;

                    case QUIT:
                        out.println("BYE");
                        return;

                    default:
                        out.println("400 ERROR Comando no válido");
                }
            }
        } catch (IOException e) {
            System.err.println("Conexión interrumpida con " + (username != null ? username : "cliente") + ": " + e.getMessage());
        } finally {
            roomManager.salirDeTodasLasSalas(this);
            if (this.username != null) {
                usuariosActivos.remove(this.username);
            }
        }
    }
}