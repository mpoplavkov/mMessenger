package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.sql.Date;
import java.sql.Time;
import java.util.Objects;

public class TextMessage extends ClientMessage {
    private long chatId;
    private String text;
    private Time time;
    private Date date;

    public TextMessage(long senderId, long chatId, String text, Date date, Time time) {
        super(senderId, Type.MSG_TEXT);
        this.chatId = chatId;
        this.text = text;
        this.date = date;
        this.time = time;
    }

    public TextMessage(long senderId, long chatId, String text) {
        this(senderId, chatId, text, new Date(0), new Time(0));
    }

    public String getText() {
        return text;
    }

    public long getChatId() {
        return chatId;
    }

    public Time getTime() {
        return time;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(
                super.encode(),
                Utils.getBytes(chatId),
                Utils.getBytes(date.getTime()),
                Utils.getBytes(time.getTime()),
                text.getBytes());
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
                "datetime=" + time + " " + date + ", " +
                "text='" + text + "\'" +
                '}';
    }
}