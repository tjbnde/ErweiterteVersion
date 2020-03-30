package Test;

import Server.Server;

public class Server1 {
    public static void main(String[] args) {
        Server myServer = new Server("192.168.178.62" , 6666, 1);
        myServer.start();
    }
}
