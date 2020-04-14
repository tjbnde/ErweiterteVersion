package Server.Worker;

import Model.Login;
import Server.DataManager;

import java.io.*;
import java.util.Date;

public class LoginWorker extends Worker {
    private Login myLogin;

    public LoginWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Login myLogin) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myLogin = myLogin;
    }

    /**
     * Startpunkt des Threads
     * Prüft ob ein Login erfolgreich ist
     * Sendet das Ergebnis an den Client zurück
     * Loggt einen Benutzer ein wenn der Login erfolgreich ist
     *
     * @see #twoPhaseCommitLogin()
     * @see Server.DataManager#loginUser(Login, ObjectOutputStream)
     * @see Server.DataManager#writeLogEntry(String)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitLogin()) {
            myLogin.setSuccessful(true);
            dataManager.loginUser(myLogin, null);
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " successful");
        } else {
            myLogin.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " not successful");
        }

        try {
            clientOut.writeObject(myLogin);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeClientConnection();
        }
    }


    /**
     * Prüft ob ein Login erfolgreich durchgeführt werden kann mit Hilfe des "Two Phase Commit" Protokolls
     *
     * @return Wahrheitswert ob der Login erfolgreich durchgeführt werden kann
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendLoginToOtherServer() ()
     * @see Server.DataManager#loginCanBeCommited(Login)
     * @see #readLoginFromOtherServer() ()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitLogin() {
        openServerConnection();

        dataManager.writeLogEntry(new Date() + " - preparing commit of login for user " + myLogin.getUsername());
        myLogin.setStatus("PREPARE");
        sendLoginToOtherServer();

        if (!dataManager.loginCanBeCommited(myLogin)) {
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited - wrong username or password");
            myLogin.setStatus("ABORT");
            sendLoginToOtherServer();
            closeServerConnection();
            return false;
        }

        readLoginFromOtherServer();

        if (myLogin.getStatus().equals("ABORT")) {
            myLogin.setStatus("ABORT");
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited");
            sendLoginToOtherServer();
            closeServerConnection();
            return false;
        }

        if (myLogin.getStatus().equals("READY")) {
            myLogin.setStatus("COMMIT");
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can be commited");
        }

        sendLoginToOtherServer();

        readLoginFromOtherServer();

        closeServerConnection();

        return myLogin.getStatus().equals("OK");
    }


    /**
     * Liest einen Login vom anderen Server
     */
    private void readLoginFromOtherServer() {
        try {
            myLogin = (Login) serverIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }

    /**
     * Sendet einen Login zum anderen Server
     */
    private void sendLoginToOtherServer() {
        try {
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}