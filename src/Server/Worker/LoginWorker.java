package Server.Worker;

import Model.Login;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class LoginWorker extends Worker {
    private Login myLogin;

    private String hostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public LoginWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Login myLogin, String hostname) {
        super(dataManager, clientOut, clientIn);
        this.myLogin = myLogin;
        this.hostname = hostname;
    }

    private boolean twoPhaseCommitLogin() {
        dataManager.writeLogEntry(new Date() + " - preparing commit of login for user " + (myLogin.getUsername()));
        myLogin.setStatus("PREPARE");
        try {
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }


        if (!dataManager.loginCanBeCommited(myLogin)) {
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited - wrong username or password");
            myLogin.setStatus("ABORT");
            try {
                serverOut.writeObject(myLogin);
                serverOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            return false;
        }
        try {
            myLogin = (Login) serverIn.readObject();
            if (myLogin.getStatus().equals("READY")) {
                myLogin.setStatus("COMMIT");
                dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can be commited");
            } else {
                myLogin.setStatus("ABORT");
                dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited");
                serverOut.writeObject(myLogin);
                serverOut.flush();
                return false;
            }
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        try {
            myLogin = (Login) serverIn.readObject();
            if (myLogin.getStatus().equals("OK")) {
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

        if (twoPhaseCommitLogin()) {
            myLogin.setSuccessful(true);
            myLogin.setErrorMessage("");
            dataManager.loginUser(myLogin, null);
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " successful");
        } else {
            myLogin.setSuccessful(false);
            myLogin.setErrorMessage("** wrong username or password");
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " not successful");
        }
        try {
            clientOut.writeObject(myLogin);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeConnection();
            if (serverConnection != null) {
                try {
                    serverConnection.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }

    }
}