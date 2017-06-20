package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.Utils;

import java.util.Objects;

public class LoginMessage extends ClientMessage {
    private String login;
    private int password;

    private LoginMessage(long senderId, String login, String password) {
        super(senderId, Type.MSG_LOGIN);
        this.login = login;
        this.password = Utils.cipherPassword(password);
    }

    public LoginMessage(String login, String password) {
        this(0, login, password);
    }

    public LoginMessage(String login, int chipheredPassword) {
        super(0, Type.MSG_LOGIN);
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
        if (!(other instanceof LoginMessage))
            return false;
        if (!super.equals(other))
            return false;
        LoginMessage message = (LoginMessage) other;
        return Objects.equals(login, message.login) && Objects.equals(password, message.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), login, password);
    }

    @Override
    public String toString() {
        return "LoginMessage{" +
                super.toString() + ", " +
                "login=" + login + ", " +
                "password='" + password + '\'' +
                '}';
    }
}