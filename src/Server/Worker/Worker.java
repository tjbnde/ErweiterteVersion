package Server.Worker;

import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public abstract class Worker implements Runnable {
    DataManager dataManager;

    ObjectInputStream clientIn;
    ObjectOutputStream clientOut;

    String hostname;
    Socket serverConnection;
    ObjectInputStream serverIn;
    ObjectOutputStream serverOut;

    /**
     * Konstrukter der Klasse
     * @param dataManager Datenmanager
     * @param clientOut Output Stream zum Client
     * @param clientIn Input Stream zum Client
     * @param hostname Hostname vom anderen Server
     */
    public Worker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname) {
        this.clientOut = clientOut;
        this.clientIn = clientIn;
        this.dataManager = dataManager;
        this.hostname = hostname;
        serverConnection = null;
        serverIn = null;
        serverOut = null;
    }

    void openServerConnection() {
        try {
            serverConnection = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("twoPhaseCommitPort")));
            InputStream inputStream = serverConnection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
            OutputStream outputStream = serverConnection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void closeServerConnection() {
        if (serverConnection != null) {
            try {
                serverConnection.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    void closeClientConnection() {
        if (clientIn != null) {
            try {
                clientIn.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        if (clientOut != null) {
            try {
                clientOut.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}