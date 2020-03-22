import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public final static int MAIN_SERVER_PORT = 5555;

    private Balancer balancer;

    private ServerSocket server;
    private BufferedReader networkIn;
    private PrintWriter networkOut;
    private int[] availableServer;

    public MainServer(int[] availableServer) {
        balancer = new Balancer();
        networkOut = null;
        networkIn = null;
        try {
            server = new ServerSocket(MAIN_SERVER_PORT);
            System.out.println("Main Server successfully started on port " + MAIN_SERVER_PORT);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        this.availableServer = availableServer;
    }


    public void start() {
        Socket connection = null;
        while (true) {
            try {
                connection = server.accept();
                // incoming message from client
                networkIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                networkOut = new PrintWriter(connection.getOutputStream());

                String request = networkIn.readLine();
                if(request.equals("SERVERREQUEST")) {
                    // answer client with random server
                    int serverNr = balancer.returnRandomNumber();
                    networkOut.println(availableServer[serverNr]);
                    networkOut.flush();
                } else {
                    networkOut.println("0");
                    networkOut.flush();
                }
            } catch(IOException e) {
                System.err.println(e);
            } finally {
                if(connection != null) {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }
        }
    }
}
