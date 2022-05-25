package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {

    public static ExecutorService clientsPool = Executors.newCachedThreadPool();
    private static final Logger LOG = LogManager.getLogger(ClientHandler.class);
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            clientsPool.execute(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException | SQLException e) {
                    e.getStackTrace();
                } finally {
                    closeConnection();
                }
            });

        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getLoginPassClientsChat(parts[1], parts[2]);
                if (nick != null) {
                    if (!myServer.isNickBusy(nick)) {
                        sendMsg("/authok " + nick + " " + parts[1]);
                        name = nick;
                        myServer.broadcastMsg(name + " зашел в чат");
                        myServer.subscribe(this);
                        LOG.info(name + " зашел в чат");
                        return;
                    } else {
                        sendMsg("Учетная запись уже используется");
                    }
                } else {
                    sendMsg("Неверный логин/пароль");
                }
            }
        }
    }

    public void readMessages() throws IOException, SQLException {
        while (true) {
            String strFromClient = in.readUTF();
            String[] parts = strFromClient.split("\\s+");
            String key = parts[0];

            switch (key) {
                case "/end":
                    LOG.info(name + " вышел из чата");
                    closeConnection();
                    break;
                case "/w":
                    String[] str = strFromClient.split("\\s+");
                    String whom = str[1];
                    String message = strFromClient.substring(4 + whom.length());
                    if (!whom.equals(getName())) {
                        if (myServer.uniqueSendMsg(whom, "вам личное сообщение от " + getName() + ": " + message)) {
                            out.writeUTF("личное сообщение для " + whom + ": " + message);

                        } else if (JdbcApp.isNickClientsChat(whom)) {
                            out.writeUTF(whom + " не в сети");
                        } else {
                            out.writeUTF(whom + " не зарегистрирован");
                        }

                    } else {
                        out.writeUTF("Вы отправили себе личное сообщение");
                    }
                    break;
                case "/ch" :
                    String[] st = strFromClient.split("\\s+");
                    String newNick = st[1];
                    if (JdbcApp.isNickClientsChat(newNick)) {
                        out.writeUTF(strFromClient + " занят!");

                    } else {
                        myServer.broadcastMsg("/ch " + name + " сменил свой ник на " + newNick);
                        LOG.info(name + " сменил свой ник на " + newNick);
                        myServer.broadcastClientsList();
                        JdbcApp.changeNick(name, newNick);
                        name = newNick;
                        JdbcApp.readEx();

                    }
                    break;
                default :
                    LOG.info("Сообщение от " + name + ": " + strFromClient);
                    myServer.broadcastMsg(name + ": " + strFromClient);
            }
        }
    }

    public void sendMsg (String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection () {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
