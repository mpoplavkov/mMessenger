package edu.technopolis.homework.messenger.messages;

public class ChatListMessage extends ClientMessage {
    public ChatListMessage(long senderId) {
        super(senderId, Type.MSG_CHAT_LIST);
    }

    @Override
    public byte[] encode() {
        return super.encode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ChatListMessage))
            return false;
        return super.equals(other);
    }

    @Override
    public String toString() {
        return "ChatListMessage{" +
                super.toString() +
                "}";
    }
}