package Test;

import Server.Server;

public class Server2 {
    public static void main(String[] args) {
        Server myServer = new Server("192.168.178.23", 8888, 2);
        myServer.start();
    }
}
