package edu.technopolis.homework.messenger.store;

import edu.technopolis.homework.messenger.config.Config;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StoreConnection {
    public static Connection connect() throws IOException, ClassNotFoundException, SQLException {
        Config config = new Config();

        String url = config.getURL();
        String name = config.getDbUserName();
        String pass = config.getDbUserPassword();

        Class.forName(config.getDriverClassName());
        return DriverManager.getConnection(url, name, pass);
    }
}
