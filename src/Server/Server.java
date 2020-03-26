package Server;

import Model.*;
import Server.Worker.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final boolean RUNNING = true;
    public static final int DEFAULT_PORT = 7777;

    private int id;

    private DataManager dataManager;

    private ServerSocket server;

    // connection to client
    private Socket connection;
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    public Server(int id, int port) {

        dataManager = new DataManager("user.txt", "chatList.txt", "chat.txt");
        this.id = id;
        try {
            server = new ServerSocket(port);
            System.out.println("Server " + id + " successfully started on port " + port);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void start() {
        while (true) {
            try {
                connection = server.accept();
                InputStream inputStream = connection.getInputStream();
                clientIn = new ObjectInputStream(inputStream);
                OutputStream outputStream = connection.getOutputStream();
                clientOut = new ObjectOutputStream(outputStream);
                Object nextElement = clientIn.readObject();
                Thread t = processElement(nextElement);
                if (t != null) {
                    t.start();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }


    }

    private Thread processElement(Object nextElement) {
        Thread t = null;
        if (nextElement instanceof Login) {
            Login myLogin = (Login) nextElement;
            LoginWorker loginWorker = new LoginWorker(dataManager, clientOut, clientIn, myLogin);
            t = new Thread(loginWorker);
        } else if (nextElement instanceof Register) {
            Register myRegister = (Register) nextElement;
            RegisterWorker registerWorker = new RegisterWorker(dataManager, clientOut, clientIn, myRegister);
            t = new Thread(registerWorker);
        } else if (nextElement instanceof Chat) {
            Chat myChat = (Chat) nextElement;
            ChatWorker chatWorker = new ChatWorker(dataManager, clientOut, clientIn, myChat);
            t = new Thread(chatWorker);
        } else if (nextElement instanceof Message) {
            Message myMessage = (Message) nextElement;
            MessageWorker messageWorker = new MessageWorker(dataManager, clientOut, clientIn, myMessage);
            t = new Thread(messageWorker);
        }
        return t;
    }

}