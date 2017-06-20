package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;
import java.util.Set;

public class ChatCreateMessage extends ClientMessage {
    private Set<Long> listOfInvited;
    private String name;

    public ChatCreateMessage(long senderId, String name, Set<Long> listOfInvited) {
        super(senderId, Type.MSG_CHAT_CREATE);
        this.listOfInvited = listOfInvited;
        this.name = name;
    }

    public ChatCreateMessage(long senderId, Set<Long> listOfInvited) {
        this(senderId,
                listOfInvited.toString().length() > 50 ? listOfInvited.toString().substring(0, 50) : listOfInvited.toString(),
                listOfInvited);
    }

    public Set<Long> getListOfInvited() {
        return listOfInvited;
    }

    public String getName() {
        return name;
    }

    @Override
    public byte[] encode() {
        byte[][] setBytes = new byte[listOfInvited.size()][];
        int i = 0;
        for (Long l : listOfInvited) {
            setBytes[i++] = Utils.getBytes(l);
        }
        byte[] invitedBytes = Utils.concat(setBytes);
        return Utils.concat(super.encode(), Utils.getBytes(listOfInvited.size()), invitedBytes, name.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ChatCreateMessage))
            return false;
        if (!super.equals(other))
            return false;
        ChatCreateMessage message = (ChatCreateMessage) other;
        return Objects.equals(name, message.name) && Objects.equals(listOfInvited, message.listOfInvited);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, listOfInvited);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("ChatCreateMessage{");
        result.append(super.toString());
        result.append(", name=");
        result.append(name);
        result.append(", InvitedUsers{");
        for (long id : listOfInvited) {
            result.append(id);
            result.append(", ");
        }
        result.delete(result.length() - 2, result.length());
        result.append("}");
        return result.toString();
    }

}