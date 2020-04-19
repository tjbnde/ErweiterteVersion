package Server.Worker;

import Model.Register;
import Server.DataManager;

import java.io.*;
import java.util.Date;

public class RegisterWorker extends Worker {
    private Register myRegister;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Register myRegister) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myRegister = myRegister;
    }

    /**
     * Start of the thread. Processes client command to register
     *
     * @see #twoPhaseCommitRegister() ()
     * @see Server.DataManager#addUser(Register)
     * @see Server.DataManager#loginUser(String, ObjectOutputStream)
     * @see Server.DataManager#writeLogEntry(String)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitRegister()) {
            myRegister.setSuccessful(true);
            dataManager.commitRegister(myRegister);
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " successful");
        } else {
            myRegister.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " not successful");
        }

        try {
            clientOut.writeObject(myRegister);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to client");
            if(myRegister.isSuccessful()) {
                dataManager.abortRegister(myRegister);
            }
        } finally {
            closeClientConnection();
        }
    }


    /**
     * Checks if a registration is successful with the "Two Phase Commit" protocol
     *
     * @return Success of registration
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendRegisterToOtherServer() () ()
     * @see Server.DataManager#registerCanBeCommited(Register)
     * @see #readRegisterFromOtherServer()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitRegister() {
        if (!openServerConnection()) {
            myRegister.setErrorMessage("** connection to server failed");
            return false;
        }

        dataManager.writeLogEntry(new Date() + " - preparing commit of register for user " + (myRegister.getUsername()));
        myRegister.setStatus("PREPARE");

        if (!sendRegisterToOtherServer()) {
            return false;
        }

        if (!dataManager.registerCanBeCommited(myRegister)) {
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can not be commited - username is already taken");
            myRegister.setStatus("ABORT");
            sendRegisterToOtherServer();
            closeServerConnection();
            return false;
        }

        if (!readRegisterFromOtherServer()) {
            return false;
        }

        if (myRegister.getStatus().equals("ABORT")) {
            myRegister.setStatus("ABORT");
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can not be commited");
            sendRegisterToOtherServer();
            closeServerConnection();
            return false;
        }

        if (myRegister.getStatus().equals("READY")) {
            myRegister.setStatus("COMMIT");
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can be commited");
        }

        if (!sendRegisterToOtherServer()) {
            return false;
        }

        if (!readRegisterFromOtherServer()) {
            return false;
        }

        closeServerConnection();

        return myRegister.getStatus().equals("OK");
    }

    /**
     * Reads a registration from the other server during the two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean readRegisterFromOtherServer() {
        try {
            myRegister = (Register) serverIn.readObject();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myRegister.setErrorMessage("** connection to server failed");
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    /**
     * Sends a registration to the other server during the two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean sendRegisterToOtherServer() {
        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myRegister.setErrorMessage("** connection to server failed");
            return false;
        }
        return true;
    }


}