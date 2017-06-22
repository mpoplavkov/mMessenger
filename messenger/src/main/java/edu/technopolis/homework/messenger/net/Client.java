package edu.technopolis.homework.messenger.net;

import edu.technopolis.homework.messenger.User;
import edu.technopolis.homework.messenger.messages.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 10013;

    private Protocol protocol = new BitProtocol();
    private User user;

    private void run() {
        try (Socket socket = new Socket(HOST, PORT);
             Scanner scanner = new Scanner(System.in)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //writing thread
            new Thread(() -> {
                while (true) {
                    String line = scanner.nextLine(); // blocking
                    Message message = processInput(line);
                    if (message == null) {
                        continue;
                    }

                    System.out.println("\nClient: sending " + message + "\n");
                    try {
                        //System.out.println(protocol.encode(message).length);
                        out.write(protocol.encode(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //reading thread
            while (true) {
                if (in.available() > 0) {
                    while (in.available() > 1) {
                        baos.write(in.read());
                    }
                    in.skip(1);
                    Message message = protocol.decode(baos.toByteArray());
                    baos.reset();

                    if (message != null) {
                        onMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onMessage(Message message) {
        System.out.println("Received " + message.getType());
        switch (message.getType()) {
            case MSG_STATUS:
                StatusMessage statusMessage = (StatusMessage) message;
                if (user == null && statusMessage.getStatus()) {
                    String[] tokens = statusMessage.getInfo().split(" ");
                    Map<String, String> map = Arrays.stream(tokens).collect(Collectors.toMap(
                            s -> s.substring(0, s.indexOf('=') == -1 ? 0 : s.indexOf('=')),
                            s -> s.substring((s.indexOf('=') + 1) == s.length() ? 0 : (s.indexOf('=') + 1), s.length())));
                    Long id = parseLong(map.get("id"));
                    String login = map.get("login");
                    user = new User(id, login);
                    System.out.println("Login successful");
                }
                System.out.println("---status   = " + statusMessage.getStatus());
                System.out.println("---info     = " + statusMessage.getInfo());
                break;
            case MSG_INFO_RESULT:
                InfoResult infoResult = (InfoResult) message;
                User user = infoResult.getUser();
                System.out.println("---id       = " + user.getId());
                System.out.println("---login    = " + user.getLogin());
                System.out.println("---about    = " + user.getAbout());
                break;
            case MSG_CHAT_HIST_RESULT:
                ChatHistoryResult chatHistoryResult = (ChatHistoryResult) message;
                System.out.println("---msgList  = ");
                for (Message mes : chatHistoryResult.getList()) {
                    System.out.println("\t\t\t\t" + mes);
                }
                break;
            case MSG_CHAT_LIST_RESULT:
                ChatListResult chatListResult = (ChatListResult) message;
                System.out.println("---chatList = " + chatListResult.getChats());
                break;
            default:
                System.out.println("Oh no! It's very bad. I should not have received this message: " + message);
                System.exit(0);
        }
    }

    /**
     * @return null if message process on client or invalid input
     */
    private Message processInput(String line) {
        byte[] bytes = line.getBytes();
        for (byte b : bytes) {
            if (b < 0) {
                System.out.println("All bytes must be positive");
                return null;
            }
        }
        String[] tokens = line.split(" ");
        String cmdType = tokens[0];
        switch (cmdType) {
            case "/login":
                if (tokens.length == 3) {
                    if (user != null) {
                        System.out.println("You're already logged in");
                        return null;
                    } else {
                        return new LoginMessage(tokens[1], tokens[2]);
                    }
                }
                System.out.println("Incorrect operands.");
                break;
            case "/help":
                System.out.println("" +
                        "/login <login> <password>              - вход в систему.\n" +
                        "/text <chatId> <message>               - отправить сообщение message в указанный чат.\n" +
                        "/info [userId]                         - получить информацию о себе (если id не указан) или о пользователе с id = userId.\n" +
                        "/chat_list                             - получить список чатов пользователя.\n" +
                        "/chat_create [name] <userId list>      - создать чат или вернуть существующий, если указан один userId. <userId list> - <userId,userId,userId...>\n" +
                        "/chat_history <chat_id>                - получить список сообщений из указанного чата." +
                        "/user_create login password            - зарегистрировать нового пользователя.");
                return null;
            case "/user_create":
                if (tokens.length == 3) {
                    if (user != null) {
                        System.out.println("You're already logged in");
                        return null;
                    } else {
                        return new UserCreateMessage(tokens[1], tokens[2]);
                    }
                }
                System.out.println("Incorrect operands.");
                break;
            case "/text":
                if (user != null) {
                    if (tokens.length > 2) {
                        long chatId = parseLong(tokens[1]);
                        if (chatId > 0) {
                            return new TextMessage(user.getId(), chatId, line.substring(tokens[0].length() + tokens[1].length() + 2));
                        }
                    }
                    System.out.println("Incorrect operands.");
                } else {
                    System.out.println("You're not logged in");
                }
                break;
            case "/info":
                if (user != null) {
                    if (tokens.length == 1) {
                        return new InfoMessage(user.getId());
                    } else if (tokens.length == 2) {
                        long userId = parseLong(tokens[1]);
                        if (userId > 0) {
                            return new InfoMessage(user.getId(), userId);
                        }
                    }
                    System.out.println("Incorrect operands.");
                } else {
                    System.out.println("You're not logged in");
                }
                break;
            case "/chat_list":
                if (user != null) {
                    if (tokens.length == 1) {
                        return new ChatListMessage(user.getId());
                    }
                    System.out.println("Incorrect operands.");
                } else {
                    System.out.println("You're not logged in");
                }
                break;
            case "/chat_create":
                if (user != null) {
                    if (tokens.length == 2) {
                        Set<Long> users = parseLongList(tokens[1]);
                        if (users != null) {
                            return new ChatCreateMessage(user.getId(), users);
                        }
                    } else if (tokens.length == 3) {
                        String name = tokens[1];
                        Set<Long> users = parseLongList(tokens[2]);
                        if (users != null) {
                            return new ChatCreateMessage(user.getId(), name, users);
                        }
                    }
                    System.out.println("Incorrect operands.");
                } else {
                    System.out.println("You're not logged in");
                }
                break;
            case "/chat_history":
                if (user != null) {
                    if (tokens.length == 2) {
                        Long chatId = parseLong(tokens[1]);
                        if (chatId > 0) {
                            return new ChatHistoryMessage(user.getId(), chatId);
                        }
                    }
                    System.out.println("Incorrect operands.");
                } else {
                    System.out.println("You're not logged in");
                }
                break;
            default:
                System.out.println("Incorrect command.");
        }

        System.out.println("Try \'/help\' for more information");
        return null;
    }

    private static long parseLong(String token) {
        try {
            return new Long(token);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static Set<Long> parseLongList(String token) {
        String[] tokens = token.split(",");
        Set<Long> set = new HashSet<>();
        for (String s : tokens) {
            long l = parseLong(s);
            if (l > 0) {
                set.add(l);
            } else {
                return null;
            }
        }
        return set;
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
