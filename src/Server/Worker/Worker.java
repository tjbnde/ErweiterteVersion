package Server.Worker;

import Server.DataManager;

import java.io.*;
import java.net.Socket;

public abstract class Worker implements Runnable{
    DataManager dataManager;

    // communication to client
    ObjectInputStream clientIn;
    ObjectOutputStream clientOut;

    // communication to other server
    String hostname;
    Socket serverConnection;
    ObjectInputStream serverIn;
    ObjectOutputStream serverOut;

    public Worker (DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn) {
        this.clientOut = clientOut;
        this.clientIn = clientIn;
        this.dataManager = dataManager;
        serverIn = null;
        serverOut = null;
    }

    void closeConnection() {
        try{
            if (clientIn != null) {
                clientIn.close();
            }

            if (clientOut != null) {
                clientOut.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}