package chat;

import java.io.Serializable;
import java.util.List;

public class ActiveChatsResponse implements Serializable {
    private final List<ChatSummary> chats;

    public ActiveChatsResponse(List<ChatSummary> chats) {
        this.chats = chats;
    }

    public List<ChatSummary> getChats() {
        return chats;
    }
}
