package edu.technopolis.homework.messenger;

import java.util.Objects;

/**
 *
 */
public class User {
    private long id;
    private String login;
    private String about;

    public User(long id, String login, String about) {
        this.id = id;
        this.login = login;
        this.about = about;
    }

    public User(long id, String login) {
        this(id, login, null);
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getAbout() {
        return about;
    }

    public byte[] encode() {
        return Utils.concat(Utils.getBytes(id), (login + "\n" + about).getBytes());
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + id + "\', " +
                "password='" + "\', " +
                "about=" + (about.length() > 10 ? about.substring(0, 10) : about) +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof User))
            return false;
        User user = (User) other;
        return Objects.equals(id, user.id) && Objects.equals(login, user.login) && Objects.equals(about, user.about);
    }
}
