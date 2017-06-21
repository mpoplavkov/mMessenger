package edu.technopolis.homework.messenger.net;

import edu.technopolis.homework.messenger.User;
import edu.technopolis.homework.messenger.messages.*;
import edu.technopolis.homework.messenger.store.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 10013;
    private static final int BUFFER_SIZE = 1024;
    private static final int N_THREADS = 5;

    private Protocol protocol = new BitProtocol();
    private Map<SocketChannel, ByteBuffer> readBuffers = new HashMap<>();
    private Map<SocketChannel, ByteBuffer> writeBuffers = new HashMap<>();
    private Map<SocketChannel, ByteArrayOutputStream> byteArrays = new HashMap<>();

    //private UserStore userStore = new UserTable();
    //private MessageStore messageStore = new MessageTable();

    private Executor pool = Executors.newFixedThreadPool(N_THREADS);
    private BlockingQueue<Connection> connectionsQueue = new ArrayBlockingQueue<>(N_THREADS);

    public void run() {
        //отсоединение от БД в конце работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Connection connection : connectionsQueue) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }));
        try (ServerSocketChannel open = openChannel();
             Selector selector = Selector.open()) {
            for (int i = 0; i < N_THREADS; i++) {
                connectionsQueue.put(StoreConnection.connect());
            }
            open.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                //select синхронизируется по тому же объекту, что и операция register, соответственно, пока выполняется
                //select, register будет ждать. Соответственно, что бы не ждать произвольных wakeUp-ов селектора, будем
                //делать селект с таймаутом.
                //PS: если непосредственно перед операцией register делатб selector.wakeUp(), то нет никаких гарантий того,
                //что данный вечный цикл не прогонится еще раз и опять не уснет в селекте до выполнения register.
                selector.select(100); //blocking
                Set<SelectionKey> keys = selector.selectedKeys();
                keys.removeIf(key -> {
                    if (!key.isValid()) {
                        return true;
                    }
                    if (key.isAcceptable()) {
                        try {
                            //key перестает быть isAcceptable() только после вызова .accept(). Т.о. нужно вызывать .accept()
                            //в главном потоке, чтобы не запустилось несколько потоков с одним и тем же запросом на accept.
                            SocketChannel accept = open.accept();
                            pool.execute(() -> accept(key, accept));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (key.isReadable()) {
                        if ((boolean) key.attachment()) {
                            pool.execute(() -> {
                                read(key);
                            });
                            key.attach(false);
                        }
                    } else if (key.isWritable()) {
                        if ((boolean) key.attachment()) {
                            pool.execute(() -> {
                                write(key);
                            });
                            key.attach(false);
                        }
                    }
                    return true;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ServerSocketChannel openChannel() throws IOException {
        ServerSocketChannel open = ServerSocketChannel.open();
        open.bind(new InetSocketAddress(PORT));
        open.configureBlocking(false);
        return open;
    }

    private void accept(SelectionKey key, SocketChannel accept) {
        try {
            accept.configureBlocking(false);
            readBuffers.put(accept, ByteBuffer.allocate(BUFFER_SIZE));
            writeBuffers.put(accept, ByteBuffer.allocate(BUFFER_SIZE));
            byteArrays.put(accept, new ByteArrayOutputStream());
            accept.register(key.selector(), SelectionKey.OP_READ, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            ByteBuffer readBuffer = readBuffers.get(channel);
            int read = channel.read(readBuffer);
            //System.out.println("received " + read + " bytes");
            if (read == -1) {
                close(channel);
                readBuffers.remove(channel);
                writeBuffers.remove(channel);
                byteArrays.remove(channel);
            } else {
                ByteArrayOutputStream baos = byteArrays.get(channel);
                readBuffer.flip();
                boolean endOfMessage = false;
                while (readBuffer.remaining() > 0) {
                    byte b = readBuffer.get();
                    if (b == BitProtocol.TERMINAL) {
                        if (readBuffer.remaining() == 0) {
                            //На данном этапе может быть ошибка в случае, если данный символ не является
                            //терминальным, а всего лишь один из двух, идущих подряд (экранированный символ).
                            //Это может произойти, если на данной итерации в байтбуфер будет считано
                            //определенно количество байт.
                            //Именно поэтому декодирование сообщения будет производиться в блоке try. Если
                            //вылетит ошибка - значит сообщение пришло не полностью
                            endOfMessage = true;
                            break;
                        } else {
                            readBuffer.mark();
                            if (readBuffer.get() == BitProtocol.TERMINAL) {
                                baos.write(BitProtocol.TERMINAL);
                                baos.write(BitProtocol.TERMINAL);
                                continue;
                            } else {
                                endOfMessage = true;
                                readBuffer.reset();
                                break;
                            }
                        }
                    }
                    baos.write(b);
                }
                readBuffer.compact();

                if (endOfMessage) {
                    try {
                        Message message = protocol.decode(baos.toByteArray());
                        baos.reset();
                        Message newMessage = processMessage(message);
                        ByteBuffer writeBuffer = writeBuffers.get(channel);
                        writeBuffer.clear();
                        writeBuffer.put(protocol.encode(newMessage));
                        key.interestOps(SelectionKey.OP_WRITE);
                        System.out.println("sending " + newMessage);
                    } catch (ProtocolException e) {
                        System.out.println("received broken message");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            key.attach(true);
        }
    }

    private void write(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = writeBuffers.get(channel);
        try {
            //перед тем как отправлять буфер, его надо перевести в режим чтения!!!
            buffer.flip();
            channel.write(buffer);
            buffer.compact();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            key.attach(true);
        }
    }

    private Message processMessage(Message message) {
        User user;
        String info;
        Connection connection = null;
        try {
            connection = connectionsQueue.take();
            UserStore userStore = new UserTable(connection);
            MessageStore messageStore = new MessageTable(connection);
            switch (message.getType()) {
                case MSG_INFO:
                    InfoMessage infoMessage = (InfoMessage) message;
                    user = userStore.getUserById(infoMessage.getUserId());
                    return new InfoResult(user);
                case MSG_TEXT:
                    TextMessage textMessage = (TextMessage) message;
                    messageStore.addMessage(textMessage);

                    info = "Sent message to chat " + textMessage.getChatId() + ": " +
                            (textMessage.getText().length() > 20 ?
                                    (textMessage.getText().substring(0, 20) + "...")
                                    : textMessage.getText());
                    return new StatusMessage(true, info);
                case MSG_LOGIN:
                    LoginMessage loginMessage = (LoginMessage) message;
                    user = userStore.getUser(loginMessage.getLogin(), loginMessage.getPassword());
                    info = "id=" + user.getId() + " login=" + user.getLogin();
                    return new StatusMessage(true, info);
                case MSG_USER_CREATE:
                    UserCreateMessage userCreateMessage = (UserCreateMessage) message;
                    user = userStore.addUser(userCreateMessage.getLogin(), userCreateMessage.getPassword());
                    info = "id=" + user.getId() + " login=" + user.getLogin();
                    return new StatusMessage(true, info);
                case MSG_CHAT_CREATE:
                    ChatCreateMessage chatCreateMessage = (ChatCreateMessage) message;
                    //Можно это будет потом заменить на хранимую процедуру в БД, чтоб
                    //выполнялось за одну операцию
                    chatCreateMessage.getListOfInvited().add(chatCreateMessage.getSenderId());
                    long chatId = messageStore.createChat(chatCreateMessage.getName(), chatCreateMessage.getListOfInvited());
                    info = "Returned chat with chatId=" + chatId;
                    return new StatusMessage(true, info);
                case MSG_CHAT_HIST:
                    ChatHistoryMessage chatHistoryMessage = (ChatHistoryMessage) message;
                    List<TextMessage> messages = messageStore.getMessagesFromChat(chatHistoryMessage.getSenderId(), chatHistoryMessage.getChatId());
                    return new ChatHistoryResult(messages);
                case MSG_CHAT_LIST:
                    ChatListMessage chatListMessage = (ChatListMessage) message;
                    return new ChatListResult(messageStore.getChatsByUserId(chatListMessage.getSenderId()));
                default:
                    System.out.println("Oh no! It's very bad. I should not have received this message: " + message);
                    System.exit(0);
            }
        } catch (Exception e) {
            return new StatusMessage(false, e.getMessage());
        } finally {
            try {
                connectionsQueue.put(connection);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void close(SocketChannel sc) {
        try {
            sc.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server().run();
    }
}
