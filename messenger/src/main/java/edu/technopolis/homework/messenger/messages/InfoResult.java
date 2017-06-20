package edu.technopolis.homework.messenger.messages;

import edu.technopolis.homework.messenger.User;
import edu.technopolis.homework.messenger.Utils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class InfoResult extends Message{
    private User user;

    public InfoResult(User user) {
        super(Type.MSG_INFO_RESULT);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public byte[] encode() {
        return Utils.concat(super.encode(), user.encode());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof InfoResult))
            return false;
        if (!super.equals(other))
            return false;
        InfoResult result = (InfoResult) other;
        return Objects.equals(user, result.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), user);
    }

    @Override
    public String toString() {
        return "InfoResult{" +
                super.toString() + ", " +
                "user=" + user +
                "}";
    }
}
