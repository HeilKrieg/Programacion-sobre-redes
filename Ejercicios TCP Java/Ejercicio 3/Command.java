public class Command {
    public enum Type {
        CONNECT,
        JOIN,
        MSG,
        QUIT,
        INVALID
    }

    private final Type type;
    private final String argument;
    private final String errorMessage;

    private Command(Type type, String argument, String errorMessage) {
        this.type = type;
        this.argument = argument;
        this.errorMessage = errorMessage;
    }

    public static Command valid(Type type, String argument) {
        return new Command(type, argument, null);
    }

    public static Command invalid(String errorMessage) {
        return new Command(Type.INVALID, null, errorMessage);
    }

    public Type getType() {
        return type;
    }

    public String getArgument() {
        return argument;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}