public class ProtocolParser {

    public Command parse(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return Command.invalid("ERROR 400: Comando vacío. Sintaxis válida: CONNECT <username> | JOIN <nombre_sala> | MSG <texto> | QUIT");
        }

        String trimmed = rawLine.trim();
        String[] parts = trimmed.split("\\s+", 2);
        String verb = parts[0].toUpperCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (verb) {
            case "CONNECT":
                if (arg.isEmpty() || arg.contains(" ")) {
                    return Command.invalid("ERROR 400: Sintaxis inválida para CONNECT. Uso: CONNECT <username> (sin espacios)");
                }
                return Command.valid(Command.Type.CONNECT, arg);

            case "JOIN":
                if (arg.isEmpty() || arg.contains(" ")) {
                    return Command.invalid("ERROR 400: Sintaxis inválida para JOIN. Uso: JOIN <nombre_sala>");
                }
                return Command.valid(Command.Type.JOIN, arg);

            case "MSG":
                if (arg.isEmpty()) {
                    return Command.invalid("ERROR 400: Sintaxis inválida para MSG. Uso: MSG <texto>");
                }
                return Command.valid(Command.Type.MSG, arg);

            case "QUIT":
                if (!arg.isEmpty()) {
                    return Command.invalid("ERROR 400: QUIT no acepta argumentos. Uso: QUIT");
                }
                return Command.valid(Command.Type.QUIT, null);

            default:
                return Command.invalid("ERROR 400: Comando desconocido '" + verb + "'. Comandos permitidos: CONNECT, JOIN, MSG, QUIT");
        }
    }
}