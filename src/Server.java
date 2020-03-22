import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final boolean RUNNING = true;
    public static final int DEFAULT_PORT = 7777;

    private DataManager dataManager;

    // just for testing
    private int port;

    private ServerSocket server;

    // connection to client
    private Socket connection;
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    public Server(int port) {
        this.port = port;
        dataManager = new DataManager("user.txt", "chatList.txt", "chat.txt");
        try {
            server = new ServerSocket(port);
            System.out.println("Server successfully started on port " + port);
        } catch (IOException e) {
            System.err.println(e);
        }
        start();
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
                if (nextElement instanceof Login) {
                    Login myLogin = (Login) nextElement;
                    LoginWorker loginWorker = new LoginWorker(dataManager, clientOut, myLogin);
                    loginWorker.start();
                } else if (nextElement instanceof Register) {
                    Register myRegister = (Register) nextElement;
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }

}
