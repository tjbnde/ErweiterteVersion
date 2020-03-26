package Server.Worker;

import Server.DataManager;

import java.io.*;

public abstract class Worker implements Runnable{
    DataManager dataManager;
    ObjectInputStream clientIn;
    ObjectOutputStream clientOut;

    public Worker (DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn) {
        this.clientOut = clientOut;
        this.clientIn = clientIn;
        this.dataManager = dataManager;
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