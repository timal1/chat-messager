package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class AnyClient extends JFrame  {
    private final String SERVER_ADDR = "localhost";
    private final int SERVER_PORT = 8189;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private JTextField fieldLogin;
    private JTextField fieldPass;
    private JFrame frame;
    private JTextArea infoText;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private JTextArea clientArea;
    private JFrame frameNick;
    private JTextField fieldNewNick;
    private JTextArea infoTextNick;
    private String nick;
    private String login;
    private File file;

    public AnyClient() {
        try {
            launchingFrame();
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openConnection() throws IOException {

        try {
            socket = new Socket(SERVER_ADDR, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            socket.setSoTimeout(120000);

            new Thread(() -> {
                try {
                    while (true) {
                        String strFromServer = in.readUTF();
                        if (strFromServer.startsWith("/authok")) {
                            String[] parts = strFromServer.split("\\s");
                            nick = parts[1];
                            login = parts[2];
                            frame.setVisible(false);
                            prepareGUI();
                            chatArea.append("Ваша авторизация прошла успешно \nВаш nickname: " + nick + "\n");
                            socket.setSoTimeout(0);
                            file = new File(login + ".txt");
                            break;
                        }
                        infoText.setText(null);
                        infoText.append(strFromServer + "\n");
                    }

                    try (FileWriter writer = new FileWriter(file.getName(), true);
                         BufferedReader reader = new BufferedReader(new FileReader(file.getName()))) {
                        ArrayList<String> arr = new ArrayList<>();
                        if (file.exists()) {
                                String str;
                                // задание к уроку 3 сделал последние 5 сообщений для наглядности
                                while ((str = reader.readLine()) != null) {
                                    arr.add(str);
                                }
                                // чтобы сделать 100, надо 5 заменить на 100
                                if (arr.size() > 5) {
                                    for (int j = arr.size() - 5; j < arr.size(); j++) {
                                        chatArea.append(arr.get(j) + "\n");
                                    }
                                } else if (arr.size() > 0) {
                                    for (String s: arr) {
                                        chatArea.append(s + "\n");
                                    }
                                }

                            while (true) {
                                String strFromServer = in.readUTF();
                                String[] parts = strFromServer.split("\\s+");
                                String key = parts[0];

                                switch (key) {
                                    case "/end":
                                        break;
                                    case "/clients":
                                        String clientsList = strFromServer.substring("/clients ".length());
                                        clientArea.setText(null);
                                        clientArea.append(clientsList);
                                        break;
                                    case "/ch" :
                                        String[] part = strFromServer.split("\\s");
                                        String serviceWord = part[2];
                                        String message = strFromServer.substring("/ch ".length());
                                        if (serviceWord.equals("сменил")) {
                                            chatArea.append(message + "\n");

                                        } else if (serviceWord.equals("занят!")) {
                                            chatArea.append("Вы пытались сменить ник, но ник " + message);
                                            infoTextNick.setText(null);
                                            infoTextNick.append(message + "\n");
                                        }
                                        break;
                                    default :
                                        chatArea.append(strFromServer + "\n");
                                        writer.write(strFromServer + "\n");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (SocketTimeoutException s) {
                    infoText.setText("Время авторизации вышло \nПерезапустите приложение");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage() {
        if (!msgInputField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(msgInputField.getText());
                msgInputField.setText("");
                msgInputField.grabFocus();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }

    public void onAuthClick () {

        if (!fieldLogin.getText().trim().isEmpty() && !fieldPass.getText().trim().isEmpty()) {
            try {
                out.writeUTF("/auth " + fieldLogin.getText() + " " + fieldPass.getText());
                fieldLogin.setText("");
                fieldPass.grabFocus();
                fieldPass.setText("");
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }
    public void changeNickClick () {
        if (!fieldNewNick.getText().trim().isEmpty()) {
            try {
                out.writeUTF("/ch " + fieldNewNick.getText());
                fieldNewNick.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


 // основное окно клиента
    public void prepareGUI() {
        // Параметры окна
        setBounds(600, 300, 300, 400);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Текстовое поле для вывода сообщений
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        //Верхнее сервисное поле с кнопкой меню
        JMenuBar servicePanel = new JMenuBar();
        setJMenuBar(servicePanel);
        clientArea = new JTextArea();
        clientArea.setEditable(false);
        servicePanel.add(clientArea, BorderLayout.EAST);
        JMenu service = new JMenu("Меню");
        JMenuItem changeNick = new JMenuItem("Сменить ник");
        JMenuItem exit = new JMenuItem("Выход");
        service.add(changeNick);
        service.addSeparator();
        service.add(exit);
        servicePanel.add(service, Box.createHorizontalGlue());
        service.setBorder(BorderFactory.createEtchedBorder());

        // слушатель кнопки смена ника
        changeNick.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                changeNickFrame();
            }
        });

        // слушатель кнопки exit
        exit.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    out.writeUTF("/end");
                    closeConnection();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        // Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton btnSendMsg = new JButton("Отправить");
        bottomPanel.add(btnSendMsg, BorderLayout.EAST);
        msgInputField = new JTextField();
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(msgInputField, BorderLayout.CENTER);

        // слушатель кнопки отправить
        btnSendMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        msgInputField.addActionListener(e -> sendMessage());

        // Настраиваем действие на закрытие окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF("/end");
                   closeConnection();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    // окно авторизации, запускается уже со словами login и pass? нужно
    // добавить только цифры, при нажатии enter курсор перескакивает и отправляет ввод
    public void launchingFrame() {
        frame = new JFrame("Авторизация");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        frame.setLocation(600,300);

        Container contentPane = frame.getContentPane();

        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);

        Component labelLogin = new JLabel("Логин");
        fieldLogin = new JTextField("login",15);
        Component labelPass = new JLabel("Пароль");
        fieldPass = new JTextField("pass",15);
        JButton button = new JButton("Отправить");
        infoText = new JTextArea("");

        contentPane.add(labelLogin);
        contentPane.add(fieldLogin);
        contentPane.add(labelPass);
        contentPane.add(fieldPass);
        contentPane.add(button);
        contentPane.add(infoText);

        layout.putConstraint(SpringLayout.WEST, labelLogin, 10,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, labelLogin, 25,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.NORTH, fieldLogin, 25,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, fieldLogin, 20,
                SpringLayout.EAST, labelLogin);

        layout.putConstraint(SpringLayout.WEST, labelPass, 10,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, labelPass, 55,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.NORTH, fieldPass, 55,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, fieldPass, 10,
                SpringLayout.EAST, labelPass);
        layout.putConstraint(SpringLayout.WEST, button, 80,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, button, 85,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.NORTH, infoText, 120,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, infoText, 10,
                SpringLayout.EAST, labelPass);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAuthClick();
            }
        });

        fieldLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldPass.grabFocus();
            }
        });

        fieldPass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAuthClick();
            }
        });

        frame.setVisible(true);
    }

    public void changeNickFrame() {
        frameNick = new JFrame("Смена ника");
        frameNick.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frameNick.setSize(300, 200);
        frameNick.setLocation(600,300);

        Container contentPane = frameNick.getContentPane();

        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);

        Component labelNewNick = new JLabel("Введите новый ник");
        fieldNewNick = new JTextField(15);
        JButton buttonChangeNick = new JButton("Сменить");
        infoTextNick = new JTextArea("");

        contentPane.add(labelNewNick);
        contentPane.add(fieldNewNick);
        contentPane.add(buttonChangeNick);
        contentPane.add(infoTextNick);

        layout.putConstraint(SpringLayout.NORTH, labelNewNick, 20,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, labelNewNick, 40,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, fieldNewNick, 50,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, fieldNewNick, 40,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, buttonChangeNick, 80,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, buttonChangeNick, 40,
                SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, infoTextNick, 110,
                SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, infoTextNick, 40,
                SpringLayout.WEST, contentPane);

        buttonChangeNick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    changeNickClick();
                    frameNick.setVisible(false);
            }
        });

        fieldNewNick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               changeNickClick();
                frameNick.setVisible(false);
            }
        });

        frameNick.setVisible(true);
    }
}
