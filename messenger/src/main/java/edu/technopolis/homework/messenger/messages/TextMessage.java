package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;

public class TextMessage extends ClientMessage {
    private long chatId;
    private String text;

    public TextMessage(long senderId, long chatId, String text) {
        super(senderId, Type.MSG_TEXT);
        this.chatId = chatId;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public long getChatId() {
        return chatId;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(chatId), text.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof TextMessage))
            return false;
        if (!super.equals(other))
            return false;
        TextMessage message = (TextMessage) other;
        return Objects.equals(chatId, message.chatId) && Objects.equals(text, message.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), chatId, text);
    }

    @Override
    public String toString() {
        return "TextMessage{" +
                super.toString() + ", " +
                "chatId='" + chatId + "\', " +
                "text='" + text + '\'' +
                '}';
    }
}