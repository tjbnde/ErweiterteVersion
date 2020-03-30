package Server;

import Model.*;
import Server.Worker.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    // id for identifaction between servers
    private int id;

    // hostname and port for own socket
    private String hostname;
    private int port;

    /**
     * Test
     */
    private ServerSocket server;

    private DataManager dataManager;

    // connection to client
    private Socket connection;
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    // hostname and ports to get connection to other server
    private int[] serverPorts;
    private String[] hostnames;

    // connection to other server for Two-Phase-Commit Protocol
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;


    public Server(String hostname, int port, int id) {
        this.hostname = hostname;
        this.port = port;
        this.id = id;

        dataManager = new DataManager("user.txt", "chatList.txt", "chat.txt", "log.txt");

        hostnames = new String[]{dataManager.getProperties().getProperty("hostname1"), dataManager.getProperties().getProperty("hostname2")};
        serverPorts = new int[]{Integer.parseInt(dataManager.getProperties().getProperty("port1")), Integer.parseInt(dataManager.getProperties().getProperty("port2"))};
        try {
            server = new ServerSocket(port);
            System.out.println("Server " + id + " successfully started at hostname: " + hostname + " - port: " + port);

            // get address information about other server
            String otherServerHostname = getOtherServerHostname();
            int otherServerPort = getOtherServerPort();

            BufferedReader systemInput = new BufferedReader(new InputStreamReader(System.in));
            String startLine = "";
            while (!startLine.equals("START")) {
                startLine = systemInput.readLine();
            }

            // connection to other server for Two-Phase-Commit  Protocol
            serverConnection = new Socket(InetAddress.getByName(otherServerHostname), otherServerPort);
            InputStream inputStream = serverConnection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
            OutputStream outputStream = serverConnection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
            System.out.println("** server connection established");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * method figuring out on what port the other server listens for communication
     * @return returns port of other server for 2 phase locking
     */
    private int getOtherServerPort() {
        if(serverPorts[0] == port) {
            return serverPorts[1];
        } else {
            return serverPorts[0];
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
        TwoPhaseCommitWorker twoPhaseCommitWorker = new TwoPhaseCommitWorker(dataManager, serverIn, serverOut);
        Thread twoPhaseCommitThread =  new Thread(twoPhaseCommitWorker);
        twoPhaseCommitThread.start();
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
            MessageWorker messageWorker = new MessageWorker(dataManager, clientOut, clientIn, myMessage, serverIn, serverOut);
            t = new Thread(messageWorker);
        }
        return t;
    }

}