package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;

public class InfoMessage extends ClientMessage {
    private long userId;

    public InfoMessage(long senderId, long userId) {
        super(senderId, Type.MSG_INFO);
        this.userId = userId;
    }

    public InfoMessage(long senderId) {
        this(senderId, senderId);
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(userId));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof InfoMessage))
            return false;
        if (!super.equals(other))
            return false;
        InfoMessage info = (InfoMessage) other;
        return Objects.equals(userId, info.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId);
    }

    @Override
    public String toString() {
        return "InfoMessage{" +
                super.toString() + ", " +
                "userId='" + userId + '\'' +
                '}';
    }
}