package edu.technopolis.homework.messenger.net;

import edu.technopolis.homework.messenger.User;
import edu.technopolis.homework.messenger.messages.*;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

public class BitProtocolTest {
    Protocol protocol = new BitProtocol();
    Random random = new Random();
    long senderId;

    @Before
    public void generateIds() {
        senderId = random.nextLong();
    }

    @Test
    public void testTextMessage() {
        TextMessage textMessage = new TextMessage(random.nextLong(), senderId, randomString());
        assert assertSerialization(textMessage);
    }

    @Test
    public void testChatCreateMessage() {
        Set<Long> set = new HashSet<>(10);
        for (int i = 0; i < 10; i++) {
            set.add(random.nextLong());
        }
        ChatCreateMessage chatCreateMessage = new ChatCreateMessage(senderId, set);
        assert assertSerialization(chatCreateMessage);
    }

    @Test
    public void testChatHistoryMessage() {
        ChatHistoryMessage chatHistoryMessage = new ChatHistoryMessage(senderId, random.nextLong());
        assert assertSerialization(chatHistoryMessage);
    }

    @Test
    public void testChatHistoryResult() {
        List<TextMessage> list = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            list.add(new TextMessage(random.nextLong(), random.nextLong(), "#" + i));
        }
        ChatHistoryResult chatHistoryResult = new ChatHistoryResult(list);
        assert assertSerialization(chatHistoryResult);
    }

    @Test
    public void testChatListMessage() {
        ChatListMessage chatListMessage = new ChatListMessage(senderId);
        assert assertSerialization(chatListMessage);
    }

    @Test
    public void testChatListResult() {
        Set<Long> set = new HashSet<>(10);
        for (int i = 0; i < 10; i++) {
            set.add(random.nextLong());
        }
        ChatListResult chatListResult = new ChatListResult(set);
        assert assertSerialization(chatListResult);
    }

    @Test
    public void testInfoMessage() {
        InfoMessage infoMessage = new InfoMessage(senderId, random.nextLong());
        assert assertSerialization(infoMessage);
    }

    @Test
    public void testInfoResult() {
        User user = new User(senderId, randomString(), randomString());
        InfoResult infoResult = new InfoResult(user);
        assert assertSerialization(infoResult);
    }

    @Test
    public void testLoginMessage() {
        LoginMessage loginMessage = new LoginMessage(randomString(), randomString());
        assert assertSerialization(loginMessage);
    }

    @Test
    public void testStatusMessage() {
        StatusMessage statusMessage = new StatusMessage(random.nextBoolean(), randomString());
        assert assertSerialization(statusMessage);
    }

    private boolean assertSerialization(Message message) {
        Message messageResult = null;
        try {
            byte[] bytes = protocol.encode(message);
            messageResult = protocol.decode(bytes);
        } catch (ProtocolException e) {
            e.printStackTrace();
            assert false;
        }
        //System.out.println(messageResult);
        return message.equals(messageResult);
    }

    private String randomString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append((char)('A' + random.nextInt('z' - 'A' + 1)));
        }
        return builder.toString();
    }
}
