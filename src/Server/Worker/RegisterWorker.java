package Server.Worker;

import Model.Register;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class RegisterWorker extends Worker {
    private Register myRegister;

    private String hostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Register myRegister, String hostname) {
        super(dataManager, clientOut, clientIn);
        this.myRegister = myRegister;
        this.hostname = hostname;
    }


    private boolean twoPhaseCommitLogin() {
        dataManager.writeLogEntry(new Date() + " - preparing commit of register for user " + (myRegister.getUsername()));
        myRegister.setStatus("PREPARE");
        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }


        if (!dataManager.registerCanBeCommited(myRegister)) {
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can not be commited - username is already taken");
            myRegister.setStatus("ABORT");
            try {
                serverOut.writeObject(myRegister);
                serverOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            return false;
        }
        try {
            myRegister = (Register) serverIn.readObject();
            if (myRegister.getStatus().equals("READY")) {
                myRegister.setStatus("COMMIT");
                dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can be commited");
            } else {
                myRegister.setStatus("ABORT");
                dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can not be commited");
                serverOut.writeObject(myRegister);
                serverOut.flush();
                return false;
            }
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        try {
            myRegister = (Register) serverIn.readObject();
            if (myRegister.getStatus().equals("OK")) {
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
            dataManager.addUser(myRegister);
            myRegister.setSuccessful(true);
            myRegister.setErrorMessage("");
            dataManager.loginUser(myRegister.getUsername(), null);
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " successful");
        } else {
            myRegister.setSuccessful(false);
            myRegister.setErrorMessage("** username is already taken");
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " not successful");
        }

        try {
            clientOut.writeObject(myRegister);
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