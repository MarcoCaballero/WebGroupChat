package es.codeurjc.webchat;

public class MessageInfo {

    public enum MessageType {
        ENTRY, EXIT, MSG
    }

    private User userFrom;
    private String message;
    private MessageType type;

    public MessageInfo(MessageType type, User userFrom, String message) {
        this.type = type;
        this.userFrom = userFrom;
        this.message = message;
    }

    public MessageInfo(MessageType type, User userFrom) {
        this.type = type;
        this.userFrom = userFrom;
        this.message = "";
    }

    public String getMessage() {
        return this.message;
    }

    public User getUserFrom() {
        return this.userFrom;
    }

    public MessageType getType() {
        return this.type;
    }
}
