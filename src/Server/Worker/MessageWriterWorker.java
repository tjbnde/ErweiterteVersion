package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageWriterWorker implements Runnable {
    private Socket connectionToOtherServer;
    private ObjectInputStream serverIn;

    private ObjectOutputStream clientOut;

    private ServerSocket server;

    private DataManager dataManager;

    public MessageWriterWorker(DataManager dataManager, int port) {
        this.dataManager = dataManager;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(e);
        }
        connectionToOtherServer = null;
        serverIn = null;
        clientOut = null;
    }


    @Override
    public void run(){
        while (true) {
            try {
                connectionToOtherServer = server.accept();
                InputStream inputStream = connectionToOtherServer.getInputStream();
                serverIn = new ObjectInputStream(inputStream);

                Message nextMessage = (Message) serverIn.readObject();

                clientOut = dataManager.getChatPartnerSocket(nextMessage);
                clientOut.writeObject(nextMessage);
                clientOut.flush();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }
}
