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
     * Startpunkt des Threads
     * Prüft ob eine Registrierung erfolgreich ist
     * Sendet das Ergebnis an den Client zurück
     * Loggt einen Benutzer ein wenn die Registrierung erfolgreich ist
     *
     * @see #twoPhaseCommitRegister() ()
     * @see Server.DataManager#addUser(Register)
     * @see Server.DataManager#loginUser(String, ObjectOutputStream)
     * @see Server.DataManager#writeLogEntry(String)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitRegister()) {
            dataManager.addUser(myRegister);
            myRegister.setSuccessful(true);
            dataManager.loginUser(myRegister.getUsername(), null);
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " successful");
        } else {
            myRegister.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " not successful");
        }

        try {
            clientOut.writeObject(myRegister);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeClientConnection();
        }
    }


    /**
     * Prüft ob eine Registrierung erfolgreich durchgeführt werden kann mit Hilfe des "Two Phase Commit" Protokolls
     *
     * @return Wahrheitswert ob die Registrierung erfolgreich durchgeführt werden kann
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendRegisterToOtherServer() () ()
     * @see Server.DataManager#registerCanBeCommited(Register)
     * @see #readRegisterFromOtherServer()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitRegister() {
        openServerConnection();

        dataManager.writeLogEntry(new Date() + " - preparing commit of register for user " + (myRegister.getUsername()));
        myRegister.setStatus("PREPARE");
        sendRegisterToOtherServer();

        if (!dataManager.registerCanBeCommited(myRegister)) {
            dataManager.writeLogEntry(new Date() + " - register for user " + myRegister.getUsername() + " can not be commited - username is already taken");
            myRegister.setStatus("ABORT");
            sendRegisterToOtherServer();
            closeServerConnection();
            return false;
        }

        readRegisterFromOtherServer();

        if(myRegister.getStatus().equals("ABORT")) {
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

        sendRegisterToOtherServer();

        readRegisterFromOtherServer();

        closeServerConnection();

        return myRegister.getStatus().equals("OK");
    }

    /**
     * Liest eine Registrierung vom anderen Server
     */
    private void readRegisterFromOtherServer() {
        try {
            myRegister = (Register) serverIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }

    /**
     * Sendet eine Registrierung zum anderen Server
     */
    private void sendRegisterToOtherServer() {
        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }


}