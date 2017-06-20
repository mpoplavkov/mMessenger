package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class UserCreateMessage extends ClientMessage {
    private String login;
    private int password;

    private UserCreateMessage(long senderId, String login, String password) {
        super(senderId, Type.MSG_USER_CREATE);
        this.login = login;
        this.password = Utils.cipherPassword(password);
    }

    public UserCreateMessage(String login, String password) {
        this(0, login, password);
    }

    public UserCreateMessage(String login, int chipheredPassword) {
        super(0, Type.MSG_USER_CREATE);
        this.login = login;
        this.password = chipheredPassword;
    }

    public String getLogin() {
        return login;
    }

    public int getPassword() {
        return password;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), Utils.getBytes(password), login.getBytes());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof UserCreateMessage))
            return false;
        if (!super.equals(other))
            return false;
        UserCreateMessage message = (UserCreateMessage) other;
        return Objects.equals(login, message.login) && Objects.equals(password, message.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), login, password);
    }

    @Override
    public String toString() {
        return "UserCreateMessage{" +
                super.toString() +
                ", login=" + login +
                ", password=" + password +
                "}";
    }
}
