package Server;

import Model.*;
import Server.Worker.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    // id for identification between servers
    private int id;

    // hostname and port for own socket
    private String hostname;
    private int port;

    private ServerSocket server;

    private DataManager dataManager;

    // connection to client
    private Socket connection;
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    // hostnames of server
    private String[] hostnames;

    // connection to other server for Two-Phase-Commit Protocol
    private String serverHostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;


    public Server(String hostname, int id) {
        this.hostname = hostname;
        this.id = id;

        dataManager = new DataManager("user.txt", "chatList.txt", "chat.txt", "log.txt");
        int communicationPort = Integer.parseInt(dataManager.getProperties().getProperty("communicationPort"));
        int twoPhaseCommitPort = Integer.parseInt(dataManager.getProperties().getProperty("twoPhaseCommitPort"));
        int messageWriterPort = Integer.parseInt(dataManager.getProperties().getProperty("messageWriterPort"));

        hostnames = new String[]{dataManager.getProperties().getProperty("hostname1"), dataManager.getProperties().getProperty("hostname2")};

        try {
            server = new ServerSocket(communicationPort);
            System.out.println("Server " + id + " successfully started at hostname: " + hostname + " - port: " + communicationPort);

            serverHostname = getOtherServerHostname();

            TwoPhaseCommitWorker twoPhaseCommitWorker = new TwoPhaseCommitWorker(dataManager, twoPhaseCommitPort);
            Thread twoPhaseCommitThread =  new Thread(twoPhaseCommitWorker);
            twoPhaseCommitThread.start();

            MessageWriterWorker messageWriterWorker = new MessageWriterWorker(dataManager, messageWriterPort);
            Thread writerThread = new Thread(messageWriterWorker);
            writerThread.start();

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private String getOtherServerHostname() {
        if(hostnames[0].equals(hostname)) {
            return hostnames[1];
        } else {
            return hostnames[0];
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
            LoginWorker loginWorker = new LoginWorker(dataManager, clientOut, clientIn, serverHostname, myLogin);
            t = new Thread(loginWorker);
        } else if (nextElement instanceof Register) {
            Register myRegister = (Register) nextElement;
            RegisterWorker registerWorker = new RegisterWorker(dataManager, clientOut, clientIn, serverHostname, myRegister);
            t = new Thread(registerWorker);
        } else if (nextElement instanceof Chat) {
            Chat myChat = (Chat) nextElement;
            ChatWorker chatWorker = new ChatWorker(dataManager, clientOut, clientIn, serverHostname, myChat);
            t = new Thread(chatWorker);
        }
        return t;
    }

}