package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;

public class DeleteTextMessage extends ClientMessage {
    private long id;

    public DeleteTextMessage(long senderId, long id) {
        super(senderId, Type.MSG_DELETE_TEXT);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    @Override
    public String toString() {
        return "DeleteTextMessage{" +
                super.toString() + ", " +
                "id='" + id + '\'' +
                '}';
    }
}
