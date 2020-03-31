package Server.Worker;

import Model.Login;
import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class LoginWorker extends Worker {
    private Login newLogin;

    private String hostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public LoginWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Login newLogin, String hostname) {
        super(dataManager, clientOut, clientIn);
        this.newLogin = newLogin;
        this.hostname = hostname;
    }


    private boolean twoPhaseCommitLogin() {
        dataManager.writeLogEntry(System.currentTimeMillis() + " - preparing user " + (newLogin.getUsername()) + "for committing");
        newLogin.setStatus("PREPARE");
        try {
            serverOut.writeObject(newLogin);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        if(!dataManager.loginCanBeCommited(newLogin)) {
            newLogin.setStatus("ABORT");
            try {
                serverOut.writeObject(newLogin);
                serverOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            return false;
        }

        try {
            newLogin = (Login) serverIn.readObject();
            if (newLogin.getStatus().equals("READY")) {
                newLogin.setStatus("COMMIT");
                serverOut.writeObject(clientOut);
                serverOut.flush();
            } else {
                newLogin.setStatus("ABORT");
                serverOut.writeObject(newLogin);
                serverOut.flush();
                return false;
            }
            serverOut.writeObject(newLogin);
            serverOut.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }

        try {
            newLogin = (Login) serverIn.readObject();
            if (newLogin.getStatus().equals("OK")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        return false;

    }

    public void run() {
        try {
            serverConnection = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("twoPhaseCommitPort")));
            InputStream inputStream = serverConnection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
            OutputStream outputStream = serverConnection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            System.err.println(e);
        }

        if(twoPhaseCommitLogin()) {
            newLogin.setSuccessful(true);
            newLogin.setErrorMessage("");
        } else {
            newLogin.setSuccessful(false);
            newLogin.setErrorMessage("** wrong username or password");
        }

        try {
            clientOut.writeObject(newLogin);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }
    }
}