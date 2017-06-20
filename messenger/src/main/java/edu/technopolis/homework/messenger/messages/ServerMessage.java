package edu.technopolis.homework.messenger.messages;

public abstract class ServerMessage extends Message {
    protected ServerMessage(Type type) {
        super(type);
    }
}
