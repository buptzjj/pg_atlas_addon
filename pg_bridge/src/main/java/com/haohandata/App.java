package com.haohandata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * 程序入口
 */
public final class App {

    /**
     *
     */

    private final static Logger logger = LoggerFactory.getLogger(App.class);

    private App() {
    }

    /**
     * Main function.
     * 
     * @param args The arguments of the program.
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws ClassNotFoundException {
        String url = MessageFormat.format("jdbc:postgresql://{0}:{1}/{2}", Constant.DB_HOST,
                String.valueOf(Constant.DB_PORT), Constant.DB_NAME);
        Connection conn;
        try {
            conn = DriverManager.getConnection(url, Constant.DB_USER, Constant.DB_PASSWORD);
            logger.info("Build connection url>{}", url);
            Listener listener = new Listener(conn);
            listener.start();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.info("Build connection url>{} failed", url);
        }

    }
}
