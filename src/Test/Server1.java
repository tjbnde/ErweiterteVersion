package Test;

import Server.Server;

public class Server1 {
    public static void main(String[] args) {
        Server myServer = new Server(6666);
        myServer.start();
    }
}
