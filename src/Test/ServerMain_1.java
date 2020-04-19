package Test;

import Server.Server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class ServerMain_1 {
    public static void main(String[] args) {
        Properties properties = new Properties();
        BufferedReader systemInput = new BufferedReader(new InputStreamReader(System.in));
        try {
            FileInputStream propertiesInputStream = new FileInputStream("config.properties") ;
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
        Server myServer = new Server(properties.getProperty("hostname1") , 1);
        Thread serverThread = new Thread(myServer);
        serverThread.start();

        String command = "";
        while (true) {
            System.out.println("** enter command");
            try {
                command = systemInput.readLine();
            } catch (IOException e) {
                System.err.println(e);
            }
            if (command.equals("exit")) {
                myServer.stopServer();
                System.out.println("** server stopped successful");
                System.exit(0);
            }
        }
    }
}
