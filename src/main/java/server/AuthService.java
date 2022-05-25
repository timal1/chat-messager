package server;

public interface AuthService {
    void start();
    String getLoginPassClientsChat(String login, String pass);
    void stop();
}

