package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;
import java.util.Set;

public class ChatListResult extends Message {
    private Set<Long> chats;

    public ChatListResult(Set<Long> chats) {
        super(Type.MSG_CHAT_LIST_RESULT);
        this.chats = chats;
    }

    public Set<Long> getChats() {
        return chats;
    }

    @Override
    public byte[] encode() {
        byte[][] setBytes = new byte[chats.size()][];
        int i = 0;
        for (Long l : chats) {
            setBytes[i++] = Utils.getBytes(l);
        }
        byte[] chatsBytes = Utils.concat(setBytes);
        return Utils.concat(super.encode(), chatsBytes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ChatListResult))
            return false;
        if (!super.equals(other))
            return false;
        ChatListResult result = (ChatListResult) other;
        return Objects.equals(chats, result.chats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), chats);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("ChatListResult{");
        stringBuilder.append(super.toString());
        stringBuilder.append(", Chats{");
        for (Long l : chats) {
            stringBuilder.append(l);
            stringBuilder.append(", ");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
