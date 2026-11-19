import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ProtocolClientHandler implements Runnable {
    private final Socket socket;
    private final ProtocolParser parser;
    private final Set<String> usuariosConectados;
    private String username;
    private String currentRoom;

    public ProtocolClientHandler(Socket socket, ProtocolParser parser, Set<String> usuariosConectados) {
        this.socket = socket;
        this.parser = parser;
        this.usuariosConectados = usuariosConectados;
    }

    @Override
    public void run() {
        try (
                Socket clientSocket = this.socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
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
                            out.println("401 ERROR Ya estás conectado como " + this.username);
                        } else if (usuariosConectados.add(cmd.getArgument())) {
                            this.username = cmd.getArgument();
                            out.println("200 OK");
                        } else {
                            out.println("401 ERROR Nombre de usuario en uso");
                        }
                        break;

                    case JOIN:
                        if (this.username == null) {
                            out.println("401 ERROR Primero debes ejecutar CONNECT <username>");
                        } else {
                            this.currentRoom = cmd.getArgument();
                            out.println("JOINED " + this.currentRoom);
                        }
                        break;

                    case MSG:
                        if (this.username == null) {
                            out.println("401 ERROR Primero debes ejecutar CONNECT <username>");
                        } else if (this.currentRoom == null) {
                            out.println("400 ERROR Primero debes unirte a una sala con JOIN <nombre_sala>");
                        } else {
                            out.println("MSG " + this.username + ": " + cmd.getArgument());
                        }
                        break;

                    case QUIT:
                        out.println("BYE");
                        return;

                    default:
                        out.println("400 ERROR Comando no soportado");
                }
            }
        } catch (IOException e) {
            System.err.println("Error de I/O en cliente: " + e.getMessage());
        } finally {
            if (this.username != null) {
                usuariosConectados.remove(this.username);
            }
        }
    }
}