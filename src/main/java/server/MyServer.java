package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
    private static final Logger LOG = LogManager.getLogger(ServerApp.class);

    private final int PORT = 8189;
    private List<ClientHandler> clients;
    private AuthService authService;
    protected AuthService getAuthService() {
        return authService;
    }

    public MyServer() {
        try (ServerSocket server = new ServerSocket (PORT)) {
            authService = new JdbcApp();
            authService.start();
            clients = new ArrayList<>();

            while (true) {
                LOG.info("Сервер ожидает подключения");
                Socket socket = server.accept();
                new ClientHandler(this, socket);
                LOG.info("Клиент подключился");
            }

        } catch (IOException e) {
            LOG.error("Ошибка в работе сервера");
        }
        if (authService != null) {
            authService.stop();
        }
    }

    public  synchronized boolean isNickBusy (String nick) {
        for (ClientHandler o : clients) {
            if (o.getName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public  synchronized void broadcastMsg (String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public synchronized void unsubscribe (ClientHandler o) {
        clients.remove(o);
        broadcastClientsList();
    }

    public synchronized void subscribe (ClientHandler o) {
        clients.add(o);
        broadcastClientsList();
    }

    public  synchronized boolean uniqueSendMsg (String whom, String message) {
        for (ClientHandler client : clients) {
            if (client.getName().equals(whom)) {
                client.sendMsg(message);
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastClientsList() {
        StringBuilder clientsSb = new StringBuilder("/clients ");
        for (ClientHandler client : clients) {
            clientsSb.append(client.getName()).append(" ");
        }
        broadcastMsg(clientsSb.toString());
    }

}
