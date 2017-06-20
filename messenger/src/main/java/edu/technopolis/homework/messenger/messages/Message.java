package edu.technopolis.homework.messenger.messages;

import java.util.Objects;

public abstract class Message {
    private Type type;

    protected Message(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public byte[] encode() {
        byte[] bytes = new byte[1];
        bytes[0] = (byte)type.ordinal();
        return bytes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Message))
            return false;
        Message message = (Message) other;
        return Objects.equals(type, message.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }

    @Override
    public String toString() {
        return "type = " + type;
    }
}
