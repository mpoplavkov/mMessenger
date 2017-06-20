package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;
import edu.technopolis.homework.messenger.net.BitProtocol;
import edu.technopolis.homework.messenger.net.Protocol;
import edu.technopolis.homework.messenger.net.ProtocolException;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ChatHistoryResult extends Message {
    private List<TextMessage> messages;
    private Protocol protocol = new BitProtocol();

    public ChatHistoryResult(List<TextMessage> messages) {
        super(Type.MSG_CHAT_HIST_RESULT);
        this.messages = messages;
    }

    public List<TextMessage> getList() {
        return messages;
    }

    @Override
    public byte[] encode() {
        byte[][] listBytes = new byte[messages.size()][];
        int i = 0;
        try {
            for (TextMessage message : messages) {
                listBytes[i] = protocol.encode(message);
                listBytes[i] = Utils.concat(Utils.getBytes(listBytes[i].length), listBytes[i]);
                i++;
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        byte[] messagesBytes = Utils.concat(listBytes);
        return Utils.concat(super.encode(), Utils.getBytes(messages.size()), messagesBytes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ChatHistoryResult))
            return false;
        if (!super.equals(other))
            return false;
        ChatHistoryResult message = (ChatHistoryResult) other;
        return Objects.equals(messages, message.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messages);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("ChatHistoryResult{");
        stringBuilder.append(super.toString());
        stringBuilder.append(", Messages{");
        for (TextMessage message : messages) {
            stringBuilder.append(message.getText().length() > 10 ? message.getText().substring(0, 10) : message.getText());
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}