package edu.technopolis.homework.messenger.net;

import edu.technopolis.homework.messenger.User;
import edu.technopolis.homework.messenger.messages.*;

import java.nio.ByteBuffer;
import java.util.*;

public class BitProtocol implements Protocol {
    public static final byte TERMINAL = '\\';

    @Override
    public Message decode(byte[] bytes) throws ProtocolException {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == TERMINAL) {
                byte[] newBytes = new byte[bytes.length - 1];
                System.arraycopy(bytes, 0, newBytes, 0, i);
                System.arraycopy(bytes, i + 1, newBytes, i, bytes.length - i - 1);
                bytes = newBytes;
            }
        }
        try {
            int pos = 0;
            Type type = Type.values()[bytes[pos++]];
            long senderId, chatId, userId;
            String text;
            int password, n;
            switch (type) {
                case MSG_CHAT_LIST:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    return new ChatListMessage(senderId);
                case MSG_CHAT_HIST:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    chatId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    return new ChatHistoryMessage(senderId, chatId);
                case MSG_LOGIN:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    password = ejectInt(Arrays.copyOfRange(bytes, pos, pos + Integer.BYTES));
                    pos += Integer.BYTES;
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    return new LoginMessage(text, password);
                case MSG_TEXT:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    chatId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    if (bytes.length - pos == 1) {
                        System.out.println();
                    }
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    return new TextMessage(senderId, chatId, text);
                case MSG_INFO:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    userId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    return new InfoMessage(senderId, userId);
                case MSG_CHAT_CREATE:
                    senderId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    n = ejectInt(Arrays.copyOfRange(bytes, pos, pos + Integer.BYTES));
                    pos += Integer.BYTES;
                    Set<Long> invited = new HashSet<>(n);
                    for (int i = 0; i < n; i++) {
                        userId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                        pos += Long.BYTES;
                        invited.add(userId);
                    }
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    return new ChatCreateMessage(senderId, text, invited);
                case MSG_USER_CREATE:
                    password = ejectInt(Arrays.copyOfRange(bytes, pos, pos + Integer.BYTES));
                    pos += Integer.BYTES;
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    return new UserCreateMessage(text, password);
                case MSG_STATUS:
                    byte b = bytes[pos++];
                    boolean status = b == 1;
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    return new StatusMessage(status, text);
                case MSG_INFO_RESULT:
                    userId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                    pos += Long.BYTES;
                    text = ejectFullString(Arrays.copyOfRange(bytes, pos, bytes.length));
                    int c = text.indexOf('\n');
                    User user = new User(userId, text.substring(0, c), text.substring(c + 1, text.length()));
                    return new InfoResult(user);
                case MSG_CHAT_HIST_RESULT:
                    n = ejectInt(Arrays.copyOfRange(bytes, pos, pos + Integer.BYTES));
                    pos += Integer.BYTES;
                    List<TextMessage> messages = new LinkedList<>();
                    for (int i = 0; i < n; i++) {
                        int m = ejectInt(Arrays.copyOfRange(bytes, pos, pos + Integer.BYTES));
                        pos += Integer.BYTES;
                        TextMessage message = (TextMessage)decode(Arrays.copyOfRange(bytes, pos, pos + m));
                        pos += m;
                        messages.add(message);
                    }
                    return new ChatHistoryResult(messages);
                case MSG_CHAT_LIST_RESULT:
                    Set<Long> chats = new HashSet<>();
                    while (pos < bytes.length) {
                        chatId = ejectLong(Arrays.copyOfRange(bytes, pos, pos + Long.BYTES));
                        pos += Long.BYTES;
                        chats.add(chatId);
                    }
                    return new ChatListResult(chats);
            }
        } catch (Exception e) {
            throw new ProtocolException(e);
        }
        return null;
    }

    private long ejectLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    private int ejectInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private String ejectFullString(byte[] bytes) {
        return new String(bytes);
    }

    @Override
    public byte[] encode(Message msg) throws ProtocolException {
        byte[] bytes = msg.encode();
        for (int i = 0; i < bytes.length; i++) {
            //экранирование терминального символа
            if (bytes[i] == TERMINAL) {
                byte[] newBytes = new byte[bytes.length + 1];
                System.arraycopy(bytes, 0, newBytes, 0, i);
                newBytes[i] = TERMINAL;
                System.arraycopy(bytes, i, newBytes, i + 1, bytes.length - i);
                bytes = newBytes;
                i++;
            }
        }
        return bytes;
    }
}
