package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.nio.ByteBuffer;
import java.util.Objects;

public abstract class ClientMessage extends Message {
    private long senderId;

    ClientMessage(long senderId, Type type) {
        super(type);
        this.senderId = senderId;
    }

    public long getSenderId() {
        return senderId;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(senderId));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ClientMessage))
            return false;
        if (!super.equals(other))
            return false;
        ClientMessage message = (ClientMessage) other;
        return Objects.equals(senderId, message.senderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), senderId);
    }

    @Override
    public String toString() {
        return super.toString() +
                ", senderId='" + senderId + "'";
    }
}
