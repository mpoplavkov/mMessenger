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
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 10013;
    private static final int BUFFER_SIZE = 1024;
    private static final int N_THREADS = 5;

    private Protocol protocol = new BitProtocol();
    private Map<SelectionKey, ClientHelper> map = new HashMap<>();
    private Map<Long, SelectionKey> helpMap = new HashMap<>();

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
                        if (map.get(key) != null) {
                            if (map.get(key).readSemaphore.tryAcquire()) {
                                pool.execute(() -> read(key));
                            }
                        }
                    } else if (key.isWritable()) {
                        if (map.get(key) != null) {
                            if (map.get(key).writeSemaphore.tryAcquire()) {
                                pool.execute(() -> write(key));
                            }
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
            SelectionKey selectionKey = accept.register(key.selector(), SelectionKey.OP_READ);
            map.put(selectionKey, new ClientHelper());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientHelper clientHelper = map.get(key);
        try {
            ByteBuffer readBuffer = clientHelper.readBuffer;
            int read = channel.read(readBuffer);
            //System.out.println("received " + read + " bytes");
            if (read == -1) {
                close(channel);
                helpMap.remove(clientHelper.id);
                map.remove(key);
            } else {
                ByteArrayOutputStream baos = clientHelper.readStream;
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
                        Message newMessage = processMessage(message, key);
                        byte[] bytes = protocol.encode(newMessage);
                        Byte[] bigBytes = new Byte[bytes.length];
                        Arrays.setAll(bigBytes, i -> bytes[i]);
                        clientHelper.writeQueue.addAll(Arrays.asList(bigBytes));
                        key.interestOps(SelectionKey.OP_WRITE);
                        clientHelper.writing = true;
                        System.out.println("sending " + newMessage);
                    } catch (ProtocolException e) {
                        System.out.println("received broken message");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientHelper.readSemaphore.release();
        }
    }

    private void write(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientHelper clientHelper = map.get(key);
        Queue<Byte> queue;
        if (!clientHelper.writeQueue.isEmpty()) {
            queue = clientHelper.writeQueue;
            clientHelper.writing = false;
        } else if(!clientHelper.pushQueue.isEmpty()){
            queue = clientHelper.pushQueue;
            clientHelper.pushing = false;
        } else {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        ByteBuffer buffer = clientHelper.writeBuffer;
        try {
            while (!queue.isEmpty()) {
                buffer.clear();
                while (!queue.isEmpty() && buffer.capacity() - buffer.position() > 0) {
                    buffer.put(queue.poll());
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!(clientHelper.writing || clientHelper.pushing)) {
                key.interestOps(SelectionKey.OP_READ);
            }
            clientHelper.writeSemaphore.release();
        }
    }

    private void push(long senderId, long chatId) {
        try {
            Connection connection = connectionsQueue.take();
            MessageStore messageStore = new MessageTable(connection);
            Set<Long> set = messageStore.getUsersFromChat(chatId);
            set.remove(senderId);
            TextMessage textMessage = messageStore.getLastMessageFromChat(chatId);
            String info = "PUSH! Received message:\n" + textMessage;
            StatusMessage statusMessage = new StatusMessage(true, info);
            byte[] bytes = statusMessage.encode();
            Byte[] bigBytes = new Byte[bytes.length];
            Arrays.setAll(bigBytes, i -> bytes[i]);
            for (Long userId : set) {
                SelectionKey key = helpMap.get(userId);
                ClientHelper clientHelper = map.get(key);
                clientHelper.pushQueue.addAll(Arrays.asList(bigBytes));
                clientHelper.pushing = true;
                key.interestOps(SelectionKey.OP_WRITE);
                /*if (key.isReadable()) {
                    key.interestOps(SelectionKey.OP_READ & SelectionKey.OP_WRITE);
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Message processMessage(Message message, SelectionKey key) {
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

                    pool.execute(() -> push(textMessage.getSenderId(), textMessage.getChatId()));

                    info = "Sent message to chat " + textMessage.getChatId() + ": " +
                            (textMessage.getText().length() > 20 ?
                                    (textMessage.getText().substring(0, 20) + "...")
                                    : textMessage.getText());
                    return new StatusMessage(true, info);
                case MSG_LOGIN:
                    LoginMessage loginMessage = (LoginMessage) message;
                    user = userStore.getUser(loginMessage.getLogin(), loginMessage.getPassword());
                    map.get(key).id = user.getId();
                    helpMap.put(user.getId(), key);
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
                case MSG_DELETE_TEXT:
                    DeleteTextMessage deleteTextMessage = (DeleteTextMessage) message;
                    textMessage = messageStore.deleteMessageById(deleteTextMessage.getId());
                    info = "Delete message \'" + textMessage.getText() + "\'";
                    return new StatusMessage(true, info);
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

    private static class ClientHelper {
        private long id;
        private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        private ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        private ByteArrayOutputStream readStream = new ByteArrayOutputStream();
        private Queue<Byte> writeQueue = new LinkedBlockingQueue<>();
        private Queue<Byte> pushQueue = new LinkedBlockingQueue<>();
        private Semaphore readSemaphore = new Semaphore(1);
        private Semaphore writeSemaphore = new Semaphore(1);
        private boolean writing;
        private boolean pushing;
    }
}
