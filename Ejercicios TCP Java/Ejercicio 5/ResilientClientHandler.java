import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Set;

public class ResilientClientHandler implements Runnable {
    private final Socket socket;
    private final ProtocolParser parser;
    private final RoomManager roomManager;
    private final Set<String> usuariosActivos;
    private final Set<ResilientClientHandler> handlersActivos;

    private PrintWriter out;
    private String username;
    private String currentRoom;

    public ResilientClientHandler(
            Socket socket,
            ProtocolParser parser,
            RoomManager roomManager,
            Set<String> usuariosActivos,
            Set<ResilientClientHandler> handlersActivos
    ) {
        this.socket = socket;
        this.parser = parser;
        this.roomManager = roomManager;
        this.usuariosActivos = usuariosActivos;
        this.handlersActivos = handlersActivos;
    }

    public void enviarMensajeDirecto(String mensaje) {
        if (out != null) {
            out.println(mensaje);
        }
    }

    public void cerrarConexion() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
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
                            out.println("401 ERROR Ya conectado como " + this.username);
                        } else if (usuariosActivos.add(cmd.getArgument())) {
                            this.username = cmd.getArgument();
                            out.println("200 OK");
                        } else {
                            out.println("401 ERROR Usuario ya en uso");
                        }
                        break;

                    case JOIN:
                        if (this.username == null) {
                            out.println("401 ERROR Primero ejecuta CONNECT <username>");
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
                            out.println("401 ERROR Primero ejecuta CONNECT <username>");
                        } else if (this.currentRoom == null) {
                            out.println("400 ERROR Primero ejecuta JOIN <nombre_sala>");
                        } else {
                            String formatted = "MSG " + this.username + ": " + cmd.getArgument();
                            roomManager.broadcast(this.currentRoom, formatted, this);
                            out.println(formatted);
                        }
                        break;

                    case QUIT:
                        out.println("BYE");
                        return;

                    default:
                        out.println("400 ERROR Comando no soportado");
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout de inactividad (30s) para " + (username != null ? username : "cliente"));
            if (out != null) {
                out.println("ERROR 408 Conexión cerrada por inactividad");
            }
        } catch (SocketException e) {
            System.out.println("Desconexión abrupta de " + (username != null ? username : "cliente") + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de I/O en handler: " + e.getMessage());
        } finally {
            handlersActivos.remove(this);
            if (this.currentRoom != null) {
                roomManager.salirDeSala(this.currentRoom, this);
            }
            if (this.username != null) {
                usuariosActivos.remove(this.username);
            }
            cerrarConexion();
        }
    }
}