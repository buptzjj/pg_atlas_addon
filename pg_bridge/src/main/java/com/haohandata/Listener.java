package com.haohandata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener extends Thread {

    private final static Logger logger = LoggerFactory.getLogger(Listener.class);

    private Connection conn;
    private PGConnection pgConn;
    private Gson gson;

    Listener(Connection conn) throws SQLException {
        logger.debug("init pg listener");
        this.conn = conn;
        this.pgConn = (PGConnection) conn;
        this.gson = new Gson();
        Statement stmt = conn.createStatement();
        stmt.execute(MessageFormat.format("listen {0}", Constant.NOTIFY_CHANNEL));
        stmt.close();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select 1");
                rs.close();
                stmt.close();
                PGNotification[] notifications = pgConn.getNotifications();
                if (notifications != null) {
                    for (int i = 0; i < notifications.length; i++) {
                        List<DDLEvent> ddlEventList = gson.fromJson(notifications[i].getParameter(),
                                new TypeToken<List<DDLEvent>>() {
                                }.getType());
                        for (DDLEvent ddlEvent : ddlEventList) {
                            System.out.println(ddlEvent.toString());
                        }
                    }
                }
                Thread.sleep(500);
            } catch (SQLException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
