package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;

public class ChatHistoryMessage extends ClientMessage{
    private long chatId;

    public ChatHistoryMessage(long senderId, long chatId) {
        super(senderId, Type.MSG_CHAT_HIST);
        this.chatId = chatId;
    }

    public long getChatId() {
        return chatId;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(chatId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), chatId);
    }

    @Override
    public String toString() {
        return "ChatHistoryMessage{" +
                super.toString() + ", " +
                "chatId='" + chatId + '\'' +
                '}';
    }
}